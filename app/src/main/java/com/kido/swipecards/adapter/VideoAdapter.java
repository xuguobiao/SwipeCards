package com.kido.swipecards.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kido.swipecards.R;
import com.kido.swipecards.bean.VideoData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kido
 */

public class VideoAdapter extends BaseAdapter {

    private Context mContext;
    private List<VideoData> mList; // 原始

    private LayoutInflater mInflater;

    public VideoAdapter(Context context) {
        this(context, null);
    }

    public VideoAdapter(Context context, List<VideoData> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        mContext = context;
        mList = list;
        mInflater = LayoutInflater.from(mContext);
    }

    public void addAll(List<VideoData> datas) {
        mList.addAll(datas);
        notifyDataSetChanged();
    }

    public void add(VideoData data) {
        mList.add(data);
        notifyDataSetChanged();
    }

    public void add(int index, VideoData data) {
        mList.add(index, data);
        notifyDataSetChanged();
    }

    public void clear() {
        mList.clear();
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mList.isEmpty();
    }

    public void remove(int index) {
        if (index > -1 && index < mList.size()) {
            mList.remove(index);
            notifyDataSetChanged();
        }
    }

    /**
     * 移除item后插入到末尾
     *
     * @param index
     */
    public void moveToTail(int index) {
        if (index > -1 && index < mList.size()) {
            mList.add(mList.remove(index));
            notifyDataSetChanged();
        }
    }

    /**
     * 移除item后插入到头部
     *
     * @param index
     */
    public void moveToHead(int index) {
        if (index > -1 && index < mList.size()) {
            mList.add(0, mList.remove(index));
            notifyDataSetChanged();
        }
    }


    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public VideoData getItem(int position) {
        return mList.size() == 0 ? null : mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_video_card, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        VideoData data = mList.get(position);
        if (data.authorIcon > 0) {
            holder.authorIcon.setImageResource(data.authorIcon);
        }
        holder.title.setText(data.title);
        if (data.videoThumb > 0) {
            holder.video.setImageResource(data.videoThumb);
        }
        holder.videoTitle.setText(data.videoTitle);
        holder.indicatorText.setText(data.indicatorText);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView authorIcon;
        public TextView title;
        public ImageView collect;
        public ImageView video;
        public TextView videoTitle;
        public TextView indicatorText;

        public ViewHolder(View convertView) {
            authorIcon = (ImageView) convertView.findViewById(R.id.author_icon_imageView);
            title = (TextView) convertView.findViewById(R.id.title_textView);
            collect = (ImageView) convertView.findViewById(R.id.collect_imageView);
            video = (ImageView) convertView.findViewById(R.id.video_imageView);
            videoTitle = (TextView) convertView.findViewById(R.id.video_title_textView);
            indicatorText = (TextView) convertView.findViewById(R.id.indicator_textView);
        }
    }
}
