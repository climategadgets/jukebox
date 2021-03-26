package com.homeclimatecontrol.jukebox.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.homeclimatecontrol.jukebox.sem.ACT;
import com.homeclimatecontrol.jukebox.sem.SemaphoreGroup;
import com.homeclimatecontrol.jukebox.service.Messenger;

import junit.framework.TestCase;

public class MessengerTest extends TestCase {
    
    private int current = 0;
    private int max = 0;
    
    public void testPool() throws InterruptedException {

        final int poolSize = 10;
        final BlockingQueue<Runnable> messengerQueue = new LinkedBlockingQueue<Runnable>();
        final ThreadPoolExecutor tpe = new ThreadPoolExecutor(poolSize, poolSize, 60L, TimeUnit.SECONDS, messengerQueue);
        SemaphoreGroup done = new SemaphoreGroup();
        
        for (int count = 0; count < poolSize * 10; count++) {
            
            done.add(new Worker().start(tpe));
        }
        
        done.waitForAll();
        
        assertEquals("Wrong max", poolSize, max);
        assertEquals("Wrong current", 0, current);
    }
    
    public void testStart() throws InterruptedException {
        
        ACT done = new Worker().start();
        
        assertTrue("Wrong status upon completion", done.waitFor());
    }

    public void testFail() throws InterruptedException {
        
        ACT done = new Fail().start();
        
        assertFalse("Wrong status upon completion", done.waitFor());
        assertNotNull("Wrong user object", done.getUserObject());
        assertEquals("Wrong user object", Error.class, done.getUserObject().getClass());
        assertEquals("Wrong user object", "Oops", ((Error) done.getUserObject()).getMessage());
    }

    private synchronized void in() {
        
        current++;
        
        if (current > max) {
            max = current;
        }
    }
    
    private synchronized void out() {
        
        current--;
    }

    class Worker extends Messenger {

        @Override
        protected Object execute() throws Throwable {

            in();
            Thread.sleep(100);
            out();
            return null;
        }
    }

    static class Fail extends Messenger {

        @Override
        protected Object execute() throws Throwable {

            throw new Error("Oops");
        }
    }
}
