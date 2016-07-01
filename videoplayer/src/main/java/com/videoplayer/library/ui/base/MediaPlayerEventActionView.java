package com.videoplayer.library.ui.base;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.videoplayer.library.R;

public class MediaPlayerEventActionView extends RelativeLayout {
    public static final int EVENT_ACTION_VIEW_MODE_COMPLETE = 0x00;
    public static final int EVENT_ACTION_VIEW_MODE_WAIT = 0x01;
    public static final int EVENT_ACTION_VIEW_MODE_ERROR = 0x02;

    private RelativeLayout mRootView;

    private ImageView mBackImageView;
    private TextView mTitleTextView;

    private View mWaitLayout;

    private RelativeLayout mCompleteLayout;
//    private LinearLayout mCompeteReplayLayout;
    private Button mCompletaReplayButton;
    private LinearLayout mErrorLayout;
    private View mErrorReplayBt;
    private TextView mErrorTextView;

    private EventActionViewCallback mCallback;

    public MediaPlayerEventActionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MediaPlayerEventActionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaPlayerEventActionView(Context context) {
        super(context);
        LayoutInflater.from(getContext()).inflate(R.layout.blue_media_player_event_action_view, this);

        initViews();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
    }

    private void initViews() {
        mRootView = (RelativeLayout) findViewById(R.id.event_action_layout);
        mBackImageView = (ImageView) findViewById(R.id.back_image_view);
        mWaitLayout = findViewById(R.id.wait_layout);
        mCompleteLayout = (RelativeLayout) findViewById(R.id.complete_layout);
//        mCompeteReplayLayout = (LinearLayout) findViewById(R.id.complete_replay_layout);
        mCompletaReplayButton = (Button) findViewById(R.id.complete_replay_bt);
        mErrorLayout = (LinearLayout) findViewById(R.id.error_layout);
        mErrorReplayBt = findViewById(R.id.video_error_replay_bt);
        mErrorTextView = (TextView) findViewById(R.id.error_info_title_text_view);

        mBackImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onActionBack();
                }
            }
        });
        findViewById(R.id.replay_iv).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onActionReplay();
                }
            }
        });
        mWaitLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onActionPlay();
                }
            }
        });
        mCompletaReplayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onActionReplay();
                }
            }
        });
        mCompleteLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onActionReplay();
                }
            }
        });
        mErrorReplayBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onActionError();
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    public void updateEventMode(int coverViewMode, String extraMessage) {
        switch (coverViewMode) {
            case EVENT_ACTION_VIEW_MODE_COMPLETE:
                mCallback.isHaveNext(mCompleteLayout, mWaitLayout, mErrorLayout);
                break;
            case EVENT_ACTION_VIEW_MODE_WAIT:
                mWaitLayout.setVisibility(View.VISIBLE);
                mCompleteLayout.setVisibility(View.GONE);
                mErrorLayout.setVisibility(View.GONE);
                show();
                break;
            case EVENT_ACTION_VIEW_MODE_ERROR:
                if (getVisibility() == GONE && mWaitLayout.getVisibility() == VISIBLE) {
                    break;
                }
                mErrorLayout.setVisibility(View.VISIBLE);
                if (TextUtils.isEmpty(extraMessage) || !extraMessage.contains("-10001")) {
                    mErrorTextView.setText(getResources().getString(R.string.player_error));
                } else {
                    mErrorTextView.setText(getResources().getString(R.string.connect_err));
                }
                mWaitLayout.setVisibility(View.GONE);
                mCompleteLayout.setVisibility(View.GONE);
                show();
                break;
            default:
                break;
        }
    }

    public void updateVideoTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTitleTextView.setText(title);
        }
    }

    public void show() {
        if (!isShowing()) {
            setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        if (isShowing()) {
            setVisibility(View.GONE);
        }
        setVisibility(View.GONE);
    }

    public void hideCompleteLayout() {
        if (mCompleteLayout != null && mCompleteLayout.getVisibility() == VISIBLE) {
            mCompleteLayout.setVisibility(GONE);
        }
    }

    public boolean isShowing() {
        return (getVisibility() == View.VISIBLE);
    }

    public void setCallback(EventActionViewCallback callback) {
        this.mCallback = callback;
    }

    public interface EventActionViewCallback {

        void onActionPlay();

        void onActionReplay();

        void onActionBack();

        void onActionError();

        void isHaveNext(RelativeLayout mxCompleteLayout, View mWaitLayout, LinearLayout mErrorLayout);
    }
}
