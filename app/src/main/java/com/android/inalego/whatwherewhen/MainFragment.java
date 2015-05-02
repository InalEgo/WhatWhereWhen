package com.android.inalego.whatwherewhen;

import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
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
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;

import java.lang.ref.WeakReference;

import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;

public class MainFragment extends Fragment implements OnClickListener {
    private static final String SCORE_WHEEL_ITEMS[] =
            {"11", "10", "9", "8", "7", "6", "5", "4", "3", "2", "1", "0"};
    private static final String ADD_MINUTES_WHEEL_ITEMS[] = {"5'", "4'", "3'", "2'", "1'", "0'"};
    private static final int INITIAL_START_BUTTON_COLOR = 0xff00ff00;
    private static final String GAME_STATE_KEY = "game_state_key";
    private static final String START_BUTTON_COLOR_KEY = "start_button_color_key";
    private static final int STANDARD_TIME = 60;
    private static final int BLITZ_TIME = 20;
    private static final Handler sTimerHandler = new Handler();
    private static final Handler sColorHandler = new Handler();
    private static MediaPlayer sPlayer;
    private static WeakReference<MainFragment> sCurrentFragment;
    private static int sTime;
    private GameState mGameState;
    private WheelView mExpertsScoreView;
    private WheelView mViewersScoreView;
    private WheelView mAdditionalMinutesView;
    private TimerView mTimerView;
    private Button mStartButton;
    private static final Runnable sTimerRunnable = new Runnable() {
        @Override
        public void run() {
            sCurrentFragment.get().handleTimer();
        }
    };
    private int mInitTime;
    private int mStartButtonColor;
    private static final Runnable sColorRunnable = new Runnable() {
        @Override
        public void run() {
            sCurrentFragment.get().handleColor();
        }
    };

