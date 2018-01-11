package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.NetworkAccessHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by king on 16-11-24.
 */

public class LuisNewsScene extends SceneBase {
    private static final String TAG = "LuisNewsScene";

    private final String NEWS_API_SOURCE_BBC = "bbc-news";
    private final String NEWS_API_SOURCE_CNN = "cnn";
    private final String NEWS_API_SOURCE_ESPN = "espn";
    private static final String NEWS_API_ARTICLES = "https://newsapi.org/v1/articles?source=%1$s&apiKey=%2$s";

    private String mCurrentNewsSource = NEWS_API_SOURCE_CNN;
    private NewsQueryTask mNewsQueryTask;

    public LuisNewsScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();

        try {
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_COMMON_RESERVED_1.equals(entitiesType)) {
                    String action = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                    if (!TextUtils.isEmpty(action)) {
                        if (action.toLowerCase().startsWith("bbc")) {
                            mCurrentNewsSource = NEWS_API_SOURCE_BBC;
                        } else if (action.toLowerCase().startsWith(NEWS_API_SOURCE_ESPN)) {
                            mCurrentNewsSource = NEWS_API_SOURCE_ESPN;
                        }
                    }
                }
            }
            mNewsQueryTask = new NewsQueryTask();
            mNewsQueryTask.execute();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("King", "LuisNewsScene error result = " + e.getMessage());
            toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "stop LuisNewsScene start");
        super.stop();
        mNewsQueryTask.cancel(true);
        Log.i(TAG, "stop LuisNewsScene end");
    }

    private class NewsQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String apiKey = context.getString(R.string.newsapi_api_key);
                String url = String.format(NEWS_API_ARTICLES, mCurrentNewsSource, apiKey);
                Log.i("King", "NewsQueryTask url = " + url);
                String getWeatherData = NetworkAccessHelper.invokeNetworkGet(url);
                JSONObject toJason = new JSONObject(getWeatherData);
                if (toJason.has("status")) {
                    String status = toJason.getString("status");
                    if (status.equals("ok")) {
                        JSONArray articlesJson = toJason.getJSONArray("articles");
                        toSpeak(String.format(getString(R.string.luis_assistant_news_speaking_start), mCurrentNewsSource), false);
                        for (int i = 0; i < articlesJson.length() && !isSceneStopped; i++) {
                            JSONObject articleJson = articlesJson.getJSONObject(i);
                            toSpeak(articleJson.getString("title"), false);
                            Thread.sleep(1000);
                        }
                        if (!isSceneStopped) {
                            toSpeak(String.format(getString(R.string.luis_assistant_news_speaking_end), mCurrentNewsSource), false);
                        }
                    } else {
                        toSpeakErrorMsg();
                    }
                } else {
                    toSpeakErrorMsg();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "NewsQueryTask error result = " + e.getMessage());
                if (!isSceneStopped) {
                    toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            stop();
        }
    }

    private void toSpeakErrorMsg() {
        toSpeakThenDone(getString(R.string.luis_assistant_news_cannot_get));
    }
}
