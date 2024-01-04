// $Id: AcnetConnection.java,v 1.2 2023/12/13 17:04:49 kingc Exp $
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
import java.util.concurrent.Delayed;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import gov.fnal.controls.servers.dpm.pools.Node;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class Rad50
{
	static final char rad50Char[] = { ' ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 
										'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 
										'X', 'Y', 'Z', '$', '.', '%', '0', '1', '2', '3', '4', '5',
										'6', '7', '8', '9' };
    static final int Rad50CharCount = 6;
    
	final private static int charToIndex(char c) 
	{
		if (c >= 'A' && c <= 'Z')
			return c - 'A' + 1;
		else if (c >= 'a' && c <= 'z')
			return c - 'a' + 1;
		else if (c >= '0' && c <= '9')
			return c - '0' + 30;
		else if (c == '$')
			return 27;
		else if (c == '.')
			return 28;
		else if (c == '%')
			return 29;

		return 0;
	}
    
    public final static int encode(String s) 
	{
		int v1 = 0, v2 = 0;
		int len = s.length();

		for (int ii = 0; ii < Rad50CharCount; ii++) {
			char c = (ii < len ? s.charAt(ii) : ' ');

			if (ii < (Rad50CharCount / 2)) {
				v1 *= 40;
				v1 += charToIndex(c);
			} else {
				v2 *= 40;
				v2 += charToIndex(c);
			}
		}

		return v2 << 16 | v1;
    }

    final static String decode(int rad50) 
	{
		final char s[] = new char[Rad50CharCount];

		int v1 = rad50 & 0xffff;
		int v2 = (rad50 >> 16) & 0xffff;

		for (int ii = 0; ii < (Rad50CharCount / 2); ii++) {
			s[(Rad50CharCount / 2) - ii - 1] = rad50Char[v1 % 40];
			v1 /= 40;
			s[Rad50CharCount - ii - 1] = rad50Char[v2 % 40];
			v2 /= 40;
		}

		return new String(s);
    }
}

interface AcnetConstants {
	static final int ACNET_HEADER_SIZE = 18;

    static final int ACNET_FLG_CAN = 0x0200;
    static final int ACNET_FLG_TYPE = 0x000e;
    static final int ACNET_FLG_USM = 0x0000;
    static final int ACNET_FLG_REQ = 0x0002;
    static final int ACNET_FLG_RPY = 0x0004;
    static final int ACNET_FLG_MLT = 0x0001;

    static final int REPLY_NORMAL = 0x00;
    static final int REPLY_ENDMULT = 0x02;

    //final static int ACNET_SUCCESS = 0;
	//final static int ACNET_ENDMULT = (1 + (2 * 256));
    //final static int ACNET_NOT_CONNECTED = (1 + (-21 * 256));
    //final static int ACNET_IVM = (1 + (-23*256));
    //final static int ACNET_NO_SUCH = (1 + (-24*256));
    //final static int ACNET_NO_NODE = (1 + (-30*256));
    //final static int ACNET_NO_TASK = (1 + (-33*256));
	//final static int ACNET_DISCONNECTED = (1 + (-34 * 256));
    //final static int ACNET_SYS = (1 + (-43 * 256));

	//final static int ACNET_TCP_PORT = 6802;
	//final static String ACSYS_PROXY_HOST = "acsys-proxy.fnal.gov";
}

abstract public class AcnetConnection implements AcnetConstants, Delayed, AcnetMessageHandler, AcnetRequestHandler, AcnetErrors
{
	static final ReentrantLock sendCommandLock = new ReentrantLock();
	//static HashMap<Integer, AcnetConnection> byName = new HashMap<>();
	//static AcnetConnection[] byId = new AcnetConnection[128];

	private abstract class RPC implements AcnetReplyHandler
	{
		int status = 0;

		abstract void rpcHandle(AcnetReply reply);
		
		@Override
        final public synchronized void handle(AcnetReply reply)
        {
			rpcHandle(reply);
			notify();
		}

