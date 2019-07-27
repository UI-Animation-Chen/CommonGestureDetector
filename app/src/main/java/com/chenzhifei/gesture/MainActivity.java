package com.chenzhifei.gesture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.chenzhifei.libgesture.TwoFingersGestureDetector;

public class MainActivity extends AppCompatActivity {

    private TextView tv;
    private float tvWidth = -1f;
    private float tvHeight = -1f;

    private float mW = -1f;
    private float mH = -1f;

    private TwoFingersGestureDetector twoFingersGestureDetector;

    private float rotateDeg = 0f;
    private float scaleFactor = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    private int transXFactor;
    private int transYFactor;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.tv);

        twoFingersGestureDetector = new TwoFingersGestureDetector();
        twoFingersGestureDetector.enableInertialScrolling();
        twoFingersGestureDetector.setTwoFingersGestureListener(new TwoFingersGestureDetector.TwoFingersGestureListener() {
            @Override
            public void onDown(float downX, float downY, long downTime) {
                if (tvWidth == -1f) {
                    tvWidth = tv.getWidth();
                    tvHeight = tv.getHeight();
                }
                if (mW == -1f) {
                    mW = ((View)tv.getParent()).getWidth();
                    mH = ((View)tv.getParent()).getHeight();
                }
            }

            @Override
            public void onMoved(float deltaMovedX, float deltaMovedY, long deltaMilliseconds) {
                if (Math.abs(translateX + deltaMovedX) <= mW/2) {
                    tv.setTranslationX(translateX += deltaMovedX);
                }
                if (Math.abs(translateY + deltaMovedY) <= mH/2) {
                    tv.setTranslationY(translateY += deltaMovedY);
                }
            }

            @Override
            public void onRotated(float deltaRotatedDeg, long deltaMilliseconds) {
                tv.setRotation(rotateDeg += deltaRotatedDeg);
            }

            @Override
            public void onScaled(float deltaScaledX, float deltaScaledY, float deltaScaledDistance, long deltaMilliseconds) {
                scaleFactor += deltaScaledDistance / tvWidth;
                tv.setScaleX(scaleFactor);
                tv.setScaleY(scaleFactor);
            }

            @Override
            public void onUp(float upX, float upY, long upTime, float xVelocity, float yVelocity) {
                transXFactor = xVelocity > 0 ? 1 : -1;
                transYFactor = yVelocity > 0 ? 1 : -1;
            }

            @Override
            public void onInertialScrolling(float deltaMovedX, float deltaMovedY) {
                if (translateX + deltaMovedX < (-mW/2)) {
                    transXFactor = 1;
                } else if (translateX + deltaMovedX > (mW/2)) {
                    transXFactor = -1;
                }
                tv.setTranslationX(translateX += Math.abs(deltaMovedX) * transXFactor);

                if (translateY + deltaMovedY < (-mH/2)) {
                    transYFactor = 1;
                } else if (translateY + deltaMovedY > (mH/2)) {
                    transYFactor = -1;
                }
                tv.setTranslationY(translateY += Math.abs(deltaMovedY) * transYFactor);
            }

            @Override
            public void onCancel() {}
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        twoFingersGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
