package com.funguscow.pixelart.scale;

import com.funguscow.pixelart.Utils;

import java.util.Arrays;

/**
 * Scale by 2x2 using Scale2x
 * <p>
 * Given:
 * - B -
 * D E F
 * - H -
 * <p>
 * E will be scaled into:
 * E0 E1
 * E2 E3
 * <p>
 * If B == H or D == F, E0-E3 will be set to E, because either:
 * There will be no matching corners, so all should be E, or
 * At least one corner will match, which by the transitive property, means at least one adjacent
 * corner must also match, so at least 3 surrounding pixels must be identical. In the Scale2x
 * algorithm, when there are three or more identical neighbors, all pixels are set to E to preserve
 * 1-pixel details. If B != H and D != F, each En is set to its neighbors if the neighbors of that
 * corner match, or to E by default.
 */
public class Scale2x extends ImageScaler {

    public Scale2x() {
        super(2);
    }

    @Override
    protected void scalePixel(int[] input, int width, int height, int x, int y) {
        int B = Utils.clampedPixelAt(input, width, height, x, y - 1);
        int D = Utils.clampedPixelAt(input, width, height, x - 1, y);
        int E = input[y * width + x];
        int F = Utils.clampedPixelAt(input, width, height, x + 1, y);
        int H = Utils.clampedPixelAt(input, width, height, x, y + 1);
        Arrays.fill(scaledPixels, E);
        if (B == H || D == F) {
            return;
        }
        if (B == D) {
            scaledPixels[0] = B;
        }
        if (B == F) {
            scaledPixels[1] = B;
        }
        if (H == D) {
            scaledPixels[2] = H;
        }
        if (H == F) {
            scaledPixels[3] = F;
        }
    }
}
