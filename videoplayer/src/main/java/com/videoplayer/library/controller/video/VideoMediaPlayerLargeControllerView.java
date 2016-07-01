package com.videoplayer.library.controller.video;

import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.videoplayer.library.R;
import com.videoplayer.library.controller.base.MediaPlayerBaseControllerView;
import com.videoplayer.library.model.MediaPlayMode;
import com.videoplayer.library.model.MediaPlayerVideoQuality;
import com.videoplayer.library.model.MediaQualityBean;
import com.videoplayer.library.model.RelateVideoInfo;
import com.videoplayer.library.model.RelatedVideoAdapter;
import com.videoplayer.library.ui.base.MediaPlayerControllerBrightView;
import com.videoplayer.library.ui.base.MediaPlayerControllerVolumeView;
import com.videoplayer.library.ui.base.MediaPlayerLockView;
import com.videoplayer.library.ui.base.MediaPlayerQualityPopupView;
import com.videoplayer.library.ui.base.MediaPlayerScreenSizePopupView;
import com.videoplayer.library.ui.base.MediaPlayerSeekView;
import com.videoplayer.library.ui.base.MediaPlayerVideoSeekBar;
import com.videoplayer.library.ui.base.MediaPlayerVolumeSeekBar;
import com.videoplayer.library.util.Constants;
import com.videoplayer.library.util.MediaPlayerUtils;

import java.util.List;

/**
 * 横屏控制页面
 */
public class VideoMediaPlayerLargeControllerView extends MediaPlayerBaseControllerView implements View.OnClickListener, MediaPlayerVolumeSeekBar.OnScreenShowListener, OnSystemUiVisibilityChangeListener {
    protected static final String TAG = VideoMediaPlayerLargeControllerView.class.getSimpleName();
    private RelativeLayout mControllerTopView;
    private RelativeLayout mBackLayout;
    private TextView mTitleTextView;

    private ImageView mVideoPlayImageView; // 播放暂停
    //    private MediaPlayerMovieRatioView mWidgetMovieRatioView;

    //	private ImageView mVideoSizeImageView;


    //    private LinearLayout mQualityLayout; // 视频清晰度切换
    //    private TextView mQualityTextView;

    private Context mContext;

    private RelativeLayout mVideoProgressLayout;
    //快进快退
    private MediaPlayerVideoSeekBar mSeekBar;
    private TextView mCurrentTimeTextView; // 当前时间
    private TextView mTotalTimeTextView; // 总时间
    private ImageView mScreenModeImageView;

    private MediaPlayerQualityPopupView mQualityPopup; // 清晰度

    private MediaPlayerLockView mLockView; // 锁屏
    private TextView video_top_switch_parts;
    private TextView video_top_rate;
    private ImageView changeScreenImage;
    private ImageView video_volume_ic;
    protected MediaPlayerScreenSizePopupView mScreenPopup;
    protected MediaPlayerControllerBrightView mControllerBrightView;
    protected MediaPlayerControllerVolumeView mWidgetVolumeControl;
    protected MediaPlayerSeekView mWidgetSeekView;
    private boolean isFirst;
    private ListView mRelateListview;
    private List relationList;
    private RelatedVideoAdapter relatedAdapter;
    private List qualitys;
    private int last_relative_position;
    // private MediaPlayerControllerVolumeView mWidgetControllerVolumeView;
    //声音控制
    // private ImageView mVideoRatioBackView;
    // private ImageView mVideoRatioForwardView;

    public VideoMediaPlayerLargeControllerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    public VideoMediaPlayerLargeControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public VideoMediaPlayerLargeControllerView(Context context) {
        super(context);
        this.mContext = context;
        mLayoutInflater.inflate(R.layout.video_blue_media_player_controller_large, this);

        initViews();
        initListeners();
    }

    @Override
    public void initViews() {
        mControllerTopView = (RelativeLayout) findViewById(R.id.controller_top_layout);
        mBackLayout = (RelativeLayout) findViewById(R.id.back_layout); // 返回
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);

        mVideoPlayImageView = (ImageView) findViewById(R.id.video_start_pause_image_view); // 播放控制

        video_top_switch_parts = (TextView) findViewById(R.id.video_top_switch_parts);
        video_top_rate = (TextView) findViewById(R.id.video_top_rate);
        changeScreenImage = (ImageView) findViewById(R.id.video_window_screen_image_view);
        video_volume_ic = (ImageView) findViewById(R.id.video_volume_ic);

