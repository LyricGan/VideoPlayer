package com.lyricgan.media.video.controller;

import android.widget.MediaController.MediaPlayerControl;

public interface IVideoController extends MediaPlayerControl {

    int getPlayMode();

    void onRequestPlayMode(int requestPlayMode);

    void onBackPressed(int playMode);

    void onControllerShow(int playMode);

    void onControllerHide(int playMode);

    void onRequestLockMode(boolean lockMode);

    void onMovieRatioChange(int screenSize);

    boolean canStart();
}
