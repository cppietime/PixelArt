package com.funguscow.pixelart.scale;

import java.util.Arrays;

/**
 * Scale by nearest-neighbor by an integer amount
 * <p>
 * For a ratio R, pixel E is scaled into:
 * E0 . . . ER-1
 * .
 * .
 * .
 * ER-1
 */
public class NearestNeighborI extends ImageScaler {

    /**
     *
     * @param ratio Integer ratio by which to expand each dimension
     */
    public NearestNeighborI(int ratio) {
        super(ratio);
    }

    @Override
    protected void scalePixel(int[] input, int width, int height, int x, int y) {
        Arrays.fill(scaledPixels, input[y * width + x]);
    }
}
