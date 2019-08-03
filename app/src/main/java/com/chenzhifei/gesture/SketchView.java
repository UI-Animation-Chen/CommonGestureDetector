package com.chenzhifei.gesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.chenzhifei.libgesture.TwoFingersGestureDetector;

import java.util.ArrayList;
import java.util.List;

public class SketchView extends FrameLayout {

    private CanvasView canvasView;
    private List<Path> pathList = new ArrayList<>();
    private Paint paint;

    private TwoFingersGestureDetector twoFingersGestureDetector;

    public SketchView(Context context) {
        this(context, null);
    }

    public SketchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SketchView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        addCanvasView(context, attrs, defaultStyle);
        setPaint();
        setGestureDetector();
    }

    private void addCanvasView(Context context, AttributeSet attrs, int defaultStyle) {
        canvasView = new CanvasView(context, attrs, defaultStyle);
        canvasView.setBackgroundColor(Color.GRAY);
        addView(canvasView);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams)canvasView.getLayoutParams();
        p.width = LayoutParams.MATCH_PARENT;
        p.height = LayoutParams.MATCH_PARENT;
        canvasView.setLayoutParams(p);
    }

    private void setPaint() {
        paint = new Paint();
        paint.setColor(Color.rgb(0, 255, 2));
        paint.setStrokeWidth(2);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void setGestureDetector() {
        twoFingersGestureDetector = new TwoFingersGestureDetector();
        twoFingersGestureDetector.setTwoFingersGestureListener(new TwoFingersGestureDetector.TwoFingersGestureListener() {
            @Override
            public void onMoved(float moveX, float moveY, float deltaMovedX, float deltaMovedY, long deltaMilliseconds, int fingers) {
                float newTransX = canvasView.getTranslationX() + deltaMovedX;
                float newTransY = canvasView.getTranslationY() + deltaMovedY;
                float transXlimit = (canvasView.getScaleX() - 1) * canvasView.getWidth();
                float transYlimit = (canvasView.getScaleY() - 1) * canvasView.getHeight();
                if (Math.abs(newTransX) > transXlimit/2) {
                    newTransX = newTransX > 0 ? transXlimit/2 : -transXlimit/2;
                }
                if (Math.abs(newTransY) > transYlimit/2) {
                    newTransY = newTransY > 0 ? transYlimit/2 : -transYlimit/2;
                }
                canvasView.setTranslationX(newTransX);
                canvasView.setTranslationY(newTransY);
            }

            @Override
            public void onRotated(float deltaRotatedDeg, long deltaMilliseconds) {
                //canvasView.setRotation(canvasView.getRotation() + deltaRotatedDeg);
            }

            @Override
            public void onScaled(float deltaScaledX, float deltaScaledY, float deltaScaledDistance, long deltaMilliseconds) {
                float scaleFactor = canvasView.getScaleX();
                scaleFactor += deltaScaledDistance * scaleFactor / canvasView.getWidth();
                if (scaleFactor < 1) {
                    scaleFactor = 1;
                }
                if (scaleFactor > 5) {
                    scaleFactor = 5;
                }
                canvasView.setScaleX(scaleFactor);
                canvasView.setScaleY(scaleFactor);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        twoFingersGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            pathList.remove(pathList.size() - 1);
            return true;
        } else {
            return false;
        }
    }

    private class CanvasView extends View{
        public CanvasView(Context context) {
            this(context, null);
        }

        public CanvasView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public CanvasView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (Path path: pathList) {
                canvas.drawPath(path, paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Path p = new Path();
                    p.moveTo(event.getX(), event.getY());
                    pathList.add(p);
                    break;
                case MotionEvent.ACTION_MOVE:
                    pathList.get(pathList.size() - 1).lineTo(event.getX(), event.getY());
                    break;
            }
            invalidate();
            return true;
        }
    }
}
