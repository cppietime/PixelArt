package com.funguscow.pixelart.scale;

import com.funguscow.pixelart.Utils;

import java.util.Arrays;

/**
 * Scale an image using Eagle2x
 * <p>
 * Given an input grid of pixels:
 * A B C
 * D E F
 * G H I
 * <p>
 * E will be scaled into four pixels:
 * E0 E1
 * E2 E3
 * <p>
 * For each En, the three neighbors are checked. If they are all equal, En is set to the neighboring
 * value, otherwise, En is set to E. Eagle2x does not preserve 1-pixel details surrounded by a solid
 * color
 */
public class Eagle2x extends  ImageScaler {

    public Eagle2x() {
        super(2);
    }

    @Override
    protected void scalePixel(int[] input, int width, int height, int x, int y) {
        int A = Utils.clampedPixelAt(input, width, height, x - 1, y - 1);
        int B = Utils.clampedPixelAt(input, width, height, x, y - 1);
        int C = Utils.clampedPixelAt(input, width, height, x + 1, y - 1);
        int D = Utils.clampedPixelAt(input, width, height, x - 1, y);
        int E = input[y * width + x];
        int F = Utils.clampedPixelAt(input, width, height, x + 1, y );
        int G = Utils.clampedPixelAt(input, width, height, x - 1, y + 1);
        int H = Utils.clampedPixelAt(input, width, height, x, y + 1);
        int I = Utils.clampedPixelAt(input, width, height, x + 1, y + 1);
        Arrays.fill(scaledPixels, E);
        if (A == B && A == D) {
            scaledPixels[0] = A;
        }
        if (B == C && C == F) {
            scaledPixels[1] = C;
        }
        if (D == G && G == H) {
            scaledPixels[2] = G;
        }
        if (I == H && H == F) {
            scaledPixels[3] = H;
        }
    }
}
