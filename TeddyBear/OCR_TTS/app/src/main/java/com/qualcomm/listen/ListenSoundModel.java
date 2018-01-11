package com.qualcomm.listen;

import android.util.Log;

import com.qualcomm.listen.ListenTypes.ConfidenceData;
import com.qualcomm.listen.ListenTypes.DetectionData;
import com.qualcomm.listen.ListenTypes.EventData;
import com.qualcomm.listen.ListenTypes.KeywordUserCounts;
import com.qualcomm.listen.ListenTypes.SVASoundModelInfo;
import com.qualcomm.listen.ListenTypes.SoundModelInfo;
import com.qualcomm.listen.ListenTypes.VoiceWakeupDetectionData;
import com.qualcomm.listen.ListenTypes.VoiceWakeupDetectionDataV2;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class ListenSoundModel {
    private static final int SVA_SOUNDMODEL_TYPE = 1;
    private static final String TAG = "ListenSoundModel";
    private static final int UNKNOWN_SOUNDMODEL_TYPE = 0;
    private static final int VERSION_0100 = 256;
    private static final int VERSION_0200 = 512;
    private static final int VERSION_UNKNOWN = 0;

    public static native int createUdkSm(String str, String str2, int i, ShortBuffer[] shortBufferArr, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ConfidenceData confidenceData);

    public static native int deleteData(ByteBuffer byteBuffer, String str, String str2, ByteBuffer byteBuffer2);

    public static native int extend(ByteBuffer byteBuffer, String str, String str2, int i, ShortBuffer[] shortBufferArr, ByteBuffer byteBuffer2, ConfidenceData confidenceData);

    private static native int getInfo(ByteBuffer byteBuffer, SVASoundModelInfo sVASoundModelInfo);

    public static native int getSizeAfterDelete(ByteBuffer byteBuffer, String str, String str2);

    public static native int getSizeWhenExtended(ByteBuffer byteBuffer, String str, String str2);

    public static native int getSizeWhenMerged(ByteBuffer[] byteBufferArr);

    public static native int getTypeVersion(ByteBuffer byteBuffer, SoundModelInfo soundModelInfo);

    public static native int getUdkSmSize(String str, String str2, ShortBuffer[] shortBufferArr, ByteBuffer byteBuffer);

    public static native int merge(ByteBuffer[] byteBufferArr, ByteBuffer byteBuffer);

    private static native int parseVWUDetectionEventData(ByteBuffer byteBuffer, EventData eventData, VoiceWakeupDetectionData voiceWakeupDetectionData);

    private static native int parseVWUDetectionEventDataV2(ByteBuffer byteBuffer, EventData eventData, VoiceWakeupDetectionDataV2 voiceWakeupDetectionDataV2);

    public static native int verifyUdkRecording(ByteBuffer byteBuffer, ShortBuffer shortBuffer);

    public static native int verifyUserRecording(ByteBuffer byteBuffer, String str, ShortBuffer shortBuffer);

    static {
        Log.d(TAG, "Load liblistenjni");
        System.loadLibrary("listenjni");
    }

    @Deprecated
    public static int verifyUserRecording(ByteBuffer userIndependentModel, ShortBuffer userRecording) {
        return verifyUserRecording(userIndependentModel, null, userRecording);
    }

    @Deprecated
    public static int getSizeWhenExtended(ByteBuffer userIndependentModel) {
        return getSizeWhenExtended(userIndependentModel, null, null);
    }

    @Deprecated
    public static int extend(ByteBuffer userIndependentModel, int numUserRecordings, ShortBuffer[] userRecordings, ByteBuffer extendedSoundModel, ConfidenceData confidenceData) {
        return extend(userIndependentModel, null, null, numUserRecordings, userRecordings, extendedSoundModel, confidenceData);
    }

    public static DetectionData parseDetectionEventData(ByteBuffer registeredSoundModel, EventData eventPayload) {
        SoundModelInfo soundModelInfo = new SoundModelInfo();
        if (soundModelInfo == null) {
            Log.e(TAG, "parseDetectionEventData() new SoundModelInfo failed");
            return null;
        }
        int status = getTypeVersion(registeredSoundModel, soundModelInfo);
        if (status != 0) {
            Log.e(TAG, "parseDetectionEventData() get SM Info failed w/ " + status);
            return null;
        } else if (soundModelInfo.type != SVA_SOUNDMODEL_TYPE) {
            Log.e(TAG, "parseDetectionEventData() SM type " + soundModelInfo.type + " unsupported!");
            return null;
        } else {
            DetectionData detData;
            if (soundModelInfo.version == VERSION_0100) {
                Log.d(TAG, "SM type is SVA 1.0");
                DetectionData vwuDetData = new VoiceWakeupDetectionData();
                vwuDetData.status = parseVWUDetectionEventData(registeredSoundModel, eventPayload, (VoiceWakeupDetectionData) vwuDetData);
                vwuDetData.type = ListenTypes.VWU_EVENT_0100;
                detData = vwuDetData;
            } else {
                Log.d(TAG, "SM type is SVA 2.0");
                DetectionData vwuDetDataV2 = new VoiceWakeupDetectionDataV2();
                vwuDetDataV2.status = parseVWUDetectionEventDataV2(registeredSoundModel, eventPayload, (VoiceWakeupDetectionDataV2) vwuDetDataV2);
                vwuDetDataV2.type = ListenTypes.VWU_EVENT_0200;
                detData = vwuDetDataV2;
            }
            if (detData == null) {
                Log.e(TAG, "parseDetectionEventData() returns null ptr ");
            } else if (detData.status != 0) {
                Log.e(TAG, "parseDetectionEventData() returns status " + detData.status);
                detData = null;
            }
            return detData;
        }
    }

    public static SoundModelInfo query(ByteBuffer soundModel) {
        SoundModelInfo genSMInfo = new SoundModelInfo();
        SVASoundModelInfo soundModelInfo = new SVASoundModelInfo();
        Log.d(TAG, "query() getTypeVersion");
        int status = getTypeVersion(soundModel, genSMInfo);
        if (status != 0) {
            Log.e(TAG, "query() getTypeVersion failed, returned " + status);
            return null;
        }
        soundModelInfo.type = genSMInfo.type;
        soundModelInfo.version = genSMInfo.version;
        soundModelInfo.size = genSMInfo.size;
        if (soundModelInfo.type != SVA_SOUNDMODEL_TYPE) {
            Log.e(TAG, "query() SM type " + genSMInfo.type + " unsupported!");
            return null;
        } else if (genSMInfo.version == VERSION_0100) {
            Log.d(TAG, "query() only returns type and version for SVA 1.0 SoundModel");
            soundModelInfo.counts = new KeywordUserCounts();
            soundModelInfo.counts.numKeywords = (short) 1;
            soundModelInfo.counts.numUsers = (short) 0;
            soundModelInfo.counts.numUserKWPairs = (short) 1;
            soundModelInfo.keywordInfo = null;
            soundModelInfo.userNames = null;
            return soundModelInfo;
        } else {
            Log.d(TAG, "query() getInfoV2 called");
            status = getInfo(soundModel, soundModelInfo);
            if (status == 0) {
                return soundModelInfo;
            }
            Log.e(TAG, "query() getInfoV2 failed, returned " + status);
            return null;
        }
    }
}