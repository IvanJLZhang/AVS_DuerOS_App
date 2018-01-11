package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.AudioRecordFunc;
import com.wistron.demo.tool.teddybear.scene.ErrorCode;
import com.wistron.demo.tool.teddybear.scene.helper.HotwordResumeHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wistron.demo.tool.teddybear.scene.SceneActivity.MSG_START_SVA_SERVICE;

/**
 * Created by king on 16-4-11.
 */
public class SpeakerRecognitionScene extends SceneBase {
    private final static int FLAG_WAV = 0;
    private final static int FLAG_AMR = 1;

    private final int TEST_ITEM_VERIFICATION = 0;
    private final int TEST_ITEM_IDENTIFICATION = 1;
    //    private int mCurTestItemFlag = TEST_ITEM_VERIFICATION;
    private int mCurTestItemFlag = TEST_ITEM_IDENTIFICATION;

    private String mCurProfileId = "2f0e91e9-88da-4dc3-a89c-0577197652cc," +
            "f15d5e6d-ff33-45ec-af17-753540b231af";
    private final String _BASE_URI = "https://westus.api.cognitive.microsoft.com/spid/v1.0/";
    private final String _IDENTIFICATION_PROFILES_URI = "identify?identificationProfileIds=";
    private final String _VERIFICATION_PROFILE_URI = "verify?verificationProfileId=";
//    private String mServiceHost =
//            "https://westus.api.cognitive.microsoft.com/spid/v1.0/identify?identificationProfileIds=" ;

    private String mServiceHost = _BASE_URI + _VERIFICATION_PROFILE_URI;
//            "https://westus.api.cognitive.microsoft.com/spid/v1.0/verify?verificationProfileId=" ;

    public static boolean isStopRecord = false;

    private List<Map<String, Object>> list = new ArrayList<>();

    private Handler handler = null;

    private int mState = -1;    //-1:没再录制，0：录制wav，1：录制amr
    private Runnable waitStop = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(5 * 1000);
//                Thread.sleep(25 * 1000);  // for test Identification
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isStopRecord) {
                isStopRecord = true;
                handler.obtainMessage(1).sendToTarget();
            }
        }
    };

    public SpeakerRecognitionScene(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    @Override
    public void simulate() {
        super.simulate();
        if (handler == null) {
            handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            Log.i("W", "The DB value aways more than 30, thread stop record");
                            stop();
                            break;
                        case 2:
                            if (isStopRecord) {
                                Log.i("W", "the thread already stop record");
                                return false;
                            }
                            isStopRecord = true;
                            Log.i("W", "The DB value is less 30, auto stop record");
                            updateLog("Stop Record");
                            VisitNetwork visitNetwork = new VisitNetwork();
                            visitNetwork.execute("");
                            break;
                    }
                    return false;
                }
            });
        }

        SceneCommonHelper.blinkLED();
