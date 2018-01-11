package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.OcrTtsActivity;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.Story;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * Created by king on 16-5-9.
 */
public class OCRScene extends SceneBase {
    private static final String STORY_FOLDER_NAME = "story";

    private String mStoryFolder;

    private static final String[] projection = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA
    };

    private ArrayList<Story> mStoryList;
    private String storyName;

    public OCRScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
        mStoryFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + STORY_FOLDER_NAME;
        Log.i("King", "OCRScene" + " mStoryFolder = " + mStoryFolder);
    }

    @Override
    public void simulate() {
        super.simulate();
        try {
            sceneAction = LuisHelper.ENTITIES_TYPE_STORY_LOCAL;
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_BOOK_STORY_TITLE.equals(entitiesType)) {
                    storyName = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                } else if (LuisHelper.ENTITIES_TYPE_STORY_LOCAL.equals(entitiesType)
                        || LuisHelper.ENTITIES_TYPE_STORY_NEW.equals(entitiesType)) {
                    sceneAction = entitiesType;
                }
            }
            if (sceneAction != null) {
                if (LuisHelper.ENTITIES_TYPE_STORY_LOCAL.equals(sceneAction)) {
                    ((Activity) context).getLoaderManager().initLoader(0, null, callbacks);
                } else if (LuisHelper.ENTITIES_TYPE_STORY_NEW.equals(sceneAction)) {
                    Intent launchOcrIntent = new Intent();
                    launchOcrIntent.setClass(context, OcrTtsActivity.class);
                    launchOcrIntent.putExtra(CommonHelper.EXTRA_LAUNCH_MODE, CommonHelper.LAUNCH_MODE_TAKE_PHOTO);
                    ((SceneActivity) context).startActivityForResult(launchOcrIntent, CommonHelper.OCR_REQUEST_CODE);
                }
            } else {
                SceneCommonHelper.closeLED();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    @Override
    public void stop() {
        super.stop();
        ((Activity) context).getLoaderManager().destroyLoader(0);
    }

    private LoaderManager.LoaderCallbacks callbacks = new LoaderManager.LoaderCallbacks() {
        @Override
        public Loader onCreateLoader(int id, Bundle args) {
            CursorLoader loader = new CursorLoader(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                    MediaStore.Images.Media.DATA + " LIKE ?", new String[]{mStoryFolder + "%"}, null);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader loader, Object data) {
            Cursor cursor = (Cursor) data;
            Log.i("King", "OCRScene cursorCount = " + cursor.getCount());
            if (cursor != null && cursor.getCount() > 0) {
                if (TextUtils.isEmpty(storyName)) {
                    Random mRandom = new Random(System.currentTimeMillis());
                    int position = mRandom.nextInt(cursor.getCount());
                    cursor.moveToPosition(position);
                    mStoryList = new ArrayList<>();
                    mStoryList.add(new Story(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))));
                } else {
                    int maxMatchCount = 0;
                    mStoryList = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        String displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                        displayName = displayName.substring(0, displayName.lastIndexOf("."));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                        Log.i("King", "FileName = " + displayName
                                + ", data = " + path);
                        boolean isEnglishSong = storyName.matches("^[A-Za-z0-9\\s]+$");
                        int matchCount = 0;
                        if (isEnglishSong) {
                            String[] sceneSplit = storyName.split(" ");
                            for (String split : sceneSplit) {
                                String[] fileNameSplit = displayName.split("[\\W_]");
                                for (String temp : fileNameSplit) {
                                    if (temp.toLowerCase().equals(split.toLowerCase())) {
                                        matchCount++;
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < storyName.length(); i++) {
                                String nameChar = storyName.substring(i, i + 1);
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
                            mStoryList.clear();
                        }
                        if (matchCount == maxMatchCount && matchCount > 0) {
                            mStoryList.add(new Story(displayName, path));
                        }
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }

            if (mStoryList == null || mStoryList.size() <= 0) {
                toSpeak(getString(R.string.ocr_to_speak_cannot_found_story));
                SceneCommonHelper.closeLED();
            } else {
                Collections.sort(mStoryList, new SortStories());
                for (Story story : mStoryList) {
                    Log.i("King", "story = " + story.getName() + ", path = " + story.getPath());
                }
                Intent launchOcrIntent = new Intent();
                launchOcrIntent.setClass(context, OcrTtsActivity.class);
                launchOcrIntent.putExtra(CommonHelper.EXTRA_LAUNCH_MODE, CommonHelper.LAUNCH_MODE_FROM_GALLERY);
                launchOcrIntent.putExtra(CommonHelper.EXTRA_STORIES, mStoryList);
                ((SceneActivity) context).startActivityForResult(launchOcrIntent, CommonHelper.OCR_REQUEST_CODE);
            }
        }

        @Override
        public void onLoaderReset(Loader loader) {

        }
    };

    private class SortStories implements Comparator<Story> {

        @Override
        public int compare(Story lhs, Story rhs) {
            return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("King", "OCRScene resultCode = " + resultCode);
        if (resultCode == CommonHelper.OCR_RESULT_CODE) {
            if (data.getExtras().containsKey(CommonHelper.EXTRA_KEY_CODE)) {
                int keyCode = data.getIntExtra(CommonHelper.EXTRA_KEY_CODE, -1);
                Log.i("King", "OCRScene keyCode = " + keyCode);
                if (keyCode == KeyEvent.KEYCODE_F11
                        || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    ((SceneActivity) context).performBtnCmd();
                }
            }
        }
    }
}
