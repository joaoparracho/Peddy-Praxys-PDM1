package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Timestamp;

public class PreambuloActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int numTask = 4;
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private static final String TAG = "xxxfences";
    private TextView preambTextV;
    private TextView titlePreamTextV;
    private Button btnPream;
    private ProgressBar gameProgress;
    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preambulo);

        gameProgress = findViewById(R.id.gameProgressBar);
        gameProgress.setMax(numTask);
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        if (Singleton.getInstance().getActivityKey().equals("finishGameKey")) removeFences();
        else setupFences();

        preambTextV = findViewById(R.id.tvPreambulo);
        titlePreamTextV = findViewById(R.id.tvPreambTitle);
        btnPream = findViewById(R.id.btnPlayTask);
        updatePreambuloText();
    }

    private void updatePreambuloText() {
        switch (Singleton.getInstance().getActivityKey()) {
            case "patioKey":
                titlePreamTextV.setText(getString(R.string.patio));
                preambTextV.setText(getString(R.string.preambPatio));
                break;
            case "edificiosKey":
                titlePreamTextV.setText(getString(R.string.edificios));
                preambTextV.setText(getString(R.string.preambEdificios));
                break;
            case "bibliotecaKey":
                titlePreamTextV.setText(getString(R.string.biblioteca));
                preambTextV.setText(getString(R.string.preamBiblioteca));
                break;
            case "descompressaoKey":
                titlePreamTextV.setText(getString(R.string.descompressap));
                preambTextV.setText(getString(R.string.preambDescompressao));
                break;
            case "finishGameKey":
                titlePreamTextV.setText("Game Finished");
                if (Singleton.getInstance().getNumTasksComplete() == numTask) preambTextV.setText("BEM CARALHO!\n Conseguiste concluir tudo");
                else preambTextV.setText("BELA MERDA CARALHO!\nSo conseguiste completar " + Singleton.getInstance().getNumTasksComplete() + " numero de tarefas");
                btnPream.setText("Voltar Menu de Jogo");
                break;
        }
    }

    public void oncLickPreamb(View view) {
        queryFences();
        switch (Singleton.getInstance().getActivityKey()) {
            case "patioKey":
                if (Singleton.getInstance().isFenceBool()) startActivity(new Intent(PreambuloActivity.this, PatioActivity.class));
                else Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para o pátio do A", Snackbar.LENGTH_LONG).show();
                break;
            case "edificiosKey":
                if (Singleton.getInstance().isbInRotA()) startActivity(new Intent(PreambuloActivity.this, EdificioActivity.class));
                else Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para a rotunda perto do A", Snackbar.LENGTH_LONG).show();
                break;
            case "bibliotecaKey":
                if (Singleton.getInstance().isbLibLoc()) startActivity(new Intent(PreambuloActivity.this, BibliotecaActivity.class));
                else Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para a Biblioteca", Snackbar.LENGTH_LONG).show();
                break;
            case "descompressaoKey":
                if (Singleton.getInstance().isFenceBool()) startActivity(new Intent(PreambuloActivity.this, DescompressaoActivity.class));
                else Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para o pátio do A", Snackbar.LENGTH_LONG).show();
                break;
            case "finishGameKey":
                showDialogExit();
                break;
        }
    }

    private void setupFences() {
        long nowMillis = System.currentTimeMillis();

        if (!Singleton.getInstance().isbCreateFenceTime()) {
            Singleton.getInstance().setbCreateFenceTime(true);
            AwarenessFence timeFence100 = TimeFence.inInterval(nowMillis, nowMillis + 60 * 60000); // one minute starting in thirty seconds
            AwarenessFence timeFence50 = TimeFence.inInterval(nowMillis, nowMillis + 60 * 30000); // one minute starting in thirty seconds
            AwarenessFence timeFence90 = TimeFence.inInterval(nowMillis, nowMillis + 60 * 54000); // one minute starting in thirty seconds

            addFence("timeFence50Key", timeFence50);
            addFence("timeFence90Key", timeFence90);
            addFence("timeFence100Key", timeFence100);

            AwarenessFence notWalkingFence = AwarenessFence.or(
                    DetectedActivityFence.during(DetectedActivityFence.ON_BICYCLE),
                    DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE));

            addFence("notWalkingFenceKey", notWalkingFence);
        }
        Log.d(TAG, "Olha a string" + Singleton.getInstance().getActivityKey());
        switch (Singleton.getInstance().getActivityKey()) {
            case "patioKey":
            case "descompressaoKey":
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
                AwarenessFence inLocationFence = LocationFence.in(39.735667, -8.820923, 80, 0L);
                addFence("locationFenceKey", inLocationFence);
                break;
            case "edificiosKey":
                AwarenessFence rotundaLocationFence = LocationFence.in(39.734801, -8.820879, 20, 0L);
                addFence("rotALocationFenceKey", rotundaLocationFence);
                break;
            case "bibliotecaKey":
                AwarenessFence libraryLocationFence = LocationFence.in(39.733381, -8.820621, 50, 0L);
                addFence("libLocationFenceKey", libraryLocationFence);
                break;
        }
    }

    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "addFence success");
//                        Snackbar.make(findViewById(android.R.id.content), "Success to add Fence", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "addFence failure");
//                        Snackbar.make(findViewById(android.R.id.content), "Failed to add Fence", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    protected void removeFences() {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(myPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\nFences " + " were successfully removed.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\n Fences could not be removed: " + e.getMessage());
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onBackPressed() {
        queryFences();
        Log.d(TAG, "back button pressed");
        if (!Singleton.getInstance().getActivityKey().equals("finishGameKey")) showDialogWarning();
        else showDialogExit();
    }

    public void showDialogExit() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Game")
                .setMessage("Caloiro pretende voltar ao menu de jogos?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Singleton.getInstance().restartVariables();
                        Singleton.getInstance().setActivityKey("patioKey");
                        startActivity(new Intent(PreambuloActivity.this, GameScreenActivity.class));
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

    public void showDialogWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Game")
                .setMessage("Caloiro tem a certeza que pretende terminar o jogo!\n Qualquer progresso que tenha feito ira ser perdido")
                .setPositiveButton("Terminar Jogo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Singleton.getInstance().restartVariables();
                        removeFences();
                        Singleton.getInstance().setActivityKey("patioKey");
                        startActivity(new Intent(PreambuloActivity.this, GameScreenActivity.class));
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
        gameProgress.setProgress(Singleton.getInstance().getNumTasksComplete());
        queryFences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        queryFences();
    }

    @Override
    public void onStop() {
        queryFences();
        super.onStop();
    }

    protected void queryFences() {
        if (ContextCompat.checkSelfPermission(PreambuloActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(PreambuloActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) Log.e(TAG, "Error: high accuracy location mode must be enabled in the device.");
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error: could not access location mode" + e);
        }

        Awareness.getSnapshotClient(this).getLocation()
                .addOnSuccessListener(new OnSuccessListener<LocationResponse>() {
                    @Override
                    public void onSuccess(LocationResponse locationResponse) {
                        Log.d(TAG, "Lat:" + locationResponse.getLocation().getLatitude() + ", Lng:" + locationResponse.getLocation().getLongitude());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Could not get Location: " + e);
                    }
                });

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
                            if (fenceKey.equals("locationFenceKey") && state == FenceState.TRUE) Singleton.getInstance().setFenceBool(true);
                            if (fenceKey.equals("rotALocationFenceKey") && state == FenceState.TRUE) Singleton.getInstance().setbInRotA(true);
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
}
