// $Id: SnapScope.java,v 1.5 2023/11/02 17:01:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.ClockEvent;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

class SnapScope implements AcnetErrors
{
	/**
	 * @serial rate in hz
	 */
	private int rate;

	/**
	 * @serial duration in seconds
	 */
	private double duration;

	/**
	 * @serial number of points
	 */
	private int points = 0;

	/**
	 * @serial when rate is preferred
	 */
	private boolean ratePreferred = false;

	/**
	 * @serial when duration is preferred
	 */
	private boolean durationPreferred = false;

	/**
	 * number of supported sample events
	 */
	public final static int NUM_SAMPLE_EVENTS = 4;

	/**
	 * @serial sample events
	 */
	private byte[] sampleEvents;

	/**
	 * @serial sample events delay
	 */
	private int sampleEventsDelay;

	/**
	 * @serial when sampling on clock event
	 */
	private boolean sampleOnClockEvent = false;

	/**
	 * @serial when sampling on rate
	 */
	private boolean sampleOnDataSamplePeriod = false;

	/**
	 * @serial when sampling on external source
	 */
	private boolean sampleOnExternalSource = false;

	/**
	 * @serial external source sample modifier
	 */
	private int sampleTriggerModifier;

	/**
	 * Constructs a SnapScope object describing a preferred snapshot rate.
	 * 
	 * @param userRate
	 *            the user preferred rate in Hertz.
	 */
	public SnapScope(int userRate)
	{
		this(userRate, 0.0, null, false, false, 0);
		ratePreferred = true;
	}

	/**
	 * Constructs a SnapScope object describing a preferred snapshot duration.
	 * 
	 * @param userDuration
	 *            the user preferred duration in seconds.
	 */
	public SnapScope(double userDuration)
	{
		this(0, userDuration, null, false, false, 0);
		durationPreferred = true;
	}

	/**
	 * Constructs a SnapScope object describing snapshot sample collection on
	 * clock events.
	 * 
	 * @param events
	 *            the sample clock events.
	 */
	SnapScope(byte[] events)
	{
		this(0, 0, events, true, false, 0);
	}

	/**
	 * Constructs a SnapScope object describing snapshot sample collection on an
	 * external source.
	 * 
	 * @param sampleModifier
	 *            the sample modifier (0-3).
	 */
	SnapScope(byte sampleModifier)
	{
		this(0, 0, null, false, true, sampleModifier);
	}

