package com.homeclimatecontrol.jukebox.aggregator;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2007-2008
 */
class RunnableAggregatorTest {

    @Test
    void testEmptyQueue() {

        RunnableAggregator aggregator = new RunnableAggregator();

        aggregator.process(10, new LinkedBlockingQueue<Runnable>(), null);

        // We simply have to arrive at this point
    }

    @Test
    void testProducerScarce() {

        for (int count = 0; count < 500; count++) {
            testProducer(100, 10);
        }
    }

    @Test
    void testProducerAbundant() {

        // VT: NOTE: This will likely fail on hardware limited platforms, @Ignore it.
        testProducer(100, 1000);
    }

    private void testProducer(int objectLimit, int threadCount) {

        try {

            RunnableAggregator aggregator = new RunnableAggregator();
            BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<Runnable>();

            List<Integer> result = Collections.synchronizedList(new LinkedList<Integer>());

            for (int count = 0; count < objectLimit; count++) {

                requestQueue.put(new Producer(result));
            }

            aggregator.process(threadCount, requestQueue, null);

            assertThat(result.size()).withFailMessage("Wrong count").isEqualTo(objectLimit);

        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }
    }

    public static class Producer implements Runnable {

        private final Collection<Integer> collector;

        public Producer(Collection<Integer> collector) {
            this.collector = collector;
        }

        @Override
        public void run() {
            collector.add(Integer.valueOf(hashCode()));
        }
    }
}
