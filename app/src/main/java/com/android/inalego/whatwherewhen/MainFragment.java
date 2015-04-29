package com.android.inalego.whatwherewhen;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainFragment extends Fragment implements View.OnClickListener {
    private final static String EXPERTS_SCORE_KEY = "experts_score_key";
    private final static String VIEWERS_SCORE_KEY = "viewers_score_key";
    private final static String INIT_TIME_KEY = "init_time_key";
    private final static String ADD_MINUTES_KEY = "add_minutes_key";
    private final static String START_BUTTON_COLOR_KEY = "start_button_color_key";

    private static int STANDARD_TIME = 60;
    private static int BLITZ_TIME = 20;

    private TextView mExpertsScoreView;
    private TextView mViewersScoreView;

    private int mExpertsScore;
    private int mViewersScore;

    private TimerView mTimerView;
    private Button mStartButton;
    private int mInitTime;

    private int mAdditionalMinutes;
    private TextView mAdditionalMinutesView;

    private static MediaPlayer sPlayer;

    private static WeakReference<MainFragment> sCurrentFragment;

    private static int sTime = -1;
    private static final Handler sHandler = new Handler();
    private static final Runnable sRunnable = new Runnable() {
        @Override
        public void run() {
            MainFragment fragment = sCurrentFragment.get();
            if(fragment != null && fragment.mTimerView != null) {
                if(sTime > 1) {
                    sTime--;
                    fragment.mTimerView.updateValue(sTime);
                    if (sTime == 10 && fragment.mInitTime == STANDARD_TIME) {
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
                    sHandler.postDelayed(sRunnable, 1000);
                } else if(sTime == 1) {
                    sTime = -1;
                    fragment.mTimerView.updateValue(0);
                    fragment.mStartButton.setText(R.string.stop);
                    fragment.setPlayer();
                }
            }
        }
    };

    private static final Handler sColorHandler = new Handler();
    private static final Runnable sColorRunnable = new Runnable() {
        @Override
        public void run() {
            MainFragment fragment = sCurrentFragment.get();
            if (fragment != null && fragment.mStartButton != null && sTime != -1) {
                ColorDrawable color = (ColorDrawable) fragment.mStartButton.getBackground();
                int red = (color.getColor() & 0x00ff0000) >> 0x10;
                int green = (color.getColor() & 0x0000ff00) >> 8;
                red = red < 0xff ? red + 1 : 0xff;
                green = green > 0 ? green - 1 : 0;
                int newColor = 0xff000000 + (red << 0x10) + (green << 8);
                color.setColor(newColor);
                fragment.mStartButton.setBackground(color);
                sColorHandler.postDelayed(sColorRunnable,
                        fragment.mInitTime == STANDARD_TIME ? 3000 / 13 : 1000 / 13);
            }
        }
    };

    private void setPlayer(){
        sPlayer = new MediaPlayer();
        try {
            sPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            sPlayer.setDataSource(getActivity(), Settings.System.DEFAULT_NOTIFICATION_URI);
            sPlayer.prepare();
            sPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.start();
                }
            });
            sPlayer.start();
        } catch(IOException e){
            e.printStackTrace();
        }
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
        mAdditionalMinutesView = (TextView) rootView.findViewById(R.id.additional_minutes);

        (rootView.findViewById(R.id.start_button)).setOnClickListener(this);

        mExpertsScoreView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            public void onSwipeTop() {
                if (mExpertsScore + mViewersScore < 11) {
                    ++mExpertsScore;
                }
                mExpertsScoreView.setText(String.valueOf(mExpertsScore));
            }

            public void onSwipeBottom() {
                mExpertsScore = mExpertsScore > 0 ? mExpertsScore - 1 : 0;
                mExpertsScoreView.setText(String.valueOf(mExpertsScore));
            }
        });

        mViewersScoreView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            public void onSwipeTop() {
                if (mExpertsScore + mViewersScore < 11) {
                    ++mViewersScore;
                }
                mViewersScoreView.setText(String.valueOf(mViewersScore));
            }

            public void onSwipeBottom() {
                mViewersScore = mViewersScore > 0 ? mViewersScore - 1 : 0;
                mViewersScoreView.setText(String.valueOf(mViewersScore));
            }
        });

        mTimerView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            public void onSwipeLeft() {
                if (sPlayer == null && sTime == -1) {
                    if (mInitTime == STANDARD_TIME) {
                        mInitTime = BLITZ_TIME;
                    } else {
                        mInitTime = STANDARD_TIME;
                    }
                    mTimerView.updateValue(mInitTime);
                }
            }

            public void onSwipeRight() {
                if (sPlayer == null && sTime == -1) {
                    if (mInitTime == STANDARD_TIME) {
                        mInitTime = BLITZ_TIME;
                    } else {
                        mInitTime = STANDARD_TIME;
                    }
                    mTimerView.updateValue(mInitTime);
                }
            }
        });

        mAdditionalMinutesView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            public void onSwipeTop() {
                mAdditionalMinutes = mAdditionalMinutes < 5 ? mAdditionalMinutes + 1 : 5;
                mAdditionalMinutesView.setText(mAdditionalMinutes + "'");
            }

            public void onSwipeBottom() {
                mAdditionalMinutes = mAdditionalMinutes > 0 ? mAdditionalMinutes - 1 : 0;
                mAdditionalMinutesView.setText(mAdditionalMinutes + "'");
            }
        });

        if (savedInstanceState != null) {
            mExpertsScore = savedInstanceState.getInt(EXPERTS_SCORE_KEY);
            mViewersScore = savedInstanceState.getInt(VIEWERS_SCORE_KEY);
            mExpertsScoreView.setText(String.valueOf(mExpertsScore));
            mViewersScoreView.setText(String.valueOf(mViewersScore));
            mInitTime = savedInstanceState.getInt(INIT_TIME_KEY);
            mAdditionalMinutes = savedInstanceState.getInt(ADD_MINUTES_KEY);
            mStartButton.setBackgroundColor(savedInstanceState.getInt(START_BUTTON_COLOR_KEY));
        } else {
            mInitTime = STANDARD_TIME;
            mAdditionalMinutes = 0;
        }
        mAdditionalMinutesView.setText(mAdditionalMinutes + "'");

        if (sPlayer != null) {
            mStartButton.setText(R.string.stop);
            mTimerView.updateValue(0);
        } else if (sTime == -1) {
            mTimerView.updateValue(mInitTime);
            mStartButton.setText(R.string.start);
        } else {
            mTimerView.updateValue(sTime);
            mStartButton.setText(R.string.reset);
        }

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXPERTS_SCORE_KEY, mExpertsScore);
        outState.putInt(VIEWERS_SCORE_KEY, mViewersScore);
        outState.putInt(INIT_TIME_KEY, mInitTime);
        outState.putInt(ADD_MINUTES_KEY, mAdditionalMinutes);
        outState.putInt(START_BUTTON_COLOR_KEY,
                ((ColorDrawable) mStartButton.getBackground()).getColor());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_button:
                if (sPlayer != null) {
                    sPlayer.release();
                    sPlayer = null;
                    mTimerView.updateValue(mInitTime);
                    mStartButton.setBackgroundColor(0xff00ff00);
                    mStartButton.setText(R.string.start);
                } else if(sTime == -1) {
                    sHandler.removeCallbacksAndMessages(null);
                    sColorHandler.removeCallbacksAndMessages(null);
                    sTime = mInitTime;
                    mStartButton.setText(R.string.reset);
                    ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    sHandler.postDelayed(sRunnable, 1000);
                    sColorHandler.postDelayed(sColorRunnable,
                            mInitTime == STANDARD_TIME ? 3000 / 13 : 1000 / 13);
                } else {
                    sTime = -1;
                    mTimerView.updateValue(mInitTime);
                    mStartButton.setBackgroundColor(0xff00ff00);
                    mStartButton.setText(R.string.start);
                }
                break;
        }
    }

    public class OnSwipeTouchListener implements OnTouchListener {
        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends SimpleOnGestureListener {
            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) < Math.abs(distanceY)) {
                    if (Math.abs(distanceY) > SWIPE_DISTANCE_THRESHOLD
                            && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (distanceY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                        return true;
                    }
                } else {
                    if (Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD
                            && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (distanceX > 0)
                            onSwipeRight();
                        else
                            onSwipeLeft();
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
