package com.kido.swipecards.widget.swipeadapterview;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.kido.swipecards.widget.swipecards.LinearRegression;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡片滑动效果
 *
 * @author Kido
 */
public class SwipeAdapterView extends ViewGroup {

    private ArrayList<View> viewList = new ArrayList<>(); // 存放的是每一层的view，从顶到底
    private List<View> releasedViewList = new ArrayList<>(); // 手指松开后存放的view列表

    //缩放层叠效果
    private int mYOffsetStep = 70; // view叠加垂直偏移量的步长
    private float mScaleStep = 0.08f; // view叠加缩放的步长

    private int mMaxVisibleCount = 4; // 最大可视个数

    private SwipeBaseAdapter mAdapter;
    private onSwipeListener mOnSwipeListener;
    private AdapterDataSetObserver mDataSetObserver;
//    private OnItemClickListener mOnItemClickListener;

    // 支持左右滑
    public boolean mIsNeedSwipe = true;

    private int mInitObjectY;
    private int mInitObjectX;

    private ViewDragHelper mViewDragHelper;
    private GestureDetector mDetector;
    private int widthMeasureSpec;
    private int heightMeasureSpec;

    private int mCurrentItem = 0;
    private boolean mRequestingPreCard = false;
    private boolean mIsAnimating = false;
    private int mTouchSlop = 5; // 判定为滑动的阈值，单位是像素

    private static final float MAX_ROTATION = 30;
    private static final float MAX_COS = (float) Math.cos(Math.toRadians(MAX_ROTATION));

    /**
     * A view is not currently being dragged or animating as a result of a fling/snap.
     */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    /**
     * A view is currently being dragged. The position is currently changing as a result
     * of user input or simulated user input.
     */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;

    /**
     * A view is currently settling into place as a result of a fling or
     * predefined non-interactive motion.
     */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;


    public SwipeAdapterView(Context context) {
        this(context, null);

    }

    public SwipeAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mDetector = new GestureDetector(context, new ScrollDetector());
        mDetector.setIsLongpressEnabled(false);
        mViewDragHelper = ViewDragHelper.create(this, 3.5f, new SwipeDragCallback());
        mViewDragHelper.mScroller = new Scroller(context, new DiffInterpolator()) {
            @Override
            public void startScroll(int startX, int startY, int dx, int dy, int duration) {
                duration = Math.min(duration, 300);
                super.startScroll(startX, startY, dx, dy, duration);
            }
        };
    }


    private void doBindAdapter() {
        if (mAdapter == null) {
            return;
        }
        // 1. addView添加到ViewGroup中
        for (int i = 0; i < mMaxVisibleCount; i++) {
            View itemView = mAdapter.onCreateView(this);
            addView(itemView);
            itemView.setVisibility(GONE);
            if (i == 0) {
                itemView.setAlpha(0);// 最底下的view让它透明，以免阴影重叠
            }
        }

        // 2. viewList初始化
        viewList.clear();
        for (int i = 0; i < mMaxVisibleCount; i++) {
            viewList.add(getChildAt(mMaxVisibleCount - 1 - i));
        }

        mCurrentItem = 0;
        updateAdapterData();
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
        if (mCurrentItem >= getAdapter().getCount()
                || getCurrent() == null
                || !isCardStatic()) {
            return false;
        }
        View releasedChild = getCurrent();
        mIsSwipeRun = true;
        releasedViewList.add(releasedChild);
        if (mViewDragHelper.smoothSlideViewTo(releasedChild, getWidth(), getHeight())) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return true;
    }

    /**
     * @return true if can goto pre-card; false if no pre-card
     */
    public boolean gotoPreCard() {
        if (mCurrentItem <= 0
                || !isCardStatic()) {
            return false;
        }
        mRequestingPreCard = true;
        View tempView = viewList.get(viewList.size() - 1); // 最底下的view
        tempView.bringToFront();
        tempView.setVisibility(View.VISIBLE);
        tempView.setAlpha(1);
        viewList.remove(tempView);
        viewList.add(0, tempView);

        mCurrentItem--;
        mAdapter.bindView(tempView, mCurrentItem);

        fadeFlyIn();
        return true;
    }

