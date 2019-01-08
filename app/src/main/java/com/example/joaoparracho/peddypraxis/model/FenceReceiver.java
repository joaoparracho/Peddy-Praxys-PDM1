package com.example.joaoparracho.peddypraxis.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
                        Singleton.getInstance().setFenceBool( true);
                        //Snackbar.make(findViewById(android.R.id.content), "HEYY", Snackbar.LENGTH_LONG).show();
                        break;
                    case FenceState.FALSE:
                        Singleton.getInstance().setFenceBool( false);
                        //Snackbar.make(findViewById(android.R.id.content), "HOOO", Snackbar.LENGTH_LONG).show();
                        break;
                    case FenceState.UNKNOWN:
                        Singleton.getInstance().setFenceBool( false);
                        //Snackbar.make(findViewById(android.R.id.content), "Unk", Snackbar.LENGTH_LONG).show();
                        break;
                }
                break;
            case "notWalkingFenceKey":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Singleton.getInstance().setNotWalkinBool(true);
                        //Snackbar.make(findViewById(android.R.id.content), "Walk Bitch", Snackbar.LENGTH_LONG).show();
                        break;
                    case FenceState.FALSE:
                        Singleton.getInstance().setNotWalkinBool(false);
                        // Snackbar.make(findViewById(android.R.id.content), "Good Job", Snackbar.LENGTH_LONG).show();
                        break;
                }
                break;
        }
    }
}