        //		mVideoSizeImageView = (ImageView) findViewById(R.id.video_size_image_view); // 视频尺寸切换
        // mVideoRatioBackView = (ImageView) findViewById(R.id.video_fast_back_view);
        // mVideoRatioForwardView = (ImageView)findViewById(R.id.video_fast_forward_view);

        //        mQualityLayout = (LinearLayout) findViewById(R.id.video_quality_layout); // 分辨率切换layout
        //        mQualityTextView = (TextView) findViewById(R.id.tv_definition); // 分辨率切换

        //        mEpisodeTextView = (TextView) findViewById(R.id.tv_episode); //剧集
        mRelateListview = (ListView) findViewById(R.id.relatedlistview);

        mLockView = (MediaPlayerLockView) findViewById(R.id.widget_lock_view);
        mVideoProgressLayout = (RelativeLayout) findViewById(R.id.video_progress_layout);
        mSeekBar = (MediaPlayerVideoSeekBar) findViewById(R.id.video_seekbar);
        mCurrentTimeTextView = (TextView) findViewById(R.id.video_current_time_text_view);
        mTotalTimeTextView = (TextView) findViewById(R.id.video_total_time_text_view);
        mScreenModeImageView = (ImageView) findViewById(R.id.video_window_screen_image_view); // 大屏切小屏

        mQualityPopup = new MediaPlayerQualityPopupView(getContext());

        //        mScreenPopup = new MediaPlayerScreenSizePopupView(getContext()/*, mMediaPlayerController*/);
        // mWidgetLightView = (MediaPlayerBrightView) findViewById(R.id.widget_light_view); //亮度调节

        mControllerBrightView = (MediaPlayerControllerBrightView) findViewById(R.id.widge_control_light_view); // 新亮度调节
        //        mWidgetMovieRatioView = (MediaPlayerMovieRatioView) findViewById(R.id.widget_video_ratio_view);
        // mWidgetVolumeView = (MediaPlayerVolumeView)
        // findViewById(R.id.widget_volume_view); //声音调节 进度条相关
        mWidgetVolumeControl = (MediaPlayerControllerVolumeView) findViewById(R.id.widget_controller_volume);

        mWidgetSeekView = (MediaPlayerSeekView) findViewById(R.id.widget_seek_view);

        setOnSystemUiVisibilityChangeListener(this);

