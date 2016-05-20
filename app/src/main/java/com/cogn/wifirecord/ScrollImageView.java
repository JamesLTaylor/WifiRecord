package com.cogn.wifirecord;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/*
Taken from https://sites.google.com/site/androidhowto/how-to-1/custom-scrollable-image-view
 */
public class ScrollImageView extends View {
    private String movementStatus = null;

    public enum ViewMode {LOCATE, RECORD}
    private RecordMenuMaker recordMenuMaker;
    private final int DEFAULT_PADDING = 10;
    private Display mDisplay;
    private Bitmap mImage;
    private float density;
    private int mPadding;
    private long startClickTime;
    private ViewMode viewMode;


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
    private List<Float> summaryRecordedX = new ArrayList<>();
    private List<Float> summaryRecordedY = new ArrayList<>();
    private List<String> summaryScores = new ArrayList<>();
    private Paint summaryRecordedPaint;
    private Float bestGuessX = null;
    private Float bestGuessY = null;
    private Paint bestGuessPaint;

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

    public void SetViewMode(ViewMode viewMode){
        this.viewMode = viewMode;
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
        summaryRecordedPaint.setColor(Color.YELLOW);
        summaryRecordedPaint.setStrokeWidth(2);
        summaryRecordedPaint.setStyle(Paint.Style.FILL);
        summaryRecordedPaint.setAntiAlias(true);
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(16);
        textPaint.setFakeBoldText(true);

        bestGuessPaint = new Paint();
        bestGuessPaint.setColor(Color.GREEN);
        bestGuessPaint.setStrokeWidth(2);
        bestGuessPaint.setStyle(Paint.Style.FILL);
        bestGuessPaint.setAlpha(100);
        bestGuessPaint.setAntiAlias(true);

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

    private List<Float> MultiplyByDensity(List<Float> newValues){
        if (newValues==null) return null;
        List<Float> result = new ArrayList<>(newValues.size());
        for (int i = 0; i<newValues.size(); i++) {
            result.add(i, newValues.get(i)*density);
        }
        return result;
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
        this.previousSessionRecordedX = MultiplyByDensity(previousSessionRecordedX);
        this.previousSessionRecordedY = MultiplyByDensity(previousSessionRecordedY);
        this.summaryRecordedX = MultiplyByDensity(summaryRecordedX);
        this.summaryRecordedY = MultiplyByDensity(summaryRecordedY);
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

    public void UpdateMovementStatus(String movementStatus) {
        this.movementStatus = movementStatus;
    }


    /**
     * Handles scrolling and clicking
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (viewMode) {
            case RECORD: {
                onTouchEventRecord(event);
                break;
            }
            case LOCATE:
                onTouchEventLocate(event);
                break;
        }
        return true;
    }

    private boolean onTouchEventLocate(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mCurrentX = event.getX();
            mCurrentY = event.getY();
            Log.d("SCROLL", "mouse down");
            return true;
        }
        else if (event.getAction()==MotionEvent.ACTION_UP){
            Log.d("SCROLL", "mouse up");
            mDeltaX = 0;
            mDeltaY = 0;
            return true;
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
            Log.d("SCROLL", "mouse move");
            return true;
        }
        // Consume event
        return true;
    }


    private boolean onTouchEventRecord(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startClickTime = Calendar.getInstance().getTimeInMillis();
            mCurrentX = event.getX();
            mCurrentY = event.getY();
            return true;
        }
        else if (event.getAction()==MotionEvent.ACTION_UP){
            long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
            float imageX = mCurrentX-mTotalX;
            float imageY = mCurrentY-mTotalY;
            // If there are no circles and there is a click add a cicle
            // If there is a circle and the click is near to it tell the activity to make a menu
            // If there is a circle but the click is not near it, replace the latest circle
            if(clickDuration < 200) {
                //click event has occurred
                boolean addCircle = true;
                if (latestCircleX!=null && latestCircleY!=null) {
                    double dist = Math.sqrt(Math.pow(latestCircleX - imageX, 2) + Math.pow(latestCircleY - imageY, 2));
                    if (dist < 50.0) {
                        recordMenuMaker.MakeRecordMenu(latestCircleX/density, latestCircleY/density);
                    }
                    else {
                        latestCircleX = imageX;
                        latestCircleY = imageY;
                    }
                }
                else {
                    latestCircleX = imageX;
                    latestCircleY = imageY;
                }
            }
            invalidate();
            return true;
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
        // Repair for any shifting that may have happened on a rotate
        if (mTotalX >= mPadding)
            mTotalX = mPadding-1;
        if (mTotalX <= getMeasuredWidth() - mImage.getWidth() - mPadding)
            mTotalX = getMeasuredWidth() - mImage.getWidth() - mPadding + 1;
        if (mTotalY >= mPadding)
            mTotalY = mPadding-1;
        if (mTotalY <= getMeasuredHeight() - mImage.getHeight() - mPadding)
            mTotalY = getMeasuredHeight() - mImage.getHeight() - mPadding + 1;


        float newTotalX = mTotalX + mDeltaX;
        // Don't scroll off the left or right edges of the bitmap.
        if (mPadding > newTotalX && newTotalX > getMeasuredWidth() - mImage.getWidth() - mPadding)
            mTotalX += mDeltaX;

        float newTotalY = mTotalY + mDeltaY;
        // Don't scroll off the top or bottom edges of the bitmap.
        if (mPadding > newTotalY && newTotalY > getMeasuredHeight() - mImage.getHeight() - mPadding)
            mTotalY += mDeltaY;
        canvas.drawBitmap(mImage, mTotalX, mTotalY, new Paint());

        if (viewMode==ViewMode.RECORD) {
            // Latest circle
            if (latestCircleX != null && latestCircleY != null) {
                canvas.drawCircle(latestCircleX + mTotalX, latestCircleY + mTotalY, 6 * density, latestCirclePaint);
            }
            // This session circles
            DrawCircles(canvas, thisSessionRecordedX, thisSessionRecordedY, thisSessionRecordedPaint);
            // Previous session circles
            DrawCircles(canvas, previousSessionRecordedX, previousSessionRecordedY, previousSessionRecordedPaint);
            // Points with summaries
            DrawCircles(canvas, summaryRecordedX, summaryRecordedY, summaryRecordedPaint);
            // The current scan progress
            // TODO: Add a nice partially transparant rectangle to hold the update.
            if (scanProgress != null) {
                canvas.drawText(scanProgress, latestCircleX + mTotalX, latestCircleY + mTotalY, textPaint);
            }
        } else if (viewMode==ViewMode.LOCATE) {
            // Points with summaries
            DrawCircles(canvas, summaryRecordedX, summaryRecordedY, summaryRecordedPaint);
            if (bestGuessX != null && bestGuessY != null) {
                canvas.drawCircle(bestGuessX + mTotalX, bestGuessY + mTotalY, 40 * density, bestGuessPaint);
            }

            Iterator<Float> xIter = summaryRecordedX.iterator();
            Iterator<Float> yIter = summaryRecordedY.iterator();
            Iterator<String> scoreIter = summaryScores.iterator();

            while (xIter.hasNext() && yIter.hasNext() && scoreIter.hasNext()) {
                canvas.drawText(scoreIter.next(), xIter.next() + mTotalX, yIter.next() + mTotalY, textPaint);
            }

            if (movementStatus!=null){

                Paint textRectPaint = new Paint();
                textRectPaint.setAlpha(50);
                textRectPaint.setColor(Color.WHITE);
                Rect textRect = new Rect();
                textPaint.getTextBounds(movementStatus, 0, movementStatus.length(), textRect);
                canvas.drawRect(textRect, textRectPaint);
                canvas.drawText(movementStatus, 20, 20, textPaint);

            }

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
        state.putFloatArray("summaryRecordedX", ListToArray(summaryRecordedX));
        state.putFloatArray("summaryRecordedY", ListToArray(summaryRecordedY));
        state.putFloat("mTotalX", mTotalX);
        state.putFloat("mTotalY", mTotalY);
        state.putSerializable("viewMode", viewMode);

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
        summaryRecordedX = ArrayToList(state.getFloatArray("summaryRecordedX"));
        summaryRecordedY = ArrayToList(state.getFloatArray("summaryRecordedY"));
        mTotalX = state.getFloat("mTotalX");
        mTotalY = state.getFloat("mTotalY");
        viewMode = (ViewMode)state.getSerializable("viewMode");
    }

    /**
     * Show the progress of the current scan on the map.
     * @param newText
     */
    public void UpdateRecordProgress(String newText) {
        scanProgress = newText;
        invalidate();
    }

    /**
     *
     * @param summaryScores the scores for the current level.  The list must be exactly the same
     *                      length as the x and y lists.
     * @param bestGuessX
     * @param bestGuessY
     */
    public void UpdateLocateProgress(List<String> summaryScores, float bestGuessX, float bestGuessY) {
        this.summaryScores = summaryScores;
        this.bestGuessX = bestGuessX*density;
        this.bestGuessY = bestGuessY*density;
        invalidate();
    }

    public void SetScanFinished() {
        scanProgress = null;
        AddRecordedPoint();
        invalidate();
    }
}