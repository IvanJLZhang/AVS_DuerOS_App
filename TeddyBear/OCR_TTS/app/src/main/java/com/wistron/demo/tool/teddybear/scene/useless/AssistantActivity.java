package com.wistron.demo.tool.teddybear.scene.useless;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AssistantActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_Speaking;
    private TextView tv_MsgIn, tv_MsgOut;
    private EditText tv_ChatLogs;

    private MicrophoneRecognitionClient micClient = null;
    private ToSpeak mToSpeak;
    private RobotTask mRobotTask;

    private SendToLUISTask mSendToLUISTask;

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
            /*stop();
            startToListenCmd();*/

            // For debug
            sendToLUIS("");
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
                            AssistantActivity.this,
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
                sendToLUIS(recognitionResult.Results[0].DisplayText.trim());
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

            SceneCommonHelper.playSpeakingSound(AssistantActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_FAIL);
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
                    SceneCommonHelper.playSpeakingSound(AssistantActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_START);
                }
            } else {
                micClient.endMicAndRecognition();
                updateLog("--- Stop listening voice input!");
                if (mCurrentSTTStatus == SceneCommonHelper.STT_STATUS_INITIAL) {
                    SceneCommonHelper.playSpeakingSound(AssistantActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_FAIL);
                } else if (mCurrentSTTStatus != SceneCommonHelper.STT_STATUS_ERROR) {
                    SceneCommonHelper.openLED();
                }
                mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_STOP_RECORDING;
            }
        }
    };

    private void sendToLUIS(String displayText) {
        displayText = "set an alarm for 6 am tomorrow called king test";
        //displayText = "create an alarm for 1 hour 30 minutes";
        Log.i("King", "question = " + displayText);
        SceneCommonHelper.openLED();
        tv_ChatLogs.setVisibility(View.GONE);
        tv_MsgOut.setVisibility(View.VISIBLE);
        tv_MsgOut.setText(displayText);

        mSendToLUISTask = new SendToLUISTask();
        mSendToLUISTask.execute(displayText);
    }

    // Send to LUIS
    private class SendToLUISTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String appId = getString(R.string.luis_main_app_id);
            String subscriptionKey = getString(R.string.luis_subscription_key);
            String message = params[0];
            BufferedReader reader = null;
            StringBuilder outputBuilder = new StringBuilder();

            HttpClient httpclient = new DefaultHttpClient();
            try {
                String getParams = appId +
                        "?subscription-key=" + subscriptionKey +
                        "&q=" + URLEncoder.encode(message, "UTF-8").replaceAll("\\+", "%20") +
                        "&timezoneOffset=0.0&verbose=true";
                HttpGet request = new HttpGet("https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/" + getParams);

                HttpResponse response = httpclient.execute(request);
                int responseCode = response.getStatusLine().getStatusCode();
                Log.i("King", "AssistantActivity responseCode = " + responseCode);
                if (responseCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        /*Log.i("King", EntityUtils.toString(entity));*/
                        reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
                        String line;
                        while (null != (line = reader.readLine())) {
                            outputBuilder.append(line);
                        }
                        String result = outputBuilder.toString();
                        Log.i("King", "AssistantActivity result = " + result);
                        return result;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "AssistantActivity result = " + e.getMessage());
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if (!TextUtils.isEmpty(response)) {
                analyzeIntent(response);
            } else {
                finish();
            }
        }
    }

    private void analyzeIntent(String response) {
        boolean isIgnoreAlarm = false;
        try {
            JSONObject toJason = new JSONObject(response);
            JSONArray intents = toJason.getJSONArray(LuisHelper.TAG_INTENTS);
            String intent = intents.getJSONObject(0).getString(LuisHelper.TAG_INTENT);
            JSONArray entities = toJason.getJSONArray(LuisHelper.TAG_ENTITIES);
            Log.i("King", "AssistantActivity:: intent = " + intent);
            switch (intent) {
                case LuisHelper.INTENT_ALARM_SET_ALARM:
                    String date = null, hour = null, minute = null;
                    String weekDay = null, duration = null;
                    String alarmTitle = null;

                    for (int i = 0; i < entities.length(); i++) {
                        String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                        Log.i("King", "AssistantActivity:: entitiesType = " + entitiesType);
                        if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_ALARM_START_TIME)) {
                            JSONObject resolution = entities.getJSONObject(i).getJSONObject(LuisHelper.TAG_RESOLUTION);
                            String resolutionType = resolution.getString(LuisHelper.TAG_RESOLUTION_TYPE);
                            Log.i("King", "AssistantActivity:: resolutionType = " + resolutionType);
                            if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_TIME)) {
                                /* "time": "2015-10-17T04:17" */
                                /* "time": "XXXX-WXX-1T07" */
                                String[] dateTime = resolution.getString(LuisHelper.TAG_TIME).split(LuisHelper.SPLIT_TAG_T);
                                Log.i("King", "AssistantActivity:: dateTime = " + dateTime[0] + "T" + dateTime[1]);
                                if (dateTime.length >= 2) {
                                    if (dateTime[0].startsWith("XXXX-WXX")) {
                                        weekDay = dateTime[0];
                                    } else {
                                        date = dateTime[0];
                                    }
                                    String[] hourMinute = dateTime[1].split(LuisHelper.SPLIT_TAG_COLON);
                                    if (hourMinute.length >= 2) {
                                        Log.i("King", "AssistantActivity:: hourMinute = " + hourMinute[0] + ":" + hourMinute[1]);
                                        hour = hourMinute[0];
                                        minute = hourMinute[1];
                                    } else if (hourMinute.length == 1) {
                                        Log.i("King", "AssistantActivity:: hourMinute = " + hourMinute[0]);
                                        hour = hourMinute[0];
                                    }
                                }
                            } else if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_DURATION)) {
                                /* "duration": "PT1H30M" */
                                duration = resolution.getString(LuisHelper.TAG_DURATION).substring(2);
                                Log.i("King", "AssistantActivity:: hourMinute = " + duration);
                                Calendar calendar = Calendar.getInstance();
                                if (duration.contains(LuisHelper.SPLIT_TAG_H)) {
                                    int tempHour = (calendar.get(Calendar.HOUR_OF_DAY) +
                                            Integer.parseInt(duration.substring(0, duration.indexOf(LuisHelper.SPLIT_TAG_H)))) % 24;
                                    hour = String.valueOf(tempHour);
                                    duration = duration.substring(duration.indexOf(LuisHelper.SPLIT_TAG_H) + 1);
                                }
                                if (duration.contains(LuisHelper.SPLIT_TAG_M)) {
                                    int tempMinute = (calendar.get(Calendar.MINUTE) +
                                            Integer.parseInt(duration.substring(0, duration.indexOf(LuisHelper.SPLIT_TAG_M)))) % 60;
                                    minute = String.valueOf(tempMinute);
                                }
                            }
                        } else if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_ALARM_START_DATE)) {
                            JSONObject resolution = entities.getJSONObject(i).getJSONObject(LuisHelper.TAG_RESOLUTION);
                            String resolutionType = resolution.getString(LuisHelper.TAG_RESOLUTION_TYPE);
                            Log.i("King", "AssistantActivity:: resolutionType = " + resolutionType);
                            if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_DATE)) {
                                date = resolution.getString(LuisHelper.TAG_DATE);
                                Log.i("King", "AssistantActivity:: date = " + date);
                            } else if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_TIME)) {
                                weekDay = resolution.getString(LuisHelper.TAG_TIME);
                                Log.i("King", "AssistantActivity:: weekDay = " + weekDay);
                            }
                        } else if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_ALARM_TITLE)) {
                            alarmTitle = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                        }
                    }

                    if (TextUtils.isEmpty(minute)) {
                        minute = "00";
                    }

                    if (!TextUtils.isEmpty(date)) {
                        Calendar calendar = Calendar.getInstance();
                        long startTime = calendar.getTimeInMillis();

                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddHHmm");
                        long endTime = format.parse(date + hour + minute).getTime();

                        Log.i("King", "duration time(minutes) = " + (endTime - startTime) / (1000 * 60));
                        if ((endTime - startTime) / (1000 * 60) > 24 * 60) { // one day
                            isIgnoreAlarm = true;
                        }
                    }

                    if (!isIgnoreAlarm) {
                        Intent setAlarmIntent = null;
                        setAlarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                        setAlarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                        if (!TextUtils.isEmpty(hour)) {
                            setAlarmIntent.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(hour)); // 0 ~ 23
                        }
                        if (!TextUtils.isEmpty(minute)) {
                            setAlarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(minute));
                        }
                        if (!TextUtils.isEmpty(alarmTitle)) {
                            setAlarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, alarmTitle);
                        }
                        if (!TextUtils.isEmpty(weekDay)) {
                            Log.i("King", "weekDay = " + weekDay);
                            if (Integer.parseInt(weekDay) == Calendar.SUNDAY) {
                                weekDay = String.valueOf(7);
                            } else {
                                weekDay = String.valueOf(Integer.parseInt(weekDay) - 1);
                            }
                            setAlarmIntent.putExtra(AlarmClock.EXTRA_DAYS, new ArrayList<>(Integer.parseInt(weekDay)));
                        }
                        if (setAlarmIntent != null && setAlarmIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(setAlarmIntent);
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            finish();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

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
                Toast.makeText(AssistantActivity.this, warning, Toast.LENGTH_LONG).show();
                toSpeak(warning);
            }
        }
    }

    // Robot speak
    private void toSpeak(String content) {
        mToSpeak.toSpeak(content, true);
    }
}
