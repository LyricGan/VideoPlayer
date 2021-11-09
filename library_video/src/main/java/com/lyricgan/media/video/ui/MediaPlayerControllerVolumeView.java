package com.lyricgan.media.video.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.lyricgan.media.video.R;
import com.lyricgan.media.video.util.Constants;

import java.lang.ref.WeakReference;

public class MediaPlayerControllerVolumeView extends RelativeLayout {
    private static final int MAX_PROGRESS = 100;
    private static final int LEVEL_VOLUME = 100;
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private AudioManager mAudioManager;
    private Callback mCallback;
    // 记录一次有效手势滑动的总距离,改值是由于多次的delta累加而成,要大于一个基础阀值,才能真正实现效果
    private float mTotalDeltaVolumeDistance = 0;
    private float mTotalLastDeltaVolumePercentage = 0;

    private MediaPlayerBrightSeekBar mSeekBarVolumeProgress;
    private VolumeChangedReceiver mVolumeChangedReceiver;

    public interface Callback {
        void onVolumeProgressChanged(AudioManager audioManager, float percentage);
    }

    public MediaPlayerControllerVolumeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MediaPlayerControllerVolumeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaPlayerControllerVolumeView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        initAudioManager();
        View root = LayoutInflater.from(context).inflate(R.layout.blue_media_player_controller_volume_view, this);
        mSeekBarVolumeProgress = (MediaPlayerBrightSeekBar) root.findViewById(R.id.seekbar_volume_progress);
        mSeekBarVolumeProgress.setMax(MAX_PROGRESS);
        mSeekBarVolumeProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float percentage = (float) progress / seekBar.getMax();
                if (percentage < 0 || percentage > 1) {
                    return;
                }
                if (mAudioManager != null) {
                    if (mCallback != null) {
                        mCallback.onVolumeProgressChanged(mAudioManager, percentage);
                    }
                }
            }
        });
        performVolumeChange(getVolume());
    }

    private void initAudioManager() {
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    private void setVolume(int volume) {
        if (null != mAudioManager) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }
    }

    private int getVolume() {
        if (null != mAudioManager) {
            return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    private int getMaxVolume() {
        if (null != mAudioManager) {
            return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void onGestureVolumeChange(float deltaVolumeDistance, float totalVolumeDistance, AudioManager audioManager) {
        if (Constants.DEBUG) {
            Log.d(Constants.LOG_TAG, "onGestureVolumeChange()---" + deltaVolumeDistance + "---" + totalVolumeDistance);
        }
        mTotalDeltaVolumeDistance = mTotalDeltaVolumeDistance + deltaVolumeDistance;
        float minVolumeDistance = totalVolumeDistance / LEVEL_VOLUME;
        float minVolumePercentage = (float) 1 / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (Math.abs(mTotalDeltaVolumeDistance) >= minVolumeDistance) {
            float deltaVolumePercentage = mTotalDeltaVolumeDistance / totalVolumeDistance;
            mTotalLastDeltaVolumePercentage = mTotalLastDeltaVolumePercentage + deltaVolumePercentage;
            int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float curVolumePercentage = (float) curVolume / maxVolume;
            int newVolume = curVolume;
            float newVolumePercentage = curVolumePercentage + mTotalLastDeltaVolumePercentage;

            if (mTotalLastDeltaVolumePercentage > 0 && mTotalLastDeltaVolumePercentage > minVolumePercentage) {
                mTotalLastDeltaVolumePercentage = 0;
                newVolume++;
            } else if (mTotalLastDeltaVolumePercentage < 0 && mTotalLastDeltaVolumePercentage < -minVolumePercentage) {
                mTotalLastDeltaVolumePercentage = 0;
                newVolume--;
            } else {
                return;
            }
            newVolume = calculateVolume(newVolume, maxVolume, newVolumePercentage);
            performVolumeChange(newVolume);
            mTotalDeltaVolumeDistance = 0;
        }
    }

    private int calculateVolume(int newVolume, int maxVolume, float newVolumePercentage) {
        if (newVolume < 0) {
            newVolume = 0;
        } else if (newVolume > maxVolume) {
            newVolume = maxVolume;
        }
        if (newVolumePercentage < 0) {
            newVolumePercentage = 0.0f;
        } else if (newVolumePercentage > 1) {
            newVolumePercentage = 1.0f;
        }
        if (newVolumePercentage == 0.0) {
            newVolume = 0;
        } else if (newVolumePercentage > 0.0 && newVolumePercentage < 0.04) {
            newVolume = 1;
        } else if (newVolumePercentage >= 0.04 && newVolumePercentage < 0.10) {
            newVolume = 2;
        } else if (newVolumePercentage >= 0.10 && newVolumePercentage < 0.17) {
            newVolume = 3;
        } else if (newVolumePercentage >= 0.17 && newVolumePercentage < 0.23) {
            newVolume = 4;
        } else if (newVolumePercentage >= 0.23 && newVolumePercentage < 0.30) {
            newVolume = 5;
        } else if (newVolumePercentage >= 0.30 && newVolumePercentage < 0.38) {
            newVolume = 6;
        } else if (newVolumePercentage >= 0.38 && newVolumePercentage < 0.44) {
            newVolume = 7;
        } else if (newVolumePercentage >= 0.44 && newVolumePercentage < 0.51) {
            newVolume = 8;
        } else if (newVolumePercentage >= 0.51 && newVolumePercentage < 0.58) {
            newVolume = 9;
        } else if (newVolumePercentage >= 0.58 && newVolumePercentage < 0.64) {
            newVolume = 10;
        } else if (newVolumePercentage >= 0.64 && newVolumePercentage < 0.71) {
            newVolume = 11;
        } else if (newVolumePercentage >= 0.71 && newVolumePercentage < 0.79) {
            newVolume = 12;
        } else if (newVolumePercentage >= 0.79 && newVolumePercentage < 0.86) {
            newVolume = 13;
        } else if (newVolumePercentage >= 0.86 && newVolumePercentage < 0.92) {
            newVolume = 14;
        } else if (newVolumePercentage >= 0.92 && newVolumePercentage <= 1.0) {
            newVolume = 15;
        }
        return newVolume;
    }

    private void performVolumeChange(int volume) {
        if (Constants.DEBUG) {
            Log.d(Constants.LOG_TAG, "volume:" + volume);
        }
        setVolume(volume);
        int maxVolume = getMaxVolume();
        int progress = volume * MAX_PROGRESS / maxVolume;
        mSeekBarVolumeProgress.setMax(MAX_PROGRESS);
        mSeekBarVolumeProgress.setProgress(progress);
    }

    public void updateVolumeSeekBar() {
        performVolumeChange(getVolume());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerReceiver();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unRegisterReceiver();
    }

    private void registerReceiver() {
        mVolumeChangedReceiver = new VolumeChangedReceiver(this);
        IntentFilter filter = new IntentFilter() ;
        filter.addAction(VOLUME_CHANGED_ACTION);
        getContext().registerReceiver(mVolumeChangedReceiver, filter);
    }

    private void unRegisterReceiver() {
        if (mVolumeChangedReceiver != null) {
            getContext().unregisterReceiver(mVolumeChangedReceiver);
        }
    }

    /**
     * 音量变化监听广播接收类
     */
    private static class VolumeChangedReceiver extends BroadcastReceiver {
        private WeakReference<MediaPlayerControllerVolumeView> mReference;
        private int mVolume;

        public VolumeChangedReceiver(MediaPlayerControllerVolumeView volumeView) {
            mReference = new WeakReference<>(volumeView);
            mVolume = volumeView.getVolume();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (VOLUME_CHANGED_ACTION.equals(intent.getAction())) {
                MediaPlayerControllerVolumeView volumeView = mReference.get();
                int volume = volumeView.getVolume();
                if (mVolume != volume) {
                    volumeView.performVolumeChange(volume);
                    mVolume = volume;
                }
            }
        }
    }
}