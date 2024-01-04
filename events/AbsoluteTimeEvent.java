// $Id: AbsoluteTimeEvent.java,v 1.3 2023/06/20 20:50:37 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.ConcurrentModificationException;

public class AbsoluteTimeEvent implements DataEvent, DataEventObserver
{
	final long absoluteTime;

	transient DataEvent wakeupEvent = null;

	final long beforeAccuracy;// accuracy in microseconds before the time
	final long afterAccuracy; // accuracy in microseconds after the time
	final String string;

	List<DataEventObserver> observers = null;

	/**
     * An absolute time event.
     * 
	 * @param theTime the event time
	 */
	public AbsoluteTimeEvent(long theTime)
	{
		this(theTime, 0, 0);
	}

	/**
     * An absolute time event.
     * 
	 * @param theTime the event time
	 * @param accuracy time accuracy
	 */
	public AbsoluteTimeEvent(long theTime, long accuracy)
	{
		this(theTime, accuracy, accuracy);
	}

	/**
     * An absolute time event.
     * 
	 * @param theTime the event time
	 * @param before accuracy before time
	 * @param after accuracy after time
	 */
	public AbsoluteTimeEvent(long absoluteTime, long before, long after)
	{
		this.absoluteTime = absoluteTime;
		this.beforeAccuracy = before;
		this.afterAccuracy = after;
		this.string = "a," + absoluteTime + "," + before + "," + after;
	}

	/**
	 * Add a DataEventObserver who is interested in receiving notification when
	 * this AbsoluteTimeEvent occurs.
	 * 
	 * @param observer
	 *            observer to add
	 */
	public void addObserver(DataEventObserver observer)
	{
		if (wakeupEvent == null) {
			long deltaTime = absoluteTime - System.currentTimeMillis();
			if (deltaTime < 0)
				wakeupEvent = new OnceImmediateEvent();
			else
				wakeupEvent = new DeltaTimeEvent(deltaTime, false);
		}
		synchronized (wakeupEvent) {
			if (observers == null)
				observers = new LinkedList<DataEventObserver>();
			observers.add(observer);
			if (observers.size() == 1)
				wakeupEvent.addObserver(this);
		}
	}

	/**
	 * Observed event has fired.
	 * 
	 * @param userEvent
	 *            the requesting event.
	 * @param currentEvent
	 *            the current event.
	 */
	public void update(DataEvent userEvent, DataEvent currentEvent)
	{
		synchronized (wakeupEvent) {
			Iterator<DataEventObserver> all = observers.iterator();
			try {
				for (DataEventObserver next = null; all.hasNext();) {
					next = all.next();
					next.update(this, new AbsoluteTimeEvent(System.currentTimeMillis()));
				}
			} catch (ConcurrentModificationException e) {
			}
			observers.clear();
			wakeupEvent.deleteObserver(this);
		}
	}

	/**
	 * Removes a previously registered DataEventObserver who was interested in
	 * receiving notification when this AbsoluteTimeEvent occurs.
	 * 
	 * @param observer
	 *            observer to remove from notification
	 */
	public void deleteObserver(DataEventObserver observer)
	{
		if (wakeupEvent != null) {
			synchronized (wakeupEvent) {
				if (observers != null) {
					observers.remove(observer);
					if (observers.size() == 0)
						wakeupEvent.deleteObserver(this);
				}
			}
		}
	}

	@Override
	public String toString()
	{
		return string;
	}

	/**
     * Returns the absolute time.
     * 
	 * @return the absolute time
	 */
	public long getAbsoluteTime()
	{
		return absoluteTime;
	}

	/**
     * Returns the accuracy (in microseconds) before time.
     * 
	 * @return the accuracy before time
	 */
	public long getBeforeAccuracy()
	{
		return beforeAccuracy;
	}

	/**
     * Returns the accuracy (in microseconds) after time.
     * 
	 * @return the accuracy after time
	 */
	public long getAfterAccuracy()
	{
		return afterAccuracy;
	}
}

