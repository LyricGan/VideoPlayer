package com.videoplayer.library.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.videoplayer.library.R;

import java.util.List;

/**
 * @author LIXIAOPENG
 * @description 相关视频
 */
public class RelatedVideoAdapter extends BaseAdapter {

    private List<RelateVideoInfo> list;

    private LayoutInflater inflater;
    RelateVideoInfo mrelateVideoInfo;
    private Context mContext;
    private CallBack callBack;

    public RelatedVideoAdapter(List<RelateVideoInfo> videoInfoList, Context context,RelateVideoInfo relateVideoInfo) {
        this.list = videoInfoList;
        this.mrelateVideoInfo=relateVideoInfo;
        this.mContext=context;
        inflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    @Override
    public RelateVideoInfo getItem(int position) {
        if (list == null) {
            return null;
        }
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void refreshList(List<RelateVideoInfo> list) {
        this.list = list;
        notifyDataSetInvalidated();
    }
   public void refreshCurrentReleteVideoInfo(RelateVideoInfo currentRelateVideoInfo){
       this.mrelateVideoInfo = currentRelateVideoInfo;
       notifyDataSetInvalidated();
   }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.blue_media_player_relate_videoinfo, null);

            viewHolder.videoImage = (ImageView) convertView.findViewById(R.id.play_status);
            viewHolder.videoTextName = (TextView) convertView.findViewById(R.id.tv_movie_name);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        RelateVideoInfo item = getItem(position);
        if(item.getId()==mrelateVideoInfo.getId()){
            viewHolder.videoImage.setVisibility(View.VISIBLE);
            viewHolder.videoTextName.setTextColor(mContext.getResources().getColor(R.color.blue));
        }else{
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
   public interface  CallBack{
     public void isSelected(int position);
 }
}

