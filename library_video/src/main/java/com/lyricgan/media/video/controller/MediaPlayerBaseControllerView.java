package com.lyricgan.media.video.controller;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController.MediaPlayerControl;

import com.lyricgan.media.video.model.MediaPlayerMovieRatio;
import com.lyricgan.media.video.model.MediaQualityBean;
import com.lyricgan.media.video.model.RelateVideoInfo;
import com.lyricgan.media.video.ui.MediaPlayerEventActionView;
import com.lyricgan.media.video.VideoPlayerView;
import com.lyricgan.media.video.util.MediaPlayerUtils;

/**
 * Base controller, handle show/hide and gesture event
 */
public abstract class MediaPlayerBaseControllerView extends FrameLayout {
    // Gesture control
    private volatile boolean mNeedGesture = false;
    private volatile boolean mNeedGestureLight = false;
    private volatile boolean mNeedGestureVolume = false;
    private volatile boolean mNeedGestureSeek = false;
    // Timer
    private volatile boolean mEnableTicker = true;
    private volatile boolean mIsTickerStarted = false;
    // Duration
    protected static final int HIDE_TIMEOUT_DEFAULT = 5000;
    protected static final int TICKER_INTERVAL_DEFAULT = 1000;
    protected static final int MAX_VIDEO_PROGRESS = 1000;
    // Message type
    protected static final int MSG_SHOW = 0x10;
    protected static final int MSG_HIDE = 0x11;
    protected static final int MSG_TICKE = 0x12;
    // Gesture recognise
    private static final double RADIUS_SLOP = Math.PI * 1 / 4;
    // Gesture type
    private static final int GESTURE_NONE = 0x00;
    private static final int GESTURE_LIGHT = 0x01;
    private static final int GESTURE_VOLUME = 0x02;
    private static final int GESTURE_SEEK = 0x03;
    // State
    private volatile int mCurrentGesture = GESTURE_NONE;
    protected volatile boolean mVideoProgressTrackingTouch = false;
    protected boolean mDeviceNavigationBarExist = false;
    protected volatile boolean mScreenLock = false;
    // Default config
    protected MediaQualityBean mCurrentQuality;

    protected RelateVideoInfo mRelateVideoInfo;
    protected MediaPlayerMovieRatio mCurrentMovieRatio = MediaPlayerMovieRatio.WIDESCREEN;
    // Views
    protected LayoutInflater mLayoutInflater;
    protected Window mHostWindow;
    protected WindowManager.LayoutParams mHostWindowLayoutParams;
    protected MediaPlayerControl mMediaPlayerController;
    protected GestureDetector mGestureDetector;
    protected VideoPlayerView.PlayerViewCallback mPlayerViewCallback;
    protected MediaPlayerEventActionView mediaPlayerEventActionView;

    public MediaPlayerBaseControllerView(Context context) {
        this(context, null);
    }

    public MediaPlayerBaseControllerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaPlayerBaseControllerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public void setMediaPlayerEventActionView(MediaPlayerEventActionView mediaPlayerEventActionView) {
        this.mediaPlayerEventActionView = mediaPlayerEventActionView;
    }

    protected void startTimerTicker() {
        if (mIsTickerStarted) {
            return;
        }
        mIsTickerStarted = true;
        mHandler.removeMessages(MSG_TICKE);
        mHandler.sendEmptyMessage(MSG_TICKE);
    }

    protected void stopTimerTicker() {
        if (!mIsTickerStarted) {
            return;
        }
        mIsTickerStarted = false;
        mHandler.removeMessages(MSG_TICKE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
        initListeners();
    }

    private void hideGestureView() {
        if (mNeedGesture) {
            onHideSeekView();
        }
    }

    public void show() {
        show(HIDE_TIMEOUT_DEFAULT);
    }

    /**
     * 控制条显示时间
     * timeout = 0 一直显示
     *
     * @param timeout
     */
    public void show(int timeout) {
        mHandler.sendEmptyMessage(MSG_SHOW);
        mHandler.removeMessages(MSG_HIDE);
        if (timeout > 0) {
            Message msgHide = mHandler.obtainMessage(MSG_HIDE);
            mHandler.sendMessageDelayed(msgHide, timeout);
        }
    }

    public void hide() {
        mHandler.sendEmptyMessage(MSG_HIDE);
    }

    public void toggle() {
        if (isShowing()) {
            hide();
        } else {
            if (!mMediaPlayerController.isPlaying()) {
                show(0);
            } else {
                show();
            }
        }
    }

    public boolean isShowing() {
        if (getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }

    public void setNeedGestureDetector(boolean need) {
        this.mNeedGesture = need;
    }

    public void setNeedGestureAction(boolean needLightGesture, boolean needVolumeGesture, boolean needSeekGesture) {
        this.mNeedGestureLight = needLightGesture;
        this.mNeedGestureVolume = needVolumeGesture;
        this.mNeedGestureSeek = needSeekGesture;
    }

    public void setNeedTicker(boolean need) {
        this.mEnableTicker = need;
    }

    public void setMediaPlayerController(MediaPlayerControl mediaPlayerController) {
        mMediaPlayerController = mediaPlayerController;
    }

    public void setPlayerViewCallback(VideoPlayerView.PlayerViewCallback callback) {
        this.mPlayerViewCallback = callback;
    }

    public void setHostWindow(Window window) {
        if (window != null) {
            mHostWindow = window;
            mHostWindowLayoutParams = window.getAttributes();
        }
    }

    public void setDeviceNavigationBarExist(boolean deviceNavigationBarExist) {
        mDeviceNavigationBarExist = deviceNavigationBarExist;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mNeedGesture && !mScreenLock) {
                    if (mNeedGestureSeek) {
                        onGestureSeekBegin(mMediaPlayerController.getCurrentPosition(), mMediaPlayerController.getDuration());
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isShowing() && !mScreenLock) {
                    show();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mNeedGesture && !mScreenLock) {
                    if (mCurrentGesture == GESTURE_LIGHT) {
                        if (mNeedGestureLight) {
                        }
                    } else if (mCurrentGesture == GESTURE_VOLUME) {
                        if (mNeedGestureVolume) {
                        }
                    } else if (mCurrentGesture == GESTURE_SEEK) {
                        if (mNeedGestureSeek) {
                            onSeekTo();
                        }
                    }
                    mCurrentGesture = GESTURE_NONE;
                }
                break;
            default:
                break;
        }
        if (mNeedGesture) {
            if (mGestureDetector != null) {
                mGestureDetector.onTouchEvent(event);
            }
        }
        return true;
    }

    private void initialize() {
        mLayoutInflater = LayoutInflater.from(getContext());
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mCurrentGesture == GESTURE_NONE) {
                    toggle();
                }
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null || mScreenLock) {
                    return false;
                }
                float oldX = e1.getX();
                final double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
                int selfWidth = getMeasuredWidth();
                final double radius = distanceY / distance;

