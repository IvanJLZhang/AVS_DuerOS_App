package com.wistron.demo.tool.teddybear.scene.google_stt_cloud;

import android.content.Context;
import android.util.Log;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1beta1.RecognitionConfig;
import com.google.cloud.speech.v1beta1.SpeechGrpc;
import com.google.cloud.speech.v1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1beta1.StreamingRecognitionResult;
import com.google.cloud.speech.v1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1beta1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;


public class GSapi {
    public static final String TAG = "GoogleCloudSTT";

    private Context mContext;

    public GSapi(Context context) {
        this.mContext = context;
    }

    public void setGSttListener(Listener listener) {
        mListener = listener;
    }

    /*
    * GSapi
    * */
    public interface Listener {
        /**
         * Called when a new piece of text was recognized by the Speech GSapi.
         *
         * @param text    The text.
         * @param isFinal {@code true} when the GSapi finished processing audio.
         */
        void onSpeechRecognized(String text, boolean isFinal);

        void stopRecognized();
    }

    private static final String HOSTNAME = "speech.googleapis.com";

    private static final int PORT = 443;

    //private final String TAG = "ApiFragment";

    private boolean isFinal = false;

    private String text = "";

    private static SpeechGrpc.SpeechStub mApi;

    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver
            = new StreamObserver<StreamingRecognizeResponse>() {

        @Override
        public void onNext(StreamingRecognizeResponse response) {
            isFinal = false;
            Log.i(TAG, "GSapi onNext.");
            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                isFinal = result.getIsFinal();
                if (result.getAlternativesCount() > 0 && isFinal) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            Log.i(TAG, "text = " + text + ", isFinal(not the result 'final') = " + isFinal);
//            if (mListener != null) {
//                mListener.onSpeechRecognized(text, isFinal);
//            }
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the GSapi.", t);
            mListener.stopRecognized();
        }

        @Override
        public void onCompleted() {
            Log.i("berlin", "4 finishRecognizing. isFinal = " + isFinal);
            Log.i(TAG, "GSapi completed. text = " + text);
            mListener.onSpeechRecognized(text, true);
        }
    };

    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;

    private Listener mListener = (Listener) mContext;

//    public void destorySTTChannel() {
//        mListener = null;
//        // Release the gRPC channel.
////        final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
//        if (channel != null && !channel.isShutdown()) {
//            channel.shutdownNow();
////                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
//            Log.e(TAG, "Error shutting down the gRPC channel. End!!!!!!");
//        }
//    }

    //    private static ManagedChannel channel = null;
    private static AccessToken lastAccessedToken = null;

    /**
     * Sets the {@link AccessToken} to be used to call the GSapi.
     *
     * @param accessToken The {@link AccessToken}.
     */
    public static void setAccessToken(AccessToken accessToken) {
        if (lastAccessedToken != null && lastAccessedToken.getTokenValue().equals(accessToken.getTokenValue())) {
            Log.i("berlin", "It is the same as the last GoogleSTTtoken.");
            return;
        }
        lastAccessedToken = accessToken;
        Log.i("berlin", "Create A new channel from the accesstoken.");
//        channel = new OkHttpChannelProvider()
        final ManagedChannel channel = new OkHttpChannelProvider()
                .builderForAddress(HOSTNAME, PORT)
                .nameResolverFactory(new DnsNameResolverProvider())
                .intercept(new GoogleCredentialsInterceptor(new GoogleCredentials(accessToken)
                        .createScoped(GSAccessTokenLoader.SCOPE)))
                .build();
        mApi = SpeechGrpc.newStub(channel);
    }

