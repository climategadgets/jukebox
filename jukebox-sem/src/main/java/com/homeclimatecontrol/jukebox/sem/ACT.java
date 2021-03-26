package com.homeclimatecontrol.jukebox.sem;

/**
 * Asynchronous Completion Token.
 *
 * A.k.a. "Magic Cookie". Associates state object with the completion of
 * asynchronous operation.
 *
 * In addition, this implementation provides the asynchronous notification
 * and optional callback.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 1995-2008
 */
public class ACT extends EventSemaphore {

    /**
     * Object containing the state information.
     */
    protected Object userObject;

    /**
     * Create a new ACT.
     */
    public ACT() {
        this(null);
    }

    /**
     * Create a new ACT with a specified user object.
     *
     * @param userObject Object containing the state information.
     */
    public ACT(Object userObject) {
        this.userObject = userObject;
    }

    /**
     * Check for completion.
     *
     * @return true if the operation has been completed.
     */
    public boolean isComplete() {
        return triggered;
    }

    /**
     * Unconditionally wait for the operation completion forever, once.  If
     * the semaphore had been triggered, just return the status.
     *
     * @return true if the operation has completed successfully, false if
     *         not.
     *
     * @throws InterruptedException if this thread was interrupted.
     */
    @Override
    public boolean waitFor() throws InterruptedException {

        if (isComplete()) {
            return status;
        }

        return super.waitFor();
    }

    /**
     * Wait for the operation completion for the specified amount of time,
     * once.  If the semaphore had been triggered, just return the status.
     *
     * @param millis Time to wait, milliseconds.
     *
     * @return true if the operation has completed successfully, false if
     *         not.
     *
     * @throws InterruptedException if this thread was interrupted by
     * another thread.
     * @throws SemaphoreTimeoutException If the timeout has expired.
     */
    @Override
    public boolean waitFor(long millis) throws InterruptedException, SemaphoreTimeoutException {

        if (isComplete()) {
            return status;
        }

        return super.waitFor(millis);
    }

    /**
     * Notify the observers about the operation completion.
     *
     * @param status Completion status (true - success, false - failure).
     *
     * @throws IllegalStateException if you attempt to trigger the ACT
     * completion more than once.
     */
    public synchronized void complete(boolean status) {

        trigger(status);
    }

    /**
     * Notify the observers about the operation completion, and pass the
     * result object to them.
     *
     * @param status Completion status (true - success, false - failure).
     * @param message Whatever message you want to pass to the observers,
     * usually the operation result.
     *
     * @throws IllegalStateException if you attempt to trigger the ACT
     * completion more than once.
     */
    public synchronized void complete(boolean status, Object message) {

        userObject = message;
        trigger(status);
    }

    /**
     * Trigger the completion.
     *
     * For the clarity reasons, shouldn't be used directly - use {@link
     * #complete(boolean) complete()} instead.
     *
     * @param status true if successful.
     *
     * @throws IllegalStateException if you attempt to trigger the ACT
     * completion more than once.
     */
    @Override
    public synchronized void trigger(boolean status) {

        if (isComplete()) {
            throw new IllegalStateException("Can't trigger ACT more than once");
        }

//        logger.debug("ACT", "complete/" + status);
        super.trigger(status);
    }

    /**
     * Get the user object associated with this ACT.
     *
     * @return The user object.
     *
     * @throws IllegalStateException if this method was called before the
     * operation completion.
     */
    public Object getUserObject() {

        if (!isComplete()) {
            throw new IllegalStateException("Too early, you have to wait until the operation is complete");
        }

        return userObject;
    }

    @Override
    public String toString() {

        StringBuilder buf = new StringBuilder("ACT.");
        buf.append(Integer.toHexString(hashCode()));
        buf.append("(");

        if (userObject != null) {
            buf.append(userObject.toString());
        } else {
            buf.append("null");
        }

        buf.append(":");
        buf.append(isComplete() ? "complete" : "waiting");
        buf.append(")");
        return buf.toString();
    }
}
