package com.keyeswest.rollview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import static java.lang.Math.min;


// Attribution: http://pygmalion.nitri.org/android-gauge-view-1039.html

public class RollView extends View {

    private static final String TAG = "RollView";

    //---------------------- Gauge Rim Properties -----------------------------
    private RectF rimRect;           // Bounding rectangle for the rim of the gauge
    private Paint rimPaint;          // Paint for the rim
    private Paint rimCirclePaint;
    private Paint rimShadowPaint;

    //---------------------- Canvas Geometry ----------------------------------
    private float canvasCenterX;
    private float canvasCenterY;
    private float canvasWidth;       // corresponds to the width of the view
    private float canvasHeight;      // corresponds to the height of the view

    // --------------------- Gauge Face Properties ----------------------------
    private RectF faceRect;
    private Paint facePaint;

    // --------------------- Gauge Scale Properties ----------------------------
    private Paint scalePaint;
    private RectF scaleRect;
    private int totalGaugeTics = 181;
    private float degreesPerGaugeTic =  180.0f/ (totalGaugeTics-1);
    private float minGaugeValue = -90.0f;
    private float maxGaugeValue = 90.0f;


    // label each 15 degree tic mark
    private int majorTicInterval = 15;

    // --------------------- Label Properties ----------------------------
    private float labelRadius;
    private Paint labelPaint;
    private Paint upperTextPaint;
    private Paint lowerTextPaint;


    // --------------------- Needle Properties ----------------------------
    private float needleValue = 0;
    private Paint needlePaint;
    private Path needlePath;
    private Paint needleScrewPaint;
    private float needleTailLength;
    private float needleWidth;
    private float needleLength;
    private float newNeedlePosition = 0f;


    private long lastMoveTime;

    // --------------------- View Constructors ----------------------------
    public RollView(Context context) {
        super(context);
        initialize();
    }

