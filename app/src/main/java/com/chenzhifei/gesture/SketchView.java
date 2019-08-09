package com.chenzhifei.gesture;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Environment;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.chenzhifei.libgesture.TwoFingersGestureDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SketchView extends FrameLayout {

    public static final int LINE_MODE_CURVE = 1;
    public static final int LINE_MODE_STRAIGHT = 2;
    public static final int LINE_MODE_RUBBER = 3;

    private int lineMode = LINE_MODE_CURVE;
    private int screenNum = 1;
    private int maxScreens = 3;
    private boolean isOperatingImage = true;

    private CanvasView canvasView;
    private List<PathWithConfig> pathList = new ArrayList<>();
    private Paint paintPen;
    private String paintPenColor = "#00ff00";
    private float paintPenWidth = 2;
    private Paint paintRubber;
    private float paintRubberWidth = 50;

    private TwoFingersGestureDetector twoFingersGestureDetector;

    public SketchView(Context context) {
        this(context, null);
    }

    public SketchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SketchView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        addCanvasView(context);
        setPaint();
        setGestureDetector();
    }

    private void addCanvasView(Context context) {
        canvasView = new CanvasView(context);
        canvasView.setBackgroundColor(Color.parseColor("#cccccc"));
        addView(canvasView);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams)canvasView.getLayoutParams();
        p.width = LayoutParams.MATCH_PARENT;
        p.height = LayoutParams.MATCH_PARENT;
        canvasView.setLayoutParams(p);
    }

    public void setImageBitmap(Bitmap bitmap) {
        canvasView.setImageBitmap(bitmap);
        canvasView.setScaleType(ImageView.ScaleType.MATRIX);
        canvasView.imageW = bitmap.getWidth();
        canvasView.imageH = bitmap.getHeight();
    }

    public void setPenColor(String color) {
        paintPenColor = color;
    }

    public void setPenWidth(float penWidth) {
        paintPenWidth = penWidth;
    }

    public void setRubberWidth(float rubberWidth) {
        paintRubberWidth = rubberWidth;
    }

    public void setMaxScreens(int maxScreens) {
        this.maxScreens = maxScreens;
    }

    public void addScreenNum() {
        if (screenNum >= maxScreens) {
            return;
        }
        screenNum++;
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams)canvasView.getLayoutParams();
        p.height = this.getHeight() * screenNum;
        canvasView.setLayoutParams(p);
    }

    public boolean setOperatingImage() {
        isOperatingImage = !isOperatingImage;
        canvasView.invalidate();
        return isOperatingImage;
    }

    public void setLineMode(int lineMode) {
        this.lineMode = lineMode;
    }

    private void setPaint() {
        paintPen = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPen.setColor(Color.parseColor(paintPenColor));
        paintPen.setStyle(Paint.Style.STROKE);
        paintPen.setStrokeWidth(paintPenWidth);
        paintPen.setStrokeCap(Paint.Cap.ROUND);
        paintPen.setStrokeJoin(Paint.Join.ROUND);

        paintRubber = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRubber.setAlpha(0);
        paintRubber.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paintRubber.setStyle(Paint.Style.STROKE);
        paintRubber.setStrokeWidth(paintRubberWidth);
        paintRubber.setStrokeCap(Paint.Cap.ROUND);
        paintRubber.setStrokeJoin(Paint.Join.ROUND);
    }

    private void setGestureDetector() {
        twoFingersGestureDetector = new TwoFingersGestureDetector();
        twoFingersGestureDetector.setTwoFingersGestureListener(new TwoFingersGestureDetector.TwoFingersGestureListener() {
            @Override
            public void onMoved(float moveX, float moveY, float deltaMovedX, float deltaMovedY, long deltaMilliseconds, int fingers) {
                if (isOperatingImage) {
                    float canvasViewScale = canvasView.getScaleX();
                    canvasView.imageMatrix.postTranslate(deltaMovedX/canvasViewScale, deltaMovedY/canvasViewScale);
                    canvasView.invalidate();
                    return;
                }
                float newTransX = canvasView.getTranslationX() + deltaMovedX;
//                float transXlimit = (canvasView.getScaleX() - 1) * getWidth() / 2;
//                if (Math.abs(newTransX) > transXlimit) {
//                    newTransX = newTransX > 0 ? transXlimit : -transXlimit;
//                }

                float newTransY = canvasView.getTranslationY() + deltaMovedY;
//                float transYlimit = (canvasView.getScaleY() - 1) * screenNum * getHeight() / 2;
//                if (newTransY >= 0) {
//                    newTransY = newTransY > transYlimit ? transYlimit : newTransY;
//                } else {
//                    float multiScreenTransYlimit = transYlimit + (screenNum-1) * getHeight();
//                    newTransY = -newTransY > multiScreenTransYlimit ? -multiScreenTransYlimit : newTransY;
//                }
                canvasView.setTranslationX(newTransX);
                canvasView.setTranslationY(newTransY);
            }

            @Override
            public void onRotated(float centerX, float centerY, float deltaRotatedDeg, long deltaMilliseconds) {
                //canvasView.setRotation(canvasView.getRotation() + deltaRotatedDeg);
            }

            @Override
            public void onScaled(float centerX, float centerY, float deltaScaledX, float deltaScaledY,
                                 float deltaScaledDistance, long deltaMilliseconds) {
                float scaleFactor = canvasView.getScaleX();
                scaleFactor += deltaScaledDistance * scaleFactor / canvasView.getWidth();
                if (isOperatingImage) {
                    float canvasViewScale = canvasView.getScaleX(); // 防止双倍缩放
                    // 这里变换具有累加效果
                    canvasView.imageMatrix.preScale(scaleFactor/canvasViewScale, scaleFactor/canvasViewScale);
                    canvasView.invalidate();
                    return;
                }
//                if (scaleFactor < 1) {
//                    scaleFactor = 1;
//                }
                if (scaleFactor > 5) {
                    scaleFactor = 5;
                }
                canvasView.setScaleX(scaleFactor);
                canvasView.setScaleY(scaleFactor);
            }

            @Override
            public void onUp(float upX, float upY, long upTime, float xVelocity, float yVelocity) {
                clampBoundsIfNeed();
            }
        });
    }

    private void clampBoundsIfNeed() {
        float scaleFactor = canvasView.getScaleX();
        if (scaleFactor >= 1) {
            float transXlimit = (canvasView.getScaleX() - 1) * getWidth() / 2;
            float transX = canvasView.getTranslationX();
            float transXNeedOffset = 0;
            if (Math.abs(transX) > transXlimit) {
                transXNeedOffset = transX > 0 ? (transXlimit - transX) : (-transXlimit - transX);
            }

            float transYlimit = (canvasView.getScaleY() - 1) * screenNum * getHeight() / 2;
            float transY = canvasView.getTranslationY();
            float transYNeedOffset = 0;
            if (transY >= 0) {
                transYNeedOffset = transY > transYlimit ? (transYlimit - transY) : 0;
            } else {
                float multiScreenTransYlimit = transYlimit + (screenNum-1) * getHeight();
                transYNeedOffset = (-transY > multiScreenTransYlimit) ? (-multiScreenTransYlimit - transY) : 0;
            }

            if (transXNeedOffset != 0) {
                animTransX(transX, transX + transXNeedOffset);
            }
            if (transYNeedOffset != 0) {
                animTransY(transY, transY + transYNeedOffset);
            }
        } else {
            animScale(canvasView.getScaleX());
            float transX = canvasView.getTranslationX();
            if (transX != 0) {
                animTransX(transX, 0);
            }
            float transY = canvasView.getTranslationY();
            if (transY > 0) {
                animTransY(transY, 0);
            } else {
                float transYlimit = (screenNum-1) * getHeight();
                if (-transY > transYlimit) {
                    animTransY(transY, -transYlimit);
                }
            }
        }
    }

    private final long animDuration = 250;
    private DecelerateInterpolator interpolator = new DecelerateInterpolator();

    private void animTransX(float start, float end) {
        ValueAnimator vaX = ValueAnimator.ofFloat(start, end).setDuration(animDuration);
        vaX.setInterpolator(interpolator);
        vaX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                canvasView.setTranslationX((float)animation.getAnimatedValue());
            }
        });
        vaX.start();
    }

    private void animTransY(float start, float end) {
        ValueAnimator vaY = ValueAnimator.ofFloat(start, end).setDuration(animDuration);
        vaY.setInterpolator(interpolator);
        vaY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                canvasView.setTranslationY((float)animation.getAnimatedValue());
            }
        });
        vaY.start();
    }

    private void animScale(float start) {
        ValueAnimator vaScale = ValueAnimator.ofFloat(start, 1).setDuration(animDuration);
        vaScale.setInterpolator(interpolator);
        vaScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                canvasView.setScaleX((float)animation.getAnimatedValue());
                canvasView.setScaleY((float)animation.getAnimatedValue());
            }
        });
        vaScale.start();
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
            canvasView.moveX = -1;
            return true;
        } else {
            return false;
        }
    }

    private class PathWithConfig {
        Path path;
        int color;
        int lineMode;
        float[] lineStart, lineEnd;
        PathWithConfig(String color, int lineMode, float x, float y) {
            this.color = Color.parseColor(color);
            this.lineMode = lineMode;
            if (lineMode != LINE_MODE_STRAIGHT) {
                path = new Path();
            } else {
                lineStart = new float[]{x, y};
                lineEnd = new float[]{x, y};
            }
        }
        void setLineEnd(float x, float y) {
            lineEnd[0] = x;
            lineEnd[1] = y;
        }
    }

    private class CanvasView extends AppCompatImageView {

        private Bitmap sketchBitmap;
        private Canvas sketchCanvas;
        private Paint sketchPaint = new Paint();
        private Paint rubberTipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int rubberTipColor, rubberBoundsColor;
        private float moveX, moveY;

        private Paint imageBoundsTipPaint = new Paint();
        private Matrix imageMatrix = new Matrix();
        private int imageW, imageH;

        public CanvasView(Context context) {
            this(context, null);
        }

        public CanvasView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public CanvasView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            rubberTipColor = Color.parseColor("#ffffff");
            rubberBoundsColor = Color.parseColor("#666666");

            imageBoundsTipPaint.setStyle(Paint.Style.STROKE);
            imageBoundsTipPaint.setStrokeWidth(3);
            imageBoundsTipPaint.setColor(Color.parseColor("#666666"));
            imageBoundsTipPaint.setPathEffect(new DashPathEffect(new float[]{20, 20},0));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            sketchBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            sketchCanvas = new Canvas(sketchBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            setImageMatrix(imageMatrix);
            sketchCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (PathWithConfig p: pathList) {
                if (p.lineMode == LINE_MODE_RUBBER) {
                    sketchCanvas.drawPath(p.path, paintRubber);
                } else if (p.lineMode == LINE_MODE_CURVE){
                    paintPen.setColor(p.color);
                    sketchCanvas.drawPath(p.path, paintPen);
                } else {
                    paintPen.setColor(p.color);
                    sketchCanvas.drawLine(p.lineStart[0], p.lineStart[1], p.lineEnd[0], p.lineEnd[1], paintPen);
                }
            }
            canvas.drawBitmap(sketchBitmap, 0, 0, sketchPaint);
            if (lineMode == LINE_MODE_RUBBER && moveX != -1) {
                rubberTipPaint.setColor(rubberBoundsColor);
                canvas.drawCircle(moveX, moveY, paintRubberWidth/2, rubberTipPaint);
                rubberTipPaint.setColor(rubberTipColor);
                canvas.drawCircle(moveX, moveY, paintRubberWidth/2 - 1, rubberTipPaint);
            }
            if (isOperatingImage) {
              float[] points = {0, 0, imageW, 0, imageW, imageH, 0, imageH};
              imageMatrix.mapPoints(points);
              canvas.drawRect(points[0], points[1], points[4], points[5], imageBoundsTipPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    PathWithConfig p = new PathWithConfig(paintPenColor, lineMode, event.getX(), event.getY());
                    if (lineMode != LINE_MODE_STRAIGHT) {
                        p.path.moveTo(event.getX(), event.getY());
                        p.path.lineTo(event.getX()+0.01f, event.getY()); // 按下不动也可以画一个点
                    }
                    pathList.add(p);
                    break;
                case MotionEvent.ACTION_MOVE:
                    moveX = event.getX();
                    moveY = event.getY();
                    if (lineMode != LINE_MODE_STRAIGHT) {
                        pathList.get(pathList.size() - 1).path.lineTo(moveX, moveY);
                    } else {
                        pathList.get(pathList.size() - 1).setLineEnd(moveX, moveY);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    moveX = -1;
                    break;
            }
            invalidate();
            return true;
        }
    }

    public void saveToJPG() {
        Bitmap bp = Bitmap.createBitmap(canvasView.getWidth(), canvasView.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bp);
        canvasView.draw(canvas);
        String jpgName = "jiandan100Sketch.jpg";
        File jpgFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + jpgName);
        try {
            FileOutputStream fos = new FileOutputStream(jpgFile);
            bp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