    private void handleTimer() {
        if (sTime > 0) {
            sTime--;
            if (sTime == 10 && !mGameState.isBlitz()) {
                sPlayer = MediaPlayer.create(getActivity(),
                        Settings.System.DEFAULT_NOTIFICATION_URI);
                sPlayer.setOnCompletionListener(new OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        sPlayer.release();
                        sPlayer = null;
                    }
                });
                sPlayer.start();
            }
            sTimerHandler.postDelayed(sTimerRunnable, 1000);
        } else {
            mStartButton.setText(R.string.stop);
            setPlayer();
        }
        if (isVisible()) {
            mTimerView.updateValue(sTime);
        }
    }

    private void setPlayer() {
        sPlayer = MediaPlayer.create(getActivity(),
                Settings.System.DEFAULT_NOTIFICATION_URI);
        sPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });
        sPlayer.start();
    }

    private void handleColor() {
        if (sTime != 0) {
            int red = (mStartButtonColor & 0x00ff0000) >> 0x10;
            int green = (mStartButtonColor & 0x0000ff00) >> 8;
            red = red < 0xff ? red + 1 : 0xff;
            green = green > 0 ? green - 1 : 0;
            mStartButtonColor = 0xff000000 + (red << 0x10) + (green << 8);
            mStartButton.setBackgroundColor(mStartButtonColor);
            sColorHandler.postDelayed(sColorRunnable, mGameState.isBlitz() ? 1000 / 13 : 3000 / 13);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reset) {
            mGameState.mExpertsScore = 0;
            mGameState.mViewersScore = 0;
            mGameState.mAdditionalMinutes = 0;
            mExpertsScoreView.setCurrentItem(
                    SCORE_WHEEL_ITEMS.length - 1 - mGameState.mExpertsScore);
            mViewersScoreView.setCurrentItem(
                    SCORE_WHEEL_ITEMS.length - 1 - mGameState.mViewersScore);
            mAdditionalMinutesView.setCurrentItem(
                    ADD_MINUTES_WHEEL_ITEMS.length - 1 - mGameState.mAdditionalMinutes);
            sTimerHandler.removeCallbacksAndMessages(null);
            sColorHandler.removeCallbacksAndMessages(null);
            mExpertsScoreView.setEnabled(true);
            mViewersScoreView.setEnabled(true);
            mAdditionalMinutesView.setEnabled(true);
            mGameState.setBlitz(false);
            mInitTime = STANDARD_TIME;
            sTime = mInitTime;
            mTimerView.updateValue(sTime);
            mStartButtonColor = INITIAL_START_BUTTON_COLOR;
            mStartButton.setBackgroundColor(mStartButtonColor);
            mStartButton.setText(R.string.start);
            if (sPlayer != null) {
                sPlayer.release();
                sPlayer = null;
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private WheelView initWheel(View rootView, int id, String[] wheelMenu) {
        WheelView wheel = (WheelView) rootView.findViewById(id);
        ArrayWheelAdapter<String> wheelAdapter = new ArrayWheelAdapter<>(getActivity(), wheelMenu);
        wheelAdapter.setTextSize(60);
        wheelAdapter.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        wheel.setViewAdapter(wheelAdapter);
        wheel.setDrawCenterRect(false);
        wheel.setDrawShadows(false);
        wheel.setBackgroundNeeded(false);
        wheel.setVisibleItems(1);
        return wheel;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        sCurrentFragment = new WeakReference<>(this);

        if (savedInstanceState != null) {
            mGameState = savedInstanceState.getParcelable(GAME_STATE_KEY);
            mInitTime = mGameState.isBlitz() ? BLITZ_TIME : STANDARD_TIME;
            mStartButtonColor = savedInstanceState.getInt(START_BUTTON_COLOR_KEY);
        } else {
            mGameState = new GameState();
            mInitTime = STANDARD_TIME;
            sTime = mInitTime;
            mStartButtonColor = INITIAL_START_BUTTON_COLOR;
        }

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mExpertsScoreView = initWheel(rootView, R.id.expertsScore, SCORE_WHEEL_ITEMS);
        mExpertsScoreView.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                mGameState.mExpertsScore = SCORE_WHEEL_ITEMS.length - 1 - newValue;
                if (mGameState.mExpertsScore + mGameState.mViewersScore > 11) {
                    mGameState.mExpertsScore = 11 - mGameState.mViewersScore;
                    mExpertsScoreView.setCurrentItem(11 - mGameState.mExpertsScore, true);
                }
            }
        });
        mViewersScoreView = initWheel(rootView, R.id.viewersScore, SCORE_WHEEL_ITEMS);
        mViewersScoreView.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                mGameState.mViewersScore = SCORE_WHEEL_ITEMS.length - 1 - newValue;
                if (mGameState.mExpertsScore + mGameState.mViewersScore > 11) {
                    mGameState.mViewersScore = 11 - mGameState.mExpertsScore;
                    mViewersScoreView.setCurrentItem(11 - mGameState.mViewersScore, true);
                }
            }
        });

        mAdditionalMinutesView = initWheel(rootView,
                R.id.additional_minutes, ADD_MINUTES_WHEEL_ITEMS);
        mAdditionalMinutesView.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                mGameState.mAdditionalMinutes = ADD_MINUTES_WHEEL_ITEMS.length - 1 - newValue;
            }
        });
        mTimerView = (TimerView) rootView.findViewById(R.id.timer);
        mStartButton = (Button) rootView.findViewById(R.id.start_button);
        mStartButton.setOnClickListener(this);

        mTimerView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            public void onSwipeLeft() {
                if (sPlayer == null && sTime == mInitTime) {
                    mGameState.setBlitz(!mGameState.isBlitz());
                    mInitTime = mGameState.isBlitz() ? BLITZ_TIME : STANDARD_TIME;
                    sTime = mInitTime;
                    mTimerView.updateValue(sTime);
                }
            }

            public void onSwipeRight() {
                if (sPlayer == null && sTime == mInitTime) {
                    mGameState.setBlitz(!mGameState.isBlitz());
                    mInitTime = mGameState.isBlitz() ? BLITZ_TIME : STANDARD_TIME;
                    sTime = mInitTime;
                    mTimerView.updateValue(sTime);
                }
            }
        });

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(GAME_STATE_KEY, mGameState);
        outState.putInt(START_BUTTON_COLOR_KEY, mStartButtonColor);
    }

    @Override
    public void onResume() {
        super.onResume();
        mExpertsScoreView.setCurrentItem(SCORE_WHEEL_ITEMS.length - 1 - mGameState.mExpertsScore);
        mViewersScoreView.setCurrentItem(SCORE_WHEEL_ITEMS.length - 1 - mGameState.mViewersScore);
        mAdditionalMinutesView.setCurrentItem(
                ADD_MINUTES_WHEEL_ITEMS.length - 1 - mGameState.mAdditionalMinutes);
        if (sTime == mInitTime) {
            mStartButton.setText(R.string.start);
        } else {
            mExpertsScoreView.setEnabled(false);
            mViewersScoreView.setEnabled(false);
            mAdditionalMinutesView.setEnabled(false);
            if (sPlayer != null) {
                mStartButton.setText(R.string.stop);
            } else {
                mStartButton.setText(R.string.reset);
            }
        }
        mTimerView.updateValue(sTime);
        mStartButton.setBackgroundColor(mStartButtonColor);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                sTimerHandler.removeCallbacksAndMessages(null);
                sColorHandler.removeCallbacksAndMessages(null);
                if (sPlayer == null && sTime == mInitTime) {
                    mStartButton.setText(R.string.reset);
                    ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    mExpertsScoreView.setEnabled(false);
                    mViewersScoreView.setEnabled(false);
                    mAdditionalMinutesView.setEnabled(false);
                    sTimerHandler.postDelayed(sTimerRunnable, 1000);
                    sColorHandler.postDelayed(sColorRunnable,
                            mGameState.isBlitz() ? 1000 / 13 : 3000 / 13);
                } else {
                    sTime = mInitTime;
                    mTimerView.updateValue(sTime);
                    mStartButtonColor = INITIAL_START_BUTTON_COLOR;
                    mStartButton.setBackgroundColor(mStartButtonColor);
                    mStartButton.setText(R.string.start);
                    mExpertsScoreView.setEnabled(true);
                    mViewersScoreView.setEnabled(true);
                    mAdditionalMinutesView.setEnabled(true);
                    if (sPlayer != null) {
                        sPlayer.release();
                        sPlayer = null;
                    }
                }
                break;
        }
    }

    public static class GameState implements Parcelable {
        public static final Parcelable.Creator<GameState> CREATOR
                = new Parcelable.Creator<GameState>() {
            public GameState createFromParcel(Parcel in) {
                return new GameState(in);
            }

            public GameState[] newArray(int size) {
                return new GameState[size];
            }
        };
        private int mExpertsScore;
        private int mViewersScore;
        private int mAdditionalMinutes;
        private boolean mBlitz;

        public GameState() {
        }

        private GameState(Parcel in) {
            mExpertsScore = in.readInt();
            mViewersScore = in.readInt();
            mAdditionalMinutes = in.readInt();
            mBlitz = in.readInt() != 0;
        }

        public boolean isBlitz() {
            return mBlitz;
        }

        public void setBlitz(boolean blitz) {
            mBlitz = blitz;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(mExpertsScore);
            out.writeInt(mViewersScore);
            out.writeInt(mAdditionalMinutes);
            out.writeInt(mBlitz ? 1 : 0);
        }
    }

    private static class OnSwipeTouchListener implements OnTouchListener {
        private final GestureDetector mGestureDetector;

        public OnSwipeTouchListener(Context context) {
            mGestureDetector = new GestureDetector(context, new GestureListener());
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
            return mGestureDetector.onTouchEvent(event);
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
