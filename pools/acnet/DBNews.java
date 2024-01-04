// $Id: DBNews.java,v 1.3 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.logging.Level;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessage;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessageHandler;
import gov.fnal.controls.service.proto.Dbnews;
import gov.fnal.controls.servers.dpm.events.DeviceDatabaseListObserver;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class DBNews extends Thread implements AcnetMessageHandler, Dbnews.Request.Receiver
{
	static ConcurrentLinkedQueue<DeviceDatabaseListObserver> observers = new ConcurrentLinkedQueue<>();

	//final AcnetConnection connection;
	final LinkedBlockingQueue<Dbnews.Request.Report> msgQ = new LinkedBlockingQueue<>();

	static void init() throws AcnetStatusException
	{
		new DBNews();
	}

	public static void addObserver(DeviceDatabaseListObserver observer)
	{
		observers.add(observer);
	}

	public static void removeObserver(DeviceDatabaseListObserver observer)
	{
		observers.remove(observer);
	}

	private DBNews() throws AcnetStatusException
	{
		final AcnetConnection connection = AcnetInterface.open("DBNEWS");

		this.setDaemon(true);
		this.setName("DBNews Handler");
		this.start();

		connection.handleMessages(this);
	}

	@Override
	public void run()
	{
		while (true) {
			Dbnews.Request.Report m;

			try {
				m = msgQ.take();
			} catch (InterruptedException e) {
				continue;
			}

			//List<DeviceDatabaseListObserver> oList = MulticastNews.getObserverList();

			//for (DeviceDatabaseListObserver obs : oList) {
			for (DeviceDatabaseListObserver observer : observers) {
				try {
					observer.deviceDatabaseListChange(m.info);
				} catch (Exception e) {
					logger.log(Level.FINE, "exception in dbnew handler", e);
				}
			}
		}
	}

	@Override
    public void handle(AcnetMessage r)
	{
		try {
			Dbnews.Request.unmarshal(r.data()).deliverTo(this);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception in dbnews message", e);
		}
	}

	@Override
	public void handle(Dbnews.Request.Report m)
	{
		msgQ.add(m);
	}
}

