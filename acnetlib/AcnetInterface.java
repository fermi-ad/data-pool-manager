// $Id: AcnetInterface.java,v 1.1 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.Enumeration;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.LinkedList;
import java.util.Random;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.lang.management.ManagementFactory;

import gov.fnal.controls.servers.dpm.pools.Node;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class IdBank implements AcnetErrors
{
	final static Random random = new Random();

	final int bankBegin, bankEnd;
	final LinkedList<Integer> freeIds;

	IdBank(int size)
	{
		this.bankBegin = random.nextInt(0xffff) & 0xf000;
		this.bankEnd = this.bankBegin + size;
		this.freeIds = new LinkedList<Integer>();

		for (int ii = bankBegin; ii < bankEnd; ii++)
			freeIds.add(ii);
	}

	synchronized int alloc() throws AcnetStatusException
	{
		if (freeIds.size() == 0)
			throw new AcnetStatusException(ACNET_NOLCLMEM);
			
		return freeIds.removeFirst();
	}
	
	synchronized void free(int id)
	{
		if (id >= bankBegin && id < bankEnd)
			freeIds.addLast(id);
		else
			logger.log(Level.WARNING, "free wrong id " + id);
	}
}


/*
public class AcnetTest
{
	public static void main(String[] args) throws Exception
	{
		ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer buf2 = ByteBuffer.allocate(64 * 1024);

		while (true) {
			buf.clear();
			SocketAddress from = c.receive(buf);

			buf.flip();
			//System.out.println("received " + buf.remaining() + " bytes");

			{
				byte[] s = buf.array();
				byte[] d = buf2.array();

				for (int ii = 0; ii < buf.remaining(); ii += 2) {
					d[ii] = s[ii + 1];
					d[ii + 1] = s[ii];
				}
				
				buf2.order(ByteOrder.LITTLE_ENDIAN).clear().limit(buf.remaining());
			}

       		int flags = buf2.getShort() & 0xffff;
			int status = buf2.getShort();
			int server = buf2.order(ByteOrder.BIG_ENDIAN).getShort() & 0xffff;
			int client = buf2.getShort() & 0xffff;
			int serverTask = buf2.order(ByteOrder.LITTLE_ENDIAN).getInt();
			String serverTaskName = decode(serverTask);
			int clientTaskId = buf2.getShort() & 0xffff;
			int id = buf2.getShort() & 0xffff;
			int length = buf2.getShort() & 0xffff;

			System.out.printf("srv:%04x clnt:%04x task:'%s' len: %04x\n", server, client, serverTaskName, length);
			while (buf2.hasRemaining()) {
				int x = buf2.get() & 0xff;
				System.out.printf("%02x %c\n", x, x);
			}

		}
	}
}
*/

class StatSet
{
	private int stats[] = new int[6];

	StatSet()
	{
		clear();
	}

	synchronized void clear()
	{
		for (int ii = 0; ii < stats.length; ii++)
			stats[ii] = 0;
	}

	synchronized void messageSent()
	{
		++stats[0];
	}

	synchronized void requestSent()
	{
		++stats[1];
	}

	synchronized void replySent()
	{
		++stats[2];
	}

	synchronized void messageReceived()
	{
		++stats[3];
	}

	synchronized void requestReceived()
	{
		++stats[4];
	}

	synchronized void replyReceived()
	{
		++stats[5];
	}

	synchronized ByteBuffer putStats(ByteBuffer buf)
	{
		for (int val : stats)
			buf.putShort((short) (val > 0xffff ? 0xffff : val));

		return buf;
	}
}

public class AcnetInterface implements AcnetConstants, AcnetErrors
{
	static class AcnetAuxHandler implements AcnetRequestHandler, AcnetErrors
	{
		final ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

		synchronized void putTime(long timeBase, ByteBuffer buf)
		{
			final long t = System.currentTimeMillis() - timeBase;
			
			buf.putShort((short) t);
			buf.putShort((short) (t >> 16));
			buf.putShort((short) (t >> 32));
		}

