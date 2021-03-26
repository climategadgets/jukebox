package com.homeclimatecontrol.jukebox.sem;

import java.util.Random;

import com.homeclimatecontrol.jukebox.sem.EventSemaphore;
import com.homeclimatecontrol.jukebox.sem.SemaphoreTimeoutException;
import com.homeclimatecontrol.jukebox.util.PackageNameStripper;

import junit.framework.TestCase;

public class EventSemaphoreTest extends TestCase {
    
    private final Random rg = new Random();

    public void testName() {
        
        String name = Integer.toHexString(rg.nextInt());
        
        EventSemaphore sem = new EventSemaphore();
        EventSemaphore semEmptyName = new EventSemaphore("");
        EventSemaphore semNamed = new EventSemaphore(name);
        EventSemaphore semOwned = new EventSemaphore(this);
        EventSemaphore semOwnedNamed = new EventSemaphore(this, name);
        EventSemaphore semOwnedByNullNamed = new EventSemaphore(null, name);
        
        assertEquals("Wrong name", null, sem.getName());
        assertEquals("Wrong name", "", semEmptyName.getName());
        assertEquals("Wrong name", name, semNamed.getName());
        assertEquals("Wrong name", PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/", semOwned.getName());
        assertEquals("Wrong name", PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/" + name, semOwnedNamed.getName());
        assertEquals("Wrong name", PackageNameStripper.stripPackage(semOwnedByNullNamed.getClass().getName()) + "/" + Integer.toHexString(semOwnedByNullNamed.hashCode()) + "/" + name, semOwnedByNullNamed.getName());
        
        assertEquals("Wrong string representation", toString(sem), sem.toString());
        assertEquals("Wrong string representation", toString(semEmptyName), semEmptyName.toString());
        assertEquals("Wrong string representation", toString(semNamed), semNamed.toString());
        assertEquals("Wrong string representation", toString(semOwned), semOwned.toString());
        assertEquals("Wrong string representation", toString(semOwnedNamed), semOwnedNamed.toString());
        assertEquals("Wrong string representation", toString(semOwnedByNullNamed), semOwnedByNullNamed.toString());
    }
    
    private String toString(EventSemaphore target) {
        
        StringBuilder sb = new StringBuilder("(EventSem");
        String name = target.getName();

        if (!"".equals(name)) {

            sb.append("[").append(name).append("]");
        }

        sb.append(".").append(Integer.toHexString(target.hashCode())).append(":").append(target.getStatus()).append(")");

        return sb.toString();
    }
    
    public void testPost() {
        testTrigger(true);
    }

    public void testClear() {
        testTrigger(false);
    }
    
    private void testTrigger(boolean value) {
        
        EventSemaphore sem = new EventSemaphore();
        
        assertFalse("Wrong state", sem.canGetStatus());
        assertFalse("Wrong state", sem.isTriggered());
        
        assertEquals("Wrong status", false, sem.getStatus());
        assertEquals("Wrong status", false, sem.getStatus());

        // Overcomplication to improve the test coverage; trigger(value) would've worked just as fine
        
        if (value) {
            sem.post();
        } else {
            sem.clear();
        }
        
        assertEquals("Wrong status", value, sem.getStatus());
        assertEquals("Wrong status", value, sem.getStatus());

        assertTrue("Wrong state", sem.isTriggered());
        
        // The 'triggered' state is already cleared by now
        assertFalse("Wrong state", sem.isTriggered());
        assertFalse("Wrong state", sem.canGetStatus());
    }
    
    public void testWaitForWithWait() throws InterruptedException {
        
        testWaitForWithWait(false);
        testWaitForWithWait(true);
    }
    
    private void testWaitForWithWait(final boolean value) throws InterruptedException {
        
        final EventSemaphore sem = new EventSemaphore();
        
        Runnable r = new Runnable() {

            @Override
            public void run() {
                
                try {
                
                    Thread.sleep(100);
                    sem.trigger(value);

                } catch (InterruptedException e) {
                    
                    fail("Unexpected exception: " + e.toString());
                }
            }
        };
        
        new Thread(r).start();
        
        sem.waitFor();
        assertEquals("Wrong status", value, sem.getStatus());
    }

    public void testWaitForNoWait() throws InterruptedException {
        
        testWaitForNoWait(false);
        testWaitForNoWait(true);
    }

    private void testWaitForNoWait(boolean value) throws InterruptedException {

        EventSemaphore sem = new EventSemaphore();
        
        sem.trigger(value);
        assertEquals("Wrong status", value, sem.waitFor());
    }

    public void testWaitForMillisWithWait() throws InterruptedException, SemaphoreTimeoutException {
        
        testWaitForMillisWithWait(false, 100, 150);
        testWaitForMillisWithWait(true, 100, 150);
    }
    
    public void testWaitForMillisWithTimeout() throws InterruptedException, SemaphoreTimeoutException {

        Boolean[] values = new Boolean[] { false, true };

        for (int offset = 0; offset < values.length; offset++) {
            
            try {
                
                testWaitForMillisWithWait(values[offset], 100, 50);
                fail("Should've been off with exception by now");
            
            } catch (SemaphoreTimeoutException ex) {
                assertEquals("Wrong exception message", "50", ex.getMessage());
            }
        }
    }
    
    private void testWaitForMillisWithWait(final boolean value, final int sleep, final int millis) throws InterruptedException, SemaphoreTimeoutException {
        
        final EventSemaphore sem = new EventSemaphore();
        
        Runnable r = new Runnable() {

            @Override
            public void run() {
                
                try {
                
                    Thread.sleep(sleep);
                    sem.trigger(value);

                } catch (InterruptedException e) {
                    
                    fail("Unexpected exception: " + e.toString());
                }
            }
        };
        
        new Thread(r).start();
        
        sem.waitFor(millis);
        assertEquals("Wrong status", value, sem.getStatus());
    }

    public void testWaitForMillisNoWait() throws InterruptedException, SemaphoreTimeoutException {
        
        testWaitForMillisNoWait(false);
        testWaitForMillisNoWait(true);
    }

    private void testWaitForMillisNoWait(boolean value) throws InterruptedException, SemaphoreTimeoutException {

        EventSemaphore sem = new EventSemaphore();
        
        sem.trigger(value);
        assertEquals("Wrong status", value, sem.waitFor(50));
    }
}
