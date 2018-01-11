package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;

/**
 * Created by king on 16-6-7.
 */
public class LuisMusicScene extends SceneBase {
    private MediaPlayer mPlayer;
    private static int mMusicIndex = 0;
    private boolean isMusicPause = false;

    public LuisMusicScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();

        String musicPath = null;
        try {
            String playAction = "";
            if (sceneAction.equals(LuisHelper.INTENT_MUSIC)) {
                String musicName = "";
                for (int i = 0; i < sceneParams.length(); i++) {
                    String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                    if (entitiesType.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_START)
                            || entitiesType.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_REPEAT)) {
                        playAction = entitiesType;
                    } else if (entitiesType.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_MUSIC_NAME)) {
                        musicName = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                    }
                }
                if (!TextUtils.isEmpty(playAction)) {
                    switch (playAction) {
                        case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_START:
                            if (TextUtils.isEmpty(musicName)) {
                                musicPath = getMusic(mMusicIndex);
                            } else {
                                musicPath = getMusic(musicName);
                            }
                            if (TextUtils.isEmpty(musicPath)) {
                                toSpeakThenDone(getString(R.string.luis_assistant_music_can_not_found));
                            } else {
                                playMusic(musicPath, false);
                                updateLog(context.getString(R.string.luis_log_music_started));
                            }
                            break;
                        case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_REPEAT:
                            if (TextUtils.isEmpty(musicName)) {
                                musicPath = getMusic(mMusicIndex);
                            } else {
                                musicPath = getMusic(musicName);
                            }
                            if (TextUtils.isEmpty(musicPath)) {
                                toSpeakThenDone(getString(R.string.luis_assistant_music_can_not_found));
                            } else {
                                playMusic(musicPath, true);
                                updateLog(context.getString(R.string.luis_log_music_repeat));
                            }
                            break;
                        default:
                            resumeMusic(true);
                            break;
                    }
                } else {
                    /*if (mPlayer != null) {
                        resumeMusic(true);
                    } else {*/
                    toSpeakThenDone(getString(R.string.luis_assistant_cmd_empty));
                    //}
                }
            } else if (sceneAction.equalsIgnoreCase(LuisHelper.INTENT_SCENARIO_ACTIONS)) {
                if (mPlayer == null) {
                    toSpeakThenDone(getString(R.string.luis_assistant_music_not_start));
                } else {
                    if (sceneParams.length() > 0) {
                        for (int i = 0; i < sceneParams.length(); i++) {
                            String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            if (entitiesType.startsWith(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PREFIX)) {
                                playAction = entitiesType;
                                break;
                            }
                        }

                        switch (playAction) {
                            case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_STOP:
                                if (mPlayer == null || (!mPlayer.isPlaying() && isMusicPause)) {
                                    toSpeakThenDone(getString(R.string.luis_assistant_music_not_start));
                                } else {
                                    stopMusic();
                                    isMusicPause = true;
                                    updateLog(context.getString(R.string.luis_log_music_stopped));
                                }
                                break;
                            case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PAUSE:
                                if (mPlayer == null || (!mPlayer.isPlaying() && isMusicPause)) {
                                    toSpeakThenDone(getString(R.string.luis_assistant_music_not_start));
                                } else {
                                    pauseMusic(false);
                                    isMusicPause = true;
                                    updateLog(context.getString(R.string.luis_log_music_paused));
                                }
                                break;
                            case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_RESUME:
                                if (mPlayer == null) {
                                    playMusic(getMusic(mMusicIndex), false);
                                } else {
                                    resumeMusic(false);
                                    updateLog(context.getString(R.string.luis_log_music_resumed));
                                }
                                break;
                            case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PREVIOUS:
                            case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_NEXT:
                                if (playAction.equals(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PREVIOUS)) {
                                    mMusicIndex--;
                                } else if (playAction.equals(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_NEXT)) {
                                    mMusicIndex++;
                                }
                                musicPath = getMusic(mMusicIndex);

                                if (!TextUtils.isEmpty(musicPath)) {
                                    playMusic(musicPath, true);
                                    if (sceneAction.equals(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PREVIOUS)) {
                                        updateLog(context.getString(R.string.luis_log_music_previous));
                                    } else if (sceneAction.equals(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_NEXT)) {
                                        updateLog(context.getString(R.string.luis_log_music_next));
                                    }
                                } else {
                                    toSpeakThenDone(getString(R.string.luis_assistant_music_can_not_found));
                                }
                                break;
                            /*case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLUP:
                            case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLDOWN:
                                AudioManager mManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                if (playAction.equalsIgnoreCase(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLUP)) {
                                    mManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                                } else {
                                    mManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                                }

                                resumeMusic(false);
                                updateLog(context.getString(R.string.luis_log_music_resumed));
                                break;*/
                            default:
                                toSpeak(getString(R.string.luis_assistant_operate_is_not_support), false);
                                break;
                        }
                    } else {
                        toSpeak(getString(R.string.luis_assistant_operate_is_not_support), false);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    @Override
    public void pause() {
        super.pause();
        pauseMusic(true);
    }

    @Override
    public void resume() {
        super.resume();
        resumeMusic(true);
    }

    @Override
    public void stop() {
        super.stop();
        stopMusic();
        SceneCommonHelper.closeLED();
    }

    private String getMusic(String name) {
        String musicPath = null;
        if (!TextUtils.isEmpty(name)) {
            Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media
                    .EXTERNAL_CONTENT_URI, null, null, null, null);
            if (null != cursor && cursor.getCount() > 0) {
                int maxMatchCount = 0;
                while (cursor.moveToNext()) {
                    String displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                    displayName = displayName.substring(0, displayName.lastIndexOf("."));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    File musicFile = new File(path);
                    if (!musicFile.exists()) {
                        continue;
                    } else {
                        boolean isEnglishSong = name.matches("^[A-Za-z0-9\\s]+$");
                        int matchCount = 0;
                        if (isEnglishSong) {
                            String[] sceneSplit = name.split(" ");
                            for (String split : sceneSplit) {
                                String[] fileNameSplit = displayName.split("[\\W_]");
                                for (String temp : fileNameSplit) {
                                    if (temp.toLowerCase().equals(split.toLowerCase())) {
                                        matchCount++;
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < name.length(); i++) {
                                String nameChar = name.substring(i, i + 1);
                                for (int j = 0; j < displayName.length(); j++) {
                                    String displayNameChar = displayName.substring(j, j + 1);
                                    if (nameChar.toLowerCase().equals(displayNameChar.toLowerCase())) {
                                        matchCount++;
                                    }
                                }
                            }
                        }
                        if (matchCount > maxMatchCount) {
                            maxMatchCount = matchCount;
                            mMusicIndex = cursor.getPosition();
                            musicPath = path;
                        }
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicPath;
    }

    private String getMusic(int index) {
        String musicPath = null;
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media
                .EXTERNAL_CONTENT_URI, null, null, null, null);
        if (null != cursor && cursor.getCount() > 0) {
            if (index >= cursor.getCount()) {
                index = 0;
            } else if (index < 0) {
                index = cursor.getCount() - 1;
            }
            mMusicIndex = index;
            cursor.moveToPosition(index);
            String tempPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            File musicFile = new File(tempPath);
            if (!musicFile.exists()) {
                while (cursor.moveToNext()) {
                    tempPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    musicFile = new File(tempPath);
                    if (musicFile.exists()) {
                        musicPath = tempPath;
                        mMusicIndex = cursor.getPosition();
                        break;
                    }
                }
            } else {
                musicPath = tempPath;
                mMusicIndex = cursor.getPosition();
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return musicPath;
    }

    private void playMusic(String path, final boolean repeat) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }

        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        Log.i("King", "Play music path: " + path);
        try {
            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.setLooping(repeat);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!repeat) {
                        SceneCommonHelper.closeLED();
                    }
                    isMusicPause = true;
                }
            });
            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    SceneCommonHelper.closeLED();
                    isMusicPause = true;
                    return false;
                }
            });
            isMusicPause = false;
        } catch (IOException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    private void stopMusic() {
        if (null != mPlayer) {
            mPlayer.stop();
            mPlayer.reset();
            mPlayer = null;
            Log.i("King", "release mPlayer...");
        }
    }

    private void pauseMusic(boolean tempControl) {
        if (!tempControl) {
            isMusicPause = true;
        }

        if (null != mPlayer) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                Log.i("King", "pause music...");
            }
        } else {
            SceneCommonHelper.closeLED();
        }
    }

    private void resumeMusic(boolean tempControl) {
        if (tempControl && isMusicPause) {
            return;
        }
        if (null != mPlayer) {
            if (!mPlayer.isPlaying()) {
                mPlayer.start();
                SceneCommonHelper.openLED();
                if (!tempControl) {
                    isMusicPause = false;
                }
            }
        }
    }
}
