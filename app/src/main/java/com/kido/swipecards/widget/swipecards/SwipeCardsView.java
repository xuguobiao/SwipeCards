package com.kido.swipecards.widget.swipecards;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.FrameLayout;

import com.kido.swipecards.R;

import java.util.ArrayList;

/**
 * @author Kido
 */
public class SwipeCardsView extends BaseFlingAdapterView {

    private ArrayList<View> cacheItems = new ArrayList<>();

    //缩放层叠效果
    private int yOffsetStep = 28; // view叠加垂直偏移量的步长
    private float scaleOffsetStep = 0.06f; // view叠加缩放的步长
    //缩放层叠效果

    private int maxVisibleCount = 5;
    private int minAdapterStack = 6;
    private float rotationDegree = 0f;
    private int lastObjectIndexInStack = 0;

    private Adapter mAdapter;
    private onSwipeListener mSwipeListener;
    private AdapterDataSetObserver mDataSetObserver;
    private boolean mInLayout = false;
    private View mActiveCard = null;
    private OnItemClickListener mOnItemClickListener;
    private FlingCardListener flingCardListener;

    // 支持滑动移除
    public boolean isNeedSwipe = true;

    private int initTop;
    private int initLeft;

//    private boolean mRequestGotoPreCard = false;

    public SwipeCardsView(Context context) {
        this(context, null);
    }

    public SwipeCardsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeCardsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeCardsView, defStyle, 0);
        maxVisibleCount = a.getInt(R.styleable.SwipeCardsView_max_visible, maxVisibleCount);
        minAdapterStack = a.getInt(R.styleable.SwipeCardsView_min_adapter_stack, minAdapterStack);
