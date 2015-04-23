package com.android.inalego.whatwherewhen;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainFragment extends Fragment implements View.OnClickListener {
    private TextView mExpertsScoreView;
    private TextView mViewersScoreView;

    private int mExpertsScore;
    private int mViewersScore;

    private TimerView mTimerView;
    private Button mStartButton;

    private MediaPlayer mPlayer;

    private static WeakReference<MainFragment> sCurrentFragment;

    private static int sTime = -1;
    private static final Handler sHandler = new Handler();
    private static final Runnable sRunnable = new Runnable() {
        @Override
        public void run() {
            MainFragment fragment = sCurrentFragment.get();
            if(fragment != null && fragment.mTimerView != null) {
                if(sTime > 0) {
                    if(sTime == 10){
                        MediaPlayer player = MediaPlayer.create(fragment.getActivity(),
                                Settings.System.DEFAULT_NOTIFICATION_URI);
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                            }
                        });
                        player.start();
                    }
                    sTime--;
                    fragment.mTimerView.updateValue(sTime);
                    sHandler.postDelayed(sRunnable, 1000);
                } else if(sTime == 0) {
                    sTime = -1;
                    fragment.mTimerView.updateValue(0);
                    fragment.mStartButton.setText(R.string.stop);
                    fragment.setPlayer();
                }
            }
        }
    };

    private void setPlayer(){
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.setDataSource(getActivity(), Settings.System.DEFAULT_NOTIFICATION_URI);
            mPlayer.prepare();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.start();
                }
            });
            mPlayer.start();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public MainFragment() {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_reset) {
            mExpertsScore = 0;
            mViewersScore = 0;
            mExpertsScoreView.setText(String.valueOf(mExpertsScore));
            mViewersScoreView.setText(String.valueOf(mViewersScore));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        sCurrentFragment = new WeakReference<>(this);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mExpertsScoreView = (TextView) rootView.findViewById(R.id.expertsScore);
        mViewersScoreView = (TextView) rootView.findViewById(R.id.viewersScore);
        mTimerView = (TimerView) rootView.findViewById(R.id.timer);
        mStartButton = (Button) rootView.findViewById(R.id.start_button);

        (rootView.findViewById(R.id.incExpertsScore)).setOnClickListener(this);
        (rootView.findViewById(R.id.incViewersScore)).setOnClickListener(this);
        (rootView.findViewById(R.id.start_button)).setOnClickListener(this);

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.incExpertsScore:
                ++mExpertsScore;
                mExpertsScoreView.setText(String.valueOf(mExpertsScore));
                break;
            case R.id.incViewersScore:
                ++mViewersScore;
                mViewersScoreView.setText(String.valueOf(mViewersScore));
                break;
            case R.id.start_button:
                if(mStartButton.getText().equals(getString(R.string.stop)) && mPlayer != null){
                    mPlayer.release();
                    mStartButton.setText(R.string.start);
                } else if(sTime == -1) {
                    sHandler.removeCallbacksAndMessages(null);
                    sTime = 60;
                    mStartButton.setText(R.string.reset);
                    sHandler.postDelayed(sRunnable, 1000);
                } else {
                    sTime = -1;
                    mTimerView.updateValue(0);
                    mStartButton.setText(R.string.start);
                }
                break;
        }
    }
}
