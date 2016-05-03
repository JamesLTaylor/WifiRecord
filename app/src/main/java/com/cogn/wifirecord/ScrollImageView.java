package com.cogn.wifirecord;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static android.util.FloatMath.sqrt;
import static java.lang.Math.pow;

/*
Taken from https://sites.google.com/site/androidhowto/how-to-1/custom-scrollable-image-view
 */
public class ScrollImageView extends View {
    private final int DEFAULT_PADDING = 10;
    private Display mDisplay;
    private Bitmap mImage;

    /* Current x and y of the touch */
    public float mCurrentX = 0;
    public float mCurrentY = 0;
    public float mTotalX = 0;
    public float mTotalY = 0;
    /* The touch distance change from the current touch */
    public float mDeltaX = 0;
    public float mDeltaY = 0;

    public List<Float> circleX = new ArrayList<Float>();
    public List<Float> circleY = new ArrayList<Float>();
    public Float latestCircleX = null;
    public Float latestCircleY = null;

    public String scanProgress = null;


    int mDisplayWidth;
    int mDisplayHeight;
    int mPadding;

    public ScrollImageView(Context context) {
        super(context);
        initScrollImageView(context);
    }
    public ScrollImageView(Context context, AttributeSet attributeSet) {
        super(context);
        initScrollImageView(context);
    }

    private void initScrollImageView(Context context) {
        mDisplay = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mPadding = DEFAULT_PADDING;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureDim(widthMeasureSpec, mDisplay.getWidth());
        int height = measureDim(heightMeasureSpec, mDisplay.getHeight());
        setMeasuredDimension(width, height);
    }

    private int measureDim(int measureSpec, int size) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = size;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public void setImage(Bitmap image) {
        mImage = image;
        mCurrentX = 0;
        mCurrentY = 0;
        mTotalX = 0;
        mTotalY = 0;
        mDeltaX = 0;
        mDeltaY = 0;

        circleX = new ArrayList<Float>();
        circleY = new ArrayList<Float>();
        latestCircleX = null;
        latestCircleY = null;
    }

    public int getPadding() {
        return mPadding;
    }

    public void setPadding(int padding) {
        this.mPadding = padding;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mImage == null) {
            return;
        }

        float newTotalX = mTotalX + mDeltaX;
        // Don't scroll off the left or right edges of the bitmap.
        if (mPadding > newTotalX && newTotalX > getMeasuredWidth() - mImage.getWidth() - mPadding)
            mTotalX += mDeltaX;

        float newTotalY = mTotalY + mDeltaY;
        // Don't scroll off the top or bottom edges of the bitmap.
        if (mPadding > newTotalY && newTotalY > getMeasuredHeight() - mImage.getHeight() - mPadding)
            mTotalY += mDeltaY;

        Paint paint = new Paint();
        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.GREEN);
        circlePaint.setStrokeWidth(2);
        circlePaint.setStyle(Paint.Style.FILL);

        canvas.drawBitmap(mImage, mTotalX, mTotalY, paint);
        Iterator<Float> xIter = circleX.iterator();
        Iterator<Float> yIter = circleY.iterator();
        while (xIter.hasNext() && yIter.hasNext()){
            canvas.drawCircle(xIter.next()+mTotalX, yIter.next()+mTotalY, 10, circlePaint);
        }
        Paint textPaint = new Paint();
        paint.setColor(Color.BLACK);
        if (scanProgress!=null) {
            canvas.drawText(scanProgress, mCurrentX, mCurrentY, textPaint);
        }

    }
}