		final synchronized void execute(int node, String task, ByteBuffer buf, int timeout) throws AcnetStatusException
		{
			if (inDataHandler())
				throw new IllegalStateException("ACNET RPC calls are invalid during ACNET handler execution.");

			sendRequest(node, task, false, buf, timeout, this);

			try {
				wait();
				if (status != 0)
					throw new AcnetStatusException(status);
			} catch (InterruptedException e) {
				throw new AcnetStatusException(ACNET_DISCONNECTED);
			}
		}
	}

	final class ReplyThread implements Runnable
	{
		//final ByteBuffer stop = ByteBuffer.allocate(0);
		final Thread thread;
		//final BlockingQueue<ByteBuffer> queue;
		final BlockingQueue<AcnetReply> queue;

		ReplyThread()
		{
			this.queue = new LinkedBlockingQueue<>();
			this.thread = new Thread(this);
			
			this.thread.setName("ACNET - " + connectedName() + " reply thread");
			this.thread.start();
		}

		void stop()
		{
			queue.clear();
			//queue.offer(stop);
			queue.offer(AcnetReply.nil);
		}

		@Override
		public void run()
		{
			logger.log(Level.INFO, "ACNET - " + connectedName() + " reply queue thread start");

			//Buffer buf;
			AcnetReply reply;

			try {
				//ByteBuffer replyBuf;

				//while ((replyBuf = queue.take()) != stop) {
				while ((reply = queue.take()) != AcnetReply.nil) {
					//handleReply(replyBuf);
					handleReply(reply);
					reply.data = null;
					reply.buf.free();
				}

			} catch (Exception e) {
				logger.log(Level.WARNING, "ACNET reply queue thread exception", e);
			}

			logger.log(Level.INFO, "ACNET - " + connectedName() + " reply queue thread stop");
		}
	}

	final static class ConnectionMonitor extends Thread
	{
		final DelayQueue<AcnetConnection> q = new DelayQueue<AcnetConnection>();
		
		ConnectionMonitor()
		{
			setName("AcnetConnection.ConnectionMonitor");
			setDaemon(true);
			start();
		}

		final void register(AcnetConnection c)
		{
			c.setDelay(5000);
			q.add(c);
		}

		@Override
		public void run()
		{
			logger.log(Level.INFO, "ACNET connection monitor thread start");

			while (true) {
				try {
					final AcnetConnection c = q.take();
					
					try {
						if (!c.disposed()) {
							if (c.connected())
								c.ping();
							else {
								logger.log(Level.INFO, "ACNET " + c.connectedName() + " - reconnect");
								c.connect();
								if (c.receiving)
									c.startReceiving();
							}
							c.setDelay(10000);
							q.add(c);
						}
					} catch (AcnetStatusException e) {
						logger.log(Level.INFO, "ACNET " + c.connectedName() + " - " + e);
						c.disconnect();
						c.setDelay(2000);
						q.add(c);
					}
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	static {
		if (AcnetInterface.readThread == null)
			connectionMonitor = new ConnectionMonitor();
	}

	//static ConnectionMonitor connectionMonitor = new ConnectionMonitor();
	static ConnectionMonitor connectionMonitor;

    int name;
    final int pid;
	final int vNode;
    int taskId;
	long nextMonitorTime;
    final ByteBuffer cmdBuf;
    final HashMap<RequestId, AcnetRequestContext> requestsOut;
	final HashMap<ReplyId, AcnetRequest> requestsIn;

	boolean receiving;
	AcnetMessageHandler messageHandler;
	AcnetRequestHandler requestHandler;

	volatile ReplyThread replyThread = null;
	final StatSet stats;

	protected AcnetConnection(String name)
	{
		this(name, "");
	}

	protected AcnetConnection()
	{
		this("", "");
    }

    protected AcnetConnection(String name, String vNode)
	{
		this.name = Rad50.encode(name);
		this.pid = AcnetInterface.getPid();
		this.vNode = Rad50.encode(vNode);
		this.taskId = -1;
		this.cmdBuf = ByteBuffer.allocateDirect(16);
		this.requestsOut = new HashMap<>();
		this.requestsIn = new HashMap<>();

		this.receiving = false;
		this.messageHandler = this;
		this.requestHandler = this;
		this.stats = new StatSet();
    }

	protected AcnetConnection(int name, int taskId, String vNode)
	{
		this.name = name;
		this.pid = AcnetInterface.getPid();
		this.vNode = Rad50.encode(vNode);
		this.taskId = taskId;
		this.cmdBuf = null;
		this.requestsOut = new HashMap<>();
		this.requestsIn = new HashMap<>();

		this.receiving = false;
		this.messageHandler = this;
		this.requestHandler = this;
		this.stats = new StatSet();
	}

	final public AcnetConnection queueReplies(boolean state)
	{
		if (state && replyThread == null)
			replyThread = new ReplyThread();
		else if (!state && replyThread != null) {
			replyThread.stop();
			replyThread = null;
		}

		return this;
	}

	final public synchronized boolean connected()
	{
		return taskId != -1;
	}

    final public String connectedName() 
	{
		return Rad50.decode(name);
    }

    final synchronized void connect8() throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putInt(pid).putShort(localPort());
		
		final ByteBuffer ackBuf = sendCommand(1, 1, null);

		this.taskId = (int) (ackBuf.get() & 0xff);
		this.name = ackBuf.getInt();
    }

    final synchronized void connect16() throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putInt(pid).putShort(localPort());
		
		final ByteBuffer ackBuf = sendCommand(16, 16, null);

		this.taskId = (int) ackBuf.getShort() & 0xffff;
		this.name = ackBuf.getInt();
    }

	public synchronized String getName(int node) throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putShort((short) node);
		return Rad50.decode(sendCommand(12, 5, null).getInt());
	}

