package com.lyric.video.app;

import android.app.Activity;
import android.os.Bundle;

import com.videoplayer.library.VideoPlayer;

public class MainActivity extends Activity {
    private static final String TEST_URL = "http://mvvideo2.meitudata.com/572e1dbe4fe681155.mp4";
    private VideoPlayer videoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoPlayer = (VideoPlayer) findViewById(R.id.video_player);

        videoPlayer.play(TEST_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.onDestroy();
    }
}
