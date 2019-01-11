package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Timestamp;

public class PreambuloActivity extends AppCompatActivity implements  GoogleApiClient.OnConnectionFailedListener {

    private TextView preambTextV;
    private TextView titlePreamTextV;

    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preambulo);

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

        setupFences();
        preambTextV=findViewById(R.id.tvPreambulo);
        titlePreamTextV=findViewById(R.id.tvPreambTitle);
        updatePreambuloText();
    }

    private void updatePreambuloText(){
        switch (Singleton.getInstance().getActivityKey()){
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
        }
    }
    public void oncLickPreamb(View view) {
        switch (Singleton.getInstance().getActivityKey()){
            case "patioKey":
                if(Singleton.getInstance().isFenceBool()){
                    startActivity(new Intent(PreambuloActivity.this, PatioActivity.class));
                }
                else{
                    Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para o pátio do A", Snackbar.LENGTH_LONG).show();
                }
                break;
            case "edificiosKey":
                break;
            case "bibliotecaKey":
                if(Singleton.getInstance().isbLibLoc()){
                    startActivity(new Intent(PreambuloActivity.this, BibliotecaActivity.class));
                }
                else{
                    Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para a Biblioteca", Snackbar.LENGTH_LONG).show();
                }
                break;
            case "descompressaoKey":
                if(Singleton.getInstance().isFenceBool()){
                    startActivity(new Intent(PreambuloActivity.this, DescompressaoActivity.class));
                }
                else{
                    Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para o pátio do A", Snackbar.LENGTH_LONG).show();
                }
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
        Log.d("xxxfences", "Olha a string"+Singleton.getInstance().getActivityKey());
        switch (Singleton.getInstance().getActivityKey()){

            case "patioKey":
            case "descompressaoKey":
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling.
                    return;
                }
                AwarenessFence inLocationFence = LocationFence.in(39.73565192930836, -8.820967774868791, 80, 0L);
                addFence("locationFenceKey", inLocationFence);
                break;
            case "bibliotecaKey":
                AwarenessFence libraryLocationFence = LocationFence.in(39.7331653,-8.8204611, 30, 0L);
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
                        Log.d("fence", "addFence success");

                        Snackbar.make(findViewById(android.R.id.content), "Success to add Fence", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(findViewById(android.R.id.content), "Failed to add Fence", Snackbar.LENGTH_LONG).show();
                        Log.d("fence", "addFence failure");
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
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences "+" were successfully removed.";
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

    @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override public void onBackPressed() {
        queryFences();
        Log.d("xxxfences", "back button pressed");
        showDialogWaring();
    }
    public void showDialogWaring() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Exit Game");
        alert.setMessage("Caloiro tem a certeza que pretende terminar o jogo!\n Qualquer progresso que tenha feito ira ser perdido");
        alert.setPositiveButton("Terminar Jogo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Singleton.getInstance().setbCreateFenceTime(false);
                Singleton.getInstance().setActivityKey("patioKey");
                removeFences();
                startActivity(new Intent(PreambuloActivity.this, GameScreenActivity.class));
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

    @Override public void onResume() {
        super.onResume();
        queryFences();
    }
    @Override protected void onPause() {
        super.onPause();
        queryFences();
    }
    @Override public void onStop() {
        queryFences();
        super.onStop();
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
}
