package com.videoplayer.library.util;

public class PlayConfig {
    public static final int INTERRUPT_MODE_ERROR = -1;
    public static final int INTERRUPT_MODE_RELEASE_CREATE = 0;
    public static final int INTERRUPT_MODE_PAUSE_RESUME = 1;
    public static final int INTERRUPT_MODE_FINISH = 2;
    public boolean isStream = false;
    public int interruptMode = INTERRUPT_MODE_PAUSE_RESUME;

    public static final int SHORT_VIDEO_MODE = 3;
    public static final int LIVE_VIDEO_MODE = 4;
    public int videoMode = SHORT_VIDEO_MODE;

    private static PlayConfig mInstance;
    private static final Object mLockObject = new Object();

    public static PlayConfig getInstance() {
        if (null == mInstance) {
            synchronized (mLockObject) {
                if (null == mInstance) {
                    mInstance = new PlayConfig();
                }
            }
        }
        return mInstance;
    }

    public void setVideoMode(int videoMode) {
        this.videoMode = videoMode;
    }

    public int getVideoMode() {
        return videoMode;
    }

    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean isStream) {
        this.isStream = isStream;
    }

    public int getInterruptMode() {
        return interruptMode;
    }

    public void setInterruptMode(int interruptMode) {
        this.interruptMode = interruptMode;
    }
}
