// $Id: ClockEvent.java,v 1.3 2023/09/26 20:52:04 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import java.sql.ResultSet;

//import gov.fnal.controls.servers.dpm.pools.acnet.DaqDefinitions;
import gov.fnal.controls.db.DbServer;

/* ****************************************************************************
To the maintainer of this file:

We (Denise & John, mainly) are making progress transferring the database from
Sybase to PostgreSQL.  As part of that effort, all database queries (and
updates) in this file have been converted to the PostgreSQL dialect of SQL, and
added to this file IN PARALLEL with the Sybase SQL.  At any time, only ONE
version of each query (or update) is ever executed.  Currently that will be the
Sybase version.  Soon, the PostgreSQL version will be activated, and the Sybase
version will be retired.

In the meantime, any updates to the SQL (by you, the maintainer) will need to
be made in parallel, to both the Sybase AND the PostgreSQL version.  Since
translation from Sybase to PostgreSQL may be non-trivial, both Denise and John
will be available to offer assistance.
* ****************************************************************************/


import static gov.fnal.controls.servers.dpm.events.Postgres.DO_SYBASE;
import static gov.fnal.controls.servers.dpm.events.Postgres.DO_POSTGRES;
import static gov.fnal.controls.db.DbServer.getDbServer;


public class ClockEvent implements DataEvent, DbServer.Constants
{
	//private static boolean classBugs = false;
	//private static int numAddObserver = 0;
	//private static int numClockEvent = 0;
	//private static int numClockEventNameToNumber = 0;
	//private static int numClockEventNumberToDescription = 0;
	//private static int numClockEventNumberToName = 0;
	//private static int numClockMulticast = 0;
	//private static int numDeleteObserver = 0;
	//private static int numGetNames = 0;
	//private static int numTimeInSuperCycle = 0;
    //private static int num02ClockFromMulticast = 0;
	//private static int numReportStatistics = 0;

	/**
	 * maximum number of unique Tevatron Clock Events
	 */
	public static final int MAX_NUMBER_EVENTS = 256;

	/**
	 * maximum value that a ClockEvent can have
	 * 
	 * @serial
	 */
	public static final int MAX_EVENT_NUMBER = 0xFF;

	/**
	 * minimum value a ClockEvent can have
	 * 
	 * @serial
	 */
	public static final int MIN_EVENT_NUMBER = 0;

	/**
	 * Super Cycle reset event
	 * 
	 * @serial
	 */
	public static final int SUPER_CYCLE_RESET_EVENT = 0x00;

	final int number; // the clock event number

	/** @serial */
	//private boolean softEvent; // manage event in local software when true

	/** @serial */
	final boolean substitute; // substitute from hard -> soft

	/** @serial */
	final int micros = 0; // number of microseconds after time stamp when

	// this event occurred
	/** @serial */
	private int count = 0; // number of times this event occurred in the
								// packet
	long eventSum = 0; // number of times this event occurred since epoch
	
	final boolean hardEvent;
	final long delay;
	final String string;

	// -------------------- class variables --------------------->    

	private static boolean getNames = true;

	//private static ClockEventDecoder eventDecoder = new ClockEventDecoder();
	// store clock event names
	private static String[] names = new String[MAX_NUMBER_EVENTS]; 
	// store short clock event descriptions
	private static String[] descr = new String[MAX_NUMBER_EVENTS]; 

	private final static String invalidClockEvent = "invalid clock event";

	private ClockEvent scReset = null;

	/**
	 * Construct a ClockEvent number for an associated clock event name.
	 * 
	 * @param name
	 *            a clock event name
	 */
	//public ClockEvent(String name) {
	//	this(clockEventNameToNumber(name));
	//}

