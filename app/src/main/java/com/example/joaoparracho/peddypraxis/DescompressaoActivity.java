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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Locale;

public class DescompressaoActivity extends AppCompatActivity implements SensorEventListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DescompressaoActivity";
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG_FENCES = "fences";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";

    public static Handler mHandler;

    private TextView timeTextView;

    private long mTimeInMillis = 60 * 500;
    private CountDownTimer2 m2;
    private boolean pauseCounterOnce;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private boolean bCheck=true;
    private boolean bFaceDown;
    private boolean bRain;
    private Weather weather;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descompressao);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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

        timeTextView = (TextView) findViewById(R.id.tvTime);

        m2 = new CountDownTimer2(mTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                if (ActivityCompat.checkSelfPermission(DescompressaoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    return;
                }
                Awareness.getSnapshotClient(DescompressaoActivity.this).getWeather()
                        .addOnSuccessListener(new OnSuccessListener<WeatherResponse>() {
                            @Override
                            public void onSuccess(WeatherResponse weatherResponse) {
                                 weather = weatherResponse.getWeather();
                                for (int condition : weather.getConditions()) {
                                    switch (condition) {
                                        case Weather.CONDITION_RAINY:
                                            bRain = true;
                                            Log.e("weatherSnap", "Rainning");
                                            break;
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("weatherSnap", "Could not get Weather: " + e);
                                Toast.makeText(DescompressaoActivity.this, "Could not get Weather: " + e,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                    if (!bRain && bFaceDown && Singleton.getInstance().isFenceBool()&& bCheck ) {
                        if (m2.ismPaused()) {
                            m2.resume();
                        }
                        mTimeInMillis = millisUntilFinished;
                        pauseCounterOnce = false;
                    } else if (!pauseCounterOnce) {
                        m2.pause();
                        pauseCounterOnce = true;
                    }
                timeTextView.setText(updateCountDownText());
            }

            @Override
            public void onFinish() {
                timeTextView.setText("Finish");
                Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete()+1);
                Singleton.getInstance().setActivityKey("finishGameKey");
                startActivity(new Intent(DescompressaoActivity.this, PreambuloActivity.class));
            }
        }.start();

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

        showDescription();
    }

    public void onCLickShowPreamb(MenuItem item) {bCheck=false;showDescription();}
    public void showDescription(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Descompressão");
        alert.setMessage(": O caloiro tem de estar sentado durante 10 minutos, no pátio do ed. A, com o " +
                "telemóvel pousado no colo com o ecrã virado para baixo (é para relaxar!).");
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bCheck=true;
            }
        });
        alert.create().show();
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        return true;
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;
        if (hours > 0) {
            if (Singleton.getInstance().isFenceBool()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d", hours, minutes, seconds);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d", hours, minutes, seconds);
            }
        } else {
            if (Singleton.getInstance().isFenceBool()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d -- TRUE", minutes, seconds);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d --FALSE", minutes, seconds);
            }
        }
        return timeLeftFormatted;
    }
    @Override public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        if(z<0)
            bFaceDown=true;
        else
            bFaceDown=false;
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Toast.makeText(DescompressaoActivity.this, sensor.getName() + "accuracy changed to " + accuracy,
                Toast.LENGTH_SHORT).show();
    }

    protected void removeFences(String fenceKey) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(fenceKey)
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
                            if(fenceKey.equals("locationFenceKey") &&state == FenceState.TRUE)
                                Singleton.getInstance().setFenceBool(true);
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

    @Override public void onBackPressed() {
        Log.d("xxxfences", "back button pressed");
        showDialogWaring();
    }
    public void showDialogWaring() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Sair Tarefa");
        alert.setMessage("Caloiro tem a certeza que pretende sair!\n Qualquer progresso que tenha feito ira ser perdido");
        alert.setPositiveButton("Terminar Tarefa", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m2.cancel();
                finish();
            }
        });
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.create().show();
    }

    @Override protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        queryFences();
    }
    @Override public void onResume() {
        super.onResume();
        mSensorManager.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        queryFences();
        Log.d(TAG, "onResume");
    }
    @Override public void onDestroy() {
        super.onDestroy();
        removeFences("locationFenceKey");
    }
    @Override public void onStop() {
        queryFences();
        super.onStop();
    }
    @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }
}
