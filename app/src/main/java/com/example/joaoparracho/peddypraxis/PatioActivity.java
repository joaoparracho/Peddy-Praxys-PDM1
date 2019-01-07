// Copyright 2018 Google LLC
package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.joaoparracho.peddypraxis.common.CameraSource;
import com.example.joaoparracho.peddypraxis.common.CameraSourcePreview;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.example.joaoparracho.peddypraxis.facedetection.FaceDetectionProcessor;
import com.example.joaoparracho.peddypraxis.model.CountDownTimer2;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Demo app showing the various features of ML Kit for Firebase. This class is used to
 * set up continuous frame processing on frames from a camera source. */
@KeepName
public final class PatioActivity extends AppCompatActivity
        implements OnRequestPermissionsResultCallback,
        OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String FACE_CONTOUR = "Face Contour";
    private static final String TAG = "LivePreviewActivity";
    private static final int PERMISSION_REQUESTS = 1;
    private static final String FEEDBACK = "FEEDBACK";

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private String selectedModel = FACE_CONTOUR;
    private TextView mTextViewCountDown;

    public static final String TAG_SNAPSHOT = "snapshot";
    private static final int REQUEST_CODE_FLPERMISSION = 42;
    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private boolean fenceBool;
    public static final String TAG_FENCES = "fences";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";


    private long mTimeInMillis = 60 * 1000;
    CountDownTimer2 m1;
    private boolean pauseCounterOnce;
    private int counterDelay;

    public static Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_patio);

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

        mTextViewCountDown = (TextView) findViewById(R.id.tVInfo);
        m1 = new CountDownTimer2(mTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                if (Singleton.getInstance().getFd() && fenceBool) {
                    if (m1.ismPaused()) {
                        m1.resume();
                    }
                    mTimeInMillis = millisUntilFinished;
                    if (counterDelay >= 3) {
                        counterDelay = 0;
                        pauseCounterOnce = false;
                        Singleton.getInstance().setFd(false);
                    } else {
                        counterDelay++;
                    }
                } else if (!pauseCounterOnce) {
                    m1.pause();
                    pauseCounterOnce = true;
                    counterDelay = 0;
                }
                mTextViewCountDown.setText(updateCountDownText());
            }

            @Override
            public void onFinish() {
                mTextViewCountDown.setText("Finish");
                startActivity(new Intent(PatioActivity.this, EdificioActivity.class));
            }
        }.start();

        preview = (CameraSourcePreview) findViewById(R.id.firePreview);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = (GraphicOverlay) findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        } else {
            getRuntimePermissions();
        }

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
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;


        if (hours > 0) {
            if (Singleton.getInstance().getFd()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d---TRUE %d", hours, minutes, seconds, counterDelay);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d--FALSE %d", hours, minutes, seconds, counterDelay);
            }
        } else {
            if (Singleton.getInstance().getFd()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d---TRUE %d", minutes, seconds, counterDelay);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d--FALSE %d", minutes, seconds, counterDelay);
            }
        }

        return timeLeftFormatted;
    }

    @Override
    public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent.getItemAtPosition(pos).toString();
        Log.d(TAG, "Selected model: " + selectedModel);
        preview.stop();
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
            startCameraSource();
        } else {
            getRuntimePermissions();
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraSource != null) {
            if (isChecked) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource();
    }
    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }
        Log.i(TAG, "Using Face Detector Processor");
        cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor());
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupFences();
        Log.d(TAG, "onResume");
        startCameraSource();
    }

    /** Stops the camera. */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    @Override
    public void onStop() {
        if (fenceReceiver != null) {
            unregisterReceiver(fenceReceiver);
            fenceReceiver = null;
        }
        super.onStop();
    }


    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    private void setupFences() {
        AwarenessFence walkingFence = AwarenessFence.or(
                DetectedActivityFence.during(DetectedActivityFence.ON_FOOT),
                DetectedActivityFence.during(DetectedActivityFence.WALKING),
                DetectedActivityFence.during(DetectedActivityFence.RUNNING));



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
       // AwarenessFence inLocationFence = LocationFence.in(39.735243, -8.8207012, 5, 1);
        //AwarenessFence walkingInPatioFence = AwarenessFence.and(inLocationFence,walkingFence);

        // Now that we have an interesting, complex condition, register the fence to receive
        // callbacks.
        // Register the fence to receive callbacks.
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence("locationFence", walkingFence, myPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Fence was successfully registered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Fence could not be registered: " + e);
                    }
                });
    }
    private class FenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(FENCE_RECEIVER_ACTION)) {
                Log.e(TAG_FENCES, "Received an unsupported action in FenceReceiver: action="
                        + intent.getAction());
                return;
            }
            FenceState fenceState = FenceState.extract(intent);
            switch (fenceState.getFenceKey()) {
                case "locationFence":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceBool = true;
                            Snackbar.make(findViewById(android.R.id.content), "HEYY", Snackbar.LENGTH_LONG).show();
                            break;
                        case FenceState.FALSE:
                        case FenceState.UNKNOWN:
                            fenceBool = false;
                            break;
                    }
                    break;
            }

        }
    }
}
