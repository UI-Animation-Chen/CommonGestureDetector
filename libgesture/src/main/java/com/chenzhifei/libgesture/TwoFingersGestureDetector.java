package com.chenzhifei.libgesture;

import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 * Created by chenzhifei on 2017/6/30.
 * two fingers: move(one or two), rotate, scale
 */

public class TwoFingersGestureDetector {

    private boolean moreThan2Fingers = false;

    private float oldX = 0f;
    private float oldY = 0f;

    // 如果在连续两次MOVE事件中转动穿过了左侧-180到180，那么这次转动效果或按0度算，或小于实际角度。
    // 但连续两次move事件超过180度几乎是不可能的。
    private static final float MAX_DEGREES_IN_TWO_MOVE_EVENTS = 180f;
    private static final float REFERENCE_DEGREES = 360f - MAX_DEGREES_IN_TWO_MOVE_EVENTS; // 权衡为180
    private static final float RADIAN_TO_DEGREE = (float) (180.0 / Math.PI);
    private float oldTanDeg = 0f;

    private float oldScaledX = 0f;
    private float oldScaledY = 0f;
    private float old2FingersDistance = 0f;

    private long oldTimestamp = 0;

    private VelocityTracker vt = VelocityTracker.obtain();

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 2) {
            moreThan2Fingers = true;
            if (twoFingersGestureListener != null) {
                twoFingersGestureListener.onCancel();
            }
        }
        vt.addMovement(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                oldX = event.getX(0);
                oldY = event.getY(0);
                oldTimestamp = event.getDownTime();
                if (twoFingersGestureListener != null) {
                    twoFingersGestureListener.onDown(oldX, oldY, oldTimestamp);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (moreThan2Fingers) {
                    return true;
                }

                // 第二个触点一出现就清空。当然上次up清理也行。
                oldTanDeg = 0f;
                oldScaledX = 0f;
                oldScaledY = 0f;
                old2FingersDistance = 0f;

                oldX = (event.getX(0) + event.getX(1)) / 2f;
                oldY = (event.getY(0) + event.getY(1)) / 2f;
                oldTimestamp = event.getEventTime();
                break;
            case MotionEvent.ACTION_MOVE:
                if (moreThan2Fingers) {
                    return true;
                }

                long newTimestamp = event.getEventTime();
                long currDeltaMilliseconds = newTimestamp - oldTimestamp;
                oldTimestamp = newTimestamp;

                float newX, newY;
                // handle 2 fingers touch
                if (event.getPointerCount() == 2) {
                    // handle rotate
                    float currDeltaRotatedDeg = getRotatedDegBetween2Events(event);
                    // handle scale
                    float deltaScaledX = getDeltaScaledXBetween2Events(event);
                    float deltaScaledY = getDeltaScaledYBetween2Events(event);
                    float currDeltaScaledDistance = getScaledDistanceBetween2Events(event);

                    if (this.twoFingersGestureListener != null) {
                        twoFingersGestureListener.onScaled(deltaScaledX, deltaScaledY, currDeltaScaledDistance, currDeltaMilliseconds);
                        twoFingersGestureListener.onRotated(currDeltaRotatedDeg, currDeltaMilliseconds);
                    }

                    // handle move
                    newX = (event.getX(0) + event.getX(1)) / 2f;
                    newY = (event.getY(0) + event.getY(1)) / 2f;
                } else {
                    newX = event.getX(0);
                    newY = event.getY(0);
                }

                float currDeltaMovedX = newX - oldX;
                float currDeltaMovedY = newY - oldY;
                oldX = newX;
                oldY = newY;

                if (this.twoFingersGestureListener != null) {
                    twoFingersGestureListener.onMoved(currDeltaMovedX, currDeltaMovedY, currDeltaMilliseconds);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (moreThan2Fingers) {
                    return true;
                }

                if (event.getActionIndex() == 0) {
                    oldX = event.getX(1);
                    oldY = event.getY(1);
                } else if (event.getActionIndex() == 1) {
                    oldX = event.getX(0);
                    oldY = event.getY(0);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (moreThan2Fingers) {
                    moreThan2Fingers = false;
                    return true;
                }

                vt.computeCurrentVelocity(1000);
                float yVelocity = vt.getYVelocity();
                float xVelocity = vt.getXVelocity();
                vt.clear();

                if (twoFingersGestureListener != null) {
                    twoFingersGestureListener.onUp(oldX, oldY, oldTimestamp, xVelocity, yVelocity);
                }
                break;
        }
        return true;
    }

    /**
     * Math.atan2(x, y): 左侧的-180和180交界需要额外处理，其余区域都是连续的。
     *
     *              | y
     *              | -90
     *          ----|----
     *  -180  /     |     \   0
     *  -----|------+------|-----> x
     *   180  \     |     /   0
     *          ----|----
     *              | 90
     * 如果连续两次move事件的转动穿过了左侧-180到180，分2种情况：
     *  实际转动角度超过180，则计算的结果是360减去该角度，即小于实际转动效果；
     *      比如逆时针从-80转到80，实际转动为200，但计算结果是80 - (-80) = 160，小于实际效果。
     *  实际转动角度不超过180，则忽略这次转动。
     *      比如逆时针从-100转到100，实际转动为160，但计算结果为200，将返回0，忽略这次转动。
     */
    private float getRotatedDegBetween2Events(MotionEvent event) {
        float spanX = event.getX(1) - event.getX(0);
        float spanY = event.getY(1) - event.getY(0);
        float tanDeg = (float) Math.atan2(spanY, spanX) * RADIAN_TO_DEGREE;
        if (oldTanDeg == 0f
                || (tanDeg >= 0f && oldTanDeg <= 0f && tanDeg - oldTanDeg > REFERENCE_DEGREES)
                || (tanDeg <= 0f && oldTanDeg >= 0f && oldTanDeg - tanDeg > REFERENCE_DEGREES)) {

            oldTanDeg = tanDeg;
            return 0f; // 忽略这次转动（如果是转动的话）。
        } else {
            float deltaDeg = tanDeg - oldTanDeg;
            oldTanDeg = tanDeg;
            return deltaDeg;
        }
    }

    private float getDeltaScaledXBetween2Events(MotionEvent event) {
        float newScaledX = Math.abs(event.getX(1) - event.getX(0));
        if (oldScaledX == 0f) {
            oldScaledX = newScaledX;
            return 0f;
        } else {
            float deltaScaledX = newScaledX - oldScaledX;
            oldScaledX = newScaledX;
            return deltaScaledX;
        }
    }

    private float getDeltaScaledYBetween2Events(MotionEvent event) {
        float newScaledY = Math.abs(event.getY(1) - event.getY(0));
        if (oldScaledY == 0f) {
            oldScaledY = newScaledY;
            return 0f;
        } else {
            float deltaScaledY = newScaledY - oldScaledY;
            oldScaledY = newScaledY;
            return deltaScaledY;
        }
    }

    private float getScaledDistanceBetween2Events(MotionEvent event) {
        float newScaledX = event.getX(1) - event.getX(0), newScaledY = event.getY(1) - event.getY(0);
        float new2FingerDistance = (float) Math.sqrt((newScaledX * newScaledX) + (newScaledY * newScaledY));
        if (old2FingersDistance == 0f) {
            old2FingersDistance = new2FingerDistance;
            return 0f;
        } else {
            float deltaDistance = new2FingerDistance - old2FingersDistance;
            old2FingersDistance = new2FingerDistance;
            return deltaDistance;
        }
    }

    public interface TwoFingersGestureListener {
        void onDown(float downX, float downY, long downTime);

        void onMoved(float deltaMovedX, float deltaMovedY, long deltaMilliseconds);

        void onRotated(float deltaRotatedDeg, long deltaMilliseconds);

        void onScaled(float deltaScaledX, float deltaScaledY, float deltaScaledDistance, long deltaMilliseconds);

        // velocity: pixels/second   degrees/second
        void onUp(float upX, float upY, long upTime, float xVelocity, float yVelocity);

        /**
         * invoked when more than 2 findgers
         */
        void onCancel();
    }

    private TwoFingersGestureListener twoFingersGestureListener;

    public void setTwoFingersGestureListener(TwoFingersGestureListener l) {
        this.twoFingersGestureListener = l;
    }
}
