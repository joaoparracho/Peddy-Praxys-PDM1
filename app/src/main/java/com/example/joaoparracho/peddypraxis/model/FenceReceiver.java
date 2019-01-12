package com.example.joaoparracho.peddypraxis.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.PreambuloActivity;
import com.google.android.gms.awareness.fence.FenceState;

public class FenceReceiver extends BroadcastReceiver {

    public static final String TAG = "fences";
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(FENCE_RECEIVER_ACTION)) {
            Log.e(TAG, "Received an unsupported action in FenceReceiver: action=" + intent.getAction());
            return;
        }
        FenceState fenceState = FenceState.extract(intent);
        Log.i(TAG, "fences " + fenceState);
        switch (fenceState.getFenceKey()) {
            case "locationFenceKey":
                Singleton.getInstance().setFenceBool(fenceState.getCurrentState() == FenceState.TRUE);
                break;
            case "libLocationFenceKey":
                Singleton.getInstance().setbLibLoc(fenceState.getCurrentState() == FenceState.TRUE);
                break;
            case "rotALocationFenceKey":
                Singleton.getInstance().setbInRotA(fenceState.getCurrentState() == FenceState.TRUE);
                break;
            case "essleiFenceKey":
                Singleton.getInstance().setbInEsslei(fenceState.getCurrentState() == FenceState.TRUE);
                break;
            case "notWalkingFenceKey":
                Singleton.getInstance().setNotWalkinBool(fenceState.getCurrentState() == FenceState.FALSE);
                if (fenceState.getCurrentState() == FenceState.TRUE) {
                    Singleton.getInstance().setActivityKey("finishGameKey");
                    context.startActivity(new Intent(context, PreambuloActivity.class));
                }
                break;
            case "timeFence100Key":
                if (fenceState.getCurrentState() == FenceState.TRUE) Log.d(TAG, "timefence100 " + fenceState.getCurrentState());
                else {
                    Singleton.getInstance().setActivityKey("finishGameKey");
                    context.startActivity(new Intent(context, PreambuloActivity.class));
                    Log.d(TAG, "timefence100 " + fenceState.getCurrentState());
                }
                break;
            case "timeFence50Key":
                if (fenceState.getCurrentState() == FenceState.FALSE) Toast.makeText(context, "Ja vens em metade do tempo  oh CAMPEAO!", Toast.LENGTH_LONG).show();
                break;
            case "timeFence90Key":
                if (fenceState.getCurrentState() == FenceState.FALSE) Toast.makeText(context, "Ui UI que vai acabar!", Toast.LENGTH_LONG).show();
                break;
        }
    }
}