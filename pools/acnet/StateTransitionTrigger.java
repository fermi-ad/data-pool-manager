// $Id: StateTransitionTrigger.java,v 1.4 2023/11/01 20:56:57 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.LinkedList;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventFactory;
import gov.fnal.controls.servers.dpm.events.StateEvent;

/**
 * Describes a state transition trigger for the "armOnExternalSource" snapshot
 * functionality of the ACNET fast time plot system for Open Access Clients. It
 * also may be used in snapshot plot collection, however the data collection is
 * forced to the DAE at rates <= 15 Hz.
 * 
 * @author Kevin Cahill
 * @version 0.01<br>
 *          Class created: 30 August 2000
 * 
 */

public class StateTransitionTrigger implements Trigger, Cloneable, AcnetErrors
{
	private StateEvent event;

	private int armDelay;          // arm delay in microseconds or number of samples after arm

	private boolean armImmediately;

	/**
	 * Constructs a StateTransitionTrigger object representing an external
	 * arming event.
	 * 
	 * @param stateEvent
	 *            state event trigger.
	 * @param immediate
	 *            arm immediately when true.
	 */
	public StateTransitionTrigger(StateEvent stateEvent, boolean immediate) {
		if (stateEvent == null) // when building from reconstruction string
		{
			event = null;
			armDelay = 0;
			armImmediately = false;
			return;
		}
		armImmediately = immediate;
		armDelay = (int) stateEvent.getDelay() * 1000;
		if (armDelay < 0) {
			armImmediately = true;
			event = new StateEvent(stateEvent.deviceIndex(),
					stateEvent.state(), 0, stateEvent.flag());
		} else
			event = stateEvent;

	}

	/**
	 * Constructs a StateTransitionTrigger object representing an external
	 * arming event.
     * 
	 * @param stateEvent a state event
	 */
	public StateTransitionTrigger(StateEvent stateEvent) {
		this(stateEvent, false);
	}

	/**
	 * Constructs a StateTransitionTrigger object from a database saved string.
	 * 
	 * @param reconstructionString
	 *            string returned by getReconstructionString().
	 * @throws AcnetStatusException
	 *             if the reconstructionString is invalid
	 */
	public StateTransitionTrigger(String reconstructionString)
			throws AcnetStatusException {
		this((StateEvent) null);

		try {
			event = (StateEvent) DataEventFactory
					.stringToEvent(reconstructionString.substring(5)); // skip
																		// trig=
            armDelay = (int) event.getDelay() * 1000;
            if (armDelay < 0) {
                armImmediately = true;
                event = new StateEvent(event.deviceIndex(),
                        event.state(), 0, event.flag());
            }
		} catch (Exception e) {
			System.out.println("StateTransitionEvent, DataEventFactory: " + e);
			e.printStackTrace();
			throw new AcnetStatusException(DPM_BAD_EVENT, "StateTransitionEvent " + e, e);
		}
	}

	/**
	 * Compare a StateTransitionTrigger for equality.
	 * 
	 * @return true when the state triggers represents the same arming
	 *         conditions
	 */
	public boolean equals(Object arg) {
		if ((arg != null) && (arg instanceof StateTransitionTrigger)) {
			StateTransitionTrigger compare = (StateTransitionTrigger) arg;
			if (compare.event.toString().equals(event.toString()))
				return true;
			return false;
		}
		return false;
	}

	/**
	 * Return a clone of a StateTransitionTrigger.
	 * 
	 * @return a clone of a StateTransitionTrigger
	 * 
	 */
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Cannot clone StateTransitionTrigger" + e);
		}
		return null;
	}

	/**
	 * Return a string representing this StateTransitionTrigger.
	 * 
	 * @return a string representing this StateTransitionTrigger
	 * 
	 */
	public String toString() {
		return "State: " + event.toString();
	}

	/**
	 * Return the arming events.
	 * 
	 * @return the arming events
	 * 
	 */
	public List<DataEvent> getArmingEvents() {
		LinkedList<DataEvent> armingEvents = new LinkedList<DataEvent>();
		armingEvents.add(event);
		return armingEvents;
	}

	/**
	 * Return the state event.
	 * 
	 * @return the state event
	 * 
	 */
	public StateEvent getStateEvent() {
		return event;
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

	/**
	 * Return the arming events.
	 * 
	 * @return String listing arming events
	 */
	public String getArmEvents() {
		return null;
	}

	/**
	 * Return a String useful for reconstructing the StateTransitionTrigger of
	 * the form 'StateEvent: stateEvent.toString()'.
	 * 
	 * @return String describing this StateTransitionTrigger
	 */
	public String getReconstructionString() {
		return "trig=" + event.toString();
	}
} // end StateTransitionTrigger class
