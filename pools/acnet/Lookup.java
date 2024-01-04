// $Id: Lookup.java,v 1.3 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;
 
import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.logging.Logger;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestContext;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.events.DeviceDatabaseListObserver;
import gov.fnal.controls.service.proto.Dbnews;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract public class Lookup extends gov.fnal.controls.service.proto.Lookup_v2 implements AcnetErrors
{
	private static String lookupNodeName = "MCAST";
	private static final DeviceInfoCache cache = new DeviceInfoCache();

	private static class DeviceInfoCache extends HashMap<String, Lookup.DeviceInfo> implements DeviceDatabaseListObserver
	{
		DeviceInfoCache()
		{
			super(8 * 1024);
			DBNews.addObserver(this);
		}

		@Override
		synchronized public void deviceDatabaseListChange(Dbnews.Request.Info[] dbnews)
		{
			for (Dbnews.Request.Info dbEdit : dbnews) {
				final Lookup.DeviceInfo info = remove("0:" + dbEdit.di);

				if (info != null) {
					remove(info.name);
					logger.fine("removing " + info.name + "(" + info.di + ") from the LOOKUP cache");
				}
			}
		}

		synchronized Lookup.DeviceInfo get(String name)
		{
			return super.get(name);
		}

		synchronized void put(Lookup.DeviceInfo info)
		{
			super.put("0:" + info.di, info);
			super.put(info.name, info);
		}
	}

	private static class RpcHandler implements AcnetReplyHandler
	{
		AcnetReply acnetReply;
		Lookup.Reply lookupReply;

		@Override
		public void handle(AcnetReply r)
		{
			synchronized (this) {
				acnetReply = r;

				if (r.status() == 0) {
					try {
						lookupReply = Lookup.Reply.unmarshal(r.data());
					} catch (IOException e) {
						lookupReply = null;
						logger.warning("unable to unmarshal LOOKUP reply");
					}
				} else
					lookupReply = null;


				this.notify();
			}
		 }
	}

	private static AcnetConnection connection;
	private static int lookupNode = 0;
	//private static final ByteBuffer buf = ByteBuffer.allocateDirect(256);
	private static final ByteBuffer buf = ByteBuffer.allocate(256);
	private static final RpcHandler rpcHandler = new RpcHandler();

	static public void init()
	{
		init(AcnetInterface.open("LOOKUP"));
	}
	
	static public void init(AcnetConnection connection)
	{
		Lookup.setNode(System.getProperty("lookupnode", "MCAST"));
		Lookup.connection = connection;

		try {
			locateService();
		} catch (Exception ignore) { }
	}

	static private void rpc(int node, String task, Lookup.Request req, int timeout) 
														throws IOException, AcnetStatusException
	{
        synchronized (rpcHandler) {
			buf.clear();
			req.marshal(buf).flip();

			//System.out.println("req: " + req + "  REMAINING: " + buf.remaining());
			//System.out.println();

            AcnetRequestContext ctxt = connection.requestSingle(node, task, buf, timeout, rpcHandler);

            try {
                rpcHandler.wait();
            } catch (InterruptedException e) {
                ctxt.cancel();
                throw new AcnetStatusException(ACNET_DISCONNECTED);
            }
        }
	}

	synchronized static private void locateService() throws AcnetStatusException, IOException
	{
		rpc(connection.getNode(lookupNodeName), "LOOKUP", new Lookup.Request.ServiceDiscovery(), 2000);

		//System.out.println("LOOKUP LOCATE REPLY status " + rpcHandler.acnetReply.status());

		if (rpcHandler.acnetReply.status() == 0) {
			Lookup.Reply.ServiceDiscovery msg = (Lookup.Reply.ServiceDiscovery) rpcHandler.lookupReply;

			lookupNode = rpcHandler.acnetReply.server();
			logger.info("Lookup service found on " + msg.serviceLocation + 
							"(" + Integer.toHexString(lookupNode) + ")");
		} else
			throw new AcnetStatusException(rpcHandler.acnetReply.status());
	}

	static private Lookup.DeviceInfo getDeviceInfoImpl(String device) throws AcnetStatusException, IOException
	{
		Lookup.DeviceInfo info = cache.get(device);

		if (info == null) {
			Lookup.Request.DeviceNameLookup req = new Lookup.Request.DeviceNameLookup();

			req.name = device;
			
			rpc(lookupNode, "LOOKUP", req, 5000);

			if (rpcHandler.acnetReply.status() == 0) {
				info = ((Lookup.Reply.DeviceLookup) rpcHandler.lookupReply).info;
				if (info != null)
					cache.put(info);
			} else
				throw new AcnetStatusException(rpcHandler.acnetReply.status());
		}

		return info;
	}

	synchronized public static Lookup.DeviceInfo getDeviceInfo(int di) throws AcnetStatusException, DeviceNotFoundException
	{
		return getDeviceInfo("0:" + di);
	}

	synchronized public static Lookup.DeviceInfo getDeviceInfo(String device) throws AcnetStatusException, DeviceNotFoundException
	{
		Lookup.DeviceInfo dInfo = null;

		try {
			dInfo = getDeviceInfoImpl(device);
		} catch (AcnetStatusException e1) {
			e1.printStackTrace();
			try {
				locateService();
				dInfo = getDeviceInfoImpl(device);
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new AcnetStatusException(DPM_LOOKUP_FAILED);
			}
		} catch (IOException e) {
			throw new AcnetStatusException(DPM_LOOKUP_FAILED);
		}

		if (dInfo != null) {
			switch (dInfo.state) {
				case Deleted:
					throw new AcnetStatusException(DBM_DELDEV);

				case Obsolete:
					throw new AcnetStatusException(DBM_OBSDEV);
					
				case Documented:
					throw new AcnetStatusException(DBM_DOCDEV);
			}
		} else
			throw new DeviceNotFoundException();

		return dInfo;
	}

	static protected void setNode(String node)
	{
		Lookup.lookupNodeName = node;
	}

	public static void main(String[] args) throws Exception
	{
		Lookup.lookupNodeName = args[0];
		Lookup.connection = AcnetInterface.open("LOOKUP");

		gov.fnal.controls.servers.dpm.pools.Node.init();


		//long total = 0;

		//for (int ii = 0; ii < 5; ii++) {
			//final long begin = System.currentTimeMillis();
			final Lookup.DeviceInfo dInfo = Lookup.getDeviceInfo(args[1]);
			
			System.out.println(dInfo.toString());

			//System.out.println(dInfo.setting.scaling.common.enumStrings[0]);
			//System.out.println(dInfo.setting.scaling.common.enumStrings[1]);
			//System.out.println(dInfo.setting.scaling.common.enumStrings[2]);

			//for (double c : dInfo.reading.scaling.common.constants)
				//System.out.println(c);
			//System.out.println(dInfo.reading.scaling.common.constants[1]);
			//System.out.println(dInfo.reading.scaling.common.constants[2]);
			//System.out.println(dInfo.reading.scaling.common.enumStrings[1]);
			//System.out.println(dInfo.reading.scaling.common.enumStrings[2]);

			//final long end = System.currentTimeMillis();

			//logger.info("DeviceInfo: " + dInfo);
			//logger.info("time: " + (end - begin) + "ms");
			
			//total += (end - begin);
		//}

		//logger.info("total: " + total);

		System.exit(0);
	}
}
