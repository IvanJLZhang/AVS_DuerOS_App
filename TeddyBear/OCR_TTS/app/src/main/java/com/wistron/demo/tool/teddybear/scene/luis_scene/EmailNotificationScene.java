package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.BluetoothSearch;
import com.wistron.demo.tool.teddybear.scene.helper.BtConfigDevice;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SplitContentToRead;
import com.wistron.demo.tool.teddybear.scene.table.EmailTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by king on 16-9-3.
 */

public class EmailNotificationScene extends SceneBase {
    private static final String PREFERENCE_NAME_EMAIL_NOTIFICATION = "email_notification";
    private static final String PREFERENCE_KEY_START_TIME = "start_time";
    private static final String PREFERENCE_KEY_PREVIOUS_TIME = "previous_time";

    private final int mSameQueryInterval = 5; // minutes

    private String mFromTime;
    private String mRecipientFilter;
    private String mSubjectFilter;

    private List<EmailTable> mEmailList;
    private int mReadIndex = 0;

    private EmailReadTask mEmailReadTask;

    public EmailNotificationScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();
        mFromTime = null;
        mRecipientFilter = null;
        mSubjectFilter = null;

        try {
            sceneAction = null;
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_DATETIME_TIME.equals(entitiesType)
                        || LuisHelper.ENTITIES_TYPE_DATETIME_DATE.equals(entitiesType)) {
                    mFromTime = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY).toLowerCase();
                } else if (LuisHelper.ENTITIES_TYPE_EMAIL_RECIPIENTS_FILTER.equals(entitiesType)) {
                    mRecipientFilter = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY).toLowerCase();
                } else if (LuisHelper.ENTITIES_TYPE_EMAIL_SUBJECT_FILTER.equals(entitiesType)) {
                    mSubjectFilter = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY).toLowerCase();
                }
            }

            queryEmail();
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    @Override
    public void updateSttResult(String result) {
        super.updateSttResult(result);
        try {
            String action = null;
            JSONObject toJason = new JSONObject(result);
            if (toJason.get(LuisHelper.TAG_QUERY) != null) {
                action = toJason.getString(LuisHelper.TAG_QUERY);
            }

            if (action.equalsIgnoreCase("yes") || action.equalsIgnoreCase("ok")) {
                startToReadEmail();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mEmailReadTask != null) {
            mEmailReadTask.cancel(true);
            mEmailReadTask = null;
        }
    }

    private void queryEmail() {
        if (mEmailList != null) {
            mEmailList.clear();
            mReadIndex = 0;
        }

        StringBuilder builder = new StringBuilder();
        ArrayList<String> conditions = new ArrayList<String>();
        conditions.add(""); // for query string.
        if (!TextUtils.isEmpty(mRecipientFilter)) {
            builder.append("sender like ?");
            conditions.add("%" + mRecipientFilter.toLowerCase() + "%");
        }
        if (!TextUtils.isEmpty(mSubjectFilter)) {
            if (TextUtils.isEmpty(builder)) {
                builder.append("subject like ?");
            } else {
                builder.append(" and subject like ?");
            }
            conditions.add("%" + mSubjectFilter.toLowerCase() + "%");
        }
        // if test time is less one minute than last time, query with original start time; otherwise, query with last time.
        mFromTime = null;
        SharedPreferences emailNotificationPreference = context.getSharedPreferences(PREFERENCE_NAME_EMAIL_NOTIFICATION, Activity.MODE_PRIVATE);
        long startTime = emailNotificationPreference.getLong(PREFERENCE_KEY_START_TIME, 0);
        long previousTime = emailNotificationPreference.getLong(PREFERENCE_KEY_PREVIOUS_TIME, 0);
        long currentTime = System.currentTimeMillis();
        Log.i(TAG, "current time = " + currentTime + ", previousTime = " + previousTime);
        long intervalTime = (currentTime - previousTime) / (60 * 1000);
        Log.i(TAG, "current interval time = " + intervalTime);
        if (intervalTime >= mSameQueryInterval) {
            mFromTime = String.valueOf(previousTime);
            if (previousTime != startTime) {
                startTime = previousTime;
            }
            previousTime = currentTime;
        } else {
            mFromTime = String.valueOf(startTime);
            previousTime = currentTime;
        }
        // save new value
        SharedPreferences.Editor editor = emailNotificationPreference.edit();
        editor.putLong(PREFERENCE_KEY_START_TIME, startTime);
        editor.putLong(PREFERENCE_KEY_PREVIOUS_TIME, previousTime);
        editor.apply();
        editor.commit();

        // add time query
        if (!TextUtils.isEmpty(mFromTime)) {
            if (TextUtils.isEmpty(builder)) {
                builder.append("receivedate >= ?");
            } else {
                builder.append(" and receivedate >= ?");
            }
            conditions.add(mFromTime);
        }

        // self email account
        int decodeStyle = SceneCommonHelper.getSvaSpeakerRecognitionStyle();
        if (decodeStyle == SceneCommonHelper.SVA_SPEAKER_RECOGNITION_STYLE_UDM) {  // UDM
            // reserved
        } else {  // BT
            BtConfigDevice nearestPerson = BluetoothSearch.getInstance(context).getNearestPerson();
            String queryEmailAddress = "null";
            if (nearestPerson != null) {
                queryEmailAddress = nearestPerson.getEmailAddress();
            }
            if (TextUtils.isEmpty(builder)) {
                builder.append("recipient = ?");
            } else {
                builder.append(" and recipient = ?");
            }
            conditions.add(queryEmailAddress.toLowerCase());
        }

        conditions.set(0, builder.toString());
        Log.i(TAG, "query string: " + Arrays.toString(conditions.toArray(new String[conditions.size()])));

        mEmailList = DataSupport.where(conditions.toArray(new String[conditions.size()])).find(EmailTable.class);
        Date tempDate = new Date(Long.parseLong(mFromTime));
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm. MMMM,dd");
        Log.i(TAG, "date prefix ---> " + dateFormat.format(tempDate));

        StringBuilder resultContent = new StringBuilder(String.format(getString(R.string.luis_assistant_email_time_prefix), dateFormat.format(tempDate)));
        if (mEmailList.size() <= 0) {
            resultContent.append(getString(R.string.luis_assistant_email_size_empty));
        } else {
            resultContent.append(String.format(getString(R.string.luis_assistant_email_got), mEmailList.size()));
        }

        if (!TextUtils.isEmpty(mSubjectFilter)) {
            resultContent.append(String.format(getString(R.string.luis_assistant_email_subject_about), mSubjectFilter));
        }
        if (!TextUtils.isEmpty(mRecipientFilter)) {
            resultContent.append(String.format(getString(R.string.luis_assistant_email_sender_from), mRecipientFilter));
        }

        if (mEmailList.size() > 0) {
            resultContent.append(getString(R.string.luis_assistant_email_to_read_require));
        }

        updateLog(resultContent.toString());
        toSpeak(resultContent.toString(), false);

        if (!isSceneStopped && mEmailList.size() > 0) {
            ((SceneActivity) context).startToListenCmd(false, getString(R.string.luis_assistant_email_to_read_require));
        }
    }

    private void startToReadEmail() {
        mEmailReadTask = new EmailReadTask();
        mEmailReadTask.execute();
    }

    private class EmailReadTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String sender = mEmailList.get(mReadIndex).getSender();
            String subject = mEmailList.get(mReadIndex).getSubject();
            final String content = SceneCommonHelper.removeBlackChar(mEmailList.get(mReadIndex).getContent(), LanguageCodes.English);

            toSpeak(String.format(getString(R.string.luis_assistant_email_sender_from), sender)
                    + String.format(getString(R.string.luis_assistant_email_subject_about), subject), false);

            SplitContentToRead mSplitContentToRead = new SplitContentToRead(content, LanguageCodes.English);
            String contentSnippet;
            while (!TextUtils.isEmpty((contentSnippet = mSplitContentToRead.getSplitContentToRead()))
                    && !isSceneStopped) {
                toSpeak(contentSnippet, false);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mReadIndex++;
            if (!isSceneStopped) {
                if (mReadIndex >= mEmailList.size()) {
                    toSpeak(getString(R.string.luis_assistant_email_read_finish), false);
                } else {
                    toSpeak(getString(R.string.luis_assistant_email_read_next), false);
                    ((SceneActivity) context).startToListenCmd(false, getString(R.string.luis_assistant_email_read_next));
                }
            }
        }
    }
}
