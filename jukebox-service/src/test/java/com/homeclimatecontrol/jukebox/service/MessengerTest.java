package com.homeclimatecontrol.jukebox.service;

import com.homeclimatecontrol.jukebox.sem.ACT;
import com.homeclimatecontrol.jukebox.sem.SemaphoreGroup;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MessengerTest {

    private int current = 0;
    private int max = 0;

    @Test
    void testPool() throws InterruptedException {

        final int poolSize = 10;
        final BlockingQueue<Runnable> messengerQueue = new LinkedBlockingQueue<Runnable>();
        final ThreadPoolExecutor tpe = new ThreadPoolExecutor(poolSize, poolSize, 60L, TimeUnit.SECONDS, messengerQueue);
        SemaphoreGroup done = new SemaphoreGroup();

        for (int count = 0; count < poolSize * 10; count++) {

            done.add(new Worker().start(tpe));
        }

        done.waitForAll();

        assertThat(max)
                .withFailMessage("Wrong max")
                .isEqualTo(poolSize);
        assertThat(current)
                .withFailMessage("Wrong current")
                .isZero();
    }

    @Test
    void testStart() throws InterruptedException {

        ACT done = new Worker().start();

        assertThat(done.waitFor())
                .withFailMessage("Wrong status upon completion")
                .isTrue();
    }

    @Test
    void testFail() throws InterruptedException {

        ACT done = new Fail().start();

        assertThat(done.waitFor())
                .withFailMessage("Wrong status upon completion")
                .isFalse();
        assertThat(done.getUserObject())
                .withFailMessage("Wrong user object")
                .isNotNull();

        assertThat(done.getUserObject().getClass())
                .withFailMessage("Wrong user object")
                .isEqualTo(Error.class);
        assertThat(((Error) done.getUserObject()).getMessage())
                .withFailMessage("Wrong user object")
                .isEqualTo("Oops");
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
