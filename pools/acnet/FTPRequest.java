// $Id: FTPRequest.java,v 1.8 2023/11/03 02:28:02 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.events.AbsoluteTimeEvent;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventObserver;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;


class FTPRequest implements DataEventObserver, ReceiveData
{
	WhatDaq device;
	ClassCode plotClass;
	FTPScope scope;
	Trigger trigger;

	ReArm reArm;

	protected int priority;
	protected boolean delete;
	protected int error;
	protected int completionParameterId;

	private ReceiveData finalDisposition = null;

	private boolean isClipped = false;

	protected double ftpRate = 0.0;

	protected double ftpDuration = 0.0;

	protected AbsoluteTimeEvent durationEndingEvent = null;

	protected AbsoluteTimeEvent reArmEvent = null;

	protected int maxPoints = 0;

	protected int nextPoint = 0;

	private long[] ftpMicroSecs = null;

	private double[] ftpValues = null;

	private int ftpError = 0;

	protected boolean completed = false;

	protected boolean waitArm = false;

	protected boolean watchArm = false;

	ReceiveData callback;


	FTPRequest(WhatDaq userDevice, ClassCode classCode,
			FTPScope userScope, ReceiveData userCallback) {
		this(userDevice, classCode, null, userScope, userCallback, null);
	}

	FTPRequest(WhatDaq userDevice, ClassCode classCode,
			Trigger userTrigger, FTPScope userScope, ReceiveData callback,
			ReArm again) {
		setCallback(callback);

		device = userDevice;
		plotClass = classCode;
		trigger = userTrigger;
		scope = userScope;
		reArm = again;
		delete = false;

		if (userScope == null)
			System.out.println(toString() + " has null scope");
		else {
			ftpRate = userScope.getRate();
			ftpDuration = userScope.getDuration();
			if (ftpDuration != 0.0 && ftpDuration != Double.MAX_VALUE) {
				if ((ftpRate * ftpDuration) > 1000000) {
					System.out
							.println("FTPRequest, too many points to clip, rate: "
									+ ftpRate + ", duration: " + ftpDuration);
				} else {
					maxPoints = (int) (ftpRate * ftpDuration);
					if (maxPoints == 0) {
						System.out.println("FTPRequest, no points, rate: "
								+ ftpRate + ", duration: " + ftpDuration);
					} else {
						isClipped = true;
						ftpMicroSecs = new long[maxPoints];
						ftpValues = new double[maxPoints];
						finalDisposition = callback;
						setCallback(this);
					}
				}
			}
		}
		//setCollectionPriority(FTPPool.PLOT_PRIORITY_USER);
	}

	void setCallback(ReceiveData callback)
	{
		this.callback = callback;
	}

	void setCompletionParameterId(int id)
	{
		completionParameterId = id;
	}

	int getCompletionParameterId()
	{
		return completionParameterId;
	}

	WhatDaq getDevice()
	{
		return device;
	}

	int getFTPClassCode()
	{
		return plotClass.ftp();
	}

	ClassCode getClassCode()
	{
		return plotClass;
	}

	Trigger getTrigger()
	{
		return trigger;
	}

	ReArm getReArm()
	{
		return reArm;
	}

	void setClassCode(ClassCode code) {
		plotClass = code;
	}

	FTPScope getScope() {
		return scope;
	}

	int getError() {
		return error;
	}

	/**
	 * Mark this request for delete.
	 */
	void setDelete() {
		delete = true;
	}

	/**
	 * Return the delete status of this request.
	 * 
	 * @return true if this request is marked for delete
	 */
	boolean getDelete()
	{
		return delete;
	}

	void watchArmEvents()
	{
		completed = false;
		nextPoint = 0;
		watchArm = true;
		if (reArmEvent != null) {
			reArmEvent.deleteObserver(this);
			reArmEvent = null;
		}
		if (getTrigger() == null)
			waitArm = false;
		else {
			waitArm = true;
			for (DataEvent armEvent : getTrigger().getArmingEvents()) {
				armEvent.addObserver(this);
			}
		}
	}

	void stopWatchingArmEvents()
	{
		waitArm = false;
		for (DataEvent armEvent : getTrigger().getArmingEvents()) {
			armEvent.deleteObserver(this);
		}
	}

	boolean isArmEvent(DataEvent event)
	{
		for (DataEvent armEvent : getTrigger().getArmingEvents()) {
			if (event == armEvent)
				return true;
		}

		return false;
	}

	void lastCall(boolean forceError, int error)
	{
		if (!completed) {
			sendDispositionPlotData(true, forceError, error);
		}
	}

	void sendDispositionPlotData(boolean lastCall, boolean forceError, int error)
	{
		if (!completed) {
			completed = true;
			if (nextPoint == 0 && forceError && ftpError == 0)
				ftpError = error;
			if (durationEndingEvent != null) {
				durationEndingEvent.deleteObserver(this);
				durationEndingEvent = null;
			}
			if (waitArm)
				stopWatchingArmEvents();

			final long now = System.currentTimeMillis();
			if (finalDisposition != null)
				finalDisposition.plotData(now, ftpError, nextPoint, ftpMicroSecs, null, ftpValues);
			if (!lastCall) {
				ReArm again = getReArm();
				if (again == null)
					setDelete();
				else {
					again.setRecent(System.currentTimeMillis());
					final long reArmTime = again.getReArmTime(now);
					if (reArmTime != 0) {
						reArmEvent = new AbsoluteTimeEvent(reArmTime);
						reArmEvent.addObserver(this);
					}
				}
			} else
				setDelete();
		}
	}

	@Override
	public void plotData(long timestamp, int error, int numberPoints,
							long[] microSecs, int[] nanoSecs, double[] values) {
		if (completed)
			return;
		if (durationEndingEvent == null) {
			if (!watchArm)
				watchArmEvents();
			if (!waitArm) {
				durationEndingEvent = new AbsoluteTimeEvent(timestamp + ((long) ftpDuration * 1000L));
				durationEndingEvent.addObserver(this);
			}
		}
		if (error != 0) {
		}
		ftpError = error;
		if (waitArm)
			return;
		if (numberPoints > 0) {
			if ((numberPoints + nextPoint) > maxPoints)
				numberPoints = maxPoints - nextPoint;
			for (int ii = 0; ii < numberPoints; ii++) {
				ftpMicroSecs[nextPoint] = microSecs[ii];
				ftpValues[nextPoint++] = values[ii];
			}
			if (nextPoint >= maxPoints) {
				sendDispositionPlotData(false, false, 0);
			}
		}
	}

	@Override
	public void update(DataEvent request, DataEvent reply)
	{
		if (request == durationEndingEvent) {
			if (!completed) {
				sendDispositionPlotData(false, false, 0);
			}
		} else if (waitArm && isArmEvent(request)) {
			if (waitArm)
				stopWatchingArmEvents();
		} else if (request == reArmEvent) {
			watchArmEvents();
		}
	}
}
