package com.funguscow.pixelart.scale;

import com.funguscow.pixelart.Utils;

import java.util.Arrays;

/**
 * Scale an image using Scale3x
 * <p>
 * Given the 3x3 input pixel grid:
 * A B C
 * D E F
 * G H I
 * <p>
 * E will be scaled into:
 * E0 E1 E2
 * E3 E4 E5
 * E6 E7 E8
 * <p>
 * E4 will always be set to E. The four corners are scaled identically to Scale2x. For the four
 * sides, if that side is equal to either adjacent side, and each of those sides are different from
 * their other neighboring side, and E is different from the outer corner on that side, it is set to
 * the neighbor.
 */
public class Scale3x extends ImageScaler {

    public Scale3x() {
        super(3);
    }

    @Override
    protected void scalePixel(int[] input, int width, int height, int x, int y) {
        int B = Utils.clampedPixelAt(input, width, height, x, y - 1);
        int D = Utils.clampedPixelAt(input, width, height, x - 1, y);
        int E = input[y * width + x];
        int F = Utils.clampedPixelAt(input, width, height, x + 1, y);
        int H = Utils.clampedPixelAt(input, width, height, x, y + 1);
        Arrays.fill(scaledPixels, E);
        if(B == H || D == F) {
            return;
        }
        int A = Utils.clampedPixelAt(input, width, height, x - 1, y - 1);
        int C = Utils.clampedPixelAt(input, width, height, x + 1, y - 1);
        int G = Utils.clampedPixelAt(input, width, height, x - 1, y + 1);
        int I = Utils.clampedPixelAt(input, width, height, x + 1, y + 1);
        if (B == D) {
            scaledPixels[0] = B;
            if (E != C) {
                scaledPixels[1] = B;
            }
            if (E != G) {
                scaledPixels[3] = B;
            }
        }
        if (B == F) {
            scaledPixels[2] = B;
            if (E != A) {
                scaledPixels[1] = B;
            }
            if (E != I) {
                scaledPixels[5] = B;
            }
        }
        if (H == D) {
            scaledPixels[6] = H;
            if (E != A) {
                scaledPixels[3] = H;
            }
            if (E != I) {
                scaledPixels[7] = H;
            }
        }
        if (H == F) {
            scaledPixels[8] = H;
            if (E != C) {
                scaledPixels[5] = H;
            }
            if (E != G) {
                scaledPixels[7] = H;
            }
        }
    }
}
