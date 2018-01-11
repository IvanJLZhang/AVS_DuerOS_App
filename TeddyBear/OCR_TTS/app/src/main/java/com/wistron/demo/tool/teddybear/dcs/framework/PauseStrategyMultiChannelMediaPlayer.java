package com.wistron.demo.tool.teddybear.dcs.framework;

import com.wistron.demo.tool.teddybear.dcs.systeminterface.IMediaPlayer;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlatformFactory;
import com.wistron.demo.tool.teddybear.dcs.util.LogUtil;

import java.util.Iterator;
import java.util.Map;

/**
 * 暂停模式的播放控制策略
 * Created by ivanjlzhang on 17-9-22.
 */

public class PauseStrategyMultiChannelMediaPlayer extends BaseMultiChannelMediaPlayer {
    private static final String TAG = BaseMultiChannelMediaPlayer.class.getSimpleName();

    public PauseStrategyMultiChannelMediaPlayer(IPlatformFactory factory) {
        super(factory);
    }

    @Override
    protected void handlePlay(String channelName, IMediaPlayer.MediaResource mediaResource) {
        int priority = getPriorityByChannelName(channelName);
        LogUtil.d(TAG, "handlePlay-priority:" + priority);
        if (priority == UNKNOWN_PRIORITY) {
            return;
        }
        ChannelMediaPlayerInfo info = new ChannelMediaPlayerInfo();
        info.mediaPlayer = getMediaPlayer(channelName);
        info.priority = priority;
        info.channelName = channelName;
        info.mediaResource = mediaResource;
        currentPlayMap.put(priority, info);

        // 把比当前优先级低的播放器都暂停
        Iterator<Map.Entry<Integer, ChannelMediaPlayerInfo>> it = currentPlayMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ChannelMediaPlayerInfo> entry = it.next();
            ChannelMediaPlayerInfo value = entry.getValue();
            LogUtil.d(TAG, "handlePlay-value:" + value.priority);
            if (priority > value.priority) {
                LogUtil.d(TAG, "handlePlay-value-pause:" + value.priority);
                value.mediaPlayer.pause();
            }
        }
        findToPlay();
    }
}
