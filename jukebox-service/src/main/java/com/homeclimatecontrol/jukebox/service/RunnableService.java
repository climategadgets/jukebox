package com.homeclimatecontrol.jukebox.service;

import com.homeclimatecontrol.jukebox.sem.EventSemaphore;

/**
 * The object that can be run.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 1995-2008
 * @since J5
 */
public interface RunnableService {

    /**
     * Start the object.
     *
     * @return A semaphore that is triggered when this service is started.
     */
    public EventSemaphore start();
}