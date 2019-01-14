// Copyright 2018 Google LLC
package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
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
import com.example.joaoparracho.peddypraxis.facedetection.FaceDetectionProcessor;
import com.example.joaoparracho.peddypraxis.model.CountDownTimer2;
import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.snapshot.LocationResponse;
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

/**
 * Demo app showing the various features of ML Kit for Firebase. This class is used to
 * set up continuous frame processing on frames from a camera source.
 */
@KeepName
public final class PatioActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "PatioActivity";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    public static Handler mHandler;
    CountDownTimer2 m1;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private TextView mTextViewCountDown;
    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private long mTimeInMillis = 60 * 100;
    private boolean pauseCounterOnce;
    private int counterDelay;
    private String text2 = "";
    private boolean checkWarning;

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) Log.i(TAG, "Permission granted: " + permission);
        else Log.i(TAG, "Permission NOT granted: " + permission);
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_patio);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        mTextViewCountDown = findViewById(R.id.tVInfo);
        m1 = new CountDownTimer2(mTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                if (Singleton.getInstance().getFd() && Singleton.getInstance().isFenceBool() && Singleton.getInstance().isWalkingBool() && !checkWarning) {
                    if (m1.ismPaused()) m1.resume();
                    mTimeInMillis = millisUntilFinished;
                    if (counterDelay >= 3) {
                        counterDelay = 0;
                        pauseCounterOnce = false;
                        Singleton.getInstance().setFd(false);
                    } else counterDelay++;
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
                //Singleton.getInstance().setActivityKey("edificiosKey");
                Singleton.getInstance().setActivityKey("bibliotecaKey");
                Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete() + 1);
                finish();
                startActivity(new Intent(PatioActivity.this, PreambuloActivity.class));

            }
        }.start();

        preview = findViewById(R.id.firePreview);
        if (preview == null) Log.d(TAG, "Preview is null");
        graphicOverlay = findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) Log.d(TAG, "graphicOverlay is null");

        if (allPermissionsGranted()) createCameraSource();
        else getRuntimePermissions();

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
        queryFences();
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;
        int a, b;

        a = Singleton.getInstance().isFenceBool() ? 1 : 0;
        b = Singleton.getInstance().isWalkingBool() ? 1 : 0;

        if (hours > 0) {
            if (Singleton.getInstance().getFd()) timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d-TRUE %d %d %d", hours, minutes, seconds, counterDelay, a, b);
            else timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d--FALSE %d %d %d", hours, minutes, seconds, counterDelay, a, b);
        } else {
            if (Singleton.getInstance().getFd()) timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d---TRUE %d %d %d", minutes, seconds, counterDelay, a, b);
            else timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d--FALSE %d %d %d", minutes, seconds, counterDelay, a, b);
        }
        return timeLeftFormatted;
    }

    public void onClickActivity(View view) {
        //printLocation();
        queryFences();
        new AlertDialog.Builder(PatioActivity.this)
                .setTitle("Fences")
                .setMessage(text2)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    public void onCLickShowPreamb(MenuItem item) {showDescription(); }

    public void showDescription() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.patio)
                .setMessage(R.string.descPat)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    private void createCameraSource() {
        if (cameraSource == null) cameraSource = new CameraSource(this, graphicOverlay);
        cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor());
    }

    private void startCameraSource() {
        if (cameraSource != null)
            try {
                if (preview == null) Log.d(TAG, "resume: Preview is null");
                if (graphicOverlay == null) Log.d(TAG, "resume: graphOverlay is null");
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
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
        if (!allNeededPermissions.isEmpty()) ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) createCameraSource();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
                            if (fenceKey.equals("locationFenceKey") && state == FenceState.TRUE) Singleton.getInstance().setFenceBool(true);
                            if (fenceKey.equals("walkingFenceKey") && state != FenceState.FALSE) Singleton.getInstance().setWalkingBool(true);
                        }
                        text2 = "location: " + fenceInfo;
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

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back button pressed");
        checkWarning = true;
        if (!m1.ismPaused()){
            m1.pause();
            pauseCounterOnce = true;
        }
        showDialogWarning();
    }

    public void showDialogWarning() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.endTask)
                .setMessage(R.string.warnLst)
                .setPositiveButton(R.string.endTask, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkWarning = false;
                        m1.cancel();
                        Singleton.getInstance().setFd(false);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkWarning = false;
                    }
                })
                .create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource();
        queryFences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        queryFences();
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
