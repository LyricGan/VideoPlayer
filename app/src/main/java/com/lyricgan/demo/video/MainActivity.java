package com.lyricgan.demo.video;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lyricgan.demo.video.databinding.ActivityMainBinding;
import com.lyricgan.media.video.VideoPlayerView;
import com.lyricgan.media.video.model.MediaPlayMode;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (XXPermissions.isGranted(this, Permission.WRITE_EXTERNAL_STORAGE)) {
            initViews();
        } else {
            XXPermissions.with(this).permission(Permission.WRITE_EXTERNAL_STORAGE).request(new OnPermissionCallback() {
                @Override
                public void onGranted(List<String> permissions, boolean all) {
                    initViews();
                }

                @Override
                public void onDenied(List<String> permissions, boolean never) {
                    finish();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewBinding != null) {
            viewBinding.videoPlayerView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewBinding != null) {
            viewBinding.videoPlayerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewBinding != null) {
            viewBinding.videoPlayerView.onDestroy();
        }
    }

    private void initViews() {
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.videoPlayerView.setPlayerViewCallback(new VideoPlayerView.PlayerViewCallback() {
            @Override
            public void hideViews() {

            }

            @Override
            public void restoreViews() {

            }

            @Override
            public void onPrepared() {

            }

            @Override
            public void onQualityChanged(int quality) {

            }

            @Override
            public void onCourseChanged(int videoId) {

            }

            @Override
            public void onFinish(int playMode) {
                if (playMode == MediaPlayMode.FULLSCREEN) {
                    viewBinding.videoPlayerView.requestPlayMode(MediaPlayMode.WINDOW);
                } else if (playMode == MediaPlayMode.WINDOW) {
                    onBackPressed();
                }
            }

            @Override
            public void onError(int errorCode, String errorMsg) {

            }
        });
        viewBinding.videoPlayerView.play("http://mvvideo2.meitudata.com/572e1dbe4fe681155.mp4");
    }
}
