package com.wistron.demo.tool.teddybear.scene;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.asr.SpeechConstant;
import com.google.auth.oauth2.AccessToken;
import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.microsoft.projectoxford.face.common.RequestMethod;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.face.rest.WebServiceRequest;
import com.qualcomm.snapdragon.sdk.face.FaceData;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.SettingsActivity;
import com.wistron.demo.tool.teddybear.avs.AVSUseClass;
import com.wistron.demo.tool.teddybear.avs.Common;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.VoiceInputDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.framework.DcsFramework;
import com.wistron.demo.tool.teddybear.dcs.framework.DeviceModuleFactory;
import com.wistron.demo.tool.teddybear.dcs.http.HttpConfig;
import com.wistron.demo.tool.teddybear.dcs.oauth.api.IOauth;
import com.wistron.demo.tool.teddybear.dcs.oauth.api.OauthImpl;
import com.wistron.demo.tool.teddybear.dcs.systemimpl.PlatformFactoryImpl;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IMediaPlayer;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlatformFactory;
import com.wistron.demo.tool.teddybear.dcs.util.LogUtil;
import com.wistron.demo.tool.teddybear.face_recognition.CameraSurfacePreview;
import com.wistron.demo.tool.teddybear.face_recognition.DrawView;
import com.wistron.demo.tool.teddybear.face_recognition.FacialRecognitionActivity;
import com.wistron.demo.tool.teddybear.google_assistant.GoogleAssistantClient;
import com.wistron.demo.tool.teddybear.led_control.LedController;
import com.wistron.demo.tool.teddybear.led_control.LedForRecording;
import com.wistron.demo.tool.teddybear.light_control.LightControllerService;
import com.wistron.demo.tool.teddybear.ocr_tts.OcrTtsActivity;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.WisShellCommandHelper;
import com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup.BDWakeupService;
import com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup.BaiduWakeupService;
import com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup.IWakeupListener;
import com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup.MessageWakeupListener;
import com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup.WakeupParams;
import com.wistron.demo.tool.teddybear.scene.baidu_stt_online.BaiduSTTRecognizer;
import com.wistron.demo.tool.teddybear.scene.baidu_stt_online.IStatus;
import com.wistron.demo.tool.teddybear.scene.baidu_stt_online.MessageRecogListener;
import com.wistron.demo.tool.teddybear.scene.baidu_stt_online.StatusRecogListener;
import com.wistron.demo.tool.teddybear.scene.google_stt_cloud.GSAccessTokenLoader;
import com.wistron.demo.tool.teddybear.scene.google_stt_cloud.GSVoiceRecorder;
import com.wistron.demo.tool.teddybear.scene.google_stt_cloud.GSapi;
import com.wistron.demo.tool.teddybear.scene.helper.BluetoothSearch;
import com.wistron.demo.tool.teddybear.scene.helper.BtConfigDevice;
import com.wistron.demo.tool.teddybear.scene.helper.LocalLuisGenerate;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.NetworkAccessHelper;
import com.wistron.demo.tool.teddybear.scene.helper.NotificationToDatabaseListener;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;
import com.wistron.demo.tool.teddybear.scene.helper.SyncCloudConfig;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.LampBulbBase;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.PhilipsHueLampBulb;
import com.wistron.demo.tool.teddybear.scene.luis_scene.DateTimeResponseScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.EmailNotificationScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.FaceEmotionScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisAlarmScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisLampBulbScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisLanguageSettingScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisMusicScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisNewsScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisPlacesScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisWeatherScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.LuisYoutubeVideoScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.MonitorModeScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.OCRScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.PlayGameScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.PlayMemoScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.SVAManagerScene;
import com.wistron.demo.tool.teddybear.scene.luis_scene.SceneBase;
import com.wistron.demo.tool.teddybear.scene.luis_scene.SpeakerRecognitionScene;
import com.wistron.demo.tool.teddybear.scene.sync_msg_by_bt.SyncMessageService;
import com.wistron.demo.tool.teddybear.scene.useless.ChatRobotActivity;
import com.wistron.demo.tool.teddybear.scene.useless.PlaySoundsFromP;
import com.wistron.demo.tool.teddybear.scene.view.FlatButton;
import com.wistron.demo.tool.teddybear.scene.view.FlatUI;
import com.wistron.demo.tool.teddybear.scene.voiceactivation.VwuService;
import com.wistron.demo.tool.teddybear.wifi_setup.service.WifiDirectSetupService;
import com.wistron.demo.tool.teddybear.wifi_setup.service.WifiSetupService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper.STT_ENGINE_MICROSOFT;
import static com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper.WAKEUP_ENGINE_BAIDU;
import static com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper.WAKEUP_ENGINE_SVA;

public class SceneActivity extends AppCompatActivity implements View.OnClickListener, Camera.PreviewCallback, IStatus {
    private static final String TAG = "SceneActivity";
    private static final String[] neededPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,

            //SVA
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.DISABLE_KEYGUARD,
            Manifest.permission.CAPTURE_AUDIO_OUTPUT,
            "android.permission.CAPTURE_AUDIO_HOTWORD",
    };
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final int TIMEOUT_TO_AUTO_STOP_STT = 15 * 1000;

    public static final int MSG_UPDATE_LOG = 50;
    private static final int MSG_PERFORM_SPEAKING = 100;
    private static final int MSG_ENABLE_BUTTON = 101;
    private static final int MSG_STT_STOP = 102;
    private static final int MSG_START_INITIAL = 103;
    private static final int MSG_DECODE_SCENE_BY_LUIS = 104;
    //SVA
    public static final int MSG_START_SVA_SERVICE = 105;
    public static final int MSG_FIRST_RUN_END = 106;
    public static final int MSG_SPEAKING_BUTTON = 107;
    public static final int MSG_SVA_VERIFY_RECORD_TASK = 108;
    public static final int MSG_SVA_DELETE_THIS_SM = 109;
    public static final int MSG_SVA_GET_USER_NAME = 110;
    public static final int MSG_SVA_WAIT_BUFFER_DATA = 111;
    public static final int MSG_SVA_DECODE_BUFFERING_DATA = 112;
    //Google STT
    public static final int MSG_GOOGLE_STT_STOP = 200;

    private static final String SHAREDPREFERENCES_FACE = "face";
    private static final String SHAREDPREFERENCES_FACE_ENABLE_KEY = "face_enable";

    private Button btn_Cmd_Teddy, btn_Cmd_Alexa, btn_Cmd_Google, btn_Cmd_Baidu;
    private Button btn_Message, btn_WifiDirect;

    private TextView tv_SceneTitle;
    private static EditText tv_SceneContent;

    private MainHandler mMainHandler;
    private ToSpeak mToSpeak;

    private DataRecognitionClient mMicrosoftDataClient = null;
    public MicrophoneRecognitionClient mMicrosoftMicClient = null;
    private int mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_INITIAL;
    private boolean isSTTInitialed = false;
    public static String Action_MemoMessage = "com.wistron.teddybear.scene.messagebutton";

    public static String mCurSpeakingLanguage = null;
    public static int mCurSttEngine = STT_ENGINE_MICROSOFT;

    /* Google stt engine */
    private android.speech.SpeechRecognizer googleSttRecognizer;
    private Intent googleRecognitionIntent;

    private DecodeSceneByLUISTask mDecodeSceneTask;
    private SceneBase mCurScene;

    private WisShellCommandHelper mShellCommandHelper;
    private boolean isSpeakingButtonPressed = false;

    private Handler mAsyncHandler;
    private HandlerThread mAsyncHandlerThread;

    private boolean isNewSceneSTT = false;
    private String mLastCommand;
    private boolean isPandoMicroMute;

    private boolean initializedOVER = false;

    private SyncMessageService mSyncMsgService;

    // Philips hue light
    private PhilipsHueLampBulb mPhilipsHueLampBulb;

    //google cloud speech --> STT
    private GSapi mGSttApi;
    private AccessToken googleCloudToken;

    //for AVS
    private AVSUseClass avsUseClass = null;

    // Google Assistant
    private GoogleAssistantClient mGoogleAssistClient;

    // baidu online STT
    protected BaiduSTTRecognizer baiduSTTRecognizer;

    // baidu wakeup service
    BDWakeupService bdWakeupService;
    BaiduWakeupService baiduWakeupService;

    // DCS service
    DcsFramework dcsFramework;
    DeviceModuleFactory deviceModuleFactory;
    IPlatformFactory platformFactory;
    private boolean isStopListenReceiveing;


    // wakeup engine
    int mCurrentWakeupEngine = WAKEUP_ENGINE_BAIDU;

    //Face recognition
    public static final String HASH_NAME = "HashMap";
    private HashMap<String, String> hash;
    public String personName = null;
    public static FacialProcessing faceObj;
    public final int confidence_value = 58;
    public static boolean activityStartedOnce = false;
    public static final String ALBUM_NAME = "serialize_deserialize";
    private long currentTime = 0;
    private long oldTime = 0;
    private Camera cameraObj; // Accessing the Android native Camera.
    private FrameLayout preview;
    private CameraSurfacePreview mPreview;
    private int FRONT_CAMERA_INDEX = 1;
    private int BACK_CAMERA_INDEX = 0;
    private OrientationEventListener orientationListener;
    private int frameWidth;
    private int frameHeight;
    private boolean initFlag = false;
    private boolean cameraFacingFront = CameraSurfacePreview.cameraFacingFront;
    private static FacialProcessing.PREVIEW_ROTATION_ANGLE rotationAngle = FacialProcessing.PREVIEW_ROTATION_ANGLE.ROT_90;
    private DrawView drawView;
    private FaceData[] faceArray; // Array in which all the face data values will be returned for each face detected.
    private SharedPreferences mFaceSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFaceSharedPreferences = getSharedPreferences(SHAREDPREFERENCES_FACE, Context.MODE_PRIVATE);
        FlatUI.initDefaultValues(this);
        FlatUI.setDefaultTheme(FlatUI.SKY);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scene);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = 0;
            for (; i < neededPermissions.length; i++) {
                if (checkSelfPermission(neededPermissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(neededPermissions, REQUEST_CODE_ASK_PERMISSIONS);
                    break;
                }
            }
            if (i >= neededPermissions.length) {
                preInitial();
            }
        } else {
            preInitial();
        }

        //Start light controller service
        Intent intent = new Intent();
        intent.setClass(this, LightControllerService.class);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (initFlag && isFaceRecognition()) {
            hash = retrieveHash(this);
            if (hash != null && hash.size() > 0) {
                if (cameraObj != null) {
                    stopCamera();
                }
                startCamera();
            }
        }
        initFlag = true;
    }

    // enable Notification access start
    @Override
    protected void onStart() {
        super.onStart();
        boolean isHaveNotificationAccess = false;
        String flat = Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(getPackageName(), cn.getPackageName())) {
                        isHaveNotificationAccess = true;
                        break;
                    }
                }
            }
        }

        if (!isHaveNotificationAccess) {
            ComponentName cn = new ComponentName(getPackageName(), NotificationToDatabaseListener.class.getName());
            Settings.Secure.putString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS,
                    TextUtils.isEmpty(flat) ? cn.flattenToString() : flat + ":" + cn.flattenToString());
            flat = Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
            Log.i("King", "add notification access package = " + flat);
        }
    }

    private boolean isFaceRecognition() {
        boolean isEnable = mFaceSharedPreferences.getBoolean(SHAREDPREFERENCES_FACE_ENABLE_KEY, false);
        Log.i("aaron", "isface " + isEnable);
        return isEnable;
    }

    // enable Notification access end

    private void enableButton(boolean enable) {
        btn_Message.setEnabled(enable);
        btn_Cmd_Teddy.setEnabled(enable);
        btn_Cmd_Alexa.setEnabled(enable);
        btn_Cmd_Google.setEnabled(enable);
        btn_Cmd_Baidu.setEnabled(enable);
    }

    private void preInitial() {
        // initial Settings parameters
        SceneCommonHelper.initSettingsParameters();
        openWifi();

        // turn off Pando blue led light
        LedForRecording.turnOffPandoBlueLight();

        // start Sync message service
        boolean isSyncNotificationEnabled = SceneCommonHelper.isEnableSyncNotification();
        Log.i("King", "enableSyncNotification = " + isSyncNotificationEnabled);
        if (isSyncNotificationEnabled) {
            Intent syncMsgIntent = new Intent(this, SyncMessageService.class);
            bindService(syncMsgIntent, SyncMsgConnection, BIND_AUTO_CREATE);
        }

        findView();
        isPandoMicroMute = SceneCommonHelper.getPandoMicroMute(this);
        SceneCommonHelper.pushLedShellFiles(this);
        mMainHandler = new MainHandler();
        mToSpeak = ToSpeak.getInstance(this).init();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCurSpeakingLanguage = sharedPreferences.getString(SceneSettingsFragment.KEY_SPEAKING_LANGUAGE, getString(R.string.preference_speaking_language_default_value));

        enableButton(false);
        BluetoothSearch.getInstance(getApplicationContext()).start();
        initialSTT();

        if (avsUseClass == null) {
            avsUseClass = AVSUseClass.getInstance(SceneActivity.this);
            avsUseClass.setAVSUseListener(avsUseListener);
        }

        // Cloud storage
        SyncCloudConfig syncCloud = new SyncCloudConfig(this, mMainHandler);
        syncCloud.syncCloudSettings();

        // Bind SVA service
        bindSVAService();
        pre2initial();
    }

    private void openWifi() {
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    AVSUseClass.AvsUseListener avsUseListener = new AVSUseClass.AvsUseListener() {
        @Override
        public void stopRecord() {
            Log.i("Bob_AVS", "avs stop record");
            mMainHandler.sendEmptyMessage(MSG_START_SVA_SERVICE);
        }

        @Override
        public void requestFail(int code) {
            switch (code) {
                case 0:
                    mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.avs_request_fail), false);
                    break;
                case 1:
                    mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.avs_analyse_return_anwser_fail), false);
                    break;
                case 204:
                    mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.avs_request_nothing_came_back), false);
                    break;
                case 500:
                    mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.avs_request_http_internal_error), false);
                    break;
                case 400:
                    mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.avs_request_bad_request), false);
                    break;
            }

        }

        @Override
        public void stopCurAction() {
            startAlexaTest();
        }
    };

    private ServiceConnection SyncMsgConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncMessageService.SyncMsgServiceBinder syncMsgServiceBinder = (SyncMessageService.SyncMsgServiceBinder) service;
            mSyncMsgService = syncMsgServiceBinder.getService(SceneActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSyncMsgService = null;
        }
    };

    private void pre2initial() {
        mPhilipsHueLampBulb = new PhilipsHueLampBulb(this);
        Map<String, String> mLastBridgeInfo = mPhilipsHueLampBulb.getLastBridgeInfo();
        String lastIpAddress = mLastBridgeInfo.get(PhilipsHueLampBulb.PREF_KEY_IP);
        if (TextUtils.isEmpty(lastIpAddress)) {
            updateLog(getString(R.string.luis_assistant_philips_hue_searching));
            enableButton(false);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void[] params) {
                    mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_philips_hue_searching), false);

                    mPhilipsHueLampBulb.setOnBulbStateChangedListener(mPhilipsBulbStateChangeddListener);
                    mPhilipsHueLampBulb.startSearch();
                    return null;
                }
            }.executeOnExecutor(SceneCommonHelper.mCachedThreadPool);
        } else {
            initial();
        }

