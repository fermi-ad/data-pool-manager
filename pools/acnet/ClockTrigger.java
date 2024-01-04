// $Id: ClockTrigger.java,v 1.4 2023/11/01 21:24:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;

import gov.fnal.controls.servers.dpm.events.ClockEvent;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

class ClockTrigger implements Trigger, Cloneable, AcnetErrors
{
	/**
	 * number of supported arming events
	 */
	public final static int NUM_ARM_EVENTS = 8;

	/**
	 * the null event
	 */
	public final static byte NULL_EVENT = (byte) 0xff;

	/**
	 * @serial arming events
	 */
	private byte[] armEvents;

	/**
	 * @serial delay from arming events in microseconds or number of samples after arm
	 */
	private int armDelay;

	/**
	 * @serial arm immediately when true
	 */
	private boolean armImmediately;

	private transient List<DataEvent> armingEvents = null;

	/**
	 * Constructs a ClassTrigger object representing an immediate arming event.
	 */
	public ClockTrigger() {
		this(null, 0, true);
	}

	/**
	 * Constructs a ClassTrigger object representing an arming event.
	 * 
	 * @param event
	 *            a Tevatron clock event.
	 * @param delay
	 *            the delay from the arming clock event (microseconds or sample
	 *            periods).
	 * 
	 */
	public ClockTrigger(byte event, int delay) {
		armEvents = new byte[NUM_ARM_EVENTS];
		for (int ii = 0; ii < NUM_ARM_EVENTS; ii++)
			armEvents[ii] = NULL_EVENT;
		armEvents[0] = event;
		armDelay = delay;
        if (armDelay < 0) armImmediately = true;
	}

	/**
	 * Constructs a ClassTrigger object representing an immediate arming event.
	 * 
	 * @param events
	 *            an array of OR'd clock events (up to NUM_ARM_EVENTS).
	 * @param delay
	 *            the delay from the arming clock event (microseconds or sample
	 *            periods).
	 * 
	 */
	public ClockTrigger(byte[] events, int delay) {
		this(events, delay, false);
	}

