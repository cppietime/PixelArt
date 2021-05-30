package com.funguscow.pixelart.data;

/**
 * Necessary parameters for image generation
 */
public class Specs {

    public int width = 16, height = 16;
    public int colors = 4, seeds = 4;
    public long seed;

    public float minProb = 0f, maxProb = 1f, bias = 0.5f, gain = 0.5f;
    public float xMirror = 0.5f, yMirror = 0.5f, pMirror = 0.5f, nMirror = 0.5f;
    public float variance = 0.5f, mutation = 0.5f;
    public float hue = 0, saturation = 0, value = 0;

    public float[] caProbs = {0.9f, 0.5f, 0, 0};

    public boolean randomSeed = true, randomColor = true;

}
