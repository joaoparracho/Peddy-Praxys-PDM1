package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.cloudtextrecognition.CloudTextRecognitionProcessor;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.example.joaoparracho.peddypraxis.common.VisionImageProcessor;
import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.sql.Timestamp;

public class BibliotecaActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
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

    private GraphicOverlay graphicOverlay;
    private Uri imageUri;
    // Max width (portrait mode)
    private Integer imageMaxWidth;
    // Max height (portrait mode)
    private Integer imageMaxHeight;
    private Bitmap bitmapForDetection;
    private VisionImageProcessor imageProcessor;
    private String selectedSize = SIZE_1024_768;

    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biblioteca);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        getImageButton = (Button) findViewById(R.id.getImageButton);
        getImageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        queryFences();
                        if(Singleton.getInstance().isbLibLoc()){
                            startCameraIntentForResult();
                       }
                        else{
                            Snackbar.make(findViewById(android.R.id.content), "You must be inside library dummie", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
        createImageProcessor();
        preview = (ImageView) findViewById(R.id.previewPane);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = (GraphicOverlay) findViewById(R.id.previewOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        // TODO: Communicate with the UI thread
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String feedback = msg.getData().getString("FEEDBACK");
                if (feedback != null) {
                    Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
                    if (Singleton.getInstance().isShowFinishBtn()) {
                        startActivity(new Intent(BibliotecaActivity.this, DescompressaoActivity.class));
                    }
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
        setupFences();
        showDescription();
    }

    public void onCLickShowPreamb(MenuItem item) {showDescription();}
    public void showDescription(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("A Biblioteca");
        alert.setMessage("A Biblioteca José Saramago é um espaço de Cultura, Conhecimento e ...? " +
                "Para concluir esta tarefa deve de retirar uma foto da resposta que se encontra algures na Biblioteca");
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.create().show();
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        return true;
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_IMAGE_URI, imageUri);
        if (imageMaxWidth != null) {
            outState.putInt(KEY_IMAGE_MAX_WIDTH, imageMaxWidth);
        }
        if (imageMaxHeight != null) {
            outState.putInt(KEY_IMAGE_MAX_HEIGHT, imageMaxHeight);
        }
        outState.putString(KEY_SELECTED_SIZE, selectedSize);
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
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            tryReloadAndDetectInImage();
        }
    }
    private void tryReloadAndDetectInImage() {
        try {
            if (imageUri == null) {
                return;
            }
            // Clear the overlay first
            graphicOverlay.clear();

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

            imageProcessor.process(bitmapForDetection, graphicOverlay);
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving saved image");
        }
    }
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

    private void setupFences() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling.
            return;
        }
        AwarenessFence libraryLocationFence = LocationFence.in(39.7331653,-8.8204611, 30, 0L);
        addFence("libLocationFenceKey", libraryLocationFence);
    }
    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(findViewById(android.R.id.content), "Sucess to add Fence", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(findViewById(android.R.id.content), "Failed to add Fence", Snackbar.LENGTH_LONG).show();
                    }
                });
    }
    protected void queryFences() {
        Awareness.getFenceClient(this).queryFences(FenceQueryRequest.all())
                .addOnSuccessListener(new OnSuccessListener<FenceQueryResponse>() {
                    @Override
                    public void onSuccess(FenceQueryResponse fenceQueryResponse) {
                        String fenceInfo = "";
                        FenceStateMap fenceStateMap = fenceQueryResponse.getFenceStateMap();
                        for (String fenceKey : fenceStateMap.getFenceKeys()) {
                            int state = fenceStateMap.getFenceState(fenceKey).getCurrentState();
                            fenceInfo += fenceKey + ": "
                                    + (state == FenceState.TRUE ? "TRUE" :
                                    state == FenceState.FALSE ? "FALSE" : "UNKNOWN") + "\n";
                            if(fenceKey.equals("libLocationFenceKey") &&state == FenceState.TRUE)
                                Singleton.getInstance().setbLibLoc(true);
                        }
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "> Fences' states:\n" + (fenceInfo.equals("") ?
                                "No registered fences." : fenceInfo);
                        Log.d("xxxfences" , text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be queried: " + e.getMessage();
                        Log.d("xxxfences" , text);
                    }
                });
    }
    protected void removeFences(String unique_key) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(unique_key)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences were successfully removed.";
                        Log.d("xxxfences" , text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be removed: " + e.getMessage();
                        Log.d("xxxfences" , text);
                    }
                });
    }

    @Override public void onResume() {
        super.onResume();
        queryFences();
        //setupFences();
        Log.d(TAG, "onResume");
    }
    @Override protected void onPause() {
        super.onPause();
        queryFences();
    }
    @Override public void onDestroy() {
        super.onDestroy();
        removeFences("libLocationFenceKey");
    }
    @Override public void onStop() {
        queryFences();
        super.onStop();
    }
    @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }
}
