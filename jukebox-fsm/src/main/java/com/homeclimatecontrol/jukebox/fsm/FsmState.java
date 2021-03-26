package com.homeclimatecontrol.jukebox.fsm;

/**
 * Describes the individual Finite State Machine state.
 *
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a>
 */
public interface FsmState {

    /**
     * @return A short name.
     */
    String getName();

    /**
     * @return Human readable state description.
     */
    String getDescription();
}
