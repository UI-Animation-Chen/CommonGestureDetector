package com.chenzhifei.gesture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Environment;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
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

    private Matrix imageMatrix = new Matrix();

    private CanvasView canvasView;
    private List<PathWithConfig> pathList = new ArrayList<>();
    private Paint paintPen;
    private String paintPenColor = "#00ff00";
    private float paintPenWidth = 2;
    private Paint paintRubber;
    private float paintRubberWidth = 10;

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
        canvasView.setBackgroundColor(Color.parseColor("#ffbbbbbb"));
        addView(canvasView);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams)canvasView.getLayoutParams();
        p.width = LayoutParams.MATCH_PARENT;
        p.height = LayoutParams.MATCH_PARENT;
        canvasView.setLayoutParams(p);
    }

    public void setImageBitmap(Bitmap bitmap) {
        canvasView.setImageBitmap(bitmap);
        canvasView.setScaleType(ImageView.ScaleType.MATRIX);
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
                    imageMatrix.postTranslate(deltaMovedX/canvasViewScale, deltaMovedY/canvasViewScale);
                    canvasView.invalidate();
                    return;
                }
                float newTransX = canvasView.getTranslationX() + deltaMovedX;
                float transXlimit = (canvasView.getScaleX() - 1) * getWidth() / 2;
                if (Math.abs(newTransX) > transXlimit) {
                    newTransX = newTransX > 0 ? transXlimit : -transXlimit;
                }

                float newTransY = canvasView.getTranslationY() + deltaMovedY;
                float transYlimit = (canvasView.getScaleY() - 1) * screenNum * getHeight() / 2;
                if (newTransY >= 0) {
                    newTransY = newTransY > transYlimit ? transYlimit : newTransY;
                } else {
                    float multiScreenTransYlimit = transYlimit + (screenNum-1) * getHeight();
                    newTransY = -newTransY > multiScreenTransYlimit ? -multiScreenTransYlimit : newTransY;
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
                if (isOperatingImage) {
                    float canvasViewScale = canvasView.getScaleX(); // 防止双倍缩放
                    // preScale具有累加效果
                    imageMatrix.preScale(scaleFactor/canvasViewScale, scaleFactor/canvasViewScale);
                    canvasView.invalidate();
                    return;
                }
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
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    PathWithConfig p = new PathWithConfig(paintPenColor, lineMode, event.getX(), event.getY());
                    if (lineMode != LINE_MODE_STRAIGHT) {
                        p.path.moveTo(event.getX(), event.getY());
                    }
                    pathList.add(p);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (lineMode != LINE_MODE_STRAIGHT) {
                        pathList.get(pathList.size() - 1).path.lineTo(event.getX(), event.getY());
                    } else {
                        pathList.get(pathList.size() - 1).setLineEnd(event.getX(), event.getY());
                    }
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
