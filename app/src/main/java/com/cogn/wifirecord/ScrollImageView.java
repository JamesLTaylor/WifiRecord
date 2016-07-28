package com.cogn.wifirecord;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
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
    private float originalTouchX;
    private float originalTouchY;

    private RecordMenuMaker recordMenuMaker;
    private final int DEFAULT_PADDING = 0;
    private Display mDisplay;
    private Bitmap mImage;
    private Integer currentLevel;
    private Paint imagePaint = new Paint();
    private float density;
    private int mPadding;
    private long startClickTime;
    private boolean autoScroll = false;

    /* Current x and y of the touch */
    private float latestTouchX = 0;
    private float latestTouchY = 0;
    // Total offset of image
    private float mTotalX = 0;
    private float mTotalY = 0;
    /* The touch distance change from the current touch */
    private float mDeltaX = 0;
    private float mDeltaY = 0;

    // For both
    private List<Float> summaryRecordedX = new ArrayList<>();
    private List<Float> summaryRecordedY = new ArrayList<>();
    private Paint summaryRecordedPaint;

    // For recording
    private Float currentX = null;
    private Float currentY = null;

    // For location
    private List<String> summaryScores = new ArrayList<>();
    private Float bestGuessX = null;
    private Float bestGuessY = null;
    private Float bestGuessRadius = null;

    private Paint bestGuessPaint;
    private Paint currentPaint;

    // Text on screen
    private String scanProgress = null;
    private Paint textPaint;

    //Shops and paths
    private List<String> shopNames;
    private List<Float> shopXs;
    private List<Float> shopYs;
    private List<Integer> shopLevels;

    private Paint routePaint;
    private Paint shopRectPaint;
    private Paint shopTextPaint;
    private boolean showDebug = true;
    private Float screenCenterX = null;
    private Float screenCenterY = null;

    public void setShowDebug(boolean showDebug) {
        this.showDebug = showDebug;
        invalidate();
    }


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
         * @param y actual pixel location of click event
         */
        void makeRecordMenu(float x, float y);
    }


    public ScrollImageView(Context context, RecordMenuMaker recordMenuMaker) {
        super(context);
        this.recordMenuMaker = recordMenuMaker;
        initScrollImageView(context);
    }

    private void initScrollImageView(Context context) {
        mDisplay = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mPadding = DEFAULT_PADDING;

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

        currentPaint = new Paint();
        currentPaint.setColor(Color.RED);
        currentPaint.setStrokeWidth(2);
        currentPaint.setStyle(Paint.Style.FILL);
        currentPaint.setAlpha(100);
        currentPaint.setAntiAlias(true);

        shopNames = new ArrayList<>();
        shopXs = new ArrayList<>();
        shopYs = new ArrayList<>();
        shopLevels = new ArrayList<>();
        shopRectPaint = new Paint();
        shopRectPaint.setColor(Color.GRAY);
        shopRectPaint.setStrokeWidth(2);
        shopRectPaint.setStyle(Paint.Style.FILL);
        shopRectPaint.setAntiAlias(true);

        shopTextPaint = new Paint();
        shopTextPaint.setColor(Color.GREEN);
        shopTextPaint.setTextSize(24);
        textPaint.setTypeface(Typeface.create("Arial", Typeface.BOLD));

        routePaint = new Paint();
        routePaint.setColor(Color.RED);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeWidth(8);
        routePaint.setStyle(Paint.Style.FILL);
        routePaint.setAlpha(200);
        routePaint.setAntiAlias(true);


    }

    public Bitmap getImage() {
        return mImage;
    }

    /**
     * Centres the image on the current location.
     */
    public void centerOnXY(float x, float y) {
        mTotalX = getMeasuredWidth() / 2 - x;
        mTotalY = getMeasuredHeight() / 2 - y;
        if (mTotalX > mPadding)
            mTotalX = mPadding;
        if (mTotalX <= getMeasuredWidth() - mImage.getWidth() - mPadding)
            mTotalX = getMeasuredWidth() - mImage.getWidth() - mPadding + 1;
        if (mTotalY > mPadding)
            mTotalY = mPadding;
        if (mTotalY <= getMeasuredHeight() - mImage.getHeight() - mPadding)
            mTotalY = getMeasuredHeight() - mImage.getHeight() - mPadding + 1;
    }
    
    public Position getCurrentPosition(){
        if (currentX == null) return null;
        return new Position(currentX/density, currentY/density, currentLevel);
    }

    public void setCurrentPosition(Position newPosition){
        currentX = newPosition.x;
        currentY = newPosition.y;

    }

    /**
     *
     * @return null if not enough has been set yet.
     */
    public Position getScreenCenter() {
        if (currentLevel==null) return null;
        float screenCenterX = getMeasuredWidth() / 2  - mTotalX;
        float screenCenterY = getMeasuredHeight() / 2  - mTotalY;
        return new Position(screenCenterX, screenCenterY, currentLevel);
    }

    public void setImage(Bitmap image, float density, int displayLevel) {
        this.density = density;
        this.currentLevel = displayLevel;
        mImage = image;
        latestTouchX = 0;
        latestTouchY = 0;

        mTotalX = Math.min(0, -image.getWidth()/2 + getMeasuredWidth()/2);
        mTotalY = Math.min(0, -image.getHeight()/2 + getMeasuredHeight()/2);

        //mTotalX = 0;
        //mTotalY = 0;
        mDeltaX = 0;
        mDeltaY = 0;

        currentX = null;
        currentY = null;
        summaryRecordedX  = new ArrayList<>();
        summaryRecordedY = new ArrayList<>();

    }

    private List<Float> multiplyByDensity(List<Float> newValues){
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
     */
    public void setPreviousPoints(List<Float> summaryRecordedX,
                                  List<Float> summaryRecordedY) {
        this.summaryRecordedX = multiplyByDensity(summaryRecordedX);
        this.summaryRecordedY = multiplyByDensity(summaryRecordedY);
    }

    /**
     * Removes the latest circle added
     */
    public void deletePoint() {
        currentX = null;
        currentY = null;
        invalidate();
    }

    public void setAutoScroll(boolean autoScroll) {this.autoScroll = autoScroll;}

    public void addShop(String shopName, float x, float y, int level) {
        shopNames.add(shopName);
        shopXs.add(x*density);
        shopYs.add(y*density);
        shopLevels.add(level);
    }

    public void updateMovementStatus(String movementStatus) {
        this.movementStatus = movementStatus;
        invalidate();
    }


    /**
     * Handles scrolling and clicking
     *
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        onTouchEventRecord(event);
        return true;
    }

    private boolean onTouchEventLocate(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            latestTouchX = event.getX();
            latestTouchY = event.getY();
            return true;
        }
        else if (event.getAction()==MotionEvent.ACTION_UP){
            mDeltaX = 0;
            mDeltaY = 0;
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            // Update how much the touch moved
            mDeltaX = x - latestTouchX;
            mDeltaY = y - latestTouchY;

            latestTouchX = x;
            latestTouchY = y;
            invalidate();
            return true;
        }
        // Consume event
        return true;
    }


    private boolean onTouchEventRecord(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startClickTime = Calendar.getInstance().getTimeInMillis();
            originalTouchX = event.getX();
            originalTouchY = event.getY();
            latestTouchX = event.getX();
            latestTouchY = event.getY();
            return true;
        }
        else if (event.getAction()==MotionEvent.ACTION_UP){
            long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
            float x = event.getX();
            float y = event.getY();
            float imageX = latestTouchX -mTotalX;
            float imageY = latestTouchY -mTotalY;
            // If there are no circles and there is a click add a cicle
            // If there is a circle and the click is near to it tell the activity to make a menu
            // If there is a circle but the click is not near it, replace the latest circle
            if(clickDuration < 200 && currentX!=null && currentY!=null) {  // Short click on circle
                //click event has occurred
                double dist = Math.sqrt(Math.pow(currentX - imageX, 2) + Math.pow(currentY - imageY, 2));
                if (dist < 50.0) {
                    recordMenuMaker.makeRecordMenu(currentX / density, currentY / density);
                }
            }
            else if (clickDuration>500 && Math.abs(originalTouchX-x)<10 && Math.abs(originalTouchY-y)<10)
            {
                 currentX = imageX;
                 currentY = imageY;
            }
            mDeltaX = 0;
            mDeltaY = 0;
            invalidate();
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            // Update how much the touch moved
            mDeltaX = x - latestTouchX;
            mDeltaY = y - latestTouchY;

            latestTouchX = x;
            latestTouchY = y;
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
        int result;
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
        canvas.drawBitmap(mImage, mTotalX, mTotalY, imagePaint);
        if (screenCenterX!=null)
        {
            centerOnXY(screenCenterX, screenCenterY);
            screenCenterX = null;
            screenCenterY = null;
        }
        // Repair for any shifting that may have happened on a rotate
        if (mTotalX > mPadding)
            mTotalX = mPadding;
        if (mTotalX <= getMeasuredWidth() - mImage.getWidth() - mPadding)
            mTotalX = getMeasuredWidth() - mImage.getWidth() - mPadding + 1;
        if (mTotalY > mPadding)
            mTotalY = mPadding;
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

        // Add shop names and path to canvas
        if (shopNames.size() > 0) {
            for (int i = 0; i < shopNames.size(); i++) {
                if (shopLevels.get(i) == currentLevel) {
                    drawShopTag(canvas, shopNames.get(i), shopXs.get(i), shopYs.get(i));
                }
            }
        }
        if (GlobalDataFragment.latestRoute != null) {
            Route route = GlobalDataFragment.latestRoute;
            for (int i = 0; i < (GlobalDataFragment.latestRoute.size() - 1); i++) {
                if (route.get(i).level == currentLevel) {
                    if (route.get(i).level == route.get(i + 1).level) {
                        float x1 = route.get(i).x * density + mTotalX;
                        float y1 = route.get(i).y * density + mTotalY;
                        float x2 = route.get(i + 1).x * density + mTotalX;
                        float y2 = route.get(i + 1).y * density + mTotalY;
                        canvas.drawLine(x1, y1, x2, y2, routePaint);
                    } else if (route.get(i).level > route.get(i + 1).level) {
                        drawShopTag(canvas, "Go down", route.get(i).x * density, route.get(i).y * density);
                    } else if (route.get(i).level < route.get(i + 1).level) {
                        drawShopTag(canvas, "Go up", route.get(i).x * density, route.get(i).y * density);
                    }
                }
            }
            for (Route.Description description : route.descriptions) {
                Position entranceLocation = description.shop.entranceLocations.get(description.entranceNumber);
                canvas.drawLine(description.pathX * density, description.pathY * density, entranceLocation.x * density, entranceLocation.y * density, shopRectPaint);
                if (entranceLocation.level == currentLevel) {
                    drawShopTag(canvas, description.shop.getName(), entranceLocation.x * density, entranceLocation.y * density);
                }
            }
        }


        // Latest circle
        if (currentX != null && currentY != null) {
            canvas.drawCircle(currentX + mTotalX, currentY + mTotalY, 20 * density, currentPaint);
        }

        // The current scan progress
        // TODO: Add a nice partially transparant rectangle to hold the update.
        if (scanProgress != null) {
            canvas.drawText(scanProgress, currentX + mTotalX, currentY + mTotalY, textPaint);
        }
        // put the current location in the middle
        // Repair for any shifting that may have happened on a rotate
        if (autoScroll && currentX != null) {
            centerOnXY(currentX, currentY);
        }

        if (showDebug) {
            // Points with summaries
            drawCircles(canvas, summaryRecordedX, summaryRecordedY, summaryRecordedPaint);
            if (bestGuessX != null && bestGuessY != null) {
                canvas.drawCircle(bestGuessX + mTotalX, bestGuessY + mTotalY, bestGuessRadius * density, bestGuessPaint);
            }

            Iterator<Float> xIter = summaryRecordedX.iterator();
            Iterator<Float> yIter = summaryRecordedY.iterator();
            Iterator<String> scoreIter = summaryScores.iterator();

            while (xIter.hasNext() && yIter.hasNext() && scoreIter.hasNext()) {
                canvas.drawText(scoreIter.next(), xIter.next() + mTotalX, yIter.next() + mTotalY, textPaint);
            }
        }

        if (movementStatus != null) {

            Paint textRectPaint = new Paint();
            textRectPaint.setAlpha(50);
            textRectPaint.setColor(Color.WHITE);
            Rect textRect = new Rect();
            textPaint.getTextBounds(movementStatus, 0, movementStatus.length(), textRect);
            canvas.drawRect(textRect, textRectPaint);
            canvas.drawText(movementStatus, 20, 20, textPaint);

        }
    }


    /**
     * Add a set of circles to the image.
     * TODO: Only display circles that are in view.
     */
    private void drawCircles(Canvas canvas, List<Float> x, List<Float> y, Paint paint){
        if (x!=null && y!=null) {
            Iterator<Float> xIter = x.iterator();
            Iterator<Float> yIter = y.iterator();
            while (xIter.hasNext() && yIter.hasNext()) {
                canvas.drawCircle(xIter.next() + mTotalX, yIter.next() + mTotalY, 6 * density, paint);
            }
        }
    }


    /**
     * Add the shop tags that have been searched for on the current level.
     */
    private void drawShopTag(Canvas canvas, String name, float x, float y) {
        Rect textBounds = new Rect();

        shopTextPaint.getTextBounds(name, 0, name.length(), textBounds);

        float l = x - textBounds.width() / 2 + mTotalX - 5; // left
        float t = y - textBounds.height() / 2 + mTotalY - 5; // top
        float r = x + textBounds.width() / 2 + mTotalX + 5;// right
        float b = y + textBounds.height() / 2 + mTotalY + 5; // bottom

        RectF rectF = new RectF(l, t, r, b);
        int cornersRadius = 5;

        canvas.save();
        canvas.rotate(-45, x + mTotalX, y + mTotalY);

        canvas.drawRoundRect(
                rectF, // rect
                cornersRadius, // rx
                cornersRadius, // ry
                shopRectPaint // Paint
        );
        canvas.drawText(name, l + 5, b - 5, shopTextPaint);
        canvas.restore();
    }


    private float[] listToArray(List<Float> list)
    {
        float[] array = new float[list.size()];
        for (int i = 0; i<list.size(); i++)
        {
            array[i] = list.get(i);
        }
        return array;
    }

    private ArrayList<Float> arrayToList(float[] array)
    {
        ArrayList<Float> list = new ArrayList<>();
        for (float value : array)
        {
            list.add(value);
        }
        return list;
    }

    public Bundle getState()
    {
        Bundle state = new Bundle();
        if (currentX!=null) {
            state.putFloat("currentX", currentX);
            state.putFloat("currentY", currentY);
        }
        state.putFloatArray("summaryRecordedX", listToArray(summaryRecordedX));
        state.putFloatArray("summaryRecordedY", listToArray(summaryRecordedY));

        float screenCenterX = getMeasuredWidth() / 2  - mTotalX;
        float screenCenterY = getMeasuredHeight() / 2  - mTotalY;
        state.putFloat("screenCenterX", screenCenterX);
        state.putFloat("screenCenterY", screenCenterY);

        return state;
    }

    public void setState(Bundle state)
    {
        if (state.containsKey("currentX")) {
            currentX = state.getFloat("currentX");
            currentY = state.getFloat("currentY");
        }else {
            currentX = null;
            currentY = null;
        }
        summaryRecordedX = arrayToList(state.getFloatArray("summaryRecordedX"));
        summaryRecordedY = arrayToList(state.getFloatArray("summaryRecordedY"));

        screenCenterX = state.getFloat("screenCenterX");
        screenCenterY = state.getFloat("screenCenterY");
    }

    /**
     * Show the progress of the current scan on the map.
     */
    public void updateRecordProgress(String newText) {
        scanProgress = newText;
        invalidate();
    }

    /**
     *
     * @param summaryScores the scores for the current level.  The list must be exactly the same
     *                      length as the x and y lists.
     * @param bestGuessX location of best guess in pixels on original image.
     * @param bestGuessY location of best guess in pixels on original image.
     */
    public void updateLocateProgress(List<String> summaryScores, float currentX, float currentY,
                                     float bestGuessX, float bestGuessY, float bestGuessRadius,
                                     boolean centerViewOnCurrent) {
        this.summaryScores = summaryScores;
        this.currentX = currentX*density;
        this.currentY = currentY*density;
        this.bestGuessX = bestGuessX*density;
        this.bestGuessY = bestGuessY*density;
        this.bestGuessRadius = bestGuessRadius;

        if (centerViewOnCurrent) centerOnXY(this.currentX, this.currentY);

        invalidate();
    }

    public void setRecordScanFinished() {
        scanProgress = null;
        invalidate();
    }
}