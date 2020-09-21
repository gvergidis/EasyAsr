package com.mls.engineAsr.asr.Google;

import android.content.Context;
import android.media.AudioFormat;

import androidx.annotation.NonNull;

import com.mls.engineAsr.Interfaces.AsrCallbacksListener;
import com.mls.engineAsr.Interfaces.AsrEngineInterface;

import java.io.IOException;

public class GoogleAsr {// implements AsrEngineInterface {
//    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
//    // ║ VARIABLES                                                                                 ║
//    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
//    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
//    //   → PRIVATE VARIABLES
//    // └───────────────────────────────────────────────────────────────────────────────────────────┘
//    /** Application context. */
//    private Context context;
//    /** Current working language. */
//    private String language;
//    /** Listener to invoke asr events. */
//    private AsrCallbacksListener listener;
//    /** Engine that implements Google Cloud Speech API. */
//    private SpeechClient speechClient;
//    private ResponseObserver<StreamingRecognizeResponse> responseObserver;
//    private ClientStream<StreamingRecognizeRequest> clientStream;
//    /** Provides information to the recognizer that specifies how to process the request.*/
//    private final RecognitionConfig recognitionConfig;
//    private final StreamingRecognitionConfig streamingRecognitionConfig;
//    private StreamingRecognizeRequest request;
//    private AudioFormat audioFormat;
//
//    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
//    // ║ FUNCTIONS                                                                                 ║
//    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
//    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
//    //   → CONSTRUCTOR
//    // └───────────────────────────────────────────────────────────────────────────────────────────┘
//    /**
//     * Construct new google free asr engine.
//     *
//     * @param context  Application context.
//     * @param language Language to set to engine.
//     * @param listener Listener to invoke asr events.
//     */
//    public GoogleAsr(@NonNull Context context, @NonNull String language,
//                     @NonNull AsrCallbacksListener listener) {
//        // Store values.
//        this.context = context;
//        this.language = language;
//        this.listener = listener;
//
//        // Initialize recognition config used for transaction.
//        recognitionConfig =
//                RecognitionConfig.newBuilder()
//                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                        .setLanguageCode(language)
//                        .setSampleRateHertz(16000)
//                        .build();
//
//        // Build the streaming config.
//        streamingRecognitionConfig =
//                StreamingRecognitionConfig.newBuilder()
//                        .setConfig(recognitionConfig)
//                        .build();
//
//        // Initialize audio format.
//        audioFormat = new AudioFormat.Builder()
//                .setSampleRate(16000)
//                .build();
//
//
//        // Initialize asr engine.
//        rebuild(language);
//    }
//
//    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
//    //   → ASSISTING FUNCTIONS
//    // └───────────────────────────────────────────────────────────────────────────────────────────┘
//
//    private void rebuild(String language) {
//        try {
//            // Initialize asr speech client.
//            speechClient = SpeechClient.create();
//
//            // Initialize client stream.
//            clientStream = speechClient.streamingRecognizeCallable().splitCall(responseObserver);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
//    //   → INTERFACE FUNCTIONS
//    // └───────────────────────────────────────────────────────────────────────────────────────────┘
//    /**
//     * Starts listening to user input. Be adviced that this operation may succeed or may fail. Do
//     * not rely on these operation instead listen to events to be sure if engine is working.
//     */
//    @Override
//    public void startListening() {
//        // Build request. First request in a streaming call has to be a config.
//        request =
//                StreamingRecognizeRequest.newBuilder()
//                        .setStreamingConfig(streamingRecognitionConfig)
//                        .build();
//
//        // Send the config request.
//        clientStream.send(request);
//    }
//
//    /**
//     * Stops listening to user input. Be adviced that this operation may succeed or may fail. Do
//     * not rely on these operation instead listen to events to be sure if engine is working.
//     */
//    @Override
//    public void stopListening() {
//
//    }
//
//    /**
//     * Destroys/disposes engine. Only call when your application is terminating since it is memory
//     * consuming. You do not need to initialize engine at all since initialization is automatically
//     * done.
//     */
//    @Override
//    public void disposeEngine() {
//
//    }
//
//    /**
//     * Defines if this engine supports given language. Make sure to perform this check, otherwise
//     * initializing an engine with an un-supported language may result to {@code null} engine.
//     *
//     * @param language Language to check if is supported by this engine.
//     * @return {@code True} if requested language is supported by this engine. {@code False}
//     * otherwise.
//     */
//    @Override
//    public boolean languageIsSupported(String language) {
//        return false;
//    }
//
//    /**
//     * Changes the listener to invoke asr results callbacks. Notice that current set listener will
//     * be override. New results and callbacks will be send to new listener from now on. Notice that
//     * you do not have to rebuild engine.
//     *
//     * @param listener Listener to invoke asr callbacks.
//     */
//    @Override
//    public void setListener(@NonNull AsrCallbacksListener listener) {
//
//    }
//
//    /**
//     * Commands engine to find the supported languages, collect them as a list and return them to
//     * the calling point. Notice that since some engines may require to send an intent or read a
//     * file in order to identify the supported languages, you will receive the list with the
//     * supported languages from the listener {@link AsrCallbacksListener#onLangListRetrieved(List)}.
//     */
//    @Override
//    public void getSupportedLanguages() {
//
//    }
//
//    /**
//     * Sets the recognition language for this engine. Given language must be of type 'el-GR' etc.
//     * Notice that the engine will rebuild on it's own. You do not need to dispose the engine and
//     * re-construct it on your own.
//     *
//     * @param language Language to set for this engine.
//     */
//    @Override
//    public void setRecognitionLanguage(@NonNull String language) {
//
//    }
//
//    /**
//     * Defines which {@link AsrEngines} int code represents this engine. Mostly used internally to
//     * avoid rebuilding engines.
//     *
//     * @return An int type of {@link AsrEngines} that represents this engine.
//     */
//    @Override
//    public int engineCode() {
//        return 0;
//    }
}
