package com.cogn.wifirecord;

import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static android.util.FloatMath.sqrt;
import static java.lang.Math.pow;

/*
Taken from https://sites.google.com/site/androidhowto/how-to-1/custom-scrollable-image-view
 */
public class ScrollImageView extends View {
    private RecordMenuMaker recordMenuMaker;
    private final int DEFAULT_PADDING = 10;
    private Display mDisplay;
    private Bitmap mImage;
    private float density;
    private int mPadding;
    private long startClickTime;

    /* Current x and y of the touch */
    private float mCurrentX = 0;
    private float mCurrentY = 0;
    private float mTotalX = 0;
    private float mTotalY = 0;
    /* The touch distance change from the current touch */
    private float mDeltaX = 0;
    private float mDeltaY = 0;

    private Float latestCircleX = null;
    private Float latestCircleY = null;
    private Paint latestCirclePaint;
    private List<Float> thisSessionRecordedX = new ArrayList<Float>();
    private List<Float> thisSessionRecordedY = new ArrayList<Float>();
    private Paint thisSessionRecordedPaint;
    private List<Float> previousSessionRecordedX = new ArrayList<Float>();
    private List<Float> previousSessionRecordedY = new ArrayList<Float>();
    private Paint previousSessionRecordedPaint;
    private List<Float> summaryRecordedX = new ArrayList<Float>();
    private List<Float> summaryRecordedY = new ArrayList<Float>();
    private Paint summaryRecordedPaint;



    private String scanProgress = null;
    private Paint textPaint;


    /**
     * Interface to be implemented by activity that creates this view so that a record menu/dialog
     * can be displayed and acted on.
     */
    public interface RecordMenuMaker {
        /**
         * Pops up a menu at the point that has been clicked for a second time.  The calling activity
         * gets to decide what that looks like.
         *
         * @param x actual pixel location of click event.  Not device independent version.
         * @param y
         */
        public void MakeRecordMenu(float x, float y);
    }


    public ScrollImageView(Context context, RecordMenuMaker recordMenuMaker) {
        super(context);
        this.recordMenuMaker = recordMenuMaker;
        initScrollImageView(context);
    }

    private void initScrollImageView(Context context) {
        mDisplay = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mPadding = DEFAULT_PADDING;

        latestCirclePaint = new Paint();
        latestCirclePaint.setColor(Color.GREEN);
        latestCirclePaint.setStrokeWidth(2);
        latestCirclePaint.setStyle(Paint.Style.FILL);
        latestCirclePaint.setAntiAlias(true);

        thisSessionRecordedPaint = new Paint();
        thisSessionRecordedPaint.setColor(Color.BLUE);
        thisSessionRecordedPaint.setStrokeWidth(2);
        thisSessionRecordedPaint.setStyle(Paint.Style.FILL);
        thisSessionRecordedPaint.setAntiAlias(true);

        previousSessionRecordedPaint = new Paint();
        previousSessionRecordedPaint.setColor(Color.CYAN);
        previousSessionRecordedPaint.setStrokeWidth(2);
        previousSessionRecordedPaint.setStyle(Paint.Style.FILL);
        previousSessionRecordedPaint.setAntiAlias(true);

        summaryRecordedPaint = new Paint();
        summaryRecordedPaint.setColor(Color.DKGRAY);
        summaryRecordedPaint.setStrokeWidth(2);
        summaryRecordedPaint.setStyle(Paint.Style.FILL);
        summaryRecordedPaint.setAntiAlias(true);
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
    }

    public Bitmap getImage() {
        return mImage;
    }

    public void setImage(Bitmap image, float density) {
        this.density = density;
        mImage = image;
        mCurrentX = 0;
        mCurrentY = 0;
        mTotalX = 0;
        mTotalY = 0;
        mDeltaX = 0;
        mDeltaY = 0;

        latestCircleX = null;
        latestCircleY = null;
        thisSessionRecordedX  = new ArrayList<Float>();
        thisSessionRecordedY = new ArrayList<Float>();
        previousSessionRecordedX  = new ArrayList<Float>();
        previousSessionRecordedY = new ArrayList<Float>();
        summaryRecordedX  = new ArrayList<Float>();
        summaryRecordedY = new ArrayList<Float>();

    }

    /**
     * Sets the points that have already been recorded on the current floorplan
     *
     * @param previousSessionRecordedX
     * @param previousSessionRecordedY
     * @param summaryRecordedX
     * @param summaryRecordedY
     */
    public void SetPreviousPoints(List<Float> previousSessionRecordedX,
                             List<Float> previousSessionRecordedY,
                             List<Float> summaryRecordedX,
                             List<Float> summaryRecordedY) {
        this.previousSessionRecordedX = previousSessionRecordedX;
        this.previousSessionRecordedY = previousSessionRecordedY;
        this.summaryRecordedX = summaryRecordedX;
        this.summaryRecordedY = summaryRecordedY;
    }

    /**
     * Removes the latest circle added
     */
    public void DeletePoint() {
        latestCircleX = null;
        latestCircleY = null;
        invalidate();
    }

    /**
     * Marks the latest circle as having been recorded
     */
    public void AddRecordedPoint()
    {
        thisSessionRecordedX.add(latestCircleX);
        thisSessionRecordedY.add(latestCircleY);
        latestCircleX = null;
        latestCircleY = null;
    }


