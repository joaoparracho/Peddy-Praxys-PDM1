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
import android.widget.EditText;
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

    private static final int numTask = 5;
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private static final String TAG = "PreambuloActivity";
    private TextView preambTextV;
    private TextView titlePreamTextV;
    private EditText respostaEdtT;
    private Button btnPream;
    private ProgressBar gameProgress;
    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private long finishTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preambulo);

        gameProgress = findViewById(R.id.gameProgressBar);
        respostaEdtT = findViewById(R.id.editTextResposta);
        preambTextV = findViewById(R.id.tvPreambulo);
        titlePreamTextV = findViewById(R.id.tvPreambTitle);
        btnPream = findViewById(R.id.btnPlayTask);

        gameProgress.setMax(numTask);
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        if (Singleton.getInstance().getStartTime() == -1) Singleton.getInstance().setStartTime(System.currentTimeMillis());

        if (!Singleton.getInstance().getActivityKey().equals("finishGameKey")) {
            setupFences();
            if (Singleton.getInstance().getActivityKey().equals("bibliotecaKey")) respostaEdtT.setVisibility(View.VISIBLE);
        } else removeFences();
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
            case "corridaKey":
                titlePreamTextV.setText(getString(R.string.corrida));
                preambTextV.setText(getString(R.string.preambCorrida));
                break;
            case "perguntaKey":
                titlePreamTextV.setText(getString(R.string.pergunta));
                preambTextV.setText(getString(R.string.preambPergunta));
                break;
            case "descompressaoKey":
                titlePreamTextV.setText(getString(R.string.descompressap));
                preambTextV.setText(getString(R.string.preambDescompressao));
                break;
            case "finishGameKey":
                titlePreamTextV.setText("Game Finished");
                if (Singleton.getInstance().getNumTasksComplete() == numTask) preambTextV.setText("BEM CARALHO!\n Conseguiste concluir tudo");
                else preambTextV.setText("BELA MERDA CARALHO!\nSo conseguiste completar " + Singleton.getInstance().getNumTasksComplete() + " numero de tarefas");
                finishTime = System.currentTimeMillis() - Singleton.getInstance().getStartTime();
                int min = (int) (((finishTime) / 1000) % 3600) / 60;
                int sec = (int) (finishTime / 1000) % 60;
                if (Singleton.getInstance().getNumTasksComplete() >= numTask) {
                    if ((int) ((finishTime / 1000) / 3600) > 0) preambTextV.setText("BEM CARALHO!\n Conseguiste concluir tudo em 1 hora mesmo por pouco");
                    else preambTextV.setText("BEM CARALHO!\n Conseguiste concluir tudo em " + min + " minutos e " + sec + " segundos");
                } else preambTextV.setText("BELA MERDA CARALHO!\nSo conseguiste completar " + Singleton.getInstance().getNumTasksComplete() + " numero de tarefas");
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
                if (Singleton.getInstance().isbLibLoc() && respostaEdtT.getText().toString().equalsIgnoreCase("criatividade")) startActivity(new Intent(PreambuloActivity.this, BibliotecaActivity.class));
                else if (!Singleton.getInstance().isbLibLoc()) Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para a Biblioteca", Snackbar.LENGTH_LONG).show();
                else Snackbar.make(findViewById(android.R.id.content), "Introduza a resposta correta!", Snackbar.LENGTH_LONG).show();
                break;
            case "corridaKey":
                if (Singleton.getInstance().isFenceBool()) startActivity(new Intent(PreambuloActivity.this, CorridaActivity.class));
                else Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para o pátio do A", Snackbar.LENGTH_LONG).show();
                break;
            case "perguntaKey":
                if (Singleton.getInstance().isFenceBool()) startActivity(new Intent(PreambuloActivity.this, PerguntaActivity.class));
                else Snackbar.make(findViewById(android.R.id.content), "Caloiro dirija-se para o pátio do A", Snackbar.LENGTH_LONG).show();
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
            addFence("timeFence50Key", TimeFence.inInterval(nowMillis, nowMillis + 60 * 30000));
            addFence("timeFence90Key", TimeFence.inInterval(nowMillis, nowMillis + 60 * 54000));
            addFence("timeFence100Key", TimeFence.inInterval(nowMillis, nowMillis + 60 * 60000));
            addFence("walkingFenceKey", AwarenessFence.not(AwarenessFence.or(DetectedActivityFence.during(DetectedActivityFence.ON_BICYCLE), DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE))));
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        Log.d(TAG, "Olha a string " + Singleton.getInstance().getActivityKey());
        switch (Singleton.getInstance().getActivityKey()) {
            case "corridaKey":
                addFence("essleiFenceKey", LocationFence.entering(39.732766, -8.820643, 30));
            case "patioKey":
            case "perguntaKey":
            case "descompressaoKey":
                addFence("locationFenceKey", LocationFence.in(39.735655, -8.820948, 50, 0L));
                break;
            case "edificiosKey":
                addFence("rotALocationFenceKey", LocationFence.in(39.734801, -8.820879, 20, 0L));
                addFence("ediA", LocationFence.in(39.734998, -8.820920, 25, 0L));
                addFence("ediB", LocationFence.in(39.734300, -8.821617, 25, 0L));
                addFence("ediC", LocationFence.in(39.733936, -8.822008, 30, 0L));
                addFence("ediD", LocationFence.in(39.734404, -8.821077, 30, 0L));
                addFence("ediE", LocationFence.in(39.733051, -8.821467, 30, 0L));
                break;
            case "bibliotecaKey":
                addFence("libLocationFenceKey", LocationFence.in(39.733381, -8.820621, 50, 0L));
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
                        Log.d(TAG, "addFence success " + fenceKey);
//                        Snackbar.make(findViewById(android.R.id.content), "Success to add Fence", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "addFence failure " + fenceKey);
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
                        removeFences();
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
                            if (fenceKey.equals("walkingFenceKey") && state == FenceState.TRUE) Singleton.getInstance().setWalkingBool(true);
                            if (fenceKey.equals("rotALocationFenceKey") && state == FenceState.TRUE) Singleton.getInstance().setbInRotA(true);
                        }
                        Log.d(TAG, "\n\n[Fences @ " + new Timestamp(System.currentTimeMillis()) + "]\n> Fences states:\n" + (fenceInfo.equals("") ? "No registered fences." : fenceInfo));
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
