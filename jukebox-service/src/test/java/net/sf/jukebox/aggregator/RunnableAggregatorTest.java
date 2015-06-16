package net.sf.jukebox.aggregator;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.TestCase;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2007-2008
 */
public class RunnableAggregatorTest extends TestCase {

    public void testEmptyQueue() {

        RunnableAggregator aggregator = new RunnableAggregator();

        aggregator.process(10, new LinkedBlockingQueue<Runnable>(), null);

        // We simply have to arrive at this point
    }

    public void testProducerScarce() {
        
        for (int count = 0; count < 500; count++) {
            testProducer(100, 10);
        }
    }

    public void testProducerAbundant() {
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

            assertEquals("Wrong count", objectLimit, result.size());

        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }
    }

    public static class Producer implements Runnable {

        private final Collection<Integer> collector;

        public Producer(Collection<Integer> collector) {
            this.collector = collector;
        }

        public void run() {
            collector.add(Integer.valueOf(hashCode()));
        }
    }
}