	public synchronized int getNode(String name) throws AcnetStatusException
	{
		cmdBuf.clear();
		cmdBuf.putInt(Rad50.encode(name));
		return sendCommand(11, 4, null).getShort() & 0xffff;
	}

	public synchronized int getLocalNode() throws AcnetStatusException
	{
		cmdBuf.clear();
		return sendCommand(13, 4, null).getShort() & 0xffff;
	}

	final public String getLocalNodeName() throws AcnetStatusException
	{
		return getName(getLocalNode());
	}

    public synchronized void send(int node, String task, ByteBuffer buf) throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putInt(Rad50.encode(task)).putShort((short) node);
		sendCommand(4, 0, buf);
    }

	final public synchronized void send(String node, String task, ByteBuffer buf) throws AcnetStatusException
	{
		send(getNode(node), task, buf);
	}

    public synchronized AcnetRequestContext sendRequest(int node, String task, boolean multRpy, ByteBuffer dataBuf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			final int t = Rad50.encode(task);

			cmdBuf.clear();
			cmdBuf.putInt(t).putShort((short) node).putShort((short) (multRpy ? 1 : 0)).putInt(tmo == 0 ? Integer.MAX_VALUE : tmo);
			final int reqid = sendCommand(18, 2, dataBuf).getShort();
			final AcnetRequestContext ctxt = new AcnetRequestContext(this, task, node, new RequestId(reqid), multRpy, tmo, replyHandler);
			requestsOut.put(ctxt.requestId(), ctxt);

			return ctxt;
		}
    }

