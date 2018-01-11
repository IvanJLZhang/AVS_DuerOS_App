package com.wistron.demo.tool.teddybear.scene.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

public class NetworkConnectionChangedReceiver extends BroadcastReceiver {
    private static boolean isNetworkUnavailable = true;

    @Override
    public void onReceive(final Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        boolean newAvailableState = false;
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (null != connectivityManager) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (null != activeNetworkInfo) {
                    newAvailableState = activeNetworkInfo.isConnected();
                }
            }
        }
        if ((newAvailableState ^ isNetworkUnavailable) && !newAvailableState) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void[] params) {
                    ToSpeak mToSpeak = ToSpeak.getInstance(context);
                    mToSpeak.stop();
                    mToSpeak.toSpeak(SceneCommonHelper.getString(context, R.string.luis_assistant_network_unavailable), true);
                    return null;
                }
            }.executeOnExecutor(SceneCommonHelper.mCachedThreadPool);
        }
        isNetworkUnavailable = newAvailableState;
    }
}
