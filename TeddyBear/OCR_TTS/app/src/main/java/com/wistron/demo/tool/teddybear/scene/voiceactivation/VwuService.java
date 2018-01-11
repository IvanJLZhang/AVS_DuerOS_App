package com.wistron.demo.tool.teddybear.scene.voiceactivation;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.soundtrigger.SoundTrigger;
import android.hardware.soundtrigger.SoundTriggerModule;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.qualcomm.listen.ListenSoundModel;
import com.qualcomm.listen.ListenTypes;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.led_control.LedForRecording;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.hardware.soundtrigger.SoundTrigger.ConfidenceLevel;
import static android.hardware.soundtrigger.SoundTrigger.KeyphraseRecognitionEvent;
import static android.hardware.soundtrigger.SoundTrigger.KeyphraseRecognitionExtra;
import static android.hardware.soundtrigger.SoundTrigger.KeyphraseSoundModel;
import static android.hardware.soundtrigger.SoundTrigger.ModuleProperties;
import static android.hardware.soundtrigger.SoundTrigger.RecognitionConfig;
import static android.hardware.soundtrigger.SoundTrigger.RecognitionEvent;
import static android.hardware.soundtrigger.SoundTrigger.SoundModelEvent;
import static android.hardware.soundtrigger.SoundTrigger.StatusListener;

public class VwuService extends Service implements StatusListener {
    //luis intent:teddybear.sva.scene  entity:teddybear.entity.type.reserved
    public final static String TAG = "SVA";

    //    public final static int MSG_REGISTER_CLIENT = 10501;
    public final static int MSG_LOAD_START_RECOG = 10510;
    public final static int MSG_VERIFY_RECORDING = 10511;
    public final static int MSG_STOP_SVA = 10513;
    public final static int MSG_RESTART_RECOG = 10514;
    public final static int MSG_DELETE_SM = 10515;
    public final static int MSG_MERGE_SM = 10516;
    public final static int MSG_GETUSER_SM = 10517;

    //choices for alexa and teddy.
    public final static int SELECTED_PROVIDER_TEDDY = 1;
    public final static int SELECTED_PROVIDER_ALEXA = 2;
    public final static int SELECTED_PROVIDER_GOOGLE = 3;
    public final static int SELECTED_PROVIDER_BAIDU = 4;
    private int mSelectedProvider = 0;

    public static int COMMAND_GET_RESULT = 10;
    public final static int COMMAND_GET_USER_NAME = 11;
    public final static int COMMAND_GET_CHOICE_FOR_SERVICE_PROVIDER = 12;

    public boolean isSVATraining = false;
    private String CHOOSE_MODEL_TEXT;
    private final String MODEL_TEDDY_TEXT = "hello teddy";
    private final String MODEL_ALEXA_TEXT = "hey alexa";
    private final String MODEL_GOOGLE_TEXT = "okay google";
    public static final String UDM_SUFFIX = ".udmc";
    private final String UIM_SUFFIX = ".uim";
    private final String UDM_MERGE_PREFIX = "merge";
    private final int MAX_REGISTER_SOUND_MODEL = 5;
    private final int REGISTER_MODEL_SAY_COUNT = 5;
    private boolean isUDKFull = false;

    private static boolean isRecording = false;
    private boolean detachAfterServiceStateEnabled = false;
    private int currentRecordingValue = -1;
    private int recognizedUserConfidence = -1;
    private int[] smHandle = new int[20];
    private String recognizedKeyWord = "Wrong";
    private String recognizedUser = "Wrong";

    // udmc prefix
    private String udmcTeddyName = "teddyModel_";
    private String udmcAlexaName = "alexaModel_";
    private String udmcGoogleName = "googleModel_";

    private String udmcFileName1 = "firstHT";
    private String udmcFileName2 = "secondHT";
    private String udmcFileName3 = "thirdHT";
    private String udmcFileName4 = "fourthHT";
    private String udmcFileName5 = "fifthHT";
    private String mergedFileName = UDM_MERGE_PREFIX + "OneHT" + UDM_SUFFIX;
    private String mUimPublicName = null;

    // sound module file name
    private String[] collectionAlexaUDMNames = new String[]{
            udmcAlexaName + udmcFileName1,
            udmcAlexaName + udmcFileName2,
            udmcAlexaName + udmcFileName3,
            udmcAlexaName + udmcFileName4,
            udmcAlexaName + udmcFileName5,
    };
    private String[] collectionTeddyUDMNames = new String[]{
            udmcTeddyName + udmcFileName1,
            udmcTeddyName + udmcFileName2,
            udmcTeddyName + udmcFileName3,
            udmcTeddyName + udmcFileName4,
            udmcTeddyName + udmcFileName5,
    };
    private String[] collectionGoogleUDMNames = new String[]{
            udmcGoogleName + udmcFileName1,
            udmcGoogleName + udmcFileName2,
            udmcGoogleName + udmcFileName3,
            udmcGoogleName + udmcFileName4,
            udmcGoogleName + udmcFileName5,
    };

    private String resultFromSceneAct = "null";
    private String mCurrentTrainingUserName = "defaultUser";
    private String mCurrentTrainingKeyphrase;

    private SoundTriggerModule stModule = null;
    private KeyphraseRecognitionExtra[] mKeyRecogExtras = null;
    private RecognitionConfig localRecognitionConfig1 = null;
    private RecognitionConfig recogConfig = null;
    private KeyguardManager.KeyguardLock keyguardLock = null;
    private SmSessionManager smSessionManager;
    private PowerManager pm;
    private Timer trainingTimer;
    private Timer wakelockTimer = null;
    private ToSpeak mToSpeak;
    private ListenAudioRecorder recorder;
    private ArrayList<String> listUDM = new ArrayList<>();

    private UUID uuid;
    private UUID vendorUuid;

