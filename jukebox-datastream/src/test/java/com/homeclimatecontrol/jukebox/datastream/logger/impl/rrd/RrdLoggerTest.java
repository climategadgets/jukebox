package com.homeclimatecontrol.jukebox.datastream.logger.impl.rrd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.rrd.RrdLogger;


public class RrdLoggerTest {
    
    private static final File tmpDir = new File(System.getProperty("java.io.tmpdir"));

    @Test
    public void testSetNull() {
        
        try {
            
            new RrdLogger<Double>(tmpDir, null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "target can't be null", ex.getMessage());
        }
    }

    @Test
    public void testSetDirectory() {
        
        try {
            
            new RrdLogger<Double>(tmpDir, tmpDir);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", tmpDir.getAbsolutePath()
                    + ": doesn't exist, unreadable, not executable or not a regular file", ex.getMessage());
        }
    }

    @Test
    public void testSetNotExecutable() throws IOException {
        
        File empty = File.createTempFile("test", "null", tmpDir);
        
        empty.deleteOnExit();
        
        try {
            
            new RrdLogger<Double>(tmpDir, empty);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", empty.getAbsolutePath()
                    + ": doesn't exist, unreadable, not executable or not a regular file", ex.getMessage());
        }
    }

    @Test
    public void testSetNotAbsolute() throws IOException {
        
        try {
            
            new RrdLogger<Double>(tmpDir, new File("../rrdtool"));
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertTrue("Wrong exception message", ex.getMessage().contains("only absolute locations are acceptable"));
        }
    }
}
