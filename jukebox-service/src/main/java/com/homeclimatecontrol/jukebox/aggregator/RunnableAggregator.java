package com.homeclimatecontrol.jukebox.aggregator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.LogManager;

/**
 * Object implementing scatter/gather, or aggregation algorithm for trivial {@link Runnable} workers.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2007-2018
 */
public class RunnableAggregator {
    
    protected final Logger logger = LogManager.getLogger(getClass());
    
    /**
     * Process the queue with the number of threads equal to the queue size.
     * 
     * @param workerQueue Queue of workers to run.
     * 
     * @param errors Queue containing errors, if any. If this queue is null on entry, no error reporting will be available
     * other than via {@link #logger}.
     * 
     * @see #process(BlockingQueue, BlockingQueue)
     */
    public void process(BlockingQueue<Runnable> workerQueue, BlockingQueue<Error<Runnable>> errors) {
        
        if (workerQueue == null) {
            throw new IllegalArgumentException("workerQueue can't be null");
        }
        
        if (workerQueue.isEmpty()) {
            logger.debug("Empty workerQueue");
            return;
        }
        
        process(workerQueue.size(), workerQueue, errors);
    }
    
    /**
     * Process the queue.
     * 
     * @param threadCount Number of threads to dedicate to processing.
     * 
     * @param workerQueue Queue of workers to run.
     * 
     * @param errors Queue containing errors, if any. If this queue is null on entry, no error reporting will be available
     * other than via {@link #logger}.
     */
    public void process(final int threadCount, BlockingQueue<Runnable> workerQueue, BlockingQueue<Error<Runnable>> errors) {
        
        ThreadContext.push("process"
                + '@' + Integer.toHexString(Thread.currentThread().hashCode())
                + ',' + Integer.toHexString(hashCode()));

        try {

            check(threadCount, workerQueue);

            final BlockingQueue<Runnable> spoolQueue = new LinkedBlockingQueue<Runnable>();
            final ThreadPoolExecutor tpe = new ThreadPoolExecutor(threadCount, threadCount + 1, 60L, TimeUnit.SECONDS, spoolQueue);
            
            // Need to freeze the queue
            BlockingQueue<Runnable> processQueue = new LinkedBlockingQueue<Runnable>(workerQueue);
            
            // This is not redundant, I'll need it later
            int queueSize = processQueue.size();

            // Need this to wait until all submitted tasks are processed
            final Semaphore queueGate = new Semaphore(queueSize);
            
            try {
                
                while (!processQueue.isEmpty()) {
                    final Runnable worker = processQueue.take();
                    
                    queueGate.acquire();
                    
                    tpe.execute(new QueueWorker(queueGate, worker, errors));
                }
                
                // By this time, permits for all the queue elements have already been issued.
                // Since we have to wait until *everyone* is finished with the job, we'll just
                // acquire all of them to make sure that is true

                queueGate.acquire(queueSize);
                
            } catch (InterruptedException ex) {
                
                logger.debug("Interrupted, stopping", ex);

                // There's nothing left to do other than shutdown normally
            }

            shutdown(tpe, queueSize, queueGate, processQueue);

        } finally {
            ThreadContext.pop();
        }
    }
    
    private void shutdown(ThreadPoolExecutor tpe, int queueSize, Semaphore queueGate, BlockingQueue<Runnable> processQueue) {

        ThreadContext.push("shutdown");

        try {

            logger.debug("shutting down");

            tpe.shutdown();

            final int processed = queueGate.availablePermits();

            // "In transit" jobs are those for which permits were issued, but not released.
            // They're still queued up inside of thread pool executor, and will be executed
            // as usual, until the internal queue is exhausted.
            final int inTransit = queueSize - processed;

            // Discarded jobs are those that were never taken out of processQueue
            final int discarded = processQueue.size();

            logger.info("Processed " + processed + ", inTransit " + inTransit + ", discarded " + discarded);

            cancel(processQueue);

            logger.debug("done");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure the values are sane.
     * 
     * @param threadCount Thread count given to {@link #process(threadCount, requestQueue, responseQueue)}.
     * @param requestQueue Request queue given to {@link #process(threadCount, requestQueue, responseQueue)}.
     * 
     * @throws IllegalArgumentException if arguments are invalid (see the code for details).
     */
    private void check(int threadCount, BlockingQueue<Runnable> requestQueue) {
        
        if (threadCount < 1) {
            throw new IllegalArgumentException("Unreasonable threadCount (" + threadCount + ')');
        }

        if (requestQueue == null) {
            throw new IllegalArgumentException("requestQueue can't be null");
        }
    }
    
    private void cancel(BlockingQueue<Runnable> processQueue) {
        ThreadContext.push("cleanup");
        try {
            logger.warn("Queue cleanup: " + processQueue.size() + " workers to take care of");
            
            while (!processQueue.isEmpty()) {
                Runnable worker = processQueue.take();
                try {

                    if (worker instanceof SafeRunnable) {
                        ((SafeRunnable)worker).cancel();
                    }
                    
                } catch (Throwable t) {
                    logger.warn("Failed to clean up: " + worker, t);
                }
            }
        } catch (InterruptedException ex) {
            logger.warn("Failed to clean up the rest of the queue: " + processQueue, ex);
        } finally {
            ThreadContext.pop();
        }
    }
    
    private class QueueWorker implements Runnable {
        
        private final Semaphore queueGate;
        private final Runnable worker;
        private final BlockingQueue<Error<Runnable>> errors;
        
        public QueueWorker(Semaphore queueGate, Runnable worker, BlockingQueue<Error<Runnable>> errors) {
            
            this.queueGate = queueGate;
            this.worker = worker;
            this.errors = errors;
        }

        public void run() {
            
        ThreadContext.push("run"
                + '@' + Integer.toHexString(Thread.currentThread().hashCode())
                + ',' + Integer.toHexString(hashCode()));
        
            try {

                worker.run();
                
            } catch (Throwable t) {
                
                if (errors == null) {
                    
                    // No other reporting is available
                    logger.error("Worker failed", t);
                
                } else {
                    
                    try {

                        // Careful, we're not logging anything in this case
                        errors.put(new Error<Runnable>(worker, t));
                        
                    } catch (InterruptedException ex) {
                        
                        ThreadContext.push("oops");
                        
                        try {
                            
                            logger.error("Interrupted while reporting an error for " + worker);
                            logger.error("Original problem:", t);
                            logger.error("Interruption caused by exception", ex);
                            
                        } finally {
                            ThreadContext.pop();
                        }
                    }
                }
                
            } finally {
                queueGate.release();
                ThreadContext.pop();
            }
        }
    }
    
    public static class Error<T> {

        public final T target;
        public final Throwable cause;
        
        public Error(T target, Throwable cause) {
            this.target = target;
            this.cause = cause;
        }
    }
}
