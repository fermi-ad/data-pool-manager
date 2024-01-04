// $Id: DPMReceiveData.java,v 1.3 2022/11/01 20:44:35 kingc Exp $
package gov.fnal.controls.servers.dpm;

/*
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.PlotCallback;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public interface DPMReceiveData extends ReceiveData, PlotCallback
{
	@Override
	default public void receiveData(WhatDaq whatDaq, int id, int error, int offset, byte[] data, 
								long timestamp, CollectionContext context, boolean isChanged)
	{
		logger.log(Level.WARNING, "missing receiveData()");
	}

	default public void receiveStatus(int status, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveStatus()");
	}	

	default public void receiveData(double value, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData()");
	}

	default public void receiveData(double value[], long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData()");
	}

	default public void receiveData(String value, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData()");
	}

	default public void receiveEnum(String value, int index, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveEnum()");
	}

	@Override
	default public void plotData(Object request, long timestamp,
							CollectionContext context, int error, int numberPoints,
							long[] microSecs, short[] nanoSecs, double[] values)
	{
		logger.log(Level.WARNING, "missing plotData()");
	}
}
*/
