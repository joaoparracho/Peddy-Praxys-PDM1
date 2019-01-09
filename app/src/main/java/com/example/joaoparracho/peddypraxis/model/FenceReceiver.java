package com.example.joaoparracho.peddypraxis.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
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
            case "notWalkingFenceKey":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Singleton.getInstance().setNotWalkinBool(true);
                        break;
                    case FenceState.FALSE:
                        Singleton.getInstance().setNotWalkinBool(false);
                        break;
                }
                break;
        }
    }
}