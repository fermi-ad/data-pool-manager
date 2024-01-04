// $Id: ReArm.java,v 1.6 2023/11/02 17:11:03 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventFactory;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

import java.util.StringTokenizer;

class ReArm implements AcnetErrors
{
	private boolean enabled;

	/**
	 * @serial re-arm delay event
	 */
	private DataEvent reArmDelayEvent;

	/**
	 * @serial maximum number of collections per hour
	 */
	protected int maxPerHour;

	/**
	 * @serial time of recent snaps
	 */
	protected transient long[] recents = null;
	private transient long mostRecentCollection = 0;
	private transient long earliestCollection = 0;
	private transient boolean reArmReady = true;

	/**
	 * Constructs a ReArm object with re-arming on or off.
	 * 
	 * @param enabled
	 *            state of the re-arming request.
	 */
	ReArm(boolean enabled)
	{
		this(enabled, null, -1);
	}

	/**
	 * Constructs a ReArm object with re-arming on after a delay.
	 * 
	 * @param delay
	 *            delay before re-arming.
	 */
	ReArm(DataEvent delay)
	{
		this(true, delay, -1);
	}

	/**
	 * Constructs a ReArm object with re-arming on after a delay but limited to a
	 * number of collections per hour.
	 * 
	 * @param delay
	 *            delay before re-arming.
	 * @param maxPerHour
	 *            maximum number of snapshots to collect per hour.
	 */
	ReArm(DataEvent delay, int maxPerHour)
	{
		this(true, delay, maxPerHour);
	}

	/**
	 * Private constructor for a ReArm object.
	 * 
	 * @param enabled
	 *            re-arming enabled when true.
	 * @param delayEvent
	 *            delay before re-arming.
	 * @param maxPerHour
	 *            maximum number of snapshots to collect per hour.
	 */
	private ReArm(boolean enabled, DataEvent delayEvent, int maxPerHour)
	{
		this.enabled = enabled;
		reArmDelayEvent = delayEvent;
		this.maxPerHour = maxPerHour;
	}

	/**
	 * Constructs a ReArm object from a database saved string.
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstruction().
	 * @throws AcnetStatusException
	 *             if the reconstructionString is invalid
	 */
	ReArm(String reconstructionString) throws AcnetStatusException
	{
		this(false);
		StringTokenizer tok = null;
		String token = null;
		try {
			tok = new StringTokenizer(reconstructionString, ",=", false);
			tok.nextToken(); // skip rearm=
			token = tok.nextToken();
			enabled = new Boolean(token).booleanValue();
			if (!enabled) {
				reArmDelayEvent = null;
				maxPerHour = 1;
			} else {
				tok.nextToken(); // skip ,dly=
				StringBuffer delayString = new StringBuffer();
				token = tok.nextToken();
				while (!token.startsWith("nmhr")) {
					if (delayString.length() != 0)
						delayString.append(",");
					delayString.append(token);
					token = tok.nextToken();
				}
				try {
					if (delayString.toString().equals("null"))
						reArmDelayEvent = null;
					else
						reArmDelayEvent = (DataEvent) DataEventFactory
								.stringToEvent(delayString.toString());
				} catch (Exception e) {
					System.out.println("ReArm, DataEventFactory: " + e);
					e.printStackTrace();
					throw new AcnetStatusException(DPM_BAD_EVENT, "ReArm, bad delay event "
							+ delayString + ", " + e, e);
				}
				token = tok.nextToken().trim();
				maxPerHour = Integer.parseInt(token);
			}
		} catch (Exception e) {
			throw new AcnetStatusException(DPM_BAD_EVENT, "ReArm.reconstructionString, " + reconstructionString, e);
		}
	}

	/**
	 * Return true if enabled for re-arming.
	 * 
	 * @return true if enabled for re-arming
	 * 
	 */
	boolean isEnabled()
	{
		return enabled;
	}

	void setReArmReady(boolean ready)
	{
		reArmReady = ready;
	}

	boolean isReArmReady()
	{
		return reArmReady;
	}

	long getEarliestCollection()
	{
		return earliestCollection;
	}

	void setRecent(long lastCollection)
	{
		mostRecentCollection = lastCollection;
		if (maxPerHour <= 0)
			return;
		if (recents == null)
			recents = new long[maxPerHour];
		int index = 0;
		long oldestCollection = lastCollection;
		for (int ii = 0; ii < maxPerHour; ii++) {
			if (recents[ii] == 0) {
				index = ii;
				break;
			} else if (recents[ii] < oldestCollection) {
				oldestCollection = recents[ii];
				index = ii;
			}
		}
		recents[index] = lastCollection;
	}

	long getReArmTime(long now)
	{
		long reArmWait = 0;
		if (reArmDelayEvent != null
				&& reArmDelayEvent instanceof DeltaTimeEvent)
			reArmWait = ((DeltaTimeEvent) reArmDelayEvent).getRepeatRate();
		if (!enabled || maxPerHour == 0) {
			earliestCollection = 0;
			return earliestCollection;
		}
		if (reArmWait == 0 && maxPerHour < 0) {
			earliestCollection = System.currentTimeMillis() - 500L;
			return earliestCollection;
		}
		long newestCollection = mostRecentCollection;
		if (maxPerHour > 0) {
			long oldestCollection = now;
			for (int ii = 0; ii < maxPerHour; ii++) {
				if (newestCollection == 0)
					newestCollection = recents[ii];
				else if (recents[ii] != 0
						&& recents[ii] > newestCollection)
					newestCollection = recents[ii];
				if (recents[ii] != 0
						&& recents[ii] < oldestCollection)
					oldestCollection = recents[ii];
			}
			long hourAgo = now - (60 * 60 * 1000);
			boolean maxedOut = true;
			for (int ii = 0; ii < maxPerHour; ii++) {
				if (recents[ii] == 0 || recents[ii] < hourAgo) {
					maxedOut = false;
					break;
				}
			}
			if (maxedOut) {
				long nextOldestCollection = now;
				for (int ii = 0; ii < maxPerHour; ii++) {
					if (recents[ii] > oldestCollection
							&& recents[ii] < nextOldestCollection)
						nextOldestCollection = recents[ii];
				}
				if (nextOldestCollection - hourAgo > reArmWait) {
					earliestCollection = nextOldestCollection + (60 * 60 * 1000);
					return earliestCollection;
				}
			}
		}

		if (newestCollection == 0)
			earliestCollection = now;
		else
			earliestCollection = newestCollection + reArmWait;

		return earliestCollection;
	}

	@Override
	public String toString()
	{
		StringBuilder buf = new StringBuilder();

		buf.append("rearm=" + enabled + ",dly=");
		if (reArmDelayEvent == null)
			buf.append("null");
		else
			buf.append(reArmDelayEvent.toString());
		buf.append(",nmhr=" + maxPerHour);

		return buf.toString();
	}

	public String getReconstructionString()
	{
		return toString();
	}
}
