package com.videoplayer.library.videoview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.MediaController.MediaPlayerControl;

import com.videoplayer.library.player.IMediaPlayerPlus;
import com.videoplayer.library.util.Constants;
import com.videoplayer.library.util.IPowerStateListener;
import com.videoplayer.library.util.PlayConfig;
import com.videoplayer.library.util.ScreenResolution;

public class MediaPlayerVideoView extends SurfaceView implements MediaPlayerControl, IPowerStateListener {
    private Uri mUri;
    private long mDuration;
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int STATE_SUSPEND = 6;
    private static final int STATE_RESUME = 7;
    private static final int STATE_SUSPEND_UNSUPPORTED = 8;

    public int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private int mVideoLayout = MOVIE_RATIO_MODE_DEFAULT;
    public static final int MOVIE_RATIO_MODE_DEFAULT = -1;
    public static final int MOVIE_RATIO_MODE_16_9 = 0;
    public static final int MOVIE_RATIO_MODE_4_3 = 1;
    public static final int MOVIE_RATIO_MODE_FULLSCREEN = 2;
    public static final int MOVIE_RATIO_MODE_ORIGIN = 3;

    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private OnCompletionListener mOnCompletionListener;
    private OnPreparedListener mOnPreparedListener;
    private OnErrorListener mOnErrorListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnInfoListener mOnInfoListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private IMediaPlayerPlus mMediaPlayerPlus;
    private int mCurrentBufferPercentage;
    private Context mContext;
    private boolean mHasPrepared = false;
    private PlayConfig mPlayConfig = PlayConfig.getInstance();
    private boolean mNeedUnlock;
    private boolean mIsTexturePowerEvent;
    private boolean mNeedPauseAfterLeave;
    private boolean mNeedAppShowProcess;
    private boolean mIsDismiss;
    private boolean mIsAppShowing;

    public MediaPlayerVideoView(Context context) {
        super(context);
        initVideoView(context);
    }

