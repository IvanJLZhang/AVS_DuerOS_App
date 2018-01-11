package com.wistron.demo.tool.teddybear.avs;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.connection.Tls12SocketFactory;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.response.ResponseParser;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
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
 * Time：16-11-23 09:29
 * Author：bob
 */
public class SendLive {
    private Context mContext;
    DataRequestBody requestBody;
    //OkHttpClient for transfer of data
    Request.Builder mRequestBuilder = null;
    MultipartBody.Builder mBodyBuilder;

    private boolean isStopPreAction = false;

    public void setStopPreAction(boolean isStopPreAction) {
        this.isStopPreAction = isStopPreAction;
    }

    public SendLive(Context context) {
        mContext = context;
        Log.i(Common.TAG, "init SendLive");
    }

    private String getEventsUrl() {
        return new StringBuilder()
                .append(mContext.getString(com.willblaschko.android.alexa.R.string.alexa_api))
                .append("/")
                .append(mContext.getString(com.willblaschko.android.alexa.R.string.alexa_api_version))
                .append("/")
                .append("events")
                .toString();
    }

    private String getAccessToken() {
        String token = "";
        Log.i(Common.TAG, "Old access token: " + token);
        SharedPreferences preferences = mContext.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);
        token = preferences.getString(Common.PREF_ACCESS_TOKEN, token);

//        Log.i(Common.TAG,"Get access token from preference: " + token);
        return token;
    }

    private OkHttpClient getTLS12OkHttpClient() {
//        OkHttpClient.Builder client = new OkHttpClient.Builder();
        OkHttpClient.Builder client = new OkHttpClient().newBuilder();
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance
                        (TrustManagerFactory.getDefaultAlgorithm());
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
        client.readTimeout(30, TimeUnit.SECONDS);
        client.writeTimeout(30, TimeUnit.SECONDS);
        return client.build();
    }

    public void sendAudio(@NotNull DataRequestBody requestBody) throws IOException {
        mRequestBuilder = new Request.Builder();

        this.requestBody = requestBody;
        Log.i(Common.TAG, "Starting SpeechSendAudio procedure");
        long start = System.currentTimeMillis();

        //call the parent class's prepareConnection() in order to prepare our URL POST
        try {
            prepareConnection(getEventsUrl(), getAccessToken());

            addFormDataParts(mBodyBuilder);
            mRequestBuilder.post(mBodyBuilder.build());

            Request request = mRequestBuilder.build();

            Log.i(Common.TAG, "reset okHttpClient start");
            // Bob debug
//            OkHttpClient okHttpClient = getTLS12OkHttpClient();
            OkHttpClient okHttpClient = ClientUtil.getTLS12OkHttpClient();

            Log.i(Common.TAG, "reset okHttpClient stop");
            Call currentCall = okHttpClient.newCall(request);
            Log.i(Common.TAG, "prepare execute");

            Log.i(Common.TAG, "okHttpClient Connect Timeout: " + okHttpClient.connectTimeoutMillis());
            Log.i(Common.TAG, "okHttpClient Read Timeout: " + okHttpClient.readTimeoutMillis());
            Log.i(Common.TAG, "okHttpClient Write Timeout: " + okHttpClient.writeTimeoutMillis());

            Response response = currentCall.execute();
            Log.i(Common.TAG, "execute finish");

            if (isStopPreAction) {
                if (liveListener != null) {
                    liveListener.stopCurAction();
                }
                return;
            }

            int code = response.code();
            Log.i(Common.TAG, "response code: " + code);

            if (code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                Log.i(Common.TAG, "http internal error");
                if (liveListener != null) {
                    liveListener.sendfailure(500);
                }
            } else if (code == HttpURLConnection.HTTP_NO_CONTENT) {
                Log.i(Common.TAG, "nothing came back");
                if (liveListener != null) {
                    liveListener.sendfailure(204);
                }
            } else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                Log.i(Common.TAG, "http bad request");
                if (liveListener != null) {
                    liveListener.sendfailure(400);
                }
            } else if (code == HttpURLConnection.HTTP_OK) {
                final AvsResponse tmpResponse = ResponseParser.parseResponse(
                        response.body().byteStream(), getBoundary(response));
                Log.i(Common.TAG, "send request success: " + response.toString());
                handleResponse(tmpResponse);
            } else {
                Log.i(Common.TAG, "not recognition came back");
                if (liveListener != null) {
                    liveListener.sendfailure(0);
                }
            }

            response.body().close();
            Log.i(Common.TAG, "Audio sending process took: " + (System.currentTimeMillis() - start));
        } catch (IOException | AvsException | RuntimeException exp) {
            Log.i(Common.TAG, "current call IOException: " + exp.toString());
            if (liveListener != null) {
                liveListener.sendfailure(0);
            }
        }

        mRequestBuilder.delete();
    }

    private void handleResponse(AvsResponse response) {
        if (response != null) {
            Log.i(Common.TAG, "avsresponse is not null;");
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that
            // come
            //from doing that
            for (int i = response.size() - 1; i >= 0; i--) {
                if (response.get(i) instanceof AvsReplaceAllItem || response.get(i) instanceof
                        AvsReplaceEnqueuedItem) {
                    Log.i(Common.TAG, "response size: " + response.size());
                    //clear our queue
//                    avsQueue.clear();
                    //remove item
                    response.remove(i);
                }
            }
            try {
                AvsItem item = response.get(0);
                AvsSpeakItem avsSpeakItem = (AvsSpeakItem) item;
//                saveAudioFile(avsSpeakItem, "avs");
                SaveAndPlayAudio saveAndPlayAudio = new SaveAndPlayAudio(avsSpeakItem, "avs");
                saveAndPlayAudio.execute();

            } catch (Exception e) {
                e.printStackTrace();
                Log.i(Common.TAG, "change AvsSpeakItem error: " + e.toString());
                if (liveListener != null) {
                    liveListener.sendfailure(1);
                }
            }
        } else {
            Log.i(Common.TAG, "avsresponse is null;");
        }
    }

    private class SaveAndPlayAudio extends AsyncTask<Void, Void, Void> {
        private AvsSpeakItem item;
        private String name, path1;

        public SaveAndPlayAudio(AvsSpeakItem item, String name) {
            this.item = item;
            this.name = name;

            path1 = mContext.getExternalFilesDir(null).getAbsolutePath() + File.separator + name + ".mp3";
            Log.i(Common.TAG, "path: " + path1);

        }

        @Override
        protected Void doInBackground(Void... params) {
//            File path = new File(mContext.getExternalFilesDir(null), name + ".mp3");
            File path = new File(path1);
            if (path.exists()) {
                path.delete();
            }
            Log.i(Common.TAG, "save audio file");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path);
                fos.write(item.getAudio());
                fos.flush();
                fos.close();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Log.i(Common.TAG, "save audio file exception: " + e.toString());
                e.printStackTrace();
                if (liveListener != null) {
                    liveListener.saveAudioFail();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isStopPreAction) {
                //save success and play it
                playAudioFile(new File(path1));
            }
        }
    }

    ;

    private void saveAudioFile(AvsSpeakItem item, String name) {
        File path = new File(mContext.getExternalFilesDir(null), name + ".mp3");
        if (path.exists()) {
            path.delete();
        }
        Log.i(Common.TAG, "save audio file");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(item.getAudio());
            fos.flush();
            fos.close();
            if (!isStopPreAction) {
                //save success and play it
                playAudioFile(path);
            }
        } catch (IOException e) {
            Log.i(Common.TAG, "save audio file exception: " + e.toString());
            e.printStackTrace();
            if (liveListener != null) {
                liveListener.saveAudioFail();
            }
        }
    }

    private void playAudioFile(File audio) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i(Common.TAG, "play finish");
                mediaPlayer.release();
                mediaPlayer = null;
                if (liveListener != null) {
                    liveListener.playfinish();
                }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.i(Common.TAG, "play error");
                mediaPlayer.release();
                mediaPlayer = null;
                if (liveListener != null) {
                    liveListener.playerror();
                }
                return false;
            }
        });

        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(audio.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(Common.TAG, "mediaplayer set data source exception: " + e.toString());
        }
    }

    private void prepareConnection(String url, String accessToken) {

        //set the request URL
        mRequestBuilder.url(url);

        //set our authentication access token header
        mRequestBuilder.addHeader("Authorization", "Bearer " + accessToken);

        mBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", "metadata", RequestBody.create(MediaType.parse
                        ("application/json; charset=UTF-8"), getEvent()));

        //reset our output stream
