// Copyright 2018 Google LLC
package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.joaoparracho.peddypraxis.common.CameraSource;
import com.example.joaoparracho.peddypraxis.common.CameraSourcePreview;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.example.joaoparracho.peddypraxis.facedetection.FaceDetectionProcessor;
import com.example.joaoparracho.peddypraxis.model.CountDownTimer2;
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
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
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

    public static final String TAG_FENCES = "fences";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";

    private long mTimeInMillis = 60 * 100;
    CountDownTimer2 m1;
    private boolean pauseCounterOnce;
    private int counterDelay;

    private String text2 = "";
    private String stringKey = "";
    public static Handler mHandler;
    private boolean checkWaring;

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
                if (Singleton.getInstance().getFd()
                        && Singleton.getInstance().isFenceBool()
                        && !Singleton.getInstance().isNotWalkinBool()
                        && !checkWaring) {
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
                Singleton.getInstance().setActivityKey("bibliotecaKey");
                Singleton.getInstance().setNumTasksComplete( Singleton.getInstance().getNumTasksComplete()+1);
                finish();
                startActivity(new Intent(PatioActivity.this, PreambuloActivity.class));

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
                String feedback = msg.getData().getString("100");
                if (feedback != null) {
                    Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
                    Intent i = new Intent(PatioActivity.this, GameScreenActivity.class);
                    startActivity(i);
                }
                feedback= msg.getData().getString("90");
                if (feedback != null) {
                    Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
                }
                feedback= msg.getData().getString("50");
                if (feedback != null) {
                    Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
                }
            }
        };
        //showDescription();
        //setupFences();
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;
        int a, b;

        if (Singleton.getInstance().isFenceBool()) {
            a = 1;
        } else {
            a = 0;
        }
        if (Singleton.getInstance().isNotWalkinBool()) {
            b = 0;
        } else {
            b = 1;
        }

        if (hours > 0) {
            if (Singleton.getInstance().getFd()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d-TRUIE %d %d %d", hours, minutes, seconds, counterDelay, a, b);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d--FAILSE %d %d %d", hours, minutes, seconds, counterDelay, a, b);
            }
        } else {
            if (Singleton.getInstance().getFd()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d---TRUIE %d %d %d", minutes, seconds, counterDelay, a, b);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d--FAILSE %d %d %d", minutes, seconds, counterDelay, a, b);
            }
        }
        return timeLeftFormatted;
    }

    public void onClickActivity(View view) {
        //printLocation();
        queryFences();
        AlertDialog.Builder alert = new AlertDialog.Builder(PatioActivity.this);
        alert.setTitle("Fences");
        alert.setMessage(text2);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.create().show();
        // Snackbar.make(findViewById(android.R.id.content), text2, Snackbar.LENGTH_LONG).show();
    }
    public void onCLickShowPreamb(MenuItem item) {
        showDescription();
    }
    public void showDescription() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("O Pátio");
        alert.setMessage(" O caloiro tem de andar, durante 5 minutos seguidos, às voltas pátio do ed. A, " +
                "a piscar um olho e a sorrir com a câmara frontal do dispositivo móvel apontada para si!\n");
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //checkDescr = true;
            }
        });
        alert.create().show();
    }

    @Override public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent.getItemAtPosition(pos).toString();
        Log.d(TAG, "Selected model: " + selectedModel);
        preview.stop();
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
            startCameraSource(CameraSource.CAMERA_FACING_FRONT);
        } else {
            getRuntimePermissions();
        }
    }
    @Override public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }
    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraSource != null) {
            if (isChecked) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource(CameraSource.CAMERA_FACING_FRONT);
    }

    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }
        Log.i(TAG, "Using Face Detector Processor");
        cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor());
    }
    private void startCameraSource(int facingStartCamera) {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }

                cameraSource.setFacing(facingStartCamera);
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
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
    private void checkFineLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                PatioActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    PatioActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_FLPERMISSION // todo: declare this constant
            );
        }
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                Toast.makeText(this,
                        "Error: high accuracy location mode must be enabled in the device.",
                        Toast.LENGTH_LONG).show();
                return;

            }
        } catch (Settings.SettingNotFoundException e) {
            Toast.makeText(this, "Error: could not access location mode.",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
    @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
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
                                + "Fences "+ stringKey+" were successfully removed.";
                        Log.d("xxxfences", text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be removed: " + e.getMessage();
                        Log.d("xxxfences", text);
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
                            if (fenceKey.equals("locationFenceKey") && state == FenceState.TRUE)
                                Singleton.getInstance().setFenceBool(true);
                            if (fenceKey.equals("notWalkingFenceKey") && state == FenceState.TRUE)
                                Singleton.getInstance().setNotWalkinBool(true);
                        }
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        text2 = "location: " + fenceInfo;
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "> Fences' states:\n" + (fenceInfo.equals("") ?
                                "No registered fences." : fenceInfo);
                        Log.d("xxxfences", text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be queried: " + e.getMessage();
                        Log.d("xxxfences", text);
                    }
                });
    }

    private void printLocation() {
        checkFineLocationPermission();
        Awareness.getSnapshotClient(this).getLocation()
                .addOnSuccessListener(new OnSuccessListener<LocationResponse>() {
                    @Override
                    public void onSuccess(LocationResponse locationResponse) {
                        Location location = locationResponse.getLocation();

                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "Lat:" + location.getLatitude() + ", Lng:" + location.getLongitude()
                                + "  39.7352267" + "-8.820709";
                        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG_SNAPSHOT, "Could not get Location: " + e);
                        Snackbar.make(findViewById(android.R.id.content), "Noo", Snackbar.LENGTH_LONG).show();

                    }
                });
    }

    @Override public void onBackPressed() {
        Log.d("xxxfences", "back button pressed");
        checkWaring=true;
        m1.pause();
        showDialogWaring();
    }
    public void showDialogWaring() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Sair Tarefa");
        alert.setMessage("Caloiro tem a certeza que pretende sair!\n Qualquer progresso que tenha feito ira ser perdido");
        alert.setPositiveButton("Terminar Tarefa", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkWaring=false;
                m1.cancel();
                Singleton.getInstance().setFd(false);
                finish();

            }
        });
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkWaring=false;
                m1.resume();
            }
        });
        alert.create().show();
    }

    @Override public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource(CameraSource.CAMERA_FACING_FRONT);
        queryFences();
    }
    @Override protected void onPause() {
        super.onPause();
        queryFences();
        preview.stop();
    }
    @Override public void onDestroy() {
        super.onDestroy();
        Log.d("xxxfences", "onResume");
        removeFences("locationFenceKey");
        if (cameraSource != null) {
            cameraSource.release();
        }
    }
    @Override public void onStop() {
        super.onStop();
        queryFences();
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        return true;
    }
}
