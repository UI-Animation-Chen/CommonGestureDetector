package com.chenzhifei.libgesture;

import android.os.Handler;
import android.os.Message;

/**
 * 惯性滚动
 */
public class InertialScrolling {

    private float currXVelocity;
    private float currYVelocity;

    private Handler inertialHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            updateXYVelocity(currXVelocity, currYVelocity);
            return true;
        }
    });

    public void stopInertialScrolling() {
        inertialHandler.removeCallbacksAndMessages(null);
    }

    public void updateXYVelocity(float xVelocity, float yVelocity) {
        if (xVelocity == 0 && yVelocity == 0) {
            return;
        }

        if (inertialScrollingListener != null) {
            float REFRESH_TIME = 0.01667f; // 屏幕刷新16.67ms
            float deltaMovedX = xVelocity * REFRESH_TIME;
            float deltaMovedY = yVelocity * REFRESH_TIME;
            inertialScrollingListener.inertialScrolling(deltaMovedX, deltaMovedY);
        }

        if (Math.abs(xVelocity) > Math.abs(yVelocity)) { // 谁大，谁的ratio是1
            currXVelocity = getDecreasedVelocity(xVelocity, 1);
            currYVelocity = getDecreasedVelocity(yVelocity, Math.abs(yVelocity / xVelocity));
        } else {
            currXVelocity = getDecreasedVelocity(xVelocity, Math.abs(xVelocity / yVelocity));
            currYVelocity = getDecreasedVelocity(yVelocity, 1);
        }
        if (currXVelocity == 0 && currYVelocity == 0) {
            return;
        }

        inertialHandler.sendEmptyMessage(0);
    }

    /**
     * x和y方向上的速度衰减不一定相同，需要按比例。
     */
    private float getDecreasedVelocity(float velocity, float ratio) {
        float VELOCITY_DECAY = 10; // 速度衰减，1s衰减60次，每次衰减量pixels/s。
        float adjustedVelocity = VELOCITY_DECAY * ratio;
        if (Math.abs(velocity) <= adjustedVelocity) {
            return 0f;
        } else if (velocity < 0) {
            return velocity + adjustedVelocity;
        } else {
            return velocity - adjustedVelocity;
        }
    }

    public interface InertialScrollingListener {
        public void inertialScrolling(float deltaMovedX, float deltaMovedY);
    }

    private InertialScrollingListener inertialScrollingListener;

    public void setInertialScrollingListener(InertialScrollingListener l) {
        inertialScrollingListener = l;
    }

}
