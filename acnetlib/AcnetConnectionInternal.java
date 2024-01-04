// $Id: AcnetConnectionInternal.java,v 1.1 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.HashMap;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.net.URL;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Delayed;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import gov.fnal.controls.servers.dpm.pools.Node;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class AcnetConnectionInternal extends AcnetConnection
{
    //protected static final Logger logger = Logger.getLogger(AcnetConnection.class.getName()); 
	//protected static final ReentrantLock sendCommandLock = new ReentrantLock();

	static final IdBank reqIdBank = new IdBank(4096);

	boolean inHandler;

    protected AcnetConnectionInternal(int name, int taskId, String vNode)
	{
		super(name, taskId, vNode);

		this.inHandler = false;
		//System.out.println("TASK: '" + connectedName() + "' id:" + taskId);
    }

	@Override
	final public synchronized String getName(int node) throws AcnetStatusException 
	{
		return Node.get(node).name();
	}

	@Override
	final public synchronized int getNode(String name) throws AcnetStatusException
	{
		return Node.get(name).value();
	}

	@Override
	final public synchronized int getLocalNode() throws AcnetStatusException
	{
		return AcnetInterface.localNode().value();
	}

	@Override
    final public synchronized void send(int node, String task, ByteBuffer buf) throws AcnetStatusException 
	{
		AcnetInterface.writeThread.sendMessage(node, task, buf);

		stats.messageSent();
    }

	@Override
    final public synchronized AcnetRequestContext sendRequest(int node, String task, boolean isMult, ByteBuffer data, int timeout, AcnetReplyHandler replyHandler) throws AcnetStatusException
	{
		//System.out.println("sendRequest 1: '" + connectedName() + "'");

		synchronized (requestsOut) {
			//final int t = encode(task);

			//cmdBuf.clear();
			//cmdBuf.putInt(t).putShort((short) node).putShort((short) (multRpy ? 1 : 0)).putInt(tmo == 0 ? Integer.MAX_VALUE : tmo);
			//final int reqid = sendCommand(18, 2, dataBuf).getShort();
			//final AcnetRequestContext ctxt = new AcnetRequestContext(this, reqid, node, replyHandler);
				//System.out.println("sendRequest 5: '" + connectedName() + "'" + "node:" + node);
			AcnetRequestContext context;

			//try {
			//final AcnetRequestContext context = new AcnetRequestContext(this, task, node, new RequestId(), isMult, timeout, replyHandler);
			context = new AcnetRequestContext(this, task, node, new RequestId(reqIdBank.alloc()), isMult, timeout, replyHandler);

			//} catch (Exception e) {
			//	e.printStackTrace();
			//	throw e;
			//}
				//System.out.println("sendRequest 6: '" + connectedName() + "'");

			requestsOut.put(context.requestId(), context);
				//System.out.println("sendRequest 7: '" + connectedName() + "'");

			AcnetInterface.writeThread.sendRequest(context, data);
				//System.out.println("sendRequest 8: '" + connectedName() + "'");

			stats.requestSent();

			return context;
		}
    }

    //final public synchronized AcnetRequestContext sendRequest(int node, int task, boolean multRpy, ByteBuffer dataBuf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 

	//{
		//System.out.println("sendRequest 2: '" + connectedName() + "'");
		//throw new RuntimeException("not supported");	
    //}

	@Override
    void requestAck(ReplyId replyId)
	{
    }

	//@Override
	//final public InetAddress nodeAddress(int node) throws AcnetStatusException
	//{
	//	return null;
	//}

	//@Override
	//final public InetAddress remoteTaskAddress(int node, int taskId) throws AcnetStatusException
	//{
	//	return null;
	//}

	@Override
    final synchronized void disconnect() 
	{
		//try {
		//} catch (Exception e) { }

		//System.out.println("ACNET disconnect - " + connectedName() + " requestsIn:" + requestsIn.size());
		//logger.log(Level.FINE, "ACNET disconnect - " + connectedName());

		synchronized (requestsOut) {
			for (AcnetRequestContext context : requestsOut.values()) {
				try {
					context.cancel();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
				//context.cancelNoEx();

			requestsOut.clear();

			for (AcnetRequest request : requestsIn.values()) {
				//System.out.println("last status to request " + request);
				try {
					request.sendLastStatus(ACNET_DISCONNECTED);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			requestsIn.clear();
		}

		//AcnetInterface.connectionsByName.remove(this.name);
		//AcnetInterface.connectionsById[taskId] = null;

		//taskId = -1;
    }

	@Override
    final synchronized void ping() throws AcnetStatusException 
	{
    }

	@Override
	final synchronized void startReceiving() throws AcnetStatusException
	{
		receiving = true;
	}

	@Override
	final synchronized void stopReceiving() throws AcnetStatusException
	{
		receiving = false;
	}

	@Override
    final synchronized void sendCancel(AcnetRequestContext context) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			requestsOut.remove(context.requestId);
		}

		AcnetInterface.writeThread.sendCancel(context);
    }

	@Override
    final synchronized void sendCancel(AcnetReply reply) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			requestsOut.remove(reply.requestId);
		}

		AcnetInterface.writeThread.sendCancel(reply);
    }

	@Override
	final synchronized void ignoreRequest(AcnetRequest request) throws AcnetStatusException
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		final ReplyId replyId = request.replyId();

		synchronized (requestsIn) {
			requestsIn.remove(replyId);
			request.cancel();
		}

	}

	@Override
    final synchronized void sendReply(AcnetRequest request, int flags, ByteBuffer dataBuf, int status) throws AcnetStatusException 
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		//final short rpyid = (short) request.replyId().value();

		if (!request.multipleReply() || flags == REPLY_ENDMULT) {
			synchronized (requestsIn) {
				//System.out.println("sendReply: " + request.connection.connectedName() + " requestsIn: " + requestsIn.size());
				requestsIn.remove(request.replyId());
				//System.out.println("sendReply: " + request.connection.connectedName() + " requestsIn: " + requestsIn.size());
				request.cancel();
			}
		}

		AcnetInterface.writeThread.sendReply(request, flags, dataBuf, status);		

		stats.replySent();
    }

	@Override
	public void handle(AcnetMessage m) 
	{ 
	}

	@Override
	public void handle(AcnetRequest r) 
	{ 
		// Default handler needs to shutdown the request

		try {
			r.sendLastStatus(ACNET_ENDMULT); 
		} catch (Exception e) { }
	}

	@Override
	public void handle(AcnetCancel c) 
	{ 
	}

	@Override
	short localPort()
	{
		return 0;
	}

	@Override
    ByteBuffer sendCommand(int cmd, int ack, ByteBuffer dataBuf)
	{
		return null;
	}

	void connect()
	{
	}

	@Override
	boolean inDataHandler()
	{
		return inHandler;
	}

	void close()
	{
	}

	public boolean disposed()
	{
		return false;
	}
}