//        mOutputStream = new ByteArrayOutputStream();
    }

    private String getEvent() {
        return getSpeechRecognizerEvent();
    }

    public static String getSpeechRecognizerEvent() {
        Event.Builder builder = new Event.Builder();
        builder.setHeaderNamespace("SpeechRecognizer")
                .setHeaderName("Recognize")
                .setHeaderMessageId(getUuid())
                .setHeaderDialogRequestId("dialogRequest-321")
                .setPayloadFormat("AUDIO_L16_RATE_16000_CHANNELS_1")
                .setPayloadProfile("CLOSE_TALK");
//                .setPayloadProfile("FAR_FIELD");
//                .setPayloadProfile("NEAR_FIELD");
        return builder.toJson();
    }

    private static String getUuid() {
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

    private void addFormDataParts(MultipartBody.Builder builder) {
        builder.addFormDataPart("audio", "speech.wav", getRequestBody());
    }

    private RequestBody getRequestBody() {
        return requestBody;
    }

    private AvsResponse parseResponse() throws IOException, AvsException, RuntimeException {
        Request request = mRequestBuilder.build();

        Call currentCall = ClientUtil.getTLS12OkHttpClient().newCall(request);
        Log.i(Common.TAG, "prepare execute");
        try {
            Response response = currentCall.execute();
            Log.i(Common.TAG, "execute finish");
            int code = response.code();
            Log.i(Common.TAG, "response code: " + code);
            if (code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                Log.i(Common.TAG, "http internal error");
            }

            final AvsResponse val = code == HttpURLConnection.HTTP_NO_CONTENT ? new AvsResponse() :
                    ResponseParser.parseResponse(response.body().byteStream(), getBoundary(response));

            response.body().close();
            return val;
        } catch (IOException exp) {
            Log.i(Common.TAG, "current call IOException: " + exp.toString());
            if (liveListener != null) {
                liveListener.sendfailure(0);
            }
            if (!currentCall.isCanceled()) {
                return new AvsResponse();
            }
        }
        return null;
    }

    protected String getBoundary(Response response) throws IOException {
        Headers headers = response.headers();
        String header = headers.get("content-type");
        String boundary = "";

        if (header != null) {
            Pattern pattern = Pattern.compile("boundary=(.*?);");
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                boundary = matcher.group(1);
            }
        } else {
            Log.i(Common.TAG, "Body: " + response.body().string());
        }
        return boundary;
    }


    public interface SendLiveListener {
        void sendfailure(int errorCode);

        void playfinish();

        void playerror();

        void saveAudioFail();

        void stopCurAction();
    }

    private SendLiveListener liveListener = null;

    public void setSendLiveListener(SendLiveListener liveListener) {
        this.liveListener = liveListener;
    }
}
