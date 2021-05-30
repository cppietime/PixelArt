package com.funguscow.pixelart.scale;

/**
 * Scales an image by a certain algorithm
 */
public abstract class ImageScaler {

    private final int ratio;

    /**
     *
     * @param ratio Ratio of output to input pixels on each axis
     */
    protected ImageScaler(int ratio) {
        this.ratio = ratio;
    }

    /**
     *
     * @return Ratio of output to input pixels on each axis
     */
    public int getRatio() {
        return ratio;
    }

}
