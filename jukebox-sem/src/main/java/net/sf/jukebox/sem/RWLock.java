package net.sf.jukebox.sem;

import java.util.LinkedList;

/**
 * Multiple reader, single writer lock. Loosely based on the implementation from
 * <a href="http://java.apache.org/">Apache JServ</a> ({@code org.apache.java.lang.Lock}
 * class), with the difference that the JServ implementation provides the
 * anonymous access, but this one keeps track of the individual locks.
 * <p>
 * Benefit: accountability. Stupid programmers wouldn't be able to hang your
 * system and make everyone else hunt for their bug for months.
 * <p>
 * Penalty: performance, of course. Hopefully, not too much.
 * <p>
 * <strong>NOTE:</strong> since the writers have a strong priority over
 * readers, the resource starvation is quite possible. Brief test shows that it
 * happens when the number of writers reaches one third of number of readers,
 * with the consumption patterns identical for readers and writers. Caveat
 * emptor.
 * <p>
 * Implementation note: {@link #activeReaders activeReaders} and
 * {@link #activeWriters activeWriters} are actually sets, not lists. However,
 * they are ordered sets in order to provide FIFO order for acquiring and
 * releasing tokens, to support any kind of fairness. The addition pattern is
 * such that only new tokens are added, and the removal pattern is that the
 * element at the head is removed, so let them be lists.
 *
 * @author Copyright &copy; 1997-1999 <a href="http://java.apache.org">Java
 * Apache Project</a>
 * @author Modified <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a>
 * 2000-2005
 * @version $Id: RWLock.java,v 1.2 2007-06-14 04:32:18 vtt Exp $
 */
public class RWLock {

    /**
     * Constant to designate a read lock token.
     */
    private static String READ = "Read";

    /**
     * Constant to designate a write lock token.
     */
    private static String WRITE = "Write";

    /**
     * Number of currently waiting read locks.
     */
    private int waitingReadLocks = 0;

    /**
     * Number of currently waiting write locks.
     */
    private int waitingWriteLocks = 0;

    /**
     * Set of active readers.
     */
    private LinkedList<LockToken> activeReaders = new LinkedList<LockToken>();

    /**
     * Set of active writers.
     */
    private LinkedList<LockToken> activeWriters = new LinkedList<LockToken>();

    /**
     * Thread that currently owns the write lock.
     */
    private Thread lockedThread = null;

    /**
     * We only allow a reader to lock if there are no write locks, and no
     * waiting writer locks.
     *
     * @return true if it is possible to get a read lock at this point.
     */
    private boolean allowReadLock() {

        return (activeWriters.isEmpty() || (Thread.currentThread() == lockedThread)) && (waitingWriteLocks == 0);
    }

    /**
     * We only allow a writer to lock if there are no active locks, except for
     * the locks originated in the same thread.
     *
     * @return true if it is possible to get a write lock at this point.
     */
    private boolean allowWriteLock() {

        return activeReaders.isEmpty() && (activeWriters.isEmpty() || (Thread.currentThread() == lockedThread));
    }

    /**
     * Wait for a read lock. This will wait for all write lock to be removed
     * before returning.
     *
     * @return The lock token that must be passed to {@link #release release()}
     * method to release the lock.
     * @exception InterruptedException if the wait is interrupted. Calling
     * thread must consider the operation to have failed and stop its
     * processing.
     */
    public synchronized Object getReadLock() throws InterruptedException {

        // Register our intent
        waitingReadLocks++;

        try {

            // Wait for a chance to read
            while (allowReadLock() == false) {

                wait();
            }

            // Bingo! Switch from a waiting lock to an active lock

            LockToken lt = new LockToken(READ);
            activeReaders.add(lt);

            return lt;

        } finally {

            // Remove intent to read
            waitingReadLocks--;
        }
    }

    /**
     * Wait for a read lock. This will wait for all write lock to be removed
     * before returning.
     *
     * @param timeout the number of millisecond before giving up and failing
     * with a SemaphoreTimeoutException.
     * @return The read lock object.
     * @exception SemaphoreTimeoutException if the lock isn't acquired after the
     * specified amount of time.
     * @exception InterruptedException if the wait is interrupted. Calling
     * thread must consider the operation to have failed and stops its
     * processing.
     */
    public synchronized Object getReadLock(long timeout) throws InterruptedException, SemaphoreTimeoutException {

        long waitTill = System.currentTimeMillis() + timeout;

        // Register our intent
        waitingReadLocks++;

        try {

            // Wait for a chance to read
            while (allowReadLock() == false) {

                wait(timeout);

                // Check to see if was have the lock

                if (allowReadLock() == false &&

                (timeout = waitTill - System.currentTimeMillis()) < 0) {
                    // Timeout without obtaining lock.
                    throw new SemaphoreTimeoutException("getReadLock: " + timeout);
                }
            }

            // Bingo! Switch from a waiting lock to an active lock

            LockToken lt = new LockToken(READ);
            activeReaders.add(lt);
            return lt;

        } finally {

            // Remove intent to read.
            waitingReadLocks--;
        }
    }

