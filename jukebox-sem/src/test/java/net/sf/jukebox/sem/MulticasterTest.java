package net.sf.jukebox.sem;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class MulticasterTest extends TestCase {
    
    private final Random rg = new Random();

    public void testAdd() {
        
        Multicaster m = new Multicaster();
        
        try {
            
            m.addListener(null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "null listener doesn't make sense", ex.getMessage());
        }
        
        AtomicInteger count = new AtomicInteger();
        Listener l = new Listener(count);

        m.addListener(l);
        
        Integer producer = rg.nextInt();
        int event = rg.nextInt();
        
        m.notifyListeners(producer, event);
        
        assertEquals("Wrong count", 1, count.intValue());
        assertSame("Wrong producer", producer, l.producer);
        assertEquals("Wrong event", event, l.event);
    }
    
    public void testRemove() {
        
        Multicaster m = new Multicaster();
        
        try {
            
            m.removeListener(null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "null argument", ex.getMessage());
        }

        AtomicInteger count = new AtomicInteger();
        Listener l = new Listener(count);
        
        m.addListener(l);

        Integer producer1 = rg.nextInt();
        int event1 = rg.nextInt();

        {
            // The listener is now connected and is affected by notifyListeners()
            
            
            m.notifyListeners(producer1, event1);
    
            assertEquals("Wrong count", 1, count.intValue());
            assertSame("Wrong producer", producer1, l.producer);
            assertEquals("Wrong event", event1, l.event);
        }
        
        m.removeListener(l);
        
        {
            // The listener is now disconnected and should not be affected

            Integer producer2 = rg.nextInt();
            int event2 = rg.nextInt();

            m.notifyListeners(producer2, event2);
    
            assertEquals("Wrong count", 1, count.intValue());
            assertSame("Wrong producer", producer1, l.producer);
            assertEquals("Wrong event", event1, l.event);
        }
    }

    private static class Listener implements EventListener {

        private final AtomicInteger count;
        public Object producer;
        public Object event;
        
        public Listener(AtomicInteger count) {
            
            this.count = count;
        }
        
        @Override
        public void eventNotification(Object producer, Object event) {
            
            this.producer = producer;
            this.event = event;
            
            count.incrementAndGet();
        }
    }
}
