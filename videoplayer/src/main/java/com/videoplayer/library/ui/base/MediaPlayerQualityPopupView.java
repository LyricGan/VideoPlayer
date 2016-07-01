package com.videoplayer.library.ui.base;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.videoplayer.library.R;
import com.videoplayer.library.model.MediaQualityBean;

import java.util.List;

public class MediaPlayerQualityPopupView {
    private Context mContext;
    private PopupWindow mPopupWindow;
    private ListView mListView;

    private QualityAdapter mAdapter;
    private List<MediaQualityBean> mData;
    private Callback mCallback;

    private boolean isShowing = false;
    private MediaQualityBean mCurrentSeletedQuality;
    private TextView video_top_rate;

    public MediaPlayerQualityPopupView(Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View root = inflater.inflate(R.layout.blue_media_player_quality_popup_view, null);
        mListView = (ListView) root.findViewById(R.id.quality_list_view);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mCallback != null) {
                    if (mData != null && mData.size() > 0) {
                        MediaQualityBean quality = mData.get(position);
                        if (quality != null) {
                            mCallback.onQualitySelected(quality);
                        }
                    }
                }
            }
        });
        mAdapter = new QualityAdapter();
        mListView.setAdapter(mAdapter);

        mPopupWindow = new PopupWindow(mContext, null, R.style.Transparent_Dialog);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        mPopupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    return true;
                }
                return false;
            }
        });
        mPopupWindow.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss() {
                isShowing = false;
                if (mCallback != null)
                    mCallback.onPopupViewDismiss();
            }
        });
        mPopupWindow.setContentView(root);
    }

    public void show(View anchor, List<MediaQualityBean> qualityList,
                     MediaQualityBean curQuality, int x, int y, int width,
                     int height, TextView mvideo_top_rate) {
        this.mData = qualityList;
        this.mCurrentSeletedQuality = curQuality;
        mAdapter.notifyDataSetChanged();
        mPopupWindow.setWidth(width);
        mPopupWindow.setHeight(height);
        mPopupWindow.showAsDropDown(anchor, -10, 15);
//		mPopupWindow.showAtLocation(anchor, Gravity.BOTTOM, 10, 10);
        isShowing = true;
        this.video_top_rate = mvideo_top_rate;
    }

    public void hide() {
        mPopupWindow.dismiss();
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public Callback getCallback() {
        return mCallback;
    }

    public interface Callback {
        void onQualitySelected(MediaQualityBean quality);

        void onPopupViewDismiss();
    }

    class QualityAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mData != null)
                return mData.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new QualityItemView(mContext);
            }
            QualityItemView itemView = (QualityItemView) convertView;
            MediaQualityBean quality = mData.get(position);
            itemView.initData(quality);
            return itemView;
        }
    }

    class QualityItemView extends RelativeLayout {
        private TextView mQualityTextView;

        public QualityItemView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init(context);
        }

        public QualityItemView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init(context);
        }

        public QualityItemView(Context context) {
            super(context);
            init(context);
        }

        private void init(Context context) {
            inflate(context, R.layout.blue_media_player_quality_item_view, this);
            mQualityTextView = (TextView) findViewById(R.id.quality_text_view);
        }

        public void initData(MediaQualityBean quality) {
            mQualityTextView.setText(quality.getQualityName());
            if (null != mCurrentSeletedQuality && quality.getQualityCode() == mCurrentSeletedQuality.getQualityCode()) {
                mQualityTextView.setSelected(true);
            } else {
                mQualityTextView.setSelected(false);
            }
            video_top_rate.setText(mCurrentSeletedQuality.getQualityName());
        }
    }
}
