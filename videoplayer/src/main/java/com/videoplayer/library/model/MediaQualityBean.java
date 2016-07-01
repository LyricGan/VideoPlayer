package com.videoplayer.library.model;

/**
 * Created by lenovo on 2016/4/28.
 */
public class MediaQualityBean {
    public int getVid() {
        return vid;
    }

    public void setVid(int vid) {
        this.vid = vid;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    private String videoName;
    private int vid;
    private String url;
    private String qualityName;
    private int qualityCode;
    private boolean isSelect = false;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getQualityName() {
        return qualityName;
    }

    public void setQualityName(String qualityName) {
        this.qualityName = qualityName;
    }

    public int getQualityCode() {
        return qualityCode;
    }

    public void setQualityCode(int qualityCode) {
        this.qualityCode = qualityCode;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }


}
