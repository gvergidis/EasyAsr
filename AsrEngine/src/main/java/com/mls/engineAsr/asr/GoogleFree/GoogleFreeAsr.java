package com.mls.engineAsr.asr.GoogleFree;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mls.engineAsr.Enums.AsrEngines;
import com.mls.engineAsr.Enums.EngineState;
import com.mls.engineAsr.Interfaces.AsrCallbacksListener;
import com.mls.engineAsr.Interfaces.AsrEngineInterface;
import com.mls.engineAsr.asr.Asr;

import java.util.ArrayList;
import java.util.List;

import static com.mls.engineAsr.asr.Asr.TAG;

public class GoogleFreeAsr implements AsrEngineInterface, RecognitionListener {
    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ VARIABLES                                                                                 ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → PRIVATE VARIABLES
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /** Application context. */
    private Context context;
    /** System audio manager used to mute device. */
    private final AudioManager audioManager;
    /** Current working language. */
    private String language;
    /** Listener to invoke asr events. */
    private AsrCallbacksListener listener;
    /** Google speech recognizer engine. */
    private SpeechRecognizer speechRecognizer = null;
    /** Intent used to initialize asr engine. */
    private Intent recognizerIntent;
    /** Defines if final result has already been sent. */
    private boolean sentFinal;
    /** Current recognized text. */
    private String currentText = "";
    /** The state of the engine. */
    private @EngineState String state;
    /** List containing engine supported languages. */
    private List<String> supportedLanguages = new ArrayList<>();
    /** Timestamp when asr was requested to start listening. */
    private long requestTimestamp;

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → HANDLERS AND RUNNABLE
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /** The mute stream device runnable. */
    private final Runnable muteRunnable;
    /** Handler that is running on the main thread. */
    private final Handler mainHandler;
    /** Runnable that is called when user requests engine to listen. If time overlaps threshold
     * but engine did not start, request will be cancelled. */
    private final Runnable listenRunnable;
    /**
     * Runnable that starts when the first partial result arrives and when it does, if not results
     * arrive in the following short period, asr is stopped and current results are accepted.
     */
    private final Runnable noResultsRunnable;

    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ FUNCTIONS                                                                                 ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → CONSTRUCTOR
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /**
     * Constructs new google free asr engine.
     *
     * @param context  Application context.
     * @param language Language to set to engine.
     * @param listener Listener to invoke asr events.
     */
    public GoogleFreeAsr(@NonNull Context context, @NonNull String language,
                         @NonNull AsrCallbacksListener listener) {
        // Store values.
        this.context = context;
        this.language = language;
        this.listener = listener;

        // Initialize handler.
        mainHandler = new Handler(Looper.getMainLooper());

        // Set state to idle since constructor called.
        state = EngineState.IDLE;

        // Get system audio manager.
        audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));

        // Initialize mute stream runnable.
        muteRunnable = new Runnable() {
            @Override
            public void run() {
                // Make sure audio manager is valid.
                if (audioManager == null) return;

                // Mute device.
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE, 0);
            }
        };

        // Initialize listen request watch dog runnable.
        listenRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if we are at another state than the fail one.
                if (!state.equals(EngineState.REQUESTED)) return;

                Log.w(TAG, TAG + GoogleFreeAsr.this.getClass().getCanonicalName() + " engine" +
                        " failed to start. Cancelling request to resume flow.");

                // Requested and time passed but engine still did not start. Cancel request.
                speechRecognizer.cancel();
                speechRecognizer.stopListening();

                // Invoke listener to keep flow.
                GoogleFreeAsr.this.listener.onAsrCancelled();

                // Reset state to idle.
                state = EngineState.IDLE;
            }
        };

        // Initialize results runnable.
        noResultsRunnable = new Runnable() {
            @Override
            public void run() {
                // Make sure we are listening to user.
                if (!state.equals(EngineState.LISTENING)) return;

                // Check that we do have results.
                if (currentText.isEmpty()) return;

                Log.w(TAG, TAG + GoogleFreeAsr.this.getClass().getCanonicalName() + " engine" +
                        " did not get results the last short period. Accepting current results.");

                // Stop asr and accept current results.
                stopListening();
            }
        };

        // Initialize asr engine.
        rebuild(language);
    }

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → ASSISTING FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /**
     * Rebuilds asr engine based on given language. Notice that engine is first disposed. Then,
     * engine is initialized again from the start.
     *
     * @param language Language to set to engine.
     */
    private void rebuild(String language) {
        Log.d(TAG, TAG + "Rebuilding " + getClass().getCanonicalName() + " engine.");

        // Check if engine is valid.
        if (!getAvailability()) {
            Log.d(TAG, TAG + "Failed to rebuild " + getClass().getCanonicalName() + " engine " +
                    "since it is not available on this device.");

            // Set undefined state.
            state = EngineState.UNDEFINED;

            // Invoke callback.
            listener.onAsrInitialized(false);
            return;
        }

        // Dispose first.
        if (speechRecognizer != null) {
            // Stop listening first.
            stopListening();

            // Dispose engine.
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        Log.d(TAG, TAG + "Constructing SpeechRecognizer at thread : " +
                Thread.currentThread().getName());

        // Initialize recognizer.
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);

        // Initialize intent.
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        Log.d(TAG, TAG + getClass().getCanonicalName() + " engine rebuilt successfully.");

        // Invoke callback.
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onAsrInitialized(true);
            }
        });
    }

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → INTERFACE FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /**
     * Starts listening to user input. Be adviced that this operation may succeed or may fail. Do
     * not rely on these operation instead listen to events to be sure if engine is working.
     */
    @Override
    public void startListening() {
        // Check if much time has passed since the request. If it did, then engine may failed
        // to start or is busy. We will retry to make a new request. Calculate time passed since
        // last request.
        final long timeFromRequest = System.currentTimeMillis() - requestTimestamp;

        // Check if user is requesting asr too often.
        if (timeFromRequest < 1500) {
            Log.w(TAG, TAG + "You requested asr to start before " + timeFromRequest + "ms. " +
                    "Please, avoid requesting so often. Skipping request...");
            return;
        }

        Log.d(TAG, TAG + getClass().getCanonicalName() + " is commanded to start...");

        // Check if engine is valid.
        if (speechRecognizer == null || recognizerIntent == null) rebuild(language);

        // Check if we are already listening.
        if (state.equals(EngineState.LISTENING)) {
            Log.w(TAG, TAG + getClass().getCanonicalName() + " engine already listening...");
            return;
        }

        // Check if we have already requested to speak.
        if (state.equals(EngineState.REQUESTED)) {
            Log.w(TAG, TAG + getClass().getCanonicalName() + " engine already received " +
                    "request. Please wait...");
            return;
        }

        // Set requested listening flag.
        state = EngineState.REQUESTED;

        // Execute watch dog runnable to make sure that engine did not stuck.
        mainHandler.postDelayed(listenRunnable, 3000);

        // Reset flag.
        sentFinal = false;

        // Clear text.
        currentText = "";

        // Store request timestamp.
        requestTimestamp = System.currentTimeMillis();

        // Mute device.
        muteStream(true);

        // Start listening to user input.
        speechRecognizer.startListening(recognizerIntent);
    }

    /**
     * Stops listening to user input. Be adviced that this operation may succeed or may fail. Do
     * not rely on these operation instead listen to events to be sure if engine is working.
     */
    @Override
    public void stopListening() {
        Log.d(TAG, TAG + getClass().getCanonicalName() + " is commanded to stop...");

        // Check if engine is idle.
        if (state.equals(EngineState.IDLE)) {
            Log.d(TAG, TAG + getClass().getCanonicalName() + " is idle. Nothing to stop...");
            return;
        }

        // Check if engine is currently working.
        if (state.equals(EngineState.UNDEFINED)) {
            Log.w(TAG, TAG + getClass().getCanonicalName() + " engine not initialized.");
            return;
        }

        if (speechRecognizer != null) {
            // Cancel any current requested to recognizer.
            speechRecognizer.cancel();

            // Stop listening to user input.
            speechRecognizer.stopListening();
        }

        // Invoke callback.
        if (!currentText.isEmpty()) listener.onAsrFinalResult(currentText);
        else listener.onAsrCancelled();

        // Set state to idle since engine is stopping.
        state = EngineState.IDLE;

        // Clear text.
        currentText = "";
    }

    /**
     * Destroys/disposes engine. Only call when your application is terminating since is it memory
     * consuming. You do not need to initialize engine at all since initialization is automatically
     * done. Notice that when engine is disposed, it's instance is {@code null} and you should
     * perform a new construction.
     */
    @Override
    public void disposeEngine() {
        if (speechRecognizer != null) {
            // Stop listening first.
            speechRecognizer.stopListening();

            // Cancel engine to release resources.
            speechRecognizer.cancel();

            // Dispose engine.
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        // Make sure device is un muted from us.
        muteStream(false);

        // Reset state since engine is disposed.
        state = EngineState.UNDEFINED;
    }

    /**
     * Defines if this engine supports given language. Make sure to perform this check, otherwise
     * initializing an engine with an un-supported language may result to a {@code null} or a
     * corrupted engine. Notice that you may need to perform a {@link Asr#getSupportedLanguages()}
     * first to initialize list of supported languages for current asr engine.
     *
     * @param language Language to check if is supported by this engine.
     *
     * @return {@code True} if requested language is supported by this engine. {@code False}
     * otherwise.
     */
    @Override
    public boolean languageIsSupported(String language) {
        return supportedLanguages.contains(language);
    }

    /**
     * Changes the listener to invoke asr results callbacks. Notice that current set listener will
     * be override. New results and callbacks will be send to new listener from now on. Notice that
     * you do not have to rebuild engine.
     *
     * @param listener Listener to invoke asr callbacks.
     */
    @Override
    public void setListener(@NonNull AsrCallbacksListener listener) {
        this.listener = listener;
    }

    /**
     * Commands engine to find the supported languages, collect them as a list and return them to
     * the calling point. Notice that since some engines may require to send an intent or read a
     * file in order to identify the supported languages, you will receive the list with the
     * supported languages from the listener {@link AsrCallbacksListener#onLangListRetrieved(List)}.
     */
    @Override
    public void getSupportedLanguages() {
        // Initialize the intent to send in order to receive the supported languages.
        Intent intent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        intent.setPackage("com.google.android.googlequicksearchbox");

        // Send the intent and await for the response.
        context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Check if result code is ok.
                if (getResultCode() != Activity.RESULT_OK) {
                    Log.e(TAG, TAG + "Failed to receive supported languages for engine : " +
                            getClass().getCanonicalName());

                    // Invoke callback.
                    listener.onLangListRetrieved(null);
                    return;
                }

                // Get languages list and store them.
                supportedLanguages = getResultExtras(true)
                        .getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);

                if (supportedLanguages != null) {
                    // Remove monkey languages.
                    supportedLanguages.remove("mk-MK");

                    // Sort languages array.
                    //supportedLanguages.sort(String::compareToIgnoreCase);
                }

                // Invoke callback.
                listener.onLangListRetrieved(supportedLanguages);
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    /**
     * Sets the recognition language for this engine. Given language must be of type 'el-GR' etc.
     * [IETF language tag (as defined by BCP 47)]. Notice that the engine will rebuild on it's own.
     * You do not need to dispose the engine and re-construct it on your own.
     *
     * @param language Language to set for this engine.
     */
    @Override
    public void setRecognitionLanguage(@NonNull String language) {
        rebuild(language);
    }

    /**
     * Defines which {@link AsrEngines} int code represents this engine. Mostly used internally to
     * avoid rebuilding engines.
     *
     * @return An int type of {@link AsrEngines} that represents this engine.
     */
    @Override
    public int engineCode() {
        return AsrEngines.GOOGLE_FREE;
    }

    /**
     * Defines if current asr engine is supported at current device or not. An engine may not be
     * available if any required libraries or apps are missing form the device. Most of the cases,
     * this function will return true.
     *
     * @return {@code True} if this asr engine can properly work at this device. {@code False}
     * otherwise.
     */
    @Override
    public boolean getAvailability() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → ASSISTING FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * Mutes/un-mutes device stream music by using {@link Context#AUDIO_SERVICE}.
     *
     * @param mute If {@code true} will mute stream music. Will un mute otherwise.
     */
    private void muteStream(boolean mute) {
        // Check if we got manager.
        if (audioManager != null) {
            // Make sure we un-mute device.
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, mute
                    ? AudioManager.ADJUST_MUTE
                    : AudioManager.ADJUST_UNMUTE, 0);

            // Google beep sound lasts 0.8 seconds. Re-enable stream levels after a duration.
            if (mute) {
                // Make sure to cancel any running mute runnable.
                mainHandler.removeCallbacks(muteRunnable);

                // Mute device after a while.
                mainHandler.postDelayed(muteRunnable, 900);
            }
        }
    }

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → GOOGLE ENGINE CALLBACKS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /**
     * Called when the endpoint is ready for the user to start speaking.
     *
     * @param params Parameters set by the recognition service. Reserved for future use.
     */
    @Override
    public void onReadyForSpeech(Bundle params) {
        // Mute device.
        muteStream(true);

        // Invoke callback.
        listener.onAsrStart();

        // Set state to listening. Engine stated!
        state = EngineState.LISTENING;

        // Clear watch dog runnable.
        mainHandler.removeCallbacks(listenRunnable);
    }

    /**
     * The user has started to speak.
     */
    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * The sound level in the audio stream has changed. There is no guarantee that this method will
     * be called.
     *
     * @param rmsdB The new RMS dB value.
     */
    @Override
    public void onRmsChanged(float rmsdB) {
    }

    /**
     * More sound has been received. The purpose of this function is to allow giving feedback to
     * the user regarding the captured audio. There is no guarantee that this method will be called.
     * @param buffer A buffer containing a sequence of big-endian 16-bit integers representing a
     *               single channel audio stream. The sample rate is implementation dependent.
     */
    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    /**
     * Called after the user stops speaking.
     */
    @Override
    public void onEndOfSpeech() {
        // Mute device to avoid hearing ending beep sound.
        muteStream(true);
    }

    /**
     * A network or recognition error occurred.
     *
     * @see <a href="https://developer.android.com/reference/android/speech/SpeechRecognizer.html">
     *     SpeechRecognizer error codes</a>
     *
     * @param error Code is defined in SpeechRecognizer.
     */
    @Override
    public void onError(int error) {
        Log.e(TAG, TAG + "An error occurred with code : " + error + " at engine : " +
                getClass().getCanonicalName());

        // Mute device to avoid hearing ending beep sound.
        muteStream(true);

        // Set state to idle. Engine thrown an error or did not get any results.
        state = EngineState.IDLE;

        // Try to fix broken engine.
        if (error == 8) {
            Log.e(TAG, TAG + "Engine : " + getClass().getCanonicalName() + " is busy...");
            speechRecognizer.cancel();
        }
        else if (error == 7) {
            Log.e(TAG, TAG + "Engine : " + getClass().getCanonicalName() +
                    " did not get matches.");

            // No match error. This is not an error. Just invoke callback.
            listener.onAsrCancelled();
            return;
        }

        // Invoke callback.
        listener.onAsrError(error);

        // Clear watch dog runnable.
        mainHandler.removeCallbacks(listenRunnable);

        // Clear no results runnable since we finished listening to user.
        mainHandler.removeCallbacks(noResultsRunnable);
    }

    /**
     * Called when recognition results are ready.
     *
     * @param results The recognition results. To retrieve the results in ArrayList<String> format
     *                use Bundle#getStringArrayList(String) with
     *                SpeechRecognizer#RESULTS_RECOGNITION as a parameter. A float array of
     *                confidence values might also be given in SpeechRecognizer#CONFIDENCE_SCORES.
     */
    @Override
    public void onResults(@NonNull Bundle results) {
        // Did we sent final results already?
        if (sentFinal || !state.equals(EngineState.LISTENING)) return;

        // Got results. Go to idle state.
        state = EngineState.IDLE;

        // Get matches out of bundle.
        final ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        // Set flag.
        sentFinal = true;

        // Clear no results runnable since we finished listening to user.
        mainHandler.removeCallbacks(noResultsRunnable);

        // Check if matches list is valid.
        if (matches != null && !matches.isEmpty())
            // Invoke callback.
            listener.onAsrFinalResult(matches.get(0));
        else
            // Invoke callback.
            listener.onAsrFinalResult("");

        // Clear text.
        currentText = "";
    }

    /**
     * Called when partial recognition results are available. The callback might be called at any
     * time between onBeginningOfSpeech() and onResults(android.os.Bundle) when partial results are
     * ready. This method may be called zero, one or multiple times for each call to
     * SpeechRecognizer#startListening(Intent), depending on the speech recognition service
     * implementation. To request partial results, use RecognizerIntent#EXTRA_PARTIAL_RESULTS.
     *
     * @param partialResults The returned results. To retrieve the results in ArrayList<String>
     *                       format use Bundle#getStringArrayList(String) with
     *                       SpeechRecognizer#RESULTS_RECOGNITION as a parameter.
     */
    @Override
    public void onPartialResults(@NonNull Bundle partialResults) {
        if (!state.equals(EngineState.LISTENING)) return;

        // Get matches out of bundle.
        final ArrayList<String> matches = partialResults
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        // Check if matches list is valid.
        if (matches != null) {
            // Store results.
            currentText = TextUtils.join("", matches);

            // Invoke callback.
            listener.onAsrPartialResult(currentText);
        }

        // Clear current results runnable.
        mainHandler.removeCallbacks(noResultsRunnable);

        // Run new results runnable.
        mainHandler.postDelayed(noResultsRunnable, 3000);
    }

    /**
     * Reserved for adding future events.
     *
     * @param eventType The type of the occurred event.
     * @param params    A Bundle containing the passed parameters.
     */
    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.e(TAG, TAG + "Engine : " + getClass().getCanonicalName() + " received an event " +
                "with type : " + eventType);
    }
}
