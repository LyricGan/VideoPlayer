package com.lyricgan.media.video;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.lyricgan.media.video.controller.IVideoController;
import com.lyricgan.media.video.controller.VideoMediaPlayerLargeControllerView;
import com.lyricgan.media.video.controller.VideoMediaPlayerSmallControllerView;
import com.lyricgan.media.video.model.MediaPlayMode;
import com.lyricgan.media.video.model.MediaPlayerVideoQuality;
import com.lyricgan.media.video.model.MediaQualityBean;
import com.lyricgan.media.video.model.RelateVideoInfo;
import com.lyricgan.media.video.player.IMediaPlayerPlus;
import com.lyricgan.media.video.ui.MediaPlayerBufferingView;
import com.lyricgan.media.video.ui.MediaPlayerEventActionView;
import com.lyricgan.media.video.ui.MediaPlayerLoadingView;
import com.lyricgan.media.video.util.Constants;
import com.lyricgan.media.video.util.IPowerStateListener;
import com.lyricgan.media.video.util.MediaPlayerUtils;
import com.lyricgan.media.video.util.NetReceiver;
import com.lyricgan.media.video.util.PlayConfig;
import com.lyricgan.media.video.util.WakeLocker;

import java.lang.reflect.Constructor;
import java.util.List;

public class VideoPlayerView extends RelativeLayout implements IPowerStateListener {
    private Activity mActivity;
    private Window mWindow;

    protected MediaVideoView mMediaVideoView;

    // 全屏控制器界面
    private VideoMediaPlayerLargeControllerView mMediaPlayerLargeControllerView;
    // 小屏控制器界面
    private VideoMediaPlayerSmallControllerView mMediaPlayerSmallControllerView;
    // 缓冲界面
    private MediaPlayerBufferingView mMediaPlayerBufferingView;
    // 加载界面
    private MediaPlayerLoadingView mMediaPlayerLoadingView;
    // 事件界面
    private MediaPlayerEventActionView mMediaPlayerEventActionView;

    private PlayerViewCallback mPlayerViewCallback;

    private final int ORIENTATION_UNKNOWN = -2;
    private final int ORIENTATION_HORIZON = -1;
    private final int ORIENTATION_PORTRAIT_NORMAL = 0;
    private final int ORIENTATION_LANDSCAPE_REVERSED = 90;
    private final int ORIENTATION_PORTRAIT_REVERSED = 180;
    private final int ORIENTATION_LANDSCAPE_NORMAL = 270;

    private final boolean mNeedGesture = true;
    private final boolean mNeedLightGesture = false;
    private final boolean mNeedVolumeGesture = true;
    private final boolean mNeedSeekGesture = true;

    private volatile int mScreenOrientation = ORIENTATION_UNKNOWN;
    private volatile int mPlayMode = MediaPlayMode.FULLSCREEN;
    private volatile boolean mLockMode = false;
    private volatile boolean mScreenLockMode = false;

    private boolean mVideoReady = false;

    private final boolean mStartAfterPause = false;

    private int mPausePosition = 0;

    private OrientationEventListener mOrientationEventListener;

    //小屏模式layoutParams
    private ViewGroup.LayoutParams mLayoutParamWindowMode;
    //全屏模式layoutParams
    private ViewGroup.LayoutParams mLayoutParamFullScreenMode;

    //全屏控制器layoutParams
    private LayoutParams mMediaPlayerControllerViewLargeParams;
    //小屏控制器layoutParams
    private LayoutParams mMediaPlayerControllerViewSmallParams;

    private volatile boolean mWindowActived = false;

    private boolean mDeviceNaturalOrientationLandscape;
    private boolean mCanLayoutSystemUI;
    private boolean mDeviceNavigationBarExist;
    private int mFullScreenNavigationBarHeight;
    private int mDeviceNavigationType = MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_UNKNOWN;
    private int mDisplaySizeMode = MediaVideoView.MOVIE_RATIO_MODE_16_9;

    private NetReceiver mNetReceiver;
    private NetReceiver.NetStateChangedListener mNetChangedListener;
    private boolean mIsComplete = false;

    private final float mCurrentPlayingRatio = 1f;
    private final float mCurrentPlayingVolumeRatio = 1f;
    public static float MAX_PLAYING_RATIO = 4f;
    public static float MAX_PLAYING_VOLUME_RATIO = 3.0f;
    // add for replay
    private boolean mRecyclePlay = false;

    //    private RelativeLayout layoutPop;

    private final PlayConfig playConfig = PlayConfig.getInstance();
    private Context mContext;
    private IPowerStateListener mPowerStateListener;
    private int play_height;

    private List<RelateVideoInfo> mRelateVideoInfo_list;
    private RelateVideoInfo mRelateVideoInfo;

    public VideoPlayerView(Context context) {
        super(context);
        init(context, null, -1);
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, -1);
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public boolean hasNetwork() {
        return true;
    }

    private void init(Context context, AttributeSet attrs, int defStyle) throws IllegalArgumentException, NullPointerException {
        this.mContext = context;
        registerPowerReceiver();
        setPowerStateListener(this);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoPlayerView);

        play_height = typedArray.getInt(R.styleable.VideoPlayerView_layout_height, -1);
        if (play_height > 0) {
            play_height = MediaPlayerUtils.dip2px(getContext(), play_height);
        }
        int playmode = typedArray.getInt(R.styleable.VideoPlayerView_play_mode, MediaPlayMode.FULLSCREEN);
        if (playmode == 0) {
            this.mPlayMode = MediaPlayMode.FULLSCREEN;
        } else if (playmode == 1) {
            this.mPlayMode = MediaPlayMode.WINDOW;
        }
        this.mLockMode = typedArray.getBoolean(R.styleable.VideoPlayerView_lock_mode, false);
        typedArray.recycle();