	/**
	 * Constructs a SnapScope object describing snapshot sample collection.
	 * 
	 * @param userRate
	 *            the user preferred rate in Hertz.
	 * @param userDuration
	 *            the user preferred duration in seconds.
	 * @param events
	 *            the sample clock events.
	 * @param onClock
	 *            sample on clock event when true.
	 * @param onExternal
	 *            sample on external source when true.
	 * @param sampleModifier
	 *            the sample modifier (0-3).
	 */
	SnapScope(int userRate, double userDuration, byte[] events,
			boolean onClock, boolean onExternal, int sampleModifier) {
		rate = userRate;
		duration = userDuration;
		points = 0;
		sampleEvents = new byte[NUM_SAMPLE_EVENTS];
		for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++) {
			if (events != null && events.length > ii)
				sampleEvents[ii] = events[ii];
			else
				sampleEvents[ii] = ClockTrigger.NULL_EVENT;
		}
		sampleTriggerModifier = sampleModifier;
		if (onClock)
			sampleOnClockEvent = true;
		else if (onExternal)
			sampleOnExternalSource = true;
		else
			sampleOnDataSamplePeriod = true;
	}

	/**
	 * Constructs a SnapScope object from a database saved string.
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstruction().
	 */
	SnapScope(String reconstructionString) {
		this(0, 0.0, null, false, false, 0);
		StringTokenizer tok = null;
		String token = null;
		try {
			tok = new StringTokenizer(reconstructionString, ",=", false);
			tok.nextToken(); // skip rate=
			token = tok.nextToken();
			rate = (int) Double.parseDouble(token); // allow double
			tok.nextToken(); // skip dur=
			token = tok.nextToken();
			duration = Double.parseDouble(token);
			tok.nextToken(); // skip npts=
			token = tok.nextToken();
			points = Integer.parseInt(token);
			tok.nextToken(); // skip pref=
			token = tok.nextToken();
			if (token.equals("both"))
				ratePreferred = durationPreferred = true;
			else if (token.equals("rate"))
				ratePreferred = true;
			else if (token.equals("dur"))
				durationPreferred = true;
			tok.nextToken(); // skip smpl=
			token = tok.nextToken();
			if (token.startsWith("e")) {
				sampleOnClockEvent = true;
				for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++) {
					sampleEvents[ii] = (byte) Integer.parseInt(tok.nextToken(),
							16);
					if (!tok.hasMoreTokens())
						break;
				}
			} else if (token.startsWith("p"))
				sampleOnDataSamplePeriod = true;
			else {
				sampleOnExternalSource = true;
				tok.nextToken(); // skip mod=
				token = tok.nextToken();
				sampleTriggerModifier = Integer.parseInt(token);
			}
		} catch (Exception e) {
			System.out.println("SnapScope.reconstructionString, "
					+ reconstructionString + "\n\ntoken: " + token);
			e.printStackTrace();
		}
	}

	/**
	 * Return the rate in hz.
	 * 
	 * @return the rate in hz
	 */
	int getRate() {
		return rate;
	}

	/**
	 * Return the duration in seconds.
	 * 
	 * @return the duration in seconds
	 */
	double getDuration() {
		return duration;
	}

	/**
	 * Sets the preferred number of points to collect in a snapshot plot.
	 * 
	 * @param numberPoints
	 *            the number of points to collect.
	 */
	void clipNumberPoints(int numberPoints) {
		points = numberPoints;
		if (rate != 0 && duration == 0.0) duration = numberPoints/rate;
	}

	/**
	 * Return the number of points to collect.
     * 
	 * @param maxPoints the maximum number of points to collect
	 * @return the number of points
	 */
	int getNumberPoints(int maxPoints) {
		if (points != 0) {
			if (points > maxPoints)
				points = maxPoints;
			return points;
		}
		if (sampleOnClockEvent || sampleOnExternalSource) {
			points = maxPoints;
			return points;
		}
		if (ratePreferred || durationPreferred) {
			points = maxPoints;
			return points;
		}
		points = (int) (duration * rate);
		if (points > maxPoints)
			points = maxPoints;
		return points;
	}

	/**
	 * Return the number of points to collect.
	 * 
	 * @return the number of points
	 */
	int getNumberPoints() {
		return points;
	}

	/**
	 * Return a clone of a SnapScope object.
	 * 
	 * @return a clone of a SnapScope object
	 * 
	 */
	//Object clone() {
	//	try {
	//		return super.clone();
	//	} catch (CloneNotSupportedException e) {
	//		System.out.println("Cannot clone SnapScope" + e);
	//	}
	//	;
	//	return null;
	//}

	@Override
	public boolean equals(Object arg)
	{
		// System.out.println("SnapScope compare");
		if ((arg != null) && (arg instanceof SnapScope)) {
			SnapScope compare = (SnapScope) arg;
			if (compare.sampleOnClockEvent != sampleOnClockEvent
					|| compare.sampleOnDataSamplePeriod != sampleOnDataSamplePeriod
					|| compare.sampleOnExternalSource != sampleOnExternalSource) {
				// System.out.println("samples differ");
				return false;
			}
			// System.out.println("samples ok");
			if (sampleOnClockEvent)
				for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++)
					if (compare.sampleEvents[ii] != sampleEvents[ii])
						return false;
			// System.out.println("sample events okay");
			if (sampleOnExternalSource)
				if (compare.sampleTriggerModifier != sampleTriggerModifier)
					return false;
			if (ratePreferred) {
				if (compare.ratePreferred && compare.rate == rate)
					return true;
				return false;
			}
			if (durationPreferred) {
				if (compare.durationPreferred && compare.duration == duration)
					return true;
				return false;
			}
			if (points == 0 || compare.points == points)
				return true;
		}
		return false;
	}

	/**
	 * Return a string describing this SnapScope.
	 * 
	 * @return a string describing this SnapScope
	 */
	@Override
	public String toString() {
		return "Rate=" + rate + ", Duration=" + duration + ", points=" + points;
	}

	/**
	 * Return a list of collection events for pool sampling.
	 * 
	 * @param maxRate
	 *            maximum rate.
	 * @param maxPoints
	 *            maximum number of points.
	 * @return a list of events
	 */
	List<DataEvent> snapCollectionEvents(int maxRate, int maxPoints) {
		if (points == 0)
			points = maxPoints;
		List<DataEvent> returnedEvents = new LinkedList<DataEvent>();
		if (sampleOnClockEvent) {
			for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++) {
				if (sampleEvents[ii] != ClockTrigger.NULL_EVENT) {
					ClockEvent clockEvent = new ClockEvent(
							sampleEvents[ii] & 0x00ff);
					returnedEvents.add(clockEvent);
				}
			}
		} else if (sampleOnExternalSource)
			return null;
		else if (sampleOnDataSamplePeriod) {
			int sampleRate = rate;
			if (sampleRate > maxRate)
				sampleRate = maxRate;
			long milliSecsSample = 1000 / sampleRate;
			DeltaTimeEvent sampleEvent = new DeltaTimeEvent(milliSecsSample,
					true);
			returnedEvents.add(sampleEvent);
		} else
			return null;
		return returnedEvents;
	}

	/**
	 * Inquire if duration is preferred.
	 * 
	 * @return true if duration is preferred
	 */
	boolean isDurationPreferred() {
		return durationPreferred;
	}
	
	protected byte[] getSamplingEvents() { 
		return sampleEvents; 
	}
	
	/**
	 * Return sample events.
	 * 
	 * @return String listing sample events
	 */
	String getSampleEvents() {
		String events = "0x";

		for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++) {
			if (sampleEvents[ii] != ClockTrigger.NULL_EVENT)
				events += Integer.toHexString(sampleEvents[ii] & 0xff) + ",";
		}
		return events;
	}

	/**
	 * Return sample events.
     * 
	 * @param eventString 
     *      the event string
	 * @return byte[] sample events.
	 */
	static byte[] getSampleEventsRaw(String eventString) {
		byte[] sampleEvents = new byte[NUM_SAMPLE_EVENTS];
		for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++)
			sampleEvents[ii] = ClockTrigger.NULL_EVENT;

		// get events
		StringTokenizer tok;
		tok = new StringTokenizer(eventString, "x", false);
		int ii = 0;
		while (tok.hasMoreTokens()) {
			String nextHexEvent = tok.nextToken();
			int commaAt = nextHexEvent.indexOf(",");
			if (commaAt > 0)
				nextHexEvent = nextHexEvent.substring(0, commaAt);
			sampleEvents[ii++] = (byte) Integer.parseInt(nextHexEvent, 16);
		}
		return (sampleEvents);
	}

	/**
	 * Return if sampling is on clock event.
	 * 
	 * @return if sampling is on clock event
	 */
	boolean isSampleOnClockEvent() {
		return sampleOnClockEvent;
	}

	/**
	 * Return if sampling is on rate.
	 * 
	 * @return if sampling is on rate
	 */
	boolean isSampleOnDataSamplePeriod() {
		return sampleOnDataSamplePeriod;
	}

	/**
	 * Return if sampling is on external source.
	 * 
	 * @return if sampling is on external source
	 */
	boolean isSampleOnExternalSource() {
		return sampleOnExternalSource;
	}

	/**
	 * Return external source sample modifier
	 * 
	 * @return external source sample modifier
	 */
	int getSampleTriggerModifier() {
		return sampleTriggerModifier;
	}

	/**
	 * Return a duration ending event.
	 * 
	 * @return an event to stop sampling
	 */
	DataEvent durationEvent()
	{
		long milliSeconds;
		if (!durationPreferred) // calculate a time based upon rate
		{
			int seconds = points / rate;
			if (seconds < 10)
				seconds = 10;
			seconds += (seconds / 10);
			milliSeconds = seconds * 1000;
		} else {
			milliSeconds = (long) (duration * 1000);
		}
		DataEvent when = new DeltaTimeEvent(milliSeconds, false);
		return when;
	}

	String getReconstructionString()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("rate=" + rate + ",dur=" + duration + ",npts="
				+ points + ",pref=");
		if (ratePreferred && durationPreferred)
			returnBuffer.append("both");
		else if (ratePreferred)
			returnBuffer.append("rate");
		else if (durationPreferred)
			returnBuffer.append("dur");
		else
			returnBuffer.append("none");
		returnBuffer.append(",smpl=");
		if (sampleOnClockEvent) {
			returnBuffer.append("e");
			for (int ii = 0; ii < NUM_SAMPLE_EVENTS; ii++) {
				returnBuffer.append(","
						+ Integer.toHexString(sampleEvents[ii] & 0xff));
			}
			returnBuffer.append(sampleEventsDelay);
		} else if (sampleOnDataSamplePeriod)
			returnBuffer.append("p");
		else if (sampleOnExternalSource)
			returnBuffer.append("x,mod=" + sampleTriggerModifier);
		return returnBuffer.toString();
	}

	static String getTestSnapShotSpecificationEventString()
	{
		return "f,type=snp," + "rate=100,dur=20.48,npts=2048,pref=rate,smpl=p"
				+ ";" + "trig=e,00,FE,FE,FE,1000" + ";"
				+ "rearm=true,dly=p,60000,false,nmhr=30";
	}

	/**
	 * Create and return a SnapScope from a scope and trigger string.
     * 
	 * @param scopeAndTriggerAndReArm 
	 *         scope and trigger re-arm object
	 * @return a SnapScope
	 * @throws AcnetStatusException
	 *             if the scope is invalid
	 */
	static SnapScope getSnapScope(String scopeAndTriggerAndReArm)
			throws AcnetStatusException {
		SnapScope scope = null;
		try {
			StringTokenizer tok;
			String stripPrefix = scopeAndTriggerAndReArm.substring(12); // strip
			// f,type=snp,
			if (stripPrefix.equals("null"))
				return null;
			tok = new StringTokenizer(stripPrefix, ";", false);
			scope = new SnapScope(tok.nextToken());
		} catch (Exception e) {
			System.out.println("SnapScope.getSnapScope, "
					+ scopeAndTriggerAndReArm);
			e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid scope: "
					+ scopeAndTriggerAndReArm, e);
		}
		return scope;
	}

	/**
	 * Create and return a Trigger from a scope and trigger string.
     * 
	 * @param scopeAndTriggerAndReArm 
	 *         a scope and trigger re-arm object
	 * @return a Trigger
	 * @throws AcnetStatusException
	 *             if the trigger is invalid
	 */
	static Trigger getTrigger(String scopeAndTriggerAndReArm)
			throws AcnetStatusException {
		Trigger trigger = null;
		try {
			StringTokenizer tok;
			String stripPrefix = scopeAndTriggerAndReArm.substring(12); // strip
			// f,type=snap,
			tok = new StringTokenizer(stripPrefix, ";", false);
			tok.nextToken(); // skip SnapScope
			String triggerString = tok.nextToken();
			if (triggerString.equals("null"))
				return null;
			else if (triggerString.startsWith("trig=d"))
				trigger = new ExternalTrigger(triggerString);
			else if (triggerString.startsWith("trig=x"))
				trigger = new ExternalTrigger(triggerString);
			else if (triggerString.startsWith("trig=s"))
				trigger = new StateTransitionTrigger(triggerString);
			else if (triggerString.startsWith("trig=e"))
				trigger = new ClockTrigger(triggerString);
		} catch (Exception e) {
			System.out.println("SnapScope.getTrigger, "
					+ scopeAndTriggerAndReArm);
			e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid trigger: "
					+ scopeAndTriggerAndReArm, e);
		}
		return trigger;
	}

	/**
	 * Create and return a Trigger from a scope and trigger string.
     * 
	 * @param scopeAndTriggerAndReArm 
	 *             a scope and trigger re-arm object
	 * @return a Trigger
	 * @throws AcnetStatusException
	 *             if the trigger is invalid
	 */
	static ReArm getReArm(String scopeAndTriggerAndReArm)
			throws AcnetStatusException {
		ReArm rearm = null;
		try {
			StringTokenizer tok;
			String stripPrefix = scopeAndTriggerAndReArm.substring(12); // strip
			// f,type=snap,
			tok = new StringTokenizer(stripPrefix, ";", false);
			tok.nextToken(); // skip SnapScope
			tok.nextToken(); // skip Trigger
			String next = tok.nextToken();
			if (!next.equals("null"))
				rearm = new ReArm(next);
		} catch (Exception e) {
			System.out
					.println("SnapScope.getReArm, " + scopeAndTriggerAndReArm);
			e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "Invalid ReArm: "
					+ scopeAndTriggerAndReArm, e);
		}
		return rearm;
	}
}
