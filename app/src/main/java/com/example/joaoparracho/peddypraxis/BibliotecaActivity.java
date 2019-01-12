package com.example.joaoparracho.peddypraxis;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.cloudtextrecognition.CloudTextRecognitionProcessor;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.example.joaoparracho.peddypraxis.common.VisionImageProcessor;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.sql.Timestamp;

public class BibliotecaActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "BibliotecaActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final String KEY_IMAGE_URI = "com.googletest.firebase.ml.demo.KEY_IMAGE_URI";
    private static final String KEY_IMAGE_MAX_WIDTH = "com.googletest.firebase.ml.demo.KEY_IMAGE_MAX_WIDTH";
    private static final String KEY_IMAGE_MAX_HEIGHT = "com.googletest.firebase.ml.demo.KEY_IMAGE_MAX_HEIGHT";
    private static final String KEY_SELECTED_SIZE = "com.googletest.firebase.ml.demo.KEY_SELECTED_SIZE";
    private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
    public static Handler mHandler;
    boolean isLandScape;
    private Button getImageButton;
    private ImageView preview;
    private GraphicOverlay graphicOverlay;
    private Uri imageUri;
    // Max width (portrait mode)
    private Integer imageMaxWidth;
    // Max height (portrait mode)
    private Integer imageMaxHeight;
    private VisionImageProcessor imageProcessor;
    private String selectedSize = SIZE_1024_768;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_biblioteca);

        getImageButton = findViewById(R.id.getImageButton);
        getImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queryFences();
                if (Singleton.getInstance().isbLibLoc()) startCameraIntentForResult();
                else Snackbar.make(findViewById(android.R.id.content), "You must be inside library dummie", Snackbar.LENGTH_LONG).show();
            }
        });
        imageProcessor = new CloudTextRecognitionProcessor();
        preview = findViewById(R.id.previewPane);
        if (preview == null) Log.d(TAG, "Preview is null");
        graphicOverlay = findViewById(R.id.previewOverlay);
        if (graphicOverlay == null) Log.d(TAG, "graphicOverlay is null");

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String feedback = msg.getData().getString("FEEDBACK");
                if (feedback != null) {
                    Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
                    // TODO: alterar o nome desta variavel. Nao esta explicita inicialmente era usar para mudar a visibilidade de um botao
                    if (Singleton.getInstance().isShowFinishBtn()) {
                        Log.d(TAG, "2 " + Singleton.getInstance().getNumTasksComplete());
                        Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete() + 1);
                        Singleton.getInstance().setActivityKey("descompressaoKey");
                        finish();
                        startActivity(new Intent(BibliotecaActivity.this, PreambuloActivity.class));

                    }
                }
            }
        };
        isLandScape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
            imageMaxWidth = savedInstanceState.getInt(KEY_IMAGE_MAX_WIDTH);
            imageMaxHeight = savedInstanceState.getInt(KEY_IMAGE_MAX_HEIGHT);
            selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);
            if (imageUri != null) tryReloadAndDetectInImage();
        }
    }

    public void onCLickShowPreamb(MenuItem item) {
        showDescription();
    }

    public void showDescription() {
        new AlertDialog.Builder(this)
                .setTitle("A Biblioteca")
                .setMessage("A Biblioteca José Saramago é um espaço de Cultura, Conhecimento e ...? Para concluir esta tarefa deve de retirar uma foto da resposta que se encontra algures na Biblioteca")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("numTask", Integer.toString(Singleton.getInstance().getNumTasksComplete()));
        outState.putParcelable(KEY_IMAGE_URI, imageUri);
        if (imageMaxWidth != null) outState.putInt(KEY_IMAGE_MAX_WIDTH, imageMaxWidth);
        if (imageMaxHeight != null) outState.putInt(KEY_IMAGE_MAX_HEIGHT, imageMaxHeight);
        outState.putString(KEY_SELECTED_SIZE, selectedSize);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) if (savedInstanceState.containsKey("numTask")) Singleton.getInstance().setNumTasksComplete(Integer.parseInt(savedInstanceState.getString("numTask")));
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void startCameraIntentForResult() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) tryReloadAndDetectInImage();
    }

    private void tryReloadAndDetectInImage() {
        try {
            if (imageUri == null) return;
            graphicOverlay.clear();
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();
            float scaleFactor = Math.max((float) imageBitmap.getWidth() / (float) targetedSize.first, (float) imageBitmap.getHeight() / (float) targetedSize.second);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, (int) (imageBitmap.getWidth() / scaleFactor), (int) (imageBitmap.getHeight() / scaleFactor), true);
            preview.setImageBitmap(resizedBitmap);
            imageProcessor.process(resizedBitmap, graphicOverlay);
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving saved image");
        }
    }

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

    protected void queryFences() {
        Awareness.getFenceClient(this).queryFences(FenceQueryRequest.all())
                .addOnSuccessListener(new OnSuccessListener<FenceQueryResponse>() {
                    @Override
                    public void onSuccess(FenceQueryResponse fenceQueryResponse) {
                        String fenceInfo = "";
                        FenceStateMap fenceStateMap = fenceQueryResponse.getFenceStateMap();
                        for (String fenceKey : fenceStateMap.getFenceKeys()) {
                            int state = fenceStateMap.getFenceState(fenceKey).getCurrentState();
                            fenceInfo += fenceKey + ": " + (state == FenceState.TRUE ? "TRUE" : state == FenceState.FALSE ? "FALSE" : "UNKNOWN") + "\n";
                            if (fenceKey.equals("libLocationFenceKey") && state == FenceState.TRUE) Singleton.getInstance().setbLibLoc(true);
                        }
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\n> Fences' states:\n" + (fenceInfo.equals("") ? "No registered fences." : fenceInfo));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\nFences could not be queried: " + e.getMessage());
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
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\nFences were successfully removed.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\nFences could not be removed: " + e.getMessage());
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back button pressed");
        showDialogWaring();
    }

    public void showDialogWaring() {
        new AlertDialog.Builder(this)
                .setTitle("Sair Tarefa")
                .setMessage("Caloiro tem a certeza que pretende sair!\n Qualquer progresso que tenha feito ira ser perdido")
                .setPositiveButton("Terminar Tarefa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        queryFences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        queryFences();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        removeFences("libLocationFenceKey");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        queryFences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }
}
