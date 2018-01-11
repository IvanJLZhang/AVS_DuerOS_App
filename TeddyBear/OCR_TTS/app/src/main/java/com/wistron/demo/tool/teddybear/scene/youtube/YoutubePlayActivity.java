package com.wistron.demo.tool.teddybear.scene.youtube;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import java.util.Arrays;
import java.util.List;

public class YoutubePlayActivity extends YouTubeBaseActivity implements
        YouTubePlayer.OnInitializedListener {
    private static final String TAG = "YoutubePlayActivity";
    public static final String EXTRA_PLAY_VIDEOS = "play_videos";
    private YouTubePlayer mYoutubePlayer;
    private List<String> mVideosList;
    private ToSpeak mToSpeak;

    // receiver the broadcast action for Scenario done
    private boolean isRegisteredDoneBroadcast = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_youtube_play);

        initial();
    }

    private void initial() {
        mVideosList = getIntent().getStringArrayListExtra(EXTRA_PLAY_VIDEOS);
        Log.i(TAG, Arrays.toString(mVideosList.toArray()));
        mToSpeak = ToSpeak.getInstance(this);

        YouTubePlayerView youTubeView = (YouTubePlayerView)
                findViewById(R.id.youtube_view);
        youTubeView.initialize(getString(SubscriptionKey.getYoutubeDeveloperKey()), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(CommonHelper.ACTION_KEYWORD_DETECTED);
        mFilter.addAction(CommonHelper.ACTION_SCENARIO_ACTION);
        registerReceiver(scenarioAction, mFilter);
        isRegisteredDoneBroadcast = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (isRegisteredDoneBroadcast) {
            unregisterReceiver(scenarioAction);
            isRegisteredDoneBroadcast = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mYoutubePlayer) {
            mYoutubePlayer.release();
        }
    }

    private BroadcastReceiver scenarioAction = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "finish the scenario from keyword_detected...");
            if (intent.getAction().equals(CommonHelper.ACTION_KEYWORD_DETECTED)) {
                mToSpeak.stop();
                finish();
            } else if (intent.getAction().equals(CommonHelper.ACTION_SCENARIO_ACTION)) {
                int action = intent.getIntExtra(CommonHelper.EXTRA_SCENARIO_ACTION, -1);
                if (mYoutubePlayer != null) {
                    switch (action) {
                        case CommonHelper.SCENARIO_ACTION_RESUME:
                            mYoutubePlayer.play();
                            break;
                        case CommonHelper.SCENARIO_ACTION_PAUSE:
                            mYoutubePlayer.pause();
                            break;
                        case CommonHelper.SCENARIO_ACTION_PREVIOUS:
                            if (mYoutubePlayer.hasPrevious()) {
                                mYoutubePlayer.previous();
                            } else {
                                mToSpeak.toSpeak(SceneCommonHelper.getString(context, R.string.luis_assistant_youtube_has_not_previous), true);
                            }
                            break;
                        case CommonHelper.SCENARIO_ACTION_NEXT:
                            if (mYoutubePlayer.hasNext()) {
                                mYoutubePlayer.next();
                            } else {
                                mToSpeak.toSpeak(SceneCommonHelper.getString(context, R.string.luis_assistant_youtube_has_not_next), true);
                            }
                            break;
                        case CommonHelper.SCENARIO_ACTION_STOP:
                            finish();
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    };

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, boolean b) {
        Log.i(TAG, "onInitializationSuccess...");
        mYoutubePlayer = youTubePlayer;

        youTubePlayer.setFullscreen(true);
        youTubePlayer.setPlaylistEventListener(new YouTubePlayer.PlaylistEventListener() {
            @Override
            public void onPrevious() {
                Log.i(TAG, "YouTubePlayer onPrevious...");
            }

            @Override
            public void onNext() {
                Log.i(TAG, "YouTubePlayer onNext...");
            }

            @Override
            public void onPlaylistEnded() {
                Log.i(TAG, "finish the scenario from onPlaylistEnded...");
                finish();
            }
        });
        youTubePlayer.loadVideos(mVideosList);
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Log.i(TAG, "onInitializationFailure..." + youTubeInitializationResult);
        Toast.makeText(this, "Oh no! " + youTubeInitializationResult.toString(),
                Toast.LENGTH_LONG).show();
        mToSpeak.toSpeak(SceneCommonHelper.getString(this, R.string.luis_assistant_youtube_initial_failed), false);
        finish();
    }
}
