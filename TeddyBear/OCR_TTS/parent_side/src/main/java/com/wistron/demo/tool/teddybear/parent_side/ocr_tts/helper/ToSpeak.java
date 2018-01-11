package com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper;

import android.content.Context;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;

/**
 * Created by king on 16-4-27.
 */
public class ToSpeak {
    private Context context;

    private SpeakTask mSpeakTask;
    private static Synthesizer m_syn = null;
    private static Voice voice = null;
    private AudioTrack audioTrack;

    private boolean isStopped;

    public ToSpeak(Context context) {
        this.context = context;
    }

    public void toSpeak(String content) {
        String text = CommonHelper.getSpeakContent(content);
        mSpeakTask = new SpeakTask();
        mSpeakTask.execute(text);
        isStopped = false;
    }

    private class SpeakTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            if (m_syn == null) {
                m_syn = new Synthesizer(context.getString(SubscriptionKey.getSpeechPrimaryKey()));
                m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
                Log.i("King", "m_syn initial...");
            }
            if (voice == null) {
                voice = new Voice(CommonHelper.LanguageRegion.REGION_ENGLISH_US,
                        String.format("Microsoft Server Speech Text to Speech Voice (%1$s, %2$s)", CommonHelper.LanguageRegion.REGION_ENGLISH_US, "ZiraRUS"),
                        Voice.Gender.Female,
                        true);
            }
            m_syn.SetVoice(voice, null);
            if (audioTrack == null) {
                audioTrack = new AudioTrack(3, 16000, 2, 2, AudioTrack.getMinBufferSize(16000, 2, 2), 1);
            }
            if (!isStopped) {
                byte[] sound = m_syn.SpeakSSML(CommonHelper.formatMicrosoftTTSToSSML(voice, params[0]));
                if (sound != null && sound.length != 0) {
                    if (audioTrack.getState() == 1) {
                        audioTrack.play();
                        audioTrack.write(sound, 0, sound.length);
                        if (audioTrack != null && audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                            audioTrack.stop();
                        }
                    }
                }
            }
            return null;
        }
    }

    public void stop() {
        isStopped = true;
        if (mSpeakTask != null) {
            mSpeakTask.cancel(true);
        }
        if (audioTrack != null) {
            if (AudioTrack.PLAYSTATE_PLAYING == audioTrack.getPlayState()
                    || AudioTrack.PLAYSTATE_PAUSED == audioTrack.getPlayState()) {
                audioTrack.stop();
            }
            audioTrack.release();
            audioTrack = null;
        }
    }
}
