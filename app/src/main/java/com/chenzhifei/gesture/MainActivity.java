package com.chenzhifei.gesture;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;

import com.chenzhifei.libgesture.TwoFingersGestureDetector;

public class MainActivity extends AppCompatActivity {

    private TextView tv;
    private float tvWidth = -1f;
    private float tvHeight = -1f;

    private TwoFingersGestureDetector twoFingersGestureDetector;

    private float rotateDeg = 0f;
    private float scaleFactor = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    private Handler animHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            Bundle data = message.getData();
            handleMoveAnim(data.getFloat("xVelocity"), data.getFloat("yVelocity"));
            return true;
        }
    });

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.tv);

        twoFingersGestureDetector = new TwoFingersGestureDetector();
        twoFingersGestureDetector.setTwoFingersGestureListener(new TwoFingersGestureDetector.TwoFingersGestureListener() {
            @Override
            public void onDown(float downX, float downY, long downTime) {
                animHandler.removeCallbacksAndMessages(null);
                if (tvWidth == -1f) {
                    tvWidth = tv.getWidth();
                    tvHeight = tv.getHeight();
                }
            }

            @Override
            public void onMoved(float deltaMovedX, float deltaMovedY, long deltaMilliseconds) {
                tv.setTranslationX(translateX += deltaMovedX);
                tv.setTranslationY(translateY += deltaMovedY);
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
                handleMoveAnim(xVelocity, yVelocity);
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

    private void handleMoveAnim(float xVelocity, float yVelocity) {
        if (xVelocity == 0 || yVelocity == 0) {
            return;
        }
        float REFRESH_TIME = 0.01667f; // 屏幕刷新16.67ms
        tv.setTranslationX(translateX += xVelocity * REFRESH_TIME);
        tv.setTranslationY(translateY += yVelocity * REFRESH_TIME);
        float newXVelocity = getDecreasedVelocity(xVelocity, 1),
              newYVelocity = getDecreasedVelocity(yVelocity, Math.abs(yVelocity / xVelocity));
        if (newXVelocity == 0 || newYVelocity == 0) {
            return;
        }
        Bundle data = new Bundle();
        data.putFloat("xVelocity", newXVelocity);
        data.putFloat("yVelocity", newYVelocity);
        Message msg = Message.obtain();
        msg.setData(data);
        animHandler.sendMessage(msg);
    }

    /**
     * x和y方向上的速度衰减不一定相同，需要按比例。
     */
    private float getDecreasedVelocity(float velocity, float ratio) {
        float VELOCITY_DECAY = 30; // 速度衰减，1s衰减60次，每次衰减量pixels/s。
        if (Math.abs(velocity) <= VELOCITY_DECAY) {
            return 0f;
        } else if (velocity < 0) {
            return velocity + VELOCITY_DECAY * ratio;
        } else {
            return velocity - VELOCITY_DECAY * ratio;
        }
    }
}