//                    initial();

    }

    private LampBulbBase.onBulbStateChangedListener mPhilipsBulbStateChangeddListener = new LampBulbBase.onBulbStateChangedListener() {
        @Override
        public void searchStart() {

        }

        @Override
        public void searchEnd() {
            Map<String, String> mLastBridgeInfo = mPhilipsHueLampBulb.getLastBridgeInfo();
            String lastIpAddress = mLastBridgeInfo.get(PhilipsHueLampBulb.PREF_KEY_IP);
            if (!TextUtils.isEmpty(lastIpAddress)) {
                Message message = mMainHandler.obtainMessage(MSG_UPDATE_LOG);
                message.obj = String.format(getString(R.string.luis_log_philips_hue_access_point_found), list.size());
                mMainHandler.sendMessage(message);

                mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_philips_hue_searching_find_bridge), true);
            } else {
                Message message = mMainHandler.obtainMessage(MSG_UPDATE_LOG);
                message.obj = getString(R.string.luis_assistant_philips_hue_searching_not_find_bridge);
                mMainHandler.sendMessage(message);

                mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_philips_hue_searching_not_find_bridge), false);
            }
            mMainHandler.sendEmptyMessage(MSG_START_INITIAL);
        }

        @Override
        public void updateBulbLog(String log) {

        }

        @Override
        public void updateBulbLogAndSpeak(String log) {

        }
    };

    // SVA function start
    private void bindSVAService() {
        mCurrentWakeupEngine = SceneCommonHelper.getmWakeupEngine();
        Log.i("Ivan", "current wakeup service: " + mCurrentWakeupEngine + "; 0-SVA, 1-Baidu");
        if(mCurrentWakeupEngine == WAKEUP_ENGINE_SVA) {
            Intent intent = new Intent();
            intent.setClass(this, VwuService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }else if(mCurrentWakeupEngine == WAKEUP_ENGINE_BAIDU){
            IWakeupListener listener = new MessageWakeupListener(mMainHandler);
            bdWakeupService = new BDWakeupService(this, listener);

            Intent intent = new Intent();
            intent.setClass(this, BaiduWakeupService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private VwuService vwuService;
    private boolean isSVAConn = false;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            isSVAConn = false;
            Log.v("SVA", "\"DisConnect!" + isSVAConn);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            if(mCurrentWakeupEngine == WAKEUP_ENGINE_SVA) {
                Log.v("SVA", "VWUService is connect");
                isSVAConn = true;
                VwuService.SvaServiceBinder svaServiceBinder = (VwuService.SvaServiceBinder) service;
                vwuService = svaServiceBinder.getService();
                Log.v("SVA", "Connect!" + isSVAConn);
                vwuService.setMainHandler(mMainHandler, SceneActivity.this);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!initializedOVER || !isSTTInitialed) {
                            try {
                                Thread.sleep(1000);
                                Log.v("SVA", "waiting for initial.");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        vwuService.detectSoundModule();
                    }
                }).start();
            }else if(mCurrentWakeupEngine == WAKEUP_ENGINE_BAIDU){
                BaiduWakeupService.WakeupBinder wakeupBinder = (BaiduWakeupService.WakeupBinder)service;
                baiduWakeupService = wakeupBinder.getService();
                baiduWakeupService.setBdWakeupService(bdWakeupService);
                startWakeupService();
            }
        }
    };
    // SVA function end

    private void initial() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Action_MemoMessage);
        registerReceiver(receiverNewMessage, filter);

        mAsyncHandlerThread = new HandlerThread(getLocalClassName());
        mAsyncHandlerThread.start();
        mAsyncHandler = new Handler(mAsyncHandlerThread.getLooper());

        //startDetectKey();

        //Bob start
        //startMonitorModeService();
        getAllIdentificationProfileIds();
        registerNotifyUiReceiver();
        //Bob end
        enableButton(true);

        initFramework();

        initializedOVER = true;
        if (Camera.getNumberOfCameras() > 0) {
            hash = retrieveHash(this);
            initFaceRecognition();
            if (hash != null && hash.size() > 0 && isFaceRecognition()) {
                if (cameraObj != null) {
                    stopCamera();
                }
                startCamera();
            }
        }
    }


    private void findView() {
        tv_SceneTitle = (TextView) findViewById(R.id.et_scene_title);
        tv_SceneContent = (EditText) findViewById(R.id.et_scene_content);
        tv_SceneContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        btn_Cmd_Teddy = (Button) findViewById(R.id.btn_scene_command_teddy);
        btn_Cmd_Alexa = (Button) findViewById(R.id.btn_scene_command_alexa);
        btn_Cmd_Google = (Button) findViewById(R.id.btn_scene_command_google);
        btn_Cmd_Baidu = (Button) findViewById(R.id.btn_scene_command_baidu);
        btn_Message = (Button) findViewById(R.id.btn_new_message);
        btn_WifiDirect = (Button) findViewById(R.id.enable_wifi_direct_mode);

        btn_Cmd_Teddy.setOnClickListener(this);
        btn_Cmd_Alexa.setOnClickListener(this);
        btn_Cmd_Google.setOnClickListener(this);
        btn_Cmd_Baidu.setOnClickListener(this);
        btn_Message.setOnClickListener(this);
        btn_WifiDirect.setOnClickListener(this);
        findViewById(R.id.show_monitor_mode_status).setOnClickListener(this);

        try {
            PackageInfo packinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version_info)).setText(String.format(getString(R.string.version_info), packinfo.versionName, packinfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //set view fonts
        Typeface fontTypeface = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");

        tv_SceneTitle.setTypeface(fontTypeface, Typeface.BOLD);
        tv_SceneContent.setTypeface(fontTypeface);

        btn_Message.setTypeface(fontTypeface);
        btn_WifiDirect.setTypeface(fontTypeface);
        btn_Cmd_Teddy.setTypeface(fontTypeface);
        btn_Cmd_Alexa.setTypeface(fontTypeface);
        btn_Cmd_Google.setTypeface(fontTypeface);
        btn_Cmd_Baidu.setTypeface(fontTypeface);
        ((FlatButton) findViewById(R.id.show_monitor_mode_status)).setTypeface(fontTypeface);

        ((TextView) findViewById(R.id.version_info)).setTypeface(fontTypeface);
    }

    /*
     * Stops the camera preview. Releases the camera. Make the objects null.
     */
    private void stopCamera() {

        if (cameraObj != null) {
            cameraObj.stopPreview();
            cameraObj.setPreviewCallback(null);
            preview.removeView(mPreview);
            cameraObj.release();
        }
        cameraObj = null;
    }

    /*
     * Method that handles initialization and starting of camera.
     */
    private void startCamera() {
        if (cameraFacingFront) {
            cameraObj = Camera.open(FRONT_CAMERA_INDEX); // Open the Front camera
        } else {
            cameraObj = Camera.open(BACK_CAMERA_INDEX); // Open the back camera
        }
        mPreview = new CameraSurfacePreview(this, cameraObj,
                orientationListener); // Create a new surface on which Camera will be displayed.
        preview = (FrameLayout) findViewById(R.id.facePreview);
        preview.addView(mPreview);
        cameraObj.setPreviewCallback(this);
        frameWidth = cameraObj.getParameters().getPreviewSize().width;
        frameHeight = cameraObj.getParameters().getPreviewSize().height;
    }

    /*
     * Function to retrieve the byte array from the Shared Preferences.
     */
    public void loadAlbum() {
        SharedPreferences settings = getSharedPreferences(ALBUM_NAME, 0);
        String arrayOfString = settings.getString("albumArray", null);

        byte[] albumArray = null;
        if (arrayOfString != null) {
            String[] splitStringArray = arrayOfString.substring(1,
                    arrayOfString.length() - 1).split(", ");

            albumArray = new byte[splitStringArray.length];
            for (int i = 0; i < splitStringArray.length; i++) {
                albumArray[i] = Byte.parseByte(splitStringArray[i]);
            }
            faceObj.deserializeRecognitionAlbum(albumArray);
            Log.e("TAG", "De-Serialized my album");
        }
    }

    /*
     * Function to retrieve a HashMap from the Shared preferences.
     * @return
     */
    protected HashMap<String, String> retrieveHash(Context context) {
        SharedPreferences settings = context.getSharedPreferences(HASH_NAME, 0);
        HashMap<String, String> hash = new HashMap<String, String>();
        hash.putAll((Map<? extends String, ? extends String>) settings.getAll());
        return hash;
    }

    private void initFaceRecognition() {
        orientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {

            }
        };
        if (!activityStartedOnce) { // Check to make sure FacialProcessing object is not created multiple times.
            activityStartedOnce = true;
            // Check if Facial Recognition feature is supported in the device
            boolean isSupported = FacialProcessing
                    .isFeatureSupported(FacialProcessing.FEATURE_LIST.FEATURE_FACIAL_RECOGNITION);
            if (isSupported) {
                Log.d(TAG, "Feature Facial Recognition is supported");
                faceObj = FacialProcessing.getInstance();
                loadAlbum(); // De-serialize a previously stored album.
                if (faceObj != null) {
                    faceObj.setRecognitionConfidence(confidence_value);
                    faceObj.setProcessingMode(FacialProcessing.FP_MODES.FP_MODE_STILL);
                }
            } else { // If Facial recognition feature is not supported then display an alert box.
                Log.e(TAG, "Feature Facial Recognition is NOT supported");
                new AlertDialog.Builder(this)
                        .setMessage(
                                "Your device does NOT support Qualcomm's Facial Recognition feature. ")
                        .setCancelable(false)
                        .setNegativeButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        SceneActivity.this.finish();
                                    }
                                }).show();
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        boolean result = false;
        faceObj.setProcessingMode(FacialProcessing.FP_MODES.FP_MODE_VIDEO);
        if (cameraFacingFront) {
            result = faceObj.setFrame(data, frameWidth, frameHeight, true,
                    rotationAngle);
        } else {
            result = faceObj.setFrame(data, frameWidth, frameHeight, false,
                    rotationAngle);
        }
        if (result) {
            int numFaces = faceObj.getNumFaces();
            if (numFaces == 0) {
                Log.d("TAG", "No Face Detected");
                if (drawView != null) {
                    preview.removeView(drawView);
                    drawView = new DrawView(this, null, false);
                    preview.addView(drawView);
                }
            } else {
                faceArray = faceObj.getFaceData();
                if (faceArray == null) {
                    Log.e("TAG", "Face array is null");
                } else {
                    int surfaceWidth = mPreview.getWidth();
                    int surfaceHeight = mPreview.getHeight();
                    faceObj.normalizeCoordinates(surfaceWidth, surfaceHeight);
                    preview.removeView(drawView); // Remove the previously created view to avoid unnecessary stacking of
                    // Views.
                    drawView = new DrawView(this, faceArray, true);
                    Log.i("a", "DrawView DrawView");
                    speakingName();
                    preview.addView(drawView);
                }
            }
        }
    }

    private void getName() {
        personName = null;
        for (int i = 0; i < faceArray.length; i++) {

            String selectedPersonId = Integer.toString(faceArray[i]
                    .getPersonId());
            String personName = null;
            Iterator<HashMap.Entry<String, String>> iter = hash.entrySet()
                    .iterator();
            while (iter.hasNext()) {
                HashMap.Entry<String, String> entry = iter.next();
                if (entry.getValue().equals(selectedPersonId)) {
                    personName = entry.getKey();
                    //aaron
                    if (i == 0) {
                        this.personName = personName;
                    }
                    //end
                }
            }
        }
    }

    private void speakingName() {
        getName();
        if (personName != null) {

            currentTime = System.currentTimeMillis();
            Log.i("a", "currentTime  " + currentTime);
            Log.i("a", "oldTime " + oldTime);
            if (currentTime - oldTime > 5000) {
                Log.i("a", "speak name");
                Message msg = new Message();
                msg.what = MSG_SPEAKING_BUTTON;
                msg.arg1 = VwuService.SELECTED_PROVIDER_TEDDY;
                msg.obj = personName;
                mMainHandler.sendMessage(msg);
                personName = null;
            }
            oldTime = currentTime;
        }

    }

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_LOG:
                    updateLog(msg.obj.toString());
                    break;
                case MSG_PERFORM_SPEAKING:
                    performBtnCmd();
                    break;
                case MSG_ENABLE_BUTTON:
                    enableButton(true);
                    break;
                case MSG_STT_STOP:
                    Log.i("King", "-----> Timeout to stop STT.");
                    //if (mCurrentSTTStatus == SceneCommonHelper.STT_STATUS_START_RECORDING){
                    mMicrosoftMicClient.endMicAndRecognition();
                    enableButton(true);
                    updateLog("--- Stop listening voice input!");
                    mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_STOP_RECORDING;
                    //}
                    break;
                case MSG_START_INITIAL:
                    initial();
                    break;
                case MSG_DECODE_SCENE_BY_LUIS:
                    String luisResult = (String) msg.obj;
                    decodeSceneByLUIS(luisResult);
                    break;
                case MSG_START_SVA_SERVICE:
                    Log.v("King", "Got a message to start SVA service.");
                    startWakeupService();
                    break;
                case MSG_FIRST_RUN_END:
                    enableButton(true);
                    break;
                case MSG_SPEAKING_BUTTON:
                    final int serviceProvider = msg.arg1;
                    if (serviceProvider == VwuService.SELECTED_PROVIDER_ALEXA) {
                        Log.i(VwuService.TAG, "You said hey alexa!!!!");
                        stopCurrentScene();
                    } else if (serviceProvider == VwuService.SELECTED_PROVIDER_TEDDY) {
                        Log.i(VwuService.TAG, "You said hello teddy!!!!");
                        pauseCurrentScene();
                    } else if (serviceProvider == VwuService.SELECTED_PROVIDER_GOOGLE) {
                        Log.i(VwuService.TAG, "You said okay google!!!!");
                        stopCurrentScene();
                    }
                    else if(serviceProvider == VwuService.SELECTED_PROVIDER_BAIDU)
                    {
                        Log.i(VwuService.TAG, "You said 小度你好!!!!");
                        stopCurrentScene();
                        startSTTAfterSVA(serviceProvider);
                        return;
                    }else {
                        Log.i(VwuService.TAG, "this should never happen.");
                        pauseCurrentScene();
                    }
                    stopSyncMsgServiceReading();

                    if (SceneCommonHelper.getBufferingMode() == SceneCommonHelper.BUFFERING_MODE_DISABLE_AND_WITH_ALERT) {
                        int decodeStyle = SceneCommonHelper.getSvaSpeakerRecognitionStyle();
                        StringBuilder sayWhatCanIDoForYou = new StringBuilder();
                        if (decodeStyle == SceneCommonHelper.SVA_SPEAKER_RECOGNITION_STYLE_UDM) {  // UDM
                            if (msg.obj != null) {
                                sayWhatCanIDoForYou.append(String.format(SceneCommonHelper.getString
                                        (SceneActivity.this, R.string.sva_to_say_hello_pre), msg.obj));
                            }
                            Log.v(VwuService.TAG, "UDM recognition. speak :" + msg.obj);
                        } else {  // BT
                            Log.v(VwuService.TAG, "BlueTooth recognition.");
                            BtConfigDevice nearestPerson = BluetoothSearch.getInstance
                                    (getApplicationContext()).getNearestPerson();
                            if (nearestPerson != null) {
                                sayWhatCanIDoForYou.append(String.format(SceneCommonHelper.getString
                                        (SceneActivity.this, R.string.sva_to_say_hello_pre), nearestPerson
                                        .getUserName()));
                            }
                        }
                        sayWhatCanIDoForYou.append(SceneCommonHelper.getString(SceneActivity.this, R.string
                                .sva_to_say_hello));

                        new AsyncTask<String, Void, Void>() {
                            @Override
                            protected Void doInBackground(String... params) {
                                if (!TextUtils.isEmpty(params[0])) {
                                    mToSpeak.toSpeak(params[0], false);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);
                                startSTTAfterSVA(serviceProvider);
                            }
                        }.execute(sayWhatCanIDoForYou.toString());
                    } else {
                        startSTTAfterSVA(serviceProvider);
                    }
                    break;
                case MSG_SVA_VERIFY_RECORD_TASK:
                    if (null != vwuService) {
                        Message recordM = vwuService.svaReceicerHandler.obtainMessage(VwuService.MSG_GETUSER_SM);
                        vwuService.svaReceicerHandler.sendMessage(recordM);
                        enableButton(false);
                    } else {
                        Log.v(VwuService.TAG, "service is null or not started");
                    }
                    break;
                case MSG_SVA_DELETE_THIS_SM:
                    if (null != vwuService) {
                        Message deleteM = vwuService.svaReceicerHandler.obtainMessage(VwuService.MSG_DELETE_SM);
                        vwuService.svaReceicerHandler.sendMessage(deleteM);
                    } else {
                        Log.v(VwuService.TAG, "service is null or not started");
                    }
                    enableButton(false);
                    break;
                case MSG_SVA_GET_USER_NAME:
                    vwuService.svaReceicerHandler.sendMessage(
                            vwuService.svaReceicerHandler.obtainMessage(
                                    VwuService.MSG_VERIFY_RECORDING, msg.obj));
                    break;
                case MSG_SVA_WAIT_BUFFER_DATA:
                    isNewSceneSTT = true;
                    mLastCommand = null;
                    pauseCurrentScene();
                    stopSyncMsgServiceReading();
                    LedForRecording.recordingStart(getApplicationContext());
                    break;
                case MSG_SVA_DECODE_BUFFERING_DATA:
                    int isBufferTaskSuccess = msg.arg1;
                    if (isBufferTaskSuccess == 0) {
                        LedForRecording.recordingStop(getApplicationContext());
                        RecognitionTask doDataReco = new RecognitionTask(mMicrosoftDataClient, SpeechRecognitionMode.ShortPhrase, (String) msg.obj);
                        try {
                            Log.i("T", "start to decode buffer data...");
                            doDataReco.execute().get(20, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            e.printStackTrace();
                            doDataReco.cancel(true);
                            //isReceivedResponse = FinalResponseStatus.Timeout;
                        }
                    }
                    break;
                case MSG_GOOGLE_STT_STOP:
                    stopVoiceRecorder(msg.obj);
                    break;

                case STATUS_FINISHED:
                    if(msg.arg2 == 1){
                        // handle recog result, recog. success
                        String result = msg.obj.toString();
                        if (result != null && !TextUtils.isEmpty(result))
                            googleNatureLanguage(result);
                        else {
                            decodeSceneByLUIS(null);
                            Log.i("Ivan", "Retry to voice command.");
                        }
                    }
                    else if(msg.arg2 == 2){
                        // finish with error
                        decodeSceneByLUIS(null);
                        Log.i("Ivan", "Retry to voice command.");
                    }
                    break;
                case STATUS_NONE:
                    stopBaiduSTTRecog();
                    break;
                default:
                    break;
            }
        }
    }

    private void startSTTAfterSVA(int serviceProvider) {
        if (serviceProvider == VwuService.SELECTED_PROVIDER_ALEXA) {
            if (btn_Cmd_Alexa.isEnabled() && initializedOVER) {
                startAlexaTest();
            }
        } else if (serviceProvider == VwuService.SELECTED_PROVIDER_GOOGLE) {
            if (btn_Cmd_Google.isEnabled() && initializedOVER) {
                startGoogleAssistantTest();
            }
        } else if (serviceProvider == VwuService.SELECTED_PROVIDER_BAIDU) {
            if(btn_Cmd_Baidu.isEnabled() && initializedOVER){
                startDcsTest();
            }
        } else {
            if (btn_Cmd_Teddy.isEnabled() && initializedOVER) {
                Log.v("SVA", "clicked");
                btn_Cmd_Teddy.performClick();
            }
        }
    }

    // Microsoft Data client decode
    private class RecognitionTask extends AsyncTask<Void, Void, Void> {
        DataRecognitionClient dataClient;
        SpeechRecognitionMode recoMode;
        String filename;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filename = filename;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Note for wave files, we can just send data from the file right to the server.
                // In the case you are not an audio file in wave format, and instead you have just
                // raw data (for example audio coming over bluetooth), then before sending up any
                // audio data, you must first send up an SpeechAudioFormat descriptor to describe
                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
                InputStream fileStream = new FileInputStream(filename);
                Log.i("T", "to read buffer data...  " + filename);
                int bytesRead = 0;
                byte[] buffer = new byte[1024];
                do {
                    // Get  Audio data to send into byte buffer.
                    bytesRead = fileStream.read(buffer);
                    Log.i("T", "to read buffer data...  bytesRead = " + bytesRead);
                    if (bytesRead > -1) {
                        // Send of audio data to service.
                        dataClient.sendAudio(buffer, bytesRead);
                    }
                } while (bytesRead > 0);
                Log.i("T", "end to decode buffer data...");
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                dataClient.endAudio();
            }

            return null;
        }
    }

    private void updateLog(String log) {
        tv_SceneContent.append(log + "\n");
        tv_SceneContent.setSelection(tv_SceneContent.length(), tv_SceneContent.length());
        Log.i("King", "" + log);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SceneCommonHelper.closeLED();

        BluetoothSearch.getInstance(getApplicationContext()).stop();

        if (null != mSyncMsgService) {
            unbindService(SyncMsgConnection);
        }

        // stop previous scene
        stopCurrentScene();
        stopDetectKey();
        resetMicrosoftSTT();

        resetBaiduSTT();

        unregisterReceiver(receiverNewMessage);

        mAsyncHandlerThread.quitSafely();
        try {
            mAsyncHandlerThread.join();
            mAsyncHandlerThread = null;
            mAsyncHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Bob add
        if (isRegisterNotifyUIReceiver) {
            unregisterReceiver(notifyUiAction);
        }
        //Bob end

        releaseRecognize();

        unbindService(connection);

        if (faceObj != null) // If FacialProcessing object is not released, then
        // release it and set it to null
        {
            faceObj.release();
            faceObj = null;
            Log.d(TAG, "Face Recog Obj released");
        }
        activityStartedOnce = false;

        //Stop light controller service
        Intent intent = new Intent();
        intent.setClass(this, LightControllerService.class);
        stopService(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F11
                || keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getRepeatCount() == 0) {
                event.startTracking();

                if (mCurScene == null || !(mCurScene instanceof RereadScene)) {
                    stopCurrentScene();
                    mCurScene = new RereadScene(this, mMainHandler);

                    tv_SceneTitle.setText(String.format(getString(R.string.scene_title_format), getString(R.string.menu_item_reread)));
                }

                tv_SceneContent.setText("");
                tv_SceneContent.scrollTo(0, 0);
                simulateScene();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            isPandoMicroMute = !isPandoMicroMute;
            SceneCommonHelper.setPandoMicroMute(this, isPandoMicroMute);
            if (isPandoMicroMute) {
                LedController.lightLed(LedController.COLORS.RED);
            } else {
                LedController.closeLed(this);
            }
        } else if (keyCode == SceneCommonHelper.getWifiSettingTriggerKeyCode()) {
            if (event.getRepeatCount() == 0)
                event.startTracking();

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Log.i("King", "you have long pressed the key: " + keyCode + " , Wifi setting trigger key: " + SceneCommonHelper.getWifiSettingTriggerKeyCode());
        if (keyCode == KeyEvent.KEYCODE_F11
                || keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i("King", "onKeyLongPress = " + event.getRepeatCount());
            if (mCurScene instanceof RereadScene) {
                ((RereadScene) mCurScene).startRecord();
            }
        } else if (keyCode == SceneCommonHelper.getWifiSettingTriggerKeyCode()) {  // default is F12
            enableWifiSetting();
        }
        return super.onKeyLongPress(keyCode, event);
    }

    private void enableWifiSetting() {
        //stop bt search
        BluetoothSearch bluetoothSearch = BluetoothSearch.getInstance(this);
        bluetoothSearch.stop();
        Intent wifiSetupServiceIntent = new Intent();
        //switch use bt or wifip2p settings
        if (SceneCommonHelper.getWifiSettingsMode() == SceneCommonHelper.WIFI_SETTINGS_MODE_BT
                ) {
            wifiSetupServiceIntent.setClass(this, WifiSetupService.class);
        } else {
            wifiSetupServiceIntent.setClass(this, WifiDirectSetupService.class);
        }
        startService(wifiSetupServiceIntent);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i("King", "onKeyUp keyCode = " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_F11
                || keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCurScene instanceof RereadScene) {
                ((RereadScene) mCurScene).stopRecord();
            }
            return true;
        }/* else if (keyCode == KeyEvent.KEYCODE_F12
                *//*|| keyCode == KeyEvent.KEYCODE_VOLUME_UP*//*) {
            performBtnCmd();
            return true;
        }*/
        return super.onKeyUp(keyCode, event);
    }

    public void performBtnCmd() {
        if (btn_Cmd_Teddy.isEnabled()) {
            btn_Cmd_Teddy.performClick();
        }
    }

    // Reserved start
    public void startDetectKey() {
        if (mShellCommandHelper == null) {
            mShellCommandHelper = new WisShellCommandHelper();
            mShellCommandHelper.setOnResultChangedListener(resultChangedListener);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mShellCommandHelper.exec("getevent -l /dev/input/event1");
            }
        }).start();

    }

    private WisShellCommandHelper.onResultChangedListener resultChangedListener = new WisShellCommandHelper.onResultChangedListener() {
        @Override
        public void onResultChanged(String result) {
            Log.i("King", "onResultChangedListener result = " + result);
            String[] ev_sw = result.split("\\s{1,}");
            if (ev_sw.length >= 3) {
                if (ev_sw[0].equals("EV_SW")) {
                    int keyValue = Integer.parseInt(ev_sw[2], 2);
                    Log.i("King", "keyValue = " + keyValue);
                    if (keyValue == 1) {
                        if (!isSpeakingButtonPressed) {
                            isSpeakingButtonPressed = true;
                        }
                    } else {
                        if (isSpeakingButtonPressed) {
                            isSpeakingButtonPressed = false;
                            mMainHandler.sendEmptyMessage(MSG_PERFORM_SPEAKING);
                        }
                    }
                }
            }
        }
    };

    public void stopDetectKey() {
        if (mShellCommandHelper != null) {
            mShellCommandHelper.destroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scene_menu, menu);
        if (Camera.getNumberOfCameras() <= 0) {
            menu.findItem(R.id.scene_menu_item_face_recognition_settings).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        hash = retrieveHash(this);
        if (hash != null && hash.size() > 0) {
            menu.findItem(R.id.scene_menu_item_face_recognition_check).setVisible(true);
            if (isFaceRecognition()) {
                Log.i("aaron", "menu disable");
                menu.findItem(R.id.scene_menu_item_face_recognition_check).setTitle(R.string.menu_face_recognition_disable);
            } else {
                Log.i("aaron", "menu enable");
                menu.findItem(R.id.scene_menu_item_face_recognition_check).setTitle(R.string.menu_face_recognition_enable);
            }
        } else {
            menu.findItem(R.id.scene_menu_item_face_recognition_check).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scene_menu_item_enroll_speaker:
                stopCurrentScene();

                Intent enrollSpeakerIntent = new Intent(this, EnrollSpeaker.class);
                startActivity(enrollSpeakerIntent);
                break;
            case R.id.scene_menu_item_reread:
                callReRead();
                break;
            case R.id.scene_menu_item_chat_robot:
                stopCurrentScene();

                Intent chatRobotIntent = new Intent(this, ChatRobotActivity.class);
                startActivity(chatRobotIntent);
                break;
            case R.id.scene_menu_settings:
                Intent settingsIntent = new Intent(this, SceneSettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.scene_menu_item_ocr_tts:
                Intent launchOcrIntent = new Intent();
                launchOcrIntent.setClass(this, OcrTtsActivity.class);
                startActivityForResult(launchOcrIntent, CommonHelper.OCR_REQUEST_CODE);
                break;
            case R.id.scene_menu_item_server_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.scene_menu_item_face_recognition_settings:
                Intent faceIntent = new Intent(this, FacialRecognitionActivity.class);
                startActivity(faceIntent);
                break;
            case R.id.scene_menu_item_face_recognition_check:
                if (isFaceRecognition()) {
                    hash = retrieveHash(this);
                    if (hash != null && hash.size() > 0) {
                        if (cameraObj != null) {
                            Log.i("aaron", "set menu disable");
                            mFaceSharedPreferences.edit().putBoolean(SHAREDPREFERENCES_FACE_ENABLE_KEY, false).commit();
                        }
                        stopCamera();
                        if (preview != null) {
                            preview.setVisibility(View.GONE);
                            Log.i("aaron", "view gone");
                        }
                    }
                } else {
                    hash = retrieveHash(this);
                    if (hash != null && hash.size() > 0) {
                        Log.i("aaron", "set menu enable");
                        mFaceSharedPreferences.edit().putBoolean(SHAREDPREFERENCES_FACE_ENABLE_KEY, true).commit();
                        if (preview != null) {
                            preview.setVisibility(View.VISIBLE);
                            Log.i("aaron", "view visible");
                        }
                        startCamera();
                    }
                }
                break;
            default:
                break;
        }
        return false;
    }
    // Reserved end

    private void callReRead() {
        stopCurrentScene();

        Intent reReadSceneIntent = new Intent(this, RereadActivity.class);
        startActivity(reReadSceneIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "You should agree all of the permissions, force exit! please retry", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            preInitial();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scene_command_teddy:
                //stop avs prev action
                if (avsUseClass != null && !avsUseClass.isStopCurrentAction()) {
                    avsUseClass.stopPrevAVS(false);
                }

                if (!isSTTInitialed) {
                    updateLog(getString(R.string.stt_in_initial));
                    mToSpeak.toSpeak(SceneCommonHelper.getString(this, R.string.stt_in_initial), false);
                    return;
                }

                enableButton(false);
                pauseCurrentScene();
                stopSyncMsgServiceReading();
                startToListenCmd(true, null);
                break;
            case R.id.btn_scene_command_alexa:
                stopCurrentScene();
                stopSyncMsgServiceReading();
                startAlexaTest();
                break;
            case R.id.btn_scene_command_google:
                stopCurrentScene();
                stopSyncMsgServiceReading();
                startGoogleAssistantTest();
                break;
            case R.id.btn_scene_command_baidu:
                stopCurrentScene();
                stopSyncMsgServiceReading();
                startDcsTest();
                break;
            case R.id.btn_new_message:
                enableButton(false);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mMainHandler.sendEmptyMessage(MSG_ENABLE_BUTTON);
                    }
                });
                if (mCurScene != null) {
                    stopCurrentScene();
                }
                mCurScene = new PlaySoundsFromP(this, mMainHandler);
                tv_SceneTitle.setText(String.format(getString(R.string.scene_title_format), "New Message"));
                btn_Message.setTextColor(Color.BLACK);
                SceneCommonHelper.closeDotLED();
                simulateScene();
                break;
            case R.id.show_monitor_mode_status:
                break;
            case R.id.enable_wifi_direct_mode:
                enableWifiSetting();
                break;
            default:
                break;
        }
    }

    private void startAlexaTest() {
        if (avsUseClass != null) {
            if (!avsUseClass.isStopCurrentAction()) {
                avsUseClass.stopPrevAVS(true);
            } else {
                Log.i(Common.TAG, "stop SVA recognize: start point");
                stopWakeupService();
                Log.i(Common.TAG, "stop SVA recognize: end point");
                // detect access token
                if (!avsUseClass.detectAccessToken()) {
                    Log.i(Common.TAG, "Has not access Token");
                    startWakeupService();
                } else {
                    //play beep sound
                    Log.i(Common.TAG, "play beep sound: start point");
                    SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper
                            .WARN_SOUND_TYPE_START, false, true);
                    Log.i(Common.TAG, "play beep sound: end point");
                    //use RawAudioRecorder recorder
                    //avsUseClass.startListening();

                    //use Google Cloud record recorder
                    avsUseClass.startGSVoiceRecorder();
                }
            }
        }
    }

    private void startGoogleAssistantTest() {
        if (mGoogleAssistClient == null) {
            mGoogleAssistClient = new GoogleAssistantClient(getApplicationContext(), mMainHandler);
        }

        if (!mGoogleAssistClient.isStop()) {
            mGoogleAssistClient.stop();
        }

        stopWakeupService();
        //play beep sound
        SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper
                .WARN_SOUND_TYPE_START, false, true);
        mGoogleAssistClient.start();
    }

    private void initFramework(){
        IOauth baiduOauth = new OauthImpl();
        if(baiduOauth.isSessionValid()){
            HttpConfig.setAccessToken(baiduOauth.getAccessToken());
        }
        else{
//            mToSpeak.toSpeak("需要登录百度帐号进行授权", false);
            return;
        }
        platformFactory = new PlatformFactoryImpl(this);
        platformFactory.getAudioRecord();
        dcsFramework = new DcsFramework(platformFactory);

        deviceModuleFactory = dcsFramework.getDeviceModuleFactory();

        deviceModuleFactory.createVoiceOutputDeviceModule();
        deviceModuleFactory.createVoiceInputDeviceModule();
        deviceModuleFactory.getVoiceInputDeviceModule().addVoiceInputListener(new VoiceInputDeviceModule.IVoiceInputListener() {
            @Override
            public void onStartRecord() {
                LogUtil.d(TAG, "onStartRecord");
                //startRecording();
            }

            @Override
            public void onFinishRecord() {
                LogUtil.d(TAG, "onFinishRecord");
                stopDcsTest();
            }
            @Override
            public void onSucceed(int statusCode) {
                LogUtil.d(TAG, "onSucceed-statusCode:" + statusCode);
                if (statusCode != 200) {
                    Toast.makeText(SceneActivity.this,
                            getResources().getString(R.string.voice_err_msg_cn),
                            Toast.LENGTH_SHORT)
                            .show();
                    mToSpeak.toSpeak(getResources().getString(R.string.voice_err_msg_cn), false);
                }
//                stopDcsTest();
            }

            @Override
            public void onFailed(String errorMessage) {
                LogUtil.d(TAG, "onFailed-errorMessage:" + errorMessage);
                Toast.makeText(SceneActivity.this,
                        getResources().getString(R.string.voice_err_msg_cn),
                        Toast.LENGTH_SHORT)
                        .show();
                mToSpeak.toSpeak(getResources().getString(R.string.voice_err_msg_cn), false);
                stopDcsTest();
            }
        });

        deviceModuleFactory.createAlertsDeviceModule();

        deviceModuleFactory.createAudioPlayerDeviceModule();
        deviceModuleFactory.getAudioPlayerDeviceModule().addAudioPlayListener(new IMediaPlayer.SimpleMediaPlayerListener() {
            @Override
            public void onPaused() {
                super.onPaused();
//                pauseOrPlayButton.setText(getResources().getString(R.string.audio_paused));
//                isPause = true;
            }

            @Override
            public void onPlaying() {
                super.onPlaying();
//                pauseOrPlayButton.setText(getResources().getString(R.string.audio_playing));
//                isPause = false;
            }

            @Override
            public void onCompletion() {
                super.onCompletion();
//                pauseOrPlayButton.setText(getResources().getString(R.string.audio_default));
//                isPause = false;
            }

            @Override
            public void onStopped() {
                super.onStopped();
//                pauseOrPlayButton.setText(getResources().getString(R.string.audio_default));
//                isPause = true;
            }
        });

        deviceModuleFactory.createSystemDeviceModule();
        deviceModuleFactory.createSpeakControllerDeviceModule();
        deviceModuleFactory.createPlaybackControllerDeviceModule();
//        deviceModuleFactory.createScreenDeviceModule();
    }
    private void stopDcsTest() {
        isStopListenReceiveing = false;
        platformFactory.getVoiceInput().stopRecord();
        platformFactory.getAudioRecord().stopRecord();
        platformFactory.releaseAudioRecord();
        LedForRecording.recordingStop(getApplicationContext());
        startWakeupService();
        enableButton(true);
    }

    private void startDcsTest() {
        isStopListenReceiveing = true;
        IOauth baiduOauth = new OauthImpl();
        if(baiduOauth.isSessionValid()){
            HttpConfig.setAccessToken(baiduOauth.getAccessToken());
        }
        else{
            mToSpeak.toSpeak(getResources().getString(R.string.dcs_need_to_login), false);
            return;
        }
        enableButton(false);
        stopWakeupService();
        try {
            Thread.sleep(1000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        LedForRecording.recordingStart(getApplicationContext());
        SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_START, false, true);

        platformFactory.getAudioRecord().startRecord();
        platformFactory.getVoiceInput().startRecord();
        deviceModuleFactory.getSystemProvider().userActivity();
    }


    public void pauseCurrentScene() {
        // stop previous scene except for LuisMusicScene && LuisYoutubeVideoScene.
        if (mCurScene != null && (mCurScene instanceof LuisMusicScene
                || mCurScene instanceof LuisYoutubeVideoScene)) {
            mCurScene.pause();
        } else {
            stopCurrentScene();
        }
    }

    public void stopCurrentScene() {
        SceneCommonHelper.closeLED();
        if (mCurScene != null) {
            mCurScene.stop();
            mCurScene = null;
        }
        if (mToSpeak != null) {
            mToSpeak.stop();
        }
    }

    private void stopSyncMsgServiceReading() {
        if (null != mSyncMsgService) {
            mSyncMsgService.stopSyncMessageServiceReading();
        }
    }

    public void initialSTT() {
        String speakingLanguage = SceneCommonHelper.getSpeakingLanguageSetting(this);
        mCurSttEngine = SceneCommonHelper.getSttEngine();
        Log.i("King", "mCurSttEngine = " + mCurSttEngine);

        if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_MICROSOFT) {
        /* Microsoft stt engine */
            if (!speakingLanguage.equals(mCurSpeakingLanguage)) {
                mCurSpeakingLanguage = speakingLanguage;
                Log.i("King", "mCurSpeakingLanguage = " + mCurSpeakingLanguage);
                mMicrosoftMicClient = null;
            }

            if (mMicrosoftMicClient == null) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        isSTTInitialed = false;

                        Log.i("King", "Start to initial Microsoft STT service...... speechKey = " + getString(SubscriptionKey.getSpeechPrimaryKey()));
                        mMicrosoftMicClient = SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                                SceneActivity.this,
                                mCurSpeakingLanguage,
                                mSpeechRecognitionEvent,
                                getString(SubscriptionKey.getSpeechPrimaryKey()),
                                SceneCommonHelper.getString(SceneActivity.this, SubscriptionKey.getLuisMainAppId()),
                                getString(SubscriptionKey.getLuisSubscriptionKey()));
                        if (SceneCommonHelper.getBufferingMode() == SceneCommonHelper.BUFFERING_MODE_ENABLE_SVA_BUFFERING) {
                            mMicrosoftDataClient =
                                    SpeechRecognitionServiceFactory.createDataClientWithIntent(
                                            SceneActivity.this,
                                            mCurSpeakingLanguage,
                                            mSpeechRecognitionEvent,
                                            getString(SubscriptionKey.getSpeechPrimaryKey()),
                                            SceneCommonHelper.getString(SceneActivity.this, SubscriptionKey.getLuisMainAppId()),
                                            getString(SubscriptionKey.getLuisSubscriptionKey()));
                            mMicrosoftDataClient.setAuthenticationUri(getString(SubscriptionKey.getSpeechAuthenticationUri()));
                        }
                        mMicrosoftMicClient.setAuthenticationUri(getString(SubscriptionKey.getSpeechAuthenticationUri()));
                        Log.i("King", "End to initial Microsoft STT service......");

                        isSTTInitialed = true;
                    }
                });
            } else {
                isSTTInitialed = true;
            }
        } else if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE) {
        /*Google stt engine */
            googleRecognitionIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            googleRecognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            googleRecognitionIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please start speaking");
            if (mCurSpeakingLanguage.equals(CommonHelper.LanguageRegion.REGION_ENGLISH_US)) {
                googleRecognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
            } else if (mCurSpeakingLanguage.equals(CommonHelper.LanguageRegion.REGION_CHINESE_CN)) {
                googleRecognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toString());
            }
            isSTTInitialed = true;
        } else if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE_CLOUD) {
            if (!speakingLanguage.equals(mCurSpeakingLanguage)) {
                mCurSpeakingLanguage = speakingLanguage;
                Log.i("King", "mCurSpeakingLanguage = " + mCurSpeakingLanguage);
            }

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (mGSttApi == null) {
                        mGSttApi = new GSapi(SceneActivity.this);
                        mGSttApi.setGSttListener(gSTTListener);
                    }
        /* Google STT Engine --- without other app's help */
                    Log.i(GSapi.TAG, "start google stt ...");
                    if (null == googleCloudToken) {
                        GSAccessTokenLoader loader = new GSAccessTokenLoader(SceneActivity.this);
                        googleCloudToken = loader.getGoogleSTTtoken();
                        if (null != googleCloudToken) {
                            mGSttApi.setAccessToken(googleCloudToken);
                        } else {
                            Log.i(GSapi.TAG, "Todo ==== googleCloudToken is null.");
                        }
                    } else {
                        Log.i(GSapi.TAG, "token existed.");
                    }
                    isSTTInitialed = true;
                }
            });

