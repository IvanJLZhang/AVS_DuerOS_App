package com.wistron.demo.tool.teddybear.google_assistant;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.google.assistant.embedded.v1alpha1.AudioInConfig;
import com.google.assistant.embedded.v1alpha1.AudioOutConfig;
import com.google.assistant.embedded.v1alpha1.ConverseConfig;
import com.google.assistant.embedded.v1alpha1.ConverseRequest;
import com.google.assistant.embedded.v1alpha1.ConverseResponse;
import com.google.assistant.embedded.v1alpha1.EmbeddedAssistantGrpc;
import com.google.protobuf.ByteString;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.stub.StreamObserver;

/**
 * Created by king on 17-5-17.
 */

public class GoogleAssistantClient {
    private final String TAG = GoogleAssistantClient.class.getSimpleName();

    // Audio constants.
    private static final int SAMPLE_RATE = 16000;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static AudioInConfig.Encoding ENCODING_INPUT = AudioInConfig.Encoding.LINEAR16;
    private static AudioOutConfig.Encoding ENCODING_OUTPUT = AudioOutConfig.Encoding.LINEAR16;
    private static final AudioInConfig ASSISTANT_AUDIO_REQUEST_CONFIG =
            AudioInConfig.newBuilder()
                    .setEncoding(ENCODING_INPUT)
                    .setSampleRateHertz(SAMPLE_RATE)
                    .build();
    private static final AudioOutConfig ASSISTANT_AUDIO_RESPONSE_CONFIG =
            AudioOutConfig.newBuilder()
                    .setEncoding(ENCODING_OUTPUT)
                    .setSampleRateHertz(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_STEREO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_OUT_MONO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_IN_MONO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final int SAMPLE_BLOCK_SIZE = 1024;

    // Google Assistant API constants.
    private static final String ASSISTANT_ENDPOINT = "embeddedassistant.googleapis.com";

    // gRPC client and stream observers.
    private String spokenRequestText = null;
    private EmbeddedAssistantGrpc.EmbeddedAssistantStub mAssistantService;
    private StreamObserver<ConverseRequest> mAssistantRequestObserver;
    private StreamObserver<ConverseResponse> mAssistantResponseObserver =
            new StreamObserver<ConverseResponse>() {
                @Override
                public void onNext(ConverseResponse value) {
                    switch (value.getConverseResponseCase()) {
                        case EVENT_TYPE:
                            Log.d(TAG, "converse response event: " + value.getEventType());
                            if (value.getEventType() == ConverseResponse.EventType.END_OF_UTTERANCE) {
                                Log.d(TAG, "end of utterance!!! to stop recording...");
                                endRecording();
                            }
                            break;
                        case RESULT:
                            String requestText = value.getResult().getSpokenRequestText();
                            Log.d(TAG, "converse Result: microphone_mode = " + value.getResult().getMicrophoneMode()
                                    + ", requestText = " + requestText);
                            if (!requestText.isEmpty()) {
                                spokenRequestText = requestText;
                                Log.i(TAG, "assistant request text: " + spokenRequestText);
                            }
                            break;
                        case AUDIO_OUT:
                            final ByteBuffer audioData =
                                    ByteBuffer.wrap(value.getAudioOut().getAudioData().toByteArray());
                            Log.d(TAG, "converse audio size: " + audioData.remaining());
                            mAudioTrack.write(audioData, audioData.remaining(), AudioTrack.WRITE_BLOCKING);
                            break;
                        case ERROR:
                            Log.e(TAG, "converse response error: " + value.getError());
                            break;
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "converse error:", t);
                    mToSpeak.toSpeak(context.getString(R.string.google_assistant_converse_error), true);
                    endRecording();
                }

                @Override
                public void onCompleted() {
                    Log.i(TAG, "assistant response finished");
                    if (TextUtils.isEmpty(spokenRequestText)) {
                        mToSpeak.toSpeak(context.getString(R.string.luis_assistant_cmd_google_stt_error7), true);
                    }
                }
            };

    // Audio playback and recording objects.
    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;
    private boolean isRequesting = false;

    // Auto stop recording
    private static final int AMPLITUDE_THRESHOLD = 1500;
    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
    private static final int MAX_SPEECH_LENGTH_MILLIS = 30 * 1000;
    private long mLastVoiceHeardMillis = Long.MAX_VALUE;
    private long mVoiceStartedMillis;

    // ToSpeak instance
    private Context context;
    private ToSpeak mToSpeak;
    private Handler mMainHandler;

    // Assistant Thread and Runnables implementing the push-to-talk functionality.
    private HandlerThread mAssistantThread;
    private Handler mAssistantHandler;
    private Runnable mStartAssistantRequest = new Runnable() {
        @Override
        public void run() {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mVoiceStartedMillis = System.currentTimeMillis();
            spokenRequestText = null;

            Log.i(TAG, "starting assistant request");
            mAudioRecord.startRecording();
            mAssistantRequestObserver = mAssistantService.converse(mAssistantResponseObserver);
            mAssistantRequestObserver.onNext(ConverseRequest.newBuilder().setConfig(
                    ConverseConfig.newBuilder()
                            .setAudioInConfig(ASSISTANT_AUDIO_REQUEST_CONFIG)
                            .setAudioOutConfig(ASSISTANT_AUDIO_RESPONSE_CONFIG)
                            .build()).build());
            mAssistantHandler.post(mStreamAssistantRequest);
        }
    };
    private Runnable mStreamAssistantRequest = new Runnable() {
        @Override
        public void run() {
            if (!isRequesting) {
                return;
            }

            ByteBuffer audioData = ByteBuffer.allocateDirect(SAMPLE_BLOCK_SIZE);
            int result =
                    mAudioRecord.read(audioData, audioData.capacity(), AudioRecord.READ_BLOCKING);
            if (result < 0) {
                Log.e(TAG, "error reading from audio stream:" + result);
                return;
            }

            final long now = System.currentTimeMillis();
            boolean isHearingVoice = isHearingVoice(audioData.array(), result);
            Log.i(TAG, "isHearingVoice = " + isHearingVoice);
            if (isHearingVoice) {
                if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                    mVoiceStartedMillis = now;
                }
                mLastVoiceHeardMillis = now;
                if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {  // MAX recording is 30s
                    Log.i(TAG, "Max record time reached, to stop recording");
                    //mLastVoiceHeardMillis = Long.MAX_VALUE;
                    endRecording();
                    return;
                }
            } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                    Log.i(TAG, "No sound detected after speaking, to stop recording");
                    //mLastVoiceHeardMillis = Long.MAX_VALUE;
                    endRecording();
                    return;
                }
            } else {
                if (now - mVoiceStartedMillis > SPEECH_TIMEOUT_MILLIS) {
                    Log.i(TAG, "No sound detected, to stop recording");
                    //mLastVoiceHeardMillis = Long.MAX_VALUE;
                    endRecording();
                    return;
                }
            }

