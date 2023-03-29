package com.homeclimatecontrol.jukebox.sem;

import com.homeclimatecontrol.jukebox.util.PackageNameStripper;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class EventSemaphoreTest {

    private static final String WRONG_NAME = "Wrong name";
    private static final String WRONG_STATE = "Wrong state";
    private static final String WRONG_STATUS = "Wrong status";

    private final Random rg = new Random();

    @Test
    void testName() {

        String name = Integer.toHexString(rg.nextInt());

        EventSemaphore sem = new EventSemaphore();
        EventSemaphore semEmptyName = new EventSemaphore("");
        EventSemaphore semNamed = new EventSemaphore(name);
        EventSemaphore semOwned = new EventSemaphore(this);
        EventSemaphore semOwnedNamed = new EventSemaphore(this, name);
        EventSemaphore semOwnedByNullNamed = new EventSemaphore(null, name);

        assertThat(sem.getName()).withFailMessage(WRONG_NAME).isNull();
        assertThat(semEmptyName.getName()).withFailMessage(WRONG_NAME).isEmpty();
        assertThat(semNamed.getName()).withFailMessage(WRONG_NAME).isEqualTo(name);
        assertThat(semOwned.getName()).withFailMessage(WRONG_NAME).isEqualTo(PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/");
        assertThat(semOwnedNamed.getName()).withFailMessage(WRONG_NAME).isEqualTo(PackageNameStripper.stripPackage(getClass().getName()) + "/" + Integer.toHexString(hashCode()) + "/" + name);
        assertThat(semOwnedByNullNamed.getName()).withFailMessage(WRONG_NAME).isEqualTo(PackageNameStripper.stripPackage(semOwnedByNullNamed.getClass().getName()) + "/" + Integer.toHexString(semOwnedByNullNamed.hashCode()) + "/" + name);

        assertThat(sem).hasToString(toString(sem));
        assertThat(semEmptyName).hasToString(toString(semEmptyName));
        assertThat(semNamed).hasToString(toString(semNamed));
        assertThat(semOwned).hasToString(toString(semOwned));
        assertThat(semOwnedNamed).hasToString(toString(semOwnedNamed));
        assertThat(semOwnedByNullNamed).hasToString(toString(semOwnedByNullNamed));
    }

    private String toString(EventSemaphore target) {

        StringBuilder sb = new StringBuilder("(EventSem");
        String name = target.getName();

        if (!"".equals(name)) {

            sb.append("[").append(name).append("]");
        }

        sb.append(".").append(Integer.toHexString(target.hashCode())).append(":").append(target.getStatus()).append(")");

        return sb.toString();
    }

    @Test
    void testPost() {
        testTrigger(true);
    }

    @Test
    void testClear() {
        testTrigger(false);
    }

    private void testTrigger(boolean value) {

        EventSemaphore sem = new EventSemaphore();

        assertThat(sem.canGetStatus()).withFailMessage(WRONG_STATE).isFalse();
        assertThat(sem.isTriggered()).withFailMessage(WRONG_STATE).isFalse();

        assertThat(sem.getStatus()).withFailMessage(WRONG_STATUS).isFalse();
        assertThat(sem.getStatus()).withFailMessage(WRONG_STATUS).isFalse();

        // Overcomplication to improve the test coverage; trigger(value) would've worked just as fine

        if (value) {
            sem.post();
        } else {
            sem.clear();
        }

        assertThat(sem.getStatus()).withFailMessage(WRONG_STATUS).isEqualTo(value);
        assertThat(sem.getStatus()).withFailMessage(WRONG_STATUS).isEqualTo(value);

        assertThat(sem.isTriggered()).withFailMessage(WRONG_STATE).isTrue();

        // The 'triggered' state is already cleared by now
        assertThat(sem.isTriggered()).withFailMessage(WRONG_STATE).isFalse();
        assertThat(sem.canGetStatus()).withFailMessage(WRONG_STATE).isFalse();
    }

    @Test
    void testWaitForWithWait() throws InterruptedException {

        testWaitForWithWait(false);
        testWaitForWithWait(true);
    }

    private void testWaitForWithWait(final boolean value) throws InterruptedException {

        final EventSemaphore sem = new EventSemaphore();

        Runnable r = () -> {

            try {

                Thread.sleep(100);
                sem.trigger(value);

            } catch (InterruptedException e) {

                fail("Unexpected exception: " + e);
            }
        };

        new Thread(r).start();

        sem.waitFor();

        assertThat(sem.getStatus()).withFailMessage(WRONG_STATUS).isEqualTo(value);
    }

    @Test
    void testWaitForNoWait() throws InterruptedException {

        testWaitForNoWait(false);
        testWaitForNoWait(true);
    }

    private void testWaitForNoWait(boolean value) throws InterruptedException {

        EventSemaphore sem = new EventSemaphore();

        sem.trigger(value);
        assertThat(sem.waitFor()).withFailMessage(WRONG_STATUS).isEqualTo(value);
    }

    @Test
    void testWaitForMillisWithWait() throws InterruptedException, SemaphoreTimeoutException {

        testWaitForMillisWithWait(false, 100, 150);
        testWaitForMillisWithWait(true, 100, 150);
    }

    @Test
    void testWaitForMillisWithTimeout() throws InterruptedException {

        Boolean[] values = new Boolean[] { false, true };

        for (var value : values) {

            try {

                testWaitForMillisWithWait(value, 100, 50);
                fail("Should've been off with exception by now");

            } catch (SemaphoreTimeoutException ex) {

                assertThat(ex.getMessage())
                        .withFailMessage("Wrong exception message")
                        .isEqualTo("50");
            }
        }
    }

    private void testWaitForMillisWithWait(final boolean value, final int sleep, final int millis) throws InterruptedException, SemaphoreTimeoutException {

        final EventSemaphore sem = new EventSemaphore();

        Runnable r = () -> {

            try {

                Thread.sleep(sleep);
                sem.trigger(value);

            } catch (InterruptedException e) {

                fail("Unexpected exception: " + e);
            }
        };

        new Thread(r).start();

        sem.waitFor(millis);

        assertThat(sem.getStatus()).withFailMessage(WRONG_STATUS).isEqualTo(value);
    }

    @Test
    void testWaitForMillisNoWait() throws InterruptedException, SemaphoreTimeoutException {

        testWaitForMillisNoWait(false);
        testWaitForMillisNoWait(true);
    }

    private void testWaitForMillisNoWait(boolean value) throws InterruptedException, SemaphoreTimeoutException {

        EventSemaphore sem = new EventSemaphore();

        sem.trigger(value);
        assertThat(sem.waitFor(50)).withFailMessage(WRONG_STATUS).isEqualTo(value);
    }
}
