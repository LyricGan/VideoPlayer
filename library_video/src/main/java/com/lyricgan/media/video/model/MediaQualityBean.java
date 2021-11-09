package com.lyricgan.media.video.model;

public class MediaQualityBean {
    private String videoName;
    private int vid;
    private String url;
    private String qualityName;
    private int qualityCode;
    private boolean isSelect = false;

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
