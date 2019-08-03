package net.sf.jukebox.instrumentation;

import java.util.Random;

import org.junit.Test;

import io.micrometer.core.instrument.Clock;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

public class MarkerTest {

    /**
     * Test a simple marker.
     *
     * There are no assertions, you will have to examine the output to make sure it works correctly.
     */
    @Test
    public void testMarker() throws InterruptedException {

        Marker m = new Marker("test");
        Random rg = new Random();

        Thread.sleep(rg.nextInt(100));
        m.checkpoint("checkpoint1");

        Thread.sleep(rg.nextInt(100));
        m.checkpoint("checkpoint2");

        Thread.sleep(rg.nextInt(100));
        m.close();
    }

    /**
     * Test a marker with metrics.
     *
     * There are no assertions, you will have to examine the monitoring system
     * databases to make sure it works correctly.
     */
    @Test
    public void testMarkerWithMetrics() throws InterruptedException {

        InfluxConfig cf = new InfluxConfig() {
            @Override
            public String db() {
                return "jukebox-test";
            }

            @Override
            public String get(String key) {
                // Accept the defaults
                return null;
            }
        };

        InfluxMeterRegistry r = new InfluxMeterRegistry(cf, Clock.SYSTEM);

        Marker m = new Marker(r, "test-metrics");
        Random rg = new Random();

        Thread.sleep(rg.nextInt(100));
        m.checkpoint("checkpoint1");

        Thread.sleep(rg.nextInt(100));
        m.checkpoint("checkpoint2");

        Thread.sleep(rg.nextInt(100));
        m.close();

        r.close();
    }
}
