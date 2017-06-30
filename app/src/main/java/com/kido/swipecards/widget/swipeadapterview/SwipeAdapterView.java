package com.kido.swipecards.widget.swipeadapterview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.kido.swipecards.utils.Logger;
import com.kido.swipecards.widget.swipecards.LinearRegression;

import java.util.ArrayList;

/**
 * 卡片滑动效果
 *
 * @author Kido
 */
public class SwipeAdapterView extends AdapterView<BaseAdapter> {

    private ArrayList<View> mViewCache = new ArrayList<>();

    //缩放层叠效果
    private int mYOffsetStep = 70; // view叠加垂直偏移量的步长
    private float mScaleStep = 0.08f; // view叠加缩放的步长

    private int mMaxVisibleCount = 4; // 最大可视个数
    private int mLastViewIndexInStack = 0;

    private BaseAdapter mAdapter;
    private onSwipeListener mOnSwipeListener;
    private AdapterDataSetObserver mDataSetObserver;
    private boolean mInLayout = false;
    private View mActiveCard = null;
    private OnItemClickListener mOnItemClickListener;

    // 支持左右滑
    public boolean mIsNeedSwipe = true;

    private int mInitObjectY;
    private int mInitObjectX;

    private ViewDragHelper mViewDragHelper;
    private GestureDetector mDetector;
    private int widthMeasureSpec;
    private int heightMeasureSpec;

    private int mCurrentIndex = 0;
    private boolean mRequestingPreCard = false;
    private boolean mIsAnimating = false;
    private static final float MAX_COS = (float) Math.cos(Math.toRadians(45));

    public SwipeAdapterView(Context context) {
        this(context, null);
    }

