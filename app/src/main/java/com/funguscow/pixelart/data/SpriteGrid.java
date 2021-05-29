package com.funguscow.pixelart.data;

import android.graphics.Bitmap;
import android.util.Log;

import com.funguscow.pixelart.Utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.Random;

public class SpriteGrid {

    private int[] grid;
    private int width, height;
    private final Specs specs;
    private final Random random;
    private int[] palette;

    private boolean mirrorX, mirrorY, mirrorP, mirrorN;

    public SpriteGrid(Specs specs) {
        this.specs = specs;
        width = this.specs.width;
        height = this.specs.height;
        grid = new int[height * width];
        palette = new int[specs.colors];
        random = new Random(specs.seed);
    }

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
        if (img.getHeight() != height || img.getWidth() != width) {
            throw new IllegalArgumentException("Provided bitmap's dimensions do not match grid");
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
//                Log.d("Pixelart", "(" + x + ", " + y + "): " + Integer.toHexString(grid[y * width + x]));
                img.setPixel(x, y, grid[y * width + x]);
            }
        }
    }

    private void populatePalette() {
        palette[0] = Utils.HSV_to_ARGB(specs.hue, specs.saturation, specs.value);
        for (int i = 1; i < palette.length; i++) {
            float h = (float) random.nextGaussian() * 0.1f + specs.hue;
            float s = (float) random.nextGaussian() * 0.1f + specs.saturation;
            s = Utils.clamp(s, 0, 1);
            float v = (float) random.nextGaussian() * 0.1f + specs.value;
            v = Utils.clamp(v, 0, 1);
            // No need to clamp h as it wraps around anyway
            palette[i] = Utils.HSV_to_ARGB(h, s, v);
//            Log.d("Pixelart", "Palette color " + Integer.toHexString(palette[i]));
        }
    }

    private void fillCells() {
        for (int y = 0; y < height; y++) {
            float yDist = 1 - Math.abs((height - y * 2f) / height);
            for (int x = 0; x < width; x++) {
                float xDist = 1 - Math.abs((width - x * 2f) / width);
                float dist = yDist * xDist;
                float param = Utils.bias(Utils.gain(dist, specs.gain), specs.bias);
                float cutoff = Utils.lerp(specs.minProb, specs.maxProb, param);
                if (random.nextFloat() <= cutoff) {
//                    Log.d("Pixelart", "Filled " + x + ", " + y);
                    grid[y * width + x] = 1; //palette[random.nextInt(palette.length)]; // Magic number w/ 0 alpha
                }
                else {
                    grid[y * width + x] = 0;
                }
            }
        }
    }

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
                    } else if (neighbors >= 7 && random.nextFloat() < specs.caProbs[neighbors - 7 + 2]) {
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

    private void colorize() {
        Deque<Integer> frontier = new ArrayDeque<>();
        for (int i = 0; i < specs.colors; i++) {
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
//            Log.d("Pixelart", "Run frontier " + Integer.toHexString(grid[index]));
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
                for (int x = 0; x < width; x++) {
                    grid[(height - 1 - y) * width + x] = grid[y * width + x];
                }
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
