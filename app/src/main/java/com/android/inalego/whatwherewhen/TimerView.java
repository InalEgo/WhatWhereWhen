package com.android.inalego.whatwherewhen;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TimerView extends TextView {

    public TimerView(Context context) {
        super(context);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void updateValue(int seconds) {
        setText(String.format("%d:%02d", seconds / 60, seconds % 60));
    }
}