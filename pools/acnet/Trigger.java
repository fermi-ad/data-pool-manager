// $Id: Trigger.java,v 1.2 2023/11/01 21:24:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import java.util.List;

public interface Trigger
{
    /**
     * Return a string suitable for a constructor.
     * 
     * @return a string suitable for a constructor
     */
    public String getReconstructionString();

    /**
     * Return a clone of this trigger.
     * 
     * @return a clone of this trigger
     */
    public Object clone();

    /**
     * Return the arming events.
     * 
     * @return the arming events
     * 
     */
    public List<DataEvent> getArmingEvents();

    /**
     * Return the arming delay.
     * 
     * @return the arming delay in microseconds
     * 
     */
    public int getArmDelay();

    /**
     * Inquire if this should be armed immediately.
     * 
     * @return true if armed immediately
     * 
     */
    public boolean isArmImmediately();
}