        this.mActivity = (Activity) context;
        this.mWindow = mActivity.getWindow();

        this.setBackgroundColor(Color.BLACK);
        this.mDeviceNavigationBarExist = MediaPlayerUtils.hasNavigationBar(mWindow);
        this.mDeviceNaturalOrientationLandscape = (MediaPlayerUtils.getDeviceNaturalOrientation(mWindow) == MediaPlayerUtils.DEVICE_NATURAL_ORIENTATION_LANDSCAPE);
        this.mCanLayoutSystemUI = Build.VERSION.SDK_INT >= 16;
        if (mDeviceNavigationBarExist && MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
            this.mFullScreenNavigationBarHeight = MediaPlayerUtils.getNavigationBarHeight(mWindow);
            this.mDeviceNavigationType = MediaPlayerUtils.getDeviceNavigationType(mWindow);
        }

		/* 初始化UI组件 */
        ViewGroup rootView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.video_blue_media_player_view, null);
        this.mMediaVideoView = rootView.findViewById(R.id.ks_camera_video_view);
        this.mMediaPlayerBufferingView = rootView.findViewById(R.id.ks_camera_buffering_view);
        this.mMediaPlayerLoadingView = rootView.findViewById(R.id.ks_camera_loading_view);
        this.mMediaPlayerEventActionView = rootView.findViewById(R.id.ks_camera_event_action_view);
        this.mMediaPlayerLargeControllerView = rootView.findViewById(R.id.media_player_controller_view_large);
        this.mMediaPlayerSmallControllerView = rootView.findViewById(R.id.media_player_controller_view_small);

		/* 设置播放器监听器 */
        this.mMediaVideoView.setOnPreparedListener(mOnPreparedListener);
        this.mMediaVideoView.setOnBufferingUpdateListener(mOnPlaybackBufferingUpdateListener);
        this.mMediaVideoView.setOnCompletionListener(mOnCompletionListener);
        this.mMediaVideoView.setOnInfoListener(mOnInfoListener);
        this.mMediaVideoView.setOnErrorListener(mOnErrorListener);
        this.mMediaVideoView.setMediaPlayerController(mMediaPlayerPlus);

        this.mMediaVideoView.setFocusable(false);
        setPowerStateListener(this.mMediaVideoView);
        /* 设置playerVideoView UI 参数 */
        LayoutParams mediaPlayerVideoViewParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mediaPlayerVideoViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);

		/* 设置playerVideoView UI 参数 */
        LayoutParams mediaPlayerBufferingViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mediaPlayerBufferingViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        this.mMediaPlayerBufferingView.hide();

		/* 设置loading UI 参数 */
        LayoutParams mediaPlayerLoadingViewParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mediaPlayerLoadingViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        this.mMediaPlayerLoadingView.hide();

		/* 设置eventActionView UI 参数 */
        LayoutParams mediaPlayerEventActionViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mediaPlayerEventActionViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);

		/* 设置eventActionView callback */
        this.mMediaPlayerEventActionView.setCallback(new MediaPlayerEventActionView.EventActionViewCallback() {

            @Override
            public void onActionPlay() {
                mIsComplete = false;
                if (Constants.DEBUG) {
                    Log.i(Constants.LOG_TAG, "event action  view action play");
                }
                mMediaPlayerEventActionView.hide();
                mMediaPlayerLoadingView.hide();
                if (mMediaPlayerController != null) {
                    mMediaPlayerController.start();
                } else {
                    mMediaVideoView.start();
                }
            }

            @Override
            public void onActionReplay() {
                if (Constants.DEBUG) {
                    Log.d(Constants.LOG_TAG, "onActionReplay  playConfig.getVideoMode() = " + playConfig.getVideoMode());
                }
                switch (playConfig.getVideoMode()) {
                    case PlayConfig.SHORT_VIDEO_MODE:
                        playConfig.setInterruptMode(PlayConfig.INTERRUPT_MODE_PAUSE_RESUME);
                        break;
                    case PlayConfig.LIVE_VIDEO_MODE:
                        playConfig.setInterruptMode(PlayConfig.INTERRUPT_MODE_RELEASE_CREATE);
                        break;
                }
                mMediaPlayerEventActionView.hide();
                mIsComplete = false;
                if (mMediaPlayerController != null) {
                    mMediaPlayerController.start();
                } else {
                    mMediaVideoView.start();
                }
            }

            @Override
            public void onActionError() {
                mIsComplete = false;
                switch (playConfig.getVideoMode()) {
                    case PlayConfig.SHORT_VIDEO_MODE:
                        playConfig.setInterruptMode(PlayConfig.INTERRUPT_MODE_PAUSE_RESUME);
                        break;
                    case PlayConfig.LIVE_VIDEO_MODE:
                        playConfig.setInterruptMode(PlayConfig.INTERRUPT_MODE_RELEASE_CREATE);
                        break;
                }
                mMediaPlayerEventActionView.hide();
                mMediaPlayerLargeControllerView.hide();
                mMediaPlayerSmallControllerView.hide();
                mMediaPlayerLoadingView.show();
                mMediaVideoView.release(true);
                mMediaVideoView.setVideoPath(mVideoUrl);
            }

            @Override
            public void isHaveNext(RelativeLayout mxCompleteLayout, View mWaitLayout, LinearLayout mErrorLayout) {
                mIsComplete = false;
                mWaitLayout.setVisibility(View.GONE);
                mErrorLayout.setVisibility(View.GONE);
                if (mRelateVideoInfo_list != null && mRelateVideoInfo_list.size() > 1) {
                    for (int i = 0; i < mRelateVideoInfo_list.size(); i++) {
                        RelateVideoInfo relateVideoInfo = mRelateVideoInfo_list.get(i);
                        if (mRelateVideoInfo.getId() == relateVideoInfo.getId()) {
                            if (mRelateVideoInfo_list.size() > i + 1) {
                                //自动播放下一集
                                mxCompleteLayout.setVisibility(View.GONE);
                                mMediaPlayerLargeControllerView.setRelateVideoInfo(mRelateVideoInfo_list.get(i + 1));
                                mIsComplete = false;
                                playConfig.setInterruptMode(PlayConfig.INTERRUPT_MODE_PAUSE_RESUME);
                                mMediaVideoView.start();
                            } else {
                                mIsComplete = false;
                                mxCompleteLayout.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                } else {
                    mxCompleteLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onActionBack() {
                mIsComplete = false;
                if (Constants.DEBUG) {
                    Log.i(Constants.LOG_TAG, "event action  view action back");
                }
                mMediaPlayerController.onBackPressed(mPlayMode);
            }
        });

		/* 初始化:ControllerViewLarge */
        this.mMediaPlayerLargeControllerView.setMediaPlayerController(mMediaPlayerController);
        this.mMediaPlayerLargeControllerView.setHostWindow(mWindow); // 声音和亮度获取
        this.mMediaPlayerLargeControllerView.setDeviceNavigationBarExist(mDeviceNavigationBarExist);
        this.mMediaPlayerLargeControllerView.setNeedGestureDetector(mNeedGesture);
        this.mMediaPlayerLargeControllerView.setNeedGestureAction(mNeedLightGesture, mNeedVolumeGesture, mNeedSeekGesture);
        this.mMediaPlayerLargeControllerView.setMediaPlayerEventActionView(mMediaPlayerEventActionView);

        this.mMediaPlayerControllerViewLargeParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        this.mMediaPlayerControllerViewLargeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        this.mMediaPlayerControllerViewLargeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        //如果有虚拟按键，留出边距
        if (mDeviceNavigationBarExist && mCanLayoutSystemUI && mFullScreenNavigationBarHeight > 0) {
            if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
                mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
            } else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
                mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
            }
        }

		/* 初始化:ControllerViewSmall */
        this.mMediaPlayerSmallControllerView.setMediaPlayerController(mMediaPlayerController);
        this.mMediaPlayerSmallControllerView.setHostWindow(mWindow);
        this.mMediaPlayerSmallControllerView.setDeviceNavigationBarExist(mDeviceNavigationBarExist);
        this.mMediaPlayerSmallControllerView.setNeedGestureDetector(true);
        this.mMediaPlayerSmallControllerView.setNeedGestureAction(false, false, false);
        this.mMediaPlayerControllerViewSmallParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		/* 移除掉所有的view */
        removeAllViews();
        rootView.removeView(mMediaVideoView);
        rootView.removeView(mMediaPlayerBufferingView);
        rootView.removeView(mMediaPlayerLoadingView);
        rootView.removeView(mMediaPlayerEventActionView);
        rootView.removeView(mMediaPlayerLargeControllerView);
        rootView.removeView(mMediaPlayerSmallControllerView);

		/* 添加全屏或者是窗口模式初始状态下所需的view */
        addView(mMediaVideoView, mediaPlayerVideoViewParams);
        addView(mMediaPlayerBufferingView, mediaPlayerBufferingViewParams);
        addView(mMediaPlayerLoadingView, mediaPlayerLoadingViewParams);
        addView(mMediaPlayerEventActionView, mediaPlayerEventActionViewParams);

        if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
            addView(mMediaPlayerLargeControllerView, mMediaPlayerControllerViewLargeParams);
            mMediaPlayerLargeControllerView.show();
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
            addView(mMediaPlayerSmallControllerView, mMediaPlayerControllerViewSmallParams);
            mMediaPlayerSmallControllerView.show();
        }

        mMediaPlayerBufferingView.hide();
        mMediaPlayerLoadingView.hide();
        mMediaPlayerEventActionView.updateEventMode(MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_WAIT, "");
        mMediaPlayerEventActionView.show();

        getWindowOrFullScreenModeLayoutParamsNew();

        mNetReceiver = NetReceiver.getInstance();

        mNetChangedListener = new NetReceiver.NetStateChangedListener() {

            @Override
            public void onNetStateChanged(NetReceiver.NetState netCode) {
                switch (netCode) {
                    case NET_NO:
                        showNetDialog();
                        break;
                    case NET_2G:
                    case NET_3G:
                    case NET_4G:
                        showNetDialog();
                        break;
                    case NET_WIFI:
                        break;
                    case NET_UNKNOWN:
                        break;
                    default:
                        if (Constants.DEBUG) {
                            Log.i(Constants.LOG_TAG, "不知道什么情况~>_<~");
                        }
                        break;
                }
            }
        };
    }

    //正在播放时才弹出
    private void showNetDialog() {
        if (mMediaVideoView.isPlaying()) {
            hasNetwork();
        }
    }

    private void getWindowOrFullScreenModeLayoutParams() {
        if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
            mLayoutParamWindowMode = getLayoutParams();
        }
        try {
            Class<? extends LayoutParams> parentLayoutParamClazz = (Class<? extends LayoutParams>) getLayoutParams().getClass();
            Constructor<? extends LayoutParams> constructor = parentLayoutParamClazz.getDeclaredConstructor(int.class, int.class);
            mLayoutParamFullScreenMode = constructor.newInstance(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getWindowOrFullScreenModeLayoutParamsNew() {
        if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
            mLayoutParamWindowMode = getLayoutParams();
        }
        mLayoutParamWindowMode = new LayoutParams(LayoutParams.MATCH_PARENT, play_height);
        mLayoutParamFullScreenMode = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    private String mVideoUrl = null;

    private void setPowerStateListener(IPowerStateListener powerStateListener) {
        this.mPowerStateListener = powerStateListener;
    }

    public void showFillScreen() {
        boolean requestResult = requestPlayMode(MediaPlayMode.FULLSCREEN);
        if (requestResult) {
            doScreenOrientationRotate(ORIENTATION_LANDSCAPE_NORMAL);
            mPlayMode = MediaPlayMode.FULLSCREEN;
        }
    }

    public void setVideoUrl(String url) {
        this.mVideoUrl = url;
    }

    public String getVideoUrl() {
        return this.mVideoUrl;
    }

    public void play() {
        if (!TextUtils.isEmpty(this.mVideoUrl)) {
            play(mVideoUrl);
        }
    }

    public void play(String url) {
        setVideoUrl(url);
        if (mMediaVideoView != null) {
            mMediaVideoView.setVideoPath(url);
        }
    }

    public void pause() {
        if (mMediaVideoView != null) {
            mMediaVideoView.pause();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = false;
        if (mMediaPlayerEventActionView.isShowing()) {
            result = mMediaPlayerEventActionView.dispatchTouchEvent(ev);
        }
        if (result) {
            return true;
        }
        if (MediaPlayerUtils.isWindowMode(mPlayMode) && inSmallController(ev)) {
            result = mMediaPlayerSmallControllerView.dispatchTouchEvent(ev);
            if (result) {
                return true;
            }
        }
        if (MediaPlayerUtils.isFullScreenMode(mPlayMode) && inLargeController(ev)) {
            result = mMediaPlayerLargeControllerView.dispatchTouchEvent(ev);
            if (result) {
                return true;
            }
        }
        if (mVideoReady) {
            if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                return mMediaPlayerLargeControllerView.dispatchTouchEvent(ev);
            }
            if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
                return mMediaPlayerSmallControllerView.dispatchTouchEvent(ev);
            }
        }
        return true;
    }

    private boolean inSmallController(MotionEvent ev) {
        float top = mMediaPlayerSmallControllerView.getY();
        float bottom = top + mMediaPlayerSmallControllerView.getHeight();
        return ev.getY() > top && ev.getY() < bottom;
    }

    private boolean inLargeController(MotionEvent ev) {
        float top = mMediaPlayerLargeControllerView.getY();
        float bottom = top + mMediaPlayerLargeControllerView.getHeight();
        return ev.getY() > top && ev.getY() < bottom;
    }

    /**
     * 手势处理
     * <p/>
     * //     * @param ev
     *
     * @return
     */
    //    @Override
    //    public boolean dispatchTouchEvent(MotionEvent ev) {
    //
    //        boolean result = false;
    //        if (mMediaPlayerEventActionView.isShowing()) {
    //            result = mMediaPlayerEventActionView.dispatchTouchEvent(ev);
    //        }
    //
    //        if (result) {
    //            return true;
    //        }
    //
    //        if (mVideoReady) {
    //
    //            if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
    //                return mMediaPlayerLargeControllerView.dispatchTouchEvent(ev);
    //            }
    //            if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
    //                return mMediaPlayerSmallControllerView.dispatchTouchEvent(ev);
    //            }
    //        }
    //
    //        return true;
    //    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    // 按键的处理
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mScreenLockMode) {
                return true;
            }
            if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                if (mLockMode) {
                    if (mPlayerViewCallback != null) {
                        mPlayerViewCallback.onFinish(mPlayMode);
                    }
                } else {
                    mMediaPlayerController.onRequestPlayMode(MediaPlayMode.WINDOW);
                }
                return true;
            } else if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
                if (mPlayerViewCallback != null) {
                    mPlayerViewCallback.onFinish(mPlayMode);
                }
                return true;
            }
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU || event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
            if (mScreenLockMode) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void setPlayerViewCallback(PlayerViewCallback callback) {
        this.mPlayerViewCallback = callback;
        if (mMediaPlayerLargeControllerView != null) {
            this.mMediaPlayerLargeControllerView.setPlayerViewCallback(callback);
        }
    }

    public void setRecyclePlay(boolean recyclePlay) {
        this.mRecyclePlay = recyclePlay;
    }

    public int getPlayMode() {
        return this.mPlayMode;
    }

    public boolean requestPlayMode(int playMode) {
        if (mPlayMode == playMode) {
            return false;
        }
        // 请求全屏模式
        if (MediaPlayerUtils.isFullScreenMode(playMode)) {
            if (mLayoutParamFullScreenMode == null) {
                return false;
            }
            removeView(mMediaPlayerSmallControllerView);
            addView(mMediaPlayerLargeControllerView, mMediaPlayerControllerViewLargeParams);
            this.setLayoutParams(mLayoutParamFullScreenMode);
            mMediaPlayerLargeControllerView.show();
            mMediaPlayerSmallControllerView.hide();

            if (mPlayerViewCallback != null) {
                mPlayerViewCallback.hideViews();
            }
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            if (mDeviceNavigationBarExist) {
                MediaPlayerUtils.hideSystemUI(mWindow, true);
            }
            mPlayMode = playMode;
            return true;
        } else if (MediaPlayerUtils.isWindowMode(playMode)) {// 请求窗口模式
            if (mLayoutParamWindowMode == null) {
                return false;
            }
            removeView(mMediaPlayerLargeControllerView);
            addView(mMediaPlayerSmallControllerView, mMediaPlayerControllerViewSmallParams);
            this.setLayoutParams(mLayoutParamWindowMode);
            mMediaPlayerLargeControllerView.hide();
            mMediaPlayerSmallControllerView.show();

            if (mPlayerViewCallback != null) {
                mPlayerViewCallback.restoreViews();
            }
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            if (mDeviceNavigationBarExist) {
                MediaPlayerUtils.showSystemUI(mWindow, false);
            }
            mPlayMode = playMode;
            return true;
        }
        return false;
    }

    public void onResume() {
        if (Constants.DEBUG) {
            Log.d(Constants.LOG_TAG, "VideoPlayerView---onResume()");
        }
        mWindowActived = true;
        mPowerStateListener.onPowerState(Constants.APP_SHOWN);
        enableOrientationEventListener();
        WakeLocker.acquire(getContext());
        mNetReceiver.registerNetBroadCast(getContext());
        mNetReceiver.addNetStateChangeListener(mNetChangedListener);
    }

    public void onPause() {
        if (Constants.DEBUG) {
            Log.d(Constants.LOG_TAG, "VideoPlayerView---onPause()");
        }
        mPowerStateListener.onPowerState(Constants.APP_HIDDEN);
        mNetReceiver.removeNetStateChangeListener(mNetChangedListener);
        mNetReceiver.unRegisterNetBroadCast(getContext());

        mWindowActived = false;
        mPausePosition = mMediaPlayerController.getCurrentPosition();

        disableOrientationEventListener();
    }

    public void onDestroy() {
        if (Constants.DEBUG) {
            Log.d(Constants.LOG_TAG, "VideoPlayerView---onDestroy()");
        }
        mIsComplete = false;
        unregisterPowerReceiver();
        mMediaVideoView.stopPlayback();
        WakeLocker.release();
        onDestroyControllerView();
    }

    private void onDestroyControllerView() {
        mMediaPlayerSmallControllerView.hide();
        mMediaPlayerLargeControllerView.hide();
        mMediaPlayerSmallControllerView.updateVideoPlaybackState(false);
        mMediaPlayerLargeControllerView.updateVideoPlaybackState(false);
    }

    private void initOrientationEventListener(Context context) {
        if (null == context)
            return;
        if (null == mOrientationEventListener) {
            mOrientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {

                @Override
                public void onOrientationChanged(int orientation) {
                    int preScreenOrientation = mScreenOrientation;
                    mScreenOrientation = convertAngle2Orientation(orientation);
                    if (mScreenLockMode) {
                        return;
                    }
                    if (!mWindowActived) {
                        return;
                    }
                    if (preScreenOrientation == ORIENTATION_UNKNOWN || (mScreenOrientation == ORIENTATION_UNKNOWN || mScreenOrientation == ORIENTATION_HORIZON)) {
                        return;
                    }
                    if (preScreenOrientation != mScreenOrientation) {
                        if (!MediaPlayerUtils.checkSystemGravity(getContext())) {
                            return;
                        }
                        if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
                            if (mScreenOrientation == ORIENTATION_LANDSCAPE_NORMAL || mScreenOrientation == ORIENTATION_LANDSCAPE_REVERSED) {
                                if (!mLockMode) {
                                    boolean requestResult = requestPlayMode(MediaPlayMode.FULLSCREEN);
                                    if (requestResult) {
                                        doScreenOrientationRotate(mScreenOrientation);
                                    }
                                }
                            }
                        } else if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                            if (mScreenOrientation == ORIENTATION_PORTRAIT_NORMAL) {
                                if (!mLockMode) {
                                    boolean requestResult = requestPlayMode(MediaPlayMode.WINDOW);
                                    if (requestResult) {
                                        doScreenOrientationRotate(mScreenOrientation);
                                    }
                                }
                            } else if (mScreenOrientation == ORIENTATION_LANDSCAPE_NORMAL || mScreenOrientation == ORIENTATION_LANDSCAPE_REVERSED) {
                                doScreenOrientationRotate(mScreenOrientation);
                            }
                        }
                    }
                }
            };
            enableOrientationEventListener();
        }
    }

    private int convertAngle2Orientation(int angle) {
        int screenOrientation = ORIENTATION_HORIZON;
        if ((angle >= 315 && angle <= 359) || (angle >= 0 && angle < 45)) {
            screenOrientation = ORIENTATION_PORTRAIT_NORMAL;
            if (mDeviceNaturalOrientationLandscape) {
                screenOrientation = ORIENTATION_LANDSCAPE_NORMAL;
            }
        } else if (angle >= 45 && angle < 135) {
            screenOrientation = ORIENTATION_LANDSCAPE_REVERSED;
            if (mDeviceNaturalOrientationLandscape) {
                screenOrientation = ORIENTATION_PORTRAIT_NORMAL;
            }
        } else if (angle >= 135 && angle < 225) {
            screenOrientation = ORIENTATION_PORTRAIT_REVERSED;
            if (mDeviceNaturalOrientationLandscape) {
                screenOrientation = ORIENTATION_LANDSCAPE_REVERSED;
            }
        } else if (angle >= 225 && angle < 315) {
            screenOrientation = ORIENTATION_LANDSCAPE_NORMAL;
            if (mDeviceNaturalOrientationLandscape) {
                screenOrientation = ORIENTATION_PORTRAIT_REVERSED;
            }
        }
        return screenOrientation;
    }

    private void doScreenOrientationRotate(int screenOrientation) {
        switch (screenOrientation) {
            case ORIENTATION_PORTRAIT_NORMAL:
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case ORIENTATION_LANDSCAPE_REVERSED:
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                if (mDeviceNavigationBarExist && mFullScreenNavigationBarHeight <= 0 && MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                    this.mFullScreenNavigationBarHeight = MediaPlayerUtils.getNavigationBarHeight(mWindow);
                    this.mDeviceNavigationType = MediaPlayerUtils.getDeviceNavigationType(mWindow);
                    if (mCanLayoutSystemUI && mFullScreenNavigationBarHeight > 0) {
                        if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
                            mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
                        } else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
                            mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
                        }
                    }
                }
                break;
            case ORIENTATION_PORTRAIT_REVERSED:
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case ORIENTATION_LANDSCAPE_NORMAL:
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                if (mDeviceNavigationBarExist && mFullScreenNavigationBarHeight <= 0 && MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                    this.mFullScreenNavigationBarHeight = MediaPlayerUtils.getNavigationBarHeight(mWindow);
                    this.mDeviceNavigationType = MediaPlayerUtils.getDeviceNavigationType(mWindow);
                    if (mCanLayoutSystemUI && mFullScreenNavigationBarHeight > 0) {
                        if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
                            mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
                        } else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
                            mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
                        }
                    }
                }
                break;
        }
        mMediaVideoView.setVideoLayout(MediaVideoView.MOVIE_RATIO_MODE_DEFAULT);
    }

    private void enableOrientationEventListener() {
        if (mOrientationEventListener != null && mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    private void disableOrientationEventListener() {
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
            mScreenOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
        }
    }

    private void updateVideoInfo2Controller() {
        //        mMediaPlayerSmallControllerView.updateVideoTitle(getResources().getString(R.string.video_small_title_tv_default));

        //        mMediaPlayerLargeControllerView.updateVideoTitle(getResources().getString(R.string.video_small_title_tv_default));
        mMediaPlayerLargeControllerView.updateVideoQualityState(MediaPlayerVideoQuality.HD);
        mMediaPlayerLargeControllerView.updateVideoVolumeState();
        //        mMediaPlayerEventActionView.updateVideoTitle(getResources().getString(R.string.video_small_title_tv_default));
    }

    private void changeMovieRatio() {
        /*
         * if (mDisplaySizeMode >
		 * MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_ORIGIN) { mDisplaySizeMode
		 * = MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9; }
		 */
        if (mDisplaySizeMode > MediaVideoView.MOVIE_RATIO_MODE_4_3) {
            mDisplaySizeMode = MediaVideoView.MOVIE_RATIO_MODE_16_9;
        }
        mMediaVideoView.setVideoLayout(mDisplaySizeMode);
    }

    /**
     * 播放完成之后，显示重新播放
     */
    private void showCompleteListener() {
    }

    MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            if (Constants.DEBUG) {
                Log.e(Constants.LOG_TAG, "onPrepared()--mPausePosition:" + mPausePosition);
            }
            if (mIsComplete) {
                mMediaPlayerLargeControllerView.hide();
                mMediaPlayerSmallControllerView.hide();
//                mMediaPlayerEventActionView.updateEventMode(MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_COMPLETE, null);
                mMediaPlayerEventActionView.show();
                WakeLocker.release();
            }
            int duration = mMediaPlayerController.getDuration();
            Log.e(Constants.LOG_TAG, "onPrepared()--duration:" + duration);
            if (mPausePosition > 0 && duration > 0) {
                if (!mIsComplete) {
                    mMediaPlayerController.pause();
                    mMediaPlayerController.seekTo(mPausePosition);
                    mPausePosition = 0;
                }
            }
            if (!WakeLocker.isScreenOn(getContext()) && mMediaPlayerController.canPause()) {
                if (!mIsComplete) {
                    mMediaPlayerController.pause();
                }
            }
            updateVideoInfo2Controller();
            mMediaPlayerLoadingView.hide();

            if (!mIsComplete) {
                if (!mMediaVideoView.isNeedPauseAfterLeave()) {
                    mMediaVideoView.start();
                } else {
                    mMediaVideoView.setNeedPauseAfterLeave(false);
                }
            }
//            mMediaPlayerEventActionView.updateEventMode(MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_WAIT, null);
            mVideoReady = true;
            if (mPlayerViewCallback != null) {
                mPlayerViewCallback.onPrepared();
            }
            if (duration <= 0) {
                reopen();
            }
        }
    };

    MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (Constants.DEBUG) {
                Log.d(Constants.LOG_TAG, "onCompletion()---mRecyclePlay:" + mRecyclePlay);
            }
            if (mRecyclePlay) {
                mMediaPlayerEventActionView.hide();
                mMediaPlayerController.start();
            } else {
                mIsComplete = true;
                mMediaPlayerLargeControllerView.updateVideoPlaybackState(false);
                mMediaPlayerSmallControllerView.updateVideoPlaybackState(false);
                mMediaPlayerLargeControllerView.hide();
                mMediaPlayerSmallControllerView.hide();
                mMediaPlayerEventActionView.updateEventMode(MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_COMPLETE, null);
                mMediaPlayerEventActionView.show();
                WakeLocker.release();
            }
        }
    };

    MediaPlayer.OnInfoListener mOnInfoListener = new MediaPlayer.OnInfoListener() {

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                    if (Constants.DEBUG) {
                        Log.e(Constants.LOG_TAG, "MEDIA_INFO_METADATA_UPDATE:" + extra);
                    }
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:// 视频缓冲开始
                    if (Constants.DEBUG) {
                        Log.e(Constants.LOG_TAG, "MEDIA_INFO_BUFFERING_START");
                    }
                    mMediaPlayerBufferingView.show();
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:// 视频缓冲结束
                    if (Constants.DEBUG) {
                        Log.e(Constants.LOG_TAG, "MEDIA_INFO_BUFFERING_END");
                    }
                    mMediaPlayerBufferingView.hide();
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    MediaPlayer.OnBufferingUpdateListener mOnPlaybackBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent > 0 && percent <= 100) {
                if (MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                    mMediaPlayerLargeControllerView.updateVideoSecondProgress(percent);
                } else {
                    mMediaPlayerSmallControllerView.updateVideoSecondProgress(percent);
                }
            }
        }
    };

    MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if (Constants.DEBUG) {
                Log.e(Constants.LOG_TAG, "onError---what:" + what + ",extra:" + extra);
            }
            mMediaPlayerLargeControllerView.hide();
            mMediaPlayerSmallControllerView.hide();
            mMediaPlayerBufferingView.hide();
            mMediaPlayerLoadingView.hide();
            String extraMessage = what + "," + extra;
            if (extraMessage.contains("-10001")) {
                mMediaPlayerEventActionView.updateEventMode(MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_ERROR, extraMessage);
            } else {
                mMediaPlayerEventActionView.updateEventMode(MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_WAIT, extraMessage);
            }
            mMediaPlayerEventActionView.show();
            return true;
        }
    };

    public void setPlayConfig(boolean isStream, int interruptMode, int videoMode) {
        playConfig.setStream(isStream);
        playConfig.setInterruptMode(interruptMode);
        playConfig.setVideoMode(videoMode);
    }

    public interface PlayerViewTitleOption {
        void onRightTopClick();
    }

    public interface PlayerViewCallback {

        // 小屏切换全屏时的事件通知
        void hideViews();

        // 全屏切回小屏时的事件通知
        void restoreViews();

        // 当播放器初始化完成，可以进入播放时的事件通知
        void onPrepared();

        // 用户操作切换清晰度时的事件通知
        void onQualityChanged(int quality);

        // 用户操作切换剧集
        void onCourseChanged(int videoId);

        // PlayerView请求Activity销毁自身时事件通知
        void onFinish(int playMode);

        // PlayerView收到错误消息时的事件通知
        void onError(int errorCode, String errorMsg);
    }

    protected final IVideoController mMediaPlayerController = new IVideoController() {

        @Override
        public void start() {
            if (canStart()) {
                mMediaVideoView.start();
                WakeLocker.acquire(getContext());
            }
        }

        @Override
        public void pause() {
            if (canPause()) {
                mMediaVideoView.pause();
            }
        }

        @Override
        public int getDuration() {
            return mMediaVideoView.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            if (mIsComplete) {
                return getDuration();
            }
            return mMediaVideoView.getCurrentPosition();
        }

        @Override
        public void seekTo(int pos) {
            if (Constants.DEBUG) {
                Log.e(Constants.LOG_TAG, "IVideoController:seekTo()---pos:" + pos);
            }
            if (canSeekBackward() && canSeekForward()) {
                mMediaVideoView.seekTo(pos);
            }
        }

        @Override
        public boolean isPlaying() {
            return mMediaVideoView.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return mMediaVideoView.getBufferPercentage();
        }

        @Override
        public boolean canPause() {
            return mMediaVideoView.canPause();
        }

        @Override
        public boolean canSeekBackward() {
            return mMediaVideoView.canSeekBackward();
        }

        @Override
        public boolean canSeekForward() {
            return mMediaVideoView.canSeekForward();
        }

        @Override
        public int getAudioSessionId() {
            return 0;
        }

        @Override
        public int getPlayMode() {
            return mPlayMode;
        }

        @Override
        public void onRequestPlayMode(int requestPlayMode) {
            if (mPlayMode == requestPlayMode) {
                return;
            }
            if (mLockMode) {
                return;
            }
            if (MediaPlayerUtils.isFullScreenMode(requestPlayMode)) {// 请求全屏模式
                boolean requestResult = requestPlayMode(requestPlayMode);
                if (requestResult) {
                    doScreenOrientationRotate(ORIENTATION_LANDSCAPE_NORMAL);
                }
            } else if (MediaPlayerUtils.isWindowMode(requestPlayMode)) {// 请求窗口模式
                boolean requestResult = requestPlayMode(requestPlayMode);
                if (requestResult) {
                    doScreenOrientationRotate(ORIENTATION_PORTRAIT_NORMAL);
                }
            }
        }

        @Override
        public void onBackPressed(int playMode) {
            if (MediaPlayerUtils.isFullScreenMode(playMode)) {
                if (mLockMode) {
                    if (mPlayerViewCallback != null) {
                        mPlayerViewCallback.onFinish(playMode);
                    }
                } else {
                    mMediaPlayerController.onRequestPlayMode(MediaPlayMode.WINDOW);
                }
            } else if (MediaPlayerUtils.isWindowMode(playMode)) {
                if (mPlayerViewCallback != null) {
                    mPlayerViewCallback.onFinish(playMode);
                }
            }
        }

        @Override
        public void onControllerShow(int playMode) {
            if (Constants.DEBUG) {
                Log.d(Constants.LOG_TAG, "onControllerShow, playMode : " + playMode);
            }
        }

        @Override
        public void onControllerHide(int playMode) {
            if (Constants.DEBUG) {
                Log.d(Constants.LOG_TAG, "onControllerHide, playMode : " + playMode);
            }
        }

        @Override
        public void onRequestLockMode(boolean lockMode) {
            if (mScreenLockMode != lockMode) {
                mScreenLockMode = lockMode;
                if (lockMode) {// 加锁:屏幕操作锁

                } else {// 解锁:屏幕操作锁

                }
            }
        }

        @Override
        public boolean canStart() {
            return mMediaVideoView.canStart();
        }

        @Override
        public void onMovieRatioChange(int screenSize) {
            mMediaVideoView.setVideoLayout(screenSize);
        }
    };

    private final IMediaPlayerPlus mMediaPlayerPlus = new IMediaPlayerPlus() {
        @Override
        public void onPrepare() {
            mMediaPlayerLoadingView.setLoadingTip("");
            mMediaPlayerLoadingView.show();
            mMediaPlayerBufferingView.hide();
            mMediaPlayerEventActionView.hide();
        }

        @Override
        public void onPlay() {
            mMediaPlayerLoadingView.hide();
            mMediaPlayerBufferingView.hide();
            mMediaPlayerEventActionView.hide();
            mMediaPlayerLargeControllerView.updateVideoPlaybackState(true);
            mMediaPlayerSmallControllerView.updateVideoPlaybackState(true);
        }

        @Override
        public void onPause() {
            mMediaPlayerLoadingView.hide();
            mMediaPlayerBufferingView.hide();
            mMediaPlayerEventActionView.updateEventMode(MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_WAIT, null);
            mMediaPlayerEventActionView.show();
            mMediaPlayerLargeControllerView.updateVideoPlaybackState(false);
            mMediaPlayerSmallControllerView.updateVideoPlaybackState(false);
        }
    };

    @Override
    public void onPowerState(int state) {
        if (mPowerStateListener != null) {
            this.mPowerStateListener.onPowerState(state);
        }
    }

    /**
     * For power state
     */
    private void registerPowerReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mBatInfoReceiver, filter);
    }

    private void unregisterPowerReceiver() {
        if (mBatInfoReceiver != null) {
            try {
                mContext.unregisterReceiver(mBatInfoReceiver);
            } catch (Exception e) {
                if (Constants.DEBUG) {
                    Log.e(Constants.LOG_TAG, "unregisterReceiver mBatInfoReceiver failure :" + e.getCause());
                }
            }
        }
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (Constants.DEBUG) {
                    Log.d(Constants.LOG_TAG, "screen off");
                }
                if (mPowerStateListener != null) {
                    mPowerStateListener.onPowerState(Constants.POWER_OFF);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (Constants.DEBUG) {
                    Log.d(Constants.LOG_TAG, "screen on");
                }
                if (mPowerStateListener != null) {
                    if (isAppOnForeground()) {
                        mPowerStateListener.onPowerState(Constants.POWER_ON);
                    }
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                if (isAppOnForeground()) {
                    mPowerStateListener.onPowerState(Constants.USER_PRESENT);
                }
            }
        }
    };

    private boolean isAppOnForeground() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        return !TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(mContext.getPackageName());
    }

    public void reopen() {
        mMediaVideoView.setVideoPath(mVideoUrl);
    }

    public void setPlayerViewTitleOption(PlayerViewTitleOption listener) {
        mMediaPlayerSmallControllerView.setPlayerViewTitleOptionListener(listener);
    }

    public void updateTitle(String title) {
        mMediaPlayerLargeControllerView.updateVideoTitle(title);
    }

    //    @Override
    //    public boolean onKeyDown(int keyCode, KeyEvent event) {
    //        if (keyCode == KeyEvent.KEYCODE_BACK) {
    //            mMediaPlayerController.onBackPress(getPlayMode());
    //            return true;
    //        }
    //        return super.onKeyDown(keyCode, event);
    //    }

    protected void setQuality(List<MediaQualityBean> lst, MediaQualityBean curr) {
        mMediaPlayerLargeControllerView.setQuality(lst, curr);
    }

    protected void setRelateVideo(List<RelateVideoInfo> lst, RelateVideoInfo relateVideoInfo) {
        this.mRelateVideoInfo_list = lst;
        this.mRelateVideoInfo = relateVideoInfo;
        mMediaPlayerLargeControllerView.setRelateVideo(lst, relateVideoInfo);
    }

    protected void setPausePosition(Long position) {
        if (position != null) {
            mPausePosition = position.intValue();
        }
    }

    protected void hideEventActionView() {
        mMediaPlayerEventActionView.hide();
    }

    protected MediaVideoView getPlayerVideoView() {
        return mMediaVideoView;
    }
}
