package com.homeclimatecontrol.jukebox.util;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class to help solve a problem the ticket for which is long gone, in a more or less elegant way.
 *
 * Probably obsolete by now.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2005-2009
 */
public class CollectionSynchronizer<T> {

    /**
     * Create a copy of the source collection in a thread-safe way.
     *
     * @param source Collection to copy.
     *
     * In order to properly utilize the functionality, the original
     * collection must be protected by {@code synchronized} block.
     *
     * @return Collection containing the same elements as the source
     * collection.
     */
    public Set<T> copy(Set<T> source) {
        
        if (source == null) {
            throw new IllegalArgumentException("source can't be null");
        }
    
        synchronized (source) {
        
            if (source instanceof TreeSet<?>) {
            
                return new TreeSet<T>(source);
                
            } else if (source instanceof HashSet<?>){
            
                return new HashSet<T>(source);
            
            } else {
                
                throw new IllegalStateException("Don't know how to handle " + source.getClass().getName());
            }
        }
    }
}
