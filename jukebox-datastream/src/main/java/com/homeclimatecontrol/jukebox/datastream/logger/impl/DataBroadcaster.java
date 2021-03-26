package com.homeclimatecontrol.jukebox.datastream.logger.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.ThreadContext;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.logger.LogAware;
import com.homeclimatecontrol.jukebox.util.CollectionSynchronizer;

/**
 * A data source. An entity capable of producing a {@link DataSample data sample}.
 *
 * @param <E> Data type to handle.
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2009-2018
 */
public class DataBroadcaster<E> extends LogAware implements DataSource<E> {

    private static String invocationError = "Consumer invocation resulted in exception, there's nothing we can do to fix it";

    private final Set<DataSink<E>> consumerSet = new HashSet<DataSink<E>>();
    private final boolean async;
    
    public DataBroadcaster() {
        
        // Hunting down the deadlock
        String property = System.getProperty(getClass().getName()+ ".async");
        
        logger.info("Async: '" + property + "'");
        async = property != null;    
    }
    
    public synchronized void addConsumer(DataSink<E> consumer) {

        ThreadContext.push("addConsumer");

        try {
            
            // Reason for the synchronized scope (and not method) is the collection synchronizer
            synchronized (consumerSet) {
            
                consumerSet.add(consumer);
                logger.debug("Added: " + consumer);
            }
            
        } finally {
            ThreadContext.pop();
        }
    }

    public synchronized void removeConsumer(DataSink<E> consumer) {
        
        ThreadContext.push("removeConsumer");
        
        try {
            
            // Reason for the synchronized scope (and not method) is the collection synchronizer
            synchronized (consumerSet) {
            
                consumerSet.remove(consumer);
                logger.debug("Removed: " + consumer);
            }
            
        } finally {
            ThreadContext.pop();
        }
    }
    
    public void broadcast(final DataSample<E> signal) {
        
        // Need the hash code to uniquely identify the broadcast invocation in the log
        ThreadContext.push("broadcast#" + Integer.toHexString(signal.hashCode()));

        try {
            
            logger.trace(signal);
            
            Set<DataSink<E>> copy = new CollectionSynchronizer<DataSink<E>>().copy(consumerSet);

            for (Iterator<DataSink<E>> i = copy.iterator(); i.hasNext();) {
                
                final DataSink<E> dataSink = i.next();
                
                if (async) { 

                    Runnable agent = new Runnable() {

                        @Override
                        public void run() {
                            
                            ThreadContext.push("execute");
                            
                            try {

                                // dataSink.toString() may be expensive
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Feeding: " + dataSink);
                                }
                                
                                dataSink.consume(signal);

                            } catch (Throwable t) {

                                logger.warn(invocationError, t);

                            } finally {
                                ThreadContext.pop();
                            }
                        }
                    };

                    // VT: FIXME: https://github.com/climategadgets/jukebox/issues/1

                    new Thread(agent).start();

                } else {
                    
                    try {
                        
                        // dataSink.toString() may be expensive
                        if (logger.isTraceEnabled()) {
                            logger.trace("Feeding: " + dataSink);
                        }
                        
                        dataSink.consume(signal);
                        
                    } catch (Throwable t) {
                        logger.warn(invocationError, t);
                    }
                }
            }
            
        } finally {
            
            ThreadContext.pop();
        }
    }
}
