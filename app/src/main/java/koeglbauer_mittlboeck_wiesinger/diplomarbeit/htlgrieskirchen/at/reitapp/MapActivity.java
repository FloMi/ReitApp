package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Integer.valueOf;

public class
MapActivity extends Activity {

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    GeoPoint currentLocation;
    List<GeoPoint> MovedDistance = new ArrayList<>();
    ArrayList<GeoPoint> PolylineWaypoints = new ArrayList<>();
    TextView distanceofrout;
    TextView distanceleft;
    Marker currentLocationMarker;
    Polyline CoverdTrack;
    List<GeoPoint> DatabaseCoordinates = new ArrayList<>();

    Polyline mainPolyline = new Polyline();
    FloatingActionButton startRecord;
    double distanceMovedSinceStart = 0;
    private TextView currenttour;
    private Double DistanceToGoal = 0.0;
    private MapView map;
    private MapController mMapController;
    private GoogleApiClient client;

    private boolean navigationStarted = false;
    private boolean recordingStarted = false;

    private boolean centerMap = true;
    private boolean atStartOfRout = true;
    private DatabaseReference mDatabase;
    ArrayList<PointOfInterest> pointOfInterests = new ArrayList<>();
    private LocationReceiver locationreceiver;
    private Timer timer;
    private String routName = "nan";
    private SharedPreferences pref;

    private SensorManager sensorManager;
    private long lastUpdate;
    Boolean bool = false, bool1 = false;

    private static SensorManager sensorService;
    private Sensor sensor;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        if (sensor != null) {
            sensorService.registerListener(mySensorEventListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            Log.i("Compass MainActivity", "Registerered for ORIENTATION Sensor");
        } else {
            Log.e("Compass MainActivity", "Registerered for ORIENTATION Sensor");
            Toast.makeText(this, "ORIENTATION Sensor not found",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            finish();
            startActivity(intent);
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        InitAttraction();
        SetMap();
        checkIfAdminLoggedIn();

        //SensorProvider o = new SensorProvider(this);
        //  o.getOrientation();

        distanceofrout = (TextView) findViewById(R.id.distanceofrout);
        distanceleft = (TextView) findViewById(R.id.distanceleft);
        currenttour = (TextView) findViewById(R.id.currenttour);
        startRecord = (FloatingActionButton) findViewById(R.id.startrecording);
        final FloatingActionButton startNav = (FloatingActionButton) findViewById(R.id.startrout);
        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.start);
        final FloatingActionButton centermap = (FloatingActionButton) findViewById(R.id.centermap);


        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
            map.invalidate();
        }

        startRecord.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (pref.getBoolean("recordingStarted", false)) {
                    exportToDatabase();

                } else {
                    SQLiteDatabase db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();
                    db.execSQL(TablePoints.SQL_CREATE);
                    pref.edit().putBoolean("recordingStarted", true).apply();
                    startRecord.setImageResource(R.drawable.ic_save);
                }
            }
        });

        Intent intent = getIntent();

        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openUserActivity();
                stopService(new Intent(MapActivity.this, LocationService.class));
            }
        });

        centermap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (centerMap) {
                    centermap.setImageResource(R.drawable.ic_notcentered);
                    centerMap = false;
                } else {
                    centermap.setImageResource(R.drawable.ic_centered);
                    centerMap = true;

                }
            }
        });

        if (pref.getBoolean("navigationStarted", false)) {
            InitTourList();

            //SQLiteDatabase db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();
            //db.execSQL(TablePoints.SQL_CREATE);
            startNav.setImageResource(R.drawable.ic_stopnav);
            timer = new Timer(getActivity());
            timer.resetClick();
            timer.startClick();
            currenttour.setVisibility(View.VISIBLE);
            distanceofrout.setVisibility(View.VISIBLE);
        }

        startNav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (pref.getBoolean("navigationStarted", false)) {
                    startNav.setImageResource(R.drawable.ic_navigation_arrow);

                    distanceleft.setText("");

                    clearMap();

                    stopRecordingHike();

                    timer = new Timer(getActivity());
                    timer.resetClick();
                    timer.stopClick();
                    currenttour.setVisibility(View.INVISIBLE);
                    distanceofrout.setVisibility(View.INVISIBLE);
                    findViewById(R.id.timer).setVisibility(View.INVISIBLE);
                    pref.edit().putBoolean("navigationStarted", false).apply();

                } else {

                    InitTourList();

                    SQLiteDatabase db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();
                    db.execSQL(TablePoints.SQL_CREATE);

                    startNav.setImageResource(R.drawable.ic_stopnav);
                    timer = new Timer(getActivity());
                    timer.resetClick();
                    timer.startClick();

                    distanceleft.setVisibility(View.VISIBLE);

                    distanceleft.setText("Berechnung läuft");

                    pref.edit().putBoolean("navigationStarted", true).apply();
                }
            }
        });

        if (intent.getStringExtra(TourActivity.EXTRA_MESSAGE) != null) {
            pref.edit().putString("routID", intent.getStringExtra(TourActivity.EXTRA_MESSAGE)).apply();
        }

        Animation slide_down = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_down);

        Animation slide_up = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_up);

        if (pref.getString("routID", null) != null) {

            //slide up navigationLayout
            findViewById(R.id.layout).setVisibility(View.VISIBLE);
            findViewById(R.id.layout).startAnimation(slide_up);
            findViewById(R.id.startrout).setVisibility(View.VISIBLE);
            findViewById(R.id.startrout).startAnimation(slide_up);

            startNav.setVisibility(View.VISIBLE);


        } else {

            //slide down navigationLayout
            findViewById(R.id.layout).setVisibility(View.GONE);
            findViewById(R.id.layout).startAnimation(slide_down);
            findViewById(R.id.startrout).startAnimation(slide_down);
            startNav.setVisibility(View.INVISIBLE);
            currenttour.setVisibility(View.INVISIBLE);
        }

        OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);


        map.setBuiltInZoomControls(false);


        locationreceiver = new LocationReceiver(this);
        Intent i = new Intent(this, LocationService.class);
        IntentFilter fi = new IntentFilter("koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp.LOCATION_CHANGED");
        registerReceiver(locationreceiver, fi);


        startService(i);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_navigation_arrow_black));


    }

    private void stopRecordingHike() {

        if (pref.getBoolean("navigationStarted", false) || pref.getBoolean("recordingStarted", false)) {
            SQLiteDatabase db = new SQLiteHelper(this).getReadableDatabase();
            db.execSQL(TablePoints.SQL_DROP);
        }
    }

    private void checkIfAdminLoggedIn() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user.getEmail().equals("florian.mittlboeck25@gmail.com")) {
            findViewById(R.id.startrecording).setVisibility(View.VISIBLE);
        }
    }

    private MapActivity getActivity() {
        return this;
    }


    private void exportToDatabase() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                mDatabase.child("Paths").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        SQLiteDatabase db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();

                        List<Coordinate> movedDistance = new ArrayList<>();
                        List<GeoPoint> movedDistanceGeoPoints = new ArrayList<>();

                        Cursor cursor = db.query(TablePoints.TABLE_NAME, new String[]{TablePoints.Latitude, TablePoints.Longitude}, null, null, null, null, null);

                        while (cursor.moveToNext()) {
                            Coordinate c = new Coordinate(cursor.getFloat(1), cursor.getFloat(0));
                            GeoPoint g = new GeoPoint(cursor.getFloat(1), cursor.getFloat(0));
                            movedDistanceGeoPoints.add(g);
                            movedDistance.add(c);
                        }

                        clearMap();
                        stopRecordingHike();
                        pref.edit().putBoolean("recordingStarted", false).apply();

                        if (CoverdTrack.getNumberOfPoints() > 0) {
                            Path p = new Path(movedDistance, input.getText().toString(), (int) calcDistanceOfRout(movedDistanceGeoPoints));
                            mDatabase.child("Paths").push().setValue(p);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(MapActivity.this, R.string.toast_show_tours_failed, Toast.LENGTH_SHORT).show();
                    }
                });

                startRecord.setImageResource(R.drawable.ic_action_name);

            }
        });

        builder.setNegativeButton("Weiter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                pref.edit().putBoolean("recordingStarted", true).apply();

                startRecord.setImageResource(R.drawable.ic_save);
            }
        });

        builder.setNeutralButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                stopRecordingHike();
                clearMap();

                pref.edit().putBoolean("recordingStarted", false).apply();

                startRecord.setImageResource(R.drawable.ic_action_name);
            }
        });
        builder.show();


    }

    private void InitTourList() {

        DatabaseCoordinates.clear();
        mDatabase.child("Paths").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String tourString;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    tourString = postSnapshot.getKey().toString();

                    if (tourString.equals(pref.getString("routID", null))) {

                        if (postSnapshot.child("Name").getValue() == null)
                            routName = postSnapshot.child("name").getValue().toString();
                        else
                            routName = postSnapshot.child("Name").getValue().toString();


                        for (DataSnapshot ps : postSnapshot.child("coordinates").getChildren()) {
                            Double l = Double.parseDouble(ps.child("latitude").getValue().toString());
                            Double w = Double.parseDouble(ps.child("longitude").getValue().toString());

                            GeoPoint g = new GeoPoint(w, l);
                            DatabaseCoordinates.add(g);
                        }
                    }
                }
                drawPath();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, R.string.toast_show_tours_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void InitAttraction() {
        pointOfInterests.clear();
        mDatabase.child("Attractions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {


                    PointOfInterest p = new PointOfInterest(postSnapshot.getKey(), Double.parseDouble(postSnapshot.child("geoLength").getValue().toString()), Double.parseDouble(postSnapshot.child("geoWidth").getValue().toString()), postSnapshot.child("Name").getValue().toString());
                    pointOfInterests.add(p);

                }

                for (PointOfInterest i : pointOfInterests) {
                    Marker cc;
                    cc = new Marker(map);
                    map.getOverlays().add(cc);

                    cc.setIcon(getResources().getDrawable(R.drawable.pointofinterest));

                    GeoPoint g = new GeoPoint(i.getLatitude(), i.getLongitude());

                    cc.setPosition(g);

                    cc.setTitle(i.getName() + "");
                    cc.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(currentLocationMarker);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, R.string.toast_show_tours_failed, Toast.LENGTH_SHORT).show();
            }
        });

    }


    public void displayMyCurrentLocationOverlay(double lat, double longi) {

        GeoPoint Location = new GeoPoint(lat, longi);
        currentLocation = Location;

        map.getOverlays().remove(currentLocationMarker);

        currentLocationMarker.setPosition(currentLocation);
        currentLocationMarker.setTitle("You");
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        map.getOverlays().add(currentLocationMarker);

        if (centerMap) {
            mMapController.animateTo(currentLocation);
        }

        //create instructions if nav startet and databse loaded coordinates and updates distance moved
        if (pref.getBoolean("navigationStarted", true) && DatabaseCoordinates.size() > 0) {

            //check if at start of rout
            float d = calcDistanceFromTo(currentLocation, DatabaseCoordinates.get(0));
            if (d < 50 && d > 0) {
                atStartOfRout = true;
            } else {
                atStartOfRout = false;
            }

            createInstructions();

            distanceMovedSinceStart += calcDistanceOfRout(MovedDistance) - distanceMovedSinceStart;

            if (distanceMovedSinceStart > 100) {
                addRangeToday(distanceMovedSinceStart);
                distanceMovedSinceStart = 0;
            }
        }


    }

    public void calcDistanceToGoal() {


        //if (!navigationStarted) return;

        DistanceToGoal = 0.0;

        if (DatabaseCoordinates.size() > 0) {

            List<GeoPoint> PointsToFinish = getRoutLeft(DatabaseCoordinates);

            for (int i = 0; i < PointsToFinish.size() - 1; i++) {

                Location actualLocation = new Location("actualLocation");

                actualLocation.setLatitude(PointsToFinish.get(i).getLatitude());
                actualLocation.setLongitude(PointsToFinish.get(i).getLongitude());

                Location nextLoaction = new Location("nextLoaction");

                nextLoaction.setLatitude(PointsToFinish.get(i + 1).getLatitude());
                nextLoaction.setLongitude(PointsToFinish.get(i + 1).getLongitude());

                DistanceToGoal = DistanceToGoal + (double) actualLocation.distanceTo(nextLoaction);
            }
        }

    }

    public float calcDistanceFromTo(GeoPoint locFrom, GeoPoint locTo) {


        Location actualLocation = new Location("actualLocation");

        actualLocation.setLatitude(locFrom.getLatitude());
        actualLocation.setLongitude(locFrom.getLongitude());

        Location nextLoaction = new Location("nextLoaction");

        nextLoaction.setLatitude(locTo.getLatitude());
        nextLoaction.setLongitude(locTo.getLongitude());

        return (actualLocation.distanceTo(nextLoaction));
    }

    public void checkIfTourFinished() {
        if (pref.getBoolean("navigationStarted", false) && DatabaseCoordinates.size() > 0) {
            if (Math.abs(calcDistanceOfRout(MovedDistance) - calcDistanceOfRout(DatabaseCoordinates)) < 50) {
                Location loc1 = new Location("");
                loc1.setLatitude(DatabaseCoordinates.get(DatabaseCoordinates.size() - 1).getLatitude());
                loc1.setLongitude(DatabaseCoordinates.get(DatabaseCoordinates.size() - 1).getLongitude());

                Location loc2 = new Location("");
                loc2.setLatitude(currentLocation.getLatitude());
                loc2.setLongitude(currentLocation.getLongitude());

                if (loc1.distanceTo(loc2) <= 15) {
                    addFinishedTour(pref.getString("routID", null));
                }
            }
        }
    }

    private float calcDistanceOfRout(List<GeoPoint> rout) {

        float distanceInMeters = 0;

        for (int i = 0; i < rout.size() - 1; i++) {

            Location loc1 = new Location("");
            loc1.setLatitude(rout.get(i).getLatitude());
            loc1.setLongitude(rout.get(i).getLongitude());

            Location loc2 = new Location("");
            loc2.setLatitude(rout.get(i + 1).getLatitude());
            loc2.setLongitude(rout.get(i + 1).getLongitude());

            distanceInMeters = +loc1.distanceTo(loc2);
        }

        return distanceInMeters;
    }

    public void drawRecordedPath() {


        if (pref.getBoolean("recordingStarted", false) || pref.getBoolean("navigationStarted", false)) {


            SQLiteDatabase db = new SQLiteHelper(this).getReadableDatabase();

            List<GeoPoint> movedDistance = new ArrayList<>();

            Cursor cursor = db.query(TablePoints.TABLE_NAME, new String[]{TablePoints.Latitude, TablePoints.Longitude}, null, null, null, null, null);

            while (cursor.moveToNext()) {
                movedDistance.add(new GeoPoint(cursor.getFloat(0), cursor.getFloat(1)));
            }

            cursor.close();
            db.close();

            Polyline l = new Polyline();
            l.setColor(Color.argb(255, 138, 152, 31));
            l.setWidth(20);

            l.setPoints(movedDistance);

            map.getOverlays().remove(l);
            CoverdTrack = l;
            map.getOverlays().add(CoverdTrack);
            map.invalidate();
        }
        /*
        if (pref.getBoolean("navigationStarted", false)) {
            MovedDistance.clear();
        }
        */
    }

    private String GetDistanceString(Double distance) {

        DecimalFormat df = new DecimalFormat("#");

        if (distance < 999.99999999999) return (df.format(distance) + " m");

        return df.format(distance / 1000) + "Km";
    }

    private List<GeoPoint> getRoutLeft(List<GeoPoint> p) {


        List<GeoPoint> points = new ArrayList<>(p);

        GeoPoint nextpoint = points.get(0);
        float smalestDistance = 10000000;
        //nähester punkt in der route zur aktuellen position
        for (GeoPoint i : points) {

            Location locationList = new Location("point List");

            locationList.setLatitude(i.getLatitude());
            locationList.setLongitude(i.getLongitude());

            Location locationCurrent = new Location("point currentlocatio");

            locationCurrent.setLatitude(currentLocation.getLatitude());
            locationCurrent.setLongitude(currentLocation.getLongitude());

            float distance = locationList.distanceTo(locationCurrent);

            if (distance < smalestDistance) {
                smalestDistance = distance;
                nextpoint = i;
            }
        }
        //lösche alle punkte vor aktueller position
        for (int l = 0; l <= points.indexOf(nextpoint) - 1; l++) {
            points.remove(l);
        }

        //aktuelle position anfang von route
        if (points.get(0) != currentLocation) {
            points.add(0, currentLocation);
        }
        return points;
    }

    private void openUserActivity() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    private SensorEventListener mySensorEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // angle between the magnetic north direction
            // 0=North, 90=East, 180=South, 270=West
            float azimuth = event.values[0];

            currentLocationMarker.setRotation(azimuth);


            map.invalidate();
        }
    };

    private void drawPath() {

        mainPolyline.setColor(Color.argb(255, 138, 152, 31));
        mainPolyline.setWidth(20);

        mainPolyline.setPoints(DatabaseCoordinates);
        String s = (routName + " (" + GetDistanceString((double) calcDistanceOfRout(DatabaseCoordinates)) + ")");

        distanceofrout.setText(s);

        clearMap();

        map.getOverlays().add(mainPolyline);
        //createInstructions();
        map.invalidate();

    }

    private void clearMap() {

        map.getOverlays().clear();

    }

    private void SetMap() {

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mMapController = (MapController) map.getController();
        mMapController.setZoom(17);
        GeoPoint s = new GeoPoint(48.092907, 13.565829);
        mMapController.setCenter(s);
    }

    private void createInstructions() {
        if (DatabaseCoordinates.size() > 0 && pref.getBoolean("navigationStarted", true) && currentLocation != null) {

            findViewById(R.id.timer).setVisibility(View.VISIBLE);
            currenttour.setVisibility(View.VISIBLE);
            distanceofrout.setVisibility(View.VISIBLE);

            if (atStartOfRout && DistanceToGoal > 0) {
                distanceleft.setText("Zum Ziel: " + GetDistanceString(DistanceToGoal));
            } else {
                float distance = currentLocation.distanceTo(DatabaseCoordinates.get(0));
                distanceleft.setText("Zum Start: " + GetDistanceString((double) distance));
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = "osmdroid permissions:";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\nLocation to show user location.";
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            message += "\nStorage access to store map tiles.";
        }

        if (!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    map.invalidate();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Map Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        FirebaseUserActions.getInstance().start(getIndexApiAction0());
    }

    @Override
    public void onStop() {
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        FirebaseUserActions.getInstance().end(getIndexApiAction0());

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    public void onBackPressed() {
        // do nothing, because back should do nothing
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(MapActivity.this, LocationService.class));


        stopRecordingHike();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        IntentFilter fi = new IntentFilter("koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp.LOCATION_CHANGED");
        registerReceiver(locationreceiver, fi);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(locationreceiver);
        super.onPause();

    }

    public void gotOffCourse(double lat, double longi) {

        GeoPoint current = new GeoPoint(lat, longi);

        if (PolylineWaypoints.size() > 0) {
            float distance = 0;
            int count = 0;
            for (GeoPoint i : PolylineWaypoints) {

                Location actualLocation = new Location("actualLocation");

                actualLocation.setLatitude(currentLocation.getLatitude());
                actualLocation.setLongitude(currentLocation.getLongitude());

                Location anyLocation = new Location("nextLoaction");

                anyLocation.setLatitude(i.getLatitude());
                anyLocation.setLongitude(i.getLongitude());

                if (count == 0) {
                    distance = actualLocation.distanceTo(anyLocation);
                }

                if (actualLocation.distanceTo(anyLocation) < distance) {
                    distance = actualLocation.distanceTo(anyLocation);
                }
                count++;
            }
            if (distance > 50) {
                distanceleft.setText("Sie verlassen die Route! " + distance);
            }
        }
    }

    public void addStatistic(final String stats) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase.child("Users")
                .child(user.getUid())
                .child(stats).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int stat = Integer.parseInt(dataSnapshot.getValue().toString());
                stat++;
                mDatabase.child("Users").child(user.getUid()).child(stats).setValue(stat);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Fehler beim Übertragen einer Statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addFinishedTour(final String id) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase.child("Users")
                .child(user.getUid())
                .child("whichTourFinished")
                .push().setValue(id);
    }

    public void addRangeToday(final double range) {
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final Date today = new Date();
        final SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");

        mDatabase.child("Users")
                .child(user.getUid())
                .child("rangePerDay").child(f.format(today)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double rangeNow;
                if (dataSnapshot.getValue() != null)
                    rangeNow = Double.parseDouble(dataSnapshot.getValue().toString());
                else
                    rangeNow = 0.0;
                rangeNow += range;
                mDatabase.child("Users").child(user.getUid()).child("rangePerDay").child(f.format(today)).setValue(rangeNow);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Fehler beim Übertragen einer Statistik", Toast.LENGTH_SHORT).show();
            }
        });

        database.child("Users")
                .child(user.getUid())
                .child("range").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double stat = Double.parseDouble(dataSnapshot.getValue().toString());
                stat += range;
                database.child("Users").child(user.getUid()).child("range").setValue(stat);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Fehler beim Übertragen einer Statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public com.google.firebase.appindexing.Action getIndexApiAction0() {
        return Actions.newView("Map", "http://[ENTER-YOUR-URL-HERE]");
    }
}