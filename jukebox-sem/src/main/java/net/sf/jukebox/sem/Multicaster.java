package net.sf.jukebox.sem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Notification multicaster.
 * <p/>
 * Mutated from {@link EventSemaphore EventSemaphore}/{@link SemaphoreGroup
 * SemaphoreGroup} interaction, now is being used to broadcast the event
 * notification by calling the callback methods.
 * <h2>Improvement plan</h2>
 * The notification mechanism is OK as long as there are no problems when
 * notifying the listeners. If any of them throws an exception, the rest are
 * left without a notification. If any of them deadlocks, the rest are left
 * hanging, as well as the notifying thread.
 * <p/>
 * The proper fix to this problem is the asynchronous multicaster that will
 * process the notifications a) from a different thread b) with the guaranteed
 * delivery. Since this implementation works for many years without any problems
 * (given the limited usage scope), it may be a good idea to just create yet
 * another class to do it.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 1995-2008
 * @see EventListener
 */
public class Multicaster {

  /**
   * Listeners. Every one of them will be called when the notification is
   * requested.
   *
   * @see EventSemaphore#trigger
   * @see #addListener
   * @see #removeListener
   * @see EventListener
   */
  private final Set<EventListener> listenerSet = new HashSet<EventListener>();

  /**
   * Add the listener.
   *
   * @param target The listener.
   *
   * @throws IllegalArgumentException if the target is null.
   */
  public synchronized void addListener(EventListener target) {

    if (target == null) {
      throw new IllegalArgumentException("null listener doesn't make sense");
    }

    // Since the implementation of HashSet doesn't add the element if it
    // is already there, we don't have to check for it

    listenerSet.add(target);
  }

  /**
   * Remove the listener.
   *
   * @param target The listener.
   *
   * @throws IllegalArgumentException if the target is null.
   */
  public synchronized void removeListener(EventListener target) {

    if (target == null) {
      throw new IllegalArgumentException("null argument");
    }

    listenerSet.remove(target);
  }

  /**
   * Notify the listeners about the event. {@code this} will be passed to
   * listeners as the event producer.
   *
   * @param status Status object to tell the listeners.
   */
  public void notifyListeners(Object status) {

    notifyListeners(this, status);
  }

  /**
   * Notify the listeners about the event.
   *
   * @param producer Object which is to appear as an event producer to the
   * listeners.
   * @param status Status object to tell the listeners.
   */
  public void notifyListeners(Object producer, Object status) {

    for (Iterator<EventListener> e = listenerSet.iterator(); e.hasNext();) {

      EventListener target = e.next();

      target.eventNotification(producer, status);
    }
  }
}