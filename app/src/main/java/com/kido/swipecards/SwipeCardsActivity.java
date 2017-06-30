package com.kido.swipecards;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.kido.swipecards.adapter.VideoAdapter;
import com.kido.swipecards.bean.VideoData;
import com.kido.swipecards.utils.Logger;
import com.kido.swipecards.widget.swipeadapterview.SwipeAdapterView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kido
 */

public class SwipeCardsActivity extends AppCompatActivity {

    private static final String TAG = "SwipeCardsActivity";

    private SwipeAdapterView mSwipeCardsView;
    private Button preButton, nextButton, loadButton;

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
        loadButton = (Button) findViewById(R.id.load_button);
        preButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeCardsView.gotoPreCard();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeCardsView.gotoNextCard();
            }
        });
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });

        mSwipeCardsView = (SwipeAdapterView) findViewById(R.id.swipe_view);
        mSwipeCardsView.setIsNeedSwipe(true);
        mSwipeCardsView.setOnSwipeListener(new SwipeAdapterView.onSwipeListener() {
            @Override
            public void onCardSelected(int index) {
                Logger.e("kido", "onCardSelected->" + index);
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