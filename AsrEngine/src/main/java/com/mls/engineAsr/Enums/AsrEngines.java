package com.mls.engineAsr.Enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(AsrEngines.GOOGLE_FREE)
public @interface AsrEngines {
    /**
     * Defines the Google Free asr engine. Notice that this engine may only work if Google speech
     * kit is installed on current device since {@link android.speech.SpeechRecognizer} is used.
     *
     * <p>Please note that the application must have
     * {@link android.Manifest.permission#RECORD_AUDIO} permission to use this class.
     */
    int GOOGLE_FREE = 0;
}
