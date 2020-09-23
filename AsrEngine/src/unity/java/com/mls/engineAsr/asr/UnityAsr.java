package com.vgraphics.engineAsr.asr;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vgraphics.engineAsr.Interfaces.AsrCallbacksListener;
import com.unity3d.player.UnityPlayer;

import java.util.List;

@Keep
@SuppressWarnings({"unused"})
public class UnityAsr extends Asr implements AsrCallbacksListener {
    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ FUNCTIONS                                                                                 ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → PUBLIC FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Returns the instance of the Unity Asr engine. This value will never be {@code null}.
     *
     * @param context Application context used to initialize asr engine.
     *
     * @return The instance of the Unity Asr engine.
     */
    @NonNull
    public static Asr getInstance(@NonNull final Context context) {
        if (instance == null)
        {
            instance = new UnityAsr();

            // Set the listener for the asr engine.
            instance.setListener((AsrCallbacksListener) instance);

            // Initialize asr wrapper object.
            instance.initialize(context);
        }

        return instance;
    }

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → LISTENER FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * Invokes when the selected asr engine is initialized. Override this function to perform
     * specific actions like {@link Asr#getSupportedLanguages()} etc.
     *
     * @param success Defines if engine was initialized successfully or not.
     */
    @Override
    public void onAsrInitialized(boolean success) {
        UnityPlayer.UnitySendMessage("MainObject", "EventAsrInitialized",
                String.valueOf(success));
    }

    /**
     * Invokes when asr starts. Unity engine is notified through corresponding function callback.
     */
    @Override
    public void onAsrStart() {
        Log.d(TAG, TAG + "Asr just started. Waiting for your sweet voice darling!");

        UnityPlayer.UnitySendMessage("MainObject", "EventAsrStarted", "");
    }

    /**
     * Invokes when engine is stopped while engine was running and listening to user input. Notice
     * that current results will be sent through callback {@link #onAsrFinalResult(String)}.
     * Callback will invoke if engine has currently stored results.
     */
    @Override
    public void onAsrCancelled() {
        Log.d(TAG, TAG + "Asr was cancelled. Chicken!");

        UnityPlayer.UnitySendMessage("MainObject", "EventAsrCancelled", "");
    }

    /**
     * Invokes when asr ends. Results are passed to unity engine at corresponding function.
     *
     * @param finalResult Results of asr recognition.
     */
    @Override
    public void onAsrFinalResult(String finalResult) {
        Log.d(TAG, TAG + "Received final results : " + finalResult);

        // Check if final result is empty.
        if (finalResult.isEmpty())
            UnityPlayer.UnitySendMessage("MainObject", "EventAsrEmptyResult", "");
        else
            UnityPlayer.UnitySendMessage("MainObject", "EventAsrFinalResults", finalResult);
    }

    /**
     * Invokes every time a partial asr result is retrieved.
     *
     * @param partialResult The partial asr result retrieved.
     */
    @Override
    public void onAsrPartialResult(String partialResult) {
        Log.d(TAG, TAG + "Received partial results : " + partialResult);

        // Send partial results to unity engine.
        UnityPlayer.UnitySendMessage("MainObject", "EventAsrPartialResults", partialResult);
    }

    /**
     * Invokes when an asr error occurs (possibly a network error or maybe an internal error).
     * @param errorCode The code of the error that occurred.
     */
    public void onAsrError(int errorCode) {
        Log.d(TAG, TAG + "An error occurred!");

        // Invoke callback on unity.
        UnityPlayer.UnitySendMessage("MainObject", "EventAsrError",
                String.valueOf(errorCode));
    }

    /**
     * Invokes when the list with the supported languages is retrieved. Since some engines require
     * to send an intent or read a file in order to identify the supported languages, you need to
     * request the languages list and listen to this callback to retrieve that list.
     *
     * @param languages The list containing the supported languages as language codes
     *                  ('el-GR' etc. [IETF language tag (as defined by BCP 47)]).
     */
    @Override
    public void onLangListRetrieved(@Nullable List<String> languages) {

    }
}