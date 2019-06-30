package net.sf.jukebox.instrumentation;

import java.util.Random;

import org.junit.Test;

public class MarkerTest {

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
}
