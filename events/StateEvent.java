// $Id: StateEvent.java,v 1.4 2023/12/13 21:08:42 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import gov.fnal.controls.servers.dpm.pools.acnet.Lookup;

public class StateEvent implements DataEvent
{
	public static final byte FLAG_EQUALS = (byte) 1;

	public static final byte FLAG_NOT_EQUALS = (byte) 2;

	public static final byte FLAG_ALL_VALUES = (byte) 4;

	public static final byte FLAG_GREATER_THAN = (byte) 8;

	public static final byte FLAG_LESS_THAN = (byte) 16;


	final String name;

	final int di;

	final int state;

	final long delay;

	private final byte flag;

	public StateEvent(int di, int state, long delay, byte flag)
	{
		this.di = di;
		this.name = name(di);
		this.state = state;
		this.delay = delay;
		this.flag = flag;
	}

	@Override
	public boolean isRepetitive()
	{
		return true;
	}

	/**
	 * Constructor for StateEvent
	 * 
	 * @param deviceName
	 *            ACNET device index (DI)
	 * @param state
	 *            value of 'di'
	 * @param delay
	 *            number of milliseconds to delay from this StateEvent before
	 *            notifying an Observer
	 * @param flag
	 *            special notification flag
	 */
	public StateEvent(String name, int state, long delay, byte flag)
	{
		this.name = name;
		this.di = di(name);
		this.state = state;
		this.delay = delay;
		this.flag = flag;
	}

	/**
	 * Adds a DataEventObserver who is interested in receiving notification when
	 * this StateEvent occurs.
	 * 
	 * @param observer
	 *            DataEventObserver to add
	 * @see #deleteObserver(DataEventObserver)
	 */
	public void addObserver(DataEventObserver observer)
	{
	}

	/**
	 * converts a device index to device name
	 * 
	 * @param di
	 *            ACNET device index
	 * @return converted ACNET device name or null if conversion failed
	 */
	static private String name(int di)
	{
		try {
			return Lookup.getDeviceInfo(di).name;
		} catch (Exception e) {
			return "" + di;
		}
	}

	/**
	 * converts a device name to device index
	 * 
	 * @param name
	 *            ACNET device name
	 * @return ACNET device index
	 */
	private int di(String name)
	{
		try {
			return Lookup.getDeviceInfo(name).di;
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Removes a DataEventObserver who was previously registered to receive
	 * notification when this StateEvent occurred. Note: it is important to use
	 * the StateEvent (or a copy of the original StateEvent) which was used to
	 * make the original request.
	 * 
	 * @param observer
	 *            DataEventObserver to remove
	 * @see #addObserver(DataEventObserver)
	 */
	public void deleteObserver(DataEventObserver observer)
	{
		//stateEventDecoder.deleteObserver(observer, this);
	}

	/**
	 * @return description of string
     * @exception Exception
	 */
	public String description() throws Exception
	{
		throw new Exception("description() not implemented");
		//return StatesDescription.description(DI, state);
	}

	/**
	 * returns the device index 'DI' associated with this StateEvent
	 * 
	 * @return device index
	 */
	public int deviceIndex()
	{
		return di;
	}

	/**
	 * returns the ACNET device name associated with this StateEvent
	 * 
	 * @return ACNET device name
	 */
	public String deviceName()
	{
		return name;
	}

	/**
	 * returns the notification flag associated with this StateEvent
	 * 
	 * @return the notification flag associated with this StateEvent
	 * @see #FLAG_EQUALS
	 * @see #FLAG_NOT_EQUALS
	 * @see #FLAG_ALL_VALUES
	 */
	public byte flag()
	{
		return flag;
	}

	/**
	 * returns the time in nanoseconds after the 'timestamp' that this
	 * StateEvent was created.
     * @param flag
	 * 
	 * @return the time in nanoseconds after timeStamp()
	 * @see #timeStamp()
	 */
	private static String interpretFlag(short flag)
	{
		switch (flag) {
		case FLAG_EQUALS:
			return "=";
		case FLAG_NOT_EQUALS:
			return "!=";
		case FLAG_GREATER_THAN:
			return ">";
		case (FLAG_GREATER_THAN | FLAG_EQUALS):
			return ">=";
		case FLAG_LESS_THAN:
			return "<";
		case (FLAG_LESS_THAN | FLAG_EQUALS):
			return "<=";
		default:
			return "*";
		}
	}

	public int state()
	{
		return state;
	}

	@Override
    public String toString()
	{
        return "s," + di + "," + state + "," + delay + "," + interpretFlag(flag);
    }
}
