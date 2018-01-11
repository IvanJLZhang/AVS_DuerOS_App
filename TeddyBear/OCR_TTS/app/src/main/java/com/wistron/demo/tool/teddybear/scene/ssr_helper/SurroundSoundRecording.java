package com.wistron.demo.tool.teddybear.scene.ssr_helper;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.WindowManager;

import com.wistron.demo.tool.teddybear.led_control.LedController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper.STORAGE_CONFIG_FILE_PATH;
import static com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper.getSingleParameters;


/**
 * Created by king on 16-9-10.
 * SSR function
 */
public class SurroundSoundRecording implements OnDataSourceUpdate {
    private static final String TAG = "SurroundSoundRecording";

    public static int dataPollingPeriod = 200;
    private static final int configPollingPeriod = 2000;

    private Context context;
    private boolean isTracking = false;

    private AudioManager mAudioManager;
    private static UpdateThread thread;

    private boolean lastWriteFailed;
    private boolean lastReadFailed;
    private ConfigData mCurrentConfig;
    private ConfigData mUserConfig;
    private DataSource mDataSource;
    private OnDataSourceUpdate mDataUpdateListener;

    private float[] mRotationOffset;

    private static SurroundSoundRecording ssrInstance;


    private float minVoiceValue = 80f;
    private int minVoiceNumber = 3;

    public static SurroundSoundRecording getInstance(Context context) {
        if (ssrInstance == null) {
            ssrInstance = new SurroundSoundRecording(context);
        }
        return ssrInstance;
    }

    private SurroundSoundRecording(Context context) {
        this.context = context;
        this.mDataSource = new DataSource();
        this.mDataUpdateListener = this;

        if (mAudioManager == null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        mRotationOffset = new float[]{90.0f, 180.0f, 270.0f, 0.0f};

        setUserConfig(new int[]{45, 135, 225, 315}, new int[]{1, 1, 1, 1});
    }

    // For Debug
    // private MediaRecorder mediaRecorder;

    public void startTracking() {
        // For Debug
        /*mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //currentFile = getOutputMediaFile(MEDIA_TYPE_AUDIO);
            mediaRecorder.setOutputFile("/storage/emulated/0/Movies/AUD_20160910_101305.wav");
            mediaRecorder.setOutputFormat(21);
            mediaRecorder.setAudioEncoder(12);
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioSamplingRate(48000);
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "Media recorder started");
        }catch (IOException e){
            e.printStackTrace();
        }*/

        Map<String, String> mParametersList = getSingleParameters(STORAGE_CONFIG_FILE_PATH);
        if (mParametersList != null && mParametersList.size() > 0) {
            for (String key : mParametersList.keySet()) {
                if (key.equals("ssrMinVoiceValue")) {
                    minVoiceValue = Float.parseFloat(mParametersList.get(key));
                } else if (key.equals("ssrMinVoiceNumber")) {
                    minVoiceNumber = Integer.parseInt(mParametersList.get(key));
                }
            }
        }

        if (isTracking) {
            return;
        }

        mAudioManager.setParameters("SourceTrack.enable_wnr=1");
        mAudioManager.setParameters("SourceTrack.zoom_beampattern=0");
        mAudioManager.setParameters("ssr.ns_level=100");

        mAudioManager.setParameters("SoundFocus.holding_position=0");
        isTracking = true;
        dataPollingPeriod = 50;
        Log.i(TAG, "startTracking thread = " + thread);
        if (thread == null) {
            thread = new UpdateThread();
            thread.start();
        } else {
            Log.d(TAG, "Thread already running.");
        }
    }

    public void stopTracking() {
        isTracking = false;
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread = null;
        }
        LedController.turnOffHighLightLed();
        Log.i(TAG, "stopTracking......");

        // for Debug
        /*try {
            Log.d(TAG, "releaseMediaRecorder");
            if (this.mediaRecorder != null) {
                this.mediaRecorder.release();
                this.mediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception " + e.getMessage());
        }*/
    }