//    private void fadeFlyIn() {
//        View activeCard = getCurrent();
//        if (mIsAnimating || activeCard == null) {
//            mRequestingPreCard = false;
//            triggerCardSelected();
//            return;
//        }
//        mIsAnimating = true;
//        activeCard.setX(activeCard.getWidth() / 3f);
//        activeCard.setY(-getRotationValue(activeCard.getHeight()));
//        activeCard.setRotation(30f);
//        adjustChildrenUnderTopView(1f);
//        activeCard.animate()
//                .setDuration(300)
//                .setInterpolator(new OvershootInterpolator(0.5f))
//                .x(mInitObjectX)
//                .y(mInitObjectY)
//                .rotation(0)
//                .setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        mIsAnimating = false;
//                        mRequestingPreCard = false;
//                        adjustChildrenUnderTopView(0);
//                        triggerCardSelected();
//
//                    }
//                }).start();
//    }

    private void fadeFlyIn() {
        View activeCard = getCurrent();
        if (mIsAnimating || activeCard == null) {
            mRequestingPreCard = false;
            triggerCardSelected();
            return;
        }
        mIsAnimating = true;
        activeCard.offsetLeftAndRight((int) (activeCard.getWidth() / 3f));
        activeCard.offsetTopAndBottom(-(int) getRotationValue(activeCard.getHeight()));
        activeCard.setRotation(MAX_ROTATION);
        if (mViewDragHelper.smoothSlideViewTo(activeCard, mInitObjectX, mInitObjectY)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void triggerCardSelected() {
        post(new Runnable() {
            @Override
            public void run() {
                if (mOnSwipeListener != null) {
                    mOnSwipeListener.onCardSelected(mCurrentItem);
                }
            }
        });
    }

    /**
     * 当前card是否处于静止正常态
     *
     * @return
     */
    private boolean isCardStatic() {
        return !mIsAnimating && !mRequestingPreCard
                && (mViewDragHelper == null || mViewDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE);
    }

    private float getRotationValue(float value) {
        return value / MAX_COS;
    }


    public void setCurrentItem(int item) {
        if (mAdapter == null) {
            return;
        }
        item = constrain(item, 0, mAdapter.getCount() - 1);
        mCurrentItem = item;
    }

    public int getCurrentItem() {
        return mCurrentItem;
    }

    public View getCurrent() {
        return viewList.size() == 0
                || mAdapter == null || mAdapter.getCount() == 0
                || mCurrentItem >= mAdapter.getCount()
                || viewList.get(0).getVisibility() != VISIBLE
                ? null : viewList.get(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        this.widthMeasureSpec = getMeasuredWidth();
        this.heightMeasureSpec = getMeasuredHeight();
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(false)) {
            mIsAnimating = true;
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (mIsSwipeRun) {
                orderViewStack();
                mIsSwipeRun = false;
            } else if (mRequestingPreCard) {
                mRequestingPreCard = false;
                triggerCardSelected();
            }
            mIsAnimating = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean b = mViewDragHelper.shouldInterceptTouchEvent(ev);
//        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
//            try {
//                mViewDragHelper.processTouchEvent(ev);
//            } catch (Exception e) {
//            }
//        }
        boolean shouldIntercept = b && mDetector.onTouchEvent(ev) && mIsNeedSwipe;
        return shouldIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mViewDragHelper.processTouchEvent(event);
        } catch (Exception e) {
        }
        return mIsNeedSwipe;
    }

    static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View viewItem = viewList.get(i);
            layoutView(viewItem, i);
            adjustChildView(viewItem, i);
        }

        if (mInitObjectY == 0 && mInitObjectX == 0 && viewList.size() > 0) {
            mInitObjectY = viewList.get(0).getTop();
            mInitObjectX = viewList.get(0).getLeft();
        }
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height, gravity);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new SwipeAdapterView.LayoutParams(getContext(), attrs);
    }

    private void layoutView(View child, int index) {

        FrameLayout.LayoutParams lp = child.getLayoutParams() != null ? (LayoutParams) child.getLayoutParams()
                : new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

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
    }

    private void adjustChildView(View child, int index) {
        if (index >= 0 && index < mMaxVisibleCount) {
            int multiple = Math.min(index, mMaxVisibleCount - 2); // 最大个是MAX_VISIBLE，实际上重叠的时候只显示 maxVisibleCount-1 个，最顶端的view的multiple从0开始，则最大为MAX_VISIBLE-2
            child.offsetTopAndBottom(mYOffsetStep * multiple);
            child.setScaleX(1 - mScaleStep * multiple);
            child.setScaleY(1 - mScaleStep * multiple);
            if (index == mMaxVisibleCount - 1) {
                child.setAlpha(0);
            }
        }
    }

    /**
     * 顶部和底部的view不动，中间的动。因为底部已经是最小（重合的时候底部上一张也是最小，它会用来变动），顶部已经是最大。
     * <p></p>
     * index 的范围是 [1, count-2], multiple 为最大的 maxVisibleCount-2
     *
     * @param scrollRate
     */
    private void adjustChildrenUnderTopView(float scrollRate) {
        if (viewList.size() == 0) {
            return;
        }
        float rate = Math.abs(scrollRate);

        for (int index = 0, size = viewList.size(); index < size; index++) {
            View childView = viewList.get(index);
            if (index == 0) {
            } else if (index == size - 1) {
                float bottomAlpha = constrain(rate > 0.1f ? rate + 0.5f : rate, 0f, 1f);
                childView.setAlpha(bottomAlpha);
            } else {
                int multiple = Math.min(index, size - 2);

                int offset = (int) (mYOffsetStep * (multiple - rate));
                childView.offsetTopAndBottom(offset - childView.getTop() + mInitObjectY);
                childView.setScaleX(1 - mScaleStep * multiple + mScaleStep * rate);
                childView.setScaleY(1 - mScaleStep * multiple + mScaleStep * rate);
            }

        }

    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(SwipeBaseAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }

        mAdapter = adapter;
        doBindAdapter();

        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    public void setOnSwipeListener(onSwipeListener listener) {
        this.mOnSwipeListener = listener;
    }

//    public void setOnItemClickListener(OnItemClickListener listener) {
//        this.mOnItemClickListener = listener;
//    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            updateAdapterData();
        }

        @Override
        public void onInvalidated() {
        }
    }

    private void updateAdapterData() {
        int count = mAdapter.getCount();
        boolean bInsertDataWhenEmpty = false;
        int delay = 0;
        for (int i = 0; i < mMaxVisibleCount; i++) {
            View itemView = viewList.get(i);
            if (itemView.getVisibility() == VISIBLE) {
                continue; // 可见的view已然存在，不改动
            }
            int index = mCurrentItem + i;
            if (index >= count) {
                break;
            }
            if (i == 0) {
                bInsertDataWhenEmpty = true;
            }
            if (i == mMaxVisibleCount - 1) {
                itemView.setAlpha(0f); // 最底下的view让它透明，以免阴影重叠
                itemView.setVisibility(View.VISIBLE);
            } else {
                setVisibilityWithAnimation(itemView, VISIBLE, delay++);
            }
            mAdapter.bindView(itemView, index);
        }

        if (bInsertDataWhenEmpty) { //当前空的情况下新增了数据。（正常情况是刚进来为空，然后新增了数据会select 0；另外一种情况是滑到最后空了（假设此时select了8），则新增数据后也会再次select 8）
            if (mOnSwipeListener != null) {
                mOnSwipeListener.onCardSelected(mCurrentItem);
            }
        }
    }

    private static void setVisibilityWithAnimation(View view, int visibility, int delayIndex) {
        if (view != null && visibility == View.VISIBLE && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(visibility);
            view.setAlpha(0);
            view.animate().alpha(1f).setStartDelay(delayIndex * 200).setDuration(360).start();
        }
    }

    /**
     * 对View重新排序
     */
    private void orderViewStack() {
        if (releasedViewList.size() == 0) {
            return;
        }

        View changedView = releasedViewList.get(0);
        if (changedView.getLeft() == mInitObjectX) {
            releasedViewList.remove(0);
            return;
        }


        // 2. 卡片View在ViewGroup中的顺次调整
        int num = viewList.size();
        for (int i = num - 1; i > 0; i--) {
            View tempView = viewList.get(i);
            tempView.bringToFront();
            tempView.setAlpha(1);
        }

        // 3. changedView填充新数据
        int newIndex = mCurrentItem + mMaxVisibleCount;
        if (newIndex < mAdapter.getCount()) {
            mAdapter.bindView(changedView, newIndex);
        } else {
            changedView.setVisibility(View.GONE);
        }

        // 4. viewList中的卡片view的位次调整
        viewList.remove(changedView);
        viewList.add(changedView);
        releasedViewList.remove(0);

        // 5. 更新showIndex、接口回调
        if (mCurrentItem + 1 <= mAdapter.getCount()) {
            mCurrentItem++;
        }
        if (null != mOnSwipeListener) {
            mOnSwipeListener.onCardSelected(mCurrentItem);
        }
    }

    public interface OnItemClickListener {
        void onItemClicked(View v, int pos);
    }

    public interface onSwipeListener {
        void onCardDragged(int position, float progress);

        void onCardSelected(int position);

        void onCardDragStateChanged(int state);

    }

    private class ScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return Math.abs(dy) + Math.abs(dx) > mTouchSlop;
        }
    }

    private class DiffInterpolator implements Interpolator {
        private Interpolator mIn = new DecelerateInterpolator();
        private Interpolator mOut = new LinearInterpolator();

        @Override
        public float getInterpolation(float input) {
            return mRequestingPreCard ? mIn.getInterpolation(input) : mOut.getInterpolation(input);
        }
    }

    /******************* ViewDragHelper.Callback ********************************/

    boolean mIsSwipeRun;

    private class SwipeDragCallback extends ViewDragHelper.Callback {

        private static final float BORDER_PERCENT_WIDTH = 1f / 3.5f; // 横向边界，超过代表可移除
        private static final float BORDER_PERCENT_HEIGHT = 1f / 3.5f; // 纵向边界，超过代表可移除

        @Override
        public int getViewHorizontalDragRange(View child) {
            return getMeasuredWidth() - child.getMeasuredWidth();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return getMeasuredHeight() - child.getMeasuredHeight();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mOnSwipeListener != null) {
                mOnSwipeListener.onCardDragStateChanged(state);
            }
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {

            boolean shouldCapture = child == getCurrent() && child.getVisibility() == View.VISIBLE
                    && !mRequestingPreCard && !mIsSwipeRun;
            if (shouldCapture) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
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

            int offsetX = left - mInitObjectX;
            int offsetY = top - mInitObjectY;
            float totalOffset = 2f * Math.min(changedView.getWidth() * BORDER_PERCENT_WIDTH, changedView.getHeight() * BORDER_PERCENT_HEIGHT);
            float progress = 1f * (Math.abs(offsetX) + Math.abs(offsetY)) / totalOffset;
            progress = constrain(progress, 0f, 1f);
            adjustChildrenUnderTopView(progress);
            if (getCurrent() != null && mRequestingPreCard) {
                getCurrent().setRotation(MAX_ROTATION * progress);
            }
            if (mOnSwipeListener != null) {
                mOnSwipeListener.onCardDragged(mCurrentItem, progress);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int disX = releasedChild.getLeft() - mInitObjectX;
            int disY = releasedChild.getTop() - mInitObjectY;
            float maxDeltaX = releasedChild.getWidth() * BORDER_PERCENT_WIDTH;
            float maxDeltaY = releasedChild.getHeight() * BORDER_PERCENT_HEIGHT;
            int finalX, finalY;
            if (disX < -maxDeltaX) { // left
                mIsSwipeRun = true;
                finalX = -releasedChild.getWidth();
                finalY = getExitYByX(finalX, releasedChild);
            } else if (disX > maxDeltaX) { // right
                mIsSwipeRun = true;
                finalX = getWidth();
                finalY = getExitYByX(finalX, releasedChild);
            } else if (disY < -maxDeltaY) { // top
                mIsSwipeRun = true;
                finalY = -releasedChild.getHeight();
                finalX = getExitXByY(finalY, releasedChild);
            } else if (disY > maxDeltaY) { // bottom
                mIsSwipeRun = true;
                finalY = getHeight();
                finalX = getExitXByY(finalY, releasedChild);
            } else { // roll back
                finalX = mInitObjectX;
                finalY = mInitObjectY;
            }

            if (mIsSwipeRun) {
                releasedViewList.add(releasedChild);
            }
            if (mViewDragHelper.smoothSlideViewTo(releasedChild, finalX, finalY)) {
                ViewCompat.postInvalidateOnAnimation(SwipeAdapterView.this);
            }
        }


        private int getExitYByX(float exitXPoint, View child) {
            return getLinearPoint(true, exitXPoint, child);
        }

        private int getExitXByY(float exitYPoint, View child) {
            return getLinearPoint(false, exitYPoint, child);
        }


        /**
         * 获取两点之间的线性值
         *
         * @param isGetYbyX true则输入为x，返回y；否则输入为y，返回x
         * @param xOrY      依赖isGetYbyX，若true则为x；否则为y
         * @return
         */
        private int getLinearPoint(boolean isGetYbyX, float xOrY, View child) {
            float[] x = new float[2];
            x[0] = mInitObjectX;
            x[1] = child.getLeft();

            float[] y = new float[2];
            y[0] = mInitObjectY;
            y[1] = child.getTop();

            LinearRegression regression = new LinearRegression(x, y);

            //Your typical
            // y = ax+b linear regression;
            // x = (y-b)/a
            float value = isGetYbyX ? (float) regression.slope() * xOrY + (float) regression.intercept()
                    : (xOrY - (float) regression.intercept()) / (float) regression.slope();

            return (int) value;
        }

    }

}
