package com.funguscow.pixelart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.funguscow.pixelart.data.Specs;
import com.funguscow.pixelart.data.SpriteGrid;
import com.funguscow.pixelart.interfaces.SeekBarProgressListener;
import com.funguscow.pixelart.interfaces.SeekBarStopListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

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

    private Button randomAllButton, refreshButton, saveButton, shareButton, randomSeedButton;

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

        sprite = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);

        random = new Random();
        long seed = random.nextLong();
        specState.seed = seed;
        seedInput.setText(seed + "");
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
                runOnUiThread(() -> {
                    probInput.setProgress((int) (1000 * specState.caProbs[position]));
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        probInput.setOnSeekBarChangeListener((SeekBarStopListener) (bar) -> {
            specState.caProbs[probSpinner.getSelectedItemPosition()] = valOf(bar);
            generate();
        });

        hueInput.setOnSeekBarChangeListener((SeekBarProgressListener) (bar, progress, user) -> {
            specState.hue = (float) progress / bar.getMax();
            updateColorPreview();
        });
        saturationInput.setOnSeekBarChangeListener((SeekBarProgressListener) (bar, progress, user) -> {
            specState.saturation = (float) progress / bar.getMax();
            updateColorPreview();
        });
        valueInput.setOnSeekBarChangeListener((SeekBarProgressListener) (bar, progress, user) -> {
            specState.value = (float) progress / bar.getMax();
            updateColorPreview();
        });

        randomSeed.setOnCheckedChangeListener((view, checked) -> {
            specState.randomSeed = checked;
            runOnUiThread(() -> seedInput.setEnabled(!checked));
        });
        randomColor.setOnCheckedChangeListener((view, checked) -> {
            specState.randomColor = checked;
        });

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
                runOnUiThread(() -> {
                    seedInput.setText(random.nextLong() + "");
                });
            }
            generate();
        });

        randomAllButton.setOnClickListener((button) -> {
            randomizeSpecs(specState);
            generate();
        });

        shareButton.setOnClickListener((button) -> {
            share();
        });

        refreshButton.setOnClickListener((button) -> {
            generate();
        });

        saveButton.setOnClickListener((button) -> {
            Utils.saveBitmap(sprite, this, "com.funguscow.pixelart");
        });

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
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
            dialog.setOnShowListener((di) -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimaryDark));
            });
            dialog.show();
            return true;
        }
        else if(item.getItemId() == R.id.source) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cppietime/PixelArt"));
            startActivity(browserIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void share() {
        try {
            File imagesFolder = new File(getCacheDir(), "images");
            imagesFolder.mkdirs();
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

    private void updateColorPreview() {
        runOnUiThread(() -> colorView.setBackgroundColor(Utils.HSV_to_ARGB(specState.hue, specState.saturation, specState.value)));
    }

    private void recreateBitmap() {
        if (sprite != null) {
            sprite.recycle();
        }
        sprite = Bitmap.createBitmap(specState.width, specState.height, Bitmap.Config.ARGB_8888);
        spriteDrawable = new BitmapDrawable(getResources(), sprite);
        spriteDrawable.getPaint().setFilterBitmap(false);
        preview.setImageDrawable(spriteDrawable);
    }

    private int intOf(EditText text) {
        try {
            return Integer.parseInt(text.getText().toString());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private float valOf(SeekBar bar) {
        return (float) bar.getProgress() / bar.getMax();
    }

    private void updateSpecs(Specs specs) {
        specs.width = intOf(widthInput);
        specs.height = intOf(heightInput);
        specs.colors = intOf(colorsInput);
        specs.seeds = intOf(seedsInput);

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

    private void randomizeSpecs(Specs specs) {
        // Doing this outside of the UI thread because we can
        for (int i = 0; i < specs.caProbs.length; i++) {
            specs.caProbs[i] = random.nextFloat();
        }

        runOnUiThread(() -> {

            colorsInput.setText(1 + random.nextInt(10) + "");
            seedsInput.setText(2 + random.nextInt(10) + "");

            seedInput.setText(random.nextLong() + "");

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

    private void generate() {
        if (specState.randomSeed) {
            long seed = random.nextLong();
            runOnUiThread(() -> seedInput.setText(seed + ""));
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

        SpriteGrid grid = new SpriteGrid(specState);
        grid.drawTo(sprite);

    }
}