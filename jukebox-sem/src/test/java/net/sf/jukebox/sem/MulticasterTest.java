package net.sf.jukebox.sem;

import junit.framework.TestCase;

public class MulticasterTest extends TestCase {

    public void testAdd() {
        
        Multicaster m = new Multicaster();
        
        try {
            
            m.addListener(null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "null listener doesn't make sense", ex.getMessage());
        }
        
        m.addListener(new Listener());
    }
    
    public void testRemove() {
        
        Multicaster m = new Multicaster();
        
        try {
            
            m.removeListener(null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "null argument", ex.getMessage());
        }
        
        m.addListener(new Listener());
    }

    private static class Listener implements EventListener {

        @Override
        public void eventNotification(Object producer, Object event) {
            
        }
        
    }
}