    private boolean readConfig() {
        try {
            String data = this.mAudioManager.getParameters(SoundFocusParameters.ALL_CONFIG);
            Log.d(TAG, "readConfig()=" + data);
            if (data != null && data.length() > 0) {
                String[] kvp;
                int i;
                int[] sectorStartAngle = new int[4];
                int[] sectorNSEnabled = new int[4];
                Map<String, String> map = SoundFocusParameters.parse(data);
                String value = (String) map.get(SoundFocusParameters.START_ANGLE);
                if (value != null) {
                    kvp = value.split(SSRConsts.COMMA);
                    i = 0;
                    while (i < kvp.length && i < 4) {
                        sectorStartAngle[i] = Integer.valueOf(kvp[i]).intValue();
                        i++;
                    }
                }
                value = (String) map.get(SoundFocusParameters.ENABLE_SECTORS);
                if (value != null) {
                    kvp = value.split(SSRConsts.COMMA);
                    i = 0;
                    while (i < kvp.length && i < 4) {
                        sectorNSEnabled[i] = Integer.valueOf(kvp[i]).intValue();
                        i++;
                    }
                }
                value = (String) map.get(SoundFocusParameters.WNR_ENABLED);
                int wnr = 1;
                if (value != null) {
                    wnr = Integer.valueOf(value).intValue();
                    Log.d(TAG, "Read WNR=" + value);
                }
                value = (String) map.get(SoundFocusParameters.BEAM);
                int beam = SSRConsts.DEFAULT_BEAM;
                if (value != null) {
                    beam = Integer.valueOf(value).intValue();
                    Log.d(TAG, "Read beam=" + value);
                }
                value = (String) map.get(SoundFocusParameters.MUTE);
                int mute = 0;
                if (value != null) {
                    mute = Integer.valueOf(value).intValue();
                    Log.d(TAG, "Read mute=" + value);
                }
                if (this.mCurrentConfig == null) {
                    this.mCurrentConfig = new ConfigData(sectorStartAngle, sectorNSEnabled, wnr, beam);
                }
                this.mCurrentConfig.update(sectorStartAngle, sectorNSEnabled, wnr, beam);
                this.mCurrentConfig.setMute(mute == 1);
                value = (String) map.get(SoundFocusParameters.SSR_NS_LEVEL);
                float nsLevel = (float) this.mCurrentConfig.getNSLevel();
                if (value != null) {
                    nsLevel = Float.valueOf(value).floatValue();
                    Log.d(TAG, "Read ns=" + value);
                }
                this.mCurrentConfig.setNSLevel(Math.round(nsLevel));
                this.lastReadFailed = false;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception in SFastConfigServer readConfig! " + e.getMessage());
        }
        Log.d(TAG, "Error reading configuration. Need to reset?");
        this.lastReadFailed = true;
        Log.d(TAG, "Need to set user config before overwriting next time.");
        return false;
    }

    private void updateData() {
        if (this.mDataUpdateListener != null) {
            this.mDataUpdateListener.onDataSourceUpdate(DataSource.obtain(this.mDataSource));
        }
    }

    private boolean readData() {
        try {
            Log.d(TAG, "reading " + SoundFocusParameters.ALL_DATA);
            String data = this.mAudioManager.getParameters(SoundFocusParameters.ALL_DATA);
            if (data == null || data.length() == 0) {
                Log.d(TAG, "Data is not available!");
                return false;
            }
            String[] kvp = null;
            int i;
            Log.d(TAG, "readDev=" + data);
            Map<String, String> map = SoundFocusParameters.parse(data);
            String value = (String) map.get(SoundFocusParameters.VAD);
            if (value != null) {
                kvp = value.split(SSRConsts.COMMA);
                i = 0;
                while (i < kvp.length && i < this.mDataSource.vad.length) {
                    Log.d(TAG, String.format("%s[%d]=%s", new Object[]{SoundFocusParameters.VAD, Integer.valueOf(i), kvp[i]}));
                    this.mDataSource.vad[i] = Integer.valueOf(kvp[i]).intValue();
                    i++;
                }
            }
            value = (String) map.get(SoundFocusParameters.DOA_NOISE);
            if (value != null) {
                kvp = value.split(SSRConsts.COMMA);
                i = 0;
                while (i < kvp.length && i < this.mDataSource.interfererAngle.length) {
                    Log.d(TAG, String.format("%s[%d]=%s", new Object[]{SoundFocusParameters.DOA_NOISE, Integer.valueOf(i), kvp[i]}));
                    this.mDataSource.interfererAngle[i] = Integer.valueOf(kvp[i]).intValue();
                    i++;
                }
            }
            value = (String) map.get(SoundFocusParameters.POLAR_ACTIVITY);
            if (value != null) {
                kvp = value.split(SSRConsts.COMMA);
                Log.d(TAG, String.format("%s[%d]= %s", new Object[]{SoundFocusParameters.POLAR_ACTIVITY, Integer.valueOf(value.split(SSRConsts.COMMA).length), value}));
                for (String valueOf : kvp) {
                    this.mDataSource.add(Float.valueOf(valueOf).floatValue());
                }
            }
            value = (String) map.get(SoundFocusParameters.SSR_NOISE_LEVEL);
            float noiseLevel = 0.0f;
            if (value != null) {
                noiseLevel = Float.valueOf(value).floatValue() + 100.0f;
                Log.d(TAG, "Read noiseLevel=" + value);
            }
            this.mDataSource.setNoiseLevel(Math.round(noiseLevel));
            value = (String) map.get(SoundFocusParameters.SSR_NOISE_LEVEL_AFTER_NS);
            float noiseLevelAfter = 0.0f;
            if (value != null) {
                noiseLevelAfter = Float.valueOf(value).floatValue() + 100.0f;
                Log.d(TAG, "Read noiseLevelAfter=" + value);
            }
            this.mDataSource.setNoiseLevelAfter(Math.round(noiseLevelAfter));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onDataSourceUpdate(DataSource dataSource) {
        Log.i("King1", "onDataSourceUpdate:: ------ start, minVoiceValue = " + minVoiceValue + ", minVoiceNumber = " + minVoiceNumber);
        HashMap<Integer, VoiceAngleRange> validAngles = new LinkedHashMap<>();
        for (int i = 0; i < mDataSource.data.length; i++) {
            if (this.mDataSource.data[i] > minVoiceValue) {
                if (validAngles.containsKey(i / 30)) {
                    VoiceAngleRange voiceAngleRange = validAngles.get(i / 30);
                    voiceAngleRange.setValidAngleNum(voiceAngleRange.getValidAngleNum() + 1);
                    voiceAngleRange.setAngleSum(voiceAngleRange.getAngleSum() + mDataSource.data[i]);
                } else {
                    VoiceAngleRange voiceAngleRange = new VoiceAngleRange(i / 30);
                    voiceAngleRange.setValidAngleNum(1);
                    voiceAngleRange.setAngleSum(mDataSource.data[i]);
                    validAngles.put(i / 30, voiceAngleRange);
                }
            }
        }
        int voiceDirection = -1;
        float maxAverage = 0;
        int maxValidAngleNum = 0;
        for (Integer key : validAngles.keySet()) {
            VoiceAngleRange voiceAngleRange = validAngles.get(key);
            Log.i(TAG, "onDataSourceUpdate:: key : " + key + ", validAngleNum : " + voiceAngleRange.getValidAngleNum() + ", angleSum= " + voiceAngleRange.getAngleSum());
            if (voiceAngleRange.getValidAngleNum() > minVoiceNumber) {
                float average = voiceAngleRange.getAngleSum() / voiceAngleRange.getValidAngleNum();
                if (voiceAngleRange.getValidAngleNum() > maxValidAngleNum) {
                    voiceDirection = key;
                    maxAverage = average;
                    maxValidAngleNum = voiceAngleRange.getValidAngleNum();
                } else if (voiceAngleRange.getValidAngleNum() == maxValidAngleNum) {
                    if (average > maxAverage) {
                        maxAverage = average;
                        voiceDirection = key;
                    }
                }
            }
        }
        Log.i(TAG, "onDataSourceUpdate:: maxAverage = " + maxAverage + ", voiceDirection = " + voiceDirection);
        Log.i(TAG, "onDataSourceUpdate:: ------ end");

        if (voiceDirection == -1) {
            LedController.turnOffHighLightLed();
        } else {
            LedController.turnOnHighlightLed(voiceDirection);
        }

        /*int i = 0;
        while (i < this.mDataSource.interfererAngle.length) {
            if (this.mDataSource.interfererAngle[i] != 65535) {
                if (this.mDataSource.interfererAngle[i] > 360) {
                    Log.e("PolarGraphControl", "Interferer angle " + this.mDataSource.interfererAngle[i] + " is invalid!!");
                } else {
                    if (mDataSource.data[mDataSource.interfererAngle[i]] > 20)
                    Log.i("King1", "angle = " + mDataSource.interfererAngle[i] + ", length = " + mDataSource.data[mDataSource.interfererAngle[i]]);
                }
            }
            i++;
        }*/
    }

    private int getScreenRotation() {
        try {
            return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    private float getDrawRotatedAngle(float angle, int rotation) {
        float rot = angle + this.mRotationOffset[rotation];
        if (rot > 360.0f) {
            rot = ((float) SSRConsts.CONFIG_MAX_SECTOR_ANGLE) - rot;
        }
        if (rot < 0.0f) {
            return Math.abs(rot);
        }
        return rot;
    }


    private class UpdateThread extends Thread {
        public void run() {
            super.run();
            Log.d(TAG, "Server running.");
            int failed = 0;
            while (isTracking) {
                try {
                    if (lastWriteFailed) {
                        Log.d(TAG, "Attempting to readConfig...");
                        readConfig();
                        if (!isTracking) {
                            break;
                        }
                        if (lastReadFailed) {
                            Log.d(TAG, "Last readConfig failed.");
                        } else {
                            Log.d(TAG, "Last readConfig succeeded.");
                            if (!mCurrentConfig.equals(mUserConfig)) {
                                Log.d(TAG, "run: Current and User Config DO NOT match. Attempt to set.");
                                writeUserConfig();
                                if (!lastWriteFailed) {
                                    Log.d(TAG, "Write success. Make sure user config matches system's.");
                                    mUserConfig.update(mCurrentConfig);
                                }
                            }
                            if (mCurrentConfig.equals(mUserConfig)) {
                                Log.d(TAG, "Current and User Config matches.");
                                lastWriteFailed = false;
                            }
                        }
                        if (lastWriteFailed) {
                            failed++;
                            if (failed > 3) {
                                lastWriteFailed = false;
                                Log.d(TAG, "MAX fails reached. Make sure user config matches system's.");
                                mUserConfig.update(mCurrentConfig);
                            } else {
                                Log.d(TAG, "Waiting " + dataPollingPeriod + " ms until next write attempt.");
                                Thread.sleep(dataPollingPeriod);
                                continue;
                            }
                        }
                    }
                    Log.i(TAG, "dataPollingPeriod = " + dataPollingPeriod + ", configPollingPeriod = " + configPollingPeriod + ", isTracking = " + isTracking);
                    boolean success = readData();
                    if (!isTracking) {
                        break;
                    } else if (success) {
                        updateData();
                        Thread.sleep(dataPollingPeriod);
                        continue;
                    } else {
                        Log.d(TAG, "Read data failed. Must reset config.");
                        lastWriteFailed = true;
                        lastReadFailed = true;
                    }
                    Thread.sleep(configPollingPeriod);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Thread has stopped! stop=" + isTracking + ",isInterrupted=" + isInterrupted());
        }
    }

    private void writeUserConfig() {
        Log.d(TAG, "writeUserConfig()");
        if (this.mUserConfig != null) {
            setUserConfigParameters();
        }
    }

    private void setUserConfigParameters() {
        int i = 1;
        try {
            if (this.mUserConfig == null) {
                Log.d(TAG, "setParameters(): Not user config to set. Ignore.");
            } else if (isRunning()) {
                Arrays.sort(this.mUserConfig.getData());
                String str = "%s=%s;%s=%s;%s=%s;%s=%s;%s=%s;%s=%s";
                Object[] objArr = new Object[12];
                objArr[0] = SoundFocusParameters.START_ANGLE;
                objArr[1] = getConfigString(this.mUserConfig.getStartAngles());
                objArr[2] = SoundFocusParameters.ENABLE_SECTORS;
                objArr[3] = getConfigString(this.mUserConfig.getEnabled());
                objArr[4] = SoundFocusParameters.GAIN_STEP;
                objArr[5] = String.valueOf(this.mUserConfig.getGainStep());
                objArr[6] = SoundFocusParameters.WNR_ENABLED;
                if (!this.mUserConfig.isWNREnabled()) {
                    i = 0;
                }
                objArr[7] = String.valueOf(i);
                objArr[8] = SoundFocusParameters.BEAM;
                objArr[9] = String.valueOf(this.mUserConfig.getBeam());
                objArr[10] = SoundFocusParameters.HOLDING_POSITION;
                objArr[11] = String.valueOf(this.mUserConfig.getOrientation());
                mAudioManager.setParameters(String.format(str, objArr));
            } else {
                Log.d(TAG, "DataServer not running. Must set next time.");
                this.lastWriteFailed = true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception in setUserConfigParameters(): " + e.getMessage());
            if (this.mUserConfig != null) {
                this.mUserConfig.printLog(TAG);
            }
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return thread != null;
    }

    public static String getConfigString(int[] values) {
        try {
            StringBuilder string = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    string.append(SSRConsts.COMMA);
                }
                string.append(Integer.toString(values[i]));
            }
            return string.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setUserConfig(int[] sectorAngles, int[] sectorEnabled) {
        Log.d(TAG, "setting user config data");
        if (this.mUserConfig != null) {
            this.mUserConfig.update(sectorAngles, sectorEnabled);
        } else {
            this.mUserConfig = new ConfigData(sectorAngles, sectorEnabled, true, (int) SSRConsts.DEFAULT_BEAM);
        }
    }
}
