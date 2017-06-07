package com.kido.swipecards.widget.flingswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

public class FlingCardListener implements View.OnTouchListener {

    private final float objectX;
    private final float objectY;
    private final int objectH;
    private final int objectW;
    private final int parentWidth;
    private final int parentHeight;
    private final FlingListener mFlingListener;
    private final Object dataObject;
    private final float halfWidth;
    private final float halfHeight;
    private float BASE_ROTATION_DEGREES;

    private float aPosX;
    private float aPosY;
    private float aDownTouchX;
    private float aDownTouchY;
    private static final int INVALID_POINTER_ID = -1;

    // The active pointer is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private View frame = null;

    private final int TOUCH_ABOVE = 0;
    private final int TOUCH_BELOW = 1;
    private int touchPosition;
    // private final Object obj = new Object();
    private boolean isAnimationRunning = false;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));
    // 支持左右滑
    private boolean isNeedSwipe = true;

    private float aTouchUpX;

    //    private int animDuration = 500;
    private float scale;

    /**
     * every time we touch down,we should stop the {@link #animRun}
     */
    private boolean resetAnimCanceled = false;

    private static final float BORDER_PERCENT_WIDTH = 1f / 4f; // 横向边界，超过代表可移除
    private static final float BORDER_PERCENT_HEIGHT = 1f / 4f; // 纵向边界，超过代表可移除

    private static final int CARD_OUT_DURATION = 300; // 卡片飞出动画的时长ms
    private static final int CARD_IN_DURATION = 350; // 卡片飞入动画的时长ms
    private static final int CARD_BACK_DURATION = 300; // 卡片恢复原位动画的时长ms

    public static final int SWIPE_ACTION_LEFT = 1;
    public static final int SWIPE_ACTION_RIGHT = 2;
    public static final int SWIPE_ACTION_TOP = 3;
    public static final int SWIPE_ACTION_BOTTOM = 4;

    public FlingCardListener(View frame, Object itemAtPosition, float rotation_degrees, FlingListener flingListener) {
        super();
        this.frame = frame;
        this.objectX = frame.getX();
        this.objectY = frame.getY();
        this.objectW = frame.getWidth();
        this.objectH = frame.getHeight();
        this.halfWidth = objectW / 2f;
        this.halfHeight = objectH / 2f;
        this.dataObject = itemAtPosition;
        this.parentWidth = ((ViewGroup) frame.getParent()).getWidth();
        this.parentHeight = ((ViewGroup) frame.getParent()).getHeight();
        this.BASE_ROTATION_DEGREES = rotation_degrees;
        this.mFlingListener = flingListener;
    }

    public void setIsNeedSwipe(boolean isNeedSwipe) {
        this.isNeedSwipe = isNeedSwipe;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        try {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:

                    // remove the listener because 'onAnimationEnd' will still be called if we cancel the animation.
                    this.frame.animate().setListener(null);
                    this.frame.animate().cancel();

                    resetAnimCanceled = true;

                    // Save the ID of this pointer
                    mActivePointerId = event.getPointerId(0);
                    final float x = event.getX(mActivePointerId);
                    final float y = event.getY(mActivePointerId);

                    // Remember where we started
                    aDownTouchX = x;
                    aDownTouchY = y;
                    // to prevent an initial jump of the magnifier, aposX and aPosY must
                    // have the values from the magnifier frame
                    aPosX = frame.getX();
                    aPosY = frame.getY();

                    if (y < objectH / 2) {
                        touchPosition = TOUCH_ABOVE;
                    } else {
                        touchPosition = TOUCH_BELOW;
                    }
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    // Extract the index of the pointer that left the touch sensor
                    final int pointerIndex = (event.getAction() &
                            MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerId = event.getPointerId(pointerIndex);
                    if (pointerId == mActivePointerId) {
                        // This was our active pointer going up. Choose a new
                        // active pointer and adjust accordingly.
                        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        mActivePointerId = event.getPointerId(newPointerIndex);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:

                    // Find the index of the active pointer and fetch its position
                    final int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                    final float xMove = event.getX(pointerIndexMove);
                    final float yMove = event.getY(pointerIndexMove);

                    // from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                    // Calculate the distance moved
                    final float dx = xMove - aDownTouchX;
                    final float dy = yMove - aDownTouchY;

                    // Move the frame
                    aPosX += dx;
                    aPosY += dy;

                    // calculate the rotation degrees
                    float distObjectX = aPosX - objectX;
                    float rotation = BASE_ROTATION_DEGREES * 2f * distObjectX / parentWidth;
                    if (touchPosition == TOUCH_BELOW) {
                        rotation = -rotation;
                    }

                    // in this area would be code for doing something with the view as the frame moves.
                    if (isNeedSwipe) {
                        frame.setX(aPosX);
                        frame.setY(aPosY);
                        frame.setRotation(rotation);
                        mFlingListener.onScroll(getScrollProgress(), getScrollXProgressPercent());
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    //mActivePointerId = INVALID_POINTER_ID;
                    int pointerCount = event.getPointerCount();
                    int activePointerId = Math.min(mActivePointerId, pointerCount - 1);
                    aTouchUpX = event.getX(activePointerId);
                    mActivePointerId = INVALID_POINTER_ID;
                    resetCardViewOnStack(event);
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }


    private float getScrollProgress() {
        float dx = aPosX - objectX;
        float dy = aPosY - objectY;
        float dis = Math.abs(dx) + Math.abs(dy);
        return Math.min(dis, 400f) / 400f;
    }

    private float getScrollXProgressPercent() {
        if (movedBeyondLeftBorder()) {
            return -1f;
        } else if (movedBeyondRightBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (aPosX + halfWidth - leftBorder()) / (rightBorder() - leftBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }

    private float getScrollYProgressPercent() {
        if (movedBeyondTopBorder()) {
            return -1f;
        } else if (movedBeyondBottomBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (aPosY + halfHeight - topBorder()) / (bottomBorder() - topBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }


    private boolean resetCardViewOnStack(MotionEvent event) {
        if (isNeedSwipe) {
            final int duration = CARD_OUT_DURATION;
            if (movedBeyondLeftBorder()) { // Left Swipe
                onSelected(SWIPE_ACTION_LEFT, -objectW, getExitYByX(-objectW), duration);
                mFlingListener.onScroll(1f, -1.0f);
            } else if (movedBeyondRightBorder()) { // Right Swipe
                onSelected(SWIPE_ACTION_RIGHT, parentWidth, getExitYByX(parentWidth), duration);
                mFlingListener.onScroll(1f, 1.0f);
            } else if (movedBeyondTopBorder()) { // Top Swipe
                onSelected(SWIPE_ACTION_TOP, getExitXByY(-objectH), -objectH, duration);
                mFlingListener.onScroll(1f, -1.0f);
            } else if (movedBeyondBottomBorder()) { // Top Swipe
                onSelected(SWIPE_ACTION_BOTTOM, getExitXByY(parentHeight), parentHeight, duration);
                mFlingListener.onScroll(1f, -1.0f);
            } else {
                float absMoveXDistance = Math.abs(aPosX - objectX);
                float absMoveYDistance = Math.abs(aPosY - objectY);
                if (absMoveXDistance < 4 && absMoveYDistance < 4) {
                    mFlingListener.onClick(event, frame, dataObject);
                } else {
                    frame.animate()
                            .setDuration(CARD_BACK_DURATION)
                            .setInterpolator(new OvershootInterpolator(0.5f)) // overshoot 1.5
                            .x(objectX)
                            .y(objectY)
                            .rotation(0)
                            .start();
                    scale = getScrollProgress();
                    this.frame.postDelayed(animRun, 0);
                    resetAnimCanceled = false;
                }
                aPosX = 0;
                aPosY = 0;
                aDownTouchX = 0;
                aDownTouchY = 0;
            }
        } else {
            float distanceX = Math.abs(aTouchUpX - aDownTouchX);
            if (distanceX < 4)
                mFlingListener.onClick(event, frame, dataObject);
        }
        return false;
    }

    private Runnable animRun = new Runnable() {
        @Override
        public void run() {
            mFlingListener.onScroll(scale, 0);
            if (scale > 0 && !resetAnimCanceled) {
                scale = scale - 0.1f;
                if (scale < 0)
                    scale = 0;
                frame.postDelayed(this, CARD_BACK_DURATION / 20);
            }
        }
    };

    private boolean movedBeyondLeftBorder() {
        return aPosX + halfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder() {
        return aPosX + halfWidth > rightBorder();
    }

    private boolean movedBeyondTopBorder() {
        return aPosY + halfHeight < topBorder();
    }

    private boolean movedBeyondBottomBorder() {
        return aPosY + halfHeight > bottomBorder();
    }


    public float leftBorder() {
        return objectW * BORDER_PERCENT_WIDTH;
    }

    public float rightBorder() {
        return objectW * (1 - BORDER_PERCENT_WIDTH);
    }

    public float topBorder() {
        return objectH * BORDER_PERCENT_HEIGHT;
    }

    public float bottomBorder() {
        return objectH * (1 - BORDER_PERCENT_HEIGHT);
    }


    public void onSelected(final int swipeAction, float exitX, float exitY, long duration) {
        isAnimationRunning = true;
//        float exitX;
//        if (isLeft) {
//            exitX = -objectW - getRotationWidthOffset();
//        } else {
//            exitX = parentWidth + getRotationWidthOffset();
//        }

        this.frame.animate()
                .setDuration(duration)
                .setInterpolator(new LinearInterpolator())
                .translationX(exitX)
                .translationY(exitY)
                //.rotation(isLeft ? -BASE_ROTATION_DEGREES:BASE_ROTATION_DEGREES)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFlingListener.onCardExited(swipeAction, dataObject);
                        isAnimationRunning = false;
                    }
                }).start();
    }

    /**
     * Starts a default left exit animation.
     */
    public void selectLeft() {
        if (!isAnimationRunning)
            selectLeft(CARD_OUT_DURATION);
    }

    /**
     * Starts a default left exit animation.
     */
    public void selectLeft(long duration) {
        if (!isAnimationRunning)
            onSelected(SWIPE_ACTION_LEFT, -objectW, objectY, duration);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRight() {
        if (!isAnimationRunning)
            selectRight(CARD_OUT_DURATION);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRight(long duration) {
        if (!isAnimationRunning)
            onSelected(SWIPE_ACTION_RIGHT, parentWidth, objectY, duration);
    }

    public void flyIn() {
        isAnimationRunning = true;
        this.frame.setX(objectW / 3f);
        this.frame.setY(-getRotationValue(objectH));
        this.frame.setRotation(30f);

        this.frame.animate()
                .setDuration(CARD_IN_DURATION)
                .setInterpolator(new OvershootInterpolator(0.5f))
                .x(objectX)
                .y(objectY)
                .rotation(0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimationRunning = false;
                    }
                }).start();

    }

    private float getExitYByX(float exitXPoint) {
        return getLinearPoint(true, exitXPoint);
    }

    private float getExitXByY(float exitYPoint) {
        return getLinearPoint(false, exitYPoint);
    }


    /**
     * 获取两点之间的线性值
     *
     * @param isGetYbyX true则输入为x，返回y；否则输入为y，返回x
     * @param xOrY      依赖isGetYbyX，若true则为x；否则为y
     * @return
     */
    private float getLinearPoint(boolean isGetYbyX, float xOrY) {
        float[] x = new float[2];
        x[0] = objectX;
        x[1] = aPosX;

        float[] y = new float[2];
        y[0] = objectY;
        y[1] = aPosY;

        LinearRegression regression = new LinearRegression(x, y);

        //Your typical
        // y = ax+b linear regression;
        // x = (y-b)/a
        float value = isGetYbyX ? (float) regression.slope() * xOrY + (float) regression.intercept()
                : (xOrY - (float) regression.intercept()) / (float) regression.slope();

        return value;
    }

    private float getExitRotation(boolean isLeft) {
        float rotation = BASE_ROTATION_DEGREES * 2f * (parentWidth - objectX) / parentWidth;
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }

    /**
     * When the object rotates it's width becomes bigger.
     * The maximum width is at 45 degrees.
     * <p>
     * The below method calculates the width offset of the rotation.
     */
    private float getRotationWidthOffset() {
        return objectW / MAX_COS - objectW;
    }

    private float getRotationValue(float value) {
        return value / MAX_COS;
    }


    public void setRotationDegrees(float degrees) {
        this.BASE_ROTATION_DEGREES = degrees;
    }


    protected interface FlingListener {
        void onCardExited(int swipeAction, Object dataObject);

        void onClick(MotionEvent event, View v, Object dataObject);

        void onScroll(float progress, float scrollXProgress);
    }


}

