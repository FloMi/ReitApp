package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by FlorianM on 28.02.2017.
 */

public class LocationReceiver extends BroadcastReceiver {
    private Context context;
    private MapActivity activity;
    List<GeoPoint> MovedDistance = new ArrayList<>();

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
        activity.calcWayToGoal();
        //activity.startRecordingHike();
        activity.gotOffCourse(intent.getDoubleExtra("currentloclat",-1.0),intent.getDoubleExtra("currentloclong",-1.0));
        activity.checkIfTourFinished();
    }
}