                if (Math.abs(radius) > RADIUS_SLOP) {
                    // for voice control
                    if (oldX > selfWidth / 2) {
                        if (!mNeedGestureVolume) {
                            return false;
                        }
                        if (mCurrentGesture == GESTURE_NONE || mCurrentGesture == GESTURE_VOLUME) {
                            mCurrentGesture = GESTURE_VOLUME;
                            if (!isShowing()) {
                                show();
                            }
                            onShowHide();
                            onShowVolumeControl();
                            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                            float totalVolumeDistance = getMeasuredHeight();
                            if (totalVolumeDistance <= 0) {
                                totalVolumeDistance = MediaPlayerUtils.getRealDisplayHeight(mHostWindow);
                            }
                            onGestureVolumeChange(distanceY, totalVolumeDistance / 4, audioManager);
                        }
                    }
                    // for light gesture
                    else {
                        if (!mNeedGestureLight)
                            return false;
                        if (mCurrentGesture == GESTURE_NONE || mCurrentGesture == GESTURE_LIGHT) {
                            mCurrentGesture = GESTURE_LIGHT;
                            if (!isShowing()) {
                                show();
                            }
                            onShowHide();
                            float totalLightDistance = getMeasuredHeight();
                            if (totalLightDistance <= 0) {
                                totalLightDistance = MediaPlayerUtils.getRealDisplayHeight(mHostWindow);
                            }
                            onGestureLightChange(distanceY, mHostWindow);
                        }
                    }
                }
                // for seek gesture
                else {
                    if (!mNeedGestureSeek) {
                        return false;
                    }
                    if (mCurrentGesture == GESTURE_NONE || mCurrentGesture == GESTURE_SEEK) {
                        mCurrentGesture = GESTURE_SEEK;
                        if (!isShowing()) {
                            show();
                        }
                        float totalSeekDistance = getMeasuredWidth();
                        if (totalSeekDistance <= 0) {
                            totalSeekDistance = MediaPlayerUtils.getRealDisplayWidth(mHostWindow);
                        }
                        onGestureSeekChange(-distanceX, totalSeekDistance);
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }
        });
        mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mScreenLock) {
                    return false;
                }
                if (mMediaPlayerController.isPlaying()) {
                    mMediaPlayerController.pause();
                } else {
                    mMediaPlayerController.start();
                }
                return true;
            }
        });
    }

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW:
                    startTimerTicker();
                    setVisibility(View.VISIBLE);
                    onShow();
                    break;
                case MSG_HIDE:
                    stopTimerTicker();
                    hideGestureView();
                    setVisibility(View.GONE);
                    onHide();
                    break;
                case MSG_TICKE:
                    if (mEnableTicker) {
                        onTimerTicker();
                    }
                    sendEmptyMessageDelayed(MSG_TICKE, TICKER_INTERVAL_DEFAULT);
                    break;
                default:
                    break;
            }
        }
    };

    public void setMediaQuality(MediaQualityBean quality) {
        this.mCurrentQuality = quality;
    }

    public MediaQualityBean getQuality() {
        return this.mCurrentQuality;
    }

    public void setRelateVideoInfo(RelateVideoInfo mRelateVideoInfo) {
        this.mRelateVideoInfo = mRelateVideoInfo;
    }

    public RelateVideoInfo getRelateVideoInfo() {
        return this.mRelateVideoInfo;
    }

    public void setMovieRatio(MediaPlayerMovieRatio movieRatio) {
        this.mCurrentMovieRatio = movieRatio;
    }

    public MediaPlayerMovieRatio getMovieRatio() {
        return this.mCurrentMovieRatio;
    }

    public abstract void initViews();

    public abstract void initListeners();

    public abstract void onShow();

    public abstract void onHide();

    public abstract void onTimerTicker();

    protected void onShowVolumeControl() {
    }

    // Added
    protected void onHideSeekView() {
    }

    protected void onGestureSeekBegin(int currentPosition, int duration) {
    }

    protected void onGestureVolumeChange(float distanceY, float v, AudioManager audioManager) {
    }

    protected void onGestureLightChange(float distanceY, Window mHostWindow) {
    }

    protected void onGestureSeekChange(float v, float totalSeekDistance) {
    }

    protected void onSeekTo() {
    }

    protected void onShowHide() {
    }
}
