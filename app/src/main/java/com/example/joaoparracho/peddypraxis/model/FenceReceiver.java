package com.example.joaoparracho.peddypraxis.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.BibliotecaActivity;
import com.example.joaoparracho.peddypraxis.DescompressaoActivity;
import com.example.joaoparracho.peddypraxis.GameScreenActivity;
import com.example.joaoparracho.peddypraxis.LoginActivity;
import com.example.joaoparracho.peddypraxis.PatioActivity;
import com.example.joaoparracho.peddypraxis.PreambuloActivity;
import com.google.android.gms.awareness.fence.FenceState;

public class FenceReceiver extends BroadcastReceiver {

    public static final String TAG_FENCES = "fences";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(FENCE_RECEIVER_ACTION)) {
            Log.e(TAG_FENCES, "Received an unsupported action in FenceReceiver: action="
                    + intent.getAction());
            return;
        }
        FenceState fenceState = FenceState.extract(intent);
        switch (fenceState.getFenceKey()) {
            case "locationFenceKey":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Singleton.getInstance().setFenceBool(true);
                        Log.d("xxxfences", "locationFenceKey " + fenceState.getCurrentState());
                        break;
                    case FenceState.FALSE:
                        Singleton.getInstance().setFenceBool(false);
                        Log.d("xxxfences", "locationFenceKey " + fenceState.getCurrentState());
                        break;
                    case FenceState.UNKNOWN:
                        break;
                }
                break;
            case "libLocationFenceKey":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Singleton.getInstance().setbLibLoc(true);
                        break;
                    case FenceState.FALSE:
                        Singleton.getInstance().setbLibLoc(false);
                        break;
                    case FenceState.UNKNOWN:
                        break;
                }
                break;
            case "essleiFenceKey":
                Singleton.getInstance().setbInEsslei(fenceState.getCurrentState()==FenceState.TRUE);
                break;
            case "notWalkingFenceKey":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Singleton.getInstance().setActivityKey("finishGameKey");
                        context.startActivity(new Intent(context,PreambuloActivity.class));
                        Singleton.getInstance().setNotWalkinBool(true);
                        break;
                    case FenceState.UNKNOWN:
                    case FenceState.FALSE:
                        Singleton.getInstance().setNotWalkinBool(false);
                        break;
                }
                break;
            case "timeFence100Key":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.d("xxxfences", "timefence100 " + fenceState.getCurrentState());
                        break;
                    case FenceState.FALSE:
                        Singleton.getInstance().setActivityKey("finishGameKey");
                        context.startActivity(new Intent(context,PreambuloActivity.class));
                        Log.d("xxxfences", "timefence100 " + fenceState.getCurrentState());
                        break;
                }
                break;
            case "timeFence50Key":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        break;
                    case FenceState.FALSE:
                        Toast.makeText(context, "Ja vens em metade do tempo  oh CAMPEAO!", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            case "timeFence90Key":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        break;
                    case FenceState.FALSE:
                        Toast.makeText(context, "Ui UI que vai acabar!", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
        }
    }
}