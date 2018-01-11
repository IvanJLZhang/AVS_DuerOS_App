package com.wistron.demo.tool.teddybear.scene.useless;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ChatRobotActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_Speaking;
    private TextView tv_MsgIn, tv_MsgOut;
    private EditText tv_ChatLogs;

    private MicrophoneRecognitionClient micClient = null;
    private ToSpeak mToSpeak;
    private RobotTask mRobotTask;

    private int mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_INITIAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_chat_robot);

        findView();
        initial();
    }

    private void initial() {
        mToSpeak = ToSpeak.getInstance(this);
    }

    private void findView() {
        btn_Speaking = (Button) findViewById(R.id.chat_button_speaking);
        tv_MsgIn = (TextView) findViewById(R.id.chat_msg_in);
        tv_MsgOut = (TextView) findViewById(R.id.chat_msg_out);
        tv_ChatLogs = (EditText) findViewById(R.id.chat_logs);
        tv_ChatLogs.setMovementMethod(ScrollingMovementMethod.getInstance());

        tv_ChatLogs.setVisibility(View.INVISIBLE);
        tv_MsgIn.setVisibility(View.GONE);
        tv_MsgOut.setVisibility(View.GONE);

        btn_Speaking.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btn_Speaking) {
            stop();
            startToListenCmd();
        }
    }

    private void stop() {
        if (mRobotTask != null) {
            mRobotTask.cancel(true);
        }

        if (mToSpeak != null) {
            mToSpeak.stop();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F12
                /*|| keyCode == KeyEvent.KEYCODE_VOLUME_UP*/) {
            return super.onKeyDown(keyCode, event);
        } else {
            finish();
            return true;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F12
                /*|| keyCode == KeyEvent.KEYCODE_VOLUME_UP*/) {
            if (btn_Speaking.isEnabled()) {
                btn_Speaking.performClick();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
        SceneCommonHelper.closeLED();
        if (micClient != null) {
            micClient.endMicAndRecognition();
            try {
                micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            micClient = null;
        }
    }

    private void startToListenCmd() {
        tv_ChatLogs.setVisibility(View.VISIBLE);
        tv_MsgIn.setVisibility(View.GONE);
        tv_MsgOut.setVisibility(View.GONE);
        tv_ChatLogs.setText(getString(R.string.stt_in_initial));

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (micClient == null) {
                    micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                            ChatRobotActivity.this,
                            SpeechRecognitionMode.ShortPhrase,
                            getString(R.string.preference_speaking_language_default_value),
                            mSpeechRecognitionEvent,
                            getString(SubscriptionKey.getSpeechPrimaryKey()));
                }
                mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_INITIAL;
                micClient.startMicAndRecognition();
            }
        });
    }

    private void updateLog(String log) {
        tv_ChatLogs.append(log + "\n");
        tv_ChatLogs.setSelection(tv_ChatLogs.length(), tv_ChatLogs.length());
        Log.i("King", "" + log);
    }

    // Speech to text
    private ISpeechRecognitionServerEvents mSpeechRecognitionEvent = new ISpeechRecognitionServerEvents() {
        @Override
        public void onPartialResponseReceived(String response) {
            /*updateLog("Partial result received by onPartialResponseReceived()");
            updateLog(response);*/
        }

        @Override
        public void onFinalResponseReceived(RecognitionResult recognitionResult) {
            if (null != micClient) {
                // we got the final result, so it we can end the mic reco.  No need to do this
                // for dataReco, since we already called endAudio() on it as soon as we were done
                // sending all the data.
                micClient.endMicAndRecognition();
            }
            mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_DONE;

            updateLog("********* Final n-BEST Results *********");
            for (int i = 0; i < recognitionResult.Results.length; i++) {
                updateLog("[" + i + "]" + " Confidence=" + recognitionResult.Results[i].Confidence +
                        " Text=\"" + recognitionResult.Results[i].DisplayText + "\"");
            }
            if (recognitionResult.Results.length > 0) {
                talkToRobot(recognitionResult.Results[0].DisplayText.trim());
            } else {
                String status = "Can\'t recognize your speaking, please try again.";
                updateLog(status);
                mToSpeak.toSpeak(status, true);
                SceneCommonHelper.closeLED();
            }
        }

        @Override
        public void onIntentReceived(String payload) {
            /*updateLog("Intent received by onIntentReceived()");
            updateLog(payload);*/
        }

        @Override
        public void onError(final int errorCode, final String response) {
            mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_ERROR;

            updateLog("Error received by onError()");
            updateLog("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
            updateLog("Error text: " + response);

            micClient.endMicAndRecognition();
            updateLog("Please retry!");

            SceneCommonHelper.playSpeakingSound(ChatRobotActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_FAIL);
        }

        @Override
        public void onAudioEvent(boolean recording) {
            if (recording) {
                tv_ChatLogs.scrollTo(0, 0);
            }
            updateLog("Microphone status change received by onAudioEvent()");
            updateLog("********* Microphone status: " + recording + " *********");
            if (recording) {
                updateLog("--- Start listening voice input!");
                updateLog(getString(R.string.scene_mic_is_on));
                if (mCurrentSTTStatus != SceneCommonHelper.STT_STATUS_ERROR) {
                    mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_START_RECORDING;
                    SceneCommonHelper.playSpeakingSound(ChatRobotActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_START);
                }
            } else {
                micClient.endMicAndRecognition();
                updateLog("--- Stop listening voice input!");
                if (mCurrentSTTStatus == SceneCommonHelper.STT_STATUS_INITIAL) {
                    SceneCommonHelper.playSpeakingSound(ChatRobotActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_FAIL);
                } else if (mCurrentSTTStatus != SceneCommonHelper.STT_STATUS_ERROR) {
                    SceneCommonHelper.openLED();
                }
                mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_STOP_RECORDING;
            }
        }
    };

    // Talk to Robot
    private void talkToRobot(String displayText) {
        Log.i("King", "question = " + displayText);
        SceneCommonHelper.openLED();
        tv_ChatLogs.setVisibility(View.GONE);
        tv_MsgOut.setVisibility(View.VISIBLE);
        tv_MsgOut.setText(displayText);

        mRobotTask = new RobotTask();
        mRobotTask.execute(displayText);
    }

    private class RobotTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String apiKey = getString(R.string.chat_robot_api_key);
            String apiSecret = getString(R.string.chat_robot_api_secret);
            String message = getMessage(params[0]);
            String hash = getHash(message, apiSecret);
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                String getParams = "apiKey=" + apiKey +
                        "&hash=" + hash +
                        "&message=" + URLEncoder.encode(message, "UTF-8");
                Log.i("King", "ChatRobotActivity encoding message = " + URLEncoder.encode(message, "UTF-8"));
                URL url = new URL("http://www.personalityforge.com/api/chat/?" + getParams);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);

                int responseCode = connection.getResponseCode();
                Log.i("King", "ChatRobotActivity responseCode = " + responseCode);
                StringBuilder outputBuilder = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String line;
                while (null != (line = reader.readLine())) {
                    outputBuilder.append(line);
                }
                String result = outputBuilder.toString();
                Log.i("King", "ChatRobotActivity result = " + result);
                if (responseCode == 200) {
                    return result;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private String getHash(String message, String apiSecret) {
            try {
                byte[] keyBytes = apiSecret.getBytes();
                SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(signingKey);

                byte[] rawHmac = mac.doFinal(message.getBytes());
                byte[] hexBytes = new Hex().encode(rawHmac);
                String hashCode = new String(hexBytes, "UTF-8");
                Log.i("King", "hashCode = " + hashCode);
                return hashCode;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private String getMessage(String text) {
            JsonObject message = new JsonObject();
            message.addProperty("message", text);
            //message.addProperty("chatBotID", 23958); // prob
            message.addProperty("chatBotID", 63906); // Cyber Ty
            message.addProperty("timestamp", System.currentTimeMillis() / 1000);

            JsonObject user = new JsonObject();
            user.addProperty("externalID", "king-0000110000");

            JsonObject root = new JsonObject();
            root.add("message", message);
            root.add("user", user);
            Log.i("King", "Message JSON = " + root.toString());
            return root.toString();
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            Log.i("King", "reponse message = " + response);
            if (response != null) {
                try {
                    JSONObject toJason = new JSONObject(response);
                    Log.i("King", "success = " + toJason.getInt("success") + " , message = " + toJason.getString("message"));
                    boolean isSuccess = toJason.getInt("success") == 1;
                    String message;
                    if (isSuccess) {
                        message = toJason.getJSONObject("message").getString("message");
                        tv_MsgIn.setVisibility(View.VISIBLE);
                        tv_MsgIn.setText(message);
                    } else {
                        message = "error: " + toJason.getString("errorMessage");
                    }

                    toSpeak(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                String warning = "Can\'t get response from bot, please try again.";
                Toast.makeText(ChatRobotActivity.this, warning, Toast.LENGTH_LONG).show();
                toSpeak(warning);
            }
        }
    }

    // Robot speak
    private void toSpeak(String content) {
        mToSpeak.toSpeak(content, true);
    }
}
