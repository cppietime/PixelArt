package com.funguscow.pixelart;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.funguscow.pixelart.data.Specs;
import com.funguscow.pixelart.data.SpriteGrid;
import com.funguscow.pixelart.interfaces.SeekBarStopListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

/**
 * Main activity for image generation
 */
public class MainActivity extends AppCompatActivity {

    private static final int MINIMUM_SIZE = 4;

    private ImageView preview, colorView;
    private Bitmap sprite;
    private BitmapDrawable spriteDrawable;
    private Random random;

    private EditText seedInput, widthInput, heightInput, colorsInput, seedsInput;
    private SeekBar edgeInput, centerInput, biasInput, gainInput, xInput, yInput, nInput, pInput, speedInput, mutationInput;
    private SeekBar hueInput, saturationInput, valueInput;
    private Spinner probSpinner;
    private SeekBar probInput;
    private CheckBox randomSeed, randomColor;

    private Specs specState = new Specs();

    private Button randomAllButton, refreshButton, saveButton, shareButton, randomSeedButton, batchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preview = findViewById(R.id.preview);
        colorView = findViewById(R.id.colorPreview);

        seedInput = findViewById(R.id.seedInput);
        widthInput = findViewById(R.id.widthInput);
        heightInput = findViewById(R.id.heightInput);
        colorsInput = findViewById(R.id.numColorsInput);
        seedsInput = findViewById(R.id.numSeedsInput);

        edgeInput = findViewById(R.id.edgeDensityInput);
        centerInput = findViewById(R.id.centerDensityInput);
        biasInput = findViewById(R.id.biasInput);
        gainInput = findViewById(R.id.gainInput);
        xInput = findViewById(R.id.xMirrorInput);
        yInput = findViewById(R.id.yMirrorInput);
        pInput = findViewById(R.id.positiveMirrorInput);
        nInput = findViewById(R.id.negativeMirrorInput);
        speedInput = findViewById(R.id.colorSpeedInput);
        mutationInput = findViewById(R.id.mutationInput);

        hueInput = findViewById(R.id.hueInput);
        saturationInput = findViewById(R.id.saturationInput);
        valueInput = findViewById(R.id.valueInput);

        probSpinner = findViewById(R.id.probSpinner);
        probInput = findViewById(R.id.probInput);

        randomSeed = findViewById(R.id.randomizeSeed);
        randomColor = findViewById(R.id.randomColor);

