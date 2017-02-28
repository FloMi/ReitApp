package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by FlorianM on 28.02.2017.
 */

public class LocationReceiver extends BroadcastReceiver {
    private Context context;
    private MapActivity activity;
    public LocationReceiver(Context context) {
        this.context = context;
        this.activity = (MapActivity) context;
    }

    public LocationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        activity.displayMyCurrentLocationOverlay(intent.getStringExtra("currentloclat"),intent.getStringExtra("currentloclong"));
        activity.calcWayToGoal(intent.getStringExtra("currentloclat"),intent.getStringExtra("currentloclong"));
        activity.startRecordingHike(intent.getStringExtra("currentloclat"),intent.getStringExtra("currentloclong"),intent.getStringExtra("currentlocProvider"));
        activity.gotOffCourse(intent.getStringExtra("currentloclat"),intent.getStringExtra("currentloclong"));
    }
}
