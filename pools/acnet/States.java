// $Id: States.java,v 1.3 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.HashSet;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.concurrent.LinkedBlockingQueue;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessage;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessageHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.pools.Node;
import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class States extends Thread implements AcnetMessageHandler
{
	class State
	{
		final int di;
		final int state;
		final long date;

		State(int di, int state, long date)
		{
			this.di = di;
			this.state = state;
			this.date = date;
		}
	}

	final AcnetConnection connection;
	final LinkedBlockingQueue<State> msgQ = new LinkedBlockingQueue<>();
	final HashSet<Integer> frontEndUp;

	static void init() throws AcnetStatusException
	{
		new States(AcnetInterface.open("STATES"));
	}

	private void add(HashSet<Integer> set, String name)
	{
		try {
			set.add(Lookup.getDeviceInfo(name).di);
		} catch (Exception ignore) { }
	}

	private States(AcnetConnection connection) throws AcnetStatusException
	{
		this.connection = connection;
		this.frontEndUp = new HashSet<>();

		add(frontEndUp, "V:FEUP");
		add(frontEndUp, "V:FESU");

		this.connection.handleMessages(this);
		setName("States Handler");
		start();
	}

	@Override
	public void run()
	{
		while (true) {
			try {
				final State report = msgQ.take();
				
				if (frontEndUp.contains(report.di)) {
					logger.info("Node " + connection.getName(report.state) + " is up");
					AcceleratorPool.nodeUp(Node.get(report.state));
				}
			} catch (InterruptedException e) {
				break;
			} catch (Exception ignore) { }
		}
	}

	@Override
    public void handle(AcnetMessage r)
	{
		try {
			final ByteBuffer buf = r.data().order(ByteOrder.LITTLE_ENDIAN);
			final int version = buf.getShort() & 0xffff;
			
			if (version == 1) {
				final int seqNo = buf.getShort() & 0xffff;
				final int count = buf.getInt();

				for (int ii = 0; ii < count; ii++) {
					final int typeCode = buf.getShort() & 0xffff;

					if (typeCode == 31) {
						final int di = buf.getInt();
						final int state = buf.getShort() & 0xffff;
						final long seconds = buf.getInt() & 0xffffffff;
						final long nanoSeconds = buf.getInt() & 0xffffffff;
						final long date = (seconds * 1000) + (nanoSeconds / 1000000);

						msgQ.add(new State(di, state, date));
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception handling states message", e);
		}
	}

/*
	public static synchronized void main(String[] args) throws Exception
	{
		AcnetInterface.init();
		Lookup.init();
		States.init();

		States.class.wait();
	}
*/
}