	/**
	 * Constructs a ClassTrigger object from a database saved string. Of the
	 * form '0x2b,2a,0',1000 where 2b, 2a, and 0 are Tevatron clock events and
	 * 1000 is the delay (measured in milliseconds)
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstructionString().
	 * @throws AcnetStatusException
	 *             if the reconstructionString is invalid
	 */
	public ClockTrigger(String reconstructionString) throws AcnetStatusException {
		this(null, 0, false);

		int ii = 0;
		String[] eventsAndDelay = new String[NUM_ARM_EVENTS + 1];

		// get events and delay
		try {
			StringTokenizer tok;
			tok = new StringTokenizer(reconstructionString.substring(5), ",",
					false); // skip trig=
			tok.nextToken(); // skip e,
			while (tok.hasMoreTokens()) {
				eventsAndDelay[ii++] = tok.nextToken();
			}
			armDelay = Integer.parseInt(eventsAndDelay[--ii], 10) * 1000;
            if (armDelay < 0) armImmediately = true;
			// --ii; // there is no 'e, s, or h" -- skip 'e,s, or h'
			for (int jj = 0; jj < ii; jj++)
				armEvents[jj] = (byte) Integer.parseInt(eventsAndDelay[jj], 16);
		} catch (Exception e) {
			System.out.println("ClockTrigger.reconstructionString, "
					+ reconstructionString);
			e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "ClockTrigger.reconstructionString, "
					+ reconstructionString, e);
		}
	}

	/**
	 * Private constructor for a ClassTrigger object.
	 * 
	 * @param events
	 *            an array of OR'd clock events (up to NUM_ARM_EVENTS).
	 * @param delay
	 *            the delay from the arming clock event (microseconds or sample
	 *            periods).
	 * @param immediately
	 *            arm immediately when true.
	 * 
	 */
	public ClockTrigger(byte[] events, int delay, boolean immediately) {
		armEvents = new byte[NUM_ARM_EVENTS];
		for (int ii = 0; ii < NUM_ARM_EVENTS; ii++) {
			if (events != null && events.length > ii)
				armEvents[ii] = events[ii];
			else
				armEvents[ii] = NULL_EVENT;
		}
		armDelay = delay;
		armImmediately = immediately;
        if (armDelay < 0) armImmediately = true;
	}

	/**
	 * Compare a ClockTrigger for equality.
	 * 
	 * @return true when the clock trigger represents the same arming conditions
	 */
	public boolean equals(Object arg) {
		// System.out.println("ClockTrigger compare");
		if ((arg != null) && (arg instanceof ClockTrigger)) {
			ClockTrigger compare = (ClockTrigger) arg;
			if (compare.armDelay != armDelay) {
				return false;
			}
			// System.out.println("delay ok");
			if (compare.armImmediately != armImmediately) {
				return false;
			}
			// System.out.println("armI ok");
			for (int ii = 0; ii < NUM_ARM_EVENTS; ii++)
				if (compare.armEvents[ii] != armEvents[ii])
					return false;
			// System.out.println("clockTrigger ok");
			return true;
		}
		return false;
	}

	/**
	 * Return a clone of a ClockTrigger.
	 * 
	 * @return a clone of a ClockTrigger
	 * 
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Cannot clone ClockTrigger" + e);
		}
		;
		return null;
	}

	/**
	 * Return a string representing this ClockTrigger.
	 * 
	 * @return a string representing this ClockTrigger
	 * 
	 */
	@Override
	public String toString() {
		return getArmEvents();
	}

	/**
	 * Return the arming events.
	 * 
	 * @return the arming events
	 * 
	 */
	@Override
	public List<DataEvent> getArmingEvents() {
		if (armingEvents == null) {
			armingEvents = new LinkedList<DataEvent>();
			int triggerArmDelay = armDelay / 1000;
			if (armImmediately)
				triggerArmDelay = 0; // delay is microseconds or samples till
										// stop collection
			for (int ii = 0; ii < NUM_ARM_EVENTS; ii++) {
				if (armEvents[ii] != NULL_EVENT) {
					ClockEvent clockEvent = new ClockEvent(
							armEvents[ii] & 0xff, true, triggerArmDelay, true);
					armingEvents.add(clockEvent);
				}
			}
		}
		return armingEvents;
	}

	/**
	 * Inquire if arm immediately.
	 * 
	 * @return true if arm immediately
	 * 
	 */
	public boolean isArmImmediately() {
		return armImmediately;
	}

	/**
	 * Get the delay from arming events.
	 * 
	 * @return the delay from arming events in microseconds
	 * 
	 */
	public int getArmDelay() {
		return armDelay;
	}
	
	protected byte[] getArmingEventArray() { return armEvents; }

	/**
	 * Return the arming events.
	 * 
	 * @return String listing arming events
	 */
	public String getArmEvents() {
		String events = "e,";
		for (int ii = 0; ii < NUM_ARM_EVENTS; ii++) {
			if (armEvents[ii] != NULL_EVENT)
				events += Integer.toHexString(armEvents[ii] & 0xff) + ",";
		}
		return events;
	}

	/**
	 * Return a String useful for reconstructing the ClockTrigger of the form
	 * '0x2b,2a,0',1000
	 * 
	 * @return String describing this ClockTrigger
	 */
	public String getReconstructionString() {
		return "trig=" + getArmEvents() + armDelay/1000;
	}

	/**
     * Returns old reconstruction event.
     * 
	 * @return old reconstruction events
	 */
	public String getOldReconstructionString() {
		String events = "0x";

		for (int ii = 0; ii < NUM_ARM_EVENTS; ii++) {
			if (armEvents[ii] != NULL_EVENT) {
				events += Integer.toHexString(armEvents[ii] & 0xff) + ",";
			}
		}
		return events + armDelay;
	}
} // end ClockTrigger class
