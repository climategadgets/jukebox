package com.homeclimatecontrol.jukebox.sem;

/**
 * Objects which implement this interface will be able to receive the event
 * notification asynchronously, as opposed to the active listening to the
 * event.
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 1995-2008
 * @see Multicaster
 * @see Semaphore
 */
public interface EventListener {
    /**
     * Receive the event notification.
     *
     * This method is called by the entity which wants to deliver the event
     * notification to the object implementing the
     * <code>EventListener</code> interface. <code>Producer</code> is
     * supposedly the object originated the event, and <code>event</code> is
     * supposedly event itself.
     *
     * @param producer The object which generated the event
     * @param event Status notification.
     */
    public void eventNotification( Object producer,Object event );
}