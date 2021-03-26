package com.homeclimatecontrol.jukebox.service;

import com.homeclimatecontrol.jukebox.sem.EventSemaphore;

/**
 * Opposite of {@link RunnableService RunnableService}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 1995-2008
 * @since J5
 */
public interface StoppableService extends RunnableService {

    /**
     * Stop the object.
     *
     * @return The semaphore that gets triggered when the service is stopped.
     */
    public EventSemaphore stop();
}