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
import android.os.Handler;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
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
    private float screenNum = 1;
    private float maxScreens = 3;
    private boolean isOperatingImage = true;

    private CanvasView canvasView;
    private List<PathWithConfig> pathList = new ArrayList<>();
    private Paint paintPen;
    private String paintPenColor = "#00ff00";
    private float paintPenWidth = 2;
    private Paint paintRubber;
    private float paintRubberWidth = 50;
    private DashPathEffect dashPathEffect = new DashPathEffect(new float[]{4, 8}, 0);

    private DecorLayer decorLayer;

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
        addDecorLayer(context);
        setPaint();
        setGestureDetector();
    }

    private void addCanvasView(Context context) {
        setBackgroundColor(Color.LTGRAY);
        canvasView = new CanvasView(context);
        canvasView.setBackgroundColor(Color.WHITE);
        addView(canvasView);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams)canvasView.getLayoutParams();
        p.width = LayoutParams.MATCH_PARENT;
        p.height = LayoutParams.MATCH_PARENT;
        canvasView.setLayoutParams(p);
    }

    private void addDecorLayer(Context context) {
        decorLayer = new DecorLayer(context);
        addView(decorLayer);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) decorLayer.getLayoutParams();
        p.width = LayoutParams.MATCH_PARENT;
        p.height = LayoutParams.MATCH_PARENT;
        decorLayer.setLayoutParams(p);
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

    public void setDashPathEffect(boolean dashPath) {
        if (dashPath) {
            paintPen.setPathEffect(dashPathEffect);
        } else {
            paintPen.setPathEffect(null);
        }
    }

    public void addScreenNum() {
        if (screenNum >= maxScreens) {
            return;
        }
        screenNum += 0.5f;
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams)canvasView.getLayoutParams();
        p.height = (int)(this.getHeight() * screenNum);
        canvasView.setLayoutParams(p);
        if (canvasView.getScaleX() > 1) { // 增加高度时，防止内容偏移。
            float transYoffset = this.getHeight() * .25f * (canvasView.getScaleX() - 1);
            canvasView.setTranslationY(canvasView.getTranslationY() + transYoffset);
        }
        decorLayer.showScrollBar();
        decorLayer.disappearScrollBarDelayed();
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
        paintPen.setPathEffect(null);

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
        twoFingersGestureDetector.enableInertialScrolling();
        twoFingersGestureDetector.setTwoFingersGestureListener(new TwoFingersGestureDetector.TwoFingersGestureListener() {
            @Override
            public void onMoved(float moveX, float moveY, float deltaMovedX, float deltaMovedY,
                                long deltaMilliseconds, int fingers) {
                if (isOperatingImage) {
                    float canvasViewScale = canvasView.getScaleX();
                    canvasView.imageMatrix.postTranslate(deltaMovedX/canvasViewScale, deltaMovedY/canvasViewScale);
                    canvasView.invalidate();
                    return;
                }
                cancelClampBoundsAnim();
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
                decorLayer.showScrollBar();
            }

            @Override
            public void onRotated(float centerX, float centerY, float deltaRotatedDeg, long deltaMilliseconds) {
                //canvasView.setRotation(canvasView.getRotation() + deltaRotatedDeg);
            }

            @Override
            public void onScaled(float centerX, float centerY, float deltaScaledX, float deltaScaledY,
                                 float deltaScaledDistance, long deltaMilliseconds) {
                if (isOperatingImage) {
                    float imageScale = deltaScaledDistance / getWidth() + 1;
                    canvasView.imageMatrix.preScale(imageScale, imageScale);
                    canvasView.invalidate();
                    return;
                }
                cancelClampBoundsAnim();
                float currScale = canvasView.getScaleX();
                float deltaScale = deltaScaledDistance * currScale / getWidth(); // 已经放大后，distance也需要放大
                currScale += deltaScale;
                if (currScale > 5) {
                    currScale = 5;
                }
                canvasView.setScaleX(currScale);
                canvasView.setScaleY(currScale);
                decorLayer.showScrollBar();
            }

            @Override
            public void onUp(float upX, float upY, long upTime, float xVelocity, float yVelocity) {
                touchEnd();
            }

            @Override
            public void onCancel() {
                touchEnd();
            }

            @Override
            public void onInertialScrolling(float deltaMovedX, float deltaMovedY) {
                if (isOperatingImage) {
                    twoFingersGestureDetector.stopInertialScrolling();
                    return;
                }

                float newTransX = canvasView.getTranslationX() + deltaMovedX;
                float transXlimit = (canvasView.getScaleX() - 1) * getWidth() / 2;
                if (Math.abs(newTransX) > transXlimit) {
                    newTransX = newTransX > 0 ? transXlimit : -transXlimit;
                }
                canvasView.setTranslationX(newTransX);
                float newTransY = canvasView.getTranslationY() + deltaMovedY;
                float transYlimit = (canvasView.getScaleY() - 1) * screenNum * getHeight() / 2;
                float multiScreenTransYlimit = 0;
                if (newTransY >= 0) {
                    newTransY = newTransY > transYlimit ? transYlimit : newTransY;
                } else {
                    multiScreenTransYlimit = transYlimit + (screenNum-1) * getHeight();
                    newTransY = -newTransY > multiScreenTransYlimit ? -multiScreenTransYlimit : newTransY;
                }
                canvasView.setTranslationY(newTransY);
                if ((newTransX == transXlimit || newTransX == -transXlimit) &&
                    (newTransY == transYlimit || newTransY == -multiScreenTransYlimit)) {
                    return;
                }
                decorLayer.showScrollBar();
                decorLayer.disappearScrollBarDelayed();
            }
        });
    }

    private void touchEnd() {
        if (!clampBoundsIfNeed()) {
            decorLayer.disappearScrollBarDelayed();
        } else {
            //twoFingersGestureDetector.stopInertialScrolling();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    decorLayer.disappearScrollBarDelayed();
                }
            }, animDuration + 100); // +100容错，保证动画已执行完
        }
    }

    private boolean clampBoundsIfNeed() {
        boolean needClamp = false;
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
            float transYNeedOffset;
            if (transY >= 0) {
                transYNeedOffset = transY > transYlimit ? (transYlimit - transY) : 0;
            } else {
                float multiScreenTransYlimit = transYlimit + (screenNum-1) * getHeight();
                transYNeedOffset = (-transY > multiScreenTransYlimit) ? (-multiScreenTransYlimit - transY) : 0;
            }

            if (transXNeedOffset != 0) {
                animTransX(transX, transX + transXNeedOffset);
                needClamp = true;
            }
            if (transYNeedOffset != 0) {
                animTransY(transY, transY + transYNeedOffset);
                needClamp = true;
            }
        } else {
            animScale(canvasView.getScaleX());
            needClamp = true;
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
        return needClamp;
    }

    private final long animDuration = 400;
    private DecelerateInterpolator interpolator = new DecelerateInterpolator(2f);
    private ValueAnimator vaX;
    private ValueAnimator vaY;
    private ValueAnimator vaScale;

    private void cancelClampBoundsAnim() {
        if (vaX != null && vaX.isRunning()) {
            vaX.cancel();
        }
        if (vaY != null && vaY.isRunning()) {
            vaY.cancel();
        }
        if (vaScale != null && vaScale.isRunning()) {
            vaScale.cancel();
        }
    }

    private void animTransX(float start, float end) {
        vaX = ValueAnimator.ofFloat(start, end).setDuration(animDuration);
        vaX.setInterpolator(interpolator);
        vaX.removeAllUpdateListeners();
        vaX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                canvasView.setTranslationX((float)animation.getAnimatedValue());
                decorLayer.showScrollBar();
            }
        });
        vaX.start();
    }

    private void animTransY(float start, float end) {
        vaY = ValueAnimator.ofFloat(start, end).setDuration(animDuration);
        vaY.setInterpolator(interpolator);
        vaY.removeAllUpdateListeners();
        vaY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                canvasView.setTranslationY((float)animation.getAnimatedValue());
                decorLayer.showScrollBar();
            }
        });
        vaY.start();
    }

    private void animScale(float start) {
        vaScale = ValueAnimator.ofFloat(start, 1).setDuration(animDuration);
        vaScale.setInterpolator(interpolator);
        vaScale.removeAllUpdateListeners();
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
        return twoFingersGestureDetector.onTouchEvent(event);
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
            if (canvasView.getWidth() != canvasView.imageW) {
                float imgScale = (float) canvasView.getWidth() / (float)canvasView.imageW;
                canvasView.imageMatrix.preScale(imgScale, imgScale);
                canvasView.invalidate();
            }
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
            canvas.drawBitmap(sketchBitmap, 0, 0, null);
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

    private class DecorLayer extends View {

        private Paint scrollBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int scrollBarWidth = 8;
        private final int scrollBarWidth1_2 = scrollBarWidth / 2;
        private final int scrollBarMargin = 5;

        private ValueAnimator vaAlpha;
        private int delayTime = 400;
        private int alphaAnimDuration = 300;
        private final int SCROLL_BAR_ALPHA = 100;
        private boolean enableScrollBar = false;
        private Handler scrollBarHandler = new Handler();

        public DecorLayer(Context context) {
            this(context, null);
        }

        public DecorLayer(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public DecorLayer(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            setBackgroundColor(Color.TRANSPARENT);
            scrollBarPaint.setColor(Color.BLACK);
            scrollBarPaint.setAlpha(SCROLL_BAR_ALPHA);
            scrollBarPaint.setStyle(Paint.Style.STROKE);
            scrollBarPaint.setStrokeCap(Paint.Cap.ROUND);
            scrollBarPaint.setStrokeWidth(scrollBarWidth);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!enableScrollBar) return;
            float containerW = getWidth(), containerH = getHeight();
            float scale = canvasView.getScaleX();
            if (scale > 1) { // draw horizontal scroll bar
                float transXlimit = (scale - 1f) * containerW / 2f;
                float scrollBarLength = containerW / scale;
                // transX的范围是从-transXlimit到transXlimit。scrollBar的范围是0到2f/scale倍的transXlimit。
                //float scrollBarLeft = (transXLimit - canvasView.getTranslationX())/2f * (2f/scale);
                float scrollBarLeft = (transXlimit - canvasView.getTranslationX()) / scale;
                float scrollBarRight = scrollBarLeft + scrollBarLength;
                if (scrollBarLeft < scrollBarWidth1_2 + scrollBarMargin) {
                    scrollBarLeft = scrollBarWidth1_2 + scrollBarMargin;
                }
                if (scrollBarLeft > containerW - scrollBarWidth1_2 - scrollBarMargin) {
                    scrollBarLeft = containerW - scrollBarWidth1_2 - scrollBarMargin;
                }
                if (scrollBarRight < scrollBarWidth1_2  + scrollBarMargin) {
                    scrollBarRight = scrollBarWidth1_2 + scrollBarMargin + .1f;
                }
                if (scrollBarRight > containerW - scrollBarWidth1_2) {
                    scrollBarRight = containerW - scrollBarWidth1_2 - scrollBarMargin + .1f;
                }
                canvas.drawLine(scrollBarLeft, containerH - (scrollBarWidth1_2 + scrollBarMargin),
                    scrollBarRight, containerH - (scrollBarWidth1_2 + scrollBarMargin), scrollBarPaint);
            }
            float canvasH = scale * canvasView.getHeight();
            if (canvasH > containerH) { // draw vertical scroll bar
                float transYlimit = (canvasH - canvasView.getHeight()) / 2f;
                float scrollBarLength = containerH * containerH / canvasH;
                float scrollBarTop = (transYlimit - canvasView.getTranslationY()) * containerH / canvasH;
                float scrollBarBottom = scrollBarTop + scrollBarLength;
                if (scrollBarTop < scrollBarWidth1_2 + scrollBarMargin) {
                    scrollBarTop = scrollBarWidth1_2 + scrollBarMargin;
                }
                if (scrollBarTop > containerH - scrollBarWidth1_2 - scrollBarMargin) {
                    scrollBarTop = containerH - scrollBarWidth1_2 - scrollBarMargin;
                }
                if (scrollBarBottom < scrollBarWidth1_2 + scrollBarMargin) {
                    scrollBarBottom = scrollBarWidth1_2 + scrollBarMargin + .1f;
                }
                if (scrollBarBottom > containerH - scrollBarWidth1_2 - scrollBarMargin) {
                    scrollBarBottom = containerH - scrollBarWidth1_2 - scrollBarMargin + .1f;
                }
                canvas.drawLine(containerW - (scrollBarWidth1_2 + scrollBarMargin), scrollBarTop,
                  containerW - (scrollBarWidth1_2 + scrollBarMargin), scrollBarBottom, scrollBarPaint);
            }
        }

        private void disappearScrollBarAnim() {
            vaAlpha = ValueAnimator.ofInt(100, 0).setDuration(alphaAnimDuration);
            vaAlpha.removeAllUpdateListeners();
            vaAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int alpha = (int)animation.getAnimatedValue();
                    if (alpha <= 10) { // 容错
                        vaAlpha.cancel();
                        scrollBarPaint.setAlpha(SCROLL_BAR_ALPHA);
                        enableScrollBar = false;
                        invalidate();
                    } else {
                        scrollBarPaint.setAlpha(alpha);
                        enableScrollBar = true;
                        invalidate();
                    }
                }
            });
            vaAlpha.start();
        }

        public void disappearScrollBarDelayed() {
            scrollBarHandler.removeCallbacksAndMessages(null);
            scrollBarHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disappearScrollBarAnim();
                }
            }, delayTime);
        }

        public void showScrollBar() {
            if (vaAlpha != null && vaAlpha.isRunning()) {
                vaAlpha.cancel();
            }
            scrollBarHandler.removeCallbacksAndMessages(null);
            decorLayer.enableScrollBar = true;
            scrollBarPaint.setAlpha(SCROLL_BAR_ALPHA);
            decorLayer.invalidate();
        }

    }

    public void saveToJPG() {
        Bitmap bp = Bitmap.createBitmap(canvasView.getWidth(), canvasView.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bp);
        canvasView.draw(canvas);
        int drawedHeight = canvasView.getHeight() - 1;
        for (int i = drawedHeight; i >= 0; i--) {
            boolean isBlackLine = true;
            for (int j = 0; j < canvasView.getWidth(); j++) {
                if (bp.getPixel(j, i) != -1) { // -1的二进制位全是1，表示ff ff ff ff。
                    isBlackLine = false;
                    break;
                }
            }
            if (!isBlackLine) {
                drawedHeight = i;
                break;
            }
        }
        if (drawedHeight != canvasView.getHeight()) {
            if (canvasView.getHeight() - drawedHeight >= 5) {
                drawedHeight += 5;
            } else {
                drawedHeight = canvasView.getHeight();
            }
            bp = Bitmap.createBitmap(bp, 0, 0, bp.getWidth(), drawedHeight);
        }
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
