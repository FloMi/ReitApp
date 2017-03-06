package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
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
    List<GeoPoint> DatabaseCoordinates1 = new ArrayList<>();

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
    private String routID = "0";
    private LocationReceiver locationreceiver;
    private Timer timer;
    private String routName = "nan";


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if(FirebaseAuth.getInstance().getCurrentUser()==null)
        {
            Intent intent = new Intent(this, LoginActivity.class);
            finish();
            startActivity(intent);
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        InitAttraction();
        SetMap();
        checkIfAdminLoggedIn();

        //OrientationProvider o = new OrientationProvider(this);
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
        }

        startRecord.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (recordingStarted) {
                    exportToKml();
                } else {
                    recordingStarted = true;
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

        startNav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                timer = new Timer(getActivity());

                if (navigationStarted) {
                    startNav.setImageResource(R.drawable.ic_navigation_arrow);
                    navigationStarted = false;

                    distanceleft.setText("");

                    clearMap();

                    timer.resetClick();
                    timer.stopClick();
                    currenttour.setVisibility(View.INVISIBLE);
                    distanceofrout.setVisibility(View.INVISIBLE);
                    findViewById(R.id.timer).setVisibility(View.INVISIBLE);


                } else {
                    startNavigation();
                    startNav.setImageResource(R.drawable.ic_stopnav);
                    timer.resetClick();
                    timer.startClick();
                    findViewById(R.id.timer).setVisibility(View.VISIBLE);
                    currenttour.setVisibility(View.VISIBLE);
                    distanceofrout.setVisibility(View.VISIBLE);
                    navigationStarted = true;

                }


            }
        });

        routID = intent.getStringExtra(TourActivity.EXTRA_MESSAGE);

        Animation slide_down = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_down);

        Animation slide_up = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_up);

        if (routID != null) {

            //slide up navigationLayout
            findViewById(R.id.layout).setVisibility(View.VISIBLE);
            findViewById(R.id.layout).startAnimation(slide_up);
            findViewById(R.id.startrout).setVisibility(View.VISIBLE);
            findViewById(R.id.startrout).startAnimation(slide_up);

            String[] s = routID.split(":");

            routID = (valueOf(s[0])) + "";
            routName = s[1];


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
        registerReceiver(locationreceiver,fi);


        startService(i);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(ContextCompat.getDrawable(this,R.drawable.ic_brightness_1_black_24dp));


    }

    private void checkIfAdminLoggedIn() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user.getEmail().equals("florian.mittlboeck25@gmail.com"))
        {
            findViewById(R.id.startrecording).setVisibility(View.VISIBLE);
        }
    }

    private MapActivity getActivity()
    {
        return this;
    }


    private void exportToKml() {

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

                File file;
                String s = input.getText().toString();
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");

                KmlDocument kmlDocument = new KmlDocument();

                kmlDocument.mKmlRoot.addOverlay(CoverdTrack, kmlDocument);

                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/saved_routs");
                myDir.mkdirs();

                file = new File(myDir, s + ".kml");

                kmlDocument.saveAsKML(file);
                recordingStarted = false;
                startRecord.setImageResource(R.drawable.ic_action_name);

            }
        });

        builder.setNegativeButton("Weiter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                recordingStarted = true;
                startRecord.setImageResource(R.drawable.ic_save);
            }
        });

        builder.setNeutralButton("Löschen", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
            recordingStarted = false;
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
                int tourString;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    tourString = valueOf(postSnapshot.getKey());

                    if (tourString == valueOf(routID))
                    {
                        postSnapshot.child("Coordinates").getChildren();

                        for (DataSnapshot ps : postSnapshot.child("Coordinates").getChildren()) {
                            Double l = Double.parseDouble(ps.child("geoLength").getValue().toString());
                            Double w = Double.parseDouble(ps.child("geoWidth").getValue().toString());

                            GeoPoint g = new GeoPoint(w, l);
                            DatabaseCoordinates.add(g);
                            DatabaseCoordinates1.add(g);

                        }

                    }
                }
                drawPath();
                //DatabaseCoordinates = Collections.unmodifiableList(DatabaseCoordinates);
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

                    PointOfInterest p = new PointOfInterest(valueOf(postSnapshot.getKey()), Double.parseDouble(postSnapshot.child("geoWidth").getValue().toString()),Double.parseDouble(postSnapshot.child("geoLength").getValue().toString()),postSnapshot.child("Name").getValue().toString());
                    pointOfInterests.add(p);
                }

                for(PointOfInterest i :pointOfInterests)
                {
                    Marker cc;
                    cc = new Marker(map);
                    map.getOverlays().add(cc);

                    cc.setIcon(getResources().getDrawable(R.drawable.pointofinterest));

                    GeoPoint g = new GeoPoint(i.getLatitude(),i.getLongitude());

                    cc.setPosition(g);

                    cc.setTitle(i.getName()+"");
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

    public void displayMyCurrentLocationOverlay(double lat,double longi) {



        GeoPoint Location = new GeoPoint(lat,longi);

        if (currentLocation == null) {
            currentLocation = Location;
        } else {
            if (calcDistanceFromTo(Location, currentLocation) > 10 && calcDistanceFromTo(Location, currentLocation) < 500) {
                currentLocation = Location;
            }
        }

        map.getOverlays().remove(currentLocationMarker);

        currentLocationMarker.setPosition(currentLocation);
        currentLocationMarker.setTitle("You");
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        map.getOverlays().add(currentLocationMarker);

        if (centerMap) {
            mMapController.animateTo(currentLocation);
        }

        //create instructions if nav startet and databse loaded coordinates and updates distance moved
        if (navigationStarted && DatabaseCoordinates.size() > 0) {

            //check if at start of rout
            float d = calcDistanceFromTo(currentLocation, DatabaseCoordinates.get(0));
            if(d<20 && d >0)
            {
                atStartOfRout = true;
            }
            else
            {
                atStartOfRout = false;
            }

            crateInstructions();

            distanceMovedSinceStart += calcDistanceOfRout(MovedDistance) - distanceMovedSinceStart;

            if (distanceMovedSinceStart > 100) {
                addRangeToday(distanceMovedSinceStart);
                distanceMovedSinceStart = 0;
            }
        }


    }

    public void calcWayToGoal() {


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
        if (navigationStarted) {
            if (Math.abs(calcDistanceOfRout(MovedDistance) - calcDistanceOfRout(DatabaseCoordinates)) < 50) {
                Location loc1 = new Location("");
                loc1.setLatitude(DatabaseCoordinates.get(DatabaseCoordinates.size()).getLatitude());
                loc1.setLongitude(DatabaseCoordinates.get(DatabaseCoordinates.size()).getLongitude());

                Location loc2 = new Location("");
                loc2.setLatitude(currentLocation.getLatitude());
                loc2.setLongitude(currentLocation.getLongitude());

                if (loc1.distanceTo(loc2) <= 15) {
                    addFinishedTour(valueOf(routID));
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

    public void startRecordingHike(double lat,double longi ,String s) {

        GeoPoint cl = new GeoPoint(lat,longi);

        if (navigationStarted) {
            if (s.equals("gps")) {
                if (MovedDistance.size() == 0) {
                    //mMapController.animateTo(currentLocation);
                    MovedDistance.add(currentLocation);
                } else {
                    float d = currentLocation.distanceTo(MovedDistance.get(MovedDistance.size() - 1));

                    if (d > (float) 5) {

                        //mMapController.animateTo(currentLocation);
                        MovedDistance.add(currentLocation);
                    }
                }
            }

            Polyline l = new Polyline();
            l.setColor(Color.argb(255, 138, 152, 31));
            l.setWidth(20);

            l.setPoints(MovedDistance);

            map.getOverlays().remove(l);
            CoverdTrack = l;
            map.getOverlays().add(CoverdTrack);
            map.invalidate();
        } else if (recordingStarted == false) {
            MovedDistance.clear();
        }

        if (recordingStarted) {
            if (s.equals("gps")) {
                if (MovedDistance.size() == 0) {
                    //mMapController.animateTo(currentLocation);
                    MovedDistance.add(currentLocation);
                } else {
                    float d = currentLocation.distanceTo(MovedDistance.get(MovedDistance.size() - 1));

                    if (d > (float) 1) {
                        //mMapController.animateTo(currentLocation);
                        MovedDistance.add(currentLocation);
                    }
                }
            }

            Polyline l = new Polyline();
            l.setColor(Color.argb(255, 138, 152, 31));
            l.setWidth(20);

            l.setPoints(MovedDistance);

            map.getOverlays().remove(l);
            CoverdTrack = l;
            map.getOverlays().add(CoverdTrack);
            map.invalidate();
        } else if (navigationStarted == false) {
            MovedDistance.clear();
        }
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
        if (points.get(0) != currentLocation)
        {
            points.add(0, currentLocation);
        }
        return points;
    }

    private void startNavigation() {

        InitTourList();

        if (navigationStarted) {
            navigationStarted = false;
        } else {
            navigationStarted = true;
        }
    }

    private void openUserActivity() {
        navigationStarted = false;
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    private void drawPath() {

        mainPolyline.setColor(Color.argb(255, 138, 152, 31));
        mainPolyline.setWidth(20);

        mainPolyline.setPoints(DatabaseCoordinates);
        distanceofrout.setText(routName + " (" + GetDistanceString((double) calcDistanceOfRout(DatabaseCoordinates)) + ")");

        clearMap();

        map.getOverlays().add(mainPolyline);
        //crateInstructions();
        map.invalidate();

    }

    private void clearMap() {

        map.getOverlays().remove(mainPolyline);

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

    private void crateInstructions() {
        if (DatabaseCoordinates.size() > 0 && navigationStarted && currentLocation != null) {
            //if(currentLocation.distanceTo(DatabaseCoordinates.get(0))>0 && currentLocation.distanceTo(DatabaseCoordinates.get(0))<300)
            //{
            distanceleft.setVisibility(View.VISIBLE);

            if (atStartOfRout) {
                distanceleft.setText("Zum Ziel: " + GetDistanceString((DistanceToGoal)));
            } else {
                float distance = currentLocation.distanceTo(DatabaseCoordinates.get(0));
                distanceleft.setText("Zum Start: " + GetDistanceString(((double) distance)));
                //}
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
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        IntentFilter fi = new IntentFilter("koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp.LOCATION_CHANGED");
        registerReceiver(locationreceiver,fi);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(locationreceiver);
        super.onPause();

    }

    public void gotOffCourse(double lat, double longi) {

        GeoPoint current = new GeoPoint(lat,longi);

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
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        database.child("Users")
                .child(user.getUid())
                .child(stats).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int stat = Integer.parseInt(dataSnapshot.getValue().toString());
                stat++;
                database.child("Users").child(user.getUid()).child(stats).setValue(stat);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Fehler beim Übertragen einer Statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addFinishedTour(final int id) {
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        database.child("Users")
                .child(user.getUid())
                .child("whichTourFinished")
                .push().setValue(id);
    }
    
    //Timer

    public void addRangeToday(final double range) {
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final Date today = new Date();
        final SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");

        database.child("Users")
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
                database.child("Users").child(user.getUid()).child("rangePerDay").child(f.format(today)).setValue(rangeNow);
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
                stat+=range;
                database.child("Users").child(user.getUid()).child("range").setValue(stat);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Fehler beim Übertragen einer Statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public com.google.firebase.appindexing.Action getIndexApiAction0() {
        return Actions.newView("Map", "http://[ENTER-YOUR-URL-HERE]");
    }
}