		void taskHandler(int subType, AcnetRequest r)
		{
			switch (subType) {
			 case 0:
				buf.putShort((short) connectionsByName.size());
				for (AcnetConnection c : connectionsById)
						buf.putInt(c.name);
				for (AcnetConnection c : connectionsById)
						buf.put((byte) c.taskId);
				break;

			 case 1:
			 	short count = 0;
				for (AcnetConnection c : connectionsById) {
					if (c.receiving)
						count++;	
				}
				buf.putShort(count);
				for (AcnetConnection c : connectionsById) {
					if (c.receiving)
						buf.putInt(c.name);
				}
				for (AcnetConnection c : connectionsById) {
					if (c.receiving)
						buf.put((byte) c.taskId);
				}
				break;

			 case 2:
				buf.putShort((short) connectionsByName.size());
				for (AcnetConnection c : connectionsById)
					buf.putInt(0);
				for (AcnetConnection c : connectionsById)
					buf.put((byte) c.taskId);
				break;
			 
			 case 3:
				break;
			}

			buf.flip();
			r.sendLastReplyNoEx(buf, 0);
		}

		@Override
		public void handle(AcnetRequest r)
		{
			try {
				final ByteBuffer data = r.data().order(ByteOrder.LITTLE_ENDIAN);

				final int type = data.get() & 0xff;
				final int subType = data.get() & 0xff;

				//System.out.println("ACNETAUX TYPE:" + type + " SUBTYPE:" + subType);

				buf.clear();

				switch (type) {
				 case 0:
					buf.putShort((short) 0).flip();
					r.sendLastReply(buf, ACNET_SUCCESS);
					break;

				 case 1:
					{
						final AcnetConnection c = connectionsByName.get(data.getInt());

						if (c != null) {
							buf.putShort((short) c.taskId).flip();
							r.sendLastReply(buf, ACNET_SUCCESS);
						} else
							r.sendLastStatus(ACNET_NO_TASK);
					}
					break;

				 case 2:
					{
						if (subType < connectionsById.size()) {
							final AcnetConnection c = connectionsById.get(subType);

							if (c != null) {
								buf.putInt(c.name).flip();
								r.sendLastReply(buf, ACNET_SUCCESS);
							} else
								r.sendLastStatus(ACNET_NO_TASK);
						} else
							r.sendLastStatus(ACNET_LEVEL2);
					}
					break;
				 
				 case 3:
					buf.putShort((short) 0x0100);
					buf.putShort((short) 0x0100);
					buf.putShort((short) 0x0100).flip();
					r.sendLastReply(buf, ACNET_SUCCESS);
					break;

				 case 4:
					taskHandler(subType, r);
					break;

				 case 6:
				 	{
						putTime(nodeStatsTime, buf);
						buf.putLong(0);
						stats.putStats(buf).flip();
						r.sendLastReply(buf, ACNET_SUCCESS);

						if (subType != 0) {
							nodeStatsTime = System.currentTimeMillis();
							stats.clear();
						}
					}
				 	break;

				 case 7:
				 	{
						putTime(connectionStatsTime, buf);
						buf.putShort((short) (0x900 + connectionsById.size()));

						for (AcnetConnection c : connectionsById) {
							buf.putShort((short) c.taskId);
							buf.putInt(c.name);
							c.stats.putStats(buf);

							if ((subType & 1) > 0) {
								connectionStatsTime = System.currentTimeMillis();
								c.stats.clear();
							}
						}
						buf.flip();
						r.sendLastReply(buf, ACNET_SUCCESS);

					}
				 	break;

				 default:
					r.sendLastStatus(ACNET_LEVEL2);
					break;
				}
			} catch (Exception e) {
				r.sendLastStatusNoEx(ACNET_LEVEL2);
			}
		}

		@Override
		public void handle(AcnetCancel c)
		{
		}
	}

	static class ReadThread extends Thread
	{
		final DatagramChannel channel;

		ReadThread(DatagramChannel channel)
		{
			this.channel = channel;

			setName("ACNET read thread");
			start();
		}

