package com.homeclimatecontrol.jukebox.aggregator;

import java.util.concurrent.BlockingQueue;

/**
 * Object implementing scatter/gather, or aggregation algorithm for homogenous {@link Runnable} workers.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2007-2008
 */
abstract public class Worker<Request, Response, ErrorTarget, T extends Throwable> {
    
    protected final Request rq;
    
    public Worker(Request rq) {
        
        if (rq == null) {
            throw new IllegalArgumentException("Request can't be null");
        }
        
        this.rq = rq;
    }

    /**
     * Process the {@link #rq request}.
     * 
     * @param responseQueue Queue to put the result[s] into.
     *
     * @param errors Queue to put the error[s] into.
     *
     * @throws T if things go sour.
     */
    abstract public void process(BlockingQueue<Response> responseQueue, BlockingQueue<RunnableAggregator.Error<ErrorTarget>> errors) throws T;
}
