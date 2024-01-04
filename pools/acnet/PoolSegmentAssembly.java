// $Id: PoolSegmentAssembly.java,v 1.3 2023/10/04 19:40:29 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.BitSet;
import java.util.ArrayList;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

class PoolSegmentAssembly implements ReceiveData
{
	class ReceiveSegment implements ReceiveData
	{
		final WhatDaq whatDaq;

		ReceiveSegment(int length, int offset, int id)
		{
			this.whatDaq = new WhatDaq(bigWhatDaq, length, offset, id, this);
		}

		@Override
		public void receiveData(ByteBuffer segment, long timestamp, long cycle)
		{
		}

		@Override
		public void receiveData(int error, int offset, byte[] segment, long timestamp)
		{
			if (whatDaq.isMarkedForDelete() || receiveError(error))
				return;

			System.arraycopy(segment, offset, data, whatDaq.offset(), whatDaq.length());

			segmentsReceived.set(whatDaq.id());

			if (segmentsReceived.cardinality() == segments.size()) {
				segmentsReceived.clear();
				bigWhatDaq.getReceiveData().receiveData(error, 0, data, timestamp);
				bigWhatDaq.setError(0);
			}
		}
	}

	private final WhatDaq bigWhatDaq;
	private BitSet segmentsReceived;
	private byte data[];
	private final ArrayList<ReceiveSegment> segments;

	private PoolSegmentAssembly(WhatDaq bigWhatDaq, int segmentSize, boolean isRepetitive)
	{
		this.bigWhatDaq = bigWhatDaq;
		this.segments = new ArrayList<>();
		this.data = new byte[bigWhatDaq.length()];

		int id = 0;
		int offset = bigWhatDaq.getOffset();

		for (; id < (bigWhatDaq.length() / segmentSize); id++, offset += segmentSize)
			this.segments.add(new ReceiveSegment(segmentSize, offset, id));

		final int lastSegmentSize = bigWhatDaq.length() % segmentSize; 

		if (lastSegmentSize > 0)
			this.segments.add(new ReceiveSegment(lastSegmentSize, offset + segmentSize, id));

		this.segmentsReceived = new BitSet(this.segments.size());
	}

	public static void insert(WhatDaq whatDaq, DaqPool pool, int segmentSize, boolean isRepetitive)
	{
		final PoolSegmentAssembly p = new PoolSegmentAssembly(whatDaq, segmentSize, isRepetitive);

		for (ReceiveSegment r : p.segments)
			pool.insert(r.whatDaq);
	}

	@Override
	public String toString()
	{
		return "PoolSegmentAssembly: " + bigWhatDaq;
	}

	private final boolean receiveError(int error)
	{
		if (error != 0) {
			bigWhatDaq.setError(error);

			if (!bigWhatDaq.isRepetitive()) {
				for (ReceiveSegment r : segments)
					r.whatDaq.setMarkedForDelete();

				bigWhatDaq.getReceiveData().receiveData(error, 0, null, System.currentTimeMillis());
				return true;
			}
		}

		return false;
	}
}
