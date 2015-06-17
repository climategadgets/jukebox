package net.sf.jukebox.sem;

import junit.framework.TestCase;

public class ACTTest extends TestCase {

    public void testLifecycle1() throws InterruptedException, SemaphoreTimeoutException {
        
        String userObject = "Hi there";
        ACT act = new ACT(userObject);
        
        try {
            
            act.getUserObject();
            fail("Should've failed by now");
            
        } catch (IllegalStateException ex) {
            assertEquals("Wrong exception message", "Too early, you have to wait until the operation is complete", ex.getMessage());
        }
        
        assertFalse(act.isComplete());
        
        assertEquals("Wrong string representation", "ACT." + Integer.toHexString(act.hashCode()) + "(Hi there:waiting)", act.toString());

        act.complete(true);
        
        assertTrue(act.isComplete());

        assertTrue(act.waitFor());
        assertTrue(act.waitFor(100));

        assertEquals("Wrong user object", userObject, act.getUserObject());
        assertTrue("Wrong status", act.getStatus());
        
        assertEquals("Wrong string representation", "ACT." + Integer.toHexString(act.hashCode()) + "(Hi there:complete)", act.toString());
    }

    public void testLifecycle2() throws InterruptedException, SemaphoreTimeoutException {
        
        final ACT act = new ACT();
        final String message = "Done!";
        
        assertFalse(act.isComplete());
        
        assertEquals("Wrong string representation", "ACT." + Integer.toHexString(act.hashCode()) + "(null:waiting)", act.toString());
        
        Runnable r = new Runnable() {

            @Override
            public void run() {
                
                try {
                    
                    Thread.sleep(100);
                    act.complete(true, message);
                    
                } catch (InterruptedException ex) {
                    
                    // The trace is irrelevant here
                    fail("Unexpected exception: " + ex.toString());
                }
            }
        };
        
        new Thread(r).start();

        assertTrue(act.waitFor(150));
        assertTrue(act.isComplete());
        assertTrue(act.waitFor(150));

        assertEquals("Wrong user object", message, act.getUserObject());
        assertTrue("Wrong status", act.getStatus());
        
        assertEquals("Wrong string representation", "ACT." + Integer.toHexString(act.hashCode()) + "(Done!:complete)", act.toString());

        try {
            act.trigger(false);
            fail("Should've failed by now");

        } catch (IllegalStateException ex) {
            assertEquals("Wrong exception message", "Can't trigger ACT more than once", ex.getMessage());
        }
    }
}
