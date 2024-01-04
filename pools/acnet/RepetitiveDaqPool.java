// $Id: RepetitiveDaqPool.java,v 1.10 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.logging.Level;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.ClockEvent;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventObserver;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;
import gov.fnal.controls.servers.dpm.events.OnceImmediateEvent;
import gov.fnal.controls.servers.dpm.events.StateEvent;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.Node;
import gov.fnal.controls.servers.dpm.pools.NodeFlags;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class RepetitiveDaqPool extends DaqPool implements NodeFlags, DataEventObserver, Completable, AcnetErrors
{
    private static Recovery recovery = new Recovery();

	public class SharedWhatDaq extends WhatDaq implements ReceiveData
	{
		private final LinkedList<WhatDaq> users = new LinkedList<>();

		public SharedWhatDaq(WhatDaq whatDaq)
		{
			super(whatDaq, 0);

			this.receiveData = this;
			addUser(whatDaq);
		}

		synchronized public void addUser(WhatDaq whatDaq)
		{
			if (whatDaq == null)
				throw new NullPointerException();

			users.add(whatDaq);
		}

		synchronized int removeDeletedUsers()
		{
			int totalDeleted = 0;
			final Iterator<WhatDaq> iter = users.iterator();

			while (iter.hasNext()) {
				if (iter.next().isMarkedForDelete()) {
					iter.remove();
					totalDeleted++;
				}
			}

			return totalDeleted;
		}

		synchronized boolean isEmpty()
		{
			return users.isEmpty();
		}

		@Override
		synchronized public void receiveData(byte[] data, int offset, long timestamp, long cycle)
		{
			for (final WhatDaq user : users) {
				try {
					user.getReceiveData().receiveData(data, offset, timestamp, cycle);
				} catch (Exception e) {
					user.setMarkedForDelete();
				}
			}
		}

		@Override
		synchronized public void receiveData(ByteBuffer data, long timestamp, long cycle)
		{
			data.mark();

			for (final WhatDaq user : users) {
				try {
					user.getReceiveData().receiveData(data, timestamp, cycle);
					data.reset();
				} catch (Exception e) {
					user.setMarkedForDelete();
				}
			}
		}

		@Override
		synchronized public void receiveStatus(int status, long timestamp, long cycle)
		{
			for (final WhatDaq user : users) {
				try {
					user.getReceiveData().receiveStatus(status, timestamp, cycle);
				} catch (Exception e) {
					user.setMarkedForDelete();
				}
			}
		}

		@Override
		//synchronized public void receiveData(WhatDaq whatDaq, int error, int offset, byte[] data, 
		//										long timestamp, CollectionContext context)
		synchronized public void receiveData(int error, int offset, byte[] data, long timestamp)
		{
			for (final WhatDaq user : users) {
				try {
					//user.getReceiveData().receiveData(user, error, offset, data, timestamp, context);
					user.getReceiveData().receiveData(error, offset, data, timestamp);
				} catch (Exception e) {
					user.setMarkedForDelete();
				}
			}
		}
	}

	int totalProcs = 0;
	int numInserts = 0;
	int totalInserts = 0;
	int totalTransactions = 0;
	volatile int lastCompletedStatus = 0;
	long procTime = 0;
	long updateTime = 0;

	final LinkedList<WhatDaq> requests = new LinkedList<>();
	DaqSendInterface xtrans = DaqSendFactory.init();

    private final static long deferredProcessMillis = 200;
    
    private DeltaTimeEvent deferredProcessEvent = new DeltaTimeEvent(deferredProcessMillis);
    
    public RepetitiveDaqPool(Node node, DataEvent event)
	{
        super(node, event);

        //recovery.add(this);
    }

	@Override
    public void update(DataEvent userEvent, DataEvent currentEvent)
	{
	}

	boolean recovery(long now)
	{
        if (requests.size() != 0) {
			if (lastCompletedStatus != 0) {
                process(true);
				return true;
            } else if (node.has(EVENT_STRING_SUPPORT)) {
				if (procTime != 0 && (now - procTime > 120000L) && updateTime == 0 || (now - updateTime > 120000L)) {
                	process(true);
					return true;
				}
            }
        }

		return false;
	}

	@Override
    public void insert(WhatDaq whatDaq)
	{
        ++totalInserts;

        if (whatDaq.length() > DaqDefinitions.MaxReplyDataLength()) {
            PoolSegmentAssembly.insert(whatDaq, this, DaqDefinitions.MaxReplyDataLength(), true);
            return;
        }

        synchronized (requests) {
            Iterator<WhatDaq> url = requests.iterator();

            while (url.hasNext()) {
                final SharedWhatDaq shared = (SharedWhatDaq) url.next();

                if (shared.isEqual(whatDaq)) {
                    synchronized (shared) {
                        shared.addUser(whatDaq);
                    }
                    return;
                }
            }
            
            requests.add(new SharedWhatDaq(whatDaq));
            numInserts++;
        }
    }

	@Override
    public void cancel(PoolUser user, int error)
	{
        final long now = System.currentTimeMillis();

        synchronized (requests) {
            Iterator<WhatDaq> url = requests.iterator();

            while (url.hasNext()) {
                final SharedWhatDaq shared = (SharedWhatDaq) url.next();

                synchronized (shared) {
                    Iterator<WhatDaq> whatDaqs = shared.users.iterator();

                    for (WhatDaq whatDaq; whatDaqs.hasNext();) {
                        whatDaq = whatDaqs.next();

                        if (user == null || whatDaq.getUser() == user) {
                            if (error != 0)
                                //whatDaq.getReceiveData().receiveData(whatDaq, error, 0, null, now, null);
                                whatDaq.getReceiveData().receiveData(error, 0, null, now);

                            whatDaq.setMarkedForDelete();
                        }
                    }
                }
            }
        }

		recovery.remove(this);
    }

	protected final int removeDeletedRequests()
	{
		int totalDeleted = 0;

        synchronized (requests) {
            final Iterator<WhatDaq> iter = requests.iterator();

            while (iter.hasNext()) {
				final SharedWhatDaq shared = (SharedWhatDaq) iter.next();
				final int deleted = shared.removeDeletedUsers();

				if (shared.isEmpty())
					iter.remove();

				totalDeleted += deleted;
            }
        }

		return totalDeleted;
	}

	//private final void cancelTransaction()
	//{
		//if (xtrans != null) {
	//		xtrans.cancel();
			//xtrans = null;
        //}
	//}

	@Override
	public boolean process(boolean forceXmit)
	{
        final boolean inserts = numInserts > 0;
		final boolean deletes = removeDeletedRequests() > 0;
		final boolean changes = inserts || deletes;

        numInserts = 0;
		procTime = System.currentTimeMillis();

		if (requests.size() == 0) {
			xtrans.cancel();
			//cancelTransaction();
			recovery.remove(this);
            return false;
        }
        
        if (!changes) {
            if (forceXmit) {
                lastCompletedStatus = 0;
                //if (xtrans != null) {
                    ++totalProcs;
                    xtrans.send(true);
                    return true;
                //}
            } else {
                return false;
            }
        }

        ++totalProcs;

		synchronized (requests) {
			//if (xtrans != null) {
				xtrans.cancel();
				//xtrans = null;
			//}
			lastCompletedStatus = 0;

			xtrans = DaqSendFactory.getDaqSendInterface(node, event, false, this, event.defaultTimeout());
			xtrans.send(requests);
			recovery.add(this);

			return true;
		}
    }

	@Override
    public void completed(int status)
	{
        if (status != ACNET_PEND) {
			lastCompletedStatus = status;

			//if (status == ACNET_UTIME)
				//process(true);

			updateTime = System.currentTimeMillis();

			++totalTransactions;
		}
    }

	@Override
	public String toString()
	{
		final String header =  String.format("RepetitiveDaqPool %-4d %-10s event:%-16s status:%04x procd:%tc",
												id, node.name(), event, lastCompletedStatus & 0xffff, 
												System.currentTimeMillis());

		final StringBuilder buf = new StringBuilder(header);

		buf.append("\n\n");

		int ii = 1;
		for (WhatDaq whatDaq : requests)
			buf.append(String.format(" %3d) %s\n", ii++, whatDaq));

		buf.append('\n');
		buf.append(xtrans);
	
        buf.append("\ninserts: ").append(totalInserts);
        buf.append(", procs: ").append(totalProcs);
        buf.append(", packets: ").append(totalTransactions);

		return buf.append('\n').toString();
	}

    private static class Recovery implements DataEventObserver
	{
    	final DeltaTimeEvent event = new DeltaTimeEvent(3 * 60 * 1000);
    	//final LinkedList<RepetitiveDaqPool> pools = new LinkedList<>();
    	final HashMap<Integer, RepetitiveDaqPool> pools = new HashMap<>();

        Recovery()
		{
            event.addObserver(this);
        }

        synchronized public void add(RepetitiveDaqPool pool)
		{
            pools.put(pool.id, pool);
        }

        synchronized public void remove(RepetitiveDaqPool pool)
		{
            //pools.remove(pool);
            pools.remove(pool.id);
        }

        synchronized public void update(DataEvent userEvent, DataEvent currentEvent)
		{
			logger.log(Level.FINE, () -> String.format("Repetitive pool recovery has %d entries", pools.size()));

			final long now = System.currentTimeMillis();

			for (RepetitiveDaqPool pool : pools.values()) {
				final boolean resent = pool.recovery(now);

				logger.log(Level.FINE, () -> String.format("Repetitive pool %6d recovery for %-10s event:%-16s entries:%5d action:[%6s]", 
															pool.id, pool.node.name(), pool.event, pool.requests.size(), resent ? "RESENT" : "")); 

			}
        }
    }
}
