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

        if (Math.abs(xVelocity) > Math.abs(yVelocity)) { // 以较大速度分量来算衰减
            float velocityDecay = getVelocityDecay(xVelocity);
            float decayRatio = Math.abs(yVelocity / xVelocity);
            currXVelocity = getDecayedVelocity(xVelocity, velocityDecay);
            currYVelocity = getDecayedVelocity(yVelocity, velocityDecay * decayRatio);
        } else {
            float velocityDecay = getVelocityDecay(Math.abs(yVelocity));
            float decayRatio = Math.abs(xVelocity / yVelocity);
            currXVelocity = getDecayedVelocity(xVelocity, velocityDecay * decayRatio);
            currYVelocity = getDecayedVelocity(yVelocity, velocityDecay);
        }

        if (currXVelocity == 0 && currYVelocity == 0) {
            return;
        }

        inertialHandler.sendEmptyMessage(0);
    }

    /**
     * 根据不同的速度范围，取不同的速度衰减值，正值。
     */
    private float getVelocityDecay(float velocity) {
        float velocityDecay; // 速度衰减，1s衰减60次，每次衰减量pixels/s。
        float velocityAbs = Math.abs(velocity);
        if (velocityAbs > 5000) {
            velocityDecay = 200;
        } else if (velocityAbs > 2000) {
            velocityDecay = 100;
        } else if (velocityAbs > 1000) {
            velocityDecay = 50;
        } else if (velocityAbs > 500) {
            velocityDecay = 25;
        } else if (velocityAbs > 200) {
            velocityDecay = 12;
        } else {
            velocityDecay = 4;
        }
        return velocityDecay;
    }

    private float getDecayedVelocity(float velocity, float velocityDecay) {
        if (Math.abs(velocity) <= velocityDecay) {
            return 0f;
        } else if (velocity < 0) {
            return velocity + velocityDecay;
        } else {
            return velocity - velocityDecay;
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
