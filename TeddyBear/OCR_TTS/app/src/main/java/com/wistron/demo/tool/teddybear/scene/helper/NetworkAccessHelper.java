package com.wistron.demo.tool.teddybear.scene.helper;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by king on 16-11-17.
 */

public class NetworkAccessHelper {
    public static final String IPAPI_QUERY_LOCATION = "https://ipapi.co/json/";
    public static final String GET_COORDINATE_FROM_CITY = "http://api.geonames.org/searchJSON?q=%1$s&maxRows=1&username=WksKing";

    public static String invokeNetworkGet(String url) {
        String result = null;

        HttpClient httpclient = new DefaultHttpClient();
        BufferedReader reader = null;
        try {
            HttpGet weatherRequest = new HttpGet(url);
            HttpResponse weatherResponse = httpclient.execute(weatherRequest);
            int weatherResponseCode = weatherResponse.getStatusLine().getStatusCode();
            Log.i("King", "NetworkAccessHelper invokeNetworkGet responseCode = " + weatherResponseCode);
            if (weatherResponseCode == 200) {
                HttpEntity entity = weatherResponse.getEntity();
                if (entity != null) {
                    StringBuilder outputBuilder = new StringBuilder();
                    reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
                    String line;
                    while (null != (line = reader.readLine())) {
                        outputBuilder.append(line).append("\n");
                    }
                    result = outputBuilder.toString().trim();
                }
            } else {
                Log.i("King", "NetworkAccessHelper invokeNetworkGet error reason = " + weatherResponse.getStatusLine().getReasonPhrase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i("King", "NetworkAccessHelper result = " + result);
        return result;
    }

    public static String invokeNetworkPost(String url, Map<String, String> headers, String params) {
        String result = null;
        HttpURLConnection mHttpURLConnection = null;
        try {
            URL accessUrl = new URL(url);
            mHttpURLConnection = (HttpURLConnection) accessUrl.openConnection();
            mHttpURLConnection.setRequestMethod("POST");
            if (headers != null) {
                Set<String> keys = headers.keySet();
                for (String key : keys) {
                    mHttpURLConnection.setRequestProperty(key, headers.get(key));
                }
            }
            mHttpURLConnection.setDoOutput(true);
            mHttpURLConnection.setDoInput(true);

            if (params != null) {
                DataOutputStream outputStream = new DataOutputStream(mHttpURLConnection.getOutputStream());
                outputStream.writeBytes(params);
                outputStream.flush();
                outputStream.close();
            }

            if (mHttpURLConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(mHttpURLConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    response.append(str).append("\n");
                }
                reader.close();

                result = response.toString().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mHttpURLConnection != null) {
                mHttpURLConnection.disconnect();
            }
        }
        return result;
    }

}