//        String content = getString(R.string.luis_assistant_speaker_recognition_please_read)+
//                context.getString(R.string.speaker_recognition_sample_text);
        String content = getString(R.string.luis_assistant_speaker_recognition_please_read);
        toSpeak(content, false);
        updateLog("\n" + content);
        updateLog("Start Record\nPlease say anything.\"");

        start();
    }

    private boolean getAllProfileId() {
        SharedPreferences mySharedPreferences = context.getSharedPreferences("_identification",
                Activity.MODE_PRIVATE);
        boolean isHave = mySharedPreferences.getBoolean("isHave", false);
        if (isHave) {
            if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                mCurProfileId = mySharedPreferences.getString("_id", mCurProfileId);
            } else if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                String content = mySharedPreferences.getString("_id", mCurProfileId);
                if (content.contains(",")) {
                    if (null != list && list.size() > 0) {
                        list.clear();
                    }
                    String[] _ids = content.split(",");
                    for (String temp : _ids) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("_id", temp);
                        map.put("result", false);
                        list.add(map);
                    }
                } else {
                    mCurProfileId = content;
                    Map<String, Object> map = new HashMap<>();
                    map.put("_id", mCurProfileId);
                    map.put("result", false);
                    list.add(map);
                }
            }
        }

        return isHave;
    }

    private void start() {
        if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
            mServiceHost = _BASE_URI + _IDENTIFICATION_PROFILES_URI;
        } else if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
            mServiceHost = _BASE_URI + _VERIFICATION_PROFILE_URI;
        }
        Log.i("W", "mServiceHost: " + mServiceHost);
        if (getAllProfileId()) {
            isStopRecord = false;
            record(FLAG_WAV);
            new Thread(waitStop).start();
        } else {
            updateLog("First, please enroll your voice");
        }
    }


    AudioRecordFunc mRecord_1;

    private void record(int mFlag) {
        if (mState != -1) {
            return;
        }
        int mResult = -1;
        switch (mFlag) {
            case FLAG_WAV:

                HotwordResumeHelper.getInstance(context).stopListening();

                mRecord_1 = AudioRecordFunc.getInstance(context, handler, 3);
//                mRecord_1 = AudioRecordFunc.getInstance(context,handler,2);  // for test Identification
                mResult = mRecord_1.startRecordAndFile();
                break;
            case FLAG_AMR:
                break;
        }
        if (mResult == ErrorCode.SUCCESS) {
            mState = mFlag;
        }
    }

    /**
     * 停止录音
     */
    public void stop() {
        super.stop();
        if (mState != -1) {
            switch (mState) {
                case FLAG_WAV:
//                    mRecord_1 = AudioRecordFunc.getInstance(context,handler);
                    mRecord_1.stopRecordAndFile();
                    //HotwordResumeHelper.getInstance(context).startListening();
                    mMainHandler.sendEmptyMessage(MSG_START_SVA_SERVICE);
                    updateLog("Stop Record");
                    VisitNetwork visitNetwork = new VisitNetwork();
                    visitNetwork.execute("");
                    break;
                case FLAG_AMR:
                    break;
            }
            mState = -1;
        }
    }

    private class VisitNetwork extends AsyncTask<String, String, String> {

        String wavFilePath = "/mnt/sdcard/";

        public VisitNetwork() {
            File file = context.getExternalFilesDir(null);
            if (null != file) {
                wavFilePath = file.getAbsolutePath() + File.separator;
                Log.i("W", "File path: " + wavFilePath);
            }

            Log.i("W", "Post URI: " + mServiceHost + mCurProfileId);
        }

        @Override
        protected String doInBackground(String... params) {
            String json = null;
            HttpClient httpClient = new DefaultHttpClient();
            if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                byte[] wavFile = fileToByteArrayOutputStream();
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> map = list.get(i);
                    String uri = mServiceHost + map.get("_id");
                    Log.i("W", "Current URI: " + uri);
                    HttpPost request = new HttpPost(uri);
                    request.setHeader("Content-Type", "application/octet-stream");
                    request.setHeader("Ocp-Apim-Subscription-Key",
                            context.getString(SubscriptionKey.getSpeakerRecognitionKey()));

                    request.setEntity(new ByteArrayEntity(wavFile));
                    HttpResponse response = null;
                    try {
                        response = httpClient.execute(request);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("W", e.toString());
                    }

                    if (null == response) {
                        continue;
                    }
                    int statusCode1 = response.getStatusLine().getStatusCode();
                    Log.i("W", "response= " + statusCode1);
                    if (statusCode1 == 200 || statusCode1 == 202) {
                        try {
                            json = EntityUtils.toString(response.getEntity());
                            Log.i("W", "result= " + json);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.i("W", e.toString());
                        }

                        if (null != json && json.length() > 0) {
                            if (json.contains("Accept")) {
                                map.put("result", true);
                                break;
                            }
                        }
                    }
                }
                return json;
            } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                json = getResult();
            }

            return json;
        }

        private String getResult() {
            HttpClient httpClient = new DefaultHttpClient();
//            HttpPost request = new HttpPost(mServiceHost + mCurProfileId);

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("westus.api.cognitive.microsoft.com")
                    .appendPath("spid")
                    .appendPath("v1.0")
                    .appendPath("identify")
                    .appendQueryParameter("identificationProfileIds", mCurProfileId)
                    .appendQueryParameter("shortAudio", "true");

//            builder.appendQueryParameter("shortAudio","true");

//            URI uri = builder.build();
            Log.i("W", "builder.build(): " + builder.build().toString());

            HttpPost request = new HttpPost(builder.build().toString());

            request.setHeader("Content-Type", "application/octet-stream");
            request.setHeader("Ocp-Apim-Subscription-Key",
                    context.getString(SubscriptionKey.getSpeakerRecognitionKey()));

            request.setEntity(new ByteArrayEntity(fileToByteArrayOutputStream()));

            HttpResponse response = null;
            try {
                response = httpClient.execute(request);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (null == response) {
                return "error";
            }
            String result = "";
            int statusCode1 = response.getStatusLine().getStatusCode();
            Log.i("W", "response= " + statusCode1);
            if (statusCode1 == 200 || statusCode1 == 202) {
                if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                    try {
                        result = EntityUtils.toString(response.getEntity());
                        Log.i("W", "result= " + result);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("W", e.toString());
                    }
                } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                    Header[] headers = response.getHeaders("Operation-Location");
                    if (null != headers) {
                        if (headers.length == 1) {
                            String content = headers[0].toString();
                            Log.i("W", "content= " + content);

                            content = content.substring(content.indexOf("https:"));
                            Log.i("W", "content= " + content);

                            Map<String, Object> parameters = new HashMap<>();
                            parameters.put("locale", "en-us");

                            HttpGet httpGet = new HttpGet(content);
                            httpGet.setHeader("Content-Type", "application/json");
                            httpGet.setHeader("Ocp-Apim-Subscription-Key",
                                    context.getString(SubscriptionKey
                                            .getSpeakerRecognitionKey()));

                            for (int i = 0; i < 10; i++) {
                                try {
                                    response = httpClient.execute(httpGet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                int status = response.getStatusLine().getStatusCode();
                                Log.i("W", "response,status= " + status);
                                if (status == 200 || status == 202) {
                                    try {
                                        result = EntityUtils.toString(response
                                                .getEntity());
                                        Log.i("W", "result= " + result);
                                        if (result.contains("succeeded") || result
                                                .contains("failed")) {
                                            break;
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Log.i("W", e.toString());
                                    }
                                }

                                try {
                                    Thread.sleep(5 * 1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            Log.i("W", "result= " + result);
                            return result;
                        }
                    }
                }

            } else {
                try {
                    result = EntityUtils.toString(response.getEntity());
                    Log.i("W", "result= " + result);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("W", e.toString());
                }

                return "error";
            }

            return "nothing";
        }

        private byte[] fileToByteArrayOutputStream() {
            FileInputStream inputStream = null;
            ByteArrayOutputStream bout = null;
            byte[] buf = new byte[1024];
            bout = new ByteArrayOutputStream();
            int length = 0;
            try {
                inputStream = new FileInputStream(new File(wavFilePath + "FinalAudio.wav"));
                try {
                    while ((length = inputStream.read(buf)) != -1) {
                        bout.write(buf, 0, length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] content = bout.toByteArray();
                return content;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (null != inputStream) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (null != bout) {
                    try {
                        bout.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                boolean isSafe = false;
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> map = list.get(i);
                    if (Boolean.parseBoolean(map.get("result").toString())) {
                        isSafe = true;
                        break;
                    }
                }

                Log.i("W", "open led");
                SceneCommonHelper.openLED();
                updateLog(isSafe ? "Safe" : "Please don't open the door");
                toSpeak(isSafe ? getString(R.string.luis_assistant_speaker_result_safe) : getString(R.string.luis_assistant_speaker_result_not_safe), false);

                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> map = list.get(i);
                    map.put("result", false);
                }
            } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                //debug
                updateLog(s);

                String returnId = null;
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONObject processingResultJson = jsonObject.getJSONObject("processingResult");
                    returnId = processingResultJson.getString("identifiedProfileId");
                    Log.i("W", returnId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SceneCommonHelper.openLED();
                boolean isSafe = s.contains("succeeded") && null != returnId && mCurProfileId
                        .contains(returnId);
                toSpeak(isSafe ? getString(R.string.luis_assistant_speaker_result_safe) : getString(R.string.luis_assistant_speaker_result_not_safe), false);
            }

            File file = new File(wavFilePath + "FinalAudio.wav");
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
