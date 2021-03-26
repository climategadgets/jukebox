package com.homeclimatecontrol.jukebox.service;

import com.homeclimatecontrol.jukebox.sem.EventListener;

/**
 * Every service which wants to watch for its idle time should implement this
 * interface.
 *
 * When the client idles out, it receives the notification with null producer
 * and the status equal to <code>Idle.OUT</code>.
 * <h3>BugTrack</h3>
 * <dl>
 * <dt>February 23 98
 * <dd>{@code Idle} implementation has been rewritten from scratch, so this
 * interface definition has been changed in such a way that there's no point to
 * preserve the former documentation for it.
 * </dl>
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 1996-1999
 * @see Idle
 * @see Idle#OUT
 * @see Alarm
 * @see com.homeclimatecontrol.jukebox.sem.EventListener#eventNotification
 */
public interface IdleClient extends EventListener {

    /**
     * Idle time limit for this service. Milliseconds is a measurement unit.
     *
     * @return Idle time limit, in milliseconds.
     */
    public long idleLimit();
}