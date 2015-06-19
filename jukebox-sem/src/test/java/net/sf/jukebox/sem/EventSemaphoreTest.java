package net.sf.jukebox.sem;

import net.sf.jukebox.util.PackageNameStripper;
import junit.framework.TestCase;

public class EventSemaphoreTest extends TestCase {

    public void testName() {
        
        String name = Integer.toHexString(hashCode());
        Semaphore semNamed = new EventSemaphore(name);
        Semaphore semOwned = new EventSemaphore(this);
        Semaphore semOwnedNamed = new EventSemaphore(this, name);
        
        assertEquals("Wrong name", name, semNamed.getName());
        assertEquals("Wrong name", PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/", semOwned.getName());
        assertEquals("Wrong name", PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/" + name, semOwnedNamed.getName());
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
}