	/**
	 * constructor
	 * 
	 * @param number
	 *            a clock event number; valid range (0,0xFF)
	 * @param hardEvent
	 *            true indicates that the Data Acquisition Engine (DAE) should
	 *            NOT simulate this event.
	 * @param delay
	 *            number of milliseconds to delay from event before action
	 *            associated with this event is taken
	 * @param substitute
	 *            if necessary substitute a software managed event for this event
	 *            which is simulated by the DAE
	 */
	public ClockEvent(int number, boolean hardEvent, long delay, boolean substitute)
	{
		final int tmp = number & 0xff;

		this.string = "e," + Integer.toHexString(number & 0xff) + "," + 
						(hardEvent ? (substitute ? "e," : "h,") : "s,") + delay;
		this.number = number;
		this.hardEvent = hardEvent;
		this.delay = delay;
		this.substitute = substitute;
	} 

	/**
	 * constructor
	 * 
	 * @param number
	 *            a clock event number; valid range (0,FF)
	 * @param hardEvent
	 *            do not manage event in local software.
	 * @param delayFromEvent
	 *            (+-) delay in milliseconds from event.
	 */
	public ClockEvent(int number, boolean hardEvent, long delay)
	{
		this(number, hardEvent, delay, true);
	}

	/**
	 * constructs a ClockEvent with value equal to 'number' and a zero
	 * millisecond delay.
	 * 
	 * @param number
	 *            a clock event number; valid range 0 - 255
	 * @param hardEvent
	 *            if true then do not manage event in local software
	 */
	public ClockEvent(int number, boolean hardEvent)
	{
		this(number, hardEvent, 0);
	}

	/**
	 * constructs a 'hard' clock event with a zero millisecond delay
	 * 
	 * @param number
	 *            a clock event number; valid range 0 - 255
	 */
	public ClockEvent(int number)
	{
		this(number, true, 0, false);
	}

	@Override
	public boolean isRepetitive()
	{
		return true;
	}

	@Override
	public int ftd()
	{
		//return delay == 0 ? (number | DaqDefinitions.EVENT_FTD_MASK) : 0;
		return delay == 0 ? (number | DataEvent.ftdMask()) : 0;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof ClockEvent) {
			return o.toString().equalsIgnoreCase(string);
		}
			
