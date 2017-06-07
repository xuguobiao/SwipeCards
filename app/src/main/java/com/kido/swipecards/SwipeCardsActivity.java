package com.kido.swipecards;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.kido.swipecards.adapter.VideoAdapter;
import com.kido.swipecards.bean.VideoData;
import com.kido.swipecards.widget.flingswipe.SwipeCardsView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kido
 */

public class SwipeCardsActivity extends AppCompatActivity {

    private SwipeCardsView mSwipeCardsView;
    private Button preButton;

    private VideoAdapter mVideoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipercardsview);
        bindViews();
        loadData();
    }

    private void bindViews() {
        preButton = (Button) findViewById(R.id.pre_button);
        preButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeCardsView.swipeLeft(500);
            }
        });

        mSwipeCardsView = (SwipeCardsView) findViewById(R.id.swipe_view);
        mSwipeCardsView.setIsNeedSwipe(true);
        mSwipeCardsView.setOnItemClickListener(new SwipeCardsView.OnItemClickListener() {
            @Override
            public void onItemClicked(MotionEvent event, View v, Object dataObject) {

            }
        });
        mSwipeCardsView.setFlingListener(new SwipeCardsView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                mVideoAdapter.remove(0);
            }

            @Override
            public void onLeftCardExit(Object dataObject) {

            }

            @Override
            public void onRightCardExit(Object dataObject) {

            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
//                if (itemsInAdapter == 3) {
//                    loadData();
//                }
            }

            @Override
            public void onScroll(float progress, float scrollXProgress) {

            }
        });

        mVideoAdapter = new VideoAdapter(this);
        mSwipeCardsView.setAdapter(mVideoAdapter);

    }

    private void loadData() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoAdapter.addAll(getTestData());
            }
        }, 500);
    }

    private static final int[] VIDEO_THUMBS = {R.drawable.image_1, R.drawable.image_2, R.drawable.image_3, R.drawable.image_4,
            R.drawable.image_5, R.drawable.image_6, R.drawable.image_7, R.drawable.image_8};

    private static List<VideoData> getTestData() {
        List<VideoData> dataList = new ArrayList<>();
        for (int i = 0; i < VIDEO_THUMBS.length; i++) {
            int indicatorIndex = i + 1;
            VideoData data = new VideoData();
            data.authorIcon = R.mipmap.ic_launcher;
            data.title = "This is the title " + indicatorIndex;
            data.videoThumb = VIDEO_THUMBS[i];
            data.videoTitle = "This is the video title " + indicatorIndex;
            dataList.add(data);
        }
        return dataList;
    }
}