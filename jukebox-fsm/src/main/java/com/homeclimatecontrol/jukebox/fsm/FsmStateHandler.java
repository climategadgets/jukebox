package com.homeclimatecontrol.jukebox.fsm;

import java.util.concurrent.BlockingQueue;

/**
 * The Finite State Machine state handler.
 *
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a>
 */
public interface FsmStateHandler<Tcontext extends FsmContext, Tstate extends FsmState, Tevent extends FsmEvent, Toutput> {

    /**
     * Process the event.
     *
     * @param context Finite state machine context.
     * @param event Event to process.
     * @param outputQueue Queue to put output objects into, if any.
     * @return State FSM must transition itself to. {@code null} means no state change is required.
     *
     * @throws Throwable if things go sour.
     */
    Tstate process(Tcontext context, Tevent event, BlockingQueue<Toutput> outputQueue) throws Throwable;

    /**
     * Enter the state corresponding to this handler.
     *
     * @param context Finite state machine context.
     * @param outputQueue Queue to put output objects into, if any.
     *
     * @throws InterruptedException if the thread was interrupted.
     * @throws Throwable if things go sour.
     */
    void enterState(Tcontext context, BlockingQueue<Toutput> outputQueue) throws InterruptedException, Throwable;

    /**
     * Leave the state corresponding to this handler.
     *
     * @param context Finite state machine context.
     * @param outputQueue Queue to put output objects into, if any.
     *
     * @throws Throwable if things go sour.
     */
    void leaveState(Tcontext context, BlockingQueue<Toutput> outputQueue) throws Throwable;

    /**
     * @return The state this handler is associated with.
     * @param context Finite state machine context.
     */
    Tstate getState(Tcontext context);
}
