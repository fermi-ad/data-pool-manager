// $Id: FTPScope.java,v 1.4 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.StringTokenizer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

class FTPScope implements Cloneable, AcnetErrors {

	// for start and stop data returns
	/**
	 * @serial 0 => none, 1 => MR resets, 2 => TEV resets
	 */
	protected int groupEventCode = 0;

	/**
	 * @serial clock event
	 */
	protected byte clockEventNumber = (byte) 0xFE;

	/**
	 * @serial use group event code rather than clock event
	 */
	protected boolean onGroupEventCode = true;

	/**
	 * @serial duration in seconds
	 */
	protected double duration;

	/**
	 * @serial data sample period in Hertz
	 */
	protected final double rate;

	/**
     * Set the rate.
     * 
	 * @param r the rate
	 */
	//public void setRate(double r)
	//{
	//	if (r >= 0)
	//		rate = r;
	//}

	/**
     * Read the rate.
     * 
	 * @return the rate
	 */
	double getRate()
	{
		return rate;
	}

	/**
	 * Constructs an FTPScope for continuous data return with a rate preference.
	 * 
	 * @param userRate
	 *            the rate request in hertz.
	 */
	public FTPScope(double userRate)
	{
		this(userRate, (byte) 0, true, Double.MAX_VALUE);
	}

	/**
	 * Constructs an FTPScope for intermittent data return.
	 * 
	 * @param userRate
	 *            the rate request in hertz.
	 * @param eventCode
	 *            a Tevatron Clock Event or group code (0-none, 1-MR, 2-TEV).
	 * @param onGroup
	 *            eventCode contains a group code rather than event.
	 * @param userDuration
	 *            duration in seconds.
	 */
	public FTPScope(double userRate, byte eventCode, boolean onGroup, double userDuration)
	{
		rate = userRate;
		groupEventCode = eventCode;
		if (onGroup) {
			onGroupEventCode = true;
			groupEventCode = eventCode;
		} else {
			onGroupEventCode = false;
			clockEventNumber = eventCode;
		}
		duration = userDuration;
	}

	/**
	 * Constructs a SnapScope object from a database saved string.
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstruction().
	 */
	public FTPScope(String reconstructionString)
	{
		//this(0);

		StringTokenizer tok = null;
		String token = null;

		//try {
			tok = new StringTokenizer(reconstructionString, ",=", false);
			tok.nextToken(); // skip rate=
			token = tok.nextToken();
			rate = Double.parseDouble(token);
			tok.nextToken(); // skip dur=
			token = tok.nextToken();
			duration = Double.parseDouble(token);
		//} catch (Exception e) {
		//	System.out.println("FTPScope.reconstructionString, "
		//			+ reconstructionString + "\n\ntoken: " + token);
		//	e.printStackTrace();
		//}
	}

	/**
	 * Return a clone of an FTPScope object.
	 * 
	 * @return a clone of an FTPScope object
	 * 
	 */
	public Object clone()
	{
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Cannot clone FTPScope" + e);
		}
		
		return null;
	}

	/**
	 * Compare an FTPScope for equality.
	 * 
	 * @return true when the FTP scope represents the same scope conditions
	 */
	public boolean equals(Object arg)
	{
		// System.out.println("FTPScope compare");
		if ((arg != null) && (arg instanceof FTPScope)) {
			FTPScope compare = (FTPScope) arg;
			if (compare.rate == rate && compare.duration == duration
					&& compare.onGroupEventCode == onGroupEventCode) {
				if (onGroupEventCode) {
					if (compare.groupEventCode == groupEventCode)
						return true;
				} else if (compare.clockEventNumber == clockEventNumber)
					return true;
			}
		}
		return false;
	}

	/**
	 * Return a string representing this FTPScope.
	 * 
	 * @return a string representing this FTPScope
	 */
	public String toString() {
		if (onGroupEventCode && groupEventCode == 0)
			return "FTPScope, rate " + rate + ", duration: " + duration;
		else
			return ("FTPScope event 0x"
					+ Integer.toHexString(clockEventNumber & 0xff)
					+ ", groupEventCode " + groupEventCode + ", is on group: "
					+ onGroupEventCode + ", duration: " + duration);
	}

	/**
	 * Return the group event code of this FTPScope.
	 * 
	 * @return the group event code of this FTPScope
	 */
	public int getGroupEventCode() {
		return groupEventCode;
	}

	/**
	 * Return the duration in seconds.
	 * 
	 * @return the duration in seconds
	 */
	public double getDuration() {
		return duration;
	}

	/**
	 * Set the duration in seconds.
	 * 
	 * @param userDuration
	 *            duration in seconds
	 */
	public void setDuration(double userDuration) {
		duration = userDuration;
	}

	/**
	 * Return a collection event for pool sampling.
     * 
	 * @param maxRate the maximum rate
	 * @return an event
	 */
	public DataEvent ftpCollectionEvent(int maxRate)
	{
		//if (onGroupEventCode && groupEventCode != 0) {
		//	System.out
		//			.println("ftpCollectionEvent, request on groupEventCode ignored");
		//}
		double plotRate = maxRate;

		if (rate < plotRate)
			plotRate = rate;

		long milliSecs = (long) ((1000. / plotRate) + 0.5);

		DataEvent collectionEvent = new DeltaTimeEvent(milliSecs);

		return collectionEvent;
	}


	/**
	 * Return a String useful for reconstructing a FTPScope. Intentionally
	 * obscuring group event code, ... Trigger, plus new kinds of DataEvent will
	 * suffice.
	 * 
	 * @return String for reconstructing this FTPScope
	 */
	public String getReconstructionString() {
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("rate=" + rate + ",dur=" + duration);
		return returnBuffer.toString();
	}

	/**
	 * Create and return a FTPScope from a scope and trigger string.
     * 
	 * @param scopeAndTriggerAndReArm 
     *            scope and trigger string 
	 * @return a FTPScope
	 * @throws AcnetStatusException
	 *             if the scope is invalid
	 */
	public static FTPScope getFTPScope(String scopeAndTriggerAndReArm)
			throws AcnetStatusException
	{
		try {
			StringTokenizer tok;
			String stripPrefix = scopeAndTriggerAndReArm.substring(12); // strip
																		// f,type=ftp,
			if (stripPrefix.equals("null"))
				return null;
			tok = new StringTokenizer(stripPrefix, ";", false);
			return new FTPScope(tok.nextToken());
		} catch (Exception e) {
			System.out.println("FTPScope.getFTPScope, "
					+ scopeAndTriggerAndReArm);
			e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid scope: "
					+ scopeAndTriggerAndReArm, e);
		}
	}
}
