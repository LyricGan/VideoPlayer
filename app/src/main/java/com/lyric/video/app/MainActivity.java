package com.lyric.video.app;

import android.app.Activity;
import android.os.Bundle;

import com.videoplayer.library.ui.VideoMediaPlayerView;

public class MainActivity extends Activity {
    private VideoMediaPlayerView mediaPlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayerView = findViewById(R.id.video_media_player_view);

        mediaPlayerView.play("http://mvvideo2.meitudata.com/572e1dbe4fe681155.mp4");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayerView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayerView.onDestroy();
    }
}