    public SwipeAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mViewDragHelper = ViewDragHelper.create(this, 4f, mDragCallback);
        mViewDragHelper.mScroller = new Scroller(context, new LinearInterpolator());
        mDetector = new GestureDetector(context, new ScrollDetector());
    }

    /**
     * 是否支持卡片手势滑动
     *
     * @param isNeedSwipe
     */
    public void setIsNeedSwipe(boolean isNeedSwipe) {
        this.mIsNeedSwipe = isNeedSwipe;
    }

    /**
     * y_offset_step 定义的是卡片之间在y轴方向上的偏移量。
     *
     * @param yOffsetStep in pixels
     */
    public void setYOffsetStep(int yOffsetStep) {
        this.mYOffsetStep = yOffsetStep;
    }

    /**
     * scale_offset_step 定义的取值范围是0-1，所以scale的步长也得在这个范围之内。
     *
     * @param scaleOffsetStep
     */
    public void setScaleOffsetStep(float scaleOffsetStep) {
        this.mScaleStep = scaleOffsetStep;
    }

    /**
     * MaxVisible 定义的是 最大可见的卡片数。
     *
     * @param count
     */
    public void setMaxVisible(int count) {
        this.mMaxVisibleCount = count;
    }

    public boolean gotoNextCard() {
        if (mCurrentIndex >= getAdapter().getCount() - 1
                || mIsAnimating || mRequestingPreCard || mViewDragHelper.getViewDragState() != ViewDragHelper.STATE_IDLE
                || mActiveCard == null) {
            return false;
        }
        mIsSwipeRun = true;
        mViewDragHelper.smoothSlideViewTo(mActiveCard, getWidth(), getHeight());
        invalidate();
        return true;
    }

    /**
     * @return true if can goto pre-card; false if no pre-card
     */
    public boolean gotoPreCard() {
        if (mCurrentIndex == 0
                || mIsAnimating || mRequestingPreCard || mViewDragHelper.getViewDragState() != ViewDragHelper.STATE_IDLE) {
            return false;
        }
        mCurrentIndex = mCurrentIndex - 1;
        mRequestingPreCard = true;
        mActiveCard = null;
        getAdapter().notifyDataSetChanged();
        return true;
    }

    private void fadeFlyIn() {
        if (mIsAnimating) {
            mRequestingPreCard = false;
            return;
        }
        mIsAnimating = true;
        mActiveCard.setX(mActiveCard.getWidth() / 3f);
        mActiveCard.setY(-getRotationValue(mActiveCard.getHeight()));
        mActiveCard.setRotation(30f);
        adjustChildrenOfUnderTopView(1f);
        mActiveCard.animate()
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(0.5f))
                .x(mInitObjectX)
                .y(mInitObjectY)
                .rotation(0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsAnimating = false;
                        mRequestingPreCard = false;
                        adjustChildrenOfUnderTopView(0);

                    }
                }).start();
    }

    private float getRotationValue(float value) {
        return value / MAX_COS;
    }

    @Override
    public View getSelectedView() {
        return mActiveCard;
    }

    @Override
    public void setSelection(int position) {
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.widthMeasureSpec = widthMeasureSpec;
        this.heightMeasureSpec = heightMeasureSpec;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(false)) {
            mIsAnimating = true;
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (mIsSwipeRun) {
                mIsSwipeRun = false;
                adjustChildrenOfUnderTopView(1f);

                mActiveCard = null;
                mCurrentIndex++;
                getAdapter().notifyDataSetChanged();
                if (mOnSwipeListener != null) {
                    mOnSwipeListener.onCardSelected(mCurrentIndex);
                }
            }
            mIsAnimating = false;
            mObjectX = 0;
            mObjectY = 0;
            mPosX = 0;
            mPosY = 0;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean b = mViewDragHelper.shouldInterceptTouchEvent(ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mViewDragHelper.processTouchEvent(ev);
        }
        return b && mDetector.onTouchEvent(ev) && mIsNeedSwipe;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return mIsNeedSwipe;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    private int getValidCount() {
        return getAdapter().getCount() - mCurrentIndex;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // if we don't have an adapter, we don't need to do anything
        if (mAdapter == null) {
            return;
        }

        mInLayout = true;
        final int validCount = getValidCount();
        if (validCount == 0) {
//            removeAllViewsInLayout();
            removeViewToCache(0);
        } else {
            View topCard = getChildAt(mLastViewIndexInStack);
            if (mActiveCard != null && topCard != null && topCard == mActiveCard && !mRequestingPreCard) {
//                removeViewsInLayout(0, mLastViewIndexInStack);
                removeViewToCache(1);
                layoutChildren(1, validCount);
            } else {
                // Reset the UI and set top view listener
//                removeAllViewsInLayout();
                removeViewToCache(0);
                layoutChildren(0, validCount);
                setTopView();
            }
        }
        mInLayout = false;

        if (mInitObjectY == 0 && mInitObjectX == 0 && mActiveCard != null) {
            mInitObjectY = mActiveCard.getTop();
            mInitObjectX = mActiveCard.getLeft();
        }

        if (mRequestingPreCard) {
            fadeFlyIn();
        }
    }

    private void removeViewToCache(int saveCount) {
        View child;
        for (int i = 0; i < getChildCount() - saveCount; ) {
            child = getChildAt(i);
            removeViewInLayout(child);
            mViewCache.add(child);
        }
    }

    private void layoutChildren(int startingIndex, int adapterCount) {
        while (startingIndex < Math.min(adapterCount, mMaxVisibleCount)) {
            View cacheView = null;
            if (mViewCache.size() > 0) {
                cacheView = mViewCache.get(0);
                mViewCache.remove(cacheView);
            }
            View newUnderChild = mAdapter.getView(mCurrentIndex + startingIndex, cacheView, this);
            if (newUnderChild.getVisibility() != GONE) {
                makeAndAddView(newUnderChild, startingIndex);
                mLastViewIndexInStack = startingIndex;
            }
            startingIndex++;
        }
    }

    private void makeAndAddView(View child, int index) {

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
        addViewInLayout(child, 0, lp, true);

        final boolean needToMeasure = child.isLayoutRequested();
        if (needToMeasure) {
            int childWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
            int childHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                    lp.height);
            child.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }

        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();

        int gravity = lp.gravity;
        if (gravity == -1) {
            gravity = Gravity.TOP | Gravity.START;
        }

        int layoutDirection = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN)
            layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        int childLeft;
        int childTop;
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2 +
                        lp.leftMargin - lp.rightMargin;
                break;
            case Gravity.END:
                childLeft = getWidth() + getPaddingRight() - w - lp.rightMargin;
                break;
            case Gravity.START:
            default:
                childLeft = getPaddingLeft() + lp.leftMargin;
                break;
        }
        switch (verticalGravity) {
            case Gravity.CENTER_VERTICAL:
                childTop = (getHeight() + getPaddingTop() - getPaddingBottom() - h) / 2 +
                        lp.topMargin - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                childTop = getHeight() - getPaddingBottom() - h - lp.bottomMargin;
                break;
            case Gravity.TOP:
            default:
                childTop = getPaddingTop() + lp.topMargin;
                break;
        }
        child.layout(childLeft, childTop, childLeft + w, childTop + h);
        // 缩放层叠效果
        adjustChildView(child, index);
    }

    private void adjustChildView(View child, int index) {
        if (index > -1 && index < mMaxVisibleCount) {
            int multiple = Math.min(index, mMaxVisibleCount - 2); // 最大个是MAX_VISIBLE，实际上重叠的时候只显示 maxVisibleCount-1 个，最顶端的view的multiple从0开始，则最大为MAX_VISIBLE-2
            child.offsetTopAndBottom(mYOffsetStep * multiple);
            child.setScaleX(1 - mScaleStep * multiple);
            child.setScaleY(1 - mScaleStep * multiple);
        }
    }

    private void adjustChildrenOfUnderTopView(float scrollRate) {
        int count = getChildCount();
        if (count > 1) {
            // count >= maxVisibleCount, 顶部和底部的view不动，中间的动。因为底部已经是最小（重合的时候底部上一张也是最小，它会用来变动），顶部已经是最大。index 的范围是 [1, count-2], multiple 为最大的 maxVisibleCount-2
            //
            int i;
            int multiple;
            if (count >= mMaxVisibleCount) {
                i = 1;
                multiple = mMaxVisibleCount - 2;
            } else {
                i = 0;
                multiple = count - 1;
            }
            float rate = Math.abs(scrollRate);
            for (; i < count - 1; i++, multiple--) {
                View underTopView = getChildAt(i);
                int offset = (int) (mYOffsetStep * (multiple - rate));
                underTopView.offsetTopAndBottom(offset - underTopView.getTop() + mInitObjectY);
                underTopView.setScaleX(1 - mScaleStep * multiple + mScaleStep * rate);
                underTopView.setScaleY(1 - mScaleStep * multiple + mScaleStep * rate);
            }
        }
    }

    /**
     * Set the top view and add the fling listener
     */
    private void setTopView() {
        if (getChildCount() > 0) {
            mActiveCard = getChildAt(mLastViewIndexInStack);
            if (mActiveCard != null) {
                mActiveCard.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null)
                            mOnItemClickListener.onItemClicked(v, mCurrentIndex);
                    }
                });
            }
        }
    }

    @Override
    public BaseAdapter getAdapter() {
        return mAdapter;
    }


    @Override
    public void setAdapter(BaseAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }

        mAdapter = adapter;

        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    public void setOnSwipeListener(onSwipeListener listener) {
        this.mOnSwipeListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }


    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FrameLayout.LayoutParams(getContext(), attrs);
    }


    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            requestLayout();
        }

    }


    public interface OnItemClickListener {
        void onItemClicked(View v, int pos);
    }

    public interface onSwipeListener {

        void onCardSelected(int index);

    }

    private class ScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return Math.abs(dy) + Math.abs(dx) > 4;
        }
    }

    /******************* ViewDragHelper.Callback ********************************/

    boolean mIsSwipeRun;
    int mObjectX, mObjectY;
    int mPosX, mPosY;

    private static final float BORDER_PERCENT_WIDTH = 1f / 4f; // 横向边界，超过代表可移除
    private static final float BORDER_PERCENT_HEIGHT = 1f / 4f; // 纵向边界，超过代表可移除

    private ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

