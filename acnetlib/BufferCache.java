// $Id: BufferCache.java,v 1.2 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import gov.fnal.controls.servers.dpm.pools.Node;

abstract class Buffer
{
	static final Buffer nil = new Buffer() { void free() { } };

	final ByteBuffer buf;
	SocketAddress address;

	Buffer()
	{ 
		this.buf = ByteBuffer.allocate(0);
	}

	Buffer(ByteBuffer buf)
	{
		this.buf = buf;
	}

	final int position()
	{
		return buf.position();
	}

	final Buffer put(byte b)
	{
		buf.put(b);
		return this;
	}

	final Buffer setAddressAndFlip(SocketAddress address)
	{
		this.address = address;
		buf.flip();
		if (buf.remaining() == 65536)
			Thread.dumpStack();
		return this;
	}

	final Buffer putShort(int val)
	{
		buf.putShort((short) val);	
		return this;
	}

	final Buffer putInt(int val)
	{
		buf.putInt(val);	
		return this;
	}

	final Buffer bigEndian()
	{
		buf.order(ByteOrder.BIG_ENDIAN);
		return this;
	}

	public final Buffer littleEndian()
	{
		buf.order(ByteOrder.LITTLE_ENDIAN);
		return this;
	}

	public final int remaining()
	{
		return buf.remaining();
	}

	public final int getShort()
	{
		return buf.getShort();
	}

	public final int getUnsignedShort()
	{
		return buf.getShort() & 0xffff;
	}

	public final int peekUnsignedShort()
	{
		buf.mark();
		final int v = buf.getShort() & 0xffff;
		buf.reset();

		return v;
	}

	public final int peekInt(int position)
	{
		//buf.mark();
		final int v = buf.getInt(position);
		//buf.reset();

		return v;
	}

	public final int getInt()
	{
		return buf.getInt();
	}
	
	public final Buffer remaining(int remaining)
	{
		buf.clear();
		buf.limit(remaining);
		return this;
	}

	public final Buffer clear()
	{
		buf.clear();
		return this;
	}

	public final ByteBuffer slice()
	{
		return buf.slice();
	}

	public final byte[] array()
	{
		return buf.array();
	}

	public final Buffer receive(DatagramChannel channel) throws IOException
	{
		buf.clear();
		channel.receive(buf);
		buf.flip();

		return this;
	}

	public final void read(SocketChannel channel) throws IOException
	{ 
		channel.read(buf);
	} 

	abstract void free();
}

final class BufferCache
{
	static final int BufferSize = 64 * 1024;
    //static final Logger logger = Logger.getLogger(BufferCache.class.getName()); 

	private static class CachedBuffer extends Buffer
	{
		final int bufNo;
		boolean inUse;

		CachedBuffer(ByteBuffer buf)
		{
			super(buf);
			this.bufNo = allocatedCount;
			this.inUse = true;
		}

		@Override
		public void free()
		{
			synchronized (BufferCache.class) {
				if (inUse) {
					inUse = false;
					buf.clear();
					buf.order(ByteOrder.LITTLE_ENDIAN);
					freeList.add(this);
					//logger.log(Level.FINER, () -> "BufferCache.free[allocated:" + allocatedCount + " free:" + freeList.size() + "]");
				} else {
					logger.log(Level.WARNING, () -> "BufferCache.free[bufNo:" + bufNo + " inUse:" + inUse + "]");
					Thread.dumpStack();
				}		
			}
		}
	}

	static volatile int allocatedCount = 0;

	static final LinkedList<CachedBuffer> freeList = new LinkedList<>();

	static final synchronized Buffer alloc()
	{
		if (freeList.isEmpty()) {
			allocatedCount++;
			//logger.log(Level.FINER, () -> "BufferCache.alloc(new)[allocated:" + allocatedCount + " free:" + freeList.size() + "]");

			return new CachedBuffer(ByteBuffer.allocate(BufferSize).order(ByteOrder.LITTLE_ENDIAN));
		} else {
			final CachedBuffer buf = freeList.remove();

			//logger.log(Level.FINER, () -> "BufferCache.alloc(free)[allocated:" + allocatedCount + " free:" + freeList.size() + "]");

			buf.inUse = true;
			return buf;
		}
	}

	static final synchronized Buffer allocDirect()
	{
		if (freeList.isEmpty()) {
			allocatedCount++;
			logger.log(Level.FINER, () -> "BufferCache.alloc(new)[allocated:" + allocatedCount + " free:" + freeList.size() + "]");

			return new CachedBuffer(ByteBuffer.allocateDirect(BufferSize).order(ByteOrder.LITTLE_ENDIAN));
		} else {
			final CachedBuffer buf = freeList.remove();

			logger.log(Level.FINER, () -> "BufferCache.alloc(free)[allocated:" + allocatedCount + " free:" + freeList.size() + "]");

			buf.inUse = true;
			return buf;
		}
	}
}
