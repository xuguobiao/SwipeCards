package com.kido.swipecards.widget.slidepanel;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * <p>Create time: 2017/7/7 17:43</p>
 *
 * @author Kido
 */

public class CardItemView extends FrameLayout {

    private ObjectAnimator alphaAnimator;

    public CardItemView(@NonNull Context context) {
        super(context);
    }

    public CardItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CardItemView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void bindLayout(View view) {
        addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void setVisibilityWithAnimation(final int visibility, int delayIndex) {
        if (visibility == View.VISIBLE && getVisibility() != View.VISIBLE) {
            setAlpha(0);
            setVisibility(visibility);
            if (null != alphaAnimator) {
                alphaAnimator.cancel();
            }
            alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0.0f, 1.0f);
            alphaAnimator.setDuration(360);
            alphaAnimator.setStartDelay(delayIndex * 200);
            alphaAnimator.start();
        }
    }

}