    /**
     * Wait for a read lock. This will wait until all read lock have been
     * removed and no other write lock are active.
     *
     * @return The lock token that must be passed to {@link #release release()}
     * method to release the lock.
     * @exception InterruptedException if the wait is interrupted. Calling
     * thread must consider the operation to have failed and stops its
     * processing.
     */
    public synchronized Object getWriteLock() throws InterruptedException {

        // Register our intent
        waitingWriteLocks++;

        try {

            // Wait for a chance to write
            while (allowWriteLock() == false) {

                wait();
            }

            // Bingo! Switch from a waiting lock to an active lock

            LockToken lt = new LockToken(WRITE);

            activeWriters.add(0, lt);

            lockedThread = Thread.currentThread();

            return lt;

        } finally {

            // Remove intent lock
            waitingWriteLocks--;
        }
    }

    /**
     * Wait for a write lock. This will wait until all read lock have been
     * removed and no other write lock are active.
     *
     * @param timeout the number of millisecond before giving up and failing
     * with a SemaphoreTimeoutException.
     * @return The lock token that must be passed to {@link #release release()}
     * method to release the lock.
     * @exception SemaphoreTimeoutException if the lock isn't acquired after the
     * specified amount of time.
     * @exception InterruptedException if the wait is interrupted. Calling
     * thread must consider the operation to have failed and stops its
     * processing.
     */
    public synchronized Object getWriteLock(long timeout) throws InterruptedException, SemaphoreTimeoutException {

        long waitTill = System.currentTimeMillis() + timeout;

        // Register our intent
        waitingWriteLocks++;

        try {

            // Wait for a chance to write

            if (allowWriteLock() == false) {

                wait(timeout);

                if (allowWriteLock() == false && (timeout = waitTill - System.currentTimeMillis()) < 0) {

                    // Timeout, we failed to acquire the lock
                    throw new SemaphoreTimeoutException("getWriteLock: " + timeout);
                }
            }

            // Bingo! Switch from a waiting lock to an active lock

            LockToken lt = new LockToken(WRITE);

            activeWriters.add(0, lt);

            lockedThread = Thread.currentThread();

            return lt;

        } finally {

            waitingWriteLocks--;
        }
    }

    /**
     * Unlock a previously acquired lock.
     *
     * @param lockToken The lock token previously granted by the lock.
     * @exception IllegalArgumentException if the parameter is not a lock token
     * previsouly granted, or it has been already released.
     * @return {@code null}, as a syntax sugar. Like this:
     * {@code lockToken = lock.release(lockToken);}
     */
    public synchronized Object release(Object lockToken) {

        if (lockToken == null) {

            // The hell with it, it was probably released before

            return null;
        }

        try {

            LockToken lt = (LockToken) lockToken;

            if (lt == activeWriters.get(0)) {

                // We're gone
                activeWriters.remove(0);

                if (activeWriters.isEmpty()) {

                    lockedThread = null;
                }

            } else {

                if (!activeReaders.contains(lockToken)) {

                    throw new IllegalArgumentException("Not a valid LockToken: '" + lockToken + "'");
                }

                // We're gone
                activeReaders.remove(lockToken);
            }

        } catch (ClassCastException ccex) {

            throw new IllegalArgumentException("Not a LockToken: " + lockToken + "'");

        } finally {

            notifyAll();
        }

        return null;
    }

    /**
     * Lock token. This object is given to whoever requests the lock so
     * non-anonymous access control and deadlock analysis can be performed.
     */
    protected class LockToken implements Comparable<LockToken> {

        /**
         * Either {@link RWLock#READ READ} or {@link RWLock#WRITE WRITE}.
         */
        private String who;

        /**
         * @param who Type of the lock. Must be either {@link RWLock#READ READ} or
         * {@link RWLock#WRITE WRITE}.
         */
        LockToken(String who) {

            this.who = who;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {

            return "(" + who + " Lock Token " + Integer.toHexString(hashCode()) + ")";
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(LockToken other) {

          if (other == null) {
            throw new IllegalArgumentException("other can't be null");
          }

          return toString().compareTo(other.toString());
        }
    }
}