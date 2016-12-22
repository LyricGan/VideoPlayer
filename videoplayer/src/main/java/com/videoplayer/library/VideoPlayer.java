package com.videoplayer.library;

import android.content.Context;
import android.util.AttributeSet;

import com.videoplayer.library.ui.VideoMediaPlayerView;

/**
 * @author lyric
 * @description
 * @time 2016/6/28 16:32
 */
public class VideoPlayer extends VideoMediaPlayerView {

    public VideoPlayer(Context context) {
        super(context);
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
