package com.kido.swipecards.widget.swipeadapterview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * <p>Create time: 2017/7/7 18:18</p>
 *
 * @author Kido
 */

public abstract class SwipeBaseAdapter extends BaseAdapter {

    public abstract void bindView(View view, int position);

    public abstract View onCreateView(ViewGroup parent);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
