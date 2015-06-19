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
}
