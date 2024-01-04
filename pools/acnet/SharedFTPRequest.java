// $Id: SharedFTPRequest.java,v 1.5 2023/11/02 17:01:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Enumeration;
import java.util.Vector;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventObserver;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

class SharedFTPRequest extends FTPRequest implements ReceiveData, DataEventObserver
{
	/**
	 * @serial the user requests
	 */
	private Vector<FTPRequest> ftpUsers;

	/**
	 * @serial should never get above
	 */
	private final static int QUEUE_LEN = 100;

	/**
	 * @serial saved time-stamps
	 */
	private long[] queuedMicroSecs = new long[QUEUE_LEN];

	/**
	 * @serial saved time-stamps
	 */
	private int[] queuedNanoSecs = new int[QUEUE_LEN];

	/**
	 * @serial saved values
	 */
	private double[] queuedValues = new double[QUEUE_LEN];

	/**
	 * @serial saved error
	 */
	private int queuedError = 0;

	/**
	 * @serial number queued
	 */
	private int numQueued = 0;

	private transient int numQueueOverflow = 0;

	private transient boolean sentMail = false;

	/**
	 * @serial 15 hz event
	 */
	private DataEvent sendPlotDataEvent = new DeltaTimeEvent(67 * FTPPool.FTP_RETURN_PERIOD, false);

	SharedFTPRequest(WhatDaq sharedDevice, ClassCode classCode, FTPScope sharedScope)
	{
		super((WhatDaq) sharedDevice.clone(), (ClassCode) classCode,
				(FTPScope) sharedScope.clone(), null);
		callback = this;
		ftpUsers = new Vector<FTPRequest>(5);
	}
	
	void addUser(FTPRequest user)
	{
		ftpUsers.add(user);
	}

	Vector<FTPRequest> getUsers() 
	{ 
		return ftpUsers;
	}

	@Override
	public void plotData(long timestamp, int error, int numberPoints,
							long[] microSecs, int[] nanoSecs, double[] values)
	{
		FTPRequest userFTPRequest;

		int count = ftpUsers.size();
		for (int ii = 0; ii < count; ii++) {
			if (ii < ftpUsers.size()) {
				userFTPRequest = ftpUsers.elementAt(ii);
				if (userFTPRequest.delete)
					continue;
				if (userFTPRequest.callback != null) {
					userFTPRequest.callback.plotData(timestamp, error, numberPoints, microSecs, nanoSecs, values);
				}
			}
		}
	}

	void deliverError(int error)
	{
		Enumeration<FTPRequest> users;
		FTPRequest user;

		users = ftpUsers.elements();
		while (users.hasMoreElements()) {
			user = users.nextElement();

			if (user.delete)
				continue;
			user.callback.plotData(System.currentTimeMillis(), error, 0, null, null, null);
		}
	}

	@Override
	public String toString()
	{
		return "SharedFTPRequest: " + super.toString();
	}

	public void stopCollection()
	{
	}

	synchronized void sendPlotData()
	{
		long[] microSecs = null;
		int[] nanoSecs = null;
		double[] values = null;
		int numToSend = 0;
		int error;
		if (numQueued != 0 || queuedError != 0) {
			synchronized (sendPlotDataEvent) {
				try {
					if (numQueued == 0) {
						microSecs = null;
						nanoSecs = null;
						values = null;
					} else {
						microSecs = new long[numQueued];
						nanoSecs = new int[numQueued];
						values = new double[numQueued];
						for (int ii = 0; ii < numQueued; ii++) {
							microSecs[ii] = queuedMicroSecs[ii];
							nanoSecs[ii] = queuedNanoSecs[ii];
							values[ii] = queuedValues[ii];
						}
					}
					numToSend = numQueued;
					numQueued = 0;
					error = queuedError;
					queuedError = 0;
				} catch (Exception e) {
					StringBuffer complaint = new StringBuffer();
					complaint.append("SharedFTPRequest, " + e);
					complaint.append("\r\nnumQueued: " + numQueued);
					if (microSecs != null)
						complaint.append(", microSecs len: " + microSecs.length);
					System.out.println(complaint.toString());
					e.printStackTrace();
					numQueued = 0;
					return;
				}

			}
			
			plotData(System.currentTimeMillis(), error, numToSend, microSecs, nanoSecs, values);
		}
	}

	public void update(DataEvent userEvent, DataEvent currentEvent) {
		if (userEvent == sendPlotDataEvent) {
			sendPlotData();
			return;
		}
	}
}
