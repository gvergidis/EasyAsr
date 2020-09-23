package com.vgraphics.engineAsr.Interfaces;

import androidx.annotation.Nullable;

import com.vgraphics.engineAsr.asr.Asr;

import java.util.List;

public interface AsrCallbacksListener {
    /**
     * Invokes when the selected asr engine is initialized. Override this function to perform
     * specific actions like {@link Asr#getSupportedLanguages()} etc.
     *
     * @param success Defines if engine was initialized successfully or not.
     */
    void onAsrInitialized(boolean success);

    /**
     * Invokes when asr starts.
     */
    void onAsrStart();

    /**
     * Invokes when engine is stopped while engine was running and listening to user input. Notice
     * that current results will be sent through callback {@link #onAsrFinalResult(String)}.
     * Callback will invoke if engine has currently stored results.
     */
    void onAsrCancelled();

    /**
     * Invokes when asr ends. Results are returned.
     * @param finalResult Results of asr recognition.
     */
    void onAsrFinalResult(String finalResult);

    /**
     * Invokes every time a partial asr result is retrieved.
     * @param partialResult The partial asr result retrieved.
     */
    void onAsrPartialResult(String partialResult);

    /**
     * Invokes when an asr error occurs (possibly a network error or maybe an internal error).
     * @param errorCode The code of the error that occurred.
     */
    void onAsrError(int errorCode);

    /**
     * Invokes when the list with the supported languages is retrieved. Since some engines require
     * to send an intent or read a file in order to identify the supported languages, you need to
     * request the languages list and listen to this callback to retrieve that list.
     *
     * @param languages The list containing the supported languages as language codes
     *                  ('el-GR' etc. [IETF language tag (as defined by BCP 47)]).
     */
    void onLangListRetrieved(@Nullable List<String> languages);
}