    /**
     * Handles scrolling and clicking
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startClickTime = Calendar.getInstance().getTimeInMillis();
            mCurrentX = event.getX();
            mCurrentY = event.getY();
        }
        else if (event.getAction()==MotionEvent.ACTION_UP){
            long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
            float imageX = mCurrentX-mTotalX;
            float imageY = mCurrentY-mTotalY;
            // If there are no circles and there is a click add a cicle
            // If there is a circle and the click is near to it tell the activity to make a menut
            // If there is a circle but the click is not near it, do nothing
            if(clickDuration < 200) {
                //click event has occurred
                boolean addCircle = true;
                if (latestCircleX!=null && latestCircleY!=null) {
                    float dist = sqrt(FloatMath.pow(latestCircleX - imageX, 2) + FloatMath.pow(latestCircleY - imageY, 2));
                    if (dist < 50.0) {
                        recordMenuMaker.MakeRecordMenu(latestCircleX/density, latestCircleY/density);
                    }
                }
                else {
                    latestCircleX = imageX;
                    latestCircleY = imageY;
                }
            }
            invalidate();
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            // Update how much the touch moved
            mDeltaX = x - mCurrentX;
            mDeltaY = y - mCurrentY;

            mCurrentX = x;
            mCurrentY = y;
            invalidate();
        }
        // Consume event
        return true;
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
        else
            mTotalX = Math.max(mTotalX, getMeasuredWidth() - mImage.getWidth() - mPadding);

        float newTotalY = mTotalY + mDeltaY;
        // Don't scroll off the top or bottom edges of the bitmap.
        if (mPadding > newTotalY && newTotalY > getMeasuredHeight() - mImage.getHeight() - mPadding)
            mTotalY += mDeltaY;
        else
            mTotalY = Math.max(mTotalY, getMeasuredHeight() - mImage.getHeight() - mPadding);


        canvas.drawBitmap(mImage, mTotalX, mTotalY, new Paint());

        // Latest circle
        if (latestCircleX!=null &&  latestCircleY!=null) {
            canvas.drawCircle(latestCircleX+mTotalX, latestCircleY+mTotalY, 6*density, latestCirclePaint);
        }

        // This session circles
        DrawCircles(canvas, thisSessionRecordedX, thisSessionRecordedY, thisSessionRecordedPaint);
        /*Iterator<Float> xIter = thisSessionRecordedX.iterator();
        Iterator<Float> yIter = thisSessionRecordedY.iterator();
        while (xIter.hasNext() && yIter.hasNext()){
            canvas.drawCircle(xIter.next()+mTotalX, yIter.next()+mTotalY, 10, thisSessionRecordedPaint);
        }*/

        // Previous session circles
        DrawCircles(canvas, previousSessionRecordedX, previousSessionRecordedY, previousSessionRecordedPaint);

        // Points with summaries
        DrawCircles(canvas, summaryRecordedX, summaryRecordedY, summaryRecordedPaint);

        // The current scan progress
        // TODO: Add a nice partially transparant rectangle to hold the update.
        if (scanProgress!=null) {
            canvas.drawText(scanProgress, latestCircleX+mTotalX, latestCircleY+mTotalY, textPaint);
        }
    }

    /**
     * Add a set of circles to the image.
     * TODO: Only display circles that are in view.
     * @param canvas
     * @param x
     * @param y
     * @param paint
     */
    private void DrawCircles(Canvas canvas, List<Float> x, List<Float> y, Paint paint){
        if (x!=null && y!=null) {
            Iterator<Float> xIter = x.iterator();
            Iterator<Float> yIter = y.iterator();
            while (xIter.hasNext() && yIter.hasNext()) {
                canvas.drawCircle(xIter.next() + mTotalX, yIter.next() + mTotalY, 6 * density, paint);
            }
        }
    }

    private float[] ListToArray(List<Float> list)
    {
        float[] array = new float[list.size()];
        for (int i = 0; i<list.size(); i++)
        {
            array[i] = list.get(i);
        }
        return array;
    }

    private ArrayList<Float> ArrayToList(float[] array)
    {
        ArrayList<Float> list = new ArrayList<Float>();
        for (int i = 0; i<array.length; i++)
        {
            list.add(array[i]);
        }
        return list;
    }

    public Bundle GetState()
    {
        Bundle state = new Bundle();
        if (latestCircleX!=null) {
            state.putFloat("latestCircleX", latestCircleX);
            state.putFloat("latestCircleY", latestCircleY);
        }
        state.putFloatArray("thisSessionRecordedX", ListToArray(thisSessionRecordedX));
        state.putFloatArray("thisSessionRecordedY", ListToArray(thisSessionRecordedY));
        state.putFloat("mTotalX", mTotalX);
        state.putFloat("mTotalY", mTotalY);

        return state;
    }

    public void SetState(Bundle state)
    {
        if (state.containsKey("latestCircleX")) {
            latestCircleX = state.getFloat("latestCircleX");
            latestCircleY = state.getFloat("latestCircleY");
        }else {
            latestCircleX = null;
            latestCircleY = null;
        }
        thisSessionRecordedX = ArrayToList(state.getFloatArray("thisSessionRecordedX"));
        thisSessionRecordedY = ArrayToList(state.getFloatArray("thisSessionRecordedY"));
        // The following does not seem to work since the meaasured width is not known when this is
        // called.  Fixed it in the actual scroll code.
        mTotalX = Math.max(getMeasuredWidth() - mImage.getWidth() - mPadding, state.getFloat("mTotalX"));
        mTotalY = Math.max(getMeasuredHeight() - mImage.getHeight() - mPadding, state.getFloat("mTotalY"));
    }

    /**
     * Show the progress of the current scan on the map.
     * @param newText
     */
    public void UpdateScanProgress(String newText) {
        scanProgress = newText;
        invalidate();
    }

    public void SetScanFinished() {
        scanProgress = null;
        AddRecordedPoint();
        invalidate();
    }
}