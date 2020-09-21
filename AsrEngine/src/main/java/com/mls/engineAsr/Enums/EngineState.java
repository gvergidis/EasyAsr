package com.mls.engineAsr.Enums;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({EngineState.UNDEFINED, EngineState.IDLE, EngineState.LISTENING, EngineState.REQUESTED})
public @interface EngineState {
    /**
     * This is the initial state of an engine. This state actually defines that engine is currently
     * un-initialized or not yet constructed. You are not permitted to perform any action at this
     * state other that configure or initialize the engine.
     */
    String UNDEFINED = "UNDEFINED";
    /**
     * Defines that engine is currently idle. This means that engine neither listens to user voice
     * neither is requested to do so. In this state, you are free to to request a speech recognition
     * session.
     */
    String IDLE = "IDLE";
    /**
     * This is the state that is between {@link EngineState#IDLE} and {@link EngineState#LISTENING}.
     * At this point, engine has been requested to start recognizing user's voice and we are
     * awaiting for any engine callback or response.
     */
    String REQUESTED = "REQUESTED";
    /**
     * Defines that engine is currently listening to user's voie and streams partial and final
     * results. This state also means that request was successful and engine managed to start.
     */
    String LISTENING = "LISTENING";
}
