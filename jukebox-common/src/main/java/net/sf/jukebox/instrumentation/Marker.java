package net.sf.jukebox.instrumentation;


import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.StructuredDataMessage;

/**
 * An object to keep track of time spent on something in a convenient manner.
 *
 * Usage pattern (just like {@link ThreadContext}:
 *
 * {@code
 *
 * ThreadContext.push("whatever");
 * Marker m = new Marker("something complicated");
 *
 * try {
 *     ... do something
 *
 * } finally {
 *
 *     // NOTE: Put m.close() BEFORE NDC.pop(), not after, to preserve the context
 *     m.close();
 *     ThreadContext.pop();
 * }
 *
 * @see https://github.com/home-climate-control/dz/blob/master/dz3-master/dz3-common/src/main/java/net/sf/dz3/instrumentation/Marker.java
 */
public class Marker {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Message to be printed in the log when the marker is closed.
     */
    private final String marker;

    /**
     * Level to print marker messages with.
     */
    private final Level level;

    /**
     * Timekeeper.
     */
    private StopWatch stopWatch;

    /**
     * Redundant invocation preventer.
     */
    private boolean closed = false;

    /**
     * Create an instance and start the {@link #stopWatch timer}.
     *
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     *
     * @param level Level to print marker messages with.
     */
    public Marker(String marker, Level level) {

        if (marker == null) {
            throw new IllegalArgumentException("Marker can't be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("Level can't be null");
        }

        this.marker = marker;
        this.level = level;

        stopWatch = new StopWatch();
        stopWatch.start();

        printStartMarker();
    }

    /**
     * Create an instance at {@link Level#DEBUG DEBUG} level and start the {@link #stopWatch timer}.
     *
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     */
    public Marker(String marker) {

        this(marker, Level.DEBUG);
    }

    private void log(Map<String, String> message) {

        StructuredDataMessage m = new StructuredDataMessage(Integer.toHexString(hashCode()), null, "marker");

        m.putAll(message);
        EventLogger.logEvent(m);
    }

    /**
     * Mark the beginning of the timed section.
     */
    protected void printStartMarker() {

        log(new Builder().append("event", "started").build());
    }

    /**
     * Print the diagnostic message and keep going.
     *
     * @param checkpointMessage Message to print.
     *
     * @return Time elapsed since creation of this marker instance, in milliseconds.
     */
    public final long checkpoint(String checkpointMessage) {

        log(new Builder()
                .append("event", "checkpoint")
                .append("checkpoint", checkpointMessage)
                .append("duration", Long.toString(stopWatch.getTime()))
                .append("elapsed", stopWatch.toString()).build());

        return stopWatch.getTime();
    }

    /**
     * Close the interval and print the diagnostic message.
     *
     * You can only {@code close()} the marker once.
     *
     * @return Time elapsed since creation of this marker instance, in milliseconds.
     */
    public final long close() {

        // VT: NOTE: No need to bother with synchronization, Marker is intended
        // to be used in a single thread

        if (closed) {
            StringBuilder sb = new StringBuilder();

            getSignature(sb);
            sb.append("already closed)");
            throw new IllegalStateException(sb.toString());
        }

        stopWatch.stop();

        closed = true;

        log(new Builder()
                .append("event", "completed")
                .append("duration", Long.toString(stopWatch.getTime()))
                .append("elapsed", stopWatch.toString()).build());

        return stopWatch.getTime();
    }

    protected void printTimeMarker(StringBuilder sb, StopWatch stopWatch) {
        sb.append(stopWatch.getTime()).append(" ms (").append(stopWatch.toString()).append(")");
    }

    /**
     * Get the unique marker signature.
     *
     * @param sb Buffer to append the signature to.
     */
    private void getSignature(StringBuilder sb) {
        sb.append("marker");
        sb.append('@').append(Integer.toHexString(Thread.currentThread().hashCode()));
        sb.append('#').append(Integer.toHexString(hashCode()));
        sb.append(": (");
    }

    protected void finalize() {

        if (!closed) {

            StringBuilder sb = new StringBuilder();

            getSignature(sb);
            sb.append(marker);
            sb.append(") RUNAWAY INSTANCE, NOT close()d");

            logger.error(sb.toString());
        }
    }

    private class Builder {

        private Map<String, String> map = new TreeMap<>();

        public Builder() {
            map.put("marker", marker);
            map.put("thread", Integer.toHexString(Thread.currentThread().hashCode()));
            map.put("instance", Integer.toHexString(Marker.this.hashCode()));
        }

        public Builder append(String key, String value) {
            map.put(key,  value);
            return this;
        }

        public Map<String, String> build() {
            return Collections.unmodifiableMap(map);
        }
    }
}
