package net.sf.jukebox.instrumentation;


import java.time.Duration;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

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

    private final MeterRegistry meterRegistry;

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
     * Create an instance, link it with the registry, and start the {@link #stopWatch timer}.
     *
     * @param meterRegistry Meter registry to submit samples to.
     *
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     *
     * @param level Level to print marker messages with.
     */
    public Marker(MeterRegistry meterRegistry, String marker, Level level) {

        this.meterRegistry = meterRegistry;

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
     * Create an instance not linked to a meter registry and start the {@link #stopWatch timer}.
     *
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     *
     * @param level Level to print marker messages with.
     */
    public Marker(String marker, Level level) {
        this(null, marker, level);
    }

    /**
     * Create an instance at {@link Level#DEBUG} level, link it with the registry,
     * and start the {@link #stopWatch timer}.
     *
     * @param meterRegistry Meter registry to submit samples to.
     *
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     */
    public Marker(MeterRegistry meterRegistry, String marker) {
        this(meterRegistry, marker, Level.DEBUG);
    }

    /**
     * Create an instance not linked to a meter registry at {@link Level#DEBUG}
     * level and start the {@link #stopWatch timer}.
     *
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     */
    public Marker(String marker) {
        this(null, marker, Level.DEBUG);
    }

    /**
     * Mark the beginning of the timed section.
     */
    protected void printStartMarker() {

        logStartMarker();
        metricsStartMarker();
    }

    private void logStartMarker() {

        StringBuilder sb = new StringBuilder();

        getSignature(sb);
        sb.append(marker).append(") started");

        logger.log(level, sb.toString());
    }

    private void metricsStartMarker() {

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.timer(marker, "type", "marker", "event", "start").record(Duration.ZERO);
    }

    /**
     * Print the diagnostic message and keep going.
     *
     * @param checkpointMessage Message to print.
     *
     * @return Time elapsed since creation of this marker instance, in milliseconds.
     */
    public final long checkpoint(String checkpointMessage) {

        StringBuilder sb = new StringBuilder();

        getSignature(sb);
        sb.append(marker);
        sb.append(") checkpoint '" + checkpointMessage + "' reached at ");

        printTimeMarker(sb, stopWatch);

        logger.log(level, sb.toString());

        long now = stopWatch.getTime();

        if (meterRegistry == null) {
            return now;
        }

        meterRegistry.timer(marker,
                "type", "marker",
                "event", "checkpoint",
                "message", checkpointMessage).record(Duration.ofMillis(now));

        return now;
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

        logClose();
        metricsClose();

        return stopWatch.getTime();
    }

    private void logClose() {

        StringBuilder sb = new StringBuilder();

        getSignature(sb);
        sb.append(marker);
        sb.append(") completed in ");

        printTimeMarker(sb, stopWatch);

        logger.log(level, sb.toString());
    }

    private void metricsClose() {

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.timer(marker, "type", "marker", "event", "end").record(Duration.ofMillis(stopWatch.getTime()));
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
}