    public MediaPlayerVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        initVideoView(context);
    }

    public MediaPlayerVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initVideoView(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    public void setVideoLayout(int layout) {
        LayoutParams lp = getLayoutParams();
        Pair<Integer, Integer> res = ScreenResolution.getResolution(mContext);
        int windowWidth = res.first.intValue(), windowHeight = res.second.intValue();
        float windowRatio = windowWidth / (float) windowHeight;
        int sarNum = mVideoSarNum;
        int sarDen = mVideoSarDen;
        if (mVideoHeight > 0 && mVideoWidth > 0) {
            float videoRatio = ((float) (mVideoWidth)) / mVideoHeight;
            if (sarNum > 0 && sarDen > 0)
                videoRatio = videoRatio * sarNum / sarDen;
            mSurfaceHeight = mVideoHeight;
            mSurfaceWidth = mVideoWidth;
            if (layout == MediaPlayerVideoView.MOVIE_RATIO_MODE_16_9) {// 16:9
                float target_ratio = 16.0f / 9.0f;
                float dh = windowHeight;
                float dw = windowWidth;
                if (windowRatio < target_ratio) {
                    dh = dw / target_ratio;
                } else {
                    dw = dh * target_ratio;
                }
                lp.width = (int) dw;
                lp.height = (int) dh;
            } else if (layout == MediaPlayerVideoView.MOVIE_RATIO_MODE_4_3) {// 4:3
                float target_ratio = 4.0f / 3.0f;
                float source_height = windowHeight;
                float source_width = windowWidth;
                if (windowRatio < target_ratio) {
                    source_height = source_width / target_ratio;
                } else {
                    source_width = source_height * target_ratio;
                }
                lp.width = (int) source_width;
                lp.height = (int) source_height;
            } else if (layout == MediaPlayerVideoView.MOVIE_RATIO_MODE_ORIGIN &&
                    mSurfaceWidth < windowWidth && mSurfaceHeight < windowHeight) {
                lp.width = (int) (mSurfaceHeight * videoRatio);
                lp.height = mSurfaceHeight;
            } else if (layout == MediaPlayerVideoView.MOVIE_RATIO_MODE_FULLSCREEN) {
                lp.width = (windowRatio < videoRatio) ? windowWidth : (int) (videoRatio * windowHeight);
                lp.height = (windowRatio > videoRatio) ? windowHeight : (int) (windowWidth / videoRatio);
            } else {
                if (windowWidth < windowHeight) {
                    windowHeight = dip2px(200);// 与视频控件大小设置一致
                    windowRatio = windowWidth / (float) windowHeight;
                }
                lp.width = (windowRatio < videoRatio) ? windowWidth : (int) (videoRatio * windowHeight);
                lp.height = (windowRatio > videoRatio) ? windowHeight : (int) (windowWidth / videoRatio);
            }
            setLayoutParams(lp);
            getHolder().setFixedSize(mSurfaceWidth, mSurfaceHeight);
        }
        mVideoLayout = layout;
    }

    public int dip2px(float dipValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public int getVideoLayoutMode() {
        return mVideoLayout;
    }

    private void initVideoView(Context context) {
        mContext = context;
        mVideoWidth = 0;
        mVideoHeight = 0;
        mVideoSarNum = 0;
        mVideoSarDen = 0;
        getHolder().addCallback(mSHCallback);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
        if (context instanceof Activity) {
            ((Activity) context).setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }

    public boolean isSurfaceValid() {
        return (mSurfaceHolder != null && mSurfaceHolder.getSurface().isValid());
    }

    public void setVideoPath(String path) {
        if (Constants.DEBUG) {
            Log.e(Constants.LOG_TAG, "setVideoPath()---path:" + path);
        }
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        mUri = uri;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                    mCurrentState = STATE_IDLE;
                    mTargetState = STATE_IDLE;
                }
            }, "thread_release").start();
        }
    }

    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            return;
        }
        stopMusicService();

        release(false);
        try {
            mDuration = -1;
            mCurrentBufferPercentage = 0;
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            if (mUri != null) {
                mMediaPlayer.setDataSource(mUri.toString());
            }
            if (!mIsTexturePowerEvent) {
                if (!isSurfaceValid()) {
                    mSurfaceHolder = getHolder();
                }
            } else {
                mIsTexturePowerEvent = false;
            }
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            if (mMediaPlayerPlus != null) {
                mMediaPlayerPlus.onPrepare();
            }
            mCurrentState = STATE_PREPARING;
        } catch (Exception e) {
            e.printStackTrace();
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    private void stopMusicService() {
        Intent intent = new Intent("com.android.music.musicservicecommand");
        intent.putExtra("command", "pause");
        mContext.sendBroadcast(intent);
    }

    OnVideoSizeChangedListener mSizeChangedListener = new OnVideoSizeChangedListener() {

        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            if (Constants.DEBUG) {
                Log.d(Constants.LOG_TAG, "onVideoSizeChanged()");
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
        }
    };

    OnPreparedListener mPreparedListener = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            if (Constants.DEBUG) {
                Log.d(Constants.LOG_TAG, "onPrepared()");
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            setVideoLayout(MediaPlayerVideoView.MOVIE_RATIO_MODE_DEFAULT);

            mHasPrepared = true;
            mCurrentState = STATE_PREPARED;
            mTargetState = STATE_PLAYING;
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
        }
    };

    private final OnCompletionListener mCompletionListener = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            mPlayConfig.setInterruptMode(PlayConfig.INTERRUPT_MODE_FINISH);
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
        }
    };

    private final OnErrorListener mErrorListener = new OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            mPlayConfig.setInterruptMode(PlayConfig.INTERRUPT_MODE_ERROR);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                    return true;
                }
            }
            return true;
        }
    };

    private final OnBufferingUpdateListener mBufferingUpdateListener = new OnBufferingUpdateListener() {

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
            if (mOnBufferingUpdateListener != null) {
                mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
            }
        }
    };

    private final OnInfoListener mInfoListener = new OnInfoListener() {

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (mOnInfoListener != null) {
                mOnInfoListener.onInfo(mp, what, extra);
            }
            return true;
        }
    };

    private final OnSeekCompleteListener mSeekCompleteListener = new OnSeekCompleteListener() {

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            if (Constants.DEBUG) {
                Log.d(Constants.LOG_TAG, "OnSeekCompleteListener---onSeekComplete()");
            }
            if (mOnSeekCompleteListener != null) {
                mOnSeekCompleteListener.onSeekComplete(mp);
            }
        }
    };

    public void setMediaPlayerController(IMediaPlayerPlus mediaPlayerPlus) {
        mMediaPlayerPlus = mediaPlayerPlus;
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    public boolean isNeedPauseAfterLeave() {
        return mNeedPauseAfterLeave;
    }

    public void setNeedPauseAfterLeave(boolean needPauseAfterLeave) {
        this.mNeedPauseAfterLeave = needPauseAfterLeave;
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            mSurfaceHolder = holder;
            if (mMediaPlayer != null) {
                mMediaPlayer.setDisplay(mSurfaceHolder);
            }
            mSurfaceWidth = w;
            mSurfaceHeight = h;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceHolder = holder;
            switch (mPlayConfig.getInterruptMode()) {
                case PlayConfig.INTERRUPT_MODE_RELEASE_CREATE:
                    openVideo();
                    break;
                case PlayConfig.INTERRUPT_MODE_PAUSE_RESUME:{
                    if (mMediaPlayer != null) {
                        mMediaPlayer.setSurface(mSurfaceHolder.getSurface());
                        if (!mNeedPauseAfterLeave) {
                            start();
                        } else {
                            mNeedPauseAfterLeave = false;
                        }
                    } else {
                        openVideo();
                    }
                }
                    break;
                case PlayConfig.INTERRUPT_MODE_FINISH: {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.setSurface(mSurfaceHolder.getSurface());
                    }
                }
                    break;
                case PlayConfig.INTERRUPT_MODE_ERROR: {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.setSurface(mSurfaceHolder.getSurface());
                        if (!mNeedPauseAfterLeave) {
                            start();
                        } else {
                            mNeedPauseAfterLeave = false;
                        }
                    } else {
                        openVideo();
                    }
                }
                break;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCurrentState == STATE_PAUSED) {
                mNeedPauseAfterLeave = true;
            }
            switch (mPlayConfig.getInterruptMode()) {
                case PlayConfig.INTERRUPT_MODE_RELEASE_CREATE:
                    release(true);
                    break;
                case PlayConfig.INTERRUPT_MODE_PAUSE_RESUME:
                    pause();
                    break;
                case PlayConfig.INTERRUPT_MODE_FINISH:
                    break;
                case PlayConfig.INTERRUPT_MODE_ERROR:
                    break;
            }
        }
    };

    public void release(final boolean clearTargetState) {
        long current = System.currentTimeMillis();
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (clearTargetState) {
                mTargetState = STATE_IDLE;
            }
        }
        if (Constants.DEBUG) {
            Log.e(Constants.LOG_TAG, "MediaPlayerVideoView release cost :" + String.valueOf(System.currentTimeMillis() - current));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_CALL && keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            mIsDismiss = true;
        }
        if (isInPlaybackState() && isKeyCodeSupported) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                } else {
                    start();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP && mMediaPlayer.isPlaying()) {
                pause();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            if (mMediaPlayerPlus != null) {
                mMediaPlayerPlus.onPlay();
            }
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                if (mMediaPlayerPlus != null) {
                    mMediaPlayerPlus.onPause();
                }
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0) {
                return (int) mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            return (int) mDuration;
        }
        mDuration = -1;
        return (int) mDuration;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
            mMediaPlayerPlus.onPrepare();
            start();
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    protected boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public boolean canPause() {
        return isPlaying();
    }

    @Override
    public boolean canSeekBackward() {
        return (this.getDuration() > 0);
    }

    @Override
    public boolean canSeekForward() {
        return (this.getDuration() > 0);
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public boolean canStart() {
        return isInPlaybackState();
    }

    @Override
    public void onPowerState(int state) {
        switch (state) {
            case Constants.POWER_OFF:
                mIsTexturePowerEvent = true;
                if (mCurrentState == STATE_PAUSED) {
                    mNeedPauseAfterLeave = true;
                }
                switch (mPlayConfig.getInterruptMode()) {
                    case PlayConfig.INTERRUPT_MODE_RELEASE_CREATE:
                        release(true);
                        break;
                    case PlayConfig.INTERRUPT_MODE_PAUSE_RESUME:
                        pause();
                        break;
                    case PlayConfig.INTERRUPT_MODE_FINISH:
                        break;
                    case PlayConfig.INTERRUPT_MODE_ERROR:
                        break;
                }
                break;
            case Constants.POWER_ON:
                if (isKeyGuard()) {
                    mNeedUnlock = true;
                } else {
                    videoResumeWithoutUnlock();
                }
                break;
            case Constants.USER_PRESENT:
                if (mIsAppShowing) {
                    videoResumeWithUnlock();
                } else {
                    mNeedAppShowProcess = true;
                }
                break;
            case Constants.APP_SHOWN:
                mIsAppShowing = true;
                if (mNeedAppShowProcess) {
                    mNeedAppShowProcess = false;
                    videoResumeWithUnlock();
                }
                break;
            case Constants.APP_HIDDEN:
                mIsAppShowing = false;
                break;
            default:
                break;
        }
    }

    private void videoResumeWithoutUnlock() {
        switch (mPlayConfig.getInterruptMode()) {
            case PlayConfig.INTERRUPT_MODE_RELEASE_CREATE:
                openVideo();
                break;
            case PlayConfig.INTERRUPT_MODE_PAUSE_RESUME:
                stopMusicService();
                if (!mNeedPauseAfterLeave) {
                    start();
                } else {
                    mNeedPauseAfterLeave = false;
                }
                break;
            case PlayConfig.INTERRUPT_MODE_FINISH:
                break;
            case PlayConfig.INTERRUPT_MODE_ERROR:
                break;
        }
    }

    private void videoResumeWithUnlock() {
        if (mNeedUnlock) {
            mNeedUnlock = false;
            switch (mPlayConfig.getInterruptMode()) {
                case PlayConfig.INTERRUPT_MODE_RELEASE_CREATE:
                    openVideo();
                    break;
                case PlayConfig.INTERRUPT_MODE_PAUSE_RESUME:
                    stopMusicService();
                    if (!mNeedPauseAfterLeave) {
                        start();
                    } else {
                        mNeedPauseAfterLeave = false;
                    }
                    break;
                case PlayConfig.INTERRUPT_MODE_FINISH:
                    mMediaPlayer.setSurface(mSurfaceHolder.getSurface());
                    break;
                case PlayConfig.INTERRUPT_MODE_ERROR:
                    mMediaPlayer.setSurface(mSurfaceHolder.getSurface());
                    break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean isKeyGuard() {
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        return (km.isKeyguardSecure() || km.isKeyguardLocked());
    }
}