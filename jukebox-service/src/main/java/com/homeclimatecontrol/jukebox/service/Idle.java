package com.homeclimatecontrol.jukebox.service;

import com.homeclimatecontrol.jukebox.sem.EventListener;

import java.util.HashMap;

/**
 * The goal of this object is to observe the idle time for another services and
 * shut it down should the idle timeout expire.
 *
 * Note that there's no need to start this service - use the static methods
 * instead.
 * <h2>BugTrack</h2>
 * <dl>
 * <dt>February 23 98
 * <dd>Reworked completely to use the timing out event semaphores instead of
 * polling, as before.
 * </dl>
 * VT: FIXME: For some reason, this class insists on using
 * {@code System.err.println} instead of logging things, as I normally do. Need
 * to remember why.
 *
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a>
 */
public final class Idle extends PassiveService implements EventListener {

    /**
     * Running instance. The only one.
     */
    private static Idle instance = null;

    /**
     * This object is being passed to the client when the idle timeout expires.
     *
     * @see EventListener#eventNotification
     */
    public static final String OUT = "Time is up";

    /**
     * Mapping (client => alarm), to reset/clear alarms.
     */
    private static HashMap<IdleClient, Alarm> idle = new HashMap<IdleClient, Alarm>();

    /**
     * Create the tracker. You can't instantiate the service from outside - this
     * is done to prevent the API user from tinkering with the internals.
     */
    private Idle() {

        // No logger for this, and use the default thread factory.

        super(null, null);
    }

    @Override
    protected void startup() {

    }

    /**
     * Register the client who wants its idle time to be tracked.
     *
     * @param cli The client.
     */
    public static synchronized void register(IdleClient cli) {

        if (instance == null) {

            instance = new Idle();
            // instance.setDaemon(true);
            instance.start();
        }

        Alarm trigger = new Alarm(instance, cli.idleLimit(), cli);
        idle.put(cli, trigger);
    }

    /**
     * Unregister the client.
     *
     * @param cli Client to unregister.
     */
    public static synchronized void unregister(IdleClient cli) {

        try {

            idle.get(cli).clear();
            idle.remove(cli);

            if (idle.size() == 0) {

                instance.stop();
            }

        } catch (NullPointerException ex) {

            // complain( null,LOG_DEBUG,
            // PassiveService.CH_SERVICE,"Idle.unregister: not registered" );

            System.err.println("Idle.unregister: not registered: " + cli);
        }
    }

    /**
     * Reset the idle time for this client.
     *
     * @param cli Client to reset the idle time for.
     */
    public static void reset(IdleClient cli) {

        try {

            idle.get(cli).set();

        } catch (NullPointerException ex) {

            // complain( null,LOG_WARNING,PassiveService.CH_SERVICE,"Idle.reset:
            // not registered" );

            System.err.println("Idle.reset: not registered: " + cli);
        }
    }

    /**
     * Catch the alarm going off.
     *
     * @param producer The alarm instance.
     * @param status The client to notify.
     */
    @Override
    public void eventNotification(Object producer, Object status) {

        IdleClient cli = (IdleClient) status;
        cli.eventNotification(null, OUT);
        unregister(cli);
    }

    @Override
    protected void shutdown() {

        instance = null;
        // complain( LOG_DEBUG,PassiveService.CH_SERVICE,"shutting down" );
    }

    @Override
    public String toString() {

        return "<IdleTracker>";
    }

    @Override
    protected void finalize() throws Throwable {

        logger.debug("finalize");
        super.finalize();
    }
}