//        rotationDegree = a.getFloat(R.styleable.SwipeCardsView_rotation_degrees, rotationDegree);
        yOffsetStep = a.getDimensionPixelOffset(R.styleable.SwipeCardsView_y_offset_step, yOffsetStep);
        scaleOffsetStep = a.getFloat(R.styleable.SwipeCardsView_scale_offset_step, scaleOffsetStep);
        a.recycle();

    }

    public void setIsNeedSwipe(boolean isNeedSwipe) {
        this.isNeedSwipe = isNeedSwipe;
    }


    @Override
    public View getSelectedView() {
        return mActiveCard;
    }


    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // if we don't have an adapter, we don't need to do anything
        if (mAdapter == null) {
            return;
        }

        mInLayout = true;
        final int adapterCount = mAdapter.getCount();
        if (adapterCount == 0) {
//            removeAllViewsInLayout();
            removeAndAddToCache(0);
        } else {
            View topCard = getChildAt(lastObjectIndexInStack);
            if (mActiveCard != null && topCard != null && topCard == mActiveCard) {
//                removeViewsInLayout(0, lastObjectIndexInStack);
                removeAndAddToCache(1);
                layoutChildren(1, adapterCount);
            } else {
                // Reset the UI and set top view listener
//                removeAllViewsInLayout();
                removeAndAddToCache(0);
                layoutChildren(0, adapterCount);
                setTopView();
            }
        }
        mInLayout = false;

        if (initTop == 0 && initLeft == 0 && mActiveCard != null) {
            initTop = mActiveCard.getTop();
            initLeft = mActiveCard.getLeft();
        }

        if (adapterCount < minAdapterStack) {
            if (mSwipeListener != null) {
                mSwipeListener.onAdapterAboutToEmpty(adapterCount);
            }
        }
    }

    private void removeAndAddToCache(int remain) {
        View view;
        for (int i = 0; i < getChildCount() - remain; ) {
            view = getChildAt(i);
            view.setOnTouchListener(null);
            removeViewInLayout(view);
            cacheItems.add(view);
        }
    }

    private void layoutChildren(int startingIndex, int adapterCount) {
        while (startingIndex < Math.min(adapterCount, maxVisibleCount)) {
            View item = null;
            if (cacheItems.size() > 0) {
                item = cacheItems.get(0);
                cacheItems.remove(item);
            }
            View newUnderChild = mAdapter.getView(startingIndex, item, this);
            if (newUnderChild.getVisibility() != GONE) {
                makeAndAddView(newUnderChild, startingIndex);
                lastObjectIndexInStack = startingIndex;
            }
            startingIndex++;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void makeAndAddView(View child, int index) {

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
        addViewInLayout(child, 0, lp, true); // 顺序插入底部（0 index）

        final boolean needToMeasure = child.isLayoutRequested();
        if (needToMeasure) {
            int childWidthSpec = getChildMeasureSpec(getWidthMeasureSpec(),
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
            int childHeightSpec = getChildMeasureSpec(getHeightMeasureSpec(),
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
        if (index > -1 && index < maxVisibleCount) {
            int multiple = Math.min(index, maxVisibleCount - 2); // 最大个是MAX_VISIBLE，实际上重叠的时候只显示 maxVisibleCount-1 个，最顶端的view的multiple从0开始，则最大为MAX_VISIBLE-2
            child.offsetTopAndBottom(yOffsetStep * multiple);
            child.setScaleX(1 - scaleOffsetStep * multiple);
            child.setScaleY(1 - scaleOffsetStep * multiple);
        }
    }

    private void adjustChildrenOfUnderTopView(float scrollRate) {
        int count = getChildCount();
        if (count > 1) {
            // count >= maxVisibleCount, 顶部和底部的view不动，中间的动。因为底部已经是最小（重合的时候底部上一张也是最小，它会用来变动），顶部已经是最大。index 的范围是 [1, count-2], multiple 为最大的 maxVisibleCount-2
            //
            int i;
            int multiple;
            if (count >= maxVisibleCount) {
                i = 1;
                multiple = maxVisibleCount - 2;
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
                int offset = (int) (yOffsetStep * (multiple - rate));
                underTopView.offsetTopAndBottom(offset - underTopView.getTop() + initTop);
                underTopView.setScaleX(1 - scaleOffsetStep * multiple + scaleOffsetStep * rate);
                underTopView.setScaleY(1 - scaleOffsetStep * multiple + scaleOffsetStep * rate);
            }
        }
    }

    /**
     * Set the top view and add the fling listener
     */
    private void setTopView() {
        if (getChildCount() > 0) {

            mActiveCard = getChildAt(lastObjectIndexInStack);
            if (mActiveCard != null && mSwipeListener != null) {

                flingCardListener = new FlingCardListener(mActiveCard, mAdapter.getItem(0),
                        rotationDegree, new FlingCardListener.FlingListener() {

                    @Override
                    public void onCardExited(int swipeAction, Object data) {
                        removeViewInLayout(mActiveCard);
                        mActiveCard = null;
                        mSwipeListener.onCardExited(swipeAction, data);
                    }

                    @Override
                    public void onPreCardTryEnter(boolean success, Object data) {
                    }

                    @Override
                    public void onClick(MotionEvent event, View v, Object dataObject) {
                        if (mOnItemClickListener != null)
                            mOnItemClickListener.onItemClicked(event, v, dataObject);
                    }

                    @Override
                    public void onScroll(float progress) {
                        adjustChildrenOfUnderTopView(progress);
//                        mSwipeListener.onScroll(progress, scrollXProgress);
                    }
                });
                // 设置是否支持左右滑
                flingCardListener.setIsNeedSwipe(isNeedSwipe);
                mActiveCard.setOnTouchListener(flingCardListener);
            }
        }
    }

    public FlingCardListener getTopCardListener() {
        return flingCardListener;
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
        this.yOffsetStep = yOffsetStep;
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
        this.scaleOffsetStep = scaleOffsetStep;
    }

    /**
     * y_offset_step 定义的是 最大可见的卡片数。
     * <p>
     * 举个例子，假设定义为5，则正常状态重合状态下能看到的卡片数是4，当触摸移动过程中可以看到 底下4个 + 触摸中的1个 = 5个。
     *
     * @param count
     */
    public void setMaxVisible(int count) {
        this.maxVisibleCount = count;
    }

    /**
     * min_adapter_stack 定义的是触发数据快空了的回调。
     * <p>
     * 举个例子，若设置为6，则当adapter数据减少到6的时候，会触发 onAdapterAboutToEmpty 回调
     *
     * @param count
     */
    public void setMinStackInAdapter(int count) {
        this.minAdapterStack = count;
    }

    public void gotoNextCard() {
        gotoNextCard(Gravity.BOTTOM);
    }

    /**
     * 进入下一张
     *
     * @param gravity 对应Gravity.LEFT, Gravity.Right, Gravity.Top, Gravity.Bottom，决定滑出的方向
     */
    public void gotoNextCard(int gravity) {
        if (getTopCardListener() != null) {
            getTopCardListener().select(gravity);
        }
    }

    /**
     * 回到上一张
     */
    public void gotoPreCard() {
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }


    @Override
    public void setAdapter(Adapter adapter) {
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

    public void setSwipeListener(onSwipeListener onSwipeListener) {
        this.mSwipeListener = onSwipeListener;
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
        void onItemClicked(MotionEvent event, View v, Object dataObject);
    }

    public interface onSwipeListener<T> {

        void onCardExited(int swipeAction, T data);

        void onAdapterAboutToEmpty(int itemsInAdapter);

        void onPreCardRequestEnter();

        void onPreCardEntered(T data);

//        void onScroll(float progress, float scrollXProgress);
    }


}
