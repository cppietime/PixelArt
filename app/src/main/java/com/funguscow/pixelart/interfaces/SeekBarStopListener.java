package com.funguscow.pixelart.interfaces;

import android.widget.SeekBar;

public interface SeekBarStopListener extends SeekBar.OnSeekBarChangeListener {

    @Override
    default void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    default void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }
}