    private Context mContext;
    private Handler mMainHandler = null;
    public Handler svaReceicerHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            Log.v(TAG, "servcie receive");
            switch (message.what) {
                case MSG_LOAD_START_RECOG:
                    AttachModuleTask attachModuleTask = new AttachModuleTask(
                            MSG_LOAD_START_RECOG, message.replyTo);
                    attachModuleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case MSG_VERIFY_RECORDING:
                    String getName = null;
                    JSONObject toJason;
                    try {
                        toJason = new JSONObject((String) message.obj);
                        getName = toJason.getString(LuisHelper.TAG_QUERY);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (null == getName) {
                        //this should never happen. The nullPointer Checker should be before luis.
                        Log.v(TAG, "Handler -> MSG_VERIFY_RECORDING -> message null.");
                        return false;
                    }
                    resultFromSceneAct = getName.replace(".", "");
                    Log.v(TAG, "getuserName = " + message.obj + "\n resultFromSceneAct =" + resultFromSceneAct);

                    // Alexa model or Teddy model.
                    switch (COMMAND_GET_RESULT) {
                        case COMMAND_GET_CHOICE_FOR_SERVICE_PROVIDER:
                            mSelectedProvider = 0;
                            if (resultFromSceneAct.equalsIgnoreCase("Alexa")) {
                                mSelectedProvider = SELECTED_PROVIDER_ALEXA;
                            } else if (resultFromSceneAct.equalsIgnoreCase("Teddy")) {
                                mSelectedProvider = SELECTED_PROVIDER_TEDDY;
                            } else if (resultFromSceneAct.equalsIgnoreCase("google")) {
                                mSelectedProvider = SELECTED_PROVIDER_GOOGLE;
                            }

                            if (mSelectedProvider == 0) {
                                Log.i(TAG, "unclear response from user.");
                                COMMAND_GET_RESULT = COMMAND_GET_CHOICE_FOR_SERVICE_PROVIDER;
                                mToSpeak.toSpeak(SceneCommonHelper.getString(mContext, R.string.luis_assistant_cmd_empty_repeat) + CHOOSE_MODEL_TEXT, false);
                                ((SceneActivity) mContext).startToListenCmd(false, CHOOSE_MODEL_TEXT);
                            } else {
                                VerifyRecordingTask verify = new VerifyRecordingTask();
                                verify.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                            break;
                        case COMMAND_GET_USER_NAME:
                            mCurrentTrainingUserName = resultFromSceneAct;
                            //then ask users the choice between Alexa and Teddy.
                            mToSpeak.toSpeak(CHOOSE_MODEL_TEXT, false);
                            COMMAND_GET_RESULT = COMMAND_GET_CHOICE_FOR_SERVICE_PROVIDER;
                            ((SceneActivity) mContext).startToListenCmd(false, CHOOSE_MODEL_TEXT);
                            break;
                    }
                    break;
                case MSG_STOP_SVA:
                    StopSVARecogTask stopSVARecogTask = new StopSVARecogTask();
                    stopSVARecogTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case MSG_RESTART_RECOG:
                    Log.v(TAG, "RestartRecognitionTask soundModelHandle = " + Arrays.toString(smHandle));
                    new RestartRecognitionTask().execute();
                    break;
                case MSG_DELETE_SM:
                    Log.v(TAG, "Delete this Sound Model");
                    DeleteSoundModelTask deleteSoundModelTask = new DeleteSoundModelTask();
                    deleteSoundModelTask.execute();
                    break;
                case MSG_MERGE_SM:
                    new MergeSMTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message.arg1);
                    break;
                case MSG_GETUSER_SM:
                    new CheckUserSMTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                default:
                    Log.v(TAG, "Handler in default message.");
                    break;
            }
            return false;
        }
    });

    public class SvaServiceBinder extends Binder {
        public VwuService getService() {
            return VwuService.this;
        }
    }

    private SvaServiceBinder svaServiceBinder = new SvaServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return svaServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mToSpeak = ToSpeak.getInstance(getApplicationContext());
        smSessionManager = new SmSessionManager();

        CHOOSE_MODEL_TEXT = getString(R.string.sva_training_tips);

        Utils.createDirIfNotExists(SVAGlobal.PATH_APP);
        Utils.createDirIfNotExists(SVAGlobal.PATH_TRAINING_RECORDINGS);
        Utils.createDirIfNotExists(SVAGlobal.PATH_VOICE_REQUESTS);
        Utils.copyAssetsToStorage(this, SVAGlobal.PATH_APP, "default.lm");
        Log.v(TAG, "user sound model output location= " + SVAGlobal.PATH_APP);

        File[] udkFilesToLoad = listUdmFileNoMerge();
        for (int j = 0; j < udkFilesToLoad.length; j++) {
            String filename = udkFilesToLoad[j].getName();
            Log.v(TAG, "filename = " + filename);
            listUDM.add(filename);
        }
        Log.v(TAG, "arrayList listUDM size = " + listUDM.size());
        Log.v(TAG, "VWUService start");
    }

    @Override
    public void onDestroy() {
//        unregisterReceiver(fromSVAScene);
        super.onDestroy();
        Log.v(TAG, "VWUService onDestroy");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "services unbind");
        return super.onUnbind(intent);
    }

    /**
     * Avoid to calculate all udm files number
     * Number of UDMs should not be bigger than 5.
     **/
    private int calculateNumberFromFiles() {
        int sumNumber = 0;
        ByteBuffer smBbToLoad = SVAGlobal.getInstance().getSmRepo().
                getSoundModelByteBufferFromName(mergedFileName);
        if (smBbToLoad != null) {
            ListenTypes.SoundModelInfo smInfo = ListenSoundModel.query(smBbToLoad);
            ListenTypes.SVASoundModelInfo v2SoundModelInfo = (ListenTypes.SVASoundModelInfo) smInfo;
            ListenTypes.KeywordInfo[] kwInfos = v2SoundModelInfo.keywordInfo;
            Log.i(TAG, "calculateNumberFromFiles keywordInfo length = " + kwInfos.length);
            sumNumber = kwInfos.length;
        }
        return sumNumber;
    }

    /*
    * Get the next model name
    * */
    private String getTrainingKeyphrase() {
        String[] collectionUDMNames = null;
        switch (mSelectedProvider) {
            case SELECTED_PROVIDER_ALEXA:
                collectionUDMNames = collectionAlexaUDMNames;
                break;
            case SELECTED_PROVIDER_TEDDY:
                collectionUDMNames = collectionTeddyUDMNames;
                break;
            case SELECTED_PROVIDER_GOOGLE:
                collectionUDMNames = collectionGoogleUDMNames;
                break;
            default:
                break;
        }

        String nextModelName = "";
        if (collectionUDMNames != null) {
            for (String nextName : collectionUDMNames) {
                if (!listUDM.contains(nextName + UDM_SUFFIX)) {
                    Log.i(TAG, "next model name = " + nextName);
                    nextModelName = nextName;
                    break;
                }
            }
        }
        return nextModelName;
    }

    public void detectSoundModule() {
        Log.v(TAG, "start to detect sound model!");
        File defaultSoundModule = new File(SVAGlobal.PATH_APP);
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(UDM_SUFFIX) ||
                        file.getName().endsWith(UIM_SUFFIX);
            }
        };
        File[] necessaryFiles = defaultSoundModule.listFiles(fileFilter);
        if (necessaryFiles != null && necessaryFiles.length > 0) {
            Log.v(TAG, "load exist sound model!");
            svaReceicerHandler.sendMessage(Message.obtain(svaReceicerHandler, MSG_LOAD_START_RECOG));
        } else {
            Log.v(TAG, "Not found sound model, need to start training!");
            svaReceicerHandler.sendMessage(Message.obtain(svaReceicerHandler, MSG_GETUSER_SM));
        }
    }

    private void wakeup() {
        try {
            Method wakeUp = pm.getClass().getDeclaredMethod("wakeUp", long.class);
            wakeUp.setAccessible(true);
            wakeUp.invoke(pm, SystemClock.uptimeMillis());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setMainHandler(Handler mainHandler, Context context) {
        this.mMainHandler = mainHandler;
        this.mContext = context;
    }

    private void resetTrainings() {
        SVAGlobal.getInstance().removeExistingRecordingFiles();
        SVAGlobal.getInstance().removeUserRecordings();
    }

    public class CheckUserSMTask extends AsyncTask<Void, String, String> {

        @Override
        protected String doInBackground(Void... params) {
            isUDKFull = true;
            if (calculateNumberFromFiles() < MAX_REGISTER_SOUND_MODEL) {
                isUDKFull = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (isUDKFull) {
                mToSpeak.toSpeak("models are out of size , please delete one first.", true);
                Log.v(TAG, "models are out of size , please delete one first.");
            } else {
                isSVATraining = true;
                mToSpeak.toSpeak("We need to create a sound model. Please tell me your name.", false);
                COMMAND_GET_RESULT = COMMAND_GET_USER_NAME;
                ((SceneActivity) mContext).startToListenCmd(false, "Please tell me your name.");
            }
        }
    }

    public class VerifyRecordingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Log.v(TAG, "VerifyRecordingTask doInBackground");
            serviceStopRecog();

            String modelText = null;
            switch (mSelectedProvider) {
                case SELECTED_PROVIDER_ALEXA:
                    modelText = MODEL_ALEXA_TEXT;
                    break;
                case SELECTED_PROVIDER_TEDDY:
                    modelText = MODEL_TEDDY_TEXT;
                    break;
                case SELECTED_PROVIDER_GOOGLE:
                    modelText = MODEL_GOOGLE_TEXT;
                    break;
                default:
                    break;
            }
            if (mSelectedProvider != 0) {
                mCurrentTrainingKeyphrase = getTrainingKeyphrase();
                if (!TextUtils.isEmpty(modelText)) {
                    mToSpeak.toSpeak(String.format(getString(R.string.sva_training_text), mCurrentTrainingUserName, modelText, REGISTER_MODEL_SAY_COUNT), false);
                }
            }
            resetTrainings();
            SVAGlobal.getInstance().getSmRepo().setTempTrainingComKeyphrase(mCurrentTrainingKeyphrase, "None", null, true);

            for (int i = 1; i < REGISTER_MODEL_SAY_COUNT + 1; i++) {
                isRecording = false;
                currentRecordingValue = -1;
                mToSpeak.toSpeak("" + i, false);

                startRecording();
                while (!isRecording) {
                    try {
                        Thread.sleep(300);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (currentRecordingValue < 16) {
                    i--;
                    SVAGlobal.getInstance().discardLastUserRecording();
                    mToSpeak.toSpeak("failed, try again", false);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.v(TAG, "VerifyRecordingTask onPostExecute");
            if (isUDKFull) {
                isSVATraining = false;
                Log.v(TAG, "VerifyRecordingTask onPostExecute == isUDK FULL! start Recog directly.");
                Message loadSoundModuleMsg = Message.obtain(svaReceicerHandler, MSG_LOAD_START_RECOG);
                loadSoundModuleMsg.sendToTarget();
                return;
            }

            CreateOrExtendSmTask createOrExtendSmTask = new CreateOrExtendSmTask();
            createOrExtendSmTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public class CreateOrExtendSmTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            mToSpeak.toSpeak("OK, please wait a moment for creating a sound model.", true);

            boolean isTrainingSuccess = false;
            String keywordPhrase = MODEL_TEDDY_TEXT;
            if (mSelectedProvider == SELECTED_PROVIDER_ALEXA) {
                keywordPhrase = MODEL_ALEXA_TEXT;
            } else if (mSelectedProvider == SELECTED_PROVIDER_GOOGLE) {
                keywordPhrase = MODEL_GOOGLE_TEXT;
            }

            int size = ListenSoundModel.getUdkSmSize(keywordPhrase, mCurrentTrainingUserName, SVAGlobal.getInstance().getUserRecordings(), SVAGlobal.getInstance().getLanguageModel());
            if (size > 0) {
                SVAGlobal.getInstance().getSmRepo().createExtendedSoundModelByteBuffer(size);
                Log.v(VwuService.TAG, "CreateOrExtendSmTask.doInBackground: calling extend with key phrase= " + mCurrentTrainingKeyphrase + ", user= " + mCurrentTrainingUserName);
                int returnStatus = ListenSoundModel.createUdkSm(keywordPhrase, mCurrentTrainingUserName, 5, SVAGlobal.getInstance().getUserRecordings(), SVAGlobal.getInstance().getLanguageModel(), SVAGlobal.getInstance().getSmRepo().getExtendedSoundModel(), SVAGlobal.getInstance().getConfidenceData());
                if (returnStatus == 0) {
                    Log.v(VwuService.TAG, "CreateOrExtendSmTask.onPostExecute: task succeeded");
                    SVAGlobal.getInstance().getSmRepo().saveExtendedSoundModel(VwuService.this.getApplicationContext());
                    SVAGlobal.getInstance().removeUserRecordings();
                    isTrainingSuccess = true;
                } else {
                    Log.e(VwuService.TAG, "CreateOrExtendSmTask.onPostExecute: task failed");
                }
            } else {
                Log.v(VwuService.TAG, "CreateOrExtendSmTask.doInBackground: getSizeWhenExtended returned error= " + size);
            }

            isSVATraining = false;
            Log.v(VwuService.TAG, "training result = " + isTrainingSuccess);
            if (isTrainingSuccess) {
                listUDM.add(mCurrentTrainingKeyphrase + UDM_SUFFIX);
                mToSpeak.toSpeak("Created! Voice Activation is On.", true);

                Message msg = Message.obtain(svaReceicerHandler, MSG_MERGE_SM);
                svaReceicerHandler.sendMessage(msg);
            } else {
                mToSpeak.toSpeak("Sorry. the recordings are not good enough . Please try again later.", false);
                detectSoundModule();
            }

            return null;
        }
    }

    public class StopSVARecogTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            serviceStopRecog();
            Log.v(TAG, "StopSVARecogTask END!");
            return null;
        }
    }

    private boolean loadSMing = false;

    public class AttachModuleTask extends AsyncTask<Void, Void, Void> {
        private int flag;
        private Messenger messenger = null;

        AttachModuleTask() {
        }

        AttachModuleTask(int flag, Messenger messenger) {
            this.flag = flag;
            this.messenger = messenger;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (messenger != null) {
                try {
                    Message message = new Message();
                    message.what = flag;
                    message.obj = "AttachModuleTask pre execute";
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (loadSMing) {
                return null;
            }
            ArrayList<ModuleProperties> localArrayList = new ArrayList<>();
            int su = SoundTrigger.listModules(localArrayList);
            Log.v(TAG, "list modules status: " + su + " \t " + ", modules length = " + localArrayList.size());
            if (localArrayList.size() < 1) {
//                buffer.append("AttachModuleTask.doInBackground: no available modules.\n");
            } else {
                for (int i = 0; i < localArrayList.size(); i++) {
//                    int id = (localArrayList.get(i)).id;
                    uuid = (localArrayList.get(i)).uuid;
                    Long uuid_most = (localArrayList.get(i)).uuid.getMostSignificantBits();
                    Long uuid_least = (localArrayList.get(i)).uuid.getLeastSignificantBits();
                    vendorUuid = new UUID(uuid_most, uuid_least);
//                    Log.v(TAG, String.valueOf(id));
//                    Log.v(TAG, String.valueOf(uuid));
//                    Log.v(TAG, String.valueOf(uuid_most));
//                    Log.v(TAG, String.valueOf(uuid_least));
                    stModule = SoundTrigger.attachModule(localArrayList.get(i).id,
                            VwuService.this, null);
                    if (stModule != null) {
                        Log.v(TAG, "AttachModuleTask.doInBackground: success");
                        break;
                    }
                }
                setRecogConfig();

                smHandle = new int[20];
                int handleIndex = 0;

                File udmcFileMerged = new File(SVAGlobal.getInstance().getSmRepo().generateSoundModelFilePath(mergedFileName));
                if (udmcFileMerged.exists() && udmcFileMerged.length() > 10) {
                    loadsm(mergedFileName, handleIndex++);
                    //attachAndLoad(udmcFileMerged);
                } else if (listUDM.size() > 0) {
                    //new
                    Log.i(TAG, "Load SM listUDM.get(0) = " + listUDM.get(0));
                    loadsm(listUDM.get(0), handleIndex++);

                    //old
                    //loadsm(udmcFileName1);
                    //attachAndLoad(udmc1);
                }

                File defaultSM = new File(SVAGlobal.PATH_APP);
                FileFilter fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().endsWith(UIM_SUFFIX);
                    }
                };
                File[] uimFiles = defaultSM.listFiles(fileFilter);
                if (uimFiles != null && uimFiles.length > 0) {
                    for(File uimPublicFile : uimFiles) {
                        if (uimPublicFile.exists()) {
                            mUimPublicName = uimPublicFile.getName();
                            Log.i("King", "uimPublicFile = " + uimPublicFile.getAbsolutePath() + ", uimFileName = " + mUimPublicName);
                            loadsm(mUimPublicName, handleIndex++);
                        }
                    }
                }
            }
            loadSMing = false;
            return null;
        }
    }

    public class RestartRecognitionTask extends AsyncTask<Integer, Void, Integer> {
        protected Integer doInBackground(Integer[] paramArrayOfInteger) {
            Log.v(TAG, "StartRecognitionTask.doInBackground");
            // King delete
            /*final Integer n = paramArrayOfInteger[0];
            SVAGlobal.SmState state = SVAGlobal.SmState.STARTED;
            if (state == null) {
                Log.e(TAG,
                        "StartRecognitionTask.doInBackground: cannot continue because SM with soundModelHandle= "
                                + n + " was " + "never loaded");
                return -1;
            }
            if (SVAGlobal.SmState.STARTED != state) {
                Log.e(TAG,
                        "StartRecognitionTask.doInBackground: cannot continue because SM with soundModelHandle= "
                                + n + " has incorrect" + " state= " + state);
                return -1;
            }*/
//            final SoundTrigger.RecognitionConfig recogConfig = new SoundTrigger.RecognitionConfig(
//                    SceneCommonHelper.getBufferingMode() == SceneCommonHelper.BUFFERING_MODE_ENABLE_SVA_BUFFERING, false, mKeyRecogExtras, null);
            for (int i = 0; i < smHandle.length; i++) {
                int n = smHandle[i];
                if (n == 0) {
                    continue;
                }
                if (recogConfig == null) {
                    Log.e(TAG,
                            "StartRecognitionTask.doInBackground: cannot continue because SM with soundModelHandle= "
                                    + n + " could not be " + "found or has no recogConfig");
                    return -1;
                }
                Log.v(TAG, "recogConfig= " + recogConfig.toString() + "\n smHandle[0]= " + n);
                final int startRecognition = stModule.startRecognition(n, recogConfig);
                if (startRecognition == 0) {
                    Log.v(TAG, "StartRecognitionTask.doInBackground: startRecognition succeeded  for soundModelHandle= " + n);
                    return 0;
                }
                Log.e(TAG, "StartRecognitionTask.doInBackground: startRecognition failed for soundModelHandle= "
                        + n + " with retrunStatus= " + startRecognition);
            }
            return -1;
        }
    }

    public class DeleteSoundModelTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (recognizedKeyWord.equals("Wrong")) {
                Log.v(TAG, "recognizedKeyWord  is  Wrong! ");
                mToSpeak.toSpeak("Delete failed. Please try again later.", false);
                return null;
            }
            if (1 < listUdmFileNoMerge().length) {
                serviceStopRecog();
//                 way 1
//                deleteUDKFromMerged(recognizedKeyWord, recognizedUser, mergedFile);

//                way 2
                File toDelSM = new File(SVAGlobal.PATH_APP + File.separator + recognizedKeyWord + UDM_SUFFIX);
                if (toDelSM.exists()) {
                    boolean delStatus = toDelSM.delete();
                    Log.v(TAG, "delete model keyword : " + recognizedKeyWord +
                            "\\n user : " + recognizedUser +
                            "\\n return status : " + delStatus);

                    mToSpeak.toSpeak("Delete Success.", false);
                } else
                    Log.v(TAG, "File does not exist. --- " + toDelSM.getAbsolutePath());
                Log.v(TAG, "listUDM remove = " + recognizedKeyWord);
                listUDM.remove(recognizedKeyWord + UDM_SUFFIX);
                svaReceicerHandler.sendMessage(svaReceicerHandler.obtainMessage(MSG_MERGE_SM));
            } else {
                mToSpeak.toSpeak("This is the last model which can not be deleted.", false);
                Log.v(TAG, "last udmc file.");
            }

//            svaReceicerHandler.sendMessage(svaReceicerHandler.obtainMessage(MSG_LOAD_START_RECOG));
            return null;
        }
    }

    public class MergeSMTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            Log.v(TAG, "MergeSMTask doInBackground");
            File mergeAlludkFile = new File(SVAGlobal.getInstance().getSmRepo().generateSoundModelFilePath(mergedFileName));
            boolean delStatus = mergeAlludkFile.delete();
            Log.v(TAG, "delete merged udks = " + delStatus);

            File[] udkFiles = listUdmFileNoMerge();
            Log.i(TAG, "mergedFileName = " + mergedFileName);
            ArrayList<String> needToMergeFiles = new ArrayList<>();
            for (int i = 0; i < udkFiles.length && i < MAX_REGISTER_SOUND_MODEL; i++) {
                File udkFile = udkFiles[i];
                needToMergeFiles.add(udkFile.getName());
                if (i == 0) {
                    continue;
                } else if (needToMergeFiles.size() == 1) {
                    needToMergeFiles.add(mergedFileName);
                }
                Log.i(TAG, "needToMergeFiles = " + Arrays.toString(needToMergeFiles.toArray()));
                mergeUDK(needToMergeFiles, mergedFileName);
                needToMergeFiles.clear();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Message msg = Message.obtain(svaReceicerHandler, MSG_LOAD_START_RECOG);
            svaReceicerHandler.sendMessage(msg);
            return null;
        }
    }

    @Override
    public void onSoundModelUpdate(SoundModelEvent paramSoundModelEvent) {
    }

    @Override
    public void onRecognition(RecognitionEvent paramRecognitionEvent) {
        Log.v(TAG, "onRecognition: entered");
        Log.v(TAG, "paramRecognitionEvent === " + paramRecognitionEvent.toString());
        processDetectionEvent(paramRecognitionEvent);
        Log.v(TAG, "recognizedUserConfidence = " + recognizedUserConfidence);
        Log.v(TAG, "recognizedKeyWord = " + recognizedKeyWord);
        Log.v(TAG, "recognizedUserSMHandler = " + paramRecognitionEvent.soundModelHandle);
        Log.v(TAG, "recognizedUser = " + recognizedUser);
        Log.v(TAG, "processDetectionEvent  ended ! ");

        //this is not need to restart again.
        /*if (isServiceStateEnabled && !paramRecognitionEvent.captureAvailable) {
            Log.v(TAG,"onRecognition: serviceStateEnabled so restarting recognition");
            Integer[] arrayOfInteger = new Integer[1];
            arrayOfInteger[0] = paramRecognitionEvent.soundModelHandle;
            Log.v(TAG,"paramRecognitionEvent.soundModelHandle = "
                    + paramRecognitionEvent.soundModelHandle);
            new RestartRecognitionTask().execute(arrayOfInteger);
        }else {
            Log.v(TAG,"isServiceStateEnabled && !paramRecognitionEvent.captureAvailable is not true");
        }*/
    }

    @Override
    public void onServiceDied() {
        Log.v(TAG, "onServiceDied");
        Log.v(TAG, "stModule detached");
        stModule.detach();
        detachAfterServiceStateEnabled = false;
    }

    @Override
    public void onServiceStateChange(int intValue) {
        Log.v(TAG, "onServiceStateChange: state= " + intValue);
        if (intValue == 0) {
            Log.v(TAG, "onServiceStateChange: SoundTrigger.SERVICE_STATE_ENABLED");
            if (this.detachAfterServiceStateEnabled) {
                Log.v(TAG, "onServiceStateChange: detachAfterServiceStateEnabled is true. Detaching.");
                this.stModule.detach();
                this.detachAfterServiceStateEnabled = false;
                Log.v(TAG, "onServiceStateChange: detachAfterServiceStateEnabled set back to false.");
//                this.smSessionManager.updateStateServiceDied();
            } else {
                Log.v(TAG, "onServiceStateChange: detachAfterServiceStateEnabled is false.");
                Log.v(TAG, "onRecognition: serviceStateEnabled so restarting recognition for smHandle= " + intValue);
                new RestartRecognitionTask().execute();
//                }
//                if (startedSessionsSmHandles.size() > 0) {
//                    this.showNotification(2130837551, "Voice Activation is running");
//                }
            }
        } else {
            if (intValue != 1) {
                Log.e(TAG, "onServiceStateChange: unrecognized state= " + intValue);
                return;
            }
            Log.v(TAG, "onServiceStateChange: SoundTrigger.SERVICE_STATE_DISABLED");
            //            if (this.smSessionManager.isASessionStarted()) {
//                this.notificationManager.cancelAll();
//                this.sendMessageDataAll(18, 0, null);
//            }
        }
    }

    private int verifyUser() {
        int verifyUdkResult = -1;
        ByteBuffer defaultBuffer = SVAGlobal.getInstance().getLanguageModel();
        ShortBuffer lastRecordingBuffer = SVAGlobal.getInstance().getLastUserRecording();
        verifyUdkResult = ListenSoundModel.verifyUdkRecording(defaultBuffer, lastRecordingBuffer);
        Log.i(TAG, "verifyUser verifyUdkResult = " + verifyUdkResult);
        return verifyUdkResult;
    }

    private void startRecording() {
        recorder = ListenAudioRecorder.getInstance();
        SceneCommonHelper.playSpeakingSound(getApplicationContext(), SceneCommonHelper.WARN_SOUND_TYPE_START, false, true);
        recorder.start();
        LedForRecording.recordingStart(getApplicationContext());
        Log.v(TAG, "start recording...");
        startRecordingTimer();
    }

    private void startRecordingTimer() {
        Log.v(TAG, "startTrainingTimer \n ====================== start recording...\n\n\n\n\n\n");
        if (trainingTimer == null) {
            trainingTimer = new Timer();
            trainingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    recorder.stop(SVAGlobal.getInstance().getLastUserRecordingFilePath());
                    if (trainingTimer != null) {
                        trainingTimer.cancel();
                        trainingTimer = null;
                    }
                    Log.v(TAG, "over recording...");

                    LedForRecording.recordingStop(getApplicationContext());
                    Log.v(TAG, "Record finish and prepare verify");
                    currentRecordingValue = verifyUser();
                    isRecording = true;
                }
            }, 3000L);
        }
    }

    public class LookAheadBufferTask extends AsyncTask<String, Void, Integer> {
        private String bufferingFilePath;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Message buttonMessage = mMainHandler.obtainMessage();
            buttonMessage.what = SceneActivity.MSG_SVA_WAIT_BUFFER_DATA;
            mMainHandler.sendMessage(buttonMessage);
        }

        protected Integer doInBackground(String... strings) {
            int streamBufferSizeInBytes;
            int returnStatus;
            Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground");
            VwuService.this.smSessionManager.setIsASessionBuffering(true);
            bufferingFilePath = strings[0];
            int audioSessionNum = Integer.parseInt(strings[1]);
            Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: sessionNum = " + audioSessionNum);
            int totalBytesToRead = ((int) (SVAGlobal.getInstance().getSettingVoiceRequestLength() * SVAGlobal.SHORTS_PER_SECOND)) * 2;
            Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: total bytesToRead = " + totalBytesToRead);
            android.media.AudioAttributes.Builder audAttBldr = new android.media.AudioAttributes.Builder();
            audAttBldr.setUsage(AudioAttributes.USAGE_MEDIA);
            audAttBldr.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH);
            try {
                Method setInternalCapturePreset = audAttBldr.getClass().getMethod("setInternalCapturePreset", int.class);
                setInternalCapturePreset.setAccessible(true);
                setInternalCapturePreset.invoke(audAttBldr, 1999); // MediaRecorder.AudioSource.HOTWORD
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                Log.i("T", "e1 = " + e.getMessage());
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                Log.i("T", "e2 = " + e.getMessage());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                Log.i("T", "e3 = " + e.getMessage());
            }
            //audAttBldr.setInternalCapturePreset(1999);
            AudioAttributes audAtt = audAttBldr.build();
            int minBuffSize = AudioRecord.getMinBufferSize(ListenAudioRecorder.SAMPLE_RATE, 16, 2);
            Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: getMinBuffSize returns " + minBuffSize);
            AudioFormat.Builder audFormatBldr = new AudioFormat.Builder();
            audFormatBldr.setEncoding(AudioFormat.ENCODING_PCM_16BIT);
            audFormatBldr.setSampleRate(ListenAudioRecorder.SAMPLE_RATE);
            audFormatBldr.setChannelMask(AudioFormat.CHANNEL_IN_MONO);
            AudioFormat audFormat = audFormatBldr.build();
            if (minBuffSize > ListenAudioRecorder.SAMPLE_RATE) {
                streamBufferSizeInBytes = minBuffSize;
            } else {
                streamBufferSizeInBytes = ListenAudioRecorder.SAMPLE_RATE;
            }
            Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: create AudioRec stream with sessionId = " + audioSessionNum);
            Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground:      buffSize = " + streamBufferSizeInBytes);
            // King debug
            // Buffered feature or Standard AudioRecord.
            AudioRecord audioRecord = null;
            try {
                //AudioRecord audioRecord = new AudioRecord(audAtt, audFormat, streamBufferSizeInBytes, audioSessionNum);
                Constructor AudioRecordConstructor = AudioRecord.class.getConstructor(AudioAttributes.class, AudioFormat.class, int.class, int.class);
                audioRecord = (AudioRecord) AudioRecordConstructor.newInstance(audAtt, audFormat, streamBufferSizeInBytes, audioSessionNum);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                Log.i("T", "e4 = " + e.getMessage());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                Log.i("T", "e5 = " + e.getMessage());
            } catch (InstantiationException e) {
                e.printStackTrace();
                Log.i("T", "e6 = " + e.getMessage());
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                Log.i("T", "e7 = " + e.getMessage());
            }

            /*int bufferSize = AudioRecord.getMinBufferSize
                    (16000, 2, 2) * 3;
            //audioData = new short[bufferSize]; //short array that pcm data is put into.
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000,
                    2,
                    2,
                    bufferSize);*/
            // King

            byte[] buffer = new byte[totalBytesToRead];
            audioRecord.startRecording();
            int curBytesRead = 0;

            while (curBytesRead < totalBytesToRead) {
                Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: call audioRecord.read curBytesRead " + curBytesRead + ", incBuffSize " + ListenAudioRecorder.SAMPLE_RATE);
                if (isCancelled()) {
                    Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: cancelled from detaching soundmodule");
                    break;
                }
                int bufferReadResult = audioRecord.read(buffer, curBytesRead, ListenAudioRecorder.SAMPLE_RATE);
                if (bufferReadResult < 0) {
                    Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: audioRecord.read returned error " + bufferReadResult + ", serviceState ");
                    break;
                }
                Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: audioRecord.read successfully read " + bufferReadResult + " bytes");
                curBytesRead += bufferReadResult;
                Log.i("T", "curBytesRead = " + curBytesRead);
            }
            Log.i("T", "end >>> totalBytesToRead = " + totalBytesToRead);
            audioRecord.stop();
            audioRecord.release();
            if (curBytesRead > 0) {
                Log.v(VwuService.TAG, "LookAheadBufferTask.doInBackground: write wav file to " + bufferingFilePath);
                SVAGlobal.getInstance().saveVoiceRequest(buffer, curBytesRead, bufferingFilePath);
                returnStatus = 0;
            } else {
                returnStatus = -1;
            }
            VwuService.this.smSessionManager.setIsASessionBuffering(false);
            return Integer.valueOf(returnStatus);
        }

        protected void onPostExecute(Integer result) {
            Log.v(VwuService.TAG, "LookAheadBufferTask.onPostExecute");
            if (result.intValue() == 0) {
                Log.v(VwuService.TAG, "LookAheadBufferTask.onPostExecute: task succeeded");
                //VwuService.this.sendMessageDataAll(25, 0, null);
                //return;
            } else {
                Log.v(VwuService.TAG, "LookAheadBufferTask.onPostExecute: task failed");
                //VwuService.this.sendMessageDataAll(25, -1, null);
            }

            Message buttonMessage = mMainHandler.obtainMessage();
            buttonMessage.what = SceneActivity.MSG_SVA_DECODE_BUFFERING_DATA;
            buttonMessage.obj = bufferingFilePath;
            buttonMessage.arg1 = result.intValue();
            mMainHandler.sendMessage(buttonMessage);
        }

        protected void onCancelled(Integer result) {
            /*if (result.intValue() == 0) {
                VwuService.this.sendMessageDataAll(25, 0, null);
            } else {
                VwuService.this.sendMessageDataAll(25, -1, null);
            }*/
        }
    }

    AsyncTask<String, Void, Integer> lookAheadBufferTask;

    private void processDetectionEvent(RecognitionEvent recognitionEvent) {
        Log.i(TAG, "\n\n\n----------------------   processDetectionEvent: keyword detected   -------------------------\n\n\n");
        Log.v(TAG, "processDetectionEvent: recognitionEvent.status= "
                + recognitionEvent.status);
        Log.v(TAG, "processDetectionEvent: recognitionEvent.captureAvailable= " + recognitionEvent.captureAvailable);
        Log.v(TAG, "processDetectionEvent: recognitionEvent.getSettingVoiceRequestsEnabled= " + SVAGlobal.getInstance().getSettingVoiceRequestsEnabled());
        Log.v(TAG, "processDetectionEvent: recognitionEvent.soundModelHandle= "
                + recognitionEvent.soundModelHandle);
        Log.v(TAG, "smSessionManager.getSmName = " + smSessionManager.getSmName(recognitionEvent.soundModelHandle));
        KeyphraseRecognitionEvent stKpRecogEvent = (KeyphraseRecognitionEvent) recognitionEvent;
        for (KeyphraseRecognitionExtra kRecogExtra : stKpRecogEvent.keyphraseExtras) {
            Log.v(TAG, "processDetectionEvent: kRecogExtra.id= "
                    + kRecogExtra.id);
            Log.v(TAG, "processDetectionEvent: kRecogExtra.confidenceLevels.length= "
                    + kRecogExtra.confidenceLevels.length);
            for (ConfidenceLevel confLevel : kRecogExtra.confidenceLevels) {
                Log.v(TAG, "processDetectionEvent: kRecogExtra.id= "
                        + kRecogExtra.id + ", userID= "
                        + confLevel.userId + ", confLevel= "
                        + confLevel.confidenceLevel);
            }
        }

        // Error checking: if recognitionEvent.status is unknown, return error.
        if ((recognitionEvent.status != SoundTrigger.RECOGNITION_STATUS_SUCCESS) &&
                (recognitionEvent.status != SoundTrigger.RECOGNITION_STATUS_FAILURE) &&
                (recognitionEvent.status != SoundTrigger.RECOGNITION_STATUS_ABORT)) {
            Log.e(TAG, "processDetectionEvent: recognitionEvent.status= " + recognitionEvent.status +
                    " is not a valid status.");
            return;
        }

        // If recognitionEvent.status is ABORT, discard the recognition because capture preempted
        // by another audio usecase.
        if (recognitionEvent.status == SoundTrigger.RECOGNITION_STATUS_ABORT) {
            Log.v(TAG, "processDetectionEvent: recognitionEvent.status= RECOGNITION_STATUS_ABORT " +
                    "discarding recognition.");
            return;
        }

        //if (recognitionEvent.status == 0 && recognitionEvent.captureAvailable && Global.getInstance().getSettingVoiceRequestsEnabled()) {
        if (recognitionEvent.status == 0 && SceneCommonHelper.getBufferingMode() == SceneCommonHelper.BUFFERING_MODE_ENABLE_SVA_BUFFERING) {
            Log.v(TAG, "processDetectionEvent: filePath= " + (SVAGlobal.PATH_VOICE_REQUESTS + "/" + String.valueOf(System.currentTimeMillis()) + SVAGlobal.PATH_RECORDINGS_FILEEXT));
            String labCaptureAudioSessionNum = String.valueOf(recognitionEvent.captureSession);
            String filePath = SVAGlobal.PATH_VOICE_REQUESTS + "/" + SVAGlobal.BUFFERING_RECORDINGS_FILE_NAME + SVAGlobal.PATH_RECORDINGS_FILEEXT;
            String[] filePathAndAudioSessionNum = new String[]{filePath, labCaptureAudioSessionNum};
            lookAheadBufferTask = new LookAheadBufferTask();
            lookAheadBufferTask.executeOnExecutor(SceneCommonHelper.mCachedThreadPool, filePathAndAudioSessionNum);
            Log.v(TAG, "processDetectionEvent: filePath= " + (SVAGlobal.PATH_VOICE_REQUESTS + "/" + SVAGlobal.BUFFERING_RECORDINGS_FILE_NAME + SVAGlobal.PATH_RECORDINGS_FILEEXT));
            Log.v(TAG, "processDetectionEvent: labCaptureAudioSessionNum= " + labCaptureAudioSessionNum);
        }

        // LAB
        Log.v(TAG, "processDetectionEvent: recognitionEvent.status = "
                + recognitionEvent.status);
        Log.v(TAG, "processDetectionEvent: voiceRequestEnabled = "
                + SVAGlobal.getInstance().getSettingVoiceRequestsEnabled());
        if ((recognitionEvent.status == SoundTrigger.RECOGNITION_STATUS_SUCCESS)
                || (recognitionEvent.status == SoundTrigger.RECOGNITION_STATUS_FAILURE)) {
            String str5 = (recognitionEvent.status == SoundTrigger.RECOGNITION_STATUS_SUCCESS) ? "success"
                    : "failure";
            String localStringBuilder2 = "processDetectionEvent: playing " +
                    str5;
            Log.v(TAG, localStringBuilder2 + " tone");
            // this.tonePlayer.play(recognitionEvent.status);
            turnOnDisplay();

            if (this.keyguardLock != null) {
                this.keyguardLock.reenableKeyguard();
                this.keyguardLock = null;
            }
            this.keyguardLock = ((KeyguardManager) getApplicationContext()
                    .getSystemService(Context.KEYGUARD_SERVICE))
                    .newKeyguardLock(TAG);
            this.keyguardLock.disableKeyguard();
        } else {
            Log.v(TAG, "processDetectionEvent: unknown recognition status= "
                    + recognitionEvent.status);
        }
        final String smName = this.smSessionManager.getSmName(recognitionEvent.soundModelHandle);
        Log.i(TAG, "processDetectionEvent:  detected sm name = "+smName);

        ListenTypes.VoiceWakeupDetectionDataV2 vwuDetectionData = new ListenTypes.VoiceWakeupDetectionDataV2();
        // Loop through KeyphraseRecognitionExtras to find out how many non-zero confLevels so
        // arrays can be initialized to the correct size.

        int keyphraseConfLevels = 0;
        int userConfLevels = 0;
        for (KeyphraseRecognitionExtra kRecogExtra : stKpRecogEvent.keyphraseExtras) {
            if (kRecogExtra.coarseConfidenceLevel > 0)
                keyphraseConfLevels++;
            for (ConfidenceLevel confLevel : kRecogExtra.confidenceLevels) {
                if (confLevel.confidenceLevel > 0) {
                    userConfLevels++;
                }
            }
        }
        vwuDetectionData.nonzeroKWConfLevels = new ListenTypes.VWUKeywordConfLevel[keyphraseConfLevels];
        vwuDetectionData.nonzeroUserKWPairConfLevels = new ListenTypes.VWUUserKeywordPairConfLevel[userConfLevels];

        KeyphraseRecognitionExtra kRecogExtra;
        ConfidenceLevel confLevel;
        String keyphraseName;
        String userName;
        int nonZeroKwIndex = 0;
        int nonZeroPairIndex = 0;
        int keyphraseConfLevel = -2;
        boolean isVoiceQnaEnabled = false;

        for (int i = 0; i < stKpRecogEvent.keyphraseExtras.length; i++) {
            kRecogExtra = stKpRecogEvent.keyphraseExtras[i];
//                keyphraseName = recognitionKey;
            keyphraseName = SVAGlobal.getInstance().getSmRepo().findKeyphraseOrUserById(smName, kRecogExtra.id);

            Log.v(TAG, "keyphraseName = " + keyphraseName);
            keyphraseConfLevel = kRecogExtra.coarseConfidenceLevel;
            Log.v(TAG, "processDetectionEvent: keyphrase with id= " + kRecogExtra.id + ", name= "
                    + keyphraseName + " detected with confidence level= " + keyphraseConfLevel);
            // Ensure valid detection (>0) before adding it.

            if (keyphraseConfLevel > 0) {
                vwuDetectionData.nonzeroKWConfLevels[nonZeroKwIndex] = new ListenTypes.VWUKeywordConfLevel();
                vwuDetectionData.nonzeroKWConfLevels[nonZeroKwIndex].keyword = keyphraseName;
                vwuDetectionData.nonzeroKWConfLevels[nonZeroKwIndex].confLevel = (short) keyphraseConfLevel;
                nonZeroKwIndex++;
                if (false == isVoiceQnaEnabled) {
                    isVoiceQnaEnabled = SVAGlobal.getInstance().getSmRepo().getLaunchPreference(
                            getApplicationContext(), smName, keyphraseName, null);
                    Log.v(TAG, "processDetectionEvent: isVoiceQnaEnabled set to= "
                            + "" + isVoiceQnaEnabled);
                }
            }
            Log.v(TAG, "processDetectionEvent: kRecogExtra.confidenceLevels.length= "
                    + kRecogExtra.confidenceLevels.length);
            for (int j = 0; j < kRecogExtra.confidenceLevels.length; j++) {
                confLevel = kRecogExtra.confidenceLevels[j];
                Log.v(TAG, "processDetectionEvent: kRecogExtra.id= " + kRecogExtra.id +
                        ", keyphrase= " + keyphraseName + ", userID= "
                        + confLevel.userId + ", confLevel= " + confLevel.confidenceLevel);
                // If user confLevel == 0, it's not a valid detection so don't add it. Continue to
                // next detection.
                if (confLevel.confidenceLevel == 0) {
                    continue;
                }
                userName = SVAGlobal.getInstance().getSmRepo().findKeyphraseOrUserById(smName,
                        confLevel.userId);
                Log.i(TAG, "recognize a userName = " + userName);
                //get parameter for delete.
                recognizedUser = userName;
                recognizedKeyWord = keyphraseName;
                recognizedUserConfidence = confLevel.confidenceLevel;
                //end
                vwuDetectionData.nonzeroUserKWPairConfLevels[nonZeroPairIndex] =
                        new ListenTypes.VWUUserKeywordPairConfLevel();
                vwuDetectionData.nonzeroUserKWPairConfLevels[nonZeroPairIndex].keyword = keyphraseName;
                vwuDetectionData.nonzeroUserKWPairConfLevels[nonZeroPairIndex].user = userName;
                vwuDetectionData.nonzeroUserKWPairConfLevels[nonZeroPairIndex].confLevel =
                        (short) confLevel.confidenceLevel;
                Log.v(TAG, "processDetectionEvent: added keyword= " +
                        vwuDetectionData.nonzeroUserKWPairConfLevels[nonZeroPairIndex].keyword +
                        ", user= " +
                        vwuDetectionData.nonzeroUserKWPairConfLevels[nonZeroPairIndex].user +
                        ", confLevel= " +
                        vwuDetectionData.nonzeroUserKWPairConfLevels[nonZeroPairIndex].confLevel);
                nonZeroPairIndex++;
                // Only change isVoiceQnaEnabled if it hasn't been set to true
                // in a previous iteration or during key phrase only checking.
                if (false == isVoiceQnaEnabled) {
                    isVoiceQnaEnabled = SVAGlobal.getInstance().getSmRepo().getLaunchPreference(
                            getApplicationContext(), smName, keyphraseName, userName);
                    Log.v(TAG, "isVoiceQnaEnabled set to= " + isVoiceQnaEnabled);
                }
            }
        }

        for (ListenTypes.VWUKeywordConfLevel nonzeroKWConfLevel : vwuDetectionData.nonzeroKWConfLevels) {
            Log.v(TAG, "nonzeroKWConfLevel.keyword= " + nonzeroKWConfLevel.keyword);
            Log.v(TAG, "\tnonzeroKWConfLevel.confLevel= " + nonzeroKWConfLevel.confLevel);
        }
        for (ListenTypes.VWUUserKeywordPairConfLevel nonzeroUserKWPairConfLevel : vwuDetectionData.nonzeroUserKWPairConfLevels) {
            Log.v(TAG, "nonzeroUserKWPairConfLevel.keyword= " +
                    nonzeroUserKWPairConfLevel.keyword);
            Log.v(TAG, "nonzeroUserKWPairConfLevel.user= " +
                    nonzeroUserKWPairConfLevel.user);
            Log.v(TAG, "nonzeroUserKWPairConfLevel.confLevel= " +
                    nonzeroUserKWPairConfLevel.confLevel);
        }

        int sessionNum = smSessionManager.getSessionNum(recognitionEvent.soundModelHandle);
        if (sessionNum == -1) {
            Log.e(TAG, "processDetectionEvent: cannot continue because could not find sessionNum " +
                    "for smHandle= " + recognitionEvent.soundModelHandle);
            return;
        }
//        String str1 = recognizedKeyWord.replace(" ", "") + UDM_SUFFIX;
//        SVAGlobal.getInstance().setDetectionContainer(0, null,
//                vwuDetectionData, i10, str1);
        SVAGlobal.getInstance().setDetectionContainer(ListenTypes.STATUS_SUCCESS, null,
                vwuDetectionData, sessionNum, smName);

        // Launch VRE.
        // If one of the detected keyphrase-user combinations set launch voice recognition engine
        // to true, the launch it.
//        if (isVoiceQnaEnabled && recognitionEvent.status != SoundTrigger.RECOGNITION_STATUS_SUCCESS) {
//            launchVoiceQna();
//        }
        try {
            if ((recognitionEvent.status == ListenTypes.STATUS_SUCCESS)
                    || (recognitionEvent.status == ListenTypes.USER_KEYWORD_DETECTION_MODE)) {
                StringBuilder localStringBuilder1 = new StringBuilder()
                        .append("processDetectionEvent: playing ");
                String str3 = (recognitionEvent.status == 0) ? "success"
                        : "failure";
                localStringBuilder1.append(str3);
                Log.v(TAG, localStringBuilder1.toString() + " tone");
                // this.tonePlayer.play(recognitionEvent.status);
                Log.v(TAG, "start intent");
                Intent teddy = new Intent();

                ComponentName componentName = new ComponentName(getApplicationInfo().packageName,
                        "com.wistron.demo.tool.teddybear.scene.SceneActivity");
                teddy.setComponent(componentName);
                teddy.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                Log.v(TAG, "getApplicationInfo().packageName=" +
//                        getApplicationInfo().packageName);

                if (!isForeGround()) {
                    getApplication().startActivity(teddy);
                    Log.v(TAG, "startActivity");
                } else Log.v(TAG, "is Foreground");

                if (SceneCommonHelper.getBufferingMode() != SceneCommonHelper.BUFFERING_MODE_ENABLE_SVA_BUFFERING) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                        /*
                        * This should send the recognizedKeyWord as well.
                        * And this can tell us the result is Alexa or Teddy.
                        * */
                            Message buttonMessage = mMainHandler.obtainMessage();
                            buttonMessage.what = SceneActivity.MSG_SPEAKING_BUTTON;
                            int model = SELECTED_PROVIDER_TEDDY;
                            if (smName.endsWith(UIM_SUFFIX)) {
                                buttonMessage.obj = "";
                                switch (SceneCommonHelper.getUimKeywordToTriggerAction()){
                                    case SceneCommonHelper.UIM_KEYWORD_TO_TRIGGER_HEY_ALEXA:
                                        Log.i(TAG, "Detected a UIM sound model, trigger to 'Hey alexa' action!");
                                        model = SELECTED_PROVIDER_ALEXA;
                                        break;
                                    case SceneCommonHelper.UIM_KEYWORD_TO_TRIGGER_OKAY_GOOGLE:
                                        Log.i(TAG, "Detected a UIM sound model, trigger to 'Okay google' action!");
                                        model = SELECTED_PROVIDER_GOOGLE;
                                        break;
                                    case SceneCommonHelper.UIM_KEYWORD_TO_TRIGGER_HELLO_TEDDY:
                                    default:
                                        Log.i(TAG, "Detected a UIM sound model, trigger to 'Hello teddy' action!");
                                        break;
                                }
                            } else {
                                buttonMessage.obj = recognizedUser;
                                if (recognizedKeyWord.contains("alexa")) {
                                    model = SELECTED_PROVIDER_ALEXA;
                                } else if (recognizedKeyWord.contains("google")) {
                                    model = SELECTED_PROVIDER_GOOGLE;
                                }
                            }

                            buttonMessage.arg1 = model;
                            mMainHandler.sendMessage(buttonMessage);
                        }
                    }, 100L);
                }

                Log.v(TAG, "DETECT_SUCCESS_EVENT: sending event");
                turnOnDisplay();
                if (this.keyguardLock != null) {
                    this.keyguardLock.reenableKeyguard();
                    this.keyguardLock = null;
                }
                this.keyguardLock = ((KeyguardManager) getApplicationContext()
                        .getSystemService(Context.KEYGUARD_SERVICE))
                        .newKeyguardLock(TAG);
                this.keyguardLock.disableKeyguard();
            }
            Log.v(TAG, "processDetectionEvent: unknown recognition status= "
                    + recognitionEvent.status);
        } catch (NullPointerException localNullPointerException1) {
            Log.v(TAG, "Player was null e= "
                    + localNullPointerException1.getMessage());
            Log.v(TAG, "processDetectionEvent: SVAGlobal getKeyphraseActionIntent "
                    + "returned null. Either no action was selected or the action was"
                    + " not found.");
            if (SVAGlobal.getInstance().getNumActivitiesShowing() < 0)
                Log.v(TAG, "processDetectionEvent: SVAGlobal getNumActivitiesShowing"
                        + " < 0. getNumActivitiesShowing= "
                        + SVAGlobal.getInstance().getNumActivitiesShowing());
            if (recognitionEvent.status == 2)
                Log.v(TAG, "status = 2 ");
        }
    }

    private File[] listUdmFileNoMerge() {
        File defaultsm = new File(SVAGlobal.PATH_APP);
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(UDM_SUFFIX) && !file.getName().startsWith(UDM_MERGE_PREFIX);
            }
        };
        return defaultsm.listFiles(fileFilter);
    }

    private void loadsm(String smNameToLoad, int handleIndex) {
        Log.i(TAG, "start load: " + smNameToLoad + " with index: " + handleIndex);
        File checkForSM = new File(SVAGlobal.getInstance().getSmRepo().generateSoundModelFilePath(smNameToLoad));
        if (!checkForSM.exists() || checkForSM.length() == 0) {
            Log.v(TAG, "Sound model files do not exist. please check!");
            return;
        }

        // Determine recognition mode from settings.
        int stRecognitionMode = -1;
        int detectionMode = SVAGlobal.getInstance().getDetectionMode();
        if (detectionMode == ListenTypes.KEYWORD_ONLY_DETECTION_MODE) {
            stRecognitionMode = SoundTrigger.RECOGNITION_MODE_VOICE_TRIGGER;
        } else if (detectionMode == ListenTypes.USER_KEYWORD_DETECTION_MODE) {
            stRecognitionMode = SoundTrigger.RECOGNITION_MODE_VOICE_TRIGGER +
                    SoundTrigger.RECOGNITION_MODE_USER_IDENTIFICATION;
        }
        Log.v(TAG, "MSG_LOAD_SM: stRecognitionMode= " + stRecognitionMode);

        // inform HAL of number of sessions that will be used
//        AudioSystem.setParameters("SVA_NUM_SESSIONS=1");
        try {
            Class<?> mAudioSystemClass = Class.forName("android.media.AudioSystem");
            Method setParametersMethod = mAudioSystemClass.getDeclaredMethod("setParameters", String.class);
            setParametersMethod.setAccessible(true);
            setParametersMethod.invoke(null, "SVA_NUM_SESSIONS=1");
        } catch (Exception e) {
            Log.i(TAG, "** Error ** android.media.AudioSystem.setParameters: " + e.getMessage());
            e.printStackTrace();
        }

        ByteBuffer smBbToLoad = SVAGlobal.getInstance().getSmRepo().
                getSoundModelByteBufferFromName(smNameToLoad);
        if (null != smBbToLoad) {
            //for debugging
            StringBuilder sb1 = new StringBuilder();
            for (int j = 0; j < 16; j++) {
                sb1.append(String.format("%02X ", smBbToLoad.get(j)));
            }
            Log.v(TAG, "bytes #1 (after getting smBbToLoad= " + sb1.toString());
        } else {
            Log.e(TAG, "MSG_LOAD_SM: loadSoundModel failed because no ByteBuffer" +
                    "is available for SM= " + smNameToLoad);
        }
        //get info for that SM
        ListenTypes.SoundModelInfo smInfo = ListenSoundModel.query(smBbToLoad);
        /*if (null == smInfo) {
            useStKeyphraseSmFromXml = true; // if smInfo isn't null, use it.
        }*/

        ArrayList<String> userKeyphrasePairIndices = new ArrayList<String>();
        SVAGlobal.getInstance().getSmRepo().setSoundModelNameToQuery(smNameToLoad);

        // Create ST SM to pass to loadSoundModel().
        ListenTypes.SVASoundModelInfo v2SoundModelInfo = (ListenTypes.SVASoundModelInfo) smInfo;
        assert v2SoundModelInfo != null;  // make sure v2SoundModelInfo is valid.
        ListenTypes.KeywordInfo[] kwInfos = v2SoundModelInfo.keywordInfo;
        SoundTrigger.Keyphrase[] keyphrases = new SoundTrigger.Keyphrase[kwInfos.length];
        KeyphraseRecognitionExtra[] kpRecExtras = new KeyphraseRecognitionExtra[kwInfos.length];

        // Create ArrayList of userKeyphrasePairs where the index in the ArrayList
        // is the index into the array that is used to create the userIDs list.

        // First add keyphrase indices to list.
        for (ListenTypes.KeywordInfo keyphraseInfo : v2SoundModelInfo.keywordInfo) {
            userKeyphrasePairIndices.add(keyphraseInfo.keywordPhrase);
        }
        // After keyphrase indices Add user|keyphrase pairs indices to list.
        for (String activeUser : v2SoundModelInfo.userNames) {
            for (ListenTypes.KeywordInfo keyphraseInfo : v2SoundModelInfo.keywordInfo) {
                for (String keyphraseUser : keyphraseInfo.activeUsers) {
                    /*Log.v(TAG, "MSG_LOAD_SM: for activeUser= " +
                            " activeUser= " + activeUser + " check " +
                            keyphraseInfo.keywordPhrase + "." + keyphraseUser);*/
                    if (activeUser.equals(keyphraseUser)) {
                        userKeyphrasePairIndices.add(activeUser + "|" +
                                keyphraseInfo.keywordPhrase);
                        /*Log.v(TAG, "MSG_LOAD_SM: added " + activeUser +
                                "|" + keyphraseInfo.keywordPhrase + " with array "
                                + "index= " + (userKeyphrasePairIndices.size() - 1));*/
                    }
                }
            }
        }
        SVAGlobal.getInstance().getSmRepo().addSmInfoFromQuery(smInfo,
                userKeyphrasePairIndices);

        // For all keyphrases
        for (int j = 0; j < kwInfos.length; j++) {
            // Create ST keyphrase objects.
            ListenTypes.KeywordInfo keyphraseInfo = kwInfos[j];
            // Add all userIDs from userKeyphrasePairs.
            int[] userIDs = new int[keyphraseInfo.activeUsers.length];
            int userIDsIndex = 0;
            int uKPairIndex = -1;
            for (String keyphraseUser : keyphraseInfo.activeUsers) {
                String userKeyphraseTokenized = keyphraseUser + "|" +
                        keyphraseInfo.keywordPhrase;
                uKPairIndex = userKeyphrasePairIndices.indexOf(userKeyphraseTokenized);
                /*Log.v(TAG, "MSG_LOAD_SM: found uKPairIndex= " +
                        uKPairIndex + " for userKeyphraseTokenized= " +
                        userKeyphraseTokenized);*/
                if (uKPairIndex != -1) {
                    userIDs[userIDsIndex++] = uKPairIndex;
                    /*Log.v(TAG, "MSG_LOAD_SM: adding uKPairIndex= " +
                            uKPairIndex + " for userKeyphraseTokenized= " +
                            userKeyphraseTokenized);*/
                } else {
                    Log.e(TAG, "MSG_LOAD_SM: The userKeyphrasePair= " +
                            userKeyphraseTokenized + " was not found.");
                    return;
                }
            }
            SoundTrigger.Keyphrase stKeyphrase = new SoundTrigger.Keyphrase(j,
                    stRecognitionMode, "en_US", keyphraseInfo.keywordPhrase, userIDs);
            keyphrases[j] = stKeyphrase;

            // Create ST KeyphraseRecognitionExtra objects.
            ConfidenceLevel[] confLevels =
                    new ConfidenceLevel[keyphraseInfo.activeUsers.length];
            if (keyphraseInfo.activeUsers.length != stKeyphrase.users.length) {
                Log.e(TAG, "MSG_LOAD_SM: " +
                        "keyphraseInfo.activeUsers.length != " +
                        "stKeyphrase.users.length.");
                return;
            }
            // For all users.
            for (int k = 0; k < keyphraseInfo.activeUsers.length; k++) {
                String userName = keyphraseInfo.activeUsers[k];
                /*int userConfLevel = SVAGlobal.getInstance().getSmRepo().
                        getConfidenceLevel(getApplicationContext(), smNameToLoad,
                                stKeyphrase.text, userName);*/
                int userConfLevel = SceneCommonHelper.getSVALevel();  // King add
                ConfidenceLevel confLevel =
                        new ConfidenceLevel(stKeyphrase.users[k],
                                userConfLevel);
                confLevels[k] = confLevel;
            }
            /*int keyphraseConfLevel = SVAGlobal.getInstance().getSmRepo().
                    getConfidenceLevel(getApplicationContext(), smNameToLoad,
                            keyphrases[j].text, null);*/
            int keyphraseConfLevel = SceneCommonHelper.getSVALevel();  // King add
            Log.v(TAG, "MSG_LOAD_SM: added confLevel= " +
                    keyphraseConfLevel + " for keyphrase= " +
                    keyphrases[j].text);
            KeyphraseRecognitionExtra kre =
                    new KeyphraseRecognitionExtra(
                            j, SoundTrigger.RECOGNITION_MODE_USER_IDENTIFICATION, keyphraseConfLevel, confLevels);  // King modified the second parameter from 0;
            kpRecExtras[j] = kre;
        }
        uuid = new UUID(0, 0);

        StringBuilder sb2 = new StringBuilder();
        for (int j = 0; j < 16; j++) {
            sb2.append(String.format("%02X ", smBbToLoad.get(j)));
        }
//        Log.v(TAG, "bytes #2 (before creating keyphraseSm)= " + sb2.toString());
//        Log.v(TAG, "bytes po/sition before rewind= " + smBbToLoad.position());
        smBbToLoad.rewind(); // in case any reading was done and position moved
//        Log.v(TAG, "bytes position after rewind= " + smBbToLoad.position());
        int remainStatus = smBbToLoad.remaining();
        Log.v(TAG, "bytes remaining= " + remainStatus);
        byte[] smByteArray = new byte[remainStatus];
        smBbToLoad.get(smByteArray);

        KeyphraseSoundModel stKeyphraseSoundModel =
                new KeyphraseSoundModel(uuid, vendorUuid, smByteArray, keyphrases);

        StringBuilder sb3 = new StringBuilder();
        for (int j = 0; j < 16; j++) {
            sb3.append(String.format("%02X ", stKeyphraseSoundModel.data[j]));
        }
//        Log.v(TAG, "bytes #3 (from keyphraseSm before calling loadSoundModel)= " +
//                sb3.toString());
//        Log.v(TAG, "stKeyphraseSoundModel= " + stKeyphraseSoundModel.toString());
        StringBuilder sb = new StringBuilder();
        for (SoundTrigger.Keyphrase keyphrase : stKeyphraseSoundModel.keyphrases) {
            sb.append("\tkeyphrase.id= " + keyphrase.id + "\n");
            sb.append("\tkeyphrase.recognitionModes= " + keyphrase.recognitionModes + "\n");
            sb.append("\tkeyphrase.locale= " + keyphrase.locale + "\n");
            sb.append("\tkeyphrase.text= " + keyphrase.text + "\n");
            for (int user1 : keyphrase.users) {
                sb.append("\t\tuser= " + user1 + "\n");
            }
        }
        Log.v(TAG, "recogConfig= " + sb.toString());

        /*Log.i(TAG, "--> STATUS_OK = " + SoundTrigger.STATUS_OK + "， STATUS_ERROR=" + SoundTrigger.STATUS_ERROR + ", STATUS_PERMISSION_DENIED = " + SoundTrigger.STATUS_PERMISSION_DENIED + ", STATUS_NO_INIT = " + SoundTrigger.STATUS_NO_INIT +
                ", STATUS_BAD_VALUE = " + SoundTrigger.STATUS_BAD_VALUE + ", STATUS_DEAD_OBJECT = " + SoundTrigger.STATUS_DEAD_OBJECT + ", STATUS_INVALID_OPERATION = " + SoundTrigger.STATUS_INVALID_OPERATION);*/

        Log.v(TAG, "MSG_LOAD_SM: calling loadSoundModel");
        int[] tempHandle = new int[20];
        int returnStatus = stModule.loadSoundModel(stKeyphraseSoundModel,
                tempHandle); // 0 is success.
        Log.v(TAG, "loadSM: " + returnStatus);
        if (returnStatus != 0) {
            Log.v(TAG, "MSG_LOAD_SM: loadSoundModel failed with returnStatus= " + returnStatus);
            return;
        }

        Log.v(TAG, "MSG_LOAD_SM: loadSoundModel returned.");

        //start recognition
        int recogValue1 = -1;
        smHandle[handleIndex] = tempHandle[0];
        smSessionManager.addLoadedSmSession(smNameToLoad, smHandle[handleIndex], kpRecExtras);
        Log.v(TAG, "MSG_LOAD_SM in handler");
        recogConfig = new RecognitionConfig(SceneCommonHelper.getBufferingMode() == SceneCommonHelper.BUFFERING_MODE_ENABLE_SVA_BUFFERING, false, kpRecExtras, null);
//        Log.v(TAG, "recogConfig = "
//                + recogConfig.toString());
        if (recogConfig == null) {
            Log.v(TAG, "StartRecognitionTask.doInBackground: cannot continue because SM with soundModelHandle= "
                    + recogValue1
                    + " could not be found or has no recogConfig");
        }
        ArrayList<Integer> startFailureNum = new ArrayList<>();
        int number = smHandle[handleIndex];
        Log.v(TAG, "smHandle = " + Arrays.toString(smHandle));
//        recogValue1 = VwuService.this.stModule.startRecognition(number, localRecognitionConfig1);
        recogValue1 = VwuService.this.stModule.startRecognition(number, recogConfig);
        if (recogValue1 == 0) {
            Log.v(TAG, "StartRecognitionTask.doInBackground: startRecognition succeeded  for soundModelHandle= "
                    + number);
            mMainHandler.sendMessage(mMainHandler.obtainMessage(SceneActivity.MSG_FIRST_RUN_END));
        } else {
            Log.v(TAG, "StartRecognitionTask.doInBackground: startRecognition failed for soundModelHandle= "
                    + number + " with retrunStatus= " + recogValue1);
            startFailureNum.add(number);
        }
        if (!startFailureNum.isEmpty()) {
            Log.v(TAG, "Failed to start recog... on " + startFailureNum);
            // King debug
            //mToSpeak.toSpeak("Sorry, failed to start recognition.", true);
        }
    }

    private void setRecogConfig() {
        int confidenceInConfig = SceneCommonHelper.getSVALevel();
        ConfidenceLevel confidenceLevel012 = new ConfidenceLevel(
                1, confidenceInConfig);
        ConfidenceLevel[] confLevel012 = new ConfidenceLevel[]{confidenceLevel012};
        KeyphraseRecognitionExtra recognitionExtra012 = new KeyphraseRecognitionExtra(
                0, 0, confidenceInConfig, confLevel012);
        mKeyRecogExtras = new KeyphraseRecognitionExtra[]{recognitionExtra012};
        localRecognitionConfig1 = new RecognitionConfig(
                SceneCommonHelper.getBufferingMode() == SceneCommonHelper.BUFFERING_MODE_ENABLE_SVA_BUFFERING, false, mKeyRecogExtras, null);
    }

    private void turnOnDisplay() {
        Log.v(TAG, "turnOnDisplay");
        wakeup();
    }

    private void startWakelockTimer() {
        Log.v(TAG, "startWakelockTimer");
        stopWakelockTimer();
        this.wakelockTimer = new Timer();
        this.wakelockTimer.schedule(new TimerTask() {
            public void run() {
                Log.v(TAG, "startWakelockTimer: run()- release"
                        + " wakelock");
                VwuService.this.stopWakelockTimer();
            }
        }, 5000L);
    }

    private void stopWakelockTimer() {
        if (this.wakelockTimer != null) {
            this.wakelockTimer.cancel();
            int i = this.wakelockTimer.purge();
            Log.v(TAG, "stopWakelockTimer: numPurgedTasks= " + i);
        }
    }

    private boolean isForeGround() {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfos = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfos) {
            if (appProcessInfo.processName.equals(getApplicationInfo().packageName)) {
                Log.v(TAG, "appProcessInfo.processName " + appProcessInfo.processName);
                Log.v(TAG, "appProcessInfo.importance = " + appProcessInfo.importance);
                return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && appProcessInfo.importanceReasonCode == ActivityManager.RunningAppProcessInfo.REASON_UNKNOWN;
            }
        }
        return false;
    }

    public void serviceDetachSVA() {
        Log.v(TAG, "DETACH  SVA!!!");
//        svaReceicerHandler.sendMessage(svaReceicerHandler.obtainMessage(MSG_STOP_SVA));
        onServiceDied();
    }

    public void serviceStopRecog() {
        if (stModule != null) {
            Log.v(TAG, "stop and unload smHandle = " + Arrays.toString(smHandle));
            for (int i = 0; i < smHandle.length; i++) {
                if (smHandle[i] != 0) {
                    stModule.stopRecognition(smHandle[i]);
                    stModule.unloadSoundModel(smHandle[i]);
                    //smSessionManager.addUnloadedSmSession(smHandle[0]);
                }
            }
            smSessionManager.removeSmNameAll();
            stModule.detach();
        } else {
            Log.v(TAG, "stModule is null");
        }
    }

    public void serviceStartRecog() {
        serviceStopRecog();
        if (stModule != null) {
            Log.v(TAG, "serviceStartRecog...");
            AttachModuleTask attachModuleTask = new AttachModuleTask();
            attachModuleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            Log.v(TAG, "register SM...service start Reocog.");
        } else Log.v(TAG, "stModule is null");
    }

    //mergeUDK. This way is like "1+1=2, 2+1=3, 3+1=4." merge the models one by one.
    private void mergeUDK(ArrayList<String> keyphraseListInUDK, String beMergedFileInUDK) {
        SVAGlobal.getInstance().getSmRepo().createNewSoundModel(keyphraseListInUDK, beMergedFileInUDK);
        final ArrayList<ByteBuffer> soundModelsToMergeByteBuffers = SVAGlobal.getInstance().getSmRepo().getSoundModelsToMergeByteBuffers();
        ByteBuffer[] buffersTOMerge = new ByteBuffer[soundModelsToMergeByteBuffers.size()];
        buffersTOMerge = soundModelsToMergeByteBuffers.toArray(buffersTOMerge);
        Log.v(TAG, "mergeUDK buffersTOMerge size = " + buffersTOMerge.length);
        for (ByteBuffer bftest : buffersTOMerge) {
            Log.v(TAG, "array = " + bftest.toString());
        }
        if (buffersTOMerge == null) {
            Log.e(TAG, "MSG_CREATE_MERGED_SM: sound models do not exist");
            return;
        }
        int sizeWhenMerged = ListenSoundModel.getSizeWhenMerged(buffersTOMerge);
        if (sizeWhenMerged < 0) {
            Log.e(TAG, "error : sizeWhenMerged < 0 ;");
            return;
        }
        Log.v(TAG, "getSizeWhenMerged " + sizeWhenMerged);
        SVAGlobal.getInstance().getSmRepo().createSoundModelToModifyByteBuffer(sizeWhenMerged);
        int mergeReturnStatus = ListenSoundModel.merge(buffersTOMerge, SVAGlobal.getInstance().getSmRepo().getSoundModelToModifyByteBuffer());

        if (mergeReturnStatus == 0) {
            Log.v(TAG, "MSG_CREATE_MERGED_SM: merge successful");

            SVAGlobal.getInstance().getSmRepo().saveMergedSoundModel();
            /*Utils.saveByteBufferToFile(tosaveBF, mergedFile);
            File deleteAfterMergedFile = new File(deleteAfterMerged);
            if (deleteAfterMergedFile.exists())
                Log.v(TAG, "Delete " + deleteAfterMergedFile.getName() +
                        "--- after merged status = " + deleteAfterMergedFile.delete());*/
        } else {
            Log.v(TAG, "merge failure");
        }
    }

    private void deleteUDKFromMerged(String keyPhrase, String userName, String mergedFile) {
        Log.v(TAG, "keyPhrase to delete = " + keyPhrase);
        Log.v(TAG, "mCurrentTrainingUserName to delete = " + userName);
        Log.v(TAG, "mergedfile to delete = " + mergedFile);
        ByteBuffer mergedByteBuffer = Utils.readFileToByteBuffer(mergedFile);
//        ByteBuffer mergedByteBuffer = Utils.readFileToByteBuffer(SVAGlobal.PATH_APP + File.separator + "Hello2.udm");
//        ByteBuffer mergedByteBuffer = Utils.readFileToByteBuffer(mergedFile);

//        keyPhrase = null;
//        resultFromSceneAct = "(null)";
        final int sizeAfterDelete = ListenSoundModel.getSizeAfterDelete(mergedByteBuffer, keyPhrase, userName);
        Log.v(TAG, "sizeAfterDelete = " + sizeAfterDelete);
        if (sizeAfterDelete < 0) {
            Log.v(TAG, "sizeAfterDelete < 0 .");
            return;
        }
        SVAGlobal.getInstance().getSmRepo().createSoundModelToModifyByteBuffer(sizeAfterDelete);
        int status = ListenSoundModel.deleteData(mergedByteBuffer, keyPhrase, userName,
                SVAGlobal.getInstance().getSmRepo().getSoundModelToModifyByteBuffer());
        if (0 == status) {
            SVAGlobal.getInstance().getSmRepo().saveSoundModelToDeleteFrom();
            File toDelSM = new File(SVAGlobal.PATH_APP + File.separator + recognizedKeyWord + UDM_SUFFIX);
            if (toDelSM.exists()) {
                boolean delStatus = toDelSM.delete();
                Log.v(TAG, "delete model keyword : " + recognizedKeyWord +
                        "\\n user : " + recognizedUser +
                        "\\n return status : " + delStatus);
            } else
                Log.v(TAG, "File does not exist. --- " + toDelSM.getAbsolutePath());
            Log.v(TAG, "Delete Sound model success!");
        } else {
            Log.v(TAG, "Delete Sound model failure!");
        }
    }

    private class SmSessionManager {
        private boolean IS_A_SESSION_BUFFERING_DEFAULT;
        private boolean IS_A_SESSION_STARTED_DEFAULT;
        private boolean IS_SERVICE_ENABLED_DEFAULT;
        private boolean isASessionBuffering;
        private boolean isASessionStarted;
        private boolean isServiceEnabled;
        private ArrayList<String> loadedSmNames;
        private int numStartedSessions;
        private Map<Integer, SmSession> smSessions;

        private SmSessionManager() {
            this.IS_A_SESSION_STARTED_DEFAULT = false;
            this.IS_A_SESSION_BUFFERING_DEFAULT = false;
            this.IS_SERVICE_ENABLED_DEFAULT = false;
            this.smSessions = new HashMap<Integer, SmSession>();
            this.isASessionStarted = this.IS_A_SESSION_STARTED_DEFAULT;
            this.isASessionBuffering = this.IS_A_SESSION_BUFFERING_DEFAULT;
            this.isServiceEnabled = this.IS_SERVICE_ENABLED_DEFAULT;
            this.numStartedSessions = 0;
            this.loadedSmNames = new ArrayList<String>();
            for (int i = 0; i < 8; ++i) {
                this.loadedSmNames.add(null);
            }
        }

        private void addLoadedSmSession(final String s, final int n, final KeyphraseRecognitionExtra[] array) {
//            Log.v(TAG, "addLoadedSmSession");
            this.smSessions.put(n, new SmSession(s, array, SVAGlobal.SmState.LOADED));
            if (this.smSessions.get(n).sessionNum == -1) {
                Log.e(TAG, "addLoadedSmSession: failed to add loaded SM with smHandle= " + n + ", smName= " + this.smSessions.get(n).smName + ". NumSessions is too large.");
            }
            Log.v(TAG, "addLoadedSmSession: successfully added loaded SM with smHandle= " + n + ", smName= " + this.smSessions.get(n).smName);
            SVAGlobal.getInstance().getSmRepo().setLoadedSmNames(this.loadedSmNames);
        }

        private int addSmNameToLoadedSmNames(final String s) {
            for (int i = 0; i < this.loadedSmNames.size(); ++i) {
                if (this.loadedSmNames.get(i) == null) {
                    this.loadedSmNames.set(i, s);
                    return i;
                }
            }
            return -1;
        }

        private boolean removeSmNameFromLoadedSmNames(final String s) {
            for (int i = 0; i < this.loadedSmNames.size(); ++i) {
                if (this.loadedSmNames.get(i) != null && this.loadedSmNames.get(i).equals(s)) {
                    this.loadedSmNames.set(i, null);
                    return true;
                }
            }
            return false;
        }

        private boolean removeSmNameAll() {
            this.loadedSmNames.clear();
            this.smSessions.clear();
            for (int i = 0; i < 8; ++i) {
                this.loadedSmNames.add(null);
            }
            return false;
        }

        public void addStartedSmSession(final int n, final RecognitionConfig recogConfig) {
            this.isASessionStarted = true;
            this.smSessions.get(n).setRecogConfig(recogConfig);
            this.smSessions.get(n).state = SVAGlobal.SmState.STARTED;
            ++this.numStartedSessions;
        }

        public void addStoppedSmSession(final int n) {
            this.smSessions.get(n).state = SVAGlobal.SmState.STOPPED;
            --this.numStartedSessions;
            this.smSessions.get(n).removeRecogConfig();
            if (this.numStartedSessions == 0) {
                Log.v(TAG, "addStoppedSmSession: last remaining started session has been stopped");
                this.isASessionStarted = false;
            }
        }

        public void addUnloadedSmSession(final int n) {
            final SmSession smSession = this.smSessions.remove(n);
            if (smSession == null) {
                Log.e(TAG, "addUnloadedSmSession: could not remove SM with smHandle= " + n + ", could not be found");
            } else {
                if (!this.removeSmNameFromLoadedSmNames(smSession.smName)) {
                    Log.e(TAG, "addUnloadedSmSession: could not remove SM with smName= " + smSession.smName + " from loadedSmNames. SM could not be found.");
                }
            }
        }

        public boolean getIsASessionBuffering() {
            return this.isASessionBuffering;
        }

        public boolean getIsServiceEnabled() {
            return this.isServiceEnabled;
        }

        public KeyphraseRecognitionExtra[] getLoadedSmSessionKpRecogExtras(final int n) {
            if (this.smSessions.containsKey(n)) {
                return this.smSessions.get(n).kpRecogExtra;
            }
            return null;
        }

        public RecognitionConfig getRecogConfig(final int n) {
            if (this.smSessions.containsKey(n)) {
                return this.smSessions.get(n).recogConfig;
            }
            return null;
        }

        public int getSessionNum(final int n) {
            if (this.smSessions.containsKey(n)) {
                return this.smSessions.get(n).sessionNum;
            }
            return -1;
        }

        public int getSmHandle(final String s, final SVAGlobal.SmState smState) {
            int intValue = -1;
            for (final Map.Entry<Integer, SmSession> entry : this.smSessions.entrySet()) {
                Log.v(TAG, "getSmHandle: inSmName= " + s + ", checking against smName= " + entry.getValue().smName);
                if (entry.getValue().smName.equals(s)) {
                    intValue = entry.getKey();
                }
            }
            if (intValue != -1 && smState != null && !this.isStatesTransitionable(this.smSessions.get(intValue).state, smState)) {
                Log.e(TAG, "getSmHandle: expected state= " + smState.name() + ", actual state= " + this.smSessions.get(intValue).state.name());
                return -2;
            }
            return intValue;
        }

        public String getSmName(final int n) {
            if (this.smSessions.containsKey(n)) {
                return this.smSessions.get(n).smName;
            }
            return null;
        }

        public ArrayList<Integer> getStartedSessionsSmHandles() {
            final ArrayList<Integer> list = new ArrayList<Integer>();
            for (final Map.Entry<Integer, SmSession> entry : this.smSessions.entrySet()) {
                Log.v(TAG, "getStartedSessionsSmHandles: checking smHandle= " + entry.getKey());
                if (entry.getValue().state.equals(SVAGlobal.SmState.STARTED)) {
                    list.add(entry.getKey());
                    Log.v(TAG, "getStartedSessionsSmHandles: added smHandle= " + entry.getKey());
                }
            }
            return list;
        }

        public SVAGlobal.SmState getState(final int n) {
            if (this.smSessions.containsKey(n)) {
                return this.smSessions.get(n).state;
            }
            return null;
        }

        public boolean isASessionStarted() {
            return this.isASessionStarted;
        }

        public boolean isStatesTransitionable(final SVAGlobal.SmState smState, final SVAGlobal.SmState smState2) {
            final boolean b = true;
            Label_0123:
            {
                switch (smState2) {
                    case UNLOADED:
                        if (smState == SVAGlobal.SmState.LOADED) {
                            return true;
                        }
                        if (smState != SVAGlobal.SmState.STOPPED) {
                            break;
                        }
                        return true;
                    case LOADED:
                        if (smState != SVAGlobal.SmState.UNLOADED) {
                            break;
                        }
                        return true;
                    case STARTED:
                        if (smState == SVAGlobal.SmState.LOADED) {
                            return true;
                        }
                        if (smState != SVAGlobal.SmState.STOPPED) {
                            break;
                        }
                        return true;
                    case STOPPED:
                        if (smState == SVAGlobal.SmState.STARTED) {
                            return true;
                        }
                        break;
                }
            }
            Log.e(TAG, "isStatesTransitionable: unrecognized state= " + smState2.name());
            return false;
        }

        public void setIsASessionBuffering(final boolean isASessionBuffering) {
            this.isASessionBuffering = isASessionBuffering;
        }

        public void setServiceEnabled(final boolean isServiceEnabled) {
            this.isServiceEnabled = isServiceEnabled;
        }

        public void updateStateServiceDied() {
            this.smSessions = new HashMap<Integer, SmSession>();
            this.isASessionStarted = this.IS_A_SESSION_STARTED_DEFAULT;
            this.isASessionBuffering = this.IS_A_SESSION_BUFFERING_DEFAULT;
            this.isServiceEnabled = this.IS_SERVICE_ENABLED_DEFAULT;
            this.numStartedSessions = 0;
            this.loadedSmNames = new ArrayList<String>();
            for (int i = 0; i < 8; ++i) {
                this.loadedSmNames.add(null);
            }
            SVAGlobal.getInstance().getSmRepo().setLoadedSmNames(this.loadedSmNames);
        }

        private class SmSession {
            private final KeyphraseRecognitionExtra[] kpRecogExtra;
            public RecognitionConfig recogConfig;
            public int sessionNum;
            public final String smName;
            public SVAGlobal.SmState state;

            public SmSession(final String smName, final KeyphraseRecognitionExtra[] kpRecogExtra, final SVAGlobal.SmState state) {
                this.state = SVAGlobal.SmState.UNLOADED;
                this.recogConfig = null;
                this.sessionNum = -1;
                this.smName = smName;
                this.kpRecogExtra = kpRecogExtra;
                this.state = state;
                this.sessionNum = SmSessionManager.this.addSmNameToLoadedSmNames(this.smName);
            }

            public void removeRecogConfig() {
                this.recogConfig = null;
            }

            public void setRecogConfig(final RecognitionConfig recogConfig) {
                this.recogConfig = recogConfig;
            }
        }
    }
}
