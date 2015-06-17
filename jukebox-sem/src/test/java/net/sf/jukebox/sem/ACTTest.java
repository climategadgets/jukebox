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
}
