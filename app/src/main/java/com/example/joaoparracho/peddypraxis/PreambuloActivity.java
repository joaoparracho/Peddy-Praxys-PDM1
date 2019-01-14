package com.example.joaoparracho.peddypraxis;

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.renderscript.ScriptIntrinsicBLAS;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.example.joaoparracho.peddypraxis.model.Utilizador;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PreambuloActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int numTask = 4;
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private static final String TAG = "PreambuloActivity";
    private TextView preambTextV;
    private TextView titlePreamTextV;
    private EditText respostaEdtT;
    private MenuItem itemPreamb;
    private Button btnPream;
    private ProgressBar gameProgress;
    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private long finishTime;

    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preambulo);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

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

        if (Singleton.getInstance().getStartTime() == -1)
            Singleton.getInstance().setStartTime(System.currentTimeMillis());

        if (!Singleton.getInstance().getActivityKey().equals("finishGameKey")) {
            setupFences();
            if (Singleton.getInstance().getActivityKey().equals("bibliotecaKey"))
                respostaEdtT.setVisibility(View.VISIBLE);
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
            case "descompressaoKey":
                titlePreamTextV.setText(getString(R.string.descompressap));
                preambTextV.setText(getString(R.string.preambDescompressao));
                break;
            case "corridaKey":
                titlePreamTextV.setText(getString(R.string.run));
                preambTextV.setText(getString(R.string.runDesc));
                break;
            case "finishGameKey":
                titlePreamTextV.setText("Game Finished");
                if (Singleton.getInstance().getNumTasksComplete() == numTask) {
                    finishTime = System.currentTimeMillis() - Singleton.getInstance().getStartTime();
                    int min = (int) (((finishTime) / 1000) % 3600) / 60;
                    int sec = (int) (finishTime / 1000) % 60;
                    Singleton.getInstance().getCurrentUser().setNumJogosTerm(Singleton.getInstance().getCurrentUser().getNumJogosTerm() + 1);
                    Log.d("xxxSingleton", "Number term" + (Singleton.getInstance().getCurrentUser().getNumJogosTerm()));

                    if ((int) ((finishTime / 1000) / 3600) > 0) {
                        preambTextV.setText(getString(R.string.fnsGame1h));
                    } else {
                        preambTextV.setText(getString(R.string.fnshGameInTime) +" "+ min+" " + getString(R.string.min) +" "+ sec + " "+getString(R.string.sec));
                    }
                    if (Singleton.getInstance().getCurrentUser().getMelhorTempo() > finishTime || Singleton.getInstance().getCurrentUser().getMelhorTempo() == 0){
                        showDialogBestTime();
                        Singleton.getInstance().getCurrentUser().setMelhorTempo(finishTime);
                    }
                }
                else preambTextV.setText(getString(R.string.finTime) + Singleton.getInstance().getNumTasksComplete() + getString(R.string.numTask));

                btnPream.setText(getString(R.string.returnGmMn));
                sendData();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_preamb, menu);
        itemPreamb = menu.findItem(R.id.iconPreamb);
        itemPreamb.setVisible(Singleton.getInstance().getActivityKey().equals("finishGameKey"));
        return true;
    }


    public void onCLickShowPreamb(MenuItem item) {
        showDescription();
    }

    public void showDescription() {
        float precVit = (Singleton.getInstance().getCurrentUser().getNumJogosTerm() * 100) / (Singleton.getInstance().getCurrentUser().getNumJogosInic());
        int min = (int) (((Singleton.getInstance().getCurrentUser().getMelhorTempo()) / 1000) % 3600) / 60;
        int sec = (int) (Singleton.getInstance().getCurrentUser().getMelhorTempo() / 1000) % 60;

        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(getString(R.string.estatiTitle));

        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                getString(R.string.estatiTitle).length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        new AlertDialog.Builder(this)
                .setTitle(ssBuilder)
                .setMessage(getString(R.string.name) + Singleton.getInstance().getCurrentUser().getName()
                        +"\n"+getString(R.string.age) + Singleton.getInstance().getCurrentUser().getIdade()
                        +"\n"+getString(R.string.bestTime) + min + ":" + sec
                        +"\n"+getString(R.string.numGameStr) + Singleton.getInstance().getCurrentUser().getNumJogosInic()
                        +"\n"+getString(R.string.finishGame) + Singleton.getInstance().getCurrentUser().getNumJogosTerm()
                        +"\n"+getString(R.string.percVict) + precVit+"%")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    public void oncLickPreamb(View view) {
        queryFences();
        switch (Singleton.getInstance().getActivityKey()) {
            case "patioKey":
                if (Singleton.getInstance().isFenceBool())
                    startActivity(new Intent(PreambuloActivity.this, PatioActivity.class));
                else
                    Snackbar.make(findViewById(android.R.id.content), R.string.goPatA, Snackbar.LENGTH_LONG).show();
                break;
            case "edificiosKey":
                if (Singleton.getInstance().isbInRotA())
                    startActivity(new Intent(PreambuloActivity.this, EdificioActivity.class));
                else
                    Snackbar.make(findViewById(android.R.id.content), R.string.goRotA, Snackbar.LENGTH_LONG).show();
                break;
            case "bibliotecaKey":
                if (Singleton.getInstance().isbLibLoc() && respostaEdtT.getText().toString().equalsIgnoreCase("criatividade"))
                    startActivity(new Intent(PreambuloActivity.this, BibliotecaActivity.class));
                else if (!Singleton.getInstance().isbLibLoc())
                    Snackbar.make(findViewById(android.R.id.content), R.string.goBib, Snackbar.LENGTH_LONG).show();
                else
                    Snackbar.make(findViewById(android.R.id.content), R.string.rightAns, Snackbar.LENGTH_LONG).show();
                break;
            case "descompressaoKey":
                if (Singleton.getInstance().isFenceBool())
                    startActivity(new Intent(PreambuloActivity.this, DescompressaoActivity.class));
                else
                    Snackbar.make(findViewById(android.R.id.content), R.string.goPatA, Snackbar.LENGTH_LONG).show();
                break;
            case "corridaKey":
                if (Singleton.getInstance().isFenceBool())
                    startActivity(new Intent(PreambuloActivity.this, CorridaActivity.class));
                else
                    Snackbar.make(findViewById(android.R.id.content), R.string.goPatA, Snackbar.LENGTH_LONG).show();
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
//        addFence("essleiFenceKey", LocationFence.entering(39.732766, -8.820643, 30));
//        addFence("locationFenceKey", LocationFence.in(39.735667, -8.820923, 80, 0L));
//        addFence("rotALocationFenceKey", LocationFence.in(39.734801, -8.820879, 20, 0L));
//        addFence("libLocationFenceKey", LocationFence.in(39.733381, -8.820621, 50, 0L));
        Log.d(TAG, "Olha a string " + Singleton.getInstance().getActivityKey());
        switch (Singleton.getInstance().getActivityKey()) {
            case "corridaKey":
                AwarenessFence essleiLocationFence = LocationFence.entering(39.732766, -8.820643, 30);
                addFence("essleiFenceKey", essleiLocationFence);
            case "patioKey":
            case "descompressaoKey":
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
                        Log.d(TAG, "addFence success " + fenceKey);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "addFence failure " + fenceKey);
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
        String s = getString(R.string.retMnJg);
        new AlertDialog.Builder(this)
                .setTitle(R.string.extGame)
                .setMessage(s)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Singleton.getInstance().restartVariables();
                        Singleton.getInstance().setActivityKey("patioKey");
                        startActivity(new Intent(PreambuloActivity.this, GameScreenActivity.class));
                        removeFences();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    public void showDialogWarning() {
        String s = getString(R.string.warnLst);
        new AlertDialog.Builder(this)
                .setTitle(R.string.extGame)
                .setMessage(s)
                .setPositiveButton(R.string.fngame, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Singleton.getInstance().restartVariables();
                        removeFences();
                        Singleton.getInstance().setActivityKey("patioKey");
                        startActivity(new Intent(PreambuloActivity.this, GameScreenActivity.class));
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
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
        setupFences();
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
        if (ContextCompat.checkSelfPermission(PreambuloActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(PreambuloActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY)
                Log.e(TAG, "Error: high accuracy location mode must be enabled in the device.");
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
                            if (fenceKey.equals("libLocationFenceKey") && state == FenceState.TRUE)
                                Singleton.getInstance().setbLibLoc(true);
                            if (fenceKey.equals("locationFenceKey") && state == FenceState.TRUE)
                                Singleton.getInstance().setFenceBool(true);
                            if (fenceKey.equals("walkingFenceKey") && state == FenceState.TRUE)
                                Singleton.getInstance().setWalkingBool(true);
                            if (fenceKey.equals("rotALocationFenceKey") && state == FenceState.TRUE)
                                Singleton.getInstance().setbInRotA(true);
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

    private void sendData() {
        DatabaseReference myRef = firebaseDatabase.getReference(firebaseAuth.getUid());
        myRef.setValue(Singleton.getInstance().getCurrentUser());
    }

    public void verifyPrvInfo() {
        final DatabaseReference databaseReference = firebaseDatabase.getReference(firebaseAuth.getUid());
        Log.d("xxxSingleton", firebaseAuth.getUid());
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Singleton.getInstance().setCurrentUser(dataSnapshot.getValue(Utilizador.class));
                Singleton.getInstance().getCurrentUser().setNumJogosTerm(Singleton.getInstance().getCurrentUser().getNumJogosTerm() + 1);
                Log.d("xxxSingleton", "Number term" + (Singleton.getInstance().getCurrentUser().getNumJogosTerm()));
                sendData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void showDialogBestTime() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.congt)
                .setMessage(R.string.checksta)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }
}
