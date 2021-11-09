package com.lyricgan.app.samples;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lyricgan.app.samples.databinding.ActivityMainBinding;
import com.lyricgan.media.video.VideoPlayerView;
import com.lyricgan.media.video.model.MediaPlayMode;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

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
        if (binding != null) {
            binding.videoPlayerView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (binding != null) {
            binding.videoPlayerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding.videoPlayerView.onDestroy();
        }
    }

    private void initViews() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.videoPlayerView.setPlayerViewCallback(new VideoPlayerView.PlayerViewCallback() {
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
                    binding.videoPlayerView.requestPlayMode(MediaPlayMode.WINDOW);
                } else if (playMode == MediaPlayMode.WINDOW) {
                    onBackPressed();
                }
            }

            @Override
            public void onError(int errorCode, String errorMsg) {

            }
        });
        binding.videoPlayerView.play("http://mvvideo2.meitudata.com/572e1dbe4fe681155.mp4");
    }
}
