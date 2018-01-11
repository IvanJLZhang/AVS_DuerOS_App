package com.wistron.demo.tool.teddybear.scene.voiceactivation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.qualcomm.listen.ListenTypes.KeywordInfo;
import com.qualcomm.listen.ListenTypes.SVASoundModelInfo;
import com.qualcomm.listen.ListenTypes.SoundModelInfo;
import com.qualcomm.listen.ListenTypes.VWUKeywordConfLevel;
import com.qualcomm.listen.ListenTypes.VWUUserKeywordPairConfLevel;
import com.wistron.demo.tool.teddybear.scene.voiceactivation.SoundModel.Setting;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class SoundModelRepository {
    private static final String FILEEXT_SOUNDMODEL_COM_KEYPHRASE = ".uimc";
    private static final String FILEEXT_SOUNDMODEL_COM_USER = ".udmc";
    private static final String FILEEXT_SOUNDMODEL_DEV_KEYPHRASE = ".uim";
    private static final String FILEEXT_SOUNDMODEL_DEV_USER = ".udm";
    private static final String TAG = "SoundModelRepository";
    private static final String TAG_DELIMITER = "|";
    private static final String TAG_PREFERENCE_SELECTEDKEYPHRASES = "com.qualcomm.qti.sva.selectedkeyphrases";
    private ByteBuffer extendedSoundModel;
    private String heySnapdragonKeyphraseName;
    private String heySnapdragonUdmcName;
    private String heySnapdragonUimcName;
    private ArrayList<String> loadedSmNames;
    private String mergedSoundModelName;
    private String niHaoXiaoLongKeyphraseName;
    private String niHaoXiaoLongUdmcName;
    private String niHaoXiaoLongUimcName;
    private String selectedSoundModelName;
    private String soundModelNameToDeleteFrom;
    private String soundModelNameToExtend;
    private String soundModelNameToQuery;
    private ByteBuffer soundModelToModifyByteBuffer;
    ArrayList<SoundModel> soundModels;
    ArrayList<ByteBuffer> soundModelsToMergeByteBuffers;
    private Keyphrase tempTrainingComKeyphrase;

    /* renamed from: com.qualcomm.qti.sva.SoundModelRepository.1 */
    class C00041 implements FilenameFilter {
        C00041() {
        }

        public boolean accept(File dir, String filename) {
            if (filename.endsWith(SoundModelRepository.FILEEXT_SOUNDMODEL_DEV_KEYPHRASE) || filename.endsWith(SoundModelRepository.FILEEXT_SOUNDMODEL_DEV_USER)) {
                return true;
            }
            return false;
        }
    }

    /* renamed from: com.qualcomm.qti.sva.SoundModelRepository.2 */
    class C00052 implements FilenameFilter {
        final /* synthetic */ boolean val$onlyKeyPhaseSoundModels;

        C00052(boolean z) {
            this.val$onlyKeyPhaseSoundModels = z;
        }

        public boolean accept(File dir, String filename) {
            if (this.val$onlyKeyPhaseSoundModels) {
                return filename.endsWith(SoundModelRepository.FILEEXT_SOUNDMODEL_DEV_KEYPHRASE);
            }
            return filename.endsWith(SoundModelRepository.FILEEXT_SOUNDMODEL_DEV_USER) || filename.endsWith(SoundModelRepository.FILEEXT_SOUNDMODEL_DEV_KEYPHRASE);
        }
    }

    /* renamed from: com.qualcomm.qti.sva.SoundModelRepository.3 */
    class C00063 implements FilenameFilter {
        C00063() {
        }

        public boolean accept(File dir, String filename) {
            if (!filename.endsWith(SoundModelRepository.FILEEXT_SOUNDMODEL_COM_KEYPHRASE) && !filename.endsWith(SoundModelRepository.FILEEXT_SOUNDMODEL_COM_USER)) {
                return false;
            }
            Log.v(SoundModelRepository.TAG, "getAllComKeyphrases: found keyphrase= " + filename);
            return true;
        }
    }

    /* renamed from: com.qualcomm.qti.sva.SoundModelRepository.4 */
    static /* synthetic */ class C00074 {
        static final /* synthetic */ int[] $SwitchMap$com$qualcomm$qti$sva$SoundModel$Setting;

        static {
            $SwitchMap$com$qualcomm$qti$sva$SoundModel$Setting = new int[Setting.values().length];
            try {
                $SwitchMap$com$qualcomm$qti$sva$SoundModel$Setting[Setting.Session.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$qualcomm$qti$sva$SoundModel$Setting[Setting.LaunchPreference.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$qualcomm$qti$sva$SoundModel$Setting[Setting.ConfidenceThreshold.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    SoundModelRepository() {
        this.soundModels = new ArrayList();
        this.selectedSoundModelName = null;
        this.soundModelNameToQuery = null;
        this.soundModelNameToExtend = null;
        this.soundModelNameToDeleteFrom = null;
        this.mergedSoundModelName = null;
        this.extendedSoundModel = null;
        this.soundModelToModifyByteBuffer = null;
        this.soundModelsToMergeByteBuffers = new ArrayList();
        this.loadedSmNames = null;
        this.tempTrainingComKeyphrase = null;
        this.heySnapdragonUimcName = "HeySnapdragon.uimc";
        this.heySnapdragonUdmcName = "HeySnapdragon.udmc";
        this.heySnapdragonKeyphraseName = "Hey Snapdragon";
        this.niHaoXiaoLongUimcName = "NiHaoXiaoLong.uimc";
        this.niHaoXiaoLongUdmcName = "NiHaoXiaoLong.udmc";
        this.niHaoXiaoLongKeyphraseName = "Ni Hao Xiao Long";
    }

    public ArrayList<SoundModel> getAllSoundModels() {
        Log.v(TAG, "getAllSoundModels");
        if (!this.soundModels.isEmpty()) {
            return this.soundModels;
        }
        File[] files = new File(SVAGlobal.PATH_APP).listFiles(new C00041());
        if (files == null) {
            Log.e(TAG, "getAllSoundModels: no sound models found");
            return null;
        }
        boolean foundSelectedSoundModel = false;
        for (File file : files) {
            String soundModelFileName = file.getName();
            if (soundModelFileName.contains(".vwu")) {
                Log.e(TAG, "getAllSoundModels: Error- v1.0 SM detected");
            } else if (this.soundModels.contains(new SoundModel(soundModelFileName))) {
                Log.v(TAG, "getAllSoundModels: duplicate sound model name: " + soundModelFileName + " skipping adding SM and continuing to next SM");
            } else {
                if (this.selectedSoundModelName != null && this.selectedSoundModelName.equals(soundModelFileName)) {
                    foundSelectedSoundModel = true;
                }
                Log.v(TAG, "getAllSoundModels: adding soundModelName= " + soundModelFileName);
                this.soundModels.add(new SoundModel(soundModelFileName));
            }
        }
        if (!foundSelectedSoundModel) {
            this.selectedSoundModelName = null;
        }
        Collections.sort(this.soundModels);
        return this.soundModels;
    }

    public ArrayList<String> getSoundModelNames(boolean onlyKeyPhaseSoundModels) {
        Log.v(TAG, "getSoundModelNames");
        File[] files = new File(SVAGlobal.PATH_APP).listFiles(new C00052(onlyKeyPhaseSoundModels));
        if (files == null) {
            Log.e(TAG, "getSoundModelNames: no sound models found");
            return null;
        }
        ArrayList<String> soundModelArrayList = new ArrayList();
        String keyPhraseName = "";
        for (File name : files) {
            String soundModelFileName = name.getName();
            if (onlyKeyPhaseSoundModels) {
                keyPhraseName = soundModelFileName.substring(0, soundModelFileName.lastIndexOf(46));
                Log.v(TAG, "getSoundModelNames: keyPhraseName= " + keyPhraseName);
                if (soundModelArrayList.contains(keyPhraseName)) {
                    Log.e(TAG, "getSoundModelNames: keyPhraseName= " + keyPhraseName + " is a duplicate.");
                } else {
                    soundModelArrayList.add(keyPhraseName);
                }
            } else if (soundModelArrayList.contains(soundModelFileName)) {
                Log.e(TAG, "getSoundModelNames: soundModelFileName= " + soundModelFileName + " is a duplicate.");
            } else {
                soundModelArrayList.add(soundModelFileName);
            }
        }
        return soundModelArrayList;
    }

    public String getSmPrettyName(String uglyName) {
        return uglyName.substring(0, uglyName.indexOf("."));
    }

    public String[] getAllUsers() {
        Log.v(TAG, "getAllUsers");
        ArrayList<String> userArrayList = new ArrayList();
        Iterator it = this.soundModels.iterator();
        while (it.hasNext()) {
            Iterator it2 = ((SoundModel) it.next()).getKeyphrases().iterator();
            while (it2.hasNext()) {
                Iterator i$ = ((Keyphrase) it2.next()).getUsers().iterator();
                while (i$.hasNext()) {
                    User user = (User) i$.next();
                    if (!userArrayList.contains(user.name)) {
                        userArrayList.add(user.name);
                    }
                }
            }
        }
        Collections.sort(userArrayList);
        return (String[]) userArrayList.toArray(new String[userArrayList.size()]);
    }

    public VWUKeywordConfLevel[] getKeyphraseConfidenceThresholdsBySmName(Context context, String smName) {
        Log.v(TAG, "getKeyphraseConfidenceThresholdsBySmName: smName= " + smName);
        SoundModel selectedSoundModel = getSmByName(smName);
        if (selectedSoundModel == null) {
            Log.e(TAG, "getKeyphraseConfidenceThresholdsBySmName: cannot get SM for smName= " + smName);
            return null;
        }
        ArrayList<VWUKeywordConfLevel> keywordConfLevels = new ArrayList();
        Iterator i$ = selectedSoundModel.getKeyphrases().iterator();
        while (i$.hasNext()) {
            Keyphrase keyword = (Keyphrase) i$.next();
            VWUKeywordConfLevel keywordConfLevel = new VWUKeywordConfLevel();
            keywordConfLevel.keyword = keyword.getName();
            keywordConfLevel.confLevel = (short) getConfidenceLevel(context, selectedSoundModel.getName(), keyword.getName(), null);
            Log.v(TAG, "getKeyphraseConfidenceThresholdsBySmName: smName= " + smName + ", keyword= " + keyword.getName() + ", confLevel= " + keywordConfLevel.confLevel);
            keywordConfLevels.add(keywordConfLevel);
        }
        return (VWUKeywordConfLevel[]) keywordConfLevels.toArray(new VWUKeywordConfLevel[keywordConfLevels.size()]);
    }

    public VWUUserKeywordPairConfLevel[] getUserConfidenceThresholdsBySmName(Context context, String smName) {
        Log.v(TAG, "getUserConfidenceThresholdsBySmName: smName= " + smName);
        SoundModel selectedSoundModel = getSmByName(smName);
        ArrayList<VWUUserKeywordPairConfLevel> userConfLevels = new ArrayList();
        Iterator it = selectedSoundModel.getKeyphrases().iterator();
        while (it.hasNext()) {
            Keyphrase keyword = (Keyphrase) it.next();
            Iterator i$ = keyword.getUsers().iterator();
            while (i$.hasNext()) {
                User user = (User) i$.next();
                VWUUserKeywordPairConfLevel userConfLevel = new VWUUserKeywordPairConfLevel();
                userConfLevel.keyword = keyword.getName();
                userConfLevel.user = user.getName();
                userConfLevel.confLevel = (short) getConfidenceLevel(context, selectedSoundModel.getName(), keyword.getName(), user.getName());
                Log.v(TAG, "getKeyphraseConfidenceThresholdsBySmName: smName= " + smName + ", keyword= " + keyword.getName() + ", userName= " + user.getName() + ", confLevel= " + userConfLevel.confLevel);
                userConfLevels.add(userConfLevel);
            }
        }
        return (VWUUserKeywordPairConfLevel[]) userConfLevels.toArray(new VWUUserKeywordPairConfLevel[userConfLevels.size()]);
    }

    private boolean soundModelFileExists(String inSoundModelName) {
        return new File(generateSoundModelFilePath(inSoundModelName)).exists();
    }

    public String generateSoundModelFilePath(String inSoundModelName) {
        Log.v(TAG, "generateSoundModelFilePath: path= " + SVAGlobal.PATH_APP + "/" + inSoundModelName);
        return SVAGlobal.PATH_APP + "/" + inSoundModelName;
    }

    public boolean isKeyphraseSoundModel(String inSoundModelName) {
        return inSoundModelName.contains(FILEEXT_SOUNDMODEL_DEV_KEYPHRASE);
    }

    public boolean soundModelExists(String soundModelName) {
        return soundModelFileExists(soundModelName);
    }

    public String getUserSoundModelName(String soundModelName) {
        return soundModelName + FILEEXT_SOUNDMODEL_DEV_USER;
    }

    public String getSystemSoundModelName(String soundModelName) {
        return soundModelName + FILEEXT_SOUNDMODEL_DEV_KEYPHRASE;
    }

    public String getSoundModelNameFromLongName(String soundModelName) {
        return soundModelName.substring(0, soundModelName.lastIndexOf(46));
    }

    public boolean createNewSoundModel(ArrayList<String> baseSoundModelNames, String newSoundModelName) {
        if (baseSoundModelNames.size() == 0 || newSoundModelName == null) {
            Log.e(TAG, "createNewSoundModel: bad params");
        }
        if (1 == baseSoundModelNames.size()) {
            Utils.createNewSoundModel(SVAGlobal.PATH_APP + "/" + ((String) baseSoundModelNames.get(0)), SVAGlobal.PATH_APP + "/" + newSoundModelName);
            this.soundModels.add(new SoundModel(newSoundModelName));
            return false;
        }
        this.soundModelsToMergeByteBuffers.clear();
        Iterator i$ = baseSoundModelNames.iterator();
        while (i$.hasNext()) {
            String soundModelToAddName = (String) i$.next();
            if (soundModelExists(soundModelToAddName)) {
                this.soundModelsToMergeByteBuffers.add(getSoundModelByteBufferFromName(soundModelToAddName));
            } else {
                Log.e(TAG, "createNewSoundModel: sound model: " + soundModelToAddName + " does not exist");
            }
        }
        setMergedSoundModelName(newSoundModelName);
        return true;
    }

    public ArrayList<ByteBuffer> getSoundModelsToMergeByteBuffers() {
        return this.soundModelsToMergeByteBuffers;
    }

    public void setSoundModelNameToQuery(String inSoundModelName) {
        this.soundModelNameToQuery = inSoundModelName;
        Log.v(TAG, "setSoundModelNameToQuery: sound model name= " + this.soundModelNameToQuery);
    }

    public ByteBuffer getSoundModelByteBufferToQuery() {
        return getSoundModelByteBufferFromName(this.soundModelNameToQuery);
    }

    public SoundModel getQueriedSoundModel() {
        if (this.soundModelNameToQuery == null) {
            return null;
        }
        return getSmByName(this.soundModelNameToQuery);
    }

    public SoundModel getSmByName(String inSoundModelName) {
        if (inSoundModelName == null) {
            Log.e(TAG, "getSoundModelByName: SM passed in is null");
            return null;
        } else if (inSoundModelName.equals(this.heySnapdragonUdmcName)) {
            return getComSoundModelFromKeyphrase(this.heySnapdragonKeyphraseName);
        } else {
            if (inSoundModelName.equals(this.niHaoXiaoLongUdmcName)) {
                return getComSoundModelFromKeyphrase(this.niHaoXiaoLongKeyphraseName);
            }
            Iterator i$ = this.soundModels.iterator();
            while (i$.hasNext()) {
                SoundModel soundModel = (SoundModel) i$.next();
                if (soundModel.equals(new SoundModel(inSoundModelName))) {
                    return soundModel;
                }
            }
            Log.e(TAG, "getSoundModelByName: SM= " + inSoundModelName + " does not exist");
            return null;
        }
    }

    public void addSmInfoFromQuery(SoundModelInfo smInfo, ArrayList<String> userKeyphrasePairIndices) {
        Log.v(TAG, "addSmInfoFromQuery: Adding info for SM: " + this.soundModelNameToQuery);
        KeywordInfo[] kwInfos = ((SVASoundModelInfo) smInfo).keywordInfo;
        boolean fillIndices = true;
        if (userKeyphrasePairIndices == null) {
            fillIndices = false;
        }
        SoundModel soundModelToAdd = new SoundModel(this.soundModelNameToQuery);
        Log.v(TAG, "addSmInfoFromQuery: Adding SM: " + this.soundModelNameToQuery + " with: " + kwInfos.length + " keywords.");
        int keyphraseIndex = -2;
        for (KeywordInfo kwInfo : kwInfos) {
            Keyphrase keyphraseToAdd;
            if (fillIndices) {
                keyphraseIndex = userKeyphrasePairIndices.indexOf(kwInfo.keywordPhrase);
                Log.v(TAG, "addSmInfoFromQuery: found keyphraseIndex= " + keyphraseIndex);
                if (keyphraseIndex == -1) {
                    Log.e(TAG, "addSmInfoFromQuery: userKeyphrasePairIndices doesn't contain kwInfo.keywordPhrase= " + kwInfo.keywordPhrase);
                }
                keyphraseToAdd = new Keyphrase(kwInfo.keywordPhrase, keyphraseIndex);
            } else {
                keyphraseToAdd = new Keyphrase(kwInfo.keywordPhrase, -3);
            }
            Log.v(TAG, "addSmInfoFromQuery: Building SM: " + this.soundModelNameToQuery + " adding keyphrase: " + kwInfo.keywordPhrase + " with ID= " + keyphraseIndex);
            for (String userName : kwInfo.activeUsers) {
                if (fillIndices) {
                    int userIndex = userKeyphrasePairIndices.indexOf(new StringBuilder().append(userName).append(TAG_DELIMITER).append(kwInfo.keywordPhrase).toString());
                    Log.v(TAG, new StringBuilder().append("addSmInfoFromQuery: found userIndex= ").append(userIndex).append(" for ").append("user|keyphrase= ").append(userName).append(TAG_DELIMITER).append(kwInfo.keywordPhrase).toString());
                    if (userIndex == -1) {
                        Log.e(TAG, new StringBuilder().append("addSmInfoFromQuery: userKeyphrasePairIndices doesn't contain user|keyphrase= ").append(userName).append(TAG_DELIMITER).append(kwInfo.keywordPhrase).toString());
                    }
                    keyphraseToAdd.addUser(userName, userIndex);
                } else {
                    keyphraseToAdd.addUser(userName, -3);
                }
                Log.v(TAG, "addSmInfoFromQuery: Building SM: " + this.soundModelNameToQuery + " building keyword: " + kwInfo.keywordPhrase + " adding user: " + userName);
            }
            soundModelToAdd.addKeyphrase(keyphraseToAdd);
        }
        if (this.soundModels.contains(new SoundModel(this.soundModelNameToQuery))) {
            Log.v(TAG, "addSmInfoFromQuery: SM: " + this.soundModelNameToQuery + " already exists, writing over exiting");
            Log.v(TAG, "addSmInfoFromQuery: printSms()= " + printSms());
            Iterator i$ = this.soundModels.iterator();
            while (i$.hasNext()) {
                SoundModel soundModel = (SoundModel) i$.next();
                Log.v(TAG, "addSmInfoFromQuery: testing SM: " + soundModel.getName());
                if (soundModel.equals(soundModelToAdd)) {
                    Log.v(TAG, "addSmInfoFromQuery: found SM: " + soundModel.getName());
                    Log.v(TAG, "addSmInfoFromQuery: " + printSms());
                    if (this.soundModels.remove(soundModel)) {
                        Log.v(TAG, "addSmInfoFromQuery: removed SM: " + soundModel.getName());
                        this.soundModels.add(soundModelToAdd);
                        Collections.sort(this.soundModels);
                        Log.v(TAG, "addSmInfoFromQuery: " + printSms());
                        return;
                    }
                    Log.e(TAG, "addSmInfoFromQuery: could not be remove SM: " + soundModel.getName());
                    return;
                }
            }
            return;
        }
        this.soundModels.add(soundModelToAdd);
        Log.v(TAG, "addSmInfoFromQuery: Added SM: " + this.soundModelNameToQuery);
    }

    public void setLoadedSmNames(ArrayList<String> inLoadedSmNames) {
        this.loadedSmNames = inLoadedSmNames;
        for (int i = 0; i < this.loadedSmNames.size(); i++) {
            Log.v(TAG, "setLoadedSmNames: loadedSmNames.get(" + i + ")= " + ((String) this.loadedSmNames.get(i)));
        }
    }

    public ArrayList<String> getLoadedSmNames() {
        return this.loadedSmNames;
    }

    public void toggleLaunchPreference(Context context, String inSoundModelName, String inKeywordName, String inUserName) {
        if (inSoundModelName == null) {
            Log.e(TAG, "Could not get launch preference because sound model name was null");
        } else if (getSmByName(inSoundModelName) == null) {
            Log.e(TAG, "Could not get launch preference because sound model does not exist");
        } else {
            String launchPreferenceTag = generateSettingsTag(Setting.LaunchPreference, inSoundModelName, inKeywordName, inUserName, null);
            SharedPreferences sp = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0);
            boolean launchPreference = sp.getBoolean(launchPreferenceTag, false);
            Log.v(TAG, "launchPreference for SM: " + inSoundModelName + ":" + inKeywordName + ":" + inUserName + " was: " + launchPreference);
            Editor editor = sp.edit();
            editor.putBoolean(launchPreferenceTag, !launchPreference);
            editor.commit();
        }
    }

    public boolean getLaunchPreference(Context context, String inSoundModelName, String inKeywordName, String inUserName) {
        if (inSoundModelName == null) {
            Log.e(TAG, "Could not get launch preference because sound model name was null");
            return false;
        } else if (getSmByName(inSoundModelName) == null) {
            Log.e(TAG, "Could not get launch preference because sound model does not exist");
            return false;
        } else {
            boolean launchPreference = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0).getBoolean(generateSettingsTag(Setting.LaunchPreference, inSoundModelName, inKeywordName, inUserName, null), false);
            Log.v(TAG, "launchPreference for SM: " + inSoundModelName + ":" + inKeywordName + ":" + inUserName + "=" + launchPreference);
            return launchPreference;
        }
    }

    public void setConfidenceLevel(Context context, String inSoundModelName, String inKeywordName, String inUserName, int inConfidenceLevel) {
        String confidenceLevelTag = generateSettingsTag(Setting.ConfidenceThreshold, inSoundModelName, inKeywordName, inUserName, null);
        Editor editor = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0).edit();
        editor.putInt(confidenceLevelTag, inConfidenceLevel);
        editor.commit();
    }

    public int getConfidenceLevel(Context context, String inSoundModelName, String inKeyphraseName, String inUserName) {
        int i = -1;
        if (inSoundModelName == null) {
            Log.e(TAG, "Could not get confidence level because sound model name was null");
        } else if (getSmByName(inSoundModelName) == null) {
            Log.e(TAG, "Could not get confidence level because sound model does not exist");
        } else {
            String confidenceLevelTag = generateSettingsTag(Setting.ConfidenceThreshold, inSoundModelName, inKeyphraseName, inUserName, null);
            SharedPreferences sp = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0);
            if (inUserName == null) {
                i = sp.getInt(confidenceLevelTag, SVAGlobal.getInstance().getSettingKeyPhraseConfidenceLevel());
            } else {
                i = sp.getInt(confidenceLevelTag, SVAGlobal.getInstance().getSettingUserConfidenceLevel());
            }
            Log.v(TAG, "getConfidenceLevel: confidenceLevel for SM: " + inSoundModelName + ":" + inKeyphraseName + ":" + inUserName + "=" + i);
        }
        return i;
    }

    private String generateSettingsTag(Setting setting, String inSoundModelName, String inKeywordName, String inUserName, Integer inSessionNum) {
        String specificSetting = "";
        switch (C00074.$SwitchMap$com$qualcomm$qti$sva$SoundModel$Setting[setting.ordinal()]) {
            case 1 /*1*/:
                return "Settings|" + "session" + TAG_DELIMITER + inSessionNum;
            case 2 /*2*/:
                specificSetting = "launch";
                break;
            case 3 /*3*/:
                specificSetting = "conf";
                break;
        }
        return "Settings|" + specificSetting + TAG_DELIMITER + inSoundModelName + TAG_DELIMITER + inKeywordName + TAG_DELIMITER + inUserName;
    }

    public void setSoundModelNameToExtend(String inSoundModelName) {
        this.soundModelNameToExtend = inSoundModelName;
    }

    public ByteBuffer getSoundModelByteBufferToExtend() {
        return getSoundModelByteBufferFromName(this.soundModelNameToExtend);
    }

    public void createExtendedSoundModelByteBuffer(int size) {
        this.extendedSoundModel = ByteBuffer.allocate(size);
    }

    public ByteBuffer getExtendedSoundModel() {
        if (this.extendedSoundModel == null) {
            Log.v(TAG, "getExtendedSoundModel: must call setExtendedSoundModel before get");
        }
        return this.extendedSoundModel;
    }

    public void saveExtendedSoundModel(Context context) {
        String filePath;
        Log.v(TAG, "saveExtendedSoundModel");
        boolean isComTraining = false;
        if (this.tempTrainingComKeyphrase != null) {
            isComTraining = true;
            filePath = generateComSoundModelFilePath(this.tempTrainingComKeyphrase.getName());
        } else {
            filePath = generateSoundModelFilePath(this.soundModelNameToExtend);
        }
        Log.v(TAG, "saveExtendedSoundModel: filePath= " + filePath);
        Utils.saveByteBufferToFile(this.extendedSoundModel, filePath);
        if (isComTraining) {
            if (!this.tempTrainingComKeyphrase.getIsUdk()) {
                getComSoundModelFromKeyphrase(this.tempTrainingComKeyphrase.getName()).setIsTrained();
            }
            String soundModelName = generateComSoundModelNameFromFilePath(filePath);
            Log.v(TAG, "saveExtendedSoundModel for SmName= " + soundModelName + ", keyphraseName= " + this.tempTrainingComKeyphrase.getName() + ", keyphraseAction= " + this.tempTrainingComKeyphrase.getActionName() + ", keyphraseActionIntent= " + this.tempTrainingComKeyphrase.getActionIntent());
            SoundModel sm = new SoundModel(soundModelName);
            sm.addKeyphrase(new Keyphrase(this.tempTrainingComKeyphrase.getName(), this.tempTrainingComKeyphrase.getActionName(), this.tempTrainingComKeyphrase.getActionIntent(), true));
            this.soundModels.add(sm);
            saveCsmToSharedPrefs(context, soundModelName, this.tempTrainingComKeyphrase.getName(), this.tempTrainingComKeyphrase.getActionName(), this.tempTrainingComKeyphrase.getActionIntent(), true);
            this.tempTrainingComKeyphrase = null;
        }
    }

    public boolean deleteSoundModel(String inSoundModelName) {
        if (inSoundModelName == null) {
            Log.e(TAG, "deleteSoundModel: Sound model with name: " + inSoundModelName + " does not exist.");
            return false;
        }
        String filePath = generateSoundModelFilePath(inSoundModelName);
        Log.v(TAG, "deleteSoundModel: filePath= " + filePath);
        File soundModelFile = new File(filePath);
        if (!soundModelFile.exists()) {
            Log.e(TAG, "deleteSoundModel: sound model does not exist");
            return false;
        } else if (soundModelFile.delete()) {
            Log.v(TAG, "deleteSoundModel: succeeded");
            return true;
        } else {
            Log.e(TAG, "deleteSoundModel: delete failed");
            return false;
        }
    }

    public void setSoundModelNameToDeleteFrom(String inSoundModelName) {
        this.soundModelNameToDeleteFrom = inSoundModelName;
    }

    public ByteBuffer getSoundModelByteBufferToDeleteFrom() {
        return getSoundModelByteBufferFromName(this.soundModelNameToDeleteFrom);
    }

    public void createSoundModelToModifyByteBuffer(int size) {
        this.soundModelToModifyByteBuffer = ByteBuffer.allocate(size);
    }

    public ByteBuffer getSoundModelToModifyByteBuffer() {
        if (this.soundModelToModifyByteBuffer == null) {
            Log.v(TAG, "getExtendedSoundModel: must call setExtendedSoundModel before get");
        }
        return this.soundModelToModifyByteBuffer;
    }

    public void saveSoundModelToDeleteFrom() {
        Log.v(TAG, "saveSoundModelToDeleteFrom");
        String filePath = generateSoundModelFilePath(this.soundModelNameToDeleteFrom);
        Log.v(TAG, "outputSoundModelToDeleteFrom: filePath= " + filePath);
        Utils.saveByteBufferToFile(this.soundModelToModifyByteBuffer, filePath);
    }

    public void setMergedSoundModelName(String inSoundModelName) {
        this.mergedSoundModelName = inSoundModelName;
    }

    public String getMergedSoundModelName() {
        return this.mergedSoundModelName;
    }

    public void saveMergedSoundModel() {
        Log.v(TAG, "saveMergedSoundModel");
        String filePath = generateSoundModelFilePath(this.mergedSoundModelName);
        Log.v(TAG, "saveMergedSoundModel: filePath= " + filePath);
        Utils.saveByteBufferToFile(this.soundModelToModifyByteBuffer, filePath);
    }

    public ByteBuffer getSoundModelByteBufferFromName(String inSoundModelName) {
        ByteBuffer byteBuffer = null;
        if (soundModelFileExists(inSoundModelName)) {
            byteBuffer = Utils.readFileToByteBuffer(generateSoundModelFilePath(inSoundModelName));
        } else {
            Log.v(TAG, "getSoundModelFromName: sound model with name: " + inSoundModelName + " does not exist");
        }
        return byteBuffer;
    }

    private String printSms() {
        StringBuilder smsToText = new StringBuilder();
        smsToText.append("\n");
        Iterator it = this.soundModels.iterator();
        while (it.hasNext()) {
            SoundModel soundModel = (SoundModel) it.next();
            smsToText.append("\t" + soundModel.getName() + "\n");
            Iterator it2 = soundModel.getKeyphrases().iterator();
            while (it2.hasNext()) {
                Keyphrase keyphrase = (Keyphrase) it2.next();
                smsToText.append("\t\t" + keyphrase.getName() + "\n");
                Iterator i$ = keyphrase.getUsers().iterator();
                while (i$.hasNext()) {
                    smsToText.append("\t\t\t" + ((User) i$.next()).getName() + "\n");
                }
            }
        }
        return smsToText.toString();
    }

    public String findKeyphraseOrUserById(String soundModelName, int keyphraseOrUserId) {
        SoundModel soundModel = getSmByName(soundModelName);
        if (soundModel == null) {
            Log.e(TAG, "findKeyphraseOrUserById: Could not find soundModel with name= " + soundModelName);
            return null;
        }
        Iterator it = soundModel.getKeyphrases().iterator();
        while (it.hasNext()) {
            Keyphrase keyphrase = (Keyphrase) it.next();
            if (keyphraseOrUserId == keyphrase.id) {
                return keyphrase.name;
            }
            Iterator i$ = keyphrase.getUsers().iterator();
            while (i$.hasNext()) {
                User user = (User) i$.next();
                if (keyphraseOrUserId == user.id) {
                    return user.name;
                }
            }
        }
        Log.e(TAG, "findKeyphraseOrUserById: Could not find keyphrase or user with id= " + keyphraseOrUserId);
        return null;
    }

    public LinkedHashSet<Keyphrase> getAllComKeyphrases(Context context) {
        Log.v(TAG, "getAllComKeyphrases");
        File dir = new File(SVAGlobal.PATH_APP);
        Log.v(TAG, "getAllComKeyphrases: dir created.");
        File[] files = dir.listFiles(new C00063());
        if (files == null) {
            Log.e(TAG, "getAllComKeyphrases: no sound models found");
            return null;
        }
        ArrayList<String> pdkSmFileNames = new ArrayList();
        for (File file : files) {
            String soundModelFileName = file.getName();
            if (isUserIndependentPdkCsm(soundModelFileName)) {
                pdkSmFileNames.add(soundModelFileName);
            }
            if (this.soundModels.contains(new SoundModel(soundModelFileName))) {
                Log.v(TAG, "getAllComKeyphrases: duplicate sound model name: " + soundModelFileName + " skipping adding SM and continuing to next SM");
            } else {
                Log.v(TAG, "getAllComKeyphrases: adding soundModelName= " + soundModelFileName);
                SoundModel sm = new SoundModel(soundModelFileName);
                sm.addKeyphrase(getKeyphraseFromSharedPrefs(context, soundModelFileName));
                this.soundModels.add(sm);
            }
        }
        ArrayList<String> smNamesToSetTrained = new ArrayList();
        Iterator it = pdkSmFileNames.iterator();
        SoundModel sm;
        while (it.hasNext()) {
            String smName = (String) it.next();
            String nameToMatch = smName.substring(0, smName.lastIndexOf(46)) + FILEEXT_SOUNDMODEL_COM_USER;
            Log.v(TAG, "getAllComKeyphrases: nameToRemove= " + nameToMatch);
            Iterator i$ = this.soundModels.iterator();
            while (i$.hasNext()) {
                sm = (SoundModel) i$.next();
                if (sm.getName().equals(nameToMatch)) {
                    this.soundModels.remove(sm);
                    Log.v(TAG, "getAllComKeyphrases: removed sm.getName()= " + sm.getName());
                    smNamesToSetTrained.add(smName);
                    break;
                }
            }
        }
        Collections.sort(this.soundModels);
        LinkedHashSet<Keyphrase> keyphrases = new LinkedHashSet();
        it = this.soundModels.iterator();
        while (it.hasNext()) {
            sm = (SoundModel) it.next();
            Log.v(TAG, "getAllComKeyphrases: keyphraseName= " + ((Keyphrase) sm.getKeyphrases().get(0)).getName() + ", keyphraseActionName= " + ((Keyphrase) sm.getKeyphrases().get(0)).getActionName());
            if (smNamesToSetTrained.contains(sm.getName())) {
                sm.setIsTrained();
            }
            keyphrases.add(sm.getKeyphrases().get(0));
        }
        return keyphrases;
    }

    private Keyphrase getKeyphraseFromSharedPrefs(Context context, String soundModelName) {
        String[] keyphraseStrings;
        Intent keyphraseIntent;
        SharedPreferences sharedPrefs = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0);
        String keyphraseString = sharedPrefs.getString(soundModelName, null);
        if (keyphraseString == null) {
            if (soundModelName.equals(this.heySnapdragonUimcName)) {
                saveCsmToSharedPrefs(context, soundModelName, this.heySnapdragonKeyphraseName, "None", null, false);
                keyphraseString = sharedPrefs.getString(soundModelName, null);
                if (keyphraseString != null) {
                    Log.e(TAG, "getKeyphraseFromSharedPrefs: unknown SM with name= " + soundModelName);
                    return null;
                }
                Log.v(TAG, "getKeyphraseFromSharedPrefs: keyphraseString= " + keyphraseString);
                keyphraseStrings = keyphraseString.split("\\|");
                Log.v(TAG, "getKeyphraseFromSharedPrefs: keyphraseStrings= " + Arrays.toString(keyphraseStrings));
                String keyphraseName = keyphraseStrings[0];
                keyphraseIntent = null;
                try {
                    keyphraseIntent = Intent.parseUri(keyphraseStrings[2], 0);
                } catch (URISyntaxException e) {
                    Log.e(TAG, "getKeyphraseFromSharedPrefs: Error parsing intent URI");
                    e.printStackTrace();
                }
                Keyphrase keyphrase = new Keyphrase(keyphraseName, keyphraseStrings[1], keyphraseIntent, keyphraseStrings[3].equals("true"));
                Log.v(TAG, "getKeyphraseFromSharedPrefs: created keyphraseName= " + keyphrase.getName() + ", keyphraseAction=" + keyphrase.getActionName() + ", keyphraseActionIntent= " + keyphrase.getActionIntent() + ", userVerificationEnabled= " + keyphrase.getIsUserVerificationEnabled());
                return keyphrase;
            }
        }
        if (keyphraseString == null) {
            if (soundModelName.equals(this.niHaoXiaoLongUimcName)) {
                saveCsmToSharedPrefs(context, soundModelName, this.niHaoXiaoLongKeyphraseName, "None", null, false);
                keyphraseString = sharedPrefs.getString(soundModelName, null);
            }
        }
        if (keyphraseString != null) {
            Log.v(TAG, "getKeyphraseFromSharedPrefs: keyphraseString= " + keyphraseString);
            keyphraseStrings = keyphraseString.split("\\|");
            Log.v(TAG, "getKeyphraseFromSharedPrefs: keyphraseStrings= " + Arrays.toString(keyphraseStrings));
            String keyphraseName2 = keyphraseStrings[0];
            keyphraseIntent = null;
            try {
                keyphraseIntent = Intent.parseUri(keyphraseStrings[2], 0);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (keyphraseStrings[3].equals("true")) {
            }
            Keyphrase keyphrase2 = new Keyphrase(keyphraseName2, keyphraseStrings[1], keyphraseIntent, keyphraseStrings[3].equals("true"));
            Log.v(TAG, "getKeyphraseFromSharedPrefs: created keyphraseName= " + keyphrase2.getName() + ", keyphraseAction=" + keyphrase2.getActionName() + ", keyphraseActionIntent= " + keyphrase2.getActionIntent() + ", userVerificationEnabled= " + keyphrase2.getIsUserVerificationEnabled());
            return keyphrase2;
        }
        Log.e(TAG, "getKeyphraseFromSharedPrefs: unknown SM with name= " + soundModelName);
        return null;
    }

    private void saveCsmToSharedPrefs(Context context, String smName, String keyphraseName, String actionName, Intent actionIntent, boolean isUserVerificationEnabled) {
        String intentStringToSave;
        if (actionIntent == null) {
            intentStringToSave = null;
        } else {
            intentStringToSave = actionIntent.toUri(0);
        }
        String value = keyphraseName + TAG_DELIMITER + actionName + TAG_DELIMITER + intentStringToSave + TAG_DELIMITER + isUserVerificationEnabled;
        Log.v(TAG, "saveCsmToSharedPrefs: saving key= " + smName + ", value= " + value);
        Editor editor = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0).edit();
        editor.putString(smName, value);
        editor.commit();
    }

    public void setKeyphraseAction(Context context, String keyphraseName, String actionName, Intent actionIntent) {
        Log.v(TAG, "setKeyphraseAction: keyphraseName= " + keyphraseName + ", actionName= " + actionName + ", actionIntent= " + actionIntent);
        Iterator i$ = this.soundModels.iterator();
        while (i$.hasNext()) {
            SoundModel sm = (SoundModel) i$.next();
            if (((Keyphrase) sm.getKeyphrases().get(0)).getName().equals(keyphraseName)) {
                Log.v(TAG, "setKeyphraseAction: sm.getKeyphrases().get(0).getName()= " + ((Keyphrase) sm.getKeyphrases().get(0)).getName());
                ((Keyphrase) sm.getKeyphrases().get(0)).setActionName(actionName);
                ((Keyphrase) sm.getKeyphrases().get(0)).setActionIntent(actionIntent);
                Keyphrase tempKeyphrase = (Keyphrase) sm.getKeyphrases().get(0);
                saveCsmToSharedPrefs(context, sm.getName(), tempKeyphrase.getName(), tempKeyphrase.getActionName(), tempKeyphrase.getActionIntent(), tempKeyphrase.getIsUserVerificationEnabled());
                return;
            }
        }
    }

    public String getKeyphraseActionName(String keyphraseName) {
        Log.v(TAG, "getKeyphraseActionName: keyphraseName= " + keyphraseName);
        Iterator i$ = this.soundModels.iterator();
        while (i$.hasNext()) {
            SoundModel sm = (SoundModel) i$.next();
            Log.v(TAG, "getKeyphraseActionName: sm.getKeyphrases().get(0).getName()= " + ((Keyphrase) sm.getKeyphrases().get(0)).getName());
            if (((Keyphrase) sm.getKeyphrases().get(0)).getName().equals(keyphraseName)) {
                String actionName = ((Keyphrase) sm.getKeyphrases().get(0)).getActionName();
                Log.v(TAG, "getKeyphraseActionName: for keyphrase= " + keyphraseName + " found " + "actionName= " + actionName);
                return actionName;
            }
        }
        return null;
    }

    public Intent getKeyphraseActionIntent(String keyphraseName) {
        Log.v(TAG, "getKeyphraseActionIntent: keyphraseName= " + keyphraseName);
        Iterator i$ = this.soundModels.iterator();
        while (i$.hasNext()) {
            SoundModel sm = (SoundModel) i$.next();
            Log.v(TAG, "getKeyphraseActionIntent: sm.getKeyphrases().get(0).getName()= " + ((Keyphrase) sm.getKeyphrases().get(0)).getName());
            if (((Keyphrase) sm.getKeyphrases().get(0)).getName().equals(keyphraseName)) {
                Intent actionIntent = ((Keyphrase) sm.getKeyphrases().get(0)).getActionIntent();
                Log.v(TAG, "getKeyphraseActionIntent: for keyphrase= " + keyphraseName + " found " + "actionName= " + actionIntent);
                return actionIntent;
            }
        }
        return null;
    }

    public Boolean getIsUserVerificationEnabledForCsm(String smNameToMatch) {
        return Boolean.valueOf(!isUserIndependentPdkCsm(smNameToMatch));
    }

    public Boolean getIsUserVerificationEnabledForKeyphrase(String keyphraseToMatch) {
        return Boolean.valueOf(((Keyphrase) getComSoundModelFromKeyphrase(keyphraseToMatch).getKeyphrases().get(0)).getIsUserVerificationEnabled());
    }

    public boolean isCSM(String smNameToMatch) {
        String smExtn = smNameToMatch.substring(smNameToMatch.lastIndexOf(46));
        if (smExtn.equals(FILEEXT_SOUNDMODEL_COM_KEYPHRASE) || smExtn.equals(FILEEXT_SOUNDMODEL_COM_USER)) {
            return true;
        }
        return false;
    }

    public boolean isUserIndependentPdkCsm(String smNameToMatch) {
        return smNameToMatch.substring(smNameToMatch.lastIndexOf(46)).equals(FILEEXT_SOUNDMODEL_COM_KEYPHRASE);
    }

    public Boolean isComUdk(String inKeyphraseName) {
        String smNameToCheck = getComSoundModelNameFromKeyphrase(inKeyphraseName);
        if (smNameToCheck != null) {
            return Boolean.valueOf(smNameToCheck.substring(smNameToCheck.lastIndexOf(46)).equals(FILEEXT_SOUNDMODEL_COM_USER));
        }
        Log.e(TAG, "isComUdk: getComSoundModelNameFromKeyphrase cannot find SM for keyphrase= " + inKeyphraseName);
        return null;
    }

    public SoundModel getComSoundModelFromKeyphrase(String keyphraseNameToMatch) {
        Iterator i$ = this.soundModels.iterator();
        while (i$.hasNext()) {
            SoundModel sm = (SoundModel) i$.next();
            if (((Keyphrase) sm.getKeyphrases().get(0)).getName().equals(keyphraseNameToMatch)) {
                return sm;
            }
        }
        return null;
    }

    private String generateUdmcNameFromUimc(String uimcName) {
        return uimcName.substring(0, uimcName.lastIndexOf(FILEEXT_SOUNDMODEL_COM_KEYPHRASE)) + FILEEXT_SOUNDMODEL_COM_USER;
    }

    public String getComSoundModelNameFromKeyphrase(String keyphraseNameToMatch) {
        SoundModel sm = getComSoundModelFromKeyphrase(keyphraseNameToMatch);
        if (sm == null) {
            return null;
        }
        return sm.getName();
    }

    private String generateComSoundModelFilePath(String inComKeyphraseToCreateSm) {
        String filePath = SVAGlobal.PATH_APP + "/" + inComKeyphraseToCreateSm.replaceAll("\\s", "") + FILEEXT_SOUNDMODEL_COM_USER;
        Log.v(TAG, "generateSoundModelFilePath: filePath= " + filePath);
        return filePath;
    }

    private String generateComSmFilePathFromSmName(String inComSmName) {
        String filePath = SVAGlobal.PATH_APP + "/" + inComSmName;
        Log.v(TAG, "generateSoundModelFilePath: filePath= " + filePath);
        return filePath;
    }

    private String generateComSoundModelNameFromFilePath(String inFilePath) {
        String fileExtension;
        if (this.tempTrainingComKeyphrase.getIsUdk()) {
            fileExtension = FILEEXT_SOUNDMODEL_COM_USER;
        } else {
            fileExtension = FILEEXT_SOUNDMODEL_COM_KEYPHRASE;
        }
        String soundModelName = inFilePath.replaceAll(SVAGlobal.PATH_APP + "/", "");
        Log.v(TAG, "generateComSoundModelNameFromFilePath: soundModelName= " + soundModelName);
        return soundModelName;
    }

    public void setComSoundModelNameToExtend(String inKeyphraseName) {
        this.soundModelNameToExtend = getComSoundModelNameFromKeyphrase(inKeyphraseName);
    }

    public void setTempTrainingComKeyphrase(String keyphraseName, String keyphraseAction, Intent keyphraseActionIntent, boolean inIsUdk) {
        if (!inIsUdk) {
            SoundModel sm = getComSoundModelFromKeyphrase(keyphraseName);
            keyphraseAction = ((Keyphrase) sm.getKeyphrases().get(0)).getActionName();
            keyphraseActionIntent = ((Keyphrase) sm.getKeyphrases().get(0)).getActionIntent();
        }
        this.tempTrainingComKeyphrase = new Keyphrase(keyphraseName, keyphraseAction, keyphraseActionIntent, true);
        this.tempTrainingComKeyphrase.setIsUdk(inIsUdk);
    }

    public void deleteComKeyphrase(String inKeyphraseName) {
        Log.v(TAG, "deleteComKeyphrase: delete SM with keyphrase= " + inKeyphraseName);
        SoundModel smToDelete = getComSoundModelFromKeyphrase(inKeyphraseName);
        if (smToDelete == null) {
            Log.e(TAG, "deleteComKeyphrase: getComSoundModelNameFromKeyphrase cannot find SM for keyphrase= " + inKeyphraseName);
            return;
        }
        Utils.deleteFile(generateComSmFilePathFromSmName(smToDelete.getName()));
        Log.v(TAG, "deleteComKeyphrase: num SMs before delete= " + this.soundModels.size());
        this.soundModels.remove(smToDelete);
        Log.v(TAG, "deleteComKeyphrase: num SMs after delete= " + this.soundModels.size());
    }

    public boolean getPdkSmIsTrained(String keyphraseName) {
        return getComSoundModelFromKeyphrase(keyphraseName).getIsTrained();
    }

    public void setIsUserVerificationEnabled(Context context, String keyphraseName, boolean isUserVerificationEnabled) {
        ((Keyphrase) getComSoundModelFromKeyphrase(keyphraseName).getKeyphrases().get(0)).setIsUserVerificationEnabled(isUserVerificationEnabled);
        SoundModel sm = getComSoundModelFromKeyphrase(keyphraseName);
        Keyphrase tempKeyphrase = (Keyphrase) sm.getKeyphrases().get(0);
        saveCsmToSharedPrefs(context, sm.getName(), keyphraseName, tempKeyphrase.getActionName(), tempKeyphrase.getActionIntent(), tempKeyphrase.getIsUserVerificationEnabled());
        Log.v(TAG, "setIsUserVerificationEnabled: for keyphraseName= " + keyphraseName + ", set" + " enabled= " + tempKeyphrase.getIsUserVerificationEnabled());
    }

    public String getUdmcName(String smName) {
        return smName.substring(0, smName.lastIndexOf(".")) + FILEEXT_SOUNDMODEL_COM_USER;
    }

    public String getComSoundModelNameToSendFromKeyphrase(String inKeyphraseName) {
        SoundModel sm = getComSoundModelFromKeyphrase(inKeyphraseName);
        if (((Keyphrase) sm.getKeyphrases().get(0)).getIsUserVerificationEnabled()) {
            return getUdmcName(sm.getName());
        }
        return sm.getName();
    }

    public void saveSelectedKeyphrases(Context context, ArrayList<String> checkedKeyphrases) {
        Editor editor = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0).edit();
        StringBuilder stringBuilder = new StringBuilder();
        Iterator i$ = checkedKeyphrases.iterator();
        while (i$.hasNext()) {
            String keyphraseName = (String) i$.next();
            if (stringBuilder.length() > 0) {
                stringBuilder.append(TAG_DELIMITER);
            }
            stringBuilder.append(keyphraseName);
        }
        Log.v(TAG, "saveSelectedKeyphrases: saving keyphrasesString= " + stringBuilder.toString());
        editor.putString(TAG_PREFERENCE_SELECTEDKEYPHRASES, stringBuilder.toString());
        editor.commit();
    }

    public ArrayList<String> getSelectedKeyphrases(Context context) {
        String savedKeyphrasesValue = context.getSharedPreferences(SVAGlobal.PREFERENCE_GROUP_NAME, 0).getString(TAG_PREFERENCE_SELECTEDKEYPHRASES, null);
        if (savedKeyphrasesValue == null) {
            return null;
        }
        Log.v(TAG, "getSelectedKeyphrases: keyphrasesString= " + savedKeyphrasesValue);
        return new ArrayList(Arrays.asList(savedKeyphrasesValue.split("\\|")));
    }
}