		@Override
		public void run()
		{
			final ByteBuffer rcvBuf = ByteBuffer.allocate(64 * 1024);

			try {
				while (true) {
					final SocketAddress from = channel.receive(rcvBuf);

					rcvBuf.flip();
					//System.out.println("RECEIVED " + rcvBuf.remaining() + " BYTES  from " + from);

					if (rcvBuf.remaining() >= 18) {
						final Buffer buf = BufferCache.alloc();
						final byte[] s = rcvBuf.array();
						final byte[] d = buf.array();

						for (int ii = 0; ii < buf.remaining(); ii += 2) {
							d[ii] = s[ii + 1];
							d[ii + 1] = s[ii];
						}
						buf.remaining(rcvBuf.remaining()).littleEndian();

						final AcnetPacket packet = AcnetPacket.create(buf);
						//final int task = buf.remaining(rcvBuf.remaining()).littleEndian().peekInt(8);

						//System.out.println("RECEIVE FROM TASK: " + packet.type() + " " + packet.serverTaskName() + " " + packet.clientTaskId());

						if (packet.isReply() && packet.clientTaskId() < connectionsById.size()) {
							final AcnetConnectionInternal connection = connectionsById.get(packet.clientTaskId());

							if (connection != null) {
								//System.out.println("FOUND TASK: " + connection.connectedName() + " " + connection.taskId);
								if (!packet.isMulticast() || packet.status() == 0) {
									connection.inHandler = true;
									packet.handle(connection);
									connection.inHandler = false;
								} else
									buf.free();
							} else
								buf.free();
						} else {
							final AcnetConnectionInternal connection = connectionsByName.get(packet.serverTask());

							if (connection != null && connection.receiving) {
								//System.out.println("FOUND TASK: " + packet.serverTaskName());
								if (!packet.isMulticast() || packet.status() == 0) {
									connection.inHandler = true;
									packet.handle(connection);
									connection.inHandler = false;
								} else
									buf.free();
							} else if (packet.isRequest()) {
								writeThread.sendReply((AcnetRequest) packet, REPLY_ENDMULT, null, ACNET_NO_TASK);
								buf.free();
							} else
								buf.free();
						}

						//buf.free();
					}

					rcvBuf.clear();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static class WriteThread extends Thread implements AcnetErrors
	{
		final DatagramChannel channel;
		final private LinkedBlockingQueue<Buffer> queue;
		//final ByteBuffer buf;
		final Node localNode;

		WriteThread(DatagramChannel channel) throws AcnetStatusException
		{
			this.channel = channel;
			this.queue = new LinkedBlockingQueue<>();
			//this.buf = ByteBuffer.allocate(64 * 1024);
			this.localNode = Node.get(AcnetInterface.vNode);

			setName("ACNET write thread");
			start();
		}

		@Override
		public void run()
		{
			while (true) {
				Buffer buf = null;

				try {
					while ((buf = queue.take()) != Buffer.nil) {
						//System.out.println("WRITE THREAD TAKE");
						//buf = queue.take();
						//System.out.println("WRITE TAKE RETURN");

						//System.out.println("SENDING " + buf.remaining() + " BYTES to " + buf.address);

						channel.send(buf.buf, buf.address);

						buf.address = null;
						buf.free();
					}

					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (buf != null) {
					buf.address = null;
					buf.free();
				}
			}
		}

		void sendRequest(AcnetRequestContext context, ByteBuffer data) throws AcnetStatusException
		{
			//System.out.println("queue: " + queue.remainingCapacity());
			//context.data = data;

			final Buffer buf = BufferCache.alloc();

			//System.out.println("SENDING REQUEST mult:" + context.isMult + " " + context.node + " " + context.task + " " + context.taskId() + " " + context.node.address());

			buf.bigEndian().putShort(ACNET_FLG_REQ | (context.isMult ? ACNET_FLG_MLT : 0));
			buf.putShort(0);
			buf.littleEndian().putShort(context.node.value());
			buf.putShort(localNode.value());

			final int task = Rad50.encode(context.task);

			buf.bigEndian().putShort((short) task);
			buf.putShort((short) (task >> 16));
			buf.putShort(context.taskId());
			buf.putShort(context.requestId.value());
			buf.putShort(18 + data.remaining());

			final byte[] src = data.array();

			//System.out.println("REQ LEN: " + data.remaining());

			for (int ii = 0; ii < data.remaining(); ii += 2) { //, pos += 2) {
				//d[pos] = s[ii + 1];
				//d[pos + 1] = s[ii];
				//System.out.println("data[" + ii + "] " + ((char) data[ii]));
				//System.out.println("data[" + (ii + 1) + "] " + ((char) data[ii + 1]));
				buf.put(src[ii + 1]).put(src[ii]);
			}

			stats.requestSent();

			queue.offer(buf.setAddressAndFlip(context.node.address()));
			//System.out.println("queue: " + queue.size());
		}

		void sendMessage(int nodeValue, String taskName, ByteBuffer data) throws AcnetStatusException
		{
			final Node node = Node.get(nodeValue); 
			final Buffer buf = BufferCache.alloc();

			buf.bigEndian().putShort(ACNET_FLG_USM);
			buf.putShort(0);
			buf.littleEndian().putShort(node.value());
			buf.putShort(localNode.value());

			final int task = Rad50.encode(taskName);

			buf.bigEndian().putShort(task);
			buf.putShort((short) (task >> 16));
			buf.putShort(0);
			buf.putShort(0);
			buf.putShort(18 + data.remaining());

			final byte[] src = data.array();

			for (int ii = 0; ii < data.remaining(); ii += 2)
				buf.put(src[ii + 1]).put(src[ii]);

			stats.messageSent();

			queue.offer(buf.setAddressAndFlip(node.address()));
		}

    	void sendReply(AcnetRequest request, int flags, ByteBuffer data, int status) throws AcnetStatusException 
		{
			final Node node = Node.get(request.client); 
			//System.out.println("SENDING REPLY " + node + " " + request.serverTaskName() + " " + request.clientTaskId + " " + request.client + " " + node.address());
			final Buffer buf = BufferCache.alloc();
			final boolean lastReply = (flags & REPLY_ENDMULT) > 0;

			buf.bigEndian().putShort(ACNET_FLG_RPY | ((request.multipleReply() && !lastReply) ? ACNET_FLG_MLT : 0));

			//System.out.printf("isMult: %s status:0x%04x\n", request.multipleReply(), status);
			//System.out.printf("flags: 0x%04x\n", flags);

			if (lastReply) {
				if (request.multipleReply()) {
					if (status != 0)
						buf.putShort(status);
					else
						buf.putShort(ACNET_ENDMULT);
				} else
					buf.putShort(status);
			} else {
				//System.out.println("putSHort(STAUTUS)");
				buf.putShort(status);
			}

			buf.littleEndian().putShort(localNode.value());
			buf.littleEndian().putShort(request.client);

			final int task = request.serverTask;

			buf.bigEndian().putShort(task);
			buf.putShort(task >> 16);
			buf.putShort(request.clientTaskId);
			buf.putShort(request.id);

			if (data != null) {
				buf.putShort(18 + data.remaining());

				final byte[] src = data.array();

				for (int ii = 0; ii < data.remaining(); ii += 2) {
					//d[pos] = s[ii + 1];
					//d[pos + 1] = s[ii];
					//System.out.println("data[" + ii + "] " + ((char) src[ii]));
					//System.out.println("data[" + (ii + 1) + "] " + ((char) src[ii + 1]));
					buf.put(src[ii + 1]).put(src[ii]);
				}
			} else
				buf.putShort(18);

			stats.replySent();

			queue.offer(buf.setAddressAndFlip(node.address()));
		}

    	void sendCancel(AcnetRequestContext context) throws AcnetStatusException 
		{
			//System.out.println("SENDING CANCEL " + context.node + " " + context.task);
			final Buffer buf = BufferCache.alloc();

			buf.bigEndian().putShort(ACNET_FLG_CAN);
			buf.putShort(0);
			buf.littleEndian().putShort(context.node.value());
			buf.putShort(localNode.value());

			final int task = Rad50.encode(context.task);

			buf.bigEndian().putShort((short) task & 0xffff);
			buf.putShort((short) (task >> 16));
			buf.putShort(context.taskId);
			buf.putShort(context.requestId.id);
			buf.putShort(18);

			queue.offer(buf.setAddressAndFlip(context.node.address()));
		}

		void sendCancel(AcnetReply reply) throws AcnetStatusException
		{
			final Node node = Node.get(reply.server);
			final Buffer buf = BufferCache.alloc();
			//System.out.println("SENDING CANCEL " + node + " " + Rad50.decode(reply.serverTask));

			buf.bigEndian().putShort(ACNET_FLG_CAN);
			buf.putShort(0);
			buf.littleEndian().putShort(node.value());
			buf.putShort(localNode.value());

			int task = reply.serverTask;

			buf.bigEndian().putShort((short) task & 0xffff);
			buf.putShort((short) (task >> 16));
			buf.putShort(reply.clientTaskId);
			buf.putShort(reply.requestId.id);
			buf.putShort(18);

			queue.offer(buf.setAddressAndFlip(node.address()));
		}
	}

	static final HashMap<Integer, AcnetConnectionInternal> connectionsByName = new HashMap<>();
	static final ArrayList<AcnetConnectionInternal> connectionsById = new ArrayList<>();
	static final InetAddress localhost;
	static final String vNode;

	static long nodeStatsTime, connectionStatsTime;
	static final StatSet stats = new StatSet(); 

	static ReadThread readThread;
	static WriteThread writeThread;

	static {
		vNode = System.getProperty("vnode", "");

		try {
			localhost = InetAddress.getByName(System.getProperty("host", "localhost"));
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

		try {
			final DatagramChannel channel = DatagramChannel.open().bind(new InetSocketAddress(6801));

			try {
				channel.configureBlocking(true);
				channel.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);

				for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
					final NetworkInterface ni = e.nextElement();

					if (!ni.isLoopback())
						channel.join(InetAddress.getByName("239.128.4.1"), ni);
				}

				nodeStatsTime = connectionStatsTime = System.currentTimeMillis();

				Node.init();

				readThread = new ReadThread(channel);
				writeThread = new WriteThread(channel);

				//for (int ii = 0; ii < connectionsById.size(); ii++)
				//	connectionsById[ii] = null;

				final AcnetConnection acnet = open("ACNET");

				acnet.handleRequests(new AcnetAuxHandler());

				logger.log(Level.CONFIG, "Using internal ACNET");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (Exception e) {
			readThread = null;
			writeThread = null;

			logger.log(Level.CONFIG, "Using the ACNET daemon");
		}
	}

	final static int getPid()
	{
		try {
			return Integer.valueOf(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		} catch (Exception e) {
			return 0;
		}
	}

	public static final int allocatedBufferCount()
	{
		return BufferCache.allocatedCount;
	}

	public static final int freeBufferCount()
	{
		return BufferCache.freeList.size();
	}

	final public static void close()
	{
		if (readThread != null) {
			for (AcnetConnection connection : connectionsByName.values()) {
				//System.out.println("connectio disconnect " + connection.connectedName());
				connection.disconnect();
			}

			writeThread.queue.offer(Buffer.nil);

			try {
				writeThread.join();
				Thread.sleep(1500);
			} catch (Exception ignore) { }
		}
	}

	//final public static AcnetConnection open()
	//{
	//	return open("");
	//}

/*
	final static private int getFreeTaskId() throws AcnetStatusException
	{
		for (int ii = 0; ii < connectionsById.length; ii++) {
			if (connectionsById[ii] == null)
				return ii;
		}

		throw new AcnetStatusException(ACNET_NOLCLMEM);
	}
*/

	final synchronized public static AcnetConnection open(String name)// throws AcnetStatusException
	{
		if (readThread == null)
			//return AcnetConnection.open(localhost, name, vNode);
			return AcnetConnection.open(name, vNode);
		else {
			final int encodedName = Rad50.encode(name);

			AcnetConnectionInternal connection = connectionsByName.get(encodedName);

			if (connection == null) {
				//final int taskId = getFreeTaskId();

				connection = new AcnetConnectionInternal(encodedName, connectionsById.size(), vNode);
				connectionsByName.put(encodedName, connection);
				connectionsById.add(connection);
			}

			return connection;
		}
	}

	final public static String host()
	{
		return localhost.getHostName();
	}

	final public static InetAddress localhost()
	{
		return localhost;
	}

	final public static String vNode()
	{
		return vNode;
	}

	final public static Node localNode() throws AcnetStatusException
	{
		return Node.get(vNode);
	}
}
