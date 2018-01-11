package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.youtube.VideoItem;
import com.wistron.demo.tool.teddybear.scene.youtube.YoutubeConnector;
import com.wistron.demo.tool.teddybear.scene.youtube.YoutubePlayActivity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by king on 17-2-28.
 */

public class LuisYoutubeVideoScene extends SceneBase {
    private YoutubeVideoQueryTask mYoutubeVideoSearchTask;

    public LuisYoutubeVideoScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();

        String videoName = null;
        try {
            if (sceneAction.equals(LuisHelper.INTENT_YOUTUBE_VIDEO)) {  // play the video
                for (int i = 0; i < sceneParams.length(); i++) {
                    String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                    if (LuisHelper.ENTITIES_TYPE_VIDEO_NAME.equals(entitiesType)) {
                        videoName = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                    }
                }

                if (TextUtils.isEmpty(videoName)) {
                    toSpeak(getString(R.string.luis_assistant_youtube_search_title_empty));
                } else {
                    mYoutubeVideoSearchTask = new LuisYoutubeVideoScene.YoutubeVideoQueryTask();
                    mYoutubeVideoSearchTask.executeOnExecutor(SceneCommonHelper.mCachedThreadPool, videoName);
                }
            } else if (sceneAction.equals(LuisHelper.INTENT_SCENARIO_ACTIONS)) {  // control the video
                if (sceneParams.length() > 0) {
                    String playAction = "";
                    for (int i = 0; i < sceneParams.length(); i++) {
                        String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                        if (entitiesType.startsWith(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PREFIX)) {
                            playAction = entitiesType;
                            break;
                        }
                    }
                    switch (playAction) {
                        case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_NEXT:
                            nextVideo();
                            break;
                        case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PREVIOUS:
                            previousVideo();
                            break;
                        case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PAUSE:
                            pauseVideo();
                            break;
                        case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_RESUME:
                            resumeVideo();
                            break;
                        case LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_STOP:
                            stopVideo();
                            break;
                        default:
                            toSpeak(getString(R.string.luis_assistant_operate_is_not_support), false);
                            break;
                    }
                } else {
                    toSpeak(getString(R.string.luis_assistant_operate_is_not_support), false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("King", "LuisYoutubeVideoScene error result = " + e.getMessage());
            toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
        }
    }

    @Override
    public void pause() {
        super.pause();
        pauseVideo();
    }

    @Override
    public void resume() {
        super.resume();
        resumeVideo();
    }

    private void previousVideo() {
        Intent intent = new Intent(CommonHelper.ACTION_SCENARIO_ACTION);
        intent.putExtra(CommonHelper.EXTRA_SCENARIO_ACTION, CommonHelper.SCENARIO_ACTION_PREVIOUS);
        context.sendBroadcast(intent);
    }

    private void nextVideo() {
        Intent intent = new Intent(CommonHelper.ACTION_SCENARIO_ACTION);
        intent.putExtra(CommonHelper.EXTRA_SCENARIO_ACTION, CommonHelper.SCENARIO_ACTION_NEXT);
        context.sendBroadcast(intent);
    }

    private void resumeVideo() {
        Intent intent = new Intent(CommonHelper.ACTION_SCENARIO_ACTION);
        intent.putExtra(CommonHelper.EXTRA_SCENARIO_ACTION, CommonHelper.SCENARIO_ACTION_RESUME);
        context.sendBroadcast(intent);
    }

    private void pauseVideo() {
        Intent intent = new Intent(CommonHelper.ACTION_SCENARIO_ACTION);
        intent.putExtra(CommonHelper.EXTRA_SCENARIO_ACTION, CommonHelper.SCENARIO_ACTION_PAUSE);
        context.sendBroadcast(intent);
    }

    private void stopVideo() {
        Intent intent = new Intent(CommonHelper.ACTION_SCENARIO_ACTION);
        intent.putExtra(CommonHelper.EXTRA_SCENARIO_ACTION, CommonHelper.SCENARIO_ACTION_STOP);
        context.sendBroadcast(intent);
    }

    @Override
    public void stop() {
        super.stop();
        if (mYoutubeVideoSearchTask != null) {
            mYoutubeVideoSearchTask.cancel(true);
            mYoutubeVideoSearchTask = null;
        }
    }

    private class YoutubeVideoQueryTask extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            String videoName = params[0];
            YoutubeConnector yc = new YoutubeConnector(context, videoName.trim());
            List<VideoItem> searchResults = yc.getVideos();

            ArrayList<String> videosList = new ArrayList<>();
            for (VideoItem item : searchResults) {
                videosList.add(item.getId());
            }

            if (videosList.size() <= 0) {
                toSpeak(getString(R.string.luis_assistant_youtube_no_resource_found));
            }

            return videosList;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            if (strings.size() > 0) {
                Intent intent = new Intent(context, YoutubePlayActivity.class);
                intent.putStringArrayListExtra(YoutubePlayActivity.EXTRA_PLAY_VIDEOS, strings);
                context.startActivity(intent);
            }
        }
    }
}
