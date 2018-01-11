package com.wistron.demo.tool.teddybear.scene.voiceactivation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.Log;

import com.qualcomm.listen.ListenTypes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SVAGlobal {
    public static final String APP_NAME = "SVA";
    public static final boolean DEFAULT_AUTOSTART = false;
    public static final boolean DEFAULT_ENABLE_LISTEN = false;
    public static final boolean DEFAULT_ENABLE_VOICEREQUESTS = false;
    public static final boolean DEFAULT_ENABLE_VOICEWAKEUP = false;
    public static final boolean DEFAULT_FAILURE_FEEDBACK_ENABLED = false;
    private static final int DEFAULT_KEYWORD_CONFIDENCE_LEVEL = 69;
    public static final boolean DEFAULT_SHOW_ADVANCED_DETAIL = false;
    public static final boolean DEFAULT_TONE_ENABLED = false;
    private static final int DEFAULT_TRAINING_CONFIDENCE_LEVEL = 72;
    public static final String DEFAULT_USERNAME = "defaultUser";
    private static final int DEFAULT_USER_CONFIDENCE_LEVEL = 69;
    public static final boolean DEFAULT_USER_VERIFICATION_ENABLED = false;
    public static final String DEFAULT_VERSION_NUMBER = "No Version Number";
    public static final double DEFAULT_VOICEREQUESTLENGTH_SECONDS = 3.0D;
    public static final String EXTRA_ACTION_DELETE_KEYPHRASE = "com.qualcomm.qti.sva.SVAGlobal.deleteKeyphrase";
    public static final String EXTRA_ACTION_UPDATE_KEYPHRASEACTION = "com.qualcomm.qti.sva.SVAGlobal.updateKeyphraseAction";
    public static final String EXTRA_DATA_KEYPHRASE_ACTION = "com.qualcomm.qti.sva.SVAGlobal.keyphraseAction";
    public static final String EXTRA_DATA_KEYPHRASE_NAME = "com.qualcomm.qti.sva.SVAGlobal.keyphraseName";
    public static final String EXTRA_SELECTACTION_MODE = "selectaction mode";
    public static final String EXTRA_TRAINING_RESULT = "com.qualcomm.qti.sva.SVAGlobal.trainingResult";
    public static final int FAILURE = -1;
    public static final String MODE_SELECTACTION_TRAINING = "selectaction mode training";
    public static final String NO_USERNAME = "<No User>";
    public static final int NUM_MAX_KEYPHRASES = 8;
    public static final int NUM_MAX_SESSIONS = 8;
    public static final int NUM_TRAINING_RECORDINGS_REQUIRED = 5;
    //    public static final String PATH_APP = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "SVA";
    public static final String PATH_APP = Environment.getExternalStorageDirectory().getAbsolutePath() + "/teddy_bear/SVA";
    public static final String PATH_DATA_FILEEXT = ".data";
    public static final String PATH_RECORDINGS_FILEEXT = ".wav";
    public static final String BUFFERING_RECORDINGS_FILE_NAME = "buffering_data";
    public static final String PATH_RECORDINGS_TEMP_FILE;
    public static final String PATH_TRAINING_FILENAME = "training";
    public static final String PATH_TRAINING_RECORDINGS = PATH_APP + "/" + "trainings";
    public static final String PATH_TRAINING_FILES_RECORDINGS = PATH_TRAINING_RECORDINGS + "/" + "training";
    public static final String PATH_VOICE_REQUESTS;
    public static final String PATH_DEFAULT_LM = PATH_APP + "/" + "default.lm";

    public static final String PREFERENCE_GROUP_NAME = "SVA";
    public static final String PREFERENCE_TAG_NAME = "Settings";
    public static final double SHORTS_PER_SECOND = 16000.0D;
    public static final int SUCCESS = 0;
    private static final String TAG = "ListenLog.SVAGlobal";
    public static final String TAG_DELETED_SOUND_MODEL = "deletedSoundModel";
    public static final String TAG_DELETED_SOUND_MODEL_NAME = "deletedSoundModelName";
    public static final String TAG_FIRST_RUN_OF_APP = "firstRunOfApp";
    public static final String TAG_KEYPHRASE_TO_DELETE = "keyPhraseToDelete";
    public static final String TAG_REQUIRES_TRAINING = "isUdkSm";
    private static final String TAG_SETTING_DEFAULT_KEYWORD_CONFIDENCE_LEVEL = "keywordThreshold";
    private static final String TAG_SETTING_DEFAULT_TRAINING_CONFIDENCE_LEVEL = "trainingThreshold";
    private static final String TAG_SETTING_DEFAULT_USER_CONFIDENCE_LEVEL = "userThreshold";
    private static final String TAG_SETTING_FAILURE_FEEDBACK_ENABLED = "failureFeedbackEnabled";
    private static final String TAG_SETTING_LISTEN_ENABLED = "listenEnabled";
    public static final String TAG_SETTING_SELECTED_SOUND_MODEL_NAME = "selectedSoundModelName";
    private static final String TAG_SETTING_SHOW_ADVANCED_DETAIL = "showAdvanceDetail";
    private static final String TAG_SETTING_TONE_ENABLED = "toneEnabled";
    private static final String TAG_SETTING_USER_VERIFICATION_ENABLED = "userVerificationEnabled";
    private static final String TAG_SETTING_VOICEWAKEUP_ENABLED = "voicewakeupEnabled";
    private static final String TAG_SETTING_VOICE_REQUESTS_ENABLED = "voiceRequestsEnabled";
    private static final String TAG_SETTING_VOICE_REQUEST_LENGTH = "voiceRequestLength";
    public static final String TAG_SOUND_MODEL_NAME = "soundModelName";
    public static final String TAG_TRAINING_IS_UDK = "trainingIsUdk";
    public static final String TAG_TRAINING_KEYPHRASE = "trainingKeyword";
    public static final String TAG_TRAINING_USER = "trainingUser";
    public static final String TAG_USER_TO_DELETE = "userToDelete";
    private int addDetectionCounter;
    private boolean autoStart = false;
    private ListenTypes.ConfidenceData confidenceData = new ListenTypes.ConfidenceData();
    private BlockingQueue<DetectionContainer> detectionContainers = new ArrayBlockingQueue(100);
    private boolean enableListen = true;
    private boolean enableVoiceWakeup = true;
    private boolean failureFeedbackEnabled = false;
    private int keyPhraseConfidenceLevel = 69;
    private String lastVoiceRequestFilePath = null;
    private boolean libsError = false;
    private int numActivitiesShowing = 0;
    private int numUserRecordings = 0;
    private int removeDetectionCounter;
    private boolean showAdvancedDetail = true;
    private SoundModelRepository smRepo = new SoundModelRepository();
    private boolean toneEnabled = true;
    private int trainingConfidenceLevel = 72;
    private int userConfidenceLevel = 69;
    private ShortBuffer[] userRecordings = new ShortBuffer[5];
    private boolean userVerificationEnabled = false;
    private String versionNumber = "No Version Number";
    //private double voiceRequestLengthSeconds = 3.0D;
    private double voiceRequestLengthSeconds = 4.0D;
    private boolean voiceRequestsEnabled = false;

    static {
        PATH_RECORDINGS_TEMP_FILE = PATH_TRAINING_RECORDINGS + "/" + "temp_recording.wav";
        PATH_VOICE_REQUESTS = PATH_APP + "/" + "voiceRequests";
    }

    public static SVAGlobal getInstance() {
        return GlobalInstance.Instance;
    }

    public void addUserRecording() {
        Log.v("ListenLog.SVAGlobal", "addUserRecording: numUserRecordings before insert= " + this.numUserRecordings);
        try {
            String str = getLastUserRecordingFilePath();
            int i = this.numUserRecordings;
            this.numUserRecordings = (i + 1);
            userRecordings[i] = com.wistron.demo.tool.teddybear.scene.voiceactivation.Utils.readWavFile(str);
            Log.v("ListenLog.SVAGlobal", "addUserRecording: this.numUserRecordings after insert= " + this.numUserRecordings);
        } catch (FileNotFoundException localFileNotFoundException) {
            Log.e("ListenLog.SVAGlobal", "addUserRecording: File cannot be opened or created based on mode. Error= " + localFileNotFoundException.getMessage());
        } catch (IOException localIOException) {
            Log.e("ListenLog.SVAGlobal", "addUserRecording: Unable to readWaveFile. File is closed or another I/O error has occurred. Error= " + localIOException.getMessage());
        }
    }

    public void decrementNumActivitiesShowing() {
        this.numActivitiesShowing = (-1 + this.numActivitiesShowing);
        Log.v("ListenLog.SVAGlobal", "decrementNumActivitiesShowing: numActivitiesShowing now = " + this.numActivitiesShowing);
    }

    public void discardLastUserRecording() {
        Log.v("ListenLog.SVAGlobal", "discardLastUserRecording: getNumUserRecordings() before discard= " + getNumUserRecordings());
        this.numUserRecordings = (-1 + this.numUserRecordings);
        Log.v("ListenLog.SVAGlobal", "discardLastUserRecording: getNumUserRecordings() after discard= " + getNumUserRecordings());
    }

    public boolean getAutoStart() {
        return this.autoStart;
    }

    public ListenTypes.ConfidenceData getConfidenceData() {
        return this.confidenceData;
    }

    public DetectionContainer getDetectionContainer() {
        this.removeDetectionCounter = (1 + this.removeDetectionCounter);
        Log.v("ListenLog.SVAGlobal", "getDetectionContainer: removeDetectionCounter= " + this.removeDetectionCounter);
        int i = -1 + this.detectionContainers.size();
        Log.v("ListenLog.SVAGlobal", "getDetectionContainer: this.detectionContainers.size() after removal= " + i);
        return this.detectionContainers.poll();
    }

    public int getDetectionMode() {
        if (this.userVerificationEnabled) {
            return 1;
        } else return 2;
    }

    public boolean getIsFirstRunOfApp(Context paramContext) {
        SharedPreferences localSharedPreferences = paramContext.getSharedPreferences("SVA", 0);
        boolean bool = localSharedPreferences.getBoolean("firstRunOfApp", true);
        if (bool)
            localSharedPreferences.edit().putBoolean("firstRunOfApp", false);
        return bool;
    }

    public ByteBuffer getLanguageModel() {
        Log.v("ListenLog.SVAGlobal", "getLanguageModel");
        String str = PATH_APP + "/" + "default.lm";
        if (new File(str).exists()) {
            return Utils.readFileToByteBuffer(str);
        } else
            Log.v("ListenLog.SVAGlobal", "getSoundModelFromName: language model with filePath: " + str + " does not exist");
        return null;
    }

    public ShortBuffer getLastUserRecording() {
        Log.v("ListenLog.SVAGlobal", "getLastUserRecording: getNumUserRecordings()= " + getNumUserRecordings());
        return this.userRecordings[(-1 + getNumUserRecordings())];
    }

    public String getLastUserRecordingFilePath() {
        String str1 = Integer.toString(1 + this.numUserRecordings);
        String str2 = PATH_TRAINING_RECORDINGS + "/" + "training" + str1 + ".wav";
        Log.v("ListenLog.SVAGlobal", "getLastUserRecordingFilePath: filePath= " + str2);
        return str2;
    }

    public String getLastVoiceRequestFilePath() {
        return this.lastVoiceRequestFilePath;
    }

    public boolean getLibsError() {
        return this.libsError;
    }

    public int getNumActivitiesShowing() {
        return this.numActivitiesShowing;
    }

    public int getNumUserRecordings() {
        return this.numUserRecordings;
    }

    public boolean getSettingDetectionTone() {
        return this.toneEnabled;
    }

    public boolean getSettingEnableListen() {
        return this.enableListen;
    }

    public boolean getSettingEnableVoiceWakeup() {
        return this.enableVoiceWakeup;
    }

    public boolean getSettingFailureFeedback() {
        return this.failureFeedbackEnabled;
    }

    public int getSettingKeyPhraseConfidenceLevel() {
        return this.keyPhraseConfidenceLevel;
    }

    public boolean getSettingShowAdvancedDetail() {
        return this.showAdvancedDetail;
    }

    public int getSettingTrainingConfidenceLevel() {
        return this.trainingConfidenceLevel;
    }

    public int getSettingUserConfidenceLevel() {
        return this.userConfidenceLevel;
    }

    public boolean getSettingUserVerification() {
        return this.userVerificationEnabled;
    }

    public double getSettingVoiceRequestLength() {
        return this.voiceRequestLengthSeconds;
    }

    public boolean getSettingVoiceRequestsEnabled() {
        return this.voiceRequestsEnabled;
    }

    public SoundModelRepository getSmRepo() {
        return this.smRepo;
    }

    public ShortBuffer[] getUserRecordings() {
        return this.userRecordings;
    }

    public String getVersionNumber() {
        return this.versionNumber;
    }

    public void incrementNumActivitiesShowing() {
        this.numActivitiesShowing = (1 + this.numActivitiesShowing);
        Log.v("ListenLog.SVAGlobal", "incrementNumActivitiesShowing: numActivitiesShowing now = " + this.numActivitiesShowing);
    }

    public void loadSettingsFromSharedPreferences(Context paramContext) {
        SharedPreferences localSharedPreferences = paramContext.getSharedPreferences("SVA", 0);
        this.keyPhraseConfidenceLevel = localSharedPreferences.getInt("keywordThreshold", 69);
        this.userConfidenceLevel = localSharedPreferences.getInt("userThreshold", 69);
        this.trainingConfidenceLevel = localSharedPreferences.getInt("trainingThreshold", 72);
        this.enableListen = localSharedPreferences.getBoolean("listenEnabled", this.enableListen);
        this.enableVoiceWakeup = localSharedPreferences.getBoolean("voicewakeupEnabled", this.enableVoiceWakeup);
        this.toneEnabled = localSharedPreferences.getBoolean("toneEnabled", this.toneEnabled);
        this.showAdvancedDetail = localSharedPreferences.getBoolean("showAdvanceDetail", this.showAdvancedDetail);
        this.userVerificationEnabled = localSharedPreferences.getBoolean("userVerificationEnabled", this.userVerificationEnabled);
        this.voiceRequestsEnabled = localSharedPreferences.getBoolean("voiceRequestsEnabled", this.voiceRequestsEnabled);
        this.voiceRequestLengthSeconds = Double.parseDouble(localSharedPreferences.getString("voiceRequestLength", String.valueOf(this.voiceRequestLengthSeconds)));
        this.failureFeedbackEnabled = localSharedPreferences.getBoolean("failureFeedbackEnabled", this.failureFeedbackEnabled);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: defaultKeywordConfidenceLevel= " + this.keyPhraseConfidenceLevel);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: defaultUserConfidenceLevel= " + this.userConfidenceLevel);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: defaultTrainingConfidenceLevel= " + this.trainingConfidenceLevel);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: enableListen= " + this.enableListen);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: enableVoiceWakeup= " + this.enableVoiceWakeup);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: toneEnabled= " + this.toneEnabled);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: showAdvancedDetail= " + this.showAdvancedDetail);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: userVerificationEnabled= " + this.userVerificationEnabled);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: voiceRequestsEnabled= " + this.voiceRequestsEnabled);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: voiceRequestLength= " + this.voiceRequestLengthSeconds);
        Log.v("ListenLog.SVAGlobal", "loadSettingsFromSharedPreferences: failureFeedbackEnabled= " + this.failureFeedbackEnabled);
    }

    public void removeExistingRecordingFiles() {
        for (int i = 0; i < 5; ++i) {
            String str1 = Integer.toString(i + 1);
            String str2 = PATH_TRAINING_RECORDINGS + "/" + "training" + str1 + ".wav";
            Log.v("ListenLog.SVAGlobal", "removeExistingRecordingFiles: filePath= " + str2);
            File localFile = new File(str2);
            if ((localFile.exists())) {
                boolean a = localFile.delete();
                Log.v("ListenLog.SVAGlobal", "removeExistingRecordingFiles: file deleted= " + str2 + "  " + a);
            }
        }
    }

    public void removeUserRecordings() {
        Log.v("ListenLog.SVAGlobal", "removeUserRecordings: getNumUserRecordings() before remove= " + getNumUserRecordings());
        Arrays.fill(this.userRecordings, null);
        this.numUserRecordings = 0;
        Log.v("ListenLog.SVAGlobal", "removeUserRecordings: getNumUserRecordings() after remove= " + getNumUserRecordings());
    }

    public void saveSettingsToSharedPreferences(Context paramContext) {
        Editor localEditor = paramContext.getSharedPreferences("SVA", 0).edit();
        localEditor.putInt("keywordThreshold", this.keyPhraseConfidenceLevel);
        localEditor.putInt("userThreshold", this.userConfidenceLevel);
        localEditor.putInt("trainingThreshold", this.trainingConfidenceLevel);
        localEditor.putBoolean("listenEnabled", this.enableListen);
        localEditor.putBoolean("voicewakeupEnabled", this.enableVoiceWakeup);
        localEditor.putBoolean("toneEnabled", this.toneEnabled);
        localEditor.putBoolean("showAdvanceDetail", this.showAdvancedDetail);
        localEditor.putBoolean("userVerificationEnabled", this.userVerificationEnabled);
        localEditor.putBoolean("voiceRequestsEnabled", this.voiceRequestsEnabled);
        localEditor.putString("voiceRequestLength", String.valueOf(this.voiceRequestLengthSeconds));
        localEditor.putBoolean("failureFeedbackEnabled", this.failureFeedbackEnabled);
        localEditor.commit();
    }

    public void saveVoiceRequest(byte[] paramArrayOfByte, int paramInt, String paramString) {
        Log.v("ListenLog.SVAGlobal", "saveVoiceRequest");
        //not decoded
        Utils.writeBufferToWavFile(paramArrayOfByte, paramInt, paramString, false);
        this.lastVoiceRequestFilePath = paramString;
    }

    public void setAutoStart(boolean paramBoolean) {
        this.autoStart = paramBoolean;
    }

    public void setDetectionContainer(int paramInt1, ListenTypes.EventData paramEventData, ListenTypes.VoiceWakeupDetectionDataV2 paramVoiceWakeupDetectionDataV2, int paramInt2, String paramString) {
        DetectionContainer localDetectionContainer = new DetectionContainer(paramInt1, paramEventData, paramVoiceWakeupDetectionDataV2, paramInt2, paramString);
        if (!(this.detectionContainers.offer(localDetectionContainer)))
            Log.e("ListenLog.SVAGlobal", "setDetectionContainer: not able to add detection container");
        else {
            this.addDetectionCounter = (1 + this.addDetectionCounter);
            Log.v("ListenLog.SVAGlobal", "setDetectionContainer: addDetectionCounter= " + this.addDetectionCounter);
            Log.v("ListenLog.SVAGlobal", "setDetectionContainer: this.detectionContainers.size()= " + this.detectionContainers.size());
        }
    }

    public void setLibsError(boolean paramBoolean) {
        this.libsError = paramBoolean;
    }

    public void setSettingDetectionTone(boolean paramBoolean) {
        this.toneEnabled = paramBoolean;
    }

    public void setSettingEnableListen(boolean paramBoolean) {
        this.enableListen = paramBoolean;
    }

    public void setSettingEnableVoiceWakeup(boolean paramBoolean) {
        this.enableVoiceWakeup = paramBoolean;
    }

    public void setSettingFailureFeeback(boolean paramBoolean) {
        this.failureFeedbackEnabled = paramBoolean;
    }

    public void setSettingKeyPhraseThreshold(int paramInt) {
        this.keyPhraseConfidenceLevel = paramInt;
    }

    public void setSettingShowAdvancedDetail(boolean paramBoolean) {
        this.showAdvancedDetail = paramBoolean;
    }

    public void setSettingTrainingConfidenceLevel(int paramInt) {
        this.trainingConfidenceLevel = paramInt;
    }

    public void setSettingUserConfidenceLevel(int paramInt) {
        this.userConfidenceLevel = paramInt;
    }

    public void setSettingUserVerification(boolean paramBoolean) {
        this.userVerificationEnabled = paramBoolean;
    }

    public void setSettingVoiceRequestLength(double paramDouble) {
        Log.v("ListenLog.SVAGlobal", "setSettingVoiceRequestLength: inLength= " + paramDouble);
        this.voiceRequestLengthSeconds = paramDouble;
    }

    public void setSettingVoiceRequestsEnabled(boolean paramBoolean) {
        this.voiceRequestsEnabled = paramBoolean;
    }

    public void setVersionNumber(String paramString) {
        this.versionNumber = paramString;
    }

    public class DetectionContainer {
        public ListenTypes.EventData eventData;
        public int eventType;
        public int sessionNum;
        public String smName;
        public ListenTypes.VoiceWakeupDetectionDataV2 vwuDetectionData;

        public DetectionContainer(int paramInt1, ListenTypes.EventData paramEventData, ListenTypes.VoiceWakeupDetectionDataV2 paramVoiceWakeupDetectionDataV2, int paramInt2, String paramString) {
            Log.v("ListenLog.SVAGlobal", "DetectionContainer: constructor- inSessionNum= " + paramInt2);
            this.eventType = paramInt1;
            this.eventData = paramEventData;
            this.vwuDetectionData = paramVoiceWakeupDetectionDataV2;
            this.sessionNum = paramInt2;
            this.smName = paramString;
        }
    }

    private static class GlobalInstance {
        public static SVAGlobal Instance = new SVAGlobal();
    }

    public static enum SmState {
        UNLOADED,
        LOADED,
        STARTED,
        STOPPED
    }
}