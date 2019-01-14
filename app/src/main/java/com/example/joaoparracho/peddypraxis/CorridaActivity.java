package com.example.joaoparracho.peddypraxis;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.example.joaoparracho.peddypraxis.model.CountDownTimer2;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Timestamp;
import java.util.Locale;

public class CorridaActivity extends AppCompatActivity {
    private static final String TAG = "CorridaActivity";
    private TextView timeTextView;
    private long mTimeInMillis = 60 * 500;
    private CountDownTimer2 m2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descompressao);

        timeTextView = (TextView) findViewById(R.id.tvTime);

        m2 = new CountDownTimer2(mTimeInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                if (!Singleton.getInstance().isbInEsslei()) {
                    mTimeInMillis = millisUntilFinished;
                } else {
                    m2.cancel();
                    endCorrida();
                }
                timeTextView.setText(updateCountDownText());
            }

            @Override
            public void onFinish() {
                if (Singleton.getInstance().isbInEsslei()) {
                    endCorrida();
                } else {
                    finish();
                    startActivity(new Intent(CorridaActivity.this, PreambuloActivity.class));
                }
            }
        }.start();
    }

    private void endCorrida() {
        Singleton.getInstance().setbInEsslei(false);
        timeTextView.setText(R.string.finish);
        Singleton.getInstance().setNumTasksComplete(Singleton.getInstance().getNumTasksComplete() + 1);
        Singleton.getInstance().setActivityKey("finishGameKey");
        finish();
        startActivity(new Intent(CorridaActivity.this, PreambuloActivity.class));
    }

    private String updateCountDownText() {
        String timeLeftFormatted;
        int hours = (int) (mTimeInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeInMillis / 1000) % 60;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%02d:%02d", minutes, seconds);
        }
        return timeLeftFormatted;
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
                        Log.d(TAG, text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be removed: " + e.getMessage();
                        Log.d(TAG, text);
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
                        }
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "> Fences' states:\n" + (fenceInfo.equals("") ?
                                "No registered fences." : fenceInfo);
                        Log.d(TAG, text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be queried: " + e.getMessage();
                        Log.d(TAG, text);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back button pressed");
        showDialogWaring();
    }

    public void showDialogWaring() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.endTask);
        alert.setMessage(R.string.descCorrida);
        alert.setPositiveButton(R.string.endTask, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m2.cancel();
                finish();
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.create().show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        queryFences();
    }

    @Override
    public void onResume() {
        super.onResume();
        queryFences();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       // removeFences("locationFenceKey");
        //removeFences("essleiFenceKey");
    }

    @Override
    public void onStop() {
        queryFences();
        super.onStop();
    }
}
