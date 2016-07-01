package com.videoplayer.library.controller.video;

import android.widget.MediaController.MediaPlayerControl;


public interface IVideoController extends MediaPlayerControl {

    int getPlayMode();

    void onRequestPlayMode(int requestPlayMode);

    void onBackPress(int playMode);

    void onControllerShow(int playMode);

    void onControllerHide(int playMode);

    void onRequestLockMode(boolean lockMode);

    void onMovieRatioChange(int screenSize);

    boolean canStart();

}
