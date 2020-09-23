package com.vgraphics.engineAsr.asr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.vgraphics.engineAsr.Enums.AsrEngines;
import com.vgraphics.engineAsr.Interfaces.AsrCallbacksListener;
import com.vgraphics.engineAsr.Interfaces.AsrEngineInterface;
import com.vgraphics.engineAsr.asr.GoogleFree.GoogleFreeAsr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class Asr {
    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ VARIABLES                                                                                 ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → INTERNAL VARIABLES
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /** Logger tag. */
    public static final String TAG = "*** VGraphics *** [AsrEngine] :: ";
    /** Static instance of Asr engine. */
    @SuppressLint("StaticFieldLeak")
    static Asr instance;

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → PRIVATE VARIABLES
    // └───────────────────────────────────────────────────────────────────────────────────────────┘
    /** Asr current engine. */
    private static AsrEngineInterface engine;
    /** Current selected asr engine. */
    private @AsrEngines int asrEngine = AsrEngines.GOOGLE_FREE;
    /** Application context. */
    private Context context;
    /** Listener to invoke asr results. */
    private AsrCallbacksListener listener;
    /** Defines the current selected language. */
    private String language;
    /** Lock object used to avoid calling multiple operations at the same time. */
    private final Object lock = new Object();

    // ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
    // ║ FUNCTIONS                                                                                 ║
    // ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → CONSTRUCTOR
    // └───────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * Private constructor to avoid initializing abstract class.
     */
    Asr() {}

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → INITIALIZE FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Initializes ASR engine object. You can use this engine for all languages. You do not
     * have to dispose the engine and re-initialize in order to change language. Notice that this
     * operation is required and must be called in order for asr to properly work.
     *
     * <p>Notice that this overload initialization method does not require a parameter for asr
     * engine to use neither a language. By default, engine {@link AsrEngines#GOOGLE_FREE} will be
     * used and language will be {@code null}. Default language of most asr engines will be the
     * language of the device default locale {@link Locale#getDefault()}.
     *
     * @param context Application context used to initialize asr engine.
     *
     * @see #getSupportedLanguages()
     * @see #languageIsSupported(String)
     */
    public void initialize(@NonNull final Context context) {
        this.initialize(context, null, AsrEngines.GOOGLE_FREE);
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Initializes ASR engine object. You can use this engine for all languages. You do not
     * have to dispose the engine and re-initialize in order to change language. Notice that this
     * operation is required and must be called in order for asr to properly work.
     *
     * <p>Notice that this overload initialization method does not require a parameter for asr
     * engine to use. By default, engine {@link AsrEngines#GOOGLE_FREE} will be used.
     *
     * @param context  Application context used to initialize asr engine.
     * @param language The language code used to initialize asr engine. If {@code null} or empty,
     *                 device default language will be used.
     *
     * @see #getSupportedLanguages()
     * @see #languageIsSupported(String)
     */
    public void initialize(@NonNull final Context context, @Nullable String language) {
        this.initialize(context, language, AsrEngines.GOOGLE_FREE);
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Initializes ASR engine object. You can use this engine for all languages. You do not
     * have to dispose the engine and re-initialize in order to change language. Notice that this
     * operation is required and must be called in order for asr to properly work.
     *
     * @param context   Application context used to initialize asr engine.
     * @param language  The language code used to initialize asr engine. If {@code null} or empty,
     *                  device default language will be used.
     * @param asrEngine The asr engine to use. No check for language support is done here. You may
     *                  end with a corrupted engine unless you perform a language validation check.
     *
     * @see #getSupportedLanguages()
     * @see #languageIsSupported(String)
     */
    public void initialize(@NonNull final Context context, @Nullable String language,
                           @AsrEngines int asrEngine) {
        synchronized (lock) {
            Log.d(TAG, TAG + "Initializing Asr engine...");

            // Check if given language is valid.
            if (language == null || language.isEmpty()) language = getCurrentLanguage();

            // Store context.
            this.context = context;
            this.asrEngine = asrEngine;
            this.language = language;

            // Initialize engine.
            constructEngine();
        }
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Sets the language of the engine. Notice that changing the language, automatically disposes
     * the engine and rebuilds it. You do not need to perform any further actions. A good practice
     * would be to have a live data object containing the current language as data value and every
     * time this value changes, you should also change the language of the engine.
     *
     * <p>Notice that the instance of the engine is the same among all fragments and activities and
     * so one, you only need to change the language from one place only. It is safe to call this
     * operation as ofter as you like.
     *
     * <p>Parsing an invalid language will result the engine to use the default
     * {@link Locale#getDefault()}.
     *
     * @param language The new language to initialize the engine. Must be type of 'en-US'.
     */
    public void setLanguage(@Nullable String language) {
        // Check if given language is valid.
        if (language == null || language.isEmpty()) language = getCurrentLanguage();

        // Check if current language matches engine language.
        if (this.language.equals(language)) return;

        // Store language.
        this.language = language;

        // Re-build engine.
        constructEngine();
    }

    /**
     *<pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Sets the asr listener to receive asr callback events. Notice that any set listener will be
     * override.
     *
     * @param listener Listener to invoke asr results.
     */
    public void setListener(@NonNull AsrCallbacksListener listener) {
        // Set listener.
        this.listener = listener;

        // Check if we have a initialized engine. If we do, just set the listener.
        if (engine != null) engine.setListener(this.listener);
    }

    /**
     *<pre>
     *    <h3 color="80d8ff">Overview</h3>
     *
     * Sets the asr engine to use. Engine is constructed automatically when changing so you do not
     * need to stop or dispose current engine.
     *
     * @param asrEngine The asr engine to use. No check for language support is done here. You may
     *                  end with a corrupted engine unless you perform a language validation check.
     */
    public void setEngine(@AsrEngines int asrEngine) {
        // Check if same engine requested.
        if (this.asrEngine == asrEngine) {
            Log.w(TAG, TAG + "Trying to set same engine. Skipping...");
            return;
        }

        // Store engine preference.
        this.asrEngine = asrEngine;

        // Re-build engine.
        constructEngine();
    }

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → OPERATING FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Starts listening to user input. Be adviced that this operation may succeed or may fail. Do
     * not rely on these operation instead listen to events to be sure if engine is working.
     *
     * <pre>
     *    <h3 color="b2ff59">Permissions required</h3>
     *    <ul>
     *           <li>{@link Manifest.permission#RECORD_AUDIO}</li>
     *           <li>{@link Manifest.permission#INTERNET}</li>
     *     </ul>
     * </pre>
     */
    @RequiresPermission(allOf = {Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET})
    public void startListening() {
        // Check if engine is initialized.
        if (engine == null) {
            Log.e(TAG, TAG + "Asr engine is null. Can not start engine!");
            return;
        }

        // Start the engine.
        final boolean result = new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                engine.startListening();
            }
        });

        // Check if engine started.
        if (!result) Log.e(TAG, TAG + "Failed to start asr engine.");
        else Log.d(TAG, TAG + "Asr engine is starting...");
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Stops listening to user input. Be adviced that this operation may succeed or may fail. Do
     * not rely on these operation instead listen to events to be sure if engine is working.
     */
    public void stopListening() {
        // Check if engine is initialized.
        if (engine == null) {
            Log.e(TAG, TAG + "Asr engine is null. Can not stop engine!");
            return;
        }

        // Stop the engine.
        final boolean result = new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                engine.stopListening();
            }
        });

        // Check if engine stopped.
        if (!result) Log.e(TAG, TAG + "Failed to stop asr engine.");
        else Log.d(TAG, TAG + "Asr engine is stopping...");
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Returns the current language of the asr engine. Defines the language of the recognition.
     * Notice that if you have not yet set a language or have passed a {@code null} language at
     * the constructors or through {@link #setLanguage(String)}, then default
     * {@link Locale#getDefault()} will be used. Returned language will be type of 'el-GR' etc.
     * [IETF language tag (as defined by BCP 47)]. This value will never be {@code null}.
     *
     * @return The current asr recognition language type of 'el-GR' etc. [IETF language tag (as
     * defined by BCP 47)] The device default if current language has not been yet set or is
     * {@code null}.
     */
    @NonNull
    public String getCurrentLanguage() {
        // Check if language is valid.
        if (language == null || language.isEmpty()) language = Locale.getDefault().toLanguageTag();

        return language;
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Defines if this engine supports given language. Make sure to perform this check, otherwise
     * initializing an engine with an un-supported language may result to {@code null} engine.
     *
     * @param language Language to check if is supported by this engine. Must be of type 'el-GR'.
     *                 [IETF language tag (as defined by BCP 47)]
     *
     * @return {@code True} if requested language is supported by this engine. {@code False}
     * otherwise.
     */
    public boolean languageIsSupported(String language) {
        // Check if engine is initialized.
        if (engine == null) {
            Log.e(TAG, TAG + "Asr engine is null. Can not check if language is supported!");
            return false;
        }

        // We have a valid engine. Check if language is supported.
        return engine.languageIsSupported(language);
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Commands current asr engine to retrieve a list with the supported languages. Notice that
     * some engines may require to send an intent or read a file in order to retrieve supported
     * languages. Languages list will be returned through
     * {@link AsrCallbacksListener#onLangListRetrieved(List)} callback.
     *
     * <p>In case current engine has not been set or is not valid, this method will return
     * {@code false} immediately to inform you that this operation failed.
     *
     * @return {@code True} if a valid engine currently exists and is successfully ordered to
     * retrieve languages list. {@code False} otherwise.
     */
    public boolean getSupportedLanguages() {
        // Check if engine is initialized.
        if (engine == null) {
            Log.e(TAG, TAG + "Asr engine is null. Can not retrieve supported languages!");
            return false;
        }

        // We have a valid engine. Retrieve list with supported engine languages.
        engine.getSupportedLanguages();
        return true;
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Returns a list of all the available asr engines. You can set an engine either when
     * constructing the asr engine by calling {@link #initialize(Context, String, int)} or by
     * calling {@link #setEngine(int)} immediately on an already constructed engine. If you need a
     * string that represents the name of the engine, consider using {@link #engineToString(int)}.
     *
     * @return A list containing all the available asr engines. Values are integers that are mapped
     * to {@link AsrEngines} values.
     */
    public List<Integer> getAvailableEngines() {
        final List<Integer> engines = new ArrayList<>();
        engines.add(AsrEngines.GOOGLE_FREE);
        return engines;
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Returns a string that represents a friendly name for a selected asr engine.
     *
     * @param asrEngine The asr engine to convert to a friendly string. Can be any of
     * {@link AsrEngines}.
     *
     * @return A string that represents a friendly name for given asr engine.
     */
    public static String engineToString(@AsrEngines int asrEngine) {
        if (asrEngine == AsrEngines.GOOGLE_FREE) return "Google Free";

        return "undefined";
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Returns the availability status of the current asr engine. If the engine is not currently
     * set, then this operation will return {@code false}. Otherwise, returns if the current engine
     * is available at the current device.
     *
     * @return {@code True} if current engine is set and is available at this device. {@code False}
     * otherwise.
     */
    public boolean getAvailability() {
        synchronized (lock) {
            if (engine == null) {
                Log.e(TAG, TAG + "Engine is null. Can not check for availability.");
                return false;
            }
            return engine.getAvailability();
        }
    }

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Disposes whole asr engine. You are safe to directly call destroy on your
     * main activity onDestroy method to clear engine. Notice that if you dispose the engine, you
     * will have to initialize it again.
     */
    public void destroy() {
        synchronized (lock) {
            // Clear instance.
            instance = null;

            // Dispose asr engine too.
            if (engine != null)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        engine.disposeEngine();
                    }
                });
        }
    }

    // ┌───────────────────────────────────────────────────────────────────────────────────────────┐
    //   → ASSISTING FUNCTIONS
    // └───────────────────────────────────────────────────────────────────────────────────────────┘

    /**
     * <pre>
     *     <h3 color="80d8ff">Overview</h3>
     *
     * Constructs the {@link #engine} based on stored {@link #language}, {@link #listener} and
     * {@link #asrEngine}. This operation will not dispose the current engine if user has not
     * changed his preference on used engine. Instead, it just rebuilds the current engine using
     * {@link AsrEngineInterface#setRecognitionLanguage(String)} to avoid memory consumption.
     * Notice that this operation will always run on the main thread.
     */
    private void constructEngine() {
        synchronized (lock) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                // Re-construct engine if requested engine is same using new language.
                if (engine != null && engine.engineCode() == asrEngine) {
                    engine.setRecognitionLanguage(language);
                    return;
                }

                // Engine is null or user requested another language. Dispose engine if we have any.
                if (engine != null) engine.disposeEngine();

                // Finally, construct a new engine.
                if (asrEngine == AsrEngines.GOOGLE_FREE) {
                    engine = new GoogleFreeAsr(context, language, listener);
                }
                }
            });
        }
    }
}
