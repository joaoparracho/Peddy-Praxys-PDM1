package com.example.joaoparracho.peddypraxis;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.barcodescanning.BarcodeScanningProcessor;
import com.example.joaoparracho.peddypraxis.common.CameraSource;
import com.example.joaoparracho.peddypraxis.common.CameraSourcePreview;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.example.joaoparracho.peddypraxis.model.FenceReceiver;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PerguntaActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "PerguntaActivity";
    private static final String FEEDBACK = "FEEDBACK";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    public static Handler mHandler;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private TextView textViewQRCode;
    private GoogleApiClient mGoogleApiClient;
    private FenceReceiver fenceReceiver;
    private PendingIntent myPendingIntent;
    private boolean[] palavras = {false, false, false, false};
    private String tempString = "";
    private boolean completa = true;
    private String text2;

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) Log.i(TAG, "Permission granted: " + permission);
        else Log.i(TAG, "Permission NOT granted: " + permission);
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_pergunta);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        textViewQRCode = findViewById(R.id.tVInfo);
        textViewQRCode.setText("Calma");

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
                String feedback = msg.getData().getString(FEEDBACK);
                if (feedback != null) {
                    if (Singleton.getInstance().isFenceBool()) {
                        queryFences();
                        if (feedback == "False") Snackbar.make(findViewById(android.R.id.content), "QR Code errado!", Snackbar.LENGTH_SHORT).show();
                        else {
                            Log.d(TAG, feedback);
                            tempString = "";
                            if (feedback.equals("Qual")) palavras[0] = true;
                            if (feedback.equals("é")) palavras[1] = true;
                            if (feedback.equals("o")) palavras[2] = true;
                            if (feedback.equals("Curso?")) palavras[3] = true;
                            if (palavras[0]) tempString += "Qual ";
                            if (palavras[1]) tempString += "é ";
                            if (palavras[2]) tempString += "o ";
                            if (palavras[3]) tempString += "Curso?";
                            for (int i = 0; i < 4; i++) completa = completa & palavras[i];
                            textViewQRCode.setText(tempString);
                            if (completa) findViewById(R.id.resposta).setVisibility(View.VISIBLE);
                            else completa = true;
                        }
                    } else Snackbar.make(findViewById(android.R.id.content), "Tens de estar no pátio do A!", Snackbar.LENGTH_SHORT).show();
                }
            }
        };
    }

    public void onClickResposta(View view) {
        final EditText input = new EditText(PerguntaActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        AlertDialog dialog = new AlertDialog.Builder(PerguntaActivity.this).setTitle(tempString).setView(input).setPositiveButton("OK", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (input.getText().toString().toUpperCase().contains("ELETRO")) {
                    Singleton.getInstance().setActivityKey("descompressaoKey");
                    Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete() + 1);
                    finish();
                    startActivity(new Intent(PerguntaActivity.this, PreambuloActivity.class));
                } else new AlertDialog.Builder(PerguntaActivity.this).setTitle("Errado!").setPositiveButton("OK", null).create().show();
            }
        });
    }

    public void onClickActivity(View view) {
        queryFences();
        new AlertDialog.Builder(PerguntaActivity.this).setTitle("Fences").setMessage(text2).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).create().show();
    }

    public void onCLickShowPreamb(MenuItem item) {
        showDescription();
    }

    public void showDescription() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.pergunta)
                .setMessage("O caloiro tem de descobrir a pergunta através de QR Codes espalhados pelo pátio. No final tem de respondar corretamente a esta.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { /*checkDescr = true;*/ }
                })
                .create().show();
    }

    private void createCameraSource() {
        if (cameraSource == null) cameraSource = new CameraSource(this, graphicOverlay);
        cameraSource.setMachineLearningFrameProcessor(new BarcodeScanningProcessor());
    }

    private void startCameraSource() {
        if (cameraSource != null)
            try {
                if (preview == null) Log.d(TAG, "resume: Preview is null");
                if (graphicOverlay == null) Log.d(TAG, "resume: graphOverlay is null");
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
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
                        }
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = text2 = "\n\n[Fences @ " + timestamp + "]\n" + "> Fences' states:\n" + (fenceInfo.equals("") ? "No registered fences." : fenceInfo);
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
        showDialogWarning();
        queryFences();
    }

    public void showDialogWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Sair Tarefa")
                .setMessage("Caloiro tem a certeza que pretende sair!\n Qualquer progresso que tenha feito ira ser perdido")
                .setPositiveButton("Terminar Tarefa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 0; i < 5; i++) Singleton.getInstance().setFaltaEdificios(i, true);
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
        Log.d(TAG, "onResume");
        startCameraSource();
        queryFences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        preview.stop();
        queryFences();
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
