package com.wistron.demo.tool.teddybear.avs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.willblaschko.android.alexa.connection.Tls12SocketFactory;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

/**
 * Time：17-5-19 09:24
 * Author：bob
 */
public class TransportProtocol {

    private Context context;
    private static TransportProtocol instance = null;
    private static OkHttpClient mClient;


    public static TransportProtocol getInstance(Context context) {
        if (instance == null) {
            instance = new TransportProtocol(context);
        }
        return instance;
    }

    private TransportProtocol(Context context) {
        this.context = context;
        mClient = getTLS12OkHttpClient();
        establishDownchannel();
    }

    public OkHttpClient getClient() {
        return mClient;
    }

    private static OkHttpClient getTLS12OkHttpClient() {
        if (mClient == null) {
            OkHttpClient.Builder client = new OkHttpClient.Builder();
            if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
                try {
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init((KeyStore) null);
                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                        throw new IllegalStateException("Unexpected default trust managers:"
                                + Arrays.toString(trustManagers));
                    }

                    X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                    SSLContext sc = SSLContext.getInstance("TLSv1.2");
                    sc.init(null, null, null);

                    String[] enabled = sc.getSocketFactory().getDefaultCipherSuites();
                    String[] supported = sc.getSocketFactory().getSupportedCipherSuites();

                    client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), trustManager);

                    ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .cipherSuites(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384)
                            .build();

                    List<ConnectionSpec> specs = new ArrayList<>();
                    specs.add(cs);

                    client.connectionSpecs(specs);
                } catch (Exception exc) {
                    Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
                }
            }
            client.addNetworkInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request().newBuilder().addHeader("Connection", "close").build();
                    return chain.proceed(request);
                }
            });
            client.connectTimeout(30, TimeUnit.SECONDS);
            client.writeTimeout(30, TimeUnit.SECONDS);
            client.readTimeout(60, TimeUnit.MINUTES);
            mClient = client.build();
        }
        return mClient;
    }

    private String getAccessToken() {
        String accessToken = null;
        SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);
        if (preferences.contains(Common.PREF_ACCESS_TOKEN)) {
            accessToken = preferences.getString(Common.PREF_ACCESS_TOKEN, null);
        }

        return accessToken;
    }

    private void establishDownchannel() {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            Log.i(Common.TAG, "get access Token is null");
            return;
        }

        Request request = new Request.Builder().url("https://avs-alexa-na.amazon" +
                ".com/v20160207/directives")
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();
        Log.i(Common.TAG, "access Token: " + accessToken);

        Response response = null;
        try {
            response = mClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(Common.TAG, "exception: " + e.getMessage());
//            output("Connect Exception: " + e.getMessage());
        }
        if (response != null) {
            try {
                int codeStatus = response.code();
                Log.i(Common.TAG, "response code: " + codeStatus);
//                output("response code: " + codeStatus);
                Headers responseHeaders = response.headers();
                for (int i = 0; i < responseHeaders.size(); i++) {
                    Log.i(Common.TAG, "response headers: " + responseHeaders.name(i) + ": " +
                            responseHeaders.value(i));
//                    output("response headers: " + responseHeaders.name(i) + ": " + responseHeaders
//                            .value(i));
                }

                if (codeStatus == 200) {
                    //SynchronizeState event
                    synchronizeStateEvent(accessToken);
                }

                String tempResponse = response.body().string();
                Log.i(Common.TAG, "response body message: " + tempResponse);
//                output("response body message: " + tempResponse);

            } catch (IOException e) {
                e.printStackTrace();
                Log.i(Common.TAG, e.getMessage());
            }
            return;
        } else {
            Log.i(Common.TAG, "response is null");
//            output("response is null");
            return;
        }
    }

    private void synchronizeStateEvent(String accessToken) {
        MediaType media_type = MediaType.parse("application/json; charset=UTF-8");

        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(Headers.of(
                        "Content-Disposition",
                        "form-data; name=\"metadata\""),
                        RequestBody.create(media_type, Common.SYNC_STATE))
                .build();

        Request request = new Request.Builder()
                .url("https://avs-alexa-na.amazon.com/v20160207/events")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        Response response = null;
        try {
            response = mClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(Common.TAG, "exception: " + e.getMessage());
//            output("Connect Exception: " + e.getMessage());
        }

        if (response != null) {
            try {
                int codeStatus = response.code();
                Log.i(Common.TAG, "response code: " + codeStatus);
//                output("response code: " + codeStatus);
                Headers responseHeaders = response.headers();
                for (int i = 0; i < responseHeaders.size(); i++) {
                    Log.i(Common.TAG, "response headers: " + responseHeaders.name(i) + ": " +
                            responseHeaders.value(i));
//                    output("response headers: " + responseHeaders.name(i) + ": " + responseHeaders
//                            .value(i));
                }
                String tempResponse = response.body().string();
                Log.i(Common.TAG, "response body message: " + tempResponse);
//                output("response body message: " + tempResponse);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(Common.TAG, e.getMessage());
            }
        }
    }

    private void releaseClient() {
        if (mClient != null) {
        }
    }
}
