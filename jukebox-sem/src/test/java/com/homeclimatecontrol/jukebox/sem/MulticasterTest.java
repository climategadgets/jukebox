package com.homeclimatecontrol.jukebox.sem;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class MulticasterTest {

    private final Random rg = new Random();

    @Test
    void testAdd() {

        Multicaster m = new Multicaster();

        try {

            m.addListener(null);
            fail("Should've failed by now");

        } catch (IllegalArgumentException ex) {

            assertThat(ex.getMessage())
                    .withFailMessage("Wrong exception message")
                    .isEqualTo("null listener doesn't make sense");

//            assertEquals("Wrong exception message", "null listener doesn't make sense", ex.getMessage());
        }

        AtomicInteger count = new AtomicInteger();
        Listener l = new Listener(count);

        m.addListener(l);

        Integer producer = rg.nextInt();
        int event = rg.nextInt();

        m.notifyListeners(producer, event);

        assertThat(count.intValue()).withFailMessage("Wrong count").isEqualTo(1);
        assertThat(l.producer).withFailMessage("Wrong producer").isEqualTo(producer);
        assertThat(l.event).withFailMessage("Wrong event").isEqualTo(event);

//        assertEquals("Wrong count", 1, count.intValue());
//        assertSame("Wrong producer", producer, l.producer);
//        assertEquals("Wrong event", event, l.event);
    }

    @Test
    void testRemove() {

        Multicaster m = new Multicaster();

        try {

            m.removeListener(null);
            fail("Should've failed by now");

        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage())
                    .withFailMessage("Wrong exception message")
                    .isEqualTo("null argument");
//            assertEquals("Wrong exception message", "null argument", ex.getMessage());
        }

        AtomicInteger count = new AtomicInteger();
        Listener l = new Listener(count);

        m.addListener(l);

        Integer producer1 = rg.nextInt();
        int event1 = rg.nextInt();

        {
            // The listener is now connected and is affected by notifyListeners()


            m.notifyListeners(producer1, event1);

            assertThat(count.intValue()).withFailMessage("Wrong count").isEqualTo(1);
            assertThat(l.producer).withFailMessage("Wrong producer").isEqualTo(producer1);
            assertThat(l.event).withFailMessage("Wrong event").isEqualTo(event1);

//            assertEquals("Wrong count", 1, count.intValue());
//            assertSame("Wrong producer", producer1, l.producer);
//            assertEquals("Wrong event", event1, l.event);
        }

        m.removeListener(l);

        {
            // The listener is now disconnected and should not be affected

            Integer producer2 = rg.nextInt();
            int event2 = rg.nextInt();

            m.notifyListeners(producer2, event2);

            assertThat(count.intValue()).withFailMessage("Wrong count").isEqualTo(1);
            assertThat(l.producer).withFailMessage("Wrong producer").isEqualTo(producer1);
            assertThat(l.event).withFailMessage("Wrong event").isEqualTo(event1);

//            assertEquals("Wrong count", 1, count.intValue());
//            assertSame("Wrong producer", producer1, l.producer);
//            assertEquals("Wrong event", event1, l.event);
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