        //        relationList = new ArrayList<RelateVideoInfo>();
        relatedAdapter = new RelatedVideoAdapter(relationList, mContext, this.mRelateVideoInfo);
        //		Log.d("lixp", "170 mContext =" + relationList + ">>relationList=" + relationList);
        mRelateListview.setAdapter(relatedAdapter);
    }

    @Override
    public void initListeners() {
        mScreenModeImageView.setOnClickListener(this);
        // mVideoRatioBackView.setOnClickListener(this);
        // mVideoRatioForwardView.setOnClickListener(this);
        video_top_switch_parts.setOnClickListener(this);
        video_top_rate.setOnClickListener(this);
        mBackLayout.setOnClickListener(this);
        mVideoPlayImageView.setOnClickListener(this);
        //        mQualityLayout.setOnClickListener(this);
        //        mVideoSizeLayout.setOnClickListener(this);
        mTitleTextView.setOnClickListener(this);

        //		mVideoSizeImageView.setOnClickListener(this);

        video_volume_ic.setOnClickListener(this);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mVideoProgressTrackingTouch = false;

                int curProgress = seekBar.getProgress();
                int maxProgress = seekBar.getMax();
                if (curProgress >= 0 && curProgress <= maxProgress) {
                    float percentage = ((float) curProgress) / maxProgress;
                    int position = (int) (mMediaPlayerController.getDuration() * percentage);
                    mMediaPlayerController.seekTo(position);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mVideoProgressTrackingTouch = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isShowing()) {
                        show();
                    }
                }
            }
        });

        //清晰度
        mQualityPopup.setCallback(new MediaPlayerQualityPopupView.Callback() {
            @Override
            public void onQualitySelected(MediaQualityBean quality) {
                mQualityPopup.hide();
                //                video_top_rate.setText(quality.getQualityName());
                if (!quality.getQualityName().equals(mCurrentQuality.getQualityName())) {
                    video_top_rate.setSelected(true);
                    setMediaQuality(quality);
                }
            }

            @Override
            public void onPopupViewDismiss() {
                video_top_rate.setSelected(false);
                if (isShowing()) {
                    show();
                }
            }
        });

        mLockView.setCallback(new MediaPlayerLockView.ScreenLockCallback() {

            @Override
            public void onActionLockMode(boolean lock) {// 加锁或解锁
                mScreenLock = lock;
                ((IVideoController) mMediaPlayerController).onRequestLockMode(lock);
                show();
            }
        });
        mRelateListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // 切换剧集
                setRelateVideoInfo((RelateVideoInfo) relationList.get(position));
                mRelateListview.setVisibility(GONE);
            }
        });
    }

    @Override
    public void setMediaQuality(MediaQualityBean quality) {
        if (mPlayerViewCallback != null) {
            mPlayerViewCallback.onQualityChanged(quality.getQualityCode());
        }
    }

    @Override
    public void setRelateVideoInfo(RelateVideoInfo relateVideoInfo) {
        if (mPlayerViewCallback != null) {
            mPlayerViewCallback.onCourseChanged(relateVideoInfo.getId());
        }
    }

    @Override
    public void onTimerTicker() {
        long currentPosition = mMediaPlayerController.getCurrentPosition();
        long duration = mMediaPlayerController.getDuration();
        if (duration > 0 && currentPosition <= duration) {
            float percentage = ((float) currentPosition) / duration;
            if (percentage >= 0 && percentage <= 1) {
                int progress = (int) (percentage * mSeekBar.getMax());
                if (!mVideoProgressTrackingTouch) {
                    mSeekBar.setProgress(progress);
                }
                mCurrentTimeTextView.setText(MediaPlayerUtils.getVideoDisplayTime(currentPosition));
                mTotalTimeTextView.setText(MediaPlayerUtils.getVideoDisplayTime(duration));
            }
        }
    }

    @Override
    public void onShow() {
        ((IVideoController) mMediaPlayerController).onControllerShow(MediaPlayMode.PLAY_MODE_FULLSCREEN);
        //        mLockView.show();
        // 如果开启屏幕锁后,controller显示时把其他控件隐藏,只显示出LockView
        if (mScreenLock) {
            mControllerTopView.setVisibility(INVISIBLE);
            mVideoProgressLayout.setVisibility(INVISIBLE);
            mWidgetVolumeControl.setVisibility(INVISIBLE);
            mControllerBrightView.setVisibility(INVISIBLE);
        } else {
            mControllerTopView.setVisibility(VISIBLE);
            mVideoProgressLayout.setVisibility(VISIBLE);
            //亮度和声音进度条不呼出
            //            mWidgetVolumeControl.setVisibility(VISIBLE);
            //            mControllerBrightView.setVisibility(VISIBLE);
        }
        if (MediaPlayerUtils.isFullScreenMode(((IVideoController) mMediaPlayerController).getPlayMode())) {
            Log.d(TAG, "325  onShow....");
            //            MediaPlayerUtils.showSystemUI(mHostWindow, false);
        }
    }

    @Override
    public void onHide() {
        ((IVideoController) mMediaPlayerController).onControllerHide(MediaPlayMode.PLAY_MODE_FULLSCREEN);
        mControllerTopView.setVisibility(INVISIBLE);
        mVideoProgressLayout.setVisibility(INVISIBLE);
        mWidgetVolumeControl.setVisibility(INVISIBLE);
        mControllerBrightView.setVisibility(INVISIBLE);
        mRelateListview.setVisibility(GONE);

        if (mQualityPopup.isShowing()) {
            mQualityPopup.hide();
        }
        // 当前全屏模式,隐藏系统UI
        if (mDeviceNavigationBarExist) {
            if (MediaPlayerUtils.isFullScreenMode(((IVideoController) mMediaPlayerController).getPlayMode())) {
                MediaPlayerUtils.hideSystemUI(mHostWindow, false);
            }
        }
        mLockView.hide();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

	/*
     * @Override public boolean dispatchKeyEvent(KeyEvent event) {
	 * 
	 * return mWidgetControllerVolumeView.dispatchKeyEvent(event); }
	 */

    public void updateVideoTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTitleTextView.setText(title);
        }
    }

    //视频播放时间
    public void updateVideoProgress(float percentage) {
        if (Constants.DEBUG) {
            Log.d(Constants.LOG_TAG, "percentage = " + percentage);
        }
        if (percentage >= 0 && percentage <= 1) {
            int progress = (int) (percentage * mSeekBar.getMax());
            if (!mVideoProgressTrackingTouch)
                mSeekBar.setProgress(progress);

            long curTime = mMediaPlayerController.getCurrentPosition();
            long durTime = mMediaPlayerController.getDuration();
            if (durTime > 0 && curTime <= durTime) {
                mCurrentTimeTextView.setText(MediaPlayerUtils.getVideoDisplayTime(curTime));
                mTotalTimeTextView.setText(MediaPlayerUtils.getVideoDisplayTime(durTime));
            }
        }
    }

    public void updateVideoSecondProgress(int percent) {
        long duration = mMediaPlayerController.getDuration();
        long progress = duration * percent / 100;

        if (duration > 0 && !isFirst) {
            mSeekBar.setMax((int) duration);
            mSeekBar.setProgress(0);
            isFirst = true;
        }
        mSeekBar.setSecondaryProgress((int) progress);
    }

    public void updateVideoPlaybackState(boolean isStart) {
        // 播放中
        Log.i(TAG, "updateVideoPlaybackState  ----> start ? " + isStart);
        if (isStart) {
            mVideoPlayImageView.setImageResource(R.drawable.video_pause_land_image);
        }
        // 未播放
        else {
            mVideoPlayImageView.setImageResource(R.drawable.video_play_land_image);
        }
    }

    public void updateVideoQualityState(MediaPlayerVideoQuality quality) {
        //        video_top_rate.setText(quality.getName());
    }

    public void updateVideoVolumeState() {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == mBackLayout.getId() || id == mTitleTextView.getId()) {// 返回
            ((IVideoController) mMediaPlayerController).onBackPress(MediaPlayMode.PLAY_MODE_FULLSCREEN);

        } else if (id == mVideoPlayImageView.getId()) {// 播放暂停
            Log.i(TAG, "playing  ? " + (mMediaPlayerController.isPlaying()));
            if (mMediaPlayerController.isPlaying()) {
                mMediaPlayerController.pause();
                if (mScreenLock) {
                    show();
                } else {
                    show(0);
                }
            } else if (!mMediaPlayerController.isPlaying()) {
                mMediaPlayerController.start();
                show();
            }

        } /* else if (id == mVideoSizeLayout.getId()) {//屏幕尺寸
            Log.d(TAG, "512 id == mVideoSizeLayout.getId() .");
//			mMediaPlayerController.onMovieRatioChange();
//			mWidgetMovieRatioView.show();
//			show();
            displayScreenSizePopupWindow();
			*//*
             * } else if (id == mVideoRatioForwardView.getId()) {//快进按纽
			 * mMediaPlayerController.onMoviePlayRatioUp(); show(); } else if
			 * (id == mVideoRatioBackView.getId()) {//快退按钮
			 * mMediaPlayerController.onMoviePlayRatioDown(); show();
			 *//*
        }*/ else if (id == mScreenModeImageView.getId()) { // 切换大小屏幕
            ((IVideoController) mMediaPlayerController).onRequestPlayMode(MediaPlayMode.PLAY_MODE_WINDOW);
        } else if (id == video_top_rate.getId()) {//清晰度
            displayQualityPopupWindow();
        } else if (id == video_top_switch_parts.getId()) {//切换剧集
            //没数据  则不展示
            if (relationList == null || relationList.size() == 0) {
                return;
            }
            if (mRelateListview.getVisibility() == VISIBLE) {
                mRelateListview.setVisibility(GONE);
            } else {
                mRelateListview.setVisibility(View.VISIBLE);
            }
        } else if (id == changeScreenImage.getId()) {//小屏
            ((IVideoController) mMediaPlayerController).onRequestPlayMode(MediaPlayMode.PLAY_MODE_WINDOW);
        } else if (id == video_volume_ic.getId()) {//声音
            if (mWidgetVolumeControl.getVisibility() == VISIBLE) {
                mWidgetVolumeControl.setVisibility(GONE);
            } else {
                mWidgetVolumeControl.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    public void onShowVolumeControl() {
        mWidgetVolumeControl.setVisibility(VISIBLE);
    }

    /**
     * 清晰度的弹框
     */
    private void displayQualityPopupWindow() {
        if (mRelateListview.getVisibility() == VISIBLE) {
            mRelateListview.setVisibility(GONE);
            return;
        }
        if (mQualityPopup.isShowing()) {
            mQualityPopup.hide();
            return;
        }
        //        if (video_top_rate.isSelected()) {
        //            mQualityPopup.hide();
        //            return;
        //        }

        //        startTimerTicker();

        // 弹出清晰度框
        if (qualitys == null || qualitys.size() == 0) {
            return;
        }
        int widthExtra = MediaPlayerUtils.dip2px(getContext(), 5);
        //        int width = video_top_rate.getMeasuredWidth() + widthExtra;
        //        int height = (MediaPlayerUtils.dip2px(getContext(), 50) + MediaPlayerUtils.dip2px(getContext(), 2)) * qualityList.size();

        int width = MediaPlayerUtils.dip2px(getContext(), 55);
        int height = MediaPlayerUtils.dip2px(getContext(), 41) * qualitys.size();
        //
        //        int x = MediaPlayerUtils.getXLocationOnScreen(video_top_rate) - widthExtra / 2;
        //        int y = MediaPlayerUtils.getYLocationOnScreen(video_top_rate) - height;

        int x = video_top_rate.getMeasuredWidth() + widthExtra;
        int y = 47 - video_top_rate.getMeasuredHeight();
        //        video_top_rate.setText(this.mCurrentQuality.getQualityName());
        //        mQualityPopup.show(video_top_rate, qualityList, this.mCurrentQuality, x, y, width, height);
        mQualityPopup.show(video_top_rate, qualitys, this.mCurrentQuality, x / 2, y, width, height, video_top_rate);
        //        video_top_rate.setSelected(true);
        show(0);//一直显示控制条
    }


    @Override
    public void onScreenShow() {
        show();
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        Log.d(TAG, "onSystemUiVisibilityChange :" + visibility);
    }

    @Override
    public void onWindowSystemUiVisibilityChanged(int visible) {
        Log.d(TAG, "onWindowSystemUiVisibilityChanged :" + visible);
    }

    protected void onHideSeekView() {
        if (mWidgetSeekView != null && mWidgetSeekView.isShowing()) {
            mWidgetSeekView.hide(true);
        }
    }

    protected void onGestureSeekBegin(int currentPosition, int duration) {
        mWidgetSeekView.onGestureSeekBegin(currentPosition, duration);
    }

    protected void onGestureVolumeChange(float distanceY, float totalVolumeDistance, AudioManager audioManager) {
        if (mWidgetVolumeControl != null && mWidgetVolumeControl.getVisibility() == VISIBLE) {
            mWidgetVolumeControl.onGestureVolumeChange(distanceY, totalVolumeDistance, audioManager);
        }
    }

    protected void onGestureLightChange(float distanceY, Window mHostWindow) {
        if (mControllerBrightView != null && mControllerBrightView.getVisibility() == VISIBLE) {
            mControllerBrightView.onGestureLightChange(distanceY, mHostWindow);
        }
    }

    protected void onGestureSeekChange(float distanceY, float totalSeekDistance) {
        if (mWidgetSeekView != null) {
            mWidgetSeekView.onGestureSeekChange(distanceY, totalSeekDistance);
            if (mediaPlayerEventActionView != null) {
                if (mediaPlayerEventActionView.isShowing()) {
                    mediaPlayerEventActionView.hideCompleteLayout();
                }
            }
        }
    }

    protected void onSeekTo() {
        if (mWidgetSeekView != null) {
            long seekPosition = mWidgetSeekView.onGestureSeekFinish();
            if (seekPosition >= 0 && seekPosition <= mMediaPlayerController.getDuration()) {
                mMediaPlayerController.seekTo((int) seekPosition);
                // mMediaPlayerController.start();
            }
        }
    }

    protected void onShowHide() {
        if (mWidgetSeekView != null) {
            mWidgetSeekView.hide(true);
        }
    }

    /**
     * 设置清晰度
     * @param qualityBeanList 清晰度列表
     * @param currentQuality 当前清晰度
     */
    public void setQuality(List<MediaQualityBean> qualityBeanList, MediaQualityBean currentQuality) {
        qualitys = qualityBeanList;
        this.mCurrentQuality = currentQuality;

        video_top_rate.setText(currentQuality.getQualityName());
        video_top_rate.setSelected(true);
        if (qualityBeanList == null || qualityBeanList.size() <= 1) {
            video_top_rate.setEnabled(false);
            video_top_rate.setTextColor(getResources().getColor(R.color.gray_primary_dark));
        } else {
            video_top_rate.setTextColor(getResources().getColor(R.color.player_quality_text_selector));
            video_top_rate.setEnabled(true);
        }
    }

    public void setRelateVideo(List<RelateVideoInfo> lst, RelateVideoInfo relateVideoInfo) {
        this.relationList = lst;
        this.mRelateVideoInfo = relateVideoInfo;
        relatedAdapter.refreshList(lst);
        relatedAdapter.refreshCurrentReleteVideoInfo(mRelateVideoInfo);
        if (relationList == null || relationList.size() <= 1) {
            video_top_switch_parts.setEnabled(false);
            video_top_switch_parts.setTextColor(getResources().getColor(R.color.gray_primary_dark));
        } else {
            video_top_switch_parts.setEnabled(true);
            video_top_switch_parts.setTextColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    public void show() {
        super.show();
        // 已通过广播实现音量变化监听，每次显示更新音量控件
        mWidgetVolumeControl.updateVolumeSeekBar();
    }
}
