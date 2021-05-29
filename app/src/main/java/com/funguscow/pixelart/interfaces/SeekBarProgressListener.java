package com.funguscow.pixelart.interfaces;

import android.widget.SeekBar;

public interface SeekBarProgressListener extends SeekBar.OnSeekBarChangeListener {

    @Override
    default void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    default void onStopTrackingTouch(SeekBar seekBar){}
}
