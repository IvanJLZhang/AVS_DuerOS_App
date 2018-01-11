package com.wistron.demo.tool.teddybear.parent_side;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.common.RequestMethod;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.face.rest.WebServiceRequest;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.SubscriptionKey;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
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
import java.util.Timer;
import java.util.TimerTask;

public class EnrollSpeaker extends AppCompatActivity implements View.OnClickListener {
    private WebServiceRequest mRestCall;
    private String mCurProfileId = "";

    private final String TAG = "EnrollSpeaker";

    //enroll Identification Profile
//    private String mServiceHost =
//        "https://westus.api.cognitive.microsoft.com/spid/v1.0/" + "identificationProfiles/";

    //enroll Verification Profile
    private String mServiceHost =
            "https://westus.api.cognitive.microsoft.com/spid/v1.0/" + "verificationProfiles/";

    private Button btn_StartRecord, btn_StopRecord;
    private EditText tv_EnrollResult;

    private Button btn_CreateProfile, btn_GetAllProfile, btn_ResetProfile;
    private Button btn_DeleteProfile;
    private EditText tv_Profile_Info;

    private ListView listView;
    private SimpleAdapter simpleAdapter;
    private final String _IdentificationId = "id";
    private final String _IdentificationEnrollTime = "enroll_time";
    private final String _IdentificationRemainTime = "remain_time";
    private final String _IdentificationStatus = "status";

    private final String _VerificationProfileId = "id";
    private final String _enrollmentsCount = "enroll_count";
    private final String _remainingEnrollmentsCount = "remain_count";
    private final String _enrollmentStatus = "status";
    private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

    private Timer timer;
    private TimerTask task;
    private int times = 0;
    private TextView tvRecordTimer;

    private int mState = -1;    //-1:没再录制，0：录制wav，1：录制amr
    private final static int FLAG_WAV = 0;
    private final static int FLAG_AMR = 1;

    private final int TEST_ITEM_VERIFICATION = 0;
    private final int TEST_ITEM_IDENTIFICATION = 1;
    //    private int mCurTestItemFlag = TEST_ITEM_VERIFICATION;
    private int mCurTestItemFlag = TEST_ITEM_IDENTIFICATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.enroll_speaker);

        if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
            mServiceHost =
                    "https://westus.api.cognitive.microsoft.com/spid/v1.0/" + "verificationProfiles/";
        } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
            mServiceHost =
                    "https://westus.api.cognitive.microsoft.com/spid/v1.0/" + "identificationProfiles/";
        }

        findView();
        initial();

        getAllIdentificationProfileIds();
    }

    private void initial() {
        enableButtons(false);
        mRestCall = new WebServiceRequest(getString(SubscriptionKey.getSpeakerRecognitionKey()));
    }

    private void findView() {
        btn_StartRecord = (Button) findViewById(R.id.enroll_speaker_btn_start_recording);
        btn_StopRecord = (Button) findViewById(R.id.enroll_speaker_btn_stop_recording);
        tv_EnrollResult = (EditText) findViewById(R.id.enroll_speaker_result);

        tv_EnrollResult.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 5) {
                    enableButtons(true);
                }
            }
        });