//                    getSupportLoaderManager().initLoader(LOADER_ACCESS_TOKEN, null,
//                            new LoaderManager.LoaderCallbacks<AccessToken>() {
//                                @Override
//                                public Loader<AccessToken> onCreateLoader(int id, Bundle args) {
//                                    return new GSAccessTokenLoader(SceneActivity.this);
//                                }
//
//                                @Override
//                                public void onLoadFinished(Loader<AccessToken> loader, AccessToken googleCloudToken) {
//                                    mGSttApi.setAccessToken(googleCloudToken);
//                                }
//
//                                @Override
//                                public void onLoaderReset(Loader<AccessToken> loader) {
//                                }
//                            });
        }// add baidu STT init
        else if(mCurSttEngine == SceneCommonHelper.STT_ENGINE_BAIDU_ONLINE){
            Log.i("Ivan", "Start to initial Baidu STT service...... speechKey = " +
                    getString(SubscriptionKey.getmBaiduAsrAuthInfo()[0]) + ", " +
                    getString(SubscriptionKey.getmBaiduAsrAuthInfo()[1]) + ", " +
                    getString(SubscriptionKey.getmBaiduAsrAuthInfo()[2]));
            // init baidu STT
            StatusRecogListener listener = new MessageRecogListener(mMainHandler);
            baiduSTTRecognizer = new BaiduSTTRecognizer(this, listener);
            isSTTInitialed = true;
        }
    }
    /**
     * start baidu stt recog
     */
    private void startBaiduSTTRecog(){
        Log.i("Ivan", "start baidu stt recog.");
        if(baiduSTTRecognizer == null){
            StatusRecogListener listener = new MessageRecogListener(mMainHandler);
            baiduSTTRecognizer = new BaiduSTTRecognizer(this, listener);
        }

        LedForRecording.recordingStart(getApplicationContext());
        SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_START, false, true);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        //  params.put(SpeechConstant.NLU, "enable");
        // params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800);
        // params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        //  params.put(SpeechConstant.PROP ,20000);
        params.put(SpeechConstant.LANGUAGE, "en-GB");
        params.put(SpeechConstant.APP_ID, getString(SubscriptionKey.getmBaiduAsrAuthInfo()[0]));
        params.put(SpeechConstant.APP_KEY, getString(SubscriptionKey.getmBaiduAsrAuthInfo()[1]));
        params.put(SpeechConstant.SECRET, getString(SubscriptionKey.getmBaiduAsrAuthInfo()[2]));
        baiduSTTRecognizer.start(params);
    }

    /**
     *stop baidu stt recog
     */
    private void stopBaiduSTTRecog() {
        Log.i("Ivan", "stop baidu stt recog.");
        baiduSTTRecognizer.stop();
        enableButton(true);
        LedForRecording.recordingStop(getApplicationContext());
    }


    // Google cloud STT
    private void startVoiceRecorder() {
        if (mGSVoiceRecorder != null) {
            mGSVoiceRecorder.stop();
        } else {
            mGSVoiceRecorder = new GSVoiceRecorder(mVoiceCallback);
        }
        LedForRecording.recordingStart(getApplicationContext());
        SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_START, false, true);
        mGSVoiceRecorder.start();
    }

    private void stopVoiceRecorder(Object text) {
        Log.i(GSapi.TAG, "stop voice recorder.");
        mGSVoiceRecorder.stop();
        Log.i(GSapi.TAG, "stoping voice recorder ended.");
        enableButton(true);
        LedForRecording.recordingStop(getApplicationContext());
        if (text != null && !TextUtils.isEmpty((String) text))
            googleNatureLanguage((String) text);
        else {
            decodeSceneByLUIS(null);
            Log.i(GSapi.TAG, "Retry to voice command.");
        }
    }

    private GSVoiceRecorder mGSVoiceRecorder;
    private GSapi.Listener gSTTListener = new GSapi.Listener() {

        @Override
        public void onSpeechRecognized(final String text, final boolean isFinal) {
            //if (isFinal) {
            mGSVoiceRecorder.dismiss();
            Message stopGstt = mMainHandler.obtainMessage(MSG_GOOGLE_STT_STOP);
            stopGstt.obj = text;
            mMainHandler.sendMessage(stopGstt);
            //}
        }

        @Override
        public void stopRecognized() {
            Log.i(GSapi.TAG, "stopRecognized . ");
            Message stopGstt = mMainHandler.obtainMessage(MSG_GOOGLE_STT_STOP);
            stopGstt.obj = null;
            mMainHandler.sendMessage(stopGstt);
        }
    };

    private final GSVoiceRecorder.Callback mVoiceCallback = new GSVoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            Log.i(GSapi.TAG, "onVoiceStart.");
            mGSttApi.startRecognizing(mGSVoiceRecorder.getSampleRate(), mCurSpeakingLanguage);
        }

        @Override
        public void onVoice(byte[] data, int size) {
            mGSttApi.recognize(data, size);
        }

        @Override
        public void onVoiceEnd() {
            Log.i(GSapi.TAG, "onVoiceEnd.");
            mGSttApi.finishRecognizing();
        }
    };

    public void startWakeupService() {
        if(platformFactory != null)
            platformFactory.getVoiceInput().stopRecord();
        if(mCurrentWakeupEngine == WAKEUP_ENGINE_SVA){
            if (null != vwuService) {
                vwuService.serviceStartRecog();
            }
        }
        else if(mCurrentWakeupEngine == WAKEUP_ENGINE_BAIDU){
            if(baiduWakeupService != null){
                baiduWakeupService.start(new WakeupParams(this).fetch());
            }
        }
    }

    public void stopWakeupService() {
        // stop Voice activation detection;
        if(mCurrentWakeupEngine == WAKEUP_ENGINE_SVA){
            if (null != vwuService) {
                vwuService.serviceStopRecog();
                Log.v(VwuService.TAG, "STOP Recog....  just log");
            }
        }
        else if(mCurrentWakeupEngine == WAKEUP_ENGINE_BAIDU){
            if(baiduWakeupService != null){
                baiduWakeupService.stop();
            }
        }
    }

    public void releaseRecognize(){
        if(mCurrentWakeupEngine == WAKEUP_ENGINE_SVA){
            if (null != vwuService) {
                vwuService.serviceDetachSVA();
            }
        }
        else if(mCurrentWakeupEngine == WAKEUP_ENGINE_BAIDU){
            if(baiduWakeupService != null)
                baiduWakeupService.release();
        }
    }

    public void startToListenCmd(boolean newScene, String lastCommand) {
        isNewSceneSTT = newScene;
        mLastCommand = lastCommand;

        stopWakeupService();

        Log.i("King", "start mic and recognition");
        if (newScene) {
            tv_SceneTitle.setTag(tv_SceneTitle.getText());
            tv_SceneTitle.setText("");
            tv_SceneContent.setText("");
        }

        mCurSttEngine = SceneCommonHelper.getSttEngine();
        Log.i("King", "mCurSttEngine = " + mCurSttEngine);

        if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_MICROSOFT) {
            mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_INITIAL;
            mMicrosoftMicClient.startMicAndRecognition();
        } else if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE) {
            Log.i("King", "start listening google engine..");
            //googleSttRecognizer.cancel();
            googleSttRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(SceneActivity.this);
            googleSttRecognizer.setRecognitionListener(mRecognitionListener);
            googleSttRecognizer.startListening(googleRecognitionIntent);
        } else if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE_CLOUD) {
            startVoiceRecorder();
        }
        else if(mCurSttEngine == SceneCommonHelper.STT_ENGINE_BAIDU_ONLINE){
            startBaiduSTTRecog();
        }
    }

    private void simulateScene() {
        startWakeupService();

        if (mCurScene != null) {
            SceneCommonHelper.openLED();
            mCurScene.simulate();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("King", "SceneActivity resultCode = " + resultCode);
        if (mCurScene != null) {
            mCurScene.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void decodeSceneByLUIS(String sceneTitle) {
        stopDecodeSceneTask();
        Log.i("King", "decodeSceneByLuis...  sceneTitle = " + sceneTitle);
        if (isNewSceneSTT) {
            mDecodeSceneTask = new DecodeSceneByLUISTask();
            mDecodeSceneTask.execute(sceneTitle);
        } else {
            if (TextUtils.isEmpty(sceneTitle)) {
                mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_cmd_empty_repeat)
                        + mLastCommand, false);
                startToListenCmd(isNewSceneSTT, mLastCommand);
            } else {
                    if (mCurrentWakeupEngine == WAKEUP_ENGINE_SVA &&
                            vwuService != null &&
                            vwuService.isSVATraining) {  // For the first time to register user.
                        vwuService.svaReceicerHandler.sendMessage(
                                vwuService.svaReceicerHandler.obtainMessage(
                                        VwuService.MSG_VERIFY_RECORDING, sceneTitle
                                )
                        );
                    } else {
                        // Start SVA recognize;
                        startWakeupService();
                        mCurScene.updateSttResult(sceneTitle);
                    }
            }
        }
    }

    //google STT natural language of local app
    private void googleNatureLanguage(final String text) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String requestResult = null;
                if (CommonHelper.isNetworkAvailable(SceneActivity.this)) {
                    boolean isMainApp = !(mCurScene != null && (mCurScene instanceof PlayMemoScene || mCurScene instanceof PlayGameScene));
                    String mLuisRequestUrl = LuisHelper.getLuisRequestUrl(SceneActivity.this, text, isMainApp);
                    requestResult = NetworkAccessHelper.invokeNetworkGet(mLuisRequestUrl);
                } else {
                    requestResult = LocalLuisGenerate.generateLocalLuisResult(text);
                }

                if (TextUtils.isEmpty(requestResult)) {
                    String status = SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_cmd_empty);

                    Message message = mMainHandler.obtainMessage(MSG_UPDATE_LOG);
                    message.obj = status;
                    mMainHandler.sendMessage(message);

                    mToSpeak.toSpeak(status, false);

                    startWakeupService();
//                    Message message = mMainHandler.obtainMessage(MSG_DECODE_SCENE_BY_LUIS);
//                    message.obj = null;
//                    mMainHandler.sendMessage(message);
                } else {
                    Message msg = mMainHandler.obtainMessage(MSG_DECODE_SCENE_BY_LUIS);
                    msg.obj = requestResult;
                    mMainHandler.sendMessage(msg);
                }
            }
        });
    }

    // Google STT engine
    private boolean isGoogleLocalSTTReady = false;
    private android.speech.RecognitionListener mRecognitionListener = new android.speech.RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            isGoogleLocalSTTReady = true;
            updateLog("onReadyForSpeech...");
            if (isNewSceneSTT) {
                tv_SceneTitle.setText("");
                tv_SceneContent.scrollTo(0, 0);
            }

            LedForRecording.recordingStart(getApplicationContext());
        }

        @Override
        public void onBeginningOfSpeech() {
            updateLog("onBeginningOfSpeech...");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //Log.i("King", "onRmsChanged...");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            updateLog("onBufferReceived...");
        }

        @Override
        public void onEndOfSpeech() {
            updateLog("onEndOfSpeech...");
            enableButton(true);

            LedForRecording.recordingStop(getApplicationContext());
        }

        @Override
        public void onError(int error) {
            if (!isGoogleLocalSTTReady) {
                return;
            }
            isGoogleLocalSTTReady = false;
            updateLog("onError... errorCode = " + error);
            updateLog("Please retry!");
            enableButton(true);

            if (error == SpeechRecognizer.ERROR_NO_MATCH || (mCurSttEngine == WAKEUP_ENGINE_SVA && vwuService != null && vwuService.isSVATraining)) {
                final String status = SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_cmd_google_stt_error7);

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPreExecute() {
                        updateLog(status);
                        super.onPreExecute();
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        mToSpeak.toSpeak(status, false);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        startToListenCmd(isNewSceneSTT, mLastCommand);
                    }
                }.execute();

            } else {
                LedForRecording.recordingStop(getApplicationContext());

                String status = SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_cmd_empty);
                updateLog(status);
                mToSpeak.toSpeak(status, false);
                SceneCommonHelper.closeLED();
                if (mCurScene != null && (mCurScene instanceof LuisMusicScene
                        || mCurScene instanceof LuisYoutubeVideoScene)) {
                    mCurScene.resume();
                }

                startWakeupService();
            }
        }

        @Override
        public void onResults(Bundle results) {
            isGoogleLocalSTTReady = false;
            updateLog("onResults...");
            ArrayList<String> decodeResult = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
            for (String temp : decodeResult) {
                updateLog("onResults:: result >>> " + temp);
            }
            String sceneTitle = null;
            if (decodeResult.size() > 0) {
                // get LUIS result
                sceneTitle = decodeResult.get(0);
                googleNatureLanguage(sceneTitle);
            } else {
                decodeSceneByLUIS(sceneTitle);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            updateLog("onPartialResults...");
            ArrayList<String> decodeResult = partialResults.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
            for (String temp : decodeResult) {
                updateLog("onPartialResults:: result >>> " + temp);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            updateLog("onEvent... eventType = " + eventType);
        }
    };

    // Microsoft Speech to text
    private ISpeechRecognitionServerEvents mSpeechRecognitionEvent = new ISpeechRecognitionServerEvents() {
        @Override
        public void onPartialResponseReceived(String response) {
            updateLog("Partial result received by onPartialResponseReceived()");
            updateLog(response);
        }

        @Override
        public void onFinalResponseReceived(RecognitionResult recognitionResult) {
            if (null != mMicrosoftMicClient) {
                // we got the final result, so it we can end the mic reco.  No need to do this
                // for dataReco, since we already called endAudio() on it as soon as we were done
                // sending all the data.
                mMicrosoftMicClient.endMicAndRecognition();
            }
            mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_DONE;
            mAsyncHandler.removeCallbacks(stopSTTRunnable);

            updateLog("********* Final n-BEST Results *********");
            if (recognitionResult.Results.length > 0) {
                updateLog("[ STT ]" + " Confidence=" + recognitionResult.Results[0].Confidence +
                        " Text=\"" + recognitionResult.Results[0].DisplayText + "\"");
            } else {
                onIntentReceived(null);
            }
        }

        @Override
        public void onIntentReceived(String payload) {
            updateLog("Intent received by onIntentReceived()");
            Log.i("King", "SceneActivity onIntentReceived = \n " + payload);

            decodeSceneByLUIS(payload);
        }

        @Override
        public void onError(final int errorCode, final String response) {
            mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_ERROR;
            mAsyncHandler.removeCallbacks(stopSTTRunnable);
            mMicrosoftMicClient.endMicAndRecognition();

            updateLog("Error received by onError()");
            updateLog("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
            updateLog("Error text: " + response);
            updateLog("Please retry!");

            enableButton(true);
            LedForRecording.recordingStop(getApplicationContext());

            SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_FAIL);

            // Start SVA recognize;
            startWakeupService();
        }

        @Override
        public void onAudioEvent(boolean recording) {
            if (recording && isNewSceneSTT) {
                tv_SceneTitle.setText("");
                tv_SceneContent.scrollTo(0, 0);
            }
            updateLog("Microphone status change received by onAudioEvent()");
            updateLog("********* Microphone status: " + recording + " *********");
            if (recording) {
                updateLog("--- Start listening voice input!");
                updateLog(getString(R.string.scene_mic_is_on));
                if (mCurrentSTTStatus != SceneCommonHelper.STT_STATUS_ERROR) {
                    mAsyncHandler.postDelayed(stopSTTRunnable, TIMEOUT_TO_AUTO_STOP_STT);
                    mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_START_RECORDING;
                    if (SceneCommonHelper.getBufferingMode() == SceneCommonHelper.BUFFERING_MODE_DISABLE_AND_WITH_ALERT) {
                        SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_START);
                    }

                    LedForRecording.recordingStart(getApplicationContext());
                }
            } else {
                LedForRecording.recordingStop(getApplicationContext());

                mMicrosoftMicClient.endMicAndRecognition();
                enableButton(true);
                updateLog("--- Stop listening voice input!");
                Log.i("King", "onAudioEvent mCurrentSTTStatus = " + mCurrentSTTStatus);
                if (mCurrentSTTStatus == SceneCommonHelper.STT_STATUS_INITIAL) {
                    SceneCommonHelper.playSpeakingSound(SceneActivity.this, SceneCommonHelper.WARN_SOUND_TYPE_FAIL);
                } else if (mCurrentSTTStatus != SceneCommonHelper.STT_STATUS_ERROR) {
                    SceneCommonHelper.openLED();
                }
                mCurrentSTTStatus = SceneCommonHelper.STT_STATUS_STOP_RECORDING;
            }
        }
    };

    private Runnable stopSTTRunnable = new Runnable() {
        @Override
        public void run() {
            mMainHandler.sendEmptyMessage(MSG_STT_STOP);
        }
    };

    private void stopDecodeSceneTask() {
        if (mDecodeSceneTask != null) {
            mDecodeSceneTask.cancel(true);
            mDecodeSceneTask = null;
        }
    }

    private class DecodeSceneByLUISTask extends AsyncTask<String, Void, Void> {
        static final int RETURN_NONE = -1;
        static final int RETURN_SUCCESS = 0;
        static final int RETURN_STT_ERROR = 1;
        static final int RETURN_SCENE_NULL = 2;

        static final int MIN_VOLUME_DEFINE = 1;

        SceneBase tempScene = null;
        String sceneTitle = null;
        String currentIntent = null;
        int returnCode = -1;

        private void adjustVolume(JSONArray entities) {
            try {
                int adjustTimes = 1;
                String action = null;
                for (int i = 0; i < entities.length(); i++) {
                    String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                    if (entitiesType.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLDOWN) ||
                            entitiesType.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLUP)) {
                        action = entitiesType;
                    } else if (entitiesType.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_BUILTIN_NUMBER)) {
                        adjustTimes = Integer.parseInt(entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY));
                    }
                }

                AudioManager mManager = (AudioManager) SceneActivity.this.getSystemService(Context.AUDIO_SERVICE);
                if (!TextUtils.isEmpty(action)) {
                    int originVolume = mManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxVolume = mManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    if (action.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLUP)) {
                        if (originVolume == maxVolume) {
                            mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_adjust_volume_max), false);
                        } else if ((originVolume + adjustTimes) < maxVolume) {
                            mManager.setStreamVolume(AudioManager.STREAM_MUSIC, originVolume + adjustTimes, AudioManager.FLAG_SHOW_UI);
                        } else {
                            mManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI);
                        }
                    } else {
                        if (originVolume == MIN_VOLUME_DEFINE) {
                            mToSpeak.toSpeak(SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_adjust_volume_min), false);
                        } else if ((originVolume - adjustTimes) > MIN_VOLUME_DEFINE) {
                            mManager.setStreamVolume(AudioManager.STREAM_MUSIC, originVolume - adjustTimes, AudioManager.FLAG_SHOW_UI);
                        } else {
                            mManager.setStreamVolume(AudioManager.STREAM_MUSIC, MIN_VOLUME_DEFINE, AudioManager.FLAG_SHOW_UI);
                        }
                    }
                    int newVolume = mManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    Log.i("King", "adjust volume: from " + originVolume + " to " + newVolume);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(String... params) {
            tempScene = null;
            sceneTitle = null;
            returnCode = RETURN_NONE;

            try {
                String result = params[0];
                Log.i("King", "start decode result...");
                if (!TextUtils.isEmpty(result)) {
                    JSONObject toJason = new JSONObject(result);
                    if (toJason.get(LuisHelper.TAG_QUERY) != null) {
                        sceneTitle = toJason.getString(LuisHelper.TAG_QUERY);
                        Log.i("King", "SceneActivity title = " + sceneTitle);
                        currentIntent = toJason.getJSONObject(LuisHelper.TAG_TOP_SCORING_INTENT).getString(LuisHelper.TAG_INTENT);
                        JSONArray entities = toJason.getJSONArray(LuisHelper.TAG_ENTITIES);
                        Log.i("King", "SceneActivity DecodeSceneByLUISTask:: intent = " + currentIntent);
                        switch (currentIntent) {
                            case LuisHelper.INTENT_SCENARIO_ACTIONS:
                                if (mCurScene != null && (mCurScene instanceof LuisMusicScene
                                        || mCurScene instanceof LuisYoutubeVideoScene)) {
                                    mCurScene.resetSceneActionAndParams(currentIntent, entities);
                                    tempScene = mCurScene;
                                }
                                break;
                            case LuisHelper.INTENT_ADJUST_VOLUME:
                                adjustVolume(entities);
                                returnCode = RETURN_SUCCESS;
                                break;
                            case LuisHelper.INTENT_ALARM_SET_ALARM:
                                tempScene = new LuisAlarmScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_MUSIC:
                                tempScene = new LuisMusicScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_WEATHER_CHECK:
                                tempScene = new LuisWeatherScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_PLACES:
                                tempScene = new LuisPlacesScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_EMOTION_FACE_DETECT:
                                tempScene = new FaceEmotionScene(SceneActivity.this, mMainHandler);
                                break;
                            case LuisHelper.INTENT_VOICE_SPEAKER_RECOGNITION:
                                tempScene = new SpeakerRecognitionScene(SceneActivity.this, mMainHandler);
                                break;
                            case LuisHelper.INTENT_GAME_PLAY:
                                tempScene = new PlayGameScene(SceneActivity.this, mMainHandler);
                                break;
                            case LuisHelper.INTENT_MESSAGE_PLAY_MEMO:
                                tempScene = new PlayMemoScene(SceneActivity.this, mMainHandler);
                                break;
                            case LuisHelper.INTENT_MONITOR_MODE:
                                tempScene = new MonitorModeScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_STORY:
                                tempScene = new OCRScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_LANGUAGE:
                                tempScene = new LuisLanguageSettingScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_LIGHTS_CONTROL:
                                tempScene = new LuisLampBulbScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_EMAIL_NOTIFICATION:
                                tempScene = new EmailNotificationScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_SVA_SCENE:
                                tempScene = new SVAManagerScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_DATETIME:
                                tempScene = new DateTimeResponseScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_NEWS:
                                tempScene = new LuisNewsScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            case LuisHelper.INTENT_YOUTUBE_VIDEO:
                                tempScene = new LuisYoutubeVideoScene(SceneActivity.this, mMainHandler, currentIntent, entities);
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    returnCode = RETURN_STT_ERROR;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "SceneActivity result = " + e.getMessage());
            }
            if (returnCode == RETURN_NONE) {
                if (tempScene != null) {
                    returnCode = RETURN_SUCCESS;
                } else {
                    returnCode = RETURN_SCENE_NULL;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void response) {
            super.onPostExecute(response);
            if (tempScene != null) {
                if (currentIntent.equalsIgnoreCase(LuisHelper.INTENT_SCENARIO_ACTIONS)) {
                    tv_SceneTitle.setText((String) tv_SceneTitle.getTag());
                } else {
                    stopCurrentScene();
                    tv_SceneTitle.setText(String.format(getString(R.string.scene_title_format), sceneTitle));
                }

                mCurScene = tempScene;
                simulateScene();
            } else {
                // Start SVA recognize;
                startWakeupService();

                if (returnCode != RETURN_SUCCESS) {
                    String status;
                    if (CommonHelper.isNetworkAvailable(SceneActivity.this)) {
                        if (returnCode == RETURN_STT_ERROR) {
                            status = SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_cmd_empty);
                        } else {
                            status = SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_scene_is_null);
                        }
                    } else {
                        status = SceneCommonHelper.getString(SceneActivity.this, R.string.luis_assistant_local_scenario_not_support);
                    }
                    if (!TextUtils.isEmpty(status)) {
                        updateLog(status);
                        mToSpeak.toSpeak(status, false);
                    }
                }

                SceneCommonHelper.closeLED();
                if (mCurScene != null && (mCurScene instanceof LuisMusicScene
                        || mCurScene instanceof LuisYoutubeVideoScene)) {
                    tv_SceneTitle.setText((String) tv_SceneTitle.getTag());
                    mCurScene.resume();
                }
            }
        }
    }

    private void resetMicrosoftSTT() {
        // Reset everything
        if (mMicrosoftMicClient != null) {
            mMicrosoftMicClient.endMicAndRecognition();
            try {
                mMicrosoftMicClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            mMicrosoftMicClient = null;
        }

        if (this.mMicrosoftDataClient != null) {
            try {
                this.mMicrosoftDataClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.mMicrosoftDataClient = null;
        }
    }

    private void resetBaiduSTT(){
        if(baiduSTTRecognizer != null){
            baiduSTTRecognizer.release();
        }
    }

    private BroadcastReceiver receiverNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Action_MemoMessage.equals(intent.getAction())) {
                Log.v("berlin", "receive the memo service");
                //Todo green circle led....not the ring led
                SceneCommonHelper.twinkleDotLED();

                btn_Message.setTextColor(Color.BLUE);
            }
        }
    };

    /* Bob start */
    private final int TEST_ITEM_VERIFICATION = 0;
    private final int TEST_ITEM_IDENTIFICATION = 1;
    private int mCurTestItemFlag = TEST_ITEM_IDENTIFICATION;
    private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    private final String _IdentificationId = "id";
    private final String _IdentificationEnrollTime = "enroll_time";
    private final String _IdentificationRemainTime = "remain_time";
    private final String _IdentificationStatus = "status";

    private final String _VerificationProfileId = "id";
    private final String _enrollmentsCount = "enroll_count";
    private final String _remainingEnrollmentsCount = "remain_count";
    private final String _enrollmentStatus = "status";

    private void startMonitorModeService() {
        Intent launchMonitorModeIntent = new Intent();
        launchMonitorModeIntent.setClass(this, MonitorModeService.class);
        startService(launchMonitorModeIntent);
    }

    private void getAllIdentificationProfileIds() {
        if (null != list && list.size() > 0) {
            list.clear();
        }
        CreateProfile getProfile = new CreateProfile();
        getProfile.execute("");
    }

    private class CreateProfile extends AsyncTask<String, String, String> {
        private WebServiceRequest mRestCall;

        public CreateProfile() {
            mRestCall = new WebServiceRequest(getString(SubscriptionKey.getSpeakerRecognitionKey()));
        }

        @Override
        protected String doInBackground(String... params) {
            Log.i("Bob", "start get all profiles id");
            String json = null;
            //create profile
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("locale", "en-us");
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

            return json;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
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
                    saveProfileId();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
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
//                    _id = _id + "%2C" + list.get(i).get(_IdentificationId).toString().trim();
                    _id = _id + "," + list.get(i).get(_IdentificationId).toString().trim();
                }
            }
            editor.putString("_id", _id);

            Log.i("Bob", "Enroll Identification: " + _id);
        } else {
            editor.putBoolean("isHave", false);
        }
        editor.commit();
    }

    boolean isRegisterNotifyUIReceiver = false;
    public final static String UI_ACTION = "com.wistron.teddy.bear.notify.ui";

    private void registerNotifyUiReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UI_ACTION);
        registerReceiver(notifyUiAction, filter);
        isRegisterNotifyUIReceiver = true;
    }

    private BroadcastReceiver notifyUiAction = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UI_ACTION)) {
                boolean isStart = intent.getBooleanExtra("ui", false);
                handler.obtainMessage(1, (Object) isStart).sendToTarget();
            }
        }
    };

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    boolean isStart = Boolean.parseBoolean(msg.obj.toString());
                    if (isStart) {
                        ((Button) findViewById(R.id.show_monitor_mode_status)).
                                setText("Monitor Mode\nStart");
                    } else {
                        ((Button) findViewById(R.id.show_monitor_mode_status)).
                                setText("Monitor Mode\nStop");
                    }
                    break;
            }
            return false;
        }
    });
    /* Bob end */
}
