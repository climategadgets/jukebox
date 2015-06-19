package net.sf.jukebox.sem;

import java.util.Random;

import net.sf.jukebox.util.PackageNameStripper;
import junit.framework.TestCase;

public class EventSemaphoreTest extends TestCase {
    
    private final Random rg = new Random();

    public void testName() {
        
        String name = Integer.toHexString(rg.nextInt());
        
        EventSemaphore sem = new EventSemaphore();
        EventSemaphore semNamed = new EventSemaphore(name);
        EventSemaphore semOwned = new EventSemaphore(this);
        EventSemaphore semOwnedNamed = new EventSemaphore(this, name);
        
        assertEquals("Wrong name", null, sem.getName());
        assertEquals("Wrong name", name, semNamed.getName());
        assertEquals("Wrong name", PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/", semOwned.getName());
        assertEquals("Wrong name", PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/" + name, semOwnedNamed.getName());
        
        assertEquals("Wrong string representation", toString(sem), sem.toString());
        assertEquals("Wrong string representation", toString(semNamed), semNamed.toString());
        assertEquals("Wrong string representation", toString(semOwned), semOwned.toString());
        assertEquals("Wrong string representation", toString(semOwnedNamed), semOwnedNamed.toString());
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
}
