package com.homeclimatecontrol.jukebox.sem;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ACTTest {

    @Test
    void testLifecycle1() throws InterruptedException, SemaphoreTimeoutException {

        String userObject = "Hi there";
        final ACT act = new ACT(userObject);

        try {

            act.getUserObject();
            fail("Should've failed by now");

        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage())
                    .withFailMessage("Wrong exception message")
                    .isEqualTo("Too early, you have to wait until the operation is complete");
        }

        assertThat(act.isComplete()).isFalse();

        assertThat(act)
                .hasToString("ACT." + Integer.toHexString(act.hashCode()) + "(Hi there:waiting)");

        Runnable r = new Runnable() {

            @Override
            public void run() {

                try {

                    Thread.sleep(100);
                    act.complete(true);

                } catch (InterruptedException ex) {

                    // The trace is irrelevant here
                    fail("Unexpected exception: " + ex.toString());
                }
            }
        };

        new Thread(r).start();

        assertThat(act.waitFor()).isTrue();
        assertThat(act.isComplete()).isTrue();
        assertThat(act.waitFor()).isTrue();

        assertThat(userObject)
                .withFailMessage("Wrong user object")
                .isEqualTo(act.getUserObject());
        assertThat(act.getStatus())
                .withFailMessage("Wrong status")
                .isTrue();

        assertThat(act)
                .hasToString("ACT." + Integer.toHexString(act.hashCode()) + "(Hi there:complete)");
    }

    @Test
    void testLifecycle2() throws InterruptedException, SemaphoreTimeoutException {

        final ACT act = new ACT();
        final String message = "Done!";

        assertThat(act.isComplete()).isFalse();

        assertThat(act)
                .hasToString("ACT." + Integer.toHexString(act.hashCode()) + "(null:waiting)");

        Runnable r = new Runnable() {

            @Override
            public void run() {

                try {

                    Thread.sleep(100);
                    act.complete(true, message);

                } catch (InterruptedException ex) {

                    // The trace is irrelevant here
                    fail("Unexpected exception: " + ex.toString());
                }
            }
        };

        new Thread(r).start();

        assertThat(act.waitFor(150)).isTrue();
        assertThat(act.isComplete()).isTrue();
        assertThat(act.waitFor(150)).isTrue();

        assertThat(act.getUserObject())
                .withFailMessage("Wrong user object")
                .isEqualTo(message);
        assertThat(act.getStatus())
                .withFailMessage("Wrong status")
                .isTrue();

        assertThat(act)
                .hasToString("ACT." + Integer.toHexString(act.hashCode()) + "(Done!:complete)");

        try {
            act.trigger(false);
            fail("Should've failed by now");

        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage())
                    .withFailMessage("Wrong exception message")
                    .isEqualTo("Can't trigger ACT more than once");
        }
    }
}
