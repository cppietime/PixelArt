package com.funguscow.pixelart.scale;

import com.funguscow.pixelart.interfaces.Generator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Scales an image by a certain algorithm
 */
public abstract class ImageScaler {

    public static final Map<String, Generator<ImageScaler>> Scalers = new HashMap<>();

    /**
     * Ratio of output to input pixels on each axis
     */
    public final int ratio;

    protected final int[] scaledPixels;

    /**
     * @param ratio Ratio of output to input pixels on each axis
     */
    protected ImageScaler(int ratio) {
        this.ratio = ratio;
        scaledPixels = new int[ratio * ratio];
    }

    /**
     * Algorithm to scale a single pixel to ratio x ratio pixels
     *
     * @param input  Input image
     * @param width  Width in pixels of {@code input}
     * @param height Height in pixels of {@code input}
     * @param x      X offset into {@code input} of current pixel
     * @param y      Y offset into {@code input} of current pixel
     */
    protected abstract void scalePixel(int[] input, int width, int height, int x, int y);

    /**
     * Scale an image
     *
     * @param input  Input image
     * @param width  Width in pixels of {@code input}
     * @param height Height in pixels of {@code input}
     * @return Scaled image of size {@code ratio} * {@code width} x {@code ratio} * {@code height}
     */
    public int[] scale(int[] input, int width, int height) {
        if (ratio == 1) {
            return input;
        }
        int[] scaled = new int[width * ratio * height * ratio];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                scalePixel(input, width, height, x, y);
                for (int oy = 0; oy < ratio; oy++) {
                    System.arraycopy(scaledPixels,
                            oy * ratio,
                            scaled,
                            index + oy * ratio * width,
                            ratio);
                }
                index += ratio;
            }
            index += (ratio - 1) * width * ratio;
        }
        return scaled;
    }

    /**
     * Scale an arbitrary amount by nearest-neighbor
     *
     * @param input     Input image
     * @param widthIn   Width in pixels of {@code input}
     * @param heightIn  Height in pixels of {@code input}
     * @param widthOut  Width in pixels of target scaled image
     * @param heightOut Height in pixels of target scaled image
     * @return An image scaled by nearest-neighbor to {@code widthOut} x {@code heightOut}
     */
    public static int[] scaleNearestNeighbor(int[] input,
                                             int widthIn,
                                             int heightIn,
                                             int widthOut,
                                             int heightOut) {
        int[] scaled = new int[widthOut * heightOut];
        float xDelta = (float) widthIn / widthOut;
        float yDelta = (float) heightIn / heightOut;
        int yPos = 0;
        float yError = 0f;
        for (int y = 0; y < heightOut; y++) {
            int xPos = 0;
            float xError = 0f;
            for (int x = 0; x < widthOut; x++) {

                scaled[y * widthOut + x] = input[yPos * widthIn + xPos];

                xError += xDelta;
                if (xError >= 1) {
                    xPos += (int) xError;
                    xError -= (int) xError;
                }
            }
            yError += yDelta;
            if (yError >= 1) {
                yPos += (int) yError;
                yError -= (int) yError;
            }
        }
        return scaled;
    }

}
