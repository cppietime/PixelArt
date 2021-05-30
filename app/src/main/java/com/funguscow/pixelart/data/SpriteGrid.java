package com.funguscow.pixelart.data;

import android.graphics.Bitmap;
import android.util.Log;

import com.funguscow.pixelart.Utils;
import com.funguscow.pixelart.scale.ImageScaler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * A grid that acts as the intermediate for sprite generation
 */
public class SpriteGrid {

    private static final float H_SIGMA = 0.2f, S_SIGMA = 0.2f, V_SIGMA = 0.2f;

    private int[] grid;
    private final int width, height;
    private final Specs specs;
    private final Random random;
    private int[] palette;

    private boolean mirrorX, mirrorY, mirrorP, mirrorN;

    /**
     * @param specs Input parameters to use in generating a sprite
     */
    public SpriteGrid(Specs specs) {
        this.specs = specs;
        width = this.specs.width;
        height = this.specs.height;
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Invalid dimensions" + width + " x " + height);
        }
        grid = new int[height * width];
        random = new Random(specs.seed);
    }

    /**
     * Generate the image and write them to {@code img}
     *
     * @param img Bitmap that acts as a destination
     * @throws IllegalArgumentException When {@code img}'s dimensions do not match this'
     */
    public void drawTo(Bitmap img) {
        mirrorX = random.nextFloat() < specs.xMirror;
        mirrorY = random.nextFloat() < specs.yMirror;
        mirrorP = random.nextFloat() < specs.pMirror;
        mirrorN = random.nextFloat() < specs.nMirror;
        populatePalette();
        fillCells();
        simulateCA();
        colorize();
        mirror();
        if (img.getHeight() != specs.targetHeight || img.getWidth() != specs.targetWidth) {
            throw new IllegalArgumentException("Provided bitmap's dimensions do not match grid");
        }
        int[] inGrid = grid;
        int w = width, h = height;
        ImageScaler scaler = ImageScaler.Scalers.get(specs.scaleName).get();
        while (w * scaler.ratio <= specs.targetWidth || h * scaler.ratio <= specs.targetHeight) {
            Log.d("Pixelart", "Scaling " + w + ", " + h + " by " + scaler.ratio);
            inGrid = scaler.scale(inGrid, w, h);
            w *= scaler.ratio;
            h *= scaler.ratio;
        }
        if (w != specs.targetWidth || h != specs.targetHeight) {
            inGrid = ImageScaler.scaleNearestNeighbor(inGrid,
                    w,
                    h,
                    specs.targetWidth,
                    specs.targetHeight);
        }
        for (int y = 0; y < specs.targetHeight; y++) {
            for (int x = 0; x < specs.targetWidth; x++) {
                img.setPixel(x, y, inGrid[y * specs.targetWidth + x]);
            }
        }
    }

    /**
     * Create/populate the image palette with colors
     */
    private void populatePalette() {
        palette = new int[specs.colors];
        palette[0] = Utils.HSV_to_ARGB(specs.hue, specs.saturation, specs.value);
        for (int i = 1; i < palette.length; i++) {
            float h = (float) random.nextGaussian() * H_SIGMA + specs.hue;
            float s = (float) random.nextGaussian() * S_SIGMA + specs.saturation;
            s = Utils.clamp(s, 0, 1);
            float v = (float) random.nextGaussian() * V_SIGMA + specs.value;
            v = Utils.clamp(v, 0, 1);
            // No need to clamp h as it wraps around anyway
            palette[i] = Utils.HSV_to_ARGB(h, s, v);
        }
    }

    /**
     * Randomly mark cells as empty/filled
     */
    private void fillCells() {
        for (int y = 0; y < height; y++) {
            float yDist = 1 - Math.abs((height - y * 2f) / height);
            for (int x = 0; x < width; x++) {
                float xDist = 1 - Math.abs((width - x * 2f) / width);
                float dist = yDist * xDist;
                float param = Utils.bias(Utils.gain(dist, specs.gain), specs.bias);
                float cutoff = Utils.lerp(specs.minProb, specs.maxProb, param);
                if (random.nextFloat() <= cutoff) {
                    grid[y * width + x] = 1;
                } else {
                    grid[y * width + x] = 0;
                }
            }
        }
    }

    /**
     * Despeckle, despur, relax, and devoid
     */
    private void simulateCA() {
        int[] temp = new int[width * height];
        int generations = 1; // TODO allow specifying
        for (int i = 0; i < generations; i++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int neighbors = -grid[y * width + x]; // Subtract self to only look at neighbors
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (x + dx >= 0 && x + dx < width && y + dy >= 0 && y + dy < height) {
                                neighbors += grid[(y + dy) * width + x + dx];
                            }
                        }
                    }
                    if (neighbors <= 1 && random.nextFloat() < specs.caProbs[neighbors]) {
                        temp[y * width + x] = 0;
                    } else if (neighbors >= 7 &&
                            random.nextFloat() < specs.caProbs[neighbors - 7 + 2]) {
                        temp[y * width + x] = 1;
                    } else {
                        temp[y * width + x] = grid[y * width + x];
                    }
                }
            }
            int[] swap = temp;
            temp = grid;
            grid = swap;
        }
    }

    /**
     * Plant random color seeds and propagate with mutation
     */
    private void colorize() {
        Deque<Integer> frontier = new ArrayDeque<>();
        for (int i = 0; i < specs.seeds; i++) {
            int index = random.nextInt(width * height);
            int color = palette[random.nextInt(palette.length)];
            int old = grid[index];
            grid[index] = 0xffffff & color;
            if (old == 1) {
                grid[index] |= 0xff000000;
            } else {
                grid[index] |= 0x01000000;
            }
            frontier.add(index);
        }
        while (!frontier.isEmpty()) {
            int index = frontier.remove();
            boolean finished = true;
            int y = index / width, x = index % width;
            for (int dx = -1; dx <= 1; dx++) {
                if (x + dx < 0 || x + dx >= width)
                    continue;
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0)
                        continue;
                    if (y + dy < 0 || y + dy >= height)
                        continue;
                    int neighbor = (y + dy) * width + x + dx;
                    if (grid[neighbor] == 0 || grid[neighbor] == 1) {
                        if (random.nextFloat() <= specs.variance) {
                            int old = grid[neighbor];
                            if (random.nextFloat() <= specs.mutation) {
                                grid[neighbor] = 0xffffff & palette[random.nextInt(palette.length)];
                            } else {
                                random.nextInt(); // Keep state consistent
                                grid[neighbor] = 0xffffff & grid[index];
                            }
                            if (old == 1) {
                                grid[neighbor] |= 0xff000000;
                            } else {
                                grid[neighbor] |= 0x01000000;
                            }
                            frontier.add(neighbor);
                        } else {
                            finished = false;
                        }
                    }
                }
            }
            if (!finished) {
                frontier.add(index);
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y * width + x] == 0 || grid[y * width + x] == 1) {
                    Log.e("Pixelart", "Problem at " + x + ", " + y);
                }
                if ((grid[y * width + x] >>> 24) != 0xff) {
                    grid[y * width + x] = 0;
                }
            }
        }
    }

    /**
     * Mirror on axes and diagonals
     */
    private void mirror() {
        if (mirrorX) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width / 2; x++) {
                    grid[y * width + width - 1 - x] = grid[y * width + x];
                }
            }
        }
        if (mirrorY) {
            for (int y = 0; y < height / 2; y++) {
                System.arraycopy(grid,
                        y * width,
                        grid,
                        (height - 1 - y) * width,
                        width);
            }
        }
        if (specs.width == specs.height) {
            if (mirrorP) {
                for (int y = 1; y < height; y++) {
                    for (int x = 0; x < y; x++) {
                        grid[x * width + y] = grid[y * width + x];
                    }
                }
            }
            if (mirrorN) {
                for (int y = 1; y < height; y++) {
                    for (int x = width - y; x < width; x++) {
                        grid[(width - 1 - x) * width + width - 1 - y] = grid[y * width + x];
                    }
                }
            }
        }
    }

}
