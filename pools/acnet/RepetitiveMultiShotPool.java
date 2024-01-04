// $Id: RepetitiveMultiShotPool.java,v 1.5 2023/11/01 20:56:57 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventObserver;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.Node;

public class RepetitiveMultiShotPool extends RepetitiveDaqPool implements DataEventObserver, Completable, AcnetErrors
{
    final OneShotDaqPool multiShotPool;
    
    public RepetitiveMultiShotPool(Node node, DataEvent event)
	{
        super(node, event);

    	this.multiShotPool = new OneShotDaqPool(node);
    }

	@Override
    public void update(DataEvent userEvent, DataEvent currentEvent)
	{
		process(false);
	}

	@Override
	public boolean process(boolean forceRetransmission)
	{
		procTime = System.currentTimeMillis();

		final int remaining = removeDeletedRequests();

		if (remaining == 0)
			event.deleteObserver(this);
		else
			event.addObserver(this);

		synchronized (requests) {
			multiShotPool.insert(requests);
			multiShotPool.process(false);
			updateTime = System.currentTimeMillis();

			++totalTransactions;

			if (!event.isRepetitive()) {
				requests.clear();
				event.deleteObserver(this);
			}

			return true;
		}
    }

	@Override
    public void completed(int status)
	{
        if (status != ACNET_PEND) {
			lastCompletedStatus = status;

			if (status == ACNET_UTIME) //{
				process(true);

			updateTime = System.currentTimeMillis();

			++totalTransactions;

			if (!event.isRepetitive())
				process(false);
			
			multiShotPool.process(false);
		}
    }

	@Override
	public String toString()
	{
		final String header =  String.format("RepetitiveDaqPool %-4d %-10s event:%-16s status:%04x procd:%tc",
												id, node.name(), event, lastCompletedStatus & 0xffff, procTime); 

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
}
