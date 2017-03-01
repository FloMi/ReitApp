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

        double a = intent.getDoubleExtra("currentloclat",-1.0);
        double s = intent.getDoubleExtra("currentloclong",-1.0);

        activity.displayMyCurrentLocationOverlay(intent.getDoubleExtra("currentloclat",-1.0),intent.getDoubleExtra("currentloclong",-1.0));
        activity.calcWayToGoal(intent.getDoubleExtra("currentloclat",-1.0),intent.getDoubleExtra("currentloclong",-1.0));
        activity.startRecordingHike(intent.getDoubleExtra("currentloclat",-1.0),intent.getDoubleExtra("currentloclong",-1.0),intent.getStringExtra("currentlocProvider"));
        activity.gotOffCourse(intent.getDoubleExtra("currentloclat",-1.0),intent.getDoubleExtra("currentloclong",-1.0));
    }
}
