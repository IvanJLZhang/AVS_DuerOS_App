package com.wistron.demo.tool.teddybear.scene.useless;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.luis_scene.SceneBase;

import java.io.IOException;
import java.util.Random;

/**
 * Time：16-4-13 14:47
 * Author：bob
 */
public class PlayMusic extends SceneBase {
    private MediaPlayer player;
    private String mCurPath = null;

    public PlayMusic(Context context, Handler handler) {
        super(context, handler);
        player = new MediaPlayer();
    }

    @Override
    public void stop() {
        super.stop();
        stopPlay();
    }

    @Override
    public void simulate() {
        super.simulate();
        if (null == player) {
            playReset();
        }
//        startPlay();
        SceneCommonHelper.openLED();
        startPlay(isSpecial);
    }

    boolean isSpecial;

    public void onPreExecute(String path, boolean special) {
        mCurPath = path;
        isSpecial = special;
    }

    public void playReset() {
        player = new MediaPlayer();
    }

    public void stopPlay() {
        if (null != player) {
            player.stop();
            player.reset();
        }
    }

    private String getNativeMusic() {
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media
                .EXTERNAL_CONTENT_URI, null, null, null, null);
        String data = null;
        if (null != cursor && cursor.getCount() > 0) {
            int index = getRandom(cursor.getCount());
            Log.i("King", "cursor count = " + cursor.getCount() + ", randomIndex = " + index);
            cursor.moveToPosition(index);
            data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        }
        if (cursor != null) {
            cursor.close();
        }
        return data;
    }

    private int getRandom(int number) {
        Random random = new Random();
        return random.nextInt(number);
    }

    public void startPlay() {
        String path = getNativeMusic();
        if (null != path) {
            try {
                player.reset();
                player.setDataSource(path);
                player.prepare();
                player.start();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mMainHandler.obtainMessage(SceneActivity.MSG_UPDATE_LOG, "Play completion.").sendToTarget();
                    mp.reset();
                }
            });

            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.reset();
                    return false;
                }
            });
        } else {
            mMainHandler.obtainMessage(SceneActivity.MSG_UPDATE_LOG, "Can not found music file").sendToTarget();
        }
    }

    public void startPlay(boolean isSpecial) {
        String path = null;
        if (isSpecial) {
            path = mCurPath;
        } else {
            path = getNativeMusic();
        }
        if (null == path) {
            mMainHandler.obtainMessage(SceneActivity.MSG_UPDATE_LOG, "Can not found music file").sendToTarget();
            return;
        }
        try {
            player.reset();
            player.setDataSource(path);
            player.prepare();
            player.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mMainHandler.obtainMessage(SceneActivity.MSG_UPDATE_LOG, "Play completion.").sendToTarget();
                SceneCommonHelper.closeLED();
                mp.reset();
            }
        });

        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                SceneCommonHelper.closeLED();
                mp.reset();
                return false;
            }
        });
    }

    static class Music {
        String name;
        String path;
    }
}