    public RollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public RollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }


    private void initialize(){
        initValues();
        initPaint();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged invoked");

        canvasWidth = (float) w;   // width of the view
        canvasHeight = (float) h;  // height of the view
        canvasCenterX = w / 2f;    // horizontal center
        canvasCenterY = h / 2f;    // vertical center

        //Bounding rectangle for the gauge.
        // Width takes up 90% of canvas width (5% margin on left and right)
        // Height takes up 90% of height      (5% margin on top and bottom)
        rimRect = new RectF(canvasWidth * .05f, canvasHeight * .05f,
                canvasWidth * 0.95f, canvasHeight * 0.95f);

        // Gradient for the rim layer which is underneath the face layer
        rimPaint.setShader(new LinearGradient(canvasWidth * 0.40f, canvasHeight * 0.0f,
                canvasWidth * 0.60f, canvasHeight * 1.0f,
                Color.rgb(0xf0, 0xf5, 0xf0),
                Color.rgb(0x30, 0x31, 0x30),
                Shader.TileMode.CLAMP));

        // the bounding rectangle for the face is 2% smaller than the bounding rim rectangle
        float rimSize = 0.02f * canvasWidth;
        faceRect = new RectF();
        faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize,
                rimRect.right - rimSize, rimRect.bottom - rimSize);

        //note -- revisit consider determining the smaller of the width and height and making
        // the bounding rectangle a square centered in view

        rimShadowPaint.setShader(new RadialGradient(0.5f * canvasWidth, 0.5f * canvasHeight, faceRect.width() / 2.0f,
                new int[]{0x00000000, 0x00000500, 0x50000500},
                new float[]{0.96f, 0.96f, 0.99f},
                Shader.TileMode.MIRROR));

        scalePaint.setStrokeWidth(0.005f * canvasWidth);
        scalePaint.setTextSize(0.045f * canvasWidth);
        scalePaint.setTextScaleX(0.8f * canvasWidth);

        float scalePosition = 0.015f * canvasWidth;
        scaleRect = new RectF();
        scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
                faceRect.right - scalePosition, faceRect.bottom - scalePosition);

        labelRadius = (canvasCenterX - scaleRect.left) * 0.70f;

        float requestedLabelTextSize = 42;
        labelPaint.setTextSize(requestedLabelTextSize);

        float textSize = w / 14f;
        upperTextPaint.setTextSize(textSize);
        lowerTextPaint.setTextSize(textSize);

        needleTailLength = canvasWidth / 12f;
        needleWidth = canvasWidth / 98f;
        needleLength = (canvasWidth / 2f) * 0.8f;

        needlePaint.setStrokeWidth(canvasWidth / 197f);

        needlePaint.setShadowLayer(canvasWidth / 123f, canvasWidth / 10000f,
                canvasWidth / 10000f, Color.GRAY);

        // set the path for drawing the needle
        setNeedle();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d(TAG, "onMeasure invoked");

        int size;
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();

        if (widthWithoutPadding > heightWithoutPadding) {
            size = heightWithoutPadding;
        } else {
            size = widthWithoutPadding;
        }

        setMeasuredDimension(size + getPaddingLeft() + getPaddingRight(), size + getPaddingTop() + getPaddingBottom());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw invoked");

        drawRim(canvas);
        drawFace(canvas);
        drawScale(canvas);
        drawLabels(canvas);

        // rotate the canvas so the needle aligns with the x-ais
        canvas.rotate(needleValue -90f, canvasCenterX,
                canvasCenterY);
        canvas.drawPath(needlePath, needlePaint);
        canvas.drawCircle(canvasCenterX, canvasCenterY, canvasWidth / 61f, needleScrewPaint);

        if (needsToMove()) {
            moveNeedle();
            invalidate();
        }
    }

    private void initValues() {

        needleValue = newNeedlePosition;
    }

    private void initPaint(){

        setSaveEnabled(true);
        int needleColor = getResources().getColor(R.color.needleColor);

        rimPaint = new Paint();
        rimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        rimCirclePaint = new Paint();
        rimCirclePaint.setAntiAlias(true);
        rimCirclePaint.setStyle(Paint.Style.STROKE);
        rimCirclePaint.setColor(getResources().getColor(R.color.rimCirclePaint));
        rimCirclePaint.setStrokeWidth(0.005f);

        facePaint = new Paint();
        facePaint.setAntiAlias(true);
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setColor(getResources().getColor(R.color.facePaint));

        rimShadowPaint = new Paint();
        rimShadowPaint.setStyle(Paint.Style.FILL);

        scalePaint = new Paint();
        scalePaint.setStyle(Paint.Style.STROKE);

        scalePaint.setAntiAlias(true);
        int scaleColor = 0x9f004d0f;
        scalePaint.setColor(scaleColor);

        labelPaint = new Paint();
        labelPaint.setColor(scaleColor);
        labelPaint.setTypeface(Typeface.SANS_SERIF);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        upperTextPaint = new Paint();
        upperTextPaint.setColor(scaleColor);
        upperTextPaint.setTypeface(Typeface.SANS_SERIF);
        upperTextPaint.setTextAlign(Paint.Align.CENTER);

        lowerTextPaint = new Paint();
        lowerTextPaint.setColor(scaleColor);
        lowerTextPaint.setTypeface(Typeface.SANS_SERIF);
        lowerTextPaint.setTextAlign(Paint.Align.CENTER);

        needlePaint = new Paint();
        needlePaint.setColor(needleColor);
        needlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        needlePaint.setAntiAlias(true);

        needlePath = new Path();

        needleScrewPaint = new Paint();
        needleScrewPaint.setColor(Color.BLACK);
        needleScrewPaint.setAntiAlias(true);

    }


    // The needle path definition requires that the canvas be rotated so that the needle aligns
    // with the x-axis
    private void setNeedle() {
        needlePath.reset();
        // move to the end to the needle tail (on the x-axis)
        needlePath.moveTo(canvasCenterX - needleTailLength, canvasCenterY);
        // draw a line corresponding to the bottom half of the needle tail to the screw
        needlePath.lineTo(canvasCenterX, canvasCenterY - (needleWidth / 2));
        // draw the needle length along the x-axis
        needlePath.lineTo(canvasCenterX + needleLength, canvasCenterY);
        // draw back to the screw forming the top of the needle
        needlePath.lineTo(canvasCenterX, canvasCenterY + (needleWidth / 2));
        // draw teh top half of the tail
        needlePath.lineTo(canvasCenterX - needleTailLength, canvasCenterY);

        // the pivot point
        needlePath.addCircle(canvasCenterX, canvasCenterY, canvasWidth / 49f, Path.Direction.CW);
        needlePath.close();

        needleScrewPaint.setShader(new RadialGradient(canvasCenterX, canvasCenterY, needleWidth / 2,
                Color.DKGRAY, Color.BLACK, Shader.TileMode.CLAMP));
    }


    private void drawRim(Canvas canvas){
        canvas.drawArc(rimRect, 180, 360, true, rimPaint);
        canvas.drawArc(rimRect, 180, 360, true, rimCirclePaint);
    }

    private void drawFace(Canvas canvas) {
        canvas.drawArc(faceRect, 180, 180, true,facePaint);
        canvas.drawArc(faceRect, 180,180, true, rimCirclePaint);
        canvas.drawArc(faceRect, 180, 180, true, rimShadowPaint);
    }

    private void drawScale(Canvas canvas) {

        canvas.save();
        canvas.rotate(-90, 0.5f * canvasWidth, 0.5f * canvasHeight);
        for (int i = 0; i < totalGaugeTics; ++i) {
            float y1 = scaleRect.top;
            float y2 = y1 + (0.020f * canvasHeight);
            float y3 = y1 + (0.060f * canvasHeight);
            float y4 = y1 + (0.030f * canvasHeight);

            float value =i - 90;

            if (value >= minGaugeValue && value <= maxGaugeValue) {
                canvas.drawLine(0.5f * canvasWidth, y1, 0.5f * canvasWidth, y2, scalePaint);

                if (i % majorTicInterval == 0) {
                    canvas.drawLine(0.5f * canvasWidth, y1, 0.5f * canvasWidth, y3, scalePaint);
                }

                if (i % (majorTicInterval / 3) == 0) {
                    canvas.drawLine(0.5f * canvasWidth, y1, 0.5f * canvasWidth, y4, scalePaint);
                }
            }

            canvas.rotate(degreesPerGaugeTic, 0.5f * canvasWidth, 0.5f * canvasHeight);
        }
        canvas.restore();
    }

    private void drawLabels(Canvas canvas) {
        for (int i = 0; i < totalGaugeTics; i += majorTicInterval) {
            int value = i - 90;
            if (value >= minGaugeValue && value <= maxGaugeValue) {
                float scaleAngle = (i-90) * degreesPerGaugeTic;
                float scaleAngleRads = (float) Math.toRadians(scaleAngle);
                //Log.d(TAG, "i = " + i + ", angle = " + scaleAngle + ", newNeedlePosition = " + newNeedlePosition);
                float deltaX = labelRadius * (float) Math.sin(scaleAngleRads);
                float deltaY = labelRadius * (float) Math.cos(scaleAngleRads);
                String valueLabel = String.valueOf(value);
                drawTextCentered(valueLabel, canvasCenterX + deltaX, canvasCenterY - deltaY, labelPaint, canvas);
            }
        }
    }


    private void drawTextCentered(String text, float x, float y, Paint paint, Canvas canvas) {
        //float xPos = x - (paint.measureText(text)/2f);
        float yPos = (y - ((paint.descent() + paint.ascent()) / 2f));
        canvas.drawText(text, x, yPos, paint);
    }


    // newNeedlePosition is the new needle position
    private boolean needsToMove() {
        return Math.abs(needleValue - newNeedlePosition) > 0;
    }

    private void moveNeedle() {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastMoveTime;

        int deltaTimeInterval = 5;
        if (deltaTime >= deltaTimeInterval) {
            float difference = Math.abs(newNeedlePosition - needleValue);
            float needleStep = 1f;
            if (difference >= needleStep) {

                float increment = min(2.0f, difference);
                if (newNeedlePosition > needleValue) {

                    needleValue += increment ;
                } else {
                    needleValue -= increment ;
                }
                lastMoveTime = System.currentTimeMillis();
            }

        }
    }


}
