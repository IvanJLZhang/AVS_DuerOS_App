package com.wistron.demo.tool.teddybear.google_assistant;

import android.content.Context;

import com.google.auth.oauth2.UserCredentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by king on 17-5-12.
 */

public class Credentials {
    //static final String CLIENT_ID = "362799477024-g4sv4rj9upt9luf8c87cp9s97o6c4pmq.apps.googleusercontent.com";
    //static final String CLIENT_SECRET = "WKRsv9EyGwakcytHK828Qj0L";
    //static final String REFRESH_TOKEN = "1/ZdI6OzgKtfdnpu-_cRAfmrSSRzMzPkh4K5Fr4p1wXTg";

    static UserCredentials fromResource(Context context, int resourceId)
            throws IOException, JSONException {
        InputStream is = context.getResources().openRawResource(resourceId);
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        JSONObject json = new JSONObject(new String(bytes, "UTF-8"));
        return new UserCredentials(
                json.getString("client_id"),
                json.getString("client_secret"),
                json.getString("refresh_token")
        );
    }

}
