package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.common.CameraSource;
import com.example.joaoparracho.peddypraxis.common.CameraSourcePreview;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.example.joaoparracho.peddypraxis.textrecognition.TextRecognitionProcessor;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EdificioActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "EdificioActivity";
    private static final int PERMISSION_REQUESTS = 1;
    private static final String FEEDBACK = "FEEDBACK";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    public static Handler mHandler;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private TextView textViewEdificio;
    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private String edificios = "Faltam os edifícios";
    private String tempString = " ";
    private boolean completa = true;

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) Log.i(TAG, "Permission granted: " + permission);
        else Log.i(TAG, "Permission NOT granted: " + permission);
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_edificio);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        textViewEdificio = findViewById(R.id.tVInfo);
        tempString = " ";
        for (int i = 0; i < 5; i++) if (Singleton.getInstance().getFaltaEdificios(i)) tempString += ((char) (65 + i)) + " ";
        textViewEdificio.setText(edificios + tempString);

        preview = findViewById(R.id.firePreview);
        if (preview == null) Log.d(TAG, "Preview is null");

        graphicOverlay = findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) Log.d(TAG, "graphicOverlay is null");

        if (allPermissionsGranted()) createCameraSource();
        else getRuntimePermissions();

        // TODO: Communicate with the UI thread
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String feedback = msg.getData().getString(FEEDBACK);
                if (feedback != null) {
                    Log.d(TAG, "Found " + feedback);
                    Snackbar.make(findViewById(android.R.id.content), "Encontrou o ediífio " + feedback + "!", Snackbar.LENGTH_SHORT).show();
                    Singleton.getInstance().setFaltaEdificios(feedback.charAt(0) - 65, false);
                    tempString = " ";
                    for (int i = 0; i < 5; i++) if (Singleton.getInstance().getFaltaEdificios(i)) { tempString += ((char) (65 + i)) + " "; completa = completa & !Singleton.getInstance().getFaltaEdificios(i); }
                    if (completa) {
                        textViewEdificio.setText("Finish");
                        Singleton.getInstance().setActivityKey("bibliotecaKey");
                        Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete() + 1);
                        startActivity(new Intent(EdificioActivity.this, PreambuloActivity.class));
                        finish();
                    } else completa = true;
                    textViewEdificio.setText(edificios + tempString);
                }
            }
        };
    }

    public void onClickActivity(View view) {
        tempString = " ";
        for (int i = 0; i < 5; i++) {
            Singleton.getInstance().setFaltaEdificios(i, true);
            tempString += ((char) (65 + i)) + " ";
        }
        textViewEdificio.setText(edificios + tempString);
    }

    public void onCLickShowPreamb(MenuItem item) {
        showDescription();
    }

    public void showDescription() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.edificios)
                .setMessage("O caloiro tem ir junto de todos os edifícios e confirmar que se encontra perto deles ao mostrar com a câmara \"Edifício X\"\n")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { /*checkDescr = true;*/ }
                })
                .create().show();
    }


    private void createCameraSource() {
        if (cameraSource == null) cameraSource = new CameraSource(this, graphicOverlay);
        cameraSource.setMachineLearningFrameProcessor(new TextRecognitionProcessor());
    }

    private void startCameraSource() {
        if (cameraSource != null)
            try {
                if (preview == null) Log.d(TAG, "resume: Preview is null");
                if (graphicOverlay == null) Log.d(TAG, "resume: graphOverlay is null");
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) return ps;
            else return new String[0];
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) if (!isPermissionGranted(this, permission)) return false;
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) if (!isPermissionGranted(this, permission)) allNeededPermissions.add(permission);
        if (!allNeededPermissions.isEmpty()) ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) createCameraSource();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    private void setupFences() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        addFence("edA", LocationFence.in(39.73565192930836, -8.820967774868791, 65, 0L));
    }

    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Fences add success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Fences add fail");
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
//        setupFences();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (cameraSource != null) cameraSource.release();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (fenceReceiver != null) {
            unregisterReceiver(fenceReceiver);
            fenceReceiver = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        return true;
    }

}
