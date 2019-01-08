package com.example.joaoparracho.peddypraxis;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.example.joaoparracho.peddypraxis.cloudtextrecognition.CloudTextRecognitionProcessor;
import com.example.joaoparracho.peddypraxis.common.VisionImageProcessor;

import java.io.IOException;

public class BibliotecaActivity extends AppCompatActivity {
    private static final String TAG = "BibliotecaActivity";
    private static final String CLOUD_TEXT_DETECTION = "Cloud Text";

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_CHOOSE_IMAGE = 1002;

    private static final String KEY_IMAGE_URI = "com.googletest.firebase.ml.demo.KEY_IMAGE_URI";
    private static final String KEY_IMAGE_MAX_WIDTH =
            "com.googletest.firebase.ml.demo.KEY_IMAGE_MAX_WIDTH";
    private static final String KEY_IMAGE_MAX_HEIGHT =
            "com.googletest.firebase.ml.demo.KEY_IMAGE_MAX_HEIGHT";
    private static final String KEY_SELECTED_SIZE =
            "com.googletest.firebase.ml.demo.KEY_SELECTED_SIZE";
    private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio

    private String selectedMode = CLOUD_TEXT_DETECTION;

    public static Handler mHandler;
    private static final String FEEDBACK = "FEEDBACK";
    private Button getImageButton;
    boolean isLandScape;
    private ImageView preview;

    private Uri imageUri;
    // Max width (portrait mode)
    private Integer imageMaxWidth;
    // Max height (portrait mode)
    private Integer imageMaxHeight;
    private Bitmap bitmapForDetection;
    private VisionImageProcessor imageProcessor;
    private String selectedSize = SIZE_1024_768;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamescreen);

        getImageButton = (Button) findViewById(R.id.getImageButton);
        getImageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Menu for selecting either: a) take new photo b) select from existing
                        PopupMenu popup = new PopupMenu(BibliotecaActivity.this, view);
                        popup.setOnMenuItemClickListener(
                                new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem menuItem) {
                                        switch (menuItem.getItemId()) {
                                            case R.id.select_images_from_local:
                                                startChooseImageIntentForResult();
                                                return true;
                                            case R.id.take_photo_using_camera:
                                                startCameraIntentForResult();
                                                return true;
                                            default:
                                                return false;
                                        }
                                    }
                                });

                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.camera_button_menu, popup.getMenu());
                        popup.show();
                    }
                });

        createImageProcessor();

        // TODO: Communicate with the UI thread
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String feedback = msg.getData().getString("FEEDBACK");
                if (feedback != null) {
                    Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
                }
            }
        };

        isLandScape =
                (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
            imageMaxWidth = savedInstanceState.getInt(KEY_IMAGE_MAX_WIDTH);
            imageMaxHeight = savedInstanceState.getInt(KEY_IMAGE_MAX_HEIGHT);
            selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);

            if (imageUri != null) {
                tryReloadAndDetectInImage();
            }
        }
    }
    private void startCameraIntentForResult() {
        // Clean up last time's image
        imageUri = null;
        preview.setImageBitmap(null);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void startChooseImageIntentForResult() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            tryReloadAndDetectInImage();
        } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data.getData();
            tryReloadAndDetectInImage();
        }
    }

    private void tryReloadAndDetectInImage() {
        try {
            if (imageUri == null) {
                return;
            }

            // Clear the overlay first
           // graphicOverlay.clear();

            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Get the dimensions of the View
            Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

            int targetWidth = targetedSize.first;
            int maxHeight = targetedSize.second;

            // Determine how much to scale down the image
            float scaleFactor =
                    Math.max(
                            (float) imageBitmap.getWidth() / (float) targetWidth,
                            (float) imageBitmap.getHeight() / (float) maxHeight);

            Bitmap resizedBitmap =
                    Bitmap.createScaledBitmap(
                            imageBitmap,
                            (int) (imageBitmap.getWidth() / scaleFactor),
                            (int) (imageBitmap.getHeight() / scaleFactor),
                            true);

            preview.setImageBitmap(resizedBitmap);
            bitmapForDetection = resizedBitmap;

            //imageProcessor.process(bitmapForDetection, graphicOverlay);
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving saved image");
        }
    }

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
  /*  private Integer getImageMaxWidth() {
        if (imageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to wait for
            // a UI layout pass to get the right values. So delay it to first time image rendering time.
            if (isLandScape) {
                imageMaxWidth =
                        ((View) preview.getParent()).getHeight() - findViewById(R.id.controlPanel).getHeight();
            } else {
                imageMaxWidth = ((View) preview.getParent()).getWidth();
            }
        }

        return imageMaxWidth;
    }*/

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
  /*  private Integer getImageMaxHeight() {
        if (imageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to wait for
            // a UI layout pass to get the right values. So delay it to first time image rendering time.
            if (isLandScape) {
                imageMaxHeight = ((View) preview.getParent()).getWidth();
            } else {
                imageMaxHeight =
                        ((View) preview.getParent()).getHeight() - findViewById(R.id.controlPanel).getHeight();
            }
        }

        return imageMaxHeight;
    }*/

    // Gets the targeted width / height.
    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;

        switch (selectedSize) {
            case SIZE_1024_768:
                targetWidth = isLandScape ? 1024 : 768;
                targetHeight = isLandScape ? 768 : 1024;
                break;
            default:
                throw new IllegalStateException("Unknown size");
        }

        return new Pair<>(targetWidth, targetHeight);
    }

    private void createImageProcessor() {
        switch (selectedMode) {
            case CLOUD_TEXT_DETECTION:
                imageProcessor = new CloudTextRecognitionProcessor();
                break;
            default:
                throw new IllegalStateException("Unknown selectedMode: " + selectedMode);
        }
    }
}
