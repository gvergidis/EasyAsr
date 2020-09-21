package com.mls.engineAsr.Interfaces;

import androidx.annotation.NonNull;

import com.mls.engineAsr.Enums.AsrEngines;

import java.util.List;

public interface AsrEngineInterface {
    /**
     * Starts listening to user input. Be adviced that this operation may succeed or may fail. Do
     * not rely on these operation instead listen to events to be sure if engine is working.
     */
    void startListening();

    /**
     * Stops listening to user input. Be adviced that this operation may succeed or may fail. Do
     * not rely on these operation instead listen to events to be sure if engine is working.
     */
    void stopListening();

    /**
     * Destroys/disposes engine. Only call when your application is terminating since it is memory
     * consuming. You do not need to initialize engine at all since initialization is automatically
     * done.
     */
    void disposeEngine();

    /**
     * Defines if this engine supports given language. Make sure to perform this check, otherwise
     * initializing an engine with an un-supported language may result to {@code null} engine.
     *
     * @param language Language to check if is supported by this engine.
     *
     * @return {@code True} if requested language is supported by this engine. {@code False}
     * otherwise.
     */
    boolean languageIsSupported(String language);

    /**
     * Changes the listener to invoke asr results callbacks. Notice that current set listener will
     * be override. New results and callbacks will be send to new listener from now on. Notice that
     * you do not have to rebuild engine.
     *
     * @param listener Listener to invoke asr callbacks.
     */
    void setListener(@NonNull AsrCallbacksListener listener);

    /**
     * Commands engine to find the supported languages, collect them as a list and return them to
     * the calling point. Notice that since some engines may require to send an intent or read a
     * file in order to identify the supported languages, you will receive the list with the
     * supported languages from the listener {@link AsrCallbacksListener#onLangListRetrieved(List)}.
     */
    void getSupportedLanguages();

    /**
     * Sets the recognition language for this engine. Given language must be of type 'el-GR' etc.
     * [IETF language tag (as defined by BCP 47)]. Notice that the engine will rebuild on it's own.
     * You do not need to dispose the engine and re-construct it on your own.
     *
     * @param language Language to set for this engine.
     */
    void setRecognitionLanguage(@NonNull String language);

    /**
     * Defines which {@link AsrEngines} int code represents this engine. Mostly used internally to
     * avoid rebuilding engines.
     *
     * @return An int type of {@link AsrEngines} that represents this engine.
     */
    @AsrEngines int engineCode();

    /**
     * Defines if current asr engine is supported at current device or not. An engine may not be
     * available if any required libraries or apps are missing form the device. Most of the cases,
     * this function will return true.
     *
     * @return {@code True} if this asr engine can properly work at this device. {@code False}
     * otherwise.
     */
    boolean getAvailability();
}