/*
    public synchronized AcnetRequestContext sendRequest(int node, int task, boolean multRpy, ByteBuffer dataBuf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			cmdBuf.clear();
			cmdBuf.putInt(task).putShort((short) node).putShort((short) (multRpy ? 1 : 0)).putInt(tmo == 0 ? Integer.MAX_VALUE : tmo);
			final int reqid = sendCommand(18, 2, dataBuf).getShort();
			final AcnetRequestContext ctxt = new AcnetRequestContext(this, reqid, node, replyHandler);
			requestsOut.put(ctxt.requestId(), ctxt);

			return ctxt;
		}
    }
	*/

    final public AcnetRequestContext requestSingle(int node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(node, task, false, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestSingle(String node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(getNode(node), task, false, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(int node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(node, task, true, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(String node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(getNode(node), task, true, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(int node, String task, ByteBuffer buf, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(node, task, true, buf, 0, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(String node, String task, ByteBuffer buf, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(getNode(node), task, true, buf, 0, replyHandler);
    }

    final public synchronized void handleMessages(AcnetMessageHandler messageHandler) throws AcnetStatusException 
	{
		this.messageHandler = messageHandler;
		startReceiving();
    }

    final public synchronized void handleRequests(AcnetRequestHandler requestHandler) throws AcnetStatusException 
	{
		this.requestHandler = requestHandler;
		startReceiving();
    }

	final public synchronized void stopHandlingAll() throws AcnetStatusException
	{
		this.requestHandler = this;
		this.messageHandler = this;
		stopReceiving();
	}

	private class RPCint extends RPC
	{
		int intVal;

		@Override
		public void rpcHandle(AcnetReply reply)
		{
			//System.out.println("RPCint reply: " + reply);
			if (reply.status == 0) {
				if (reply.data().remaining() == 4)
					intVal = reply.data().getInt();
				else
					status = ACNET_IVM;
			} else
				status = reply.status;
		}
	}

	private class RPCshort extends RPC
	{
		int shortVal;

		@Override
		public void rpcHandle(AcnetReply reply)
		{
			if (reply.status == 0) {
				if (reply.data().remaining() == 2)
					shortVal = reply.data().getShort();
				else
					status = ACNET_IVM;
			} else
				status = reply.status;
		}
	}

	public InetAddress nodeAddress(int node) throws AcnetStatusException
	{
		final RPCint rpc = new RPCint();
		final ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		final byte subcode = (byte) (0x40 | (((node >> 8) & 0xff) - 9));

		buf.put((byte) 17).put(subcode).putShort((short) (node & 0xff)).flip();
		rpc.execute(node, "ACNET", buf, 100);
		buf.clear();

		try {
			return InetAddress.getByAddress(buf.putInt(rpc.intVal).array());
		} catch (UnknownHostException e) {
			throw new AcnetStatusException(ACNET_NO_NODE);
		}
	}

	public InetAddress remoteTaskAddress(int node, int taskId) throws AcnetStatusException
	{
		//System.out.println("remoteTaskAddres: " + Node.get(node) + " " + taskId);

		final RPCint rpc = new RPCint();
		final ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);

		buf.put((byte) 19).put((byte) 0).putShort((short) taskId).flip();
		rpc.execute(node, "ACNET", buf, 1500);
		buf.clear();

		try {
			return InetAddress.getByAddress(buf.putInt(rpc.intVal).array());
		} catch (UnknownHostException e) {
			throw new AcnetStatusException(ACNET_NO_TASK);
		}
	}

	final public int remoteTaskId(int node, String taskName) throws AcnetStatusException
	{
		final RPCshort rpc = new RPCshort();
		final ByteBuffer buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);

		buf.put((byte) 1).put((byte) 0).putInt(Rad50.encode(taskName)).flip();
		rpc.execute(node, "ACNET", buf, 100);

		return rpc.shortVal & 0xffff;
	}

	final public int remoteTaskId(String nodeName, String taskName) throws AcnetStatusException
	{
		return remoteTaskId(getNode(nodeName), taskName);
	}

	final public void dispose()
	{
		if (replyThread != null)
			replyThread.stop();

		disconnect();
		close();
	}

	//final private void handleReply(Buffer buf)
	final void handleReply(AcnetReply reply)
	{
		//final AcnetReply reply = new AcnetReply(this, buf);
		final RequestId requestId = reply.requestId();
		final AcnetRequestContext context;

		synchronized (requestsOut) {
			context = requestsOut.get(requestId);
		}

		if (context != null) {
			try {
				//System.out.println("PASSING TO REPLY HANDLER");
				context.replyHandler.handle(reply);
			} catch (Exception e) {
				logger.log(Level.WARNING, "ACNET reply callback exception for connection '" + connectedName() + "'", e);
			} //finally {
				//reply.release();
			//}

			if (reply.last()) {
				synchronized (requestsOut) {
					requestsOut.remove(requestId);
				}
				context.localCancel();
			}
		} else
			sendCancelNoEx(reply);

		//reply.data = null;
		//reply.buf.free();
	}

	//final void handleAcnetPacket(ByteBuffer buf)
	//final void handleAcnetPacket(Buffer buf)
	//{
		//buf.mark();
		//buf.mark();
	    //final int flags = buf.buf().getShort() & 0xffff;
	    //final int flags = buf.getUnsignedShort();
		//buf.reset();

/*
		final int flags = buf.peekUnsignedShort();
		
		switch (flags & (ACNET_FLG_TYPE | ACNET_FLG_CAN)) {
			case ACNET_FLG_RPY:  
				{
					System.out.println("RECEIVED A REPLY");
					if (replyThread != null) {
						System.out.println("PUT ON REPLY QUEUE");
						replyThread.queue.offer(buf);
					} else {
						handleReply(buf);
						buf.free();
					}
				}
				break;

			case ACNET_FLG_USM: 
				{
					System.out.println("RECEIVED A USM");
					final AcnetMessage message = new AcnetMessage(this, buf);

					try {
						messageHandler.handle(message);
					} catch (Exception e) {
						logger.log(Level.WARNING, "ACNET message callback exception for connection '" + connectedName() + "'", e);
					} finally {
						message.data = null;
						buf.free();
					}
				}
				break;

			case ACNET_FLG_REQ: 	
				{
					System.out.println("RECEIVED A REQUEST");
					final AcnetRequest request = new AcnetRequest(this, buf);
					final ReplyId replyId = request.replyId();

					try {	
						requestAck(replyId);

						synchronized (requestsIn) {
							requestsIn.put(replyId, request);
						}
						try {
							requestHandler.handle(request);
						} catch (Exception e) {
							logger.log(Level.WARNING, "ACNET request callback exception for connection '" + connectedName() + "'", e);
						}
					} catch (Exception e) {
						logger.log(Level.FINER, "ACNET request ack exception for connection '" + connectedName() + "'", e);
					} finally {
						request.data = null;
						buf.free();
					}
				}
				break;

			case ACNET_FLG_CAN:
				{
					System.out.println("RECEIVED A CANCEL");
					final AcnetCancel cancel = new AcnetCancel(this, buf);
					final AcnetRequest request;
					
					synchronized (requestsIn) {
						if ((request = requestsIn.remove(cancel.replyId())) != null)
							request.cancel();
					}

					try {
						requestHandler.handle(cancel);
					} catch (Exception e) {
						logger.log(Level.WARNING, "ACNET cancel callback exception for connection '" + connectedName() + "'", e);
					} finally {
						cancel.data = null;
						buf.free();
					}
				}
				break;

			default:
				buf.free();
				break;
		}
		*/
    //}

	public static void safeThreadInterrupt(Thread thread)
	{
		try {
			sendCommandLock.lock();
			thread.interrupt();
		} finally {
			sendCommandLock.unlock();
		}
	}

    synchronized void disconnect() 
	{
		try {
			cmdBuf.clear();
			sendCommand(3, 0, null);
		} catch (Exception e) { }

		synchronized (requestsOut) {
			for (AcnetRequestContext ctxt : requestsOut.values())
				ctxt.sendEndMult();

			requestsOut.clear();
		}

		taskId = -1;
    }

    synchronized void ping() throws AcnetStatusException 
	{
		cmdBuf.clear();
		sendCommand(0, 0, null);
    }

	synchronized void startReceiving() throws AcnetStatusException
	{
		receiving = true;
		cmdBuf.clear();
		sendCommand(6, 0, null);
	}

	synchronized void stopReceiving() throws AcnetStatusException
	{
		receiving = false;
		cmdBuf.clear();
		sendCommand(20, 0, null);
	}

    synchronized void requestAck(ReplyId replyId) throws AcnetStatusException
	{
		cmdBuf.clear();
		cmdBuf.putShort((short) replyId.value());
		sendCommand(9, 0, null);
    }

    synchronized void sendCancel(AcnetRequestContext context) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			requestsOut.remove(context.requestId);
		}

		cmdBuf.clear();
		cmdBuf.putShort((short) context.requestId.id);
		sendCommand(8, 0, null);
    }

	final void sendCancelNoEx(AcnetRequestContext context)
	{
		try {
			sendCancel(context);
		} catch (Exception ignore) { }
	}

	void sendCancel(AcnetReply reply) throws AcnetStatusException
	{
		synchronized (requestsOut) {
			requestsOut.remove(reply.requestId);
		}

		cmdBuf.clear();
		cmdBuf.putShort((short) reply.requestId.id);
		sendCommand(8, 0, null);
	}

	final void sendCancelNoEx(AcnetReply reply)
	{
		try {
			sendCancel(reply);
		} catch (Exception ignore) { }
	}

	synchronized void ignoreRequest(AcnetRequest request) throws AcnetStatusException
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		final ReplyId replyId = request.replyId();

		synchronized (requestsIn) {
			requestsIn.remove(replyId);
			request.cancel();
		}

		cmdBuf.clear();
		cmdBuf.putShort((short) replyId.value());
		sendCommand(19, 0, null);
	}

    synchronized void sendReply(AcnetRequest request, int flags, ByteBuffer dataBuf, int status) throws AcnetStatusException 
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		final short rpyid = (short) request.replyId().value();

		if (!request.multipleReply() || flags == REPLY_ENDMULT) {
			synchronized (requestsIn) {
				requestsIn.remove(request.replyId());
				request.cancel();
			}
		}

		cmdBuf.clear();
		cmdBuf.putShort(rpyid).putShort((short) flags).putShort((short) status);
		sendCommand(7, 3, dataBuf);
    }

	final synchronized void setDelay(long delay)
	{
		nextMonitorTime = System.currentTimeMillis() + delay;
	}

	final public synchronized long getDelay(TimeUnit unit)
	{
		return unit.convert(nextMonitorTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
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
	final public synchronized int compareTo(Delayed o)
	{
		if (this.nextMonitorTime < ((AcnetConnection) o).nextMonitorTime)
			return -1;
		else if (this.nextMonitorTime > ((AcnetConnection) o).nextMonitorTime) 
			return 1;

		return 0;
	}


	public static boolean isLocalAddress(InetAddress address)
	{
    	// Check if the address is a valid special local or loop back

    	if (address.isAnyLocalAddress() || address.isLoopbackAddress())
        	return true;

    	// Check if the address is defined on any interface

    	try {
        	return NetworkInterface.getByInetAddress(address) != null;
    	} catch (Exception e) {
        	return false;
    	}
	}

	public static AcnetConnection open()
	{
		return open("", "");
	}

	public static AcnetConnection open(String name)
	{
		return open(name, "");
	}

	public static AcnetConnection open(String name, String vNode)
	{
		return new AcnetConnectionUDP(name, vNode);
	}

/*
	public static AcnetConnection open(InetAddress address)
	{
		return open(address, "", "");
	}
	
	public static AcnetConnection open(InetAddress address, String name)
	{
		return open(address, "", "");
	}
*/
/*
	public static AcnetConnection open(InetAddress address, String name, String vNode)
	{
		return open(new InetSocketAddress(address, ACNET_TCP_PORT), name, vNode);
	}
	*/

/*
	public static AcnetConnection openProxy() throws UnknownHostException
	{
		return openProxy("", "");
	}

	public static AcnetConnection openProxy(String name) throws UnknownHostException
	{
		return openProxy(name, "");
	}

	public static AcnetConnection openProxy(String name, String vNode) throws UnknownHostException
	{
		return open(new InetSocketAddress(ACSYS_PROXY_HOST, ACNET_TCP_PORT), name, vNode);
	}

	public static AcnetConnection openProxy(URL url, String name, String vNode)
	{
		return open(new InetSocketAddress(url.getHost(), url.getPort()), name, vNode);
	}
	*/

/*
	public static AcnetConnection open(InetSocketAddress address, String name, String vNode)
	{
		if (isLocalAddress(address.getAddress()))
			return open(name, vNode);

		return new AcnetConnectionTCP(address, name, vNode);
	}
	*/

	abstract short localPort();
    abstract ByteBuffer sendCommand(int cmd, int ack, ByteBuffer dataBuf) throws AcnetStatusException;
	abstract void connect() throws AcnetStatusException;
	abstract boolean inDataHandler();
	abstract void close();
	abstract public boolean disposed();
}
