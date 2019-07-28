package com.chenzhifei.gesture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.chenzhifei.libgesture.TwoFingersGestureDetector;

public class MainActivity extends AppCompatActivity {

    private TextView tv;

    private TwoFingersGestureDetector twoFingersGestureDetector;

    private int transXDirection;
    private int transYDirection;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = findViewById(R.id.tv);

        twoFingersGestureDetector = new TwoFingersGestureDetector();
        twoFingersGestureDetector.enableInertialScrolling();
        twoFingersGestureDetector.setTwoFingersGestureListener(new TwoFingersGestureDetector.TwoFingersGestureListener() {
            @Override
            public void onDown(float downX, float downY, long downTime) {}

            @Override
            public void onMoved(float deltaMovedX, float deltaMovedY, long deltaMilliseconds) {
                if (Math.abs(tv.getTranslationX() + deltaMovedX) <= getTransXLimit()) {
                    tv.setTranslationX(tv.getTranslationX() + deltaMovedX);
                }
                if (Math.abs(tv.getTranslationY() + deltaMovedY) <= getTransYLimit()) {
                    tv.setTranslationY(tv.getTranslationY() + deltaMovedY);
                }
            }

            @Override
            public void onRotated(float deltaRotatedDeg, long deltaMilliseconds) {
                tv.setRotation(tv.getRotation() + deltaRotatedDeg);
            }

            @Override
            public void onScaled(float deltaScaledX, float deltaScaledY, float deltaScaledDistance, long deltaMilliseconds) {
                float scaleFactor = tv.getScaleX();
                scaleFactor += deltaScaledDistance / tv.getWidth();
                if (scaleFactor < 1) {
                    scaleFactor = 1;
                }
                tv.setScaleX(scaleFactor);
                tv.setScaleY(scaleFactor);
            }

            @Override
            public void onUp(float upX, float upY, long upTime, float xVelocity, float yVelocity) {
                setReboundFactor(xVelocity, yVelocity);
            }

            @Override
            public void onInertialScrolling(float deltaMovedX, float deltaMovedY) {
                reboundEffect(deltaMovedX, deltaMovedY);
            }

            @Override
            public void onCancel() {}
        });
    }

    private void setReboundFactor(float xVelocity, float yVelocity) {
        transXDirection = xVelocity > 0 ? 1 : -1;
        transYDirection = yVelocity > 0 ? 1 : -1;
    }

    private void reboundEffect(float deltaMovedX, float deltaMovedY) {
        float transXLimit = getTransXLimit();
        if (tv.getTranslationX() + deltaMovedX < -transXLimit) {
            transXDirection = 1;
        } else if (tv.getTranslationX() + deltaMovedX > transXLimit) {
            transXDirection = -1;
        }
        tv.setTranslationX(tv.getTranslationX() + Math.abs(deltaMovedX) * transXDirection);

        float transYLimit = getTransYLimit();
        if (tv.getTranslationY() + deltaMovedY < -transYLimit) {
            transYDirection = 1;
        } else if (tv.getTranslationY() + deltaMovedY > transYLimit) {
            transYDirection = -1;
        }
        tv.setTranslationY(tv.getTranslationY() + Math.abs(deltaMovedY) * transYDirection);
    }

    private float getTransXLimit() {
        float scaledW = tv.getWidth() * tv.getScaleX();
        return (getContainerW() - scaledW) / 2;
    }

    private float getTransYLimit() {
        float scaledH = tv.getHeight() * tv.getScaleY();
        return (getContainerH() - scaledH) / 2;
    }

    private int getContainerW() {
        return ((View)tv.getParent()).getWidth();
    }

    private int getContainerH() {
        return ((View)tv.getParent()).getHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        twoFingersGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
