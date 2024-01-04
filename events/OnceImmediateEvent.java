// $Id: OnceImmediateEvent.java,v 1.3 2023/09/26 20:52:04 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public class OnceImmediateEvent implements DataEvent
{
	final long createdTime = System.currentTimeMillis();

	@Override
	public boolean equals(Object o)
	{
		return o instanceof OnceImmediateEvent;
	}

	@Override
	public long defaultTimeout()
	{
		return 5000;
	}

	@Override
	public long createdTime()
	{
		return createdTime;
	}

	@Override
	public String toString()
	{
		return "i";
	}

	@Override
	public int ftd()
	{
		return 0;
	}

	@Override
	public void addObserver(DataEventObserver observer)
	{
	}

	public void deleteObserver(DataEventObserver observer) { }
}