        randomSeedButton = findViewById(R.id.newSeed);
        randomAllButton = findViewById(R.id.randomButton);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);
        refreshButton = findViewById(R.id.refreshButton);
        batchButton = findViewById(R.id.batchButton);

        sprite = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);

        random = new Random();
        long seed = random.nextLong();
        specState.seed = seed;
        seedInput.setText(getString(R.string.integer, seed));
        updateSpecs(specState);
        SpriteGrid grid = new SpriteGrid(specState);
        grid.drawTo(sprite);

        BitmapDrawable drawable = new BitmapDrawable(getResources(), sprite);

        drawable.getPaint().setFilterBitmap(false);
        preview.setImageDrawable(drawable);

        updateColorPreview();

        probSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                runOnUiThread(() -> probInput.setProgress((int) (1000 * specState.caProbs[position])));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        probInput.setOnSeekBarChangeListener((SeekBarStopListener) (bar) -> {
            specState.caProbs[probSpinner.getSelectedItemPosition()] = valOf(bar);
            generate();
        });

        hueInput.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                specState.hue = (float) progress / bar.getMax();
                MainActivity.this.updateColorPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                runOnUiThread(() -> randomColor.setChecked(false));
                specState.randomColor = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                generate();
            }
        });
        saturationInput.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                specState.saturation = (float) progress / bar.getMax();
                MainActivity.this.updateColorPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                runOnUiThread(() -> randomColor.setChecked(false));
                specState.randomColor = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                generate();
            }
        });
        valueInput.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                specState.value = (float) progress / bar.getMax();
                MainActivity.this.updateColorPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                runOnUiThread(() -> randomColor.setChecked(false));
                specState.randomColor = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                generate();
            }
        });

        randomSeed.setOnCheckedChangeListener((view, checked) -> {
            specState.randomSeed = checked;
            runOnUiThread(() -> seedInput.setEnabled(!checked));
        });
        randomColor.setOnCheckedChangeListener((view, checked) -> specState.randomColor = checked);

        SeekBarStopListener generalListener = (bar) -> generate();
        edgeInput.setOnSeekBarChangeListener(generalListener);
        centerInput.setOnSeekBarChangeListener(generalListener);
        biasInput.setOnSeekBarChangeListener(generalListener);
        gainInput.setOnSeekBarChangeListener(generalListener);
        xInput.setOnSeekBarChangeListener(generalListener);
        yInput.setOnSeekBarChangeListener(generalListener);
        pInput.setOnSeekBarChangeListener(generalListener);
        nInput.setOnSeekBarChangeListener(generalListener);
        speedInput.setOnSeekBarChangeListener(generalListener);
        mutationInput.setOnSeekBarChangeListener(generalListener);

        randomSeedButton.setOnClickListener((button) -> {
            if (!specState.randomSeed) {
                runOnUiThread(() -> seedInput.setText(getString(R.string.integer, random.nextLong())));
            }
            generate();
        });

        randomAllButton.setOnClickListener((button) -> {
            randomizeSpecs(specState);
            generate();
        });

        shareButton.setOnClickListener((button) -> share());

        refreshButton.setOnClickListener((button) -> generate());

        saveButton.setOnClickListener((button) -> Utils.saveBitmap(sprite, this, "PixelSprites", "PixelArt_" + System.currentTimeMillis(), true));

        batchButton.setOnClickListener(button -> promptSaveBatch());

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.info) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Help");
            builder.setMessage(getString(R.string.explanation));
            builder.setPositiveButton("Ok", (dialog, which) -> {

            });
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener((di) -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimaryDark)));
            dialog.show();
            return true;
        } else if (item.getItemId() == R.id.source) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cppietime/PixelArt"));
            startActivity(browserIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Share the currently displayed image as the user chooses.
     * Saves a file to the cache dir for sharing
     */
    private void share() {
        try {
            File imagesFolder = new File(getCacheDir(), "images");
            if (!imagesFolder.exists()) {
                if( !imagesFolder.mkdirs() ) {
                    Log.e("Pixelart", "Could not create directory");
                }
            }
            File cacheFile = new File(imagesFolder, "shared_image.png");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            sprite.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Uri uri = FileProvider.getUriForFile(this, "com.funguscow.fileprovider", cacheFile);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setDataAndType(uri, "image/png");
            startActivity(Intent.createChooser(shareIntent, "Share"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the background color of the base color preview to what is selected
     */
    private void updateColorPreview() {
        runOnUiThread(() -> colorView.setBackgroundColor(Utils.HSV_to_ARGB(specState.hue, specState.saturation, specState.value)));
    }

    /**
     * (re)creates the bitmap and bitmap-drawable for the currently selected size
     */
    private void recreateBitmap() {
        if (sprite != null) {
            sprite.recycle();
        }
        sprite = Bitmap.createBitmap(specState.width, specState.height, Bitmap.Config.ARGB_8888);
        spriteDrawable = new BitmapDrawable(getResources(), sprite);
        spriteDrawable.getPaint().setFilterBitmap(false);
        preview.setImageDrawable(spriteDrawable);
    }

    /**
     * Get the integer value of an EditText
     * @param text EditText view to extract from
     * @return int value contained in {@code text}, or 0
     */
    private int intOf(EditText text) {
        try {
            return Integer.parseInt(text.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get the progress out of 1 of a SeekBar
     * @param bar Bar view to poll
     * @return Progress in [0, 1]
     */
    private float valOf(SeekBar bar) {
        return (float) bar.getProgress() / bar.getMax();
    }

    /**
     * Match {@code specs} to the current inputs
     * @param specs State to update
     */
    private void updateSpecs(Specs specs) {
        specs.width = intOf(widthInput);
        if (specs.width < MINIMUM_SIZE) {
            specs.width = MINIMUM_SIZE;
            widthInput.setText(getString(R.string.integer, MINIMUM_SIZE));
        }
        specs.height = intOf(heightInput);
        if (specs.height < MINIMUM_SIZE) {
            specs.height = MINIMUM_SIZE;
            heightInput.setText(getString(R.string.integer, MINIMUM_SIZE));
        }
        specs.colors = intOf(colorsInput);
        if (specs.colors < 1) {
            specs.colors = 1;
            colorsInput.setText(getString(R.string.integer, 1));
        }
        specs.seeds = intOf(seedsInput);
        if (specs.seeds < 1) {
            specs.seeds = 1;
            seedsInput.setText(getString(R.string.integer, 1));
        }

        specs.randomSeed = randomSeed.isChecked();
        specs.seed = Long.parseLong(seedInput.getText().toString());

        specs.minProb = valOf(edgeInput);
        specs.maxProb = valOf(centerInput);
        specs.bias = valOf(biasInput);
        specs.gain = valOf(gainInput);
        specs.xMirror = valOf(xInput);
        specs.yMirror = valOf(yInput);
        specs.pMirror = valOf(pInput);
        specs.nMirror = valOf(nInput);
        specs.variance = valOf(speedInput) + 0.1f;
        specs.mutation = valOf(mutationInput);

        specs.randomColor = randomColor.isChecked();
        specs.hue = valOf(hueInput);
        specs.saturation = valOf(saturationInput);
        specs.value = valOf(valueInput);

        specs.caProbs[probSpinner.getSelectedItemPosition()] = valOf(probInput);
    }

    /**
     * Randomly set UI selections and specs state
     * @param specs state to randomize
     */
    private void randomizeSpecs(Specs specs) {
        // Doing this outside of the UI thread because we can
        for (int i = 0; i < specs.caProbs.length; i++) {
            specs.caProbs[i] = random.nextFloat();
        }

        runOnUiThread(() -> {

            colorsInput.setText(getString(R.string.integer, 1 + random.nextInt(10)));
            seedsInput.setText(getString(R.string.integer, 2 + random.nextInt(10)));

            seedInput.setText(getString(R.string.integer, random.nextLong()));

            int edge = random.nextInt(500);
            edgeInput.setProgress(edge);
            centerInput.setProgress(edge + random.nextInt(1000 - edge));
            biasInput.setProgress(random.nextInt(1000));
            gainInput.setProgress(random.nextInt(1000));
            xInput.setProgress(random.nextInt(1000));
            yInput.setProgress(random.nextInt(1000));
            pInput.setProgress(random.nextInt(1000));
            nInput.setProgress(random.nextInt(1000));
            speedInput.setProgress(random.nextInt(1000));
            mutationInput.setProgress(random.nextInt(1000));

            hueInput.setProgress(random.nextInt(256));
            saturationInput.setProgress(random.nextInt(256));
            valueInput.setProgress(random.nextInt(256));

            probInput.setProgress(random.nextInt(1000));

            updateSpecs(specs);
        });
    }

    /**
     * Common operations to perform before generation, like randomization and resizing
     */
    private void preGenerate() {
        if (specState.randomSeed) {
            long seed = random.nextLong();
            runOnUiThread(() -> seedInput.setText(getString(R.string.integer, seed)));
        }

        if (specState.randomColor) {
            runOnUiThread(() -> {
                hueInput.setProgress(random.nextInt(256));
                saturationInput.setProgress(random.nextInt(256));
                valueInput.setProgress(random.nextInt(256));
            });
        }

        updateSpecs(specState);
        updateColorPreview();

        if (specState.width != sprite.getWidth() || specState.height != sprite.getHeight()) {
            recreateBitmap();
        }
    }

    /**
     * Generate an image to match the specs
     */
    private void generate() {
        preGenerate();

        SpriteGrid grid = new SpriteGrid(specState);
        grid.drawTo(sprite);

    }

    /**
     * Prompt the user for a number of images to save in a batch. If confirmed and non-zero, save them
     */
    private void promptSaveBatch() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Dialog));
        final EditText numberInput = new EditText(this);
        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        numberInput.setHint(R.string.batch_hint);
        numberInput.setText("1");
        builder.setView(numberInput);
        builder.setTitle(R.string.batch_title);
        builder.setNegativeButton("Cancel", (dialog, which) -> {

        });
        builder.setPositiveButton("Save", (dialog, which) -> {
            int size;
            try {
                size = Integer.parseInt(numberInput.getText().toString());
                size = Math.max(size, 1);
                saveBatch(size);
            } catch (Exception e) {
                runOnUiThread(() ->
                Toast.makeText(MainActivity.this, "Enter an integer", Toast.LENGTH_LONG).show());
            }
        });
        builder.show();
    }

    /**
     * Save a sequence of images
     * @param size Number of images in sequence
     */
    private void saveBatch(int size) {
        preGenerate();

        SpriteGrid grid = new SpriteGrid(specState);
        String base = "PixelArtBatch_" + System.currentTimeMillis() + "_";
        int successful = 0;
        for (int i = 0; i < size; i++) {
            grid.drawTo(sprite);
            if (Utils.saveBitmap(sprite, this, "PixelSprites", base + i, false)) {
                successful++;
            }
        }
        final int worked = successful;
        runOnUiThread(() ->
                Toast.makeText(this, "Saved " + worked + " out of " + size + " images!", Toast.LENGTH_LONG).show());
    }
}