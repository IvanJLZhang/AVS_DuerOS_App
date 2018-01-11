package com.wistron.demo.tool.teddybear.scene.helper;

import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;

/**
 * Created by king on 16-9-19.
 */

public class SplitContentToRead {
    private String content;
    private String mAudioLanguage;

    // for Long text
    private int mPlayPosition = -1;

    public SplitContentToRead(String content, String mAudioLanguage) {
        this.content = content;
        this.mAudioLanguage = mAudioLanguage;
        mPlayPosition = 0;
    }

    public String getSplitContentToRead() {
        String splitContent = null;
        int totalLength = content.length();

        if (mPlayPosition < totalLength) {
            int startPosition = mPlayPosition;
            int LENGTH_TO_READ = CommonHelper.mLanguageLimitationPair.get(mAudioLanguage);
            if (mPlayPosition + LENGTH_TO_READ >= totalLength) {
                splitContent = content.substring(mPlayPosition);
                mPlayPosition = totalLength;
            } else {
                int offset;
                for (offset = mPlayPosition + LENGTH_TO_READ; offset < totalLength; offset++) {
                    String temp = content.substring(offset, offset + 1);
                    if (CommonHelper.PUNCTUATION_CONTAINS.contains(temp)) {
                        offset++;
                        break;
                    }
                }
                if (offset >= totalLength) {
                    splitContent = content.substring(mPlayPosition);
                    mPlayPosition = totalLength;
                } else {
                    splitContent = content.substring(mPlayPosition, offset);
                    mPlayPosition = offset;
                }
            }
        }

        /*SpannableStringBuilder spannable = new SpannableStringBuilder(content);
        ForegroundColorSpan span = new ForegroundColorSpan(Color.BLUE);
        spannable.setSpan(span, startPosition, mPlayPosition, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        et_AnalyzeResult.setText(spannable);

        int scrollPosition = mPlayPosition;
        et_AnalyzeResult.setSelection(scrollPosition, scrollPosition); // auto scroll*/

        return splitContent;
    }
}