    /**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     */
    public void startRecognizing(int sampleRate, String language) {
        if (mApi == null) {
            Log.w(TAG, "GSapi not ready. Ignoring the request.");
            return;
        }
        text = "";
        isFinal = false;
        Log.i("berlin", "startRecognizing. ");
        // Configure the GSapi
        mRequestObserver = mApi.streamingRecognize(mResponseObserver);
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(language)
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRate(sampleRate)
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(true)
                        .build())
                .build());
    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the {@code data}.
     */
    public void recognize(byte[] data, int size) {
        if (mRequestObserver == null) {
            Log.i("berlin", "mRequestObserver = null");
            return;
        }
        // Call the streaming recognition GSapi
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());
    }

    /**
     * Finishes recognizing speech audio.
     */
    public void finishRecognizing() {
        Log.i("berlin", "finishRecognizing.");
        if (mRequestObserver == null) {
            Log.i("berlin", "mRequestObserver == null.");
            return;
        }
        Log.i("berlin", "2 finishRecognizing.");
        mRequestObserver.onCompleted();
        Log.i("berlin", "3 finishRecognizing.");
        mRequestObserver = null;
    }

    /**
     * Authenticates the gRPC channel using the specified {@link GoogleCredentials}.
     */
    private static class GoogleCredentialsInterceptor implements ClientInterceptor {

        private final Credentials mCredentials;

        private Metadata mCached;

        private Map<String, List<String>> mLastMetadata;

        GoogleCredentialsInterceptor(Credentials credentials) {
            mCredentials = credentials;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                final MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
                final Channel next) {
            return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {
                @Override
                protected void checkedStart(Listener<RespT> responseListener, Metadata headers)
                        throws StatusException {
                    Metadata cachedSaved;
                    URI uri = serviceUri(next, method);
                    synchronized (GoogleCredentialsInterceptor.this) {
                        Map<String, List<String>> latestMetadata = getRequestMetadata(uri);
                        if (mLastMetadata == null || mLastMetadata != latestMetadata) {
                            Log.i("berlin", "Google Credentials ---> set the mLasMetaData.");
                            mLastMetadata = latestMetadata;
                            mCached = toHeaders(mLastMetadata);
                        }
                        cachedSaved = mCached;
                    }
                    Log.i("berlin", "Google Credentials ---> merge. " + cachedSaved.toString());
                    headers.merge(cachedSaved);
                    delegate().start(responseListener, headers);
                }
            };
        }

        /**
         * Generate a JWT-specific service URI. The URI is simply an identifier with enough
         * information for a service to know that the JWT was intended for it. The URI will
         * commonly be verified with a simple string equality check.
         */
        private URI serviceUri(Channel channel, MethodDescriptor<?, ?> method)
                throws StatusException {
            String authority = channel.authority();
            if (authority == null) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Channel has no authority")
                        .asException();
            }
            // Always use HTTPS, by definition.
            final String scheme = "https";
            final int defaultPort = 443;
            String path = "/" + MethodDescriptor.extractFullServiceName(method.getFullMethodName());
            URI uri;
            try {
                uri = new URI(scheme, authority, path, null, null);
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI for auth")
                        .withCause(e).asException();
            }
            // The default port must not be present. Alternative ports should be present.
            if (uri.getPort() == defaultPort) {
                uri = removePort(uri);
            }
            return uri;
        }

        private URI removePort(URI uri) throws StatusException {
            try {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), -1 /* port */,
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI after removing port")
                        .withCause(e).asException();
            }
        }

        private Map<String, List<String>> getRequestMetadata(URI uri) throws StatusException {
            try {
                return mCredentials.getRequestMetadata(uri);
            } catch (IOException e) {
                throw Status.UNAUTHENTICATED.withCause(e).asException();
            }
        }

        private Metadata toHeaders(Map<String, List<String>> metadata) {
            Metadata headers = new Metadata();
            if (metadata != null) {
                for (String key : metadata.keySet()) {
                    Metadata.Key<String> headerKey = Metadata.Key.of(
                            key, Metadata.ASCII_STRING_MARSHALLER);
                    for (String value : metadata.get(key)) {
                        headers.put(headerKey, value);
                    }
                }
            }
            return headers;
        }

    }

}
