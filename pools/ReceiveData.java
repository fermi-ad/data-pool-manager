// $Id: ReceiveData.java,v 1.4 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.nio.ByteBuffer;
import java.util.logging.Level;

//import gov.fnal.controls.servers.dpm.CollectionContext;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public interface ReceiveData
{
	default public void receiveData(ByteBuffer data, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData(ByteBuffer) for " + getClass());
		Thread.dumpStack();
	}

	default public void receiveData(byte[] data, int offset, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() cycle for " + getClass());
	}

	//default public void receiveData(WhatDaq whatDaq, int error, int offset, byte[] data, 
	//									long timestamp, CollectionContext context)
	//{
	//	logger.log(Level.WARNING, "missing receiveData() context");
	//}
	default public void receiveData(int error, int offset, byte[] data, long timestamp)
	{
		logger.log(Level.WARNING, "missing receiveData() context for " + getClass());
	}

	default public void receiveStatus(int status, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveStatus() for " + getClass());
		Thread.dumpStack();
	}	

	default public void receiveStatus(int status)
	{
		receiveStatus(status, System.currentTimeMillis(), 0);
	}

	default public void receiveData(double value, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() for " + getClass());
	}

	default public void receiveData(double value[], long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() for " + getClass());
	}

	default public void receiveData(String value, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() for " + getClass());
	}

	default public void receiveEnum(String value, int index, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveEnum() for " + getClass());
	}

	//default public void plotData(Object request, long timestamp, CollectionContext context, 
	//								int error, int numberPoints, long[] microSecs, short[] nanoSecs, double[] values)
	default public void plotData(long timestamp, int error, int numberPoints, long[] microSecs, int[] nanoSecs, double[] values)
	{
		logger.log(Level.WARNING, "missing plotData() for " + getClass());
	}
}
