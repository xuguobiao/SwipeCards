package com.kido.swipecards;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kido.swipecards.adapter.VideoAdapter;
import com.kido.swipecards.bean.VideoData;
import com.kido.swipecards.utils.Logger;
import com.kido.swipecards.widget.swipecards.SwipeCardsView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kido
 */

public class SwipeCardsActivity extends AppCompatActivity {

    private static final String TAG = "SwipeCardsActivity";

    private SwipeCardsView mSwipeCardsView;
    private Button preButton, nextButton;

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
        nextButton = (Button) findViewById(R.id.next_button);
        preButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeCardsView.gotoPreCard();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeCardsView.gotoNextCard(Gravity.BOTTOM);
            }
        });

        mSwipeCardsView = (SwipeCardsView) findViewById(R.id.swipe_view);
        mSwipeCardsView.setIsNeedSwipe(true);
        mSwipeCardsView.setOnItemClickListener(new SwipeCardsView.OnItemClickListener() {
            @Override
            public void onItemClicked(MotionEvent event, View v, Object dataObject) {
                Toast.makeText(getApplicationContext(), "You clicked me!", Toast.LENGTH_SHORT).show();
            }
        });
        mSwipeCardsView.setSwipeListener(new SwipeCardsView.onSwipeListener<VideoData>() {

            @Override
            public void onCardExited(int gravity, VideoData data) {
                Logger.d(TAG, "onCardExited-> gravity=%s, data.indicator=%s", gravity, data.indicatorText);
                mVideoAdapter.moveIndexTo(0, mVideoAdapter.getCount() - 1); // 头部移到末尾，实现循环
            }

            @Override
            public void onPreCardRequestEnter() {
                Logger.d(TAG, "onPreCardRequestEnter->");
                mVideoAdapter.moveIndexTo(mVideoAdapter.getCount() - 1, 0);// 末尾移到头部，实现回到上一张
            }

            @Override
            public void onPreCardEntered(VideoData data) {
                Logger.d(TAG, "onPreCardEntered-> data.indicator=%s", data.indicatorText);
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                Logger.d(TAG, "onAdapterAboutToEmpty-> itemsInAdapter=%s", itemsInAdapter);
            }

        });

        mVideoAdapter = new VideoAdapter(this);
        mSwipeCardsView.setAdapter(mVideoAdapter);

    }

    private void loadData() {
        mVideoAdapter.addAll(getTestData());
    }

    private static final int[] VIDEO_THUMBS = {R.drawable.image_1, R.drawable.image_2, R.drawable.image_3, R.drawable.image_4,
            R.drawable.image_5, R.drawable.image_6, R.drawable.image_7, R.drawable.image_8};

    private static List<VideoData> getTestData() {
        List<VideoData> dataList = new ArrayList<>();
        for (int i = 0, z = VIDEO_THUMBS.length; i < z; i++) {
            int indicatorIndex = i + 1;
            VideoData data = new VideoData();
            data.authorIcon = R.mipmap.ic_launcher;
            data.title = "This is the title " + indicatorIndex;
            data.videoThumb = VIDEO_THUMBS[i];
            data.videoTitle = "This is the video title " + indicatorIndex;
            data.indicatorText = (i + 1) + "/" + z;
            dataList.add(data);
        }
        return dataList;
    }
}