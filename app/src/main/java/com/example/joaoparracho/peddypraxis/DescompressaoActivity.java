package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.model.CountDownTimer2;
import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.PlacesResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

public class DescompressaoActivity extends AppCompatActivity implements SensorEventListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DescompressaoActivity";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    public static Handler mHandler;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private TextView timeTextView;
    private long startMillis = 10000;
    private long mTimeInMillis = startMillis;
    private CountDownTimer2 m2;
    private boolean pauseCounterOnce;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean bCheck = true;
    private boolean bFaceDown;
    private boolean bRain;
    private Weather weather;
    private String plText="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descompressao);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        timeTextView = findViewById(R.id.tvTime);

        m2 = new CountDownTimer2(mTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                if (ActivityCompat.checkSelfPermission(DescompressaoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
                Awareness.getSnapshotClient(DescompressaoActivity.this).getWeather()
                        .addOnSuccessListener(new OnSuccessListener<WeatherResponse>() {
                            @Override
                            public void onSuccess(WeatherResponse weatherResponse) {
                                weather = weatherResponse.getWeather();
                                for (int condition : weather.getConditions()) {
                                    switch (condition) {
                                        case Weather.CONDITION_RAINY:
                                            bRain = true;
                                            Log.e(TAG, "weatherSnap: Rainning");
                                            break;
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "weatherSnap: Could not get Weather: " + e);
                            }
                        });
                if (!bRain && bFaceDown && (Singleton.getInstance().isFenceBool() || Singleton.getInstance().isNearbyBool()) && bCheck) {
                    if (m2.ismPaused()) {
                        m2.cancel();
                        m2.start();
                    }
                    mTimeInMillis = millisUntilFinished;
                    pauseCounterOnce = false;
                } else if (!pauseCounterOnce) {
                    if (bRain) printNearbyPlaces();
                    m2.pause();
                    pauseCounterOnce = true;
                }
                timeTextView.setText(updateCountDownText());
            }

            @Override
            public void onFinish() {
                m2.cancel();
                timeTextView.setText(getString(R.string.finish));
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(500);
                Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete() + 1);
                Singleton.getInstance().setActivityKey("finishGameKey");
                finish();
                startActivity(new Intent(DescompressaoActivity.this, PreambuloActivity.class));
            }
        }.start();

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String feedback = msg.getData().getString("FEEDBACK");
                if (feedback != null) Snackbar.make(findViewById(android.R.id.content), feedback, Snackbar.LENGTH_LONG).show();
            }
        };
    }

    public void onClickShowPreamb(MenuItem item) {
        bCheck = false;
        new AlertDialog.Builder(this).setTitle(R.string.descompressao).setMessage(getString(R.string.descompDesc) + "\n\n" + plText).setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bCheck = true;
            }
        }).create().show();
    }

    private void printNearbyPlaces() {
        if (ContextCompat.checkSelfPermission(DescompressaoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(DescompressaoActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) Log.e(TAG, "Error: high accuracy location mode must be enabled in the device.");
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error: could not access location mode.");
            e.printStackTrace();
        }
        Awareness.getSnapshotClient(this).getPlaces()
                .addOnSuccessListener(new OnSuccessListener<PlacesResponse>() {
                    @Override
                    public void onSuccess(PlacesResponse placesResponse) {
                        List<PlaceLikelihood> pll = placesResponse.getPlaceLikelihoods();
                        plText = "\n";
                        for (int i = 0; i < (pll.size() < 3 ? pll.size() : 3); i++) {
                            PlaceLikelihood pl = pll.get(i);
                            plText += "\t#" + (i + 1) + ": " + pl.getPlace().getName().toString() + "\n";
                        }
                        setupNearbyFences(pll.get(0).getPlace().getLatLng().latitude, pll.get(0).getPlace().getLatLng().longitude,
                                pll.get(1).getPlace().getLatLng().latitude, pll.get(1).getPlace().getLatLng().longitude,
                                pll.get(2).getPlace().getLatLng().latitude, pll.get(2).getPlace().getLatLng().longitude);
                        new AlertDialog.Builder(DescompressaoActivity.this).setTitle(R.string.atention).setMessage(getString(R.string.warnAtent) + getString(R.string.goNearbyPlac) + plText).setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).create().show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Could not get Places: " + e);
                        Toast.makeText(DescompressaoActivity.this, "Could not get Places: " + e, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupNearbyFences(double lat1, double long1, double lat2, double long2, double lat3, double long3) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        AwarenessFence nearbyLoc = AwarenessFence.or(LocationFence.in(lat1, long1, 30, 0L),
                LocationFence.in(lat2, long2, 30, 0L),
                LocationFence.in(lat3, long3, 30, 0L));
        addFence("nearbyFence", nearbyLoc);
    }

    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("AddFence", "addFence success " + fenceKey);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "addFence failure " + fenceKey);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        return true;
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;
        if (hours > 0) timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        else timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        return timeLeftFormatted;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        bFaceDown = sensorEvent.values[2] < -9.5;
        if (!bFaceDown) {
            mTimeInMillis =startMillis;
            m2.cancel();
            m2.start();
            timeTextView.setText(updateCountDownText());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, sensor.getName() + "accuracy changed to " + accuracy);
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
                        }
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n" + "> Fences' states:\n" + (fenceInfo.equals("") ? "No registered fences." : fenceInfo);
                        Log.d(TAG, text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n" + "Fences could not be queried: " + e.getMessage();
                        Log.d(TAG, text);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back button pressed");
        new AlertDialog.Builder(this).setTitle(R.string.endTask).setMessage(R.string.warnLst).setPositiveButton(R.string.endTask, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m2.cancel();
                finish();
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        queryFences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mSensorManager.unregisterListener(this);
        queryFences();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeFences("nearbyFence");
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        queryFences();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }
}
