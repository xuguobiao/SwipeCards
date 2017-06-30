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
 * @author payge
 *         通过ViewDragHelper实现拖拽滑动
 */
public class SwipeAdapterView extends AdapterView<BaseAdapter> {

    private ArrayList<View> cache = new ArrayList<>();

    //缩放层叠效果
    private int mYOffsetStep = 70; // view叠加垂直偏移量的步长
    private float mScaleStep = 0.08f; // view叠加缩放的步长

    private int mMaxVisibleCount = 4; // 值建议最小为4
    private int LAST_VIEW_IN_STACK = 0;

    private BaseAdapter mAdapter;
    private onSwipeListener mFlingListener;
    private AdapterDataSetObserver mDataSetObserver;
    private boolean mInLayout = false;
    private View mActiveCard = null;
    private OnItemClickListener mOnItemClickListener;

    // 支持左右滑
    public boolean isNeedSwipe = true;

    private int mInitObjectY;
    private int mInitObjectX;

    private ViewDragHelper mViewDragHelper;
    private GestureDetector mDetector;
    private int widthMeasureSpec;
    private int heightMeasureSpec;

    private int mCurrentIndex = 0;
    private boolean mRequestingPreCard = false;
    private boolean mIsAnimating = false;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));

    public SwipeAdapterView(Context context) {
        this(context, null);
    }

    public SwipeAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mViewDragHelper = ViewDragHelper.create(this, 4f, mDragCallback);
        mViewDragHelper.mScroller = new Scroller(context, new LinearInterpolator());
        mDetector = new GestureDetector(context, new ScrollDetector());
    }


    public void setIsNeedSwipe(boolean isNeedSwipe) {
        this.isNeedSwipe = isNeedSwipe;
    }

    /**
     * y_offset_step 定义的是卡片之间在y轴方向上的偏移量。
     * <p>
     * 举个例子，可见的卡片有3个，如果步长是20dp，从前往后看，卡片y轴坐标会依次增加20dp，表现上就是后面一张卡片底部有20dp会露出来；如果值是负的，如 -20dp，那么表现则相反。
     * <p>
     * 如果不需要对卡片进行y轴方向上的偏移量处理，不设置这个属性或者设置为0dp就可以了。
     *
     * @param yOffsetStep in pixels
     */
    public void setYOffsetStep(int yOffsetStep) {
        this.mYOffsetStep = yOffsetStep;
    }

    /**
     * scale_offset_step 定义的取值范围是0-1，所以scale的步长也得在这个范围之内。
     * <p>
     * 举个例子，可见的卡片有3个，如果步长是0.08，那么最前面的scale是1，后面一点的是0.92，最后面的是0.84；值得注意的是 x 和 y同时被缩放了(1 - scaleStep*index)。
     * <p>
     * 如果不需要对卡片进行缩放处理，不设置这个属性或者设置为0就可以了
     *
     * @param scaleOffsetStep
     */
    public void setScaleOffsetStep(float scaleOffsetStep) {
        this.mScaleStep = scaleOffsetStep;
    }

    /**
     * y_offset_step 定义的是 最大可见的卡片数。
     * <p>
     * 举个例子，假设定义为5，则正常状态重合状态下能看到的卡片数是4，当触摸移动过程中可以看到 底下4个 + 触摸中的1个 = 5个。
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
        isSwipeRun = true;
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
            if (isSwipeRun) {
                isSwipeRun = false;
                adjustChildrenOfUnderTopView(1f);

                mActiveCard = null;
                mCurrentIndex++;
                getAdapter().notifyDataSetChanged();
                if (mFlingListener != null) {
                    mFlingListener.onCardSelected(mCurrentIndex);
                }
            }
            mIsAnimating = false;
            objectX = 0;
            objectY = 0;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean b = mViewDragHelper.shouldInterceptTouchEvent(ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mViewDragHelper.processTouchEvent(ev);
        }
        return b && mDetector.onTouchEvent(ev) && isNeedSwipe;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return isNeedSwipe;
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
            View topCard = getChildAt(LAST_VIEW_IN_STACK);
            if (mActiveCard != null && topCard != null && topCard == mActiveCard && !mRequestingPreCard) {
//                removeViewsInLayout(0, LAST_VIEW_IN_STACK);
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
            cache.add(child);
        }
    }

    private void layoutChildren(int startingIndex, int adapterCount) {
        while (startingIndex < Math.min(adapterCount, mMaxVisibleCount)) {
            View cacheView = null;
            if (cache.size() > 0) {
                cacheView = cache.get(0);
                cache.remove(cacheView);
            }
            View newUnderChild = mAdapter.getView(mCurrentIndex + startingIndex, cacheView, this);
            if (newUnderChild.getVisibility() != GONE) {
                makeAndAddView(newUnderChild, startingIndex);
                LAST_VIEW_IN_STACK = startingIndex;
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
//            if (count <= maxVisibleCount - 2) {
//                i = 0; //lastObjectIndexInStack - (count - 1);
//                multiple = count - 1;
//            } else {
//                i = lastObjectIndexInStack - (maxVisibleCount - 2);
//                multiple = maxVisibleCount - 2;
//            }
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
            mActiveCard = getChildAt(LAST_VIEW_IN_STACK);
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

    public void setOnSwipeListener(onSwipeListener onSwipeListener) {
        this.mFlingListener = onSwipeListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
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

    boolean isSwipeRun;
    int objectX, objectY;
    float aPosX, aPosY;

    private static final float BORDER_PERCENT_WIDTH = 1f / 4f; // 横向边界，超过代表可移除
    private static final float BORDER_PERCENT_HEIGHT = 1f / 4f; // 纵向边界，超过代表可移除

    private ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

//        int disX, disY;

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            objectX = child.getLeft();
            objectY = child.getTop();
            Logger.e("kido", "tryCaptureView->");
            return child == mActiveCard;
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
            int offsetX = left - objectX;
            int offsetY = top - objectY;
//            float progress = 1f * (Math.abs(offsetX) + Math.abs(offsetY)) / 1f * (changedView.getWidth() * BORDER_PERCENT_WIDTH + changedView.getHeight() * BORDER_PERCENT_HEIGHT);
            float progress = 1f * (Math.abs(offsetX) + Math.abs(offsetY)) / 400;
            progress = Math.min(progress, 1f);
            adjustChildrenOfUnderTopView(progress);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            aPosX = releasedChild.getLeft();
            aPosY = releasedChild.getTop();
            int disX = releasedChild.getLeft() - objectX;
            int disY = releasedChild.getTop() - objectY;
            float maxDeltaX = releasedChild.getWidth() * BORDER_PERCENT_WIDTH;
            float maxDeltaY = releasedChild.getHeight() * BORDER_PERCENT_HEIGHT;
            int finalX, finalY;
            if (disX < -maxDeltaX) { // left
                isSwipeRun = true;
                finalX = -releasedChild.getWidth();
                finalY = getExitYByX(finalX);
            } else if (disX > maxDeltaX) { // right
                isSwipeRun = true;
                finalX = getWidth();
                finalY = getExitYByX(finalX);
            } else if (disY < -maxDeltaY) { // top
                isSwipeRun = true;
                finalY = -releasedChild.getHeight();
                finalX = getExitXByY(finalY);
            } else if (disY > maxDeltaY) { // bottom
                isSwipeRun = true;
                finalY = getHeight();
                finalX = getExitXByY(finalY);
            } else {
                finalX = mInitObjectX;
                finalY = mInitObjectY;
            }

            mViewDragHelper.smoothSlideViewTo(releasedChild, finalX, finalY);
            invalidate();

            Logger.e("kido", "smoothSlideViewTo-> finalLeft=" + finalX + ", finalTop=" + finalY);
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

        return (int) value;
    }

}