            Log.d(TAG, "streaming ConverseRequest: " + result);
            mAssistantRequestObserver.onNext(ConverseRequest.newBuilder()
                    .setAudioIn(ByteString.copyFrom(audioData))
                    .build());
            mAssistantHandler.post(mStreamAssistantRequest);
        }
    };
    private Runnable mStopAssistantRequest = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "ending assistant request");
            mAssistantHandler.removeCallbacks(mStreamAssistantRequest);
            Log.i(TAG, "to stop recording...");
            mAudioRecord.stop();
            Log.i(TAG, "to re-start SVA service...");
            mMainHandler.sendEmptyMessage(SceneActivity.MSG_START_SVA_SERVICE);

            if (mAssistantRequestObserver != null) {
                mAssistantRequestObserver.onCompleted();
                mAssistantRequestObserver = null;
            }
            mAudioTrack.play();
        }
    };

    private boolean isHearingVoice(byte[] buffer, int size) {
        for (int i = 0; i < size - 1; i += 2) {
            // The buffer has LINEAR16 in little endian.
            int s = buffer[i + 1];
            if (s < 0) s *= -1;
            s <<= 8;
            s += Math.abs(buffer[i]);
            if (s > AMPLITUDE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    public GoogleAssistantClient(Context context, Handler mainHandler) {
        this.context = context;
        this.mMainHandler = mainHandler;
        initial();
    }

    private void initial() {
        mToSpeak = ToSpeak.getInstance(context);

        mAssistantThread = new HandlerThread("assistantThread");
        mAssistantThread.start();
        mAssistantHandler = new Handler(mAssistantThread.getLooper());

        int outputBufferSize = AudioTrack.getMinBufferSize(AUDIO_FORMAT_OUT_MONO.getSampleRate(),
                AUDIO_FORMAT_OUT_MONO.getChannelMask(),
                AUDIO_FORMAT_OUT_MONO.getEncoding());
        mAudioTrack = new AudioTrack.Builder()
                .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
                .setBufferSizeInBytes(outputBufferSize)
                .build();
        //mAudioTrack.play();
        int inputBufferSize = AudioRecord.getMinBufferSize(AUDIO_FORMAT_STEREO.getSampleRate(),
                AUDIO_FORMAT_STEREO.getChannelMask(),
                AUDIO_FORMAT_STEREO.getEncoding());
        mAudioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AUDIO_FORMAT_IN_MONO)
                .setBufferSizeInBytes(inputBufferSize)
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(ASSISTANT_ENDPOINT).build();
        /*mAssistantService = EmbeddedAssistantGrpc.newStub(channel)
                .withCallCredentials(MoreCallCredentials.from(ASSISTANT_CREDENTIALS));*/
        try {
            mAssistantService = EmbeddedAssistantGrpc.newStub(channel)
                    .withCallCredentials(MoreCallCredentials.from(
                            Credentials.fromResource(context, R.raw.google_assistant_credentials)
                    ));
        } catch (IOException | JSONException e) {
            Log.e(TAG, "error creating assistant service:", e);
        }
    }

    private void endRecording() {
        if (isRequesting) {
            isRequesting = false;
            mAssistantHandler.post(mStopAssistantRequest);
        }
    }

    public boolean isStop() {
        return !isRequesting;
    }

    public void start() {
        isRequesting = true;
        mAudioRecord.stop();
        mAudioTrack.stop();
        mAssistantHandler.post(mStartAssistantRequest);
    }

    public void stop() {
        isRequesting = false;
        mAssistantHandler.post(new Runnable() {
            @Override
            public void run() {
                mAssistantHandler.removeCallbacks(mStreamAssistantRequest);
                if (mAssistantRequestObserver != null) {
                    mAssistantRequestObserver.onCompleted();
                    mAssistantRequestObserver = null;
                }
                mAudioRecord.stop();
                mAudioTrack.stop();
                if (mToSpeak != null) {
                    mToSpeak.stop();
                }
            }
        });

    }

    public void destroy() {
        Log.d(TAG, "destroy");
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
        if (mToSpeak != null) {
            mToSpeak.stop();
            mToSpeak = null;
        }
        mAssistantHandler.post(new Runnable() {
            @Override
            public void run() {
                mAssistantHandler.removeCallbacks(mStreamAssistantRequest);
            }
        });
        mAssistantThread.quitSafely();
    }
}
