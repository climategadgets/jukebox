package net.sf.jukebox.datastream.logger.impl.rrd;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.ThreadContext;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * RRD data logger using <a href="https://github.com/rrd4j/rrd4j">rrd4j</a>.
 *
 * @param <E> Data type to log.
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2019
 */
public final class RrdNativeLogger<E extends Number> extends AbstractRrdLogger<E, File> {

    /**
     * Map to hold the last signal timestamp (RRD overrun prevention).
     */
    private final Map<File, String> signature2timestamp = new TreeMap<File, String>();

    /**
     * Create an instance with no listeners.
     *
     * @param rrdBase Base directory for RRD database files.
     */
    public RrdNativeLogger(File rrdBase) {

        this(null, rrdBase);
    }

    /**
     * Create an instance listening to given data sources.
     *
     * @param producers Data sources to listen to.
     * @param rrdBase Base directory for RRD database files.
     */
    public RrdNativeLogger(Set<DataSource<E>> producers, File rrdBase) {
        super(producers, rrdBase);
    }

    @Override
    protected final synchronized void createChannel(String name,
            String signature, long timestamp) throws IOException {

        ThreadContext.push("createChannel");

        try {

            checkStatus();
            checkSignature(signature);

            // RRD may or may not exist.

            try {

                File rrdFile = new File(getRrdBase(), signature + ".rrd");

                if (!rrdFile.exists()) {

                    logger.info("Creating " + rrdFile);

                    // Have to create it.

                    RrdDef rrdDef = new RrdDef(rrdFile.getAbsolutePath(), 1);

                    {
                        // VT: FIXME: Let's see if this block is still relevant for
                        // the native RRD writer

                        // If the span between now and the timestamp is quite large,
                        // it'll take
                        // quite a long time to complete the first rrdupdate after
                        // the
                        // creation.
                        // Should be fine afterwards, though

                        //command += " --start " + Long.toString(timestamp / 1000 - 600);
                    }


                    // Heartbeat is 90 seconds
                    // No minimum cutoff
                    // No maximum cutoff

                    rrdDef.addDatasource(new DsDef(signature, DsType.GAUGE, 90, Double.NaN, Double.NaN));

                    // 3600 samples of 1 second: 1 hour

                    rrdDef.addArchive(ConsolFun.LAST, 0.5, 1, 3600);
                    rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, 3600);
                    rrdDef.addArchive(ConsolFun.MIN, 0.5, 1, 3600);

                    // 5760 samples of 30 seconds: 3 hours

                    rrdDef.addArchive(ConsolFun.LAST, 0.5, 30, 5760);
                    rrdDef.addArchive(ConsolFun.MAX, 0.5, 30, 5760);
                    rrdDef.addArchive(ConsolFun.MIN, 0.5, 30, 5760);

                    // 13824 samples of 150 seconds: 48 hours

                    rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 5, 13824);
                    rrdDef.addArchive(ConsolFun.MAX, 0.5, 5, 13824);
                    rrdDef.addArchive(ConsolFun.MIN, 0.5, 5, 13824);

                    // 16704 samples of 9000 seconds: 4 years

                    rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 60, 16704);
                    rrdDef.addArchive(ConsolFun.MAX, 0.5, 60, 16704);
                    rrdDef.addArchive(ConsolFun.MIN, 0.5, 60, 16704);

                    rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1440, 50000);
                    rrdDef.addArchive(ConsolFun.MAX, 0.5, 1440, 50000);
                    rrdDef.addArchive(ConsolFun.MIN, 0.5, 1440, 50000);


                    RrdDb.getBuilder().setRrdDef(rrdDef).build();

                    // VT: FIXME: Add a test case to verify this assumption - see FIXME above
                    logger.warn("Created RRD database. First update may take a few minutes, be patient");
                }

                // After all is done: remember where the file is

                consume(signature, name, rrdFile);

            } catch (Throwable t) {

                throw (IOException) new IOException("Unable to create RRD").initCause(t);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Check the channel signature for {@code rrdtool} compliance.
     *
     * @param ignored Signature to check.
     *
     * @exception IllegalArgumentException if the signature doesn't conform to {@code rrdtool}
     * constraints.
     */
    private void checkSignature(String ignored) {

        if (ignored.length() >= 20) {
            throw new IllegalArgumentException("Signature longer than 19 characters will blow up RRD");
        }
    }

    @Override
    protected final synchronized void consume(String signature, DataSample<E> value) {

        ThreadContext.push("consume");

        try {

            checkStatus();

            // Get the RRD responsible for this signal

            File rrd = (File) getRrd(signature);

            if (rrd == null) {

                logger.error("RRD for '" + signature + "' supposed to exist, but doesn't - sample skipped");
                return;
            }

            // VT: NOTE: This assumes that the signal came in with a good
            // timestamp.

            // Time is defined in seconds
            String timestamp = Long.toString(value.timestamp / 1000);

            // Careful, this method has a side effect
            if (haveSampleFor(rrd, timestamp)) {

                // Since RRD is collecting all sorts of measurements (min, max, average),
                // there's no sense in guessing what to do with te signal. Maybe in the future
                // it will be averaged, but for now (according to "worse is better") it's discarded.

                logger.debug("Already have sample @" + timestamp + ", discarded");
                return;
            }

            // Let's doublecheck: even though the sample may be present, its
            // signal value may be NaN

            double signalValue = (value.sample == null)
                    ? Double.NaN
                            : value.sample.doubleValue();

            try (RrdDb rrdDb = getRrd(rrd)) {

                Sample s = rrdDb.createSample();

                s.setTime(Long.parseLong(timestamp));
                s.setValue(signature, signalValue);
                s.update();

            } catch (IOException ex) {
                logger.error("failed to update RRD for " + signature + "(" + rrd + ")", ex);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private RrdDb getRrd(File rrd) throws IOException {

        ThreadContext.push("getRrd");

        try {

            return RrdDb.getBuilder().setPath(rrd.getAbsolutePath()).build();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Check whether {@link #signature2timesamp} already has this timestamp,
     * if not, add it.
     *
     * @param rrd Signature to check against.
     * @param timestamp Timestamp to check.
     *
     * @return {@code true} if there is already a record of this timestamp for this source.
     */
    private boolean haveSampleFor(File rrd, String timestamp) {

        String have = signature2timestamp.get(rrd);

        if (have == null) {

            // Record. This is a side effect.
            signature2timestamp.put(rrd, timestamp);
            return false;
        }

        if (have.equals(timestamp)) {
            return true;
        }

        // Replace. This is a side effect.
        signature2timestamp.put(rrd, timestamp);
        return false;
    }

    @Override
    protected final void shutdown() throws Throwable {
    }

    @Override
    public String getDescription() {
        return "Native RRD logger";
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        JmxDescriptor d = super.getJmxDescriptor();
        return new JmxDescriptor("jukebox", d.name, d.instance,
                "Native RRD logger");
    }

    @Override
    protected void startup2() {
    }
}