		return false;
	}

	@Override
	public String toString()
	{
		return string;
	}

	/**
	 * Constructs a Tevatron Clock Event from a Multicast Clock Event and the
	 * time of the last $02 event and super cycle reset.
	 * 
	 * @param event
	 *            the Multicast Clock Event
	 * @param event02
	 *            time stamp when TCLK $02 last happened.
	 * @param sc
	 *            Super cycle reset time
	 */
	 /*
	ClockEvent(MCClockEvent event, ClockEvent sc) {
		super(ClockEventDecoder.multicastClockEventString[event.event]);
		++numClockMulticast;
		clockEventNumber = event.event();
		eventSum = event.sum;
		delay = 0;
		softEvent = false;
		ftd = (short) (event.event() | DaqDefinitions.EVENT_FTD_MASK);
		repetitive = true;
		canSubstitute = false;
		count = event.count;
		micros = (int) (event.micros() % (int) 1000);
		setTime(event.millis);
		if (clockEventNumber != SUPER_CYCLE_RESET_EVENT)
			scReset = sc; // do not want a chain of references
		else
			scReset = null;
        if (clockEventNumber == 0x02) {
            ++num02ClockFromMulticast;
        }
	}
	*/

	/**
	 * Add a DataEventObserver who is interested in receiving notification when
	 * this accelerator clock event occurs.
	 * 
	 * @param observer
	 *            observer to add
	 */
	@Override
	public void addObserver(DataEventObserver observer)
	{
		//++numAddObserver;
		//eventDecoder.addObserver(observer, this);
	//	throw new RuntimeException("addObserver not implemented");
	}

	/**
	 * Indicates whether or not this ClockEvent can be substituted from a
	 * 'hardware' event to a 'software' event
	 * 
	 * @return true if this event can be substituted for a 'soft' event.
	 */
	public boolean canSubstitute()
	{
		return substitute;
	}

	/**
	 * removes a DataEventObserver who was previously registered to receive
	 * notification when this accelerator ClockEvent had occurred.
	 * 
	 * @param observer
	 *            observer to remove
	 */
	@Override
	public void deleteObserver(DataEventObserver observer)
	{
		//++numDeleteObserver;
		//eventDecoder.deleteObserver(observer, this);
		//throw new RuntimeException("deleteObserver not implemented");
	}

	/**
	 * Returns clock event name for an associated clock event number.
	 * 
	 * @param clockEvent
	 *            Range (0 to 255)
	 * @return clock event name for an associated clock event number.
	 */
	 
	public static String clockEventNumberToName(int clockEvent) {
		//++numClockEventNumberToName;
		// look it up
		if (clockEvent < 0 || clockEvent >= MAX_NUMBER_EVENTS)
			return invalidClockEvent;
		if (getNames) {
			getNames();
			getNames = false;
		}
		return names[clockEvent];
	}
	

	/**
	 * Returns clock event description for an associated clock event number.
	 * 
	 * @param clockEvent
	 *            Range (0 to 255)
	 * @return clock event description for an associated clock event number.
	 */
	public static String clockEventNumberToDescription(int clockEvent) {
		//++numClockEventNumberToDescription;
		// look it up
		if (clockEvent < 0 || clockEvent > 255)
			return invalidClockEvent;
		if (getNames) {
			getNames();
			getNames = false;
		}
		return descr[clockEvent];
	}

	/**
	 * Returns clock event number for an associated clock event name.
     * @param name
	 * 
	 * @return a clock event number for an associated clock event name; If
	 *         <it>name</it> is a valid clock event name then a positive value
	 *         is returned; else -1 is returned for an invalid name string
	 */
	public static int clockEventNameToNumber(String name) {
		// look it up
		//++numClockEventNameToNumber;
		if (getNames) {
			getNames = false;
			getNames();
		}
		for (int ii = 0; ii < MAX_NUMBER_EVENTS; ii++) {
			if (names[ii].equalsIgnoreCase(name))
				return ii;
		}
		return -1;
	}

	/**
	 * Returns clock event description.
	 * 
	 * @return clock event description.
	 */
	//public String getClockEventDescription() {
	//	return clockEventNumberToDescription(clockEventNumber);
	//}

	/**
	 * Returns clock event name.
	 * 
	 * @return clock event name.
	 */
	//public String getClockEventName() {
	//	return clockEventNumberToName(clockEventNumber);
