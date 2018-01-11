package com.wistron.demo.tool.teddybear.scene.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper.STORAGE_CONFIG_FILE_PATH;
import static com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper.getSingleParameters;
import static com.wistron.demo.tool.teddybear.scene.luis_scene.SceneBase.TAG;

/**
 * Created by king on 16-8-18.
 */

public class HotwordResumeHelper {
    private Context context;
    private static HotwordResumeHelper instance;

    // Keyword recognition
    private static final String RECOGNITION_NAME = "wakeup";
    public static final String RECOGNITION_PHASE = "hello teddy";
    private SpeechRecognizer recognizer;
    private RecognitionListener mListener;

    private HotwordResumeHelper(Context context) {
        this.context = context;
    }

    public static HotwordResumeHelper getInstance(Context context) {
        if (instance == null) {
            instance = new HotwordResumeHelper(context);
        }
        return instance;
    }

    public void addRecognitionListener(RecognitionListener listener) {
        mListener = listener;
    }

    // Keyword to resume start: enable app to accept voice command.
    public void initial() {
        new AsyncTask<Void, Void, Exception>() {
            // Recognizer initialization is a time-consuming and it involves IO,
            // so we execute it in async task
            @Override
            protected Exception doInBackground(Void... params) {
                Log.i(TAG, "----> initialKeywordToResume    doInBackground.....");
                try {
                    Assets assets = new Assets(context);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                Log.i(TAG, "----> initialKeywordToResume    onPostExecute.....");
                if (result != null) {
                    // Failed to init recognizer
                    Log.i(TAG, "----> initialKeywordToResume    Failed to init recognizer: " + result);
                } else {
                    startListening();
                }
            }
        }.execute();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        Log.i(TAG, "setupRecognizer.....");
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        // For debug
        String mValue = "-20";
        Map<String, String> mParametersList = getSingleParameters(STORAGE_CONFIG_FILE_PATH);
        if (mParametersList != null && mParametersList.size() > 0) {
            for (String key : mParametersList.keySet()) {
                if (key.equals(SceneCommonHelper.CONFIG_KEY_KEYWORD_THRESHOLD)) {
                    mValue = mParametersList.get(key);
                    break;
                }
            }
        }

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                //.setKeywordThreshold(1e-45f) // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold((float) Math.pow(10, Integer.parseInt(mValue)))
                .setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile
                .getRecognizer();
        Log.i("King", "1e-20 = " + (1e-20f) + ", new Value = " + (float) Math.pow(10, Integer.parseInt(mValue)));
        recognizer.addListener(mListener);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */
        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(RECOGNITION_NAME, RECOGNITION_PHASE);
    }

    public void startListening() {
        stopListening();

        Log.i(TAG, "Hotword startListening......");
        recognizer.startListening(RECOGNITION_NAME);
    }

    public void stopListening() {
        Log.i(TAG, "Hotword stopListening......");
        if (recognizer != null) {
            recognizer.stop();
        }
    }

    public void destroy() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }
}
