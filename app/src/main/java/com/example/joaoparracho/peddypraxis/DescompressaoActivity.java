package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.model.CountDownTimer2;
import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Locale;

public class DescompressaoActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "BibliotecaActivity";
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;

    private TextView timeTextView;

    private long mTimeInMillis = 60 * 10000;
    private CountDownTimer2 m2;
    private boolean pauseCounterOnce;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private boolean bFaceDown;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descompressao);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        timeTextView=(TextView) findViewById(R.id.tvTime);

        m2 = new CountDownTimer2(mTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                if (bFaceDown) {
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
                //startActivity(new Intent(PatioActivity.this, BibliotecaActivity.class));
            }
        }.start();
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;
        if (hours > 0) {
            if (Singleton.getInstance().getFd()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d", hours, minutes, seconds);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%d:%02d:%02d", hours, minutes, seconds);
            }
        } else {
            if (Singleton.getInstance().getFd()) {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d", minutes, seconds);
            } else {
                timeLeftFormatted = String.format(Locale.getDefault(),
                        "%02d:%02d", minutes, seconds);
            }
        }
        return timeLeftFormatted;
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        if(z<0)
            bFaceDown=true;
        else
            bFaceDown=false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Toast.makeText(DescompressaoActivity.this, sensor.getName() + "accuracy changed to " + accuracy,
                Toast.LENGTH_SHORT).show();
    }



    private void setupFences() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling.
            return;
        }
        //Parracho Casa

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

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener((SensorEventListener) this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //setupFences();
        Log.d(TAG, "onResume");
    }
    @Override
    public void onStop() {
        if (fenceReceiver != null) {
            unregisterReceiver(fenceReceiver);
            fenceReceiver = null;
        }
        super.onStop();
    }

}