//        btn_StartRecord.setOnClickListener(this);
        btn_StartRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btn_StartRecord.setText(getString(R.string.btn_start_record_touch_up));
                        if (mCurProfileId.length() > 10) {
                            tv_Profile_Info.setText("");
                            record(FLAG_WAV);
                        } else {
                            Toast.makeText(EnrollSpeaker.this, "Please select one profile id.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        btn_StartRecord.setText(getString(R.string.btn_start_record_touch_down));
                        stop();
                        break;
                }
                return false;
            }
        });

        btn_StopRecord.setOnClickListener(this);
        btn_StopRecord.setVisibility(View.GONE);

        btn_CreateProfile = (Button) findViewById(R.id.create_profile);
        btn_GetAllProfile = (Button) findViewById(R.id.get_all_profile);
        btn_CreateProfile.setOnClickListener(this);
        btn_GetAllProfile.setOnClickListener(this);

        btn_ResetProfile = (Button) findViewById(R.id.reset_profile);
        btn_ResetProfile.setOnClickListener(this);

        btn_DeleteProfile = (Button) findViewById(R.id.delete_profile);
        btn_DeleteProfile.setOnClickListener(this);

        tv_Profile_Info = (EditText) findViewById(R.id.profile_info);
        tv_Profile_Info.setMovementMethod(ScrollingMovementMethod.getInstance());

        listView = (ListView) findViewById(R.id.list_profiles);
        simpleAdapter = new SimpleAdapter(this, list,
                R.layout.enroll_speaker_listview_item,
                mCurTestItemFlag == TEST_ITEM_IDENTIFICATION ?
                        new String[]{_IdentificationId, _IdentificationEnrollTime,
                                _IdentificationRemainTime, _IdentificationStatus} :
                        new String[]{_VerificationProfileId, _enrollmentsCount,
                                _remainingEnrollmentsCount, _enrollmentStatus},
                new int[]{R.id.item_profile_id, R.id.item_enroll_time,
                        R.id.item_remain_time, R.id.item_status});

        listView.setAdapter(simpleAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = list.get(position);
                if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                    mCurProfileId = map.get(_VerificationProfileId).toString();
                } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                    mCurProfileId = map.get(_IdentificationId).toString();
                }
                tv_EnrollResult.setText(mCurProfileId);
            }
        });

        tvRecordTimer = (TextView) findViewById(R.id.record_time);
        tvRecordTimer.setVisibility(View.GONE);

        setDisplayFonts();

    }

    private void setDisplayFonts() {
        Typeface typeface1 = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");

        btn_StopRecord.setTypeface(typeface1);
        btn_StartRecord.setTypeface(typeface1);
        btn_CreateProfile.setTypeface(typeface1);
        btn_DeleteProfile.setTypeface(typeface1);
        btn_GetAllProfile.setTypeface(typeface1);
        btn_ResetProfile.setTypeface(typeface1);

        ((TextView) findViewById(R.id.show_warning_info)).setTypeface(typeface1);
        tv_EnrollResult.setTypeface(typeface1);
        tv_Profile_Info.setTypeface(typeface1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.enroll_speaker_btn_start_recording:
                if (mCurProfileId.length() > 10) {
                    times = 0;
//                    initTimer();
                    record(FLAG_WAV);
                } else {
                    Toast.makeText(EnrollSpeaker.this, "Please select one profile id.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.enroll_speaker_btn_stop_recording:
                stop();
                break;
            case R.id.create_profile:
                CreateProfile createProfile = new CreateProfile(1);
                createProfile.execute("");
                break;
            case R.id.get_all_profile:
                CreateProfile getProfile = new CreateProfile(2);
                getProfile.execute("");
                break;
            case R.id.delete_profile:
                if (mCurProfileId.length() <= 0) {
                    Toast.makeText(EnrollSpeaker.this, "Please select one profile id.", Toast.LENGTH_SHORT).show();
                    return;
                }
                tv_Profile_Info.setText("");
                CreateProfile deleteProfile = new CreateProfile(5);
                deleteProfile.execute("");
                break;
            case R.id.reset_profile:
                if (mCurProfileId.length() <= 0) {
                    Toast.makeText(EnrollSpeaker.this, "Please select one profile id.", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(EnrollSpeaker.this)
                        .setTitle("Warning")
                        .setMessage("Are you sure need to reset profile?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                tv_Profile_Info.setText("");
                                CreateProfile resetProfile = new CreateProfile(3);
                                resetProfile.execute("");
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setCancelable(false)
                        .show();
                break;
        }
    }

    private void initTimer() {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                times++;
                handler.obtainMessage(1).sendToTarget();
            }
        };
        timer.schedule(task, 1000, 1000);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    tvRecordTimer.setText(times + "");
                    break;
            }
            return false;
        }
    });

    private void getAllIdentificationProfileIds() {
        if (null != list && list.size() > 0) {
            list.clear();
        }
        CreateProfile getProfile = new CreateProfile(2);
        getProfile.execute("");
    }

    private class CreateProfile extends AsyncTask<String, String, String> {
        private int type = -1;

        public CreateProfile(int type) {
            this.type = type;
        }

        @Override
        protected String doInBackground(String... params) {
            String json = null;
            //create profile
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("locale", "en-us");
            if (type == 1) {
                try {
                    if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                        // create Identification profile
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com/" +
                                        "spid/v1.0/identificationProfiles",
                                RequestMethod.POST, parameters, "application/json");
                    } else if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                        //Verification Profile - Create Profile
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com/" +
                                        "spid/v1.0/verificationProfiles",
                                RequestMethod.POST, parameters, "application/json");
                    }
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (type == 2) {
                try {
                    if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                        //Identification Profile - Get All Profiles
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com/" +
                                        "spid/v1.0/identificationProfiles",
                                RequestMethod.GET, parameters, "application/json");
                    } else if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                        //Verification Profile - Get All Profiles
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com/" +
                                        "spid/v1.0/verificationProfiles",
                                RequestMethod.GET, parameters, "application/json");
                    }
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (type == 3) {
                String result = "Reset Success.";
                try {
                    if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                        //Identification Profile - Reset Enrollments
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com" +
                                        "/spid/v1.0/identificationProfiles/" +
                                        mCurProfileId +
                                        "/reset",
                                RequestMethod.POST, parameters, "application/json");
                    } else if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                        //Verification Profile - Reset Enrollments
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com" +
                                        "/spid/v1.0/verificationProfiles/" +
                                        mCurProfileId +
                                        "/reset",
                                RequestMethod.POST, parameters, "application/json");
                    }
                } catch (ClientException e) {
                    e.printStackTrace();
                    result = e.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                    result = e.toString();
                }
                json = result;
            } else if (type == 5) {
                String result = "Delete Success.";
                try {
                    if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                        //Identification Profile - Delete Profile
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com/" +
                                        "spid/v1.0/identificationProfiles/" +
                                        mCurProfileId,
                                RequestMethod.DELETE, parameters, "application/json");

                    } else if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                        //Verification Profile - Delete Profile
                        json = (String) mRestCall.request("https://westus.api.cognitive.microsoft.com/" +
                                        "spid/v1.0/verificationProfiles/" +
                                        mCurProfileId,
                                RequestMethod.DELETE, parameters, "application/json");
                    }
                } catch (ClientException e) {
                    e.printStackTrace();
                    result = e.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                    result = e.toString();
                }
                json = result;
            }
            return json;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (type == 2) {
//                Log.i(TAG,s == null ? "Null":s);
                if (null != s && s.length() > 0) {
                    try {
                        JSONArray jsonArray = new JSONArray(s);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Map<String, Object> map = new HashMap<>();

                            if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                                //Identification Profile
                                map.put(_IdentificationId, jsonObject.getString("identificationProfileId"));
                                map.put(_IdentificationEnrollTime, "" + jsonObject.getDouble("enrollmentSpeechTime"));
                                map.put(_IdentificationRemainTime, "" + jsonObject.getString("remainingEnrollmentSpeechTime"));
                                map.put(_IdentificationStatus, jsonObject.getString("enrollmentStatus"));
                            } else if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                                //Verification Profile
                                map.put(_VerificationProfileId, jsonObject.getString("verificationProfileId"));
                                map.put(_enrollmentsCount, "Enrollments Counts: " + jsonObject.getInt("enrollmentsCount"));
                                map.put(_remainingEnrollmentsCount, "Remain Counts: " + jsonObject.getInt("remainingEnrollmentsCount"));
                                map.put(_enrollmentStatus, "Enrollment Status: " + jsonObject.getString("enrollmentStatus"));
                            }
                            list.add(map);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    simpleAdapter.notifyDataSetChanged();
                }
            } else {
                if (type == 1) {
                    if (null == s) {
                        return;
                    }
                    if (null != s && s.length() > 0 && s.contains("identificationProfileId")
                            || s.contains("verificationProfileId")) {
                        getAllIdentificationProfileIds();
                    }
                }
                if ((type == 5 || type == 3) && null != s && s.contains("Success")) {
                    getAllIdentificationProfileIds();
                    if (type == 5) {
                        mCurProfileId = "";
                        tv_EnrollResult.setText(mCurProfileId);
                        enableButtons(false);
                    }
                }
                tv_Profile_Info.setText(s + "\n");
                tv_Profile_Info.setSelection(tv_Profile_Info.length(), tv_Profile_Info.length());
            }
        }
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.enroll_speaker_btn_start_recording, isRecording);
        enableButton(R.id.reset_profile, isRecording);
        enableButton(R.id.delete_profile, isRecording);
    }


    private void record(int mFlag) {
        if (mState != -1) {
            return;
        }
        int mResult = -1;
        switch (mFlag) {
            case FLAG_WAV:
                AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance(EnrollSpeaker.this, 2);
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
    private void stop() {
        if (mState != -1) {
            switch (mState) {
                case FLAG_WAV:
                    AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance(EnrollSpeaker.this, 2);
                    mRecord_1.stopRecordAndFile();

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
            File file = EnrollSpeaker.this.getExternalFilesDir(null);
            if (null != file) {
                wavFilePath = file.getAbsolutePath() + File.separator;
                Log.i(TAG, "File path: " + wavFilePath);
            }

            Log.i(TAG, "enroll URI: " + mServiceHost + mCurProfileId + "/enroll");
        }

        @Override
        protected String doInBackground(String... params) {
            String json = "";
            HttpClient httpClient = new DefaultHttpClient();

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("westus.api.cognitive.microsoft.com")
                    .appendPath("spid")
                    .appendPath("v1.0")
                    .appendPath("identificationProfiles")
                    .appendPath(mCurProfileId)
                    .appendPath("enroll")
                    .appendQueryParameter("shortAudio", "true");

            Log.i(TAG, "builder.build(): " + builder.build().toString());
            HttpPost request = new HttpPost(builder.build().toString());

//            HttpPost request = new HttpPost(mServiceHost + mCurProfileId + "/enroll");

            request.setHeader("Content-Type", "application/octet-stream");
            request.setHeader("Ocp-Apim-Subscription-Key",
                    EnrollSpeaker.this.getString(SubscriptionKey.getSpeakerRecognitionKey()));
            request.setEntity(new ByteArrayEntity((byte[]) ((byte[]) fileToByteArrayOutputStream())));

            HttpResponse response = null;
            try {
                response = httpClient.execute(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (null == response) {
                return "failed";
            }

            int statusCode1 = response.getStatusLine().getStatusCode();
            Log.i(TAG, "response= " + statusCode1);
            String result = "";
            if (statusCode1 == 200 || statusCode1 == 202) {
                if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                    try {
                        result = EntityUtils.toString(response.getEntity());
                        Log.i(TAG, "result= " + result);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i(TAG, e.toString());
                    }
                    json = "success";
                } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                    Header[] headers = response.getHeaders("Operation-Location");
                    if (null != headers) {
                        if (headers.length == 1) {
                            String content = headers[0].toString();
                            content = content.substring(content.indexOf("https:"));
                            Log.i(TAG, "content= " + content);

                            HttpGet httpGet = new HttpGet(content);
                            httpGet.setHeader("Content-Type", "application/json");
                            httpGet.setHeader("Ocp-Apim-Subscription-Key",
                                    EnrollSpeaker.this.getString(SubscriptionKey.getSpeakerRecognitionKey()));

                            for (int i = 0; i < 10; i++) {
                                try {
                                    response = httpClient.execute(httpGet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                int status = response.getStatusLine().getStatusCode();
                                Log.i(TAG, "response,status= " + status);
                                if (status == 200 || status == 202) {
                                    try {
                                        result = EntityUtils.toString(response.getEntity());
                                        Log.i(TAG, "result= " + result);
                                        if (result.contains("succeeded") || result.contains("failed")) {
                                            break;
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Log.i(TAG, e.toString());
                                    }
                                }

                                try {
                                    Thread.sleep(5 * 1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            json = result;
                        }
                    }
                }
            } else {
                try {
                    json = EntityUtils.toString(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "json result = " + json);
            return json;
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
            Log.i(TAG, "onPostExecute: " + s);
            if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                if (null != s && s.length() > 0) {
                    if (s.contains("Enrolled") || s.contains("Enrolling")
                            || s.contains("Training") || s.contains("success")) {
                        tv_Profile_Info.setText("Enroll Succeed" + "\n");
                    } else {
                        tv_Profile_Info.setText("Enroll failed" + "\n");
                    }
                } else {
                    tv_Profile_Info.setText("Enroll failed" + "\n");
                }
            } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                if (s.contains("succeeded")) {
                    tv_Profile_Info.setText("Enroll Succeed" + "\n");
                } else {
                    tv_Profile_Info.setText("Enroll failed" + "\n");
                }
            }

//            if(s.contains("success")){
            getAllIdentificationProfileIds();
//            }

            File file = new File(wavFilePath + "FinalAudio.wav");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        saveProfileId();
    }

    private void saveProfileId() {
        SharedPreferences mySharedPreferences = getSharedPreferences("_identification",
                Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        if (null != list && list.size() > 0) {
            editor.putBoolean("isHave", true);
            String _id = list.get(0).get(_IdentificationId).toString().trim();

            for (int i = 1; i < list.size(); i++) {
                if (mCurTestItemFlag == TEST_ITEM_VERIFICATION) {
                    _id = _id + "," + list.get(i).get(_IdentificationId).toString().trim();
                } else if (mCurTestItemFlag == TEST_ITEM_IDENTIFICATION) {
                    _id = _id + "%2C" + list.get(i).get(_IdentificationId).toString().trim();
                }
            }
            editor.putString("_id", _id);

            Log.i(TAG, "Enroll Identification: " + _id);
        } else {
            editor.putBoolean("isHave", false);
        }
        editor.commit();
    }
}
