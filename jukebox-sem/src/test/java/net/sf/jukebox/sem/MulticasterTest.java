package net.sf.jukebox.sem;

import junit.framework.TestCase;

public class MulticasterTest extends TestCase {

    public void testAdd() {
        
        Multicaster m = new Multicaster();
        
        try {
            
            m.addListener(null);
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "null listener doesn't make sense", ex.getMessage());
        }
        
        m.addListener(new Listener());
    }
    
    private static class Listener implements EventListener {

        @Override
        public void eventNotification(Object producer, Object event) {
            
        }
        
    }
}