//        int disX, disY;

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            boolean shouldCapture = child == mActiveCard && !mRequestingPreCard && !mIsAnimating;
            if (shouldCapture) {
                mObjectX = child.getLeft();
                mObjectY = child.getTop();
            }
            Logger.e("kido", "tryCaptureView-> shouldCapture=%s", shouldCapture);
            return shouldCapture;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            int offsetX = left - mObjectX;
            int offsetY = top - mObjectY;
            float totalOffset = 2f * Math.min(changedView.getWidth() * BORDER_PERCENT_WIDTH, changedView.getHeight() * BORDER_PERCENT_HEIGHT);
            float progress = 1f * (Math.abs(offsetX) + Math.abs(offsetY)) / totalOffset;
//            float progress = 1f * (Math.abs(offsetX) + Math.abs(offsetY)) / 400;
            progress = Math.min(progress, 1f);
            adjustChildrenOfUnderTopView(progress);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            mPosX = releasedChild.getLeft();
            mPosY = releasedChild.getTop();
            int disX = releasedChild.getLeft() - mObjectX;
            int disY = releasedChild.getTop() - mObjectY;
            float maxDeltaX = releasedChild.getWidth() * BORDER_PERCENT_WIDTH;
            float maxDeltaY = releasedChild.getHeight() * BORDER_PERCENT_HEIGHT;
            int finalX, finalY;
            if (disX < -maxDeltaX) { // left
                mIsSwipeRun = true;
                finalX = -releasedChild.getWidth();
                finalY = getExitYByX(finalX);
            } else if (disX > maxDeltaX) { // right
                mIsSwipeRun = true;
                finalX = getWidth();
                finalY = getExitYByX(finalX);
            } else if (disY < -maxDeltaY) { // top
                mIsSwipeRun = true;
                finalY = -releasedChild.getHeight();
                finalX = getExitXByY(finalY);
            } else if (disY > maxDeltaY) { // bottom
                mIsSwipeRun = true;
                finalY = getHeight();
                finalX = getExitXByY(finalY);
            } else { // roll back
                finalX = mInitObjectX;
                finalY = mInitObjectY;
            }

            mViewDragHelper.smoothSlideViewTo(releasedChild, finalX, finalY);
            invalidate();

            Logger.e("kido", "smoothSlideViewTo-> finalX=%s, finalY=%s", finalX, finalY);
        }
    };

    private int getExitYByX(float exitXPoint) {
        return getLinearPoint(true, exitXPoint);
    }

    private int getExitXByY(float exitYPoint) {
        return getLinearPoint(false, exitYPoint);
    }


    /**
     * 获取两点之间的线性值
     *
     * @param isGetYbyX true则输入为x，返回y；否则输入为y，返回x
     * @param xOrY      依赖isGetYbyX，若true则为x；否则为y
     * @return
     */
    private int getLinearPoint(boolean isGetYbyX, float xOrY) {
        float[] x = new float[2];
        x[0] = mObjectX;
        x[1] = mPosX;

        float[] y = new float[2];
        y[0] = mObjectY;
        y[1] = mPosY;

        LinearRegression regression = new LinearRegression(x, y);

        //Your typical
        // y = ax+b linear regression;
        // x = (y-b)/a
        float value = isGetYbyX ? (float) regression.slope() * xOrY + (float) regression.intercept()
                : (xOrY - (float) regression.intercept()) / (float) regression.slope();

        return (int) value;
    }

}
