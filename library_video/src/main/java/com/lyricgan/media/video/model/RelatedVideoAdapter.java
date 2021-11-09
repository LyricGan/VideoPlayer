package com.lyricgan.media.video.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lyricgan.media.video.R;

import java.util.List;

public class RelatedVideoAdapter extends BaseAdapter {
    private List<RelateVideoInfo> list;
    private LayoutInflater mInflater;
    RelateVideoInfo mRelateVideoInfo;
    private Context mContext;
    private CallBack callBack;

    public RelatedVideoAdapter(List<RelateVideoInfo> videoInfoList, Context context, RelateVideoInfo relateVideoInfo) {
        this.list = videoInfoList;
        this.mRelateVideoInfo = relateVideoInfo;
        this.mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return list != null ? list.size() : 0;
    }

    @Override
    public RelateVideoInfo getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void refreshList(List<RelateVideoInfo> list) {
        this.list = list;
        notifyDataSetInvalidated();
    }

    public void refreshCurrentReleteVideoInfo(RelateVideoInfo currentRelateVideoInfo) {
        this.mRelateVideoInfo = currentRelateVideoInfo;
        notifyDataSetInvalidated();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.blue_media_player_relate_videoinfo, null);
            viewHolder.videoImage = (ImageView) convertView.findViewById(R.id.play_status);
            viewHolder.videoTextName = (TextView) convertView.findViewById(R.id.tv_movie_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        RelateVideoInfo item = getItem(position);
        if (item.getId() == mRelateVideoInfo.getId()) {
            viewHolder.videoImage.setVisibility(View.VISIBLE);
            viewHolder.videoTextName.setTextColor(mContext.getResources().getColor(R.color.blue));
        } else {
            viewHolder.videoImage.setVisibility(View.GONE);
            viewHolder.videoTextName.setTextColor(mContext.getResources().getColor(R.color.white));
        }
        viewHolder.videoTextName.setText(item.getDisplayName());

        return convertView;
    }

    class ViewHolder {
        public ImageView videoImage;
        public TextView videoTextName;
    }

    public interface CallBack {
        void isSelected(int position);
    }
}