//	}

	/**
	 * Returns clock event number.
	 * 
	 * @return clock event number.
	 */
	public int getClockEventNumber()
	{
		return number;
	}

	/**
	 * Returns soft clock event.
	 * 
	 * @return <code>true<\code> if this ClockEvent is a
	 *         software generated event
	 */
	public boolean isSoftClockEvent()
	{
		return !hardEvent;
	}

	/**
	 * Performs a JDBC query so all clock events have a name and description.
	 * Stores names and descriptions statically.
	 */
	private static void getNames() {
		//++numGetNames;
		String query = null;
		if ( DO_SYBASE ) {
		    query = "SELECT symbolic_name, long_text, event_number FROM appdb.hendricks.clock_events where event_type = 0";
		}
		if ( DO_POSTGRES ) {
		    query = "SELECT symbolic_name, long_text, event_number FROM hendricks.clock_events where event_type = 0";
		}
		//if (classBugs) System.out.println(query);
		try {
			ResultSet rs = null;
			if ( DO_SYBASE ) { rs = ADBS.executeQuery(query);  }
			if ( DO_POSTGRES ) { rs = getDbServer("adbs").executeQuery(query);  }

			int eventNo;
			for (eventNo = 0; eventNo < names.length; eventNo++) {
				names[eventNo] = "Undefined";
				descr[eventNo] = "Undefined Event Number";
			}
			while (rs.next()) {
				eventNo = rs.getInt(3);
				if (eventNo < names.length) {
					names[eventNo] = rs.getString(1);
					descr[eventNo] = rs.getString(2);
				}
				//if (classBugs)
				//	System.out.println("Event#:" + eventNo + "   Name: "
				//			+ names[eventNo] + "    Description: "
				//			+ descr[eventNo]);
			}
			rs.close();
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	/**
	 * ClockEvent occurred.
	 * 
	 * @return microsecond time resolution
	 */
	public int micros() {
		return micros;
	}

	/**
	 * returns the number of times this event occurred in the last multicast
	 * update.
	 * 
	 * @return the count of events
	 */
	public int count() {
		return count;
	}

	/**
	 * returns the number of times this event occurred since the epoch
	 * 
	 * @return the count of events since the epoch
	 */
	public long eventSum() {
		return eventSum;
	}

	/**
	 * returns the number of microseconds this event occurred relative to the
	 * super cycle reset event.
	 * 
	 * @return <code>positive value</code> number of microseconds when this
	 *         event occurred relative to super cycle reset
	 *         <code>negative value</code> super cycle reset hasn't occurred
	 *         yet
	 * @see #SUPER_CYCLE_RESET_EVENT
	 */
	public long timeInSuperCycle() {
		//++numTimeInSuperCycle;
		if (scReset == null) {
			return -1;
		} else {
			return (1000 * (createdTime() - scReset.createdTime()) + (micros - scReset.micros()));
		}
	}

	/**
	 * Return a statistics report.
	 * 
	 * @return a statistics report
	 */
	 /*
	public static String reportStatistics() {
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("\r\nAcceleratorMulticastMonitor statistics:");
		if (numClockEvent != 0) returnBuffer.append("\r\n\tnumClockEvent: " + numClockEvent);	    
		if (numClockMulticast != 0) returnBuffer.append("\r\n\tnumClockMulticast: " + numClockMulticast);	    
		if (num02ClockFromMulticast != 0) returnBuffer.append("\r\n\tnum02ClockFromMulticast: " + num02ClockFromMulticast);	    
		if (numAddObserver != 0) returnBuffer.append("\r\n\tnumAddObserver: " + numAddObserver);	    
		if (numDeleteObserver != 0) returnBuffer.append("\r\n\tnumDeleteObserver: " + numDeleteObserver);	    
		if (numClockEventNameToNumber != 0) returnBuffer.append("\r\n\tnumClockEventNameToNumber: " + numClockEventNameToNumber);	    
		if (numClockEventNumberToDescription != 0) returnBuffer.append("\r\n\tnumClockEventNumberToDescription: " + numClockEventNumberToDescription);	    
		if (numClockEventNumberToName != 0) returnBuffer.append("\r\n\tnumClockEventNumberToName: " + numClockEventNumberToName);	    
		if (numGetNames != 0) returnBuffer.append("\r\n\tnumGetNames: " + numGetNames);	    
		if (numTimeInSuperCycle != 0) returnBuffer.append("\r\n\tnumTimeInSuperCycle: " + numTimeInSuperCycle);	    
		//if ( != 0) returnBuffer.append("\r\n\t: " + );	    
		if (lastClearedStatistics != null) returnBuffer.append("\r\n\tlastClearedStatistics: " + lastClearedStatistics);
		if (numReportStatistics != 0) returnBuffer.append("\r\n\tnumReportStatistics: " + numReportStatistics);
		returnBuffer.append("\r\n");
		return returnBuffer.toString();
	}
	*/
	
	/**
	 * clear statistics.
	 */
	 /*
	public static void clearStatistics() {
		numAddObserver = 0;
		numClockEvent = 0;
		numClockEventNameToNumber = 0;
		numClockEventNumberToDescription = 0;
		numClockEventNumberToName = 0;
		numClockMulticast = 0;
		num02ClockFromMulticast = 0;
		numDeleteObserver = 0;
		numGetNames = 0;
		numTimeInSuperCycle = 0;
	}
	*/

    /**
     * Verbose debugging when true.
     * 
     * @return true if verbose debug switch on.
     */
    //static public boolean isClassBugs() {
     //   return classBugs;
    //}

    /**
     * Sets verbose debug switch.
     * 
     * @param flag the debug flag
     */
    //static public void setClassBugs(boolean flag) {
     //   classBugs = flag;
    //}
}
