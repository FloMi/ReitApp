package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
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
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import static java.lang.Integer.valueOf;

public class MapActivity extends Activity {

    //Timer
    private TextView tempTextView; //Temporary TextView
    private Button tempBtn; //Temporary Button
    private Handler mHandler = new Handler();
    private long startTime;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;
    private String hours, minutes, seconds, milliseconds;
    private long secs, mins, hrs;
    private boolean stopped = false;

    static GeoPoint currentLocation;
    static GeoPoint previousBestLocation;
    static List<GeoPoint> MovedDistance = new ArrayList<>();
    static ArrayList<GeoPoint> PolylineWaypoints = new ArrayList<>();

    static TextView distanceofrout;
    static TextView distanceleft;
    private static TextView currenttour;


    static private Double DistanceToGoal = 0.0;
    static Marker currentLocationMarker;

    static Polyline CoverdTrack;
    private static MapView map;
    private static MapController mMapController;
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private GoogleApiClient client;

    private static boolean navigationStarted = false;
    private static boolean recordingStarted = false;
    private static boolean centerMap = true;
    private static boolean atStartOfRout = true;

    private DatabaseReference mDatabase;

    Polyline response;

    static List<GeoPoint> DatabaseCoordinates = new ArrayList<>();
    private String routID = "0";
    private String routName = "nan";

    Polyline mainPolyline = new Polyline();

    FloatingActionButton startRecord;

    float distanceOfRout;



    MyLocationNewOverlay myLocationOverlay = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();

        addRangeToday(1.1);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        distanceofrout = (TextView) findViewById(R.id.distanceofrout);
        distanceleft = (TextView) findViewById(R.id.distanceleft);
        currenttour = (TextView) findViewById(R.id.currenttour);


        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }





        startRecord = (FloatingActionButton) findViewById(R.id.startrecording);

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

        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.start);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openUserActivity();
                stopService(new Intent(MapActivity.this, LocationService.class));
            }
        });


        final FloatingActionButton centermap = (FloatingActionButton) findViewById(R.id.centermap);
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


        final FloatingActionButton startNav = (FloatingActionButton) findViewById(R.id.startrout);
        startNav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (navigationStarted) {
                    startNav.setImageResource(R.drawable.ic_navigation_arrow);
                    navigationStarted = false;

                    distanceleft.setText("");

                    clearMap();
                    resetClick();
                    stopClick();
                    currenttour.setVisibility(View.INVISIBLE);
                    distanceofrout.setVisibility(View.INVISIBLE);

                    map.invalidate();
                } else {
                    startNavigation();
                    startNav.setImageResource(R.drawable.ic_stopnav);
                    startClick();
                    currenttour.setVisibility(View.VISIBLE);
                    distanceofrout.setVisibility(View.VISIBLE);
                    navigationStarted = true;

                }


            }
        });

        routID = intent.getStringExtra(TourActivity.EXTRA_MESSAGE);

        if (routID != null) {

            String[] s = routID.split(";");

            routID = (valueOf(s[0])) + "";
            routName = s[1].split(":")[1];


            startNav.setVisibility(View.VISIBLE);


        } else {
            startNav.setVisibility(View.INVISIBLE);
            currenttour.setVisibility(View.INVISIBLE);

        }


        OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

        SetMap();


        map.setBuiltInZoomControls(false);

        Intent i = new Intent(this, LocationService.class);
        startService(i);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_brightness_1_black_24dp));
    }


    private void exportToKml() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                StringBuilder str = new StringBuilder();
                File file;
                String s = input.getText().toString();
                String coordinatesString = "";
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(c.getTime());


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
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                recordingStarted = true;
                startRecord.setImageResource(R.drawable.ic_save);
            }
        });

        builder.show();


    }

    private void writeToFile(String filename, String data) {
        File file;
        FileOutputStream outputStream;
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/saved_routs");
            myDir.mkdirs();

            file = new File(myDir, filename + ".kml");

            outputStream = new FileOutputStream(file);
            outputStream.write(data.toString().getBytes());
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void InitTourList() {
        DatabaseCoordinates.clear();
        mDatabase.child("Paths").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int tourString;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    tourString = valueOf(postSnapshot.getKey());
                    String[] parts = routID.split(":");
                    if (tourString == valueOf(parts[0])) {
                        postSnapshot.child("Coordinates").getChildren();

                        for (DataSnapshot ps : postSnapshot.child("Coordinates").getChildren()) {
                            Double l = Double.parseDouble(ps.child("geoLength").getValue().toString());
                            Double w = Double.parseDouble(ps.child("geoWidth").getValue().toString());

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

    public static void displayMyCurrentLocationOverlay(GeoPoint Location) {

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

        if (navigationStarted && DatabaseCoordinates.size() > 0) {
            crateInstructions();
        }
    }

    private static boolean checkIfNextPositionIsValid(GeoPoint location) {

        if (calcDistanceFromTo(location,currentLocation)<50) {
            return true;
        }
        return false;
    }

    public static void calcWayToGoal(GeoPoint currentLocation) {

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


    public static float calcDistanceFromTo(GeoPoint locFrom, GeoPoint locTo) {


                Location actualLocation = new Location("actualLocation");

                actualLocation.setLatitude(locFrom.getLatitude());
                actualLocation.setLongitude(locFrom.getLongitude());

                Location nextLoaction = new Location("nextLoaction");

                nextLoaction.setLatitude(locTo.getLatitude());
                nextLoaction.setLongitude(locTo.getLongitude());

                return(actualLocation.distanceTo(nextLoaction));
            }


    public void checkIfTourFinished()
    {
        if (navigationStarted)
        {
            if(Math.abs(calcDistanceOfRout(MovedDistance) - calcDistanceOfRout(DatabaseCoordinates)) < 50 )
            {
                Location loc1 = new Location("");
                loc1.setLatitude(DatabaseCoordinates.get(DatabaseCoordinates.size()).getLatitude());
                loc1.setLongitude(DatabaseCoordinates.get(DatabaseCoordinates.size()).getLongitude());

                Location loc2 = new Location("");
                loc2.setLatitude(currentLocation.getLatitude());
                loc2.setLongitude(currentLocation.getLongitude());

                if (loc1.distanceTo(loc2) <= 15)
                {

                }
            }
        }
    }

    private float calcDistanceOfRout(List<GeoPoint> rout) {

        float distanceInMeters = 0;

        for ( int i = 0;i<rout.size()-1;i++){

            Location loc1 = new Location("");
            loc1.setLatitude(rout.get(i).getLatitude());
            loc1.setLongitude(rout.get(i).getLongitude());

            Location loc2 = new Location("");
            loc2.setLatitude(rout.get(i+1).getLatitude());
            loc2.setLongitude(rout.get(i+1).getLongitude());

            distanceInMeters =+ loc1.distanceTo(loc2);
        }

        return distanceInMeters;
    }

    public static void startRecordingHike(GeoPoint cl, String s) {

        if (navigationStarted)
        {
            if (s.equals("gps"))
            {
                if (MovedDistance.size() == 0)
                {
                    //mMapController.animateTo(currentLocation);
                    MovedDistance.add(currentLocation);
                }
                else
                {
                    float d = currentLocation.distanceTo(MovedDistance.get(MovedDistance.size()-1));

                    if (d>(float)5)
                    {

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
        }
        else if (recordingStarted == false)
        {
            MovedDistance.clear();
        }

        if (recordingStarted)
        {
            if (s.equals("gps"))
            {
                if (MovedDistance.size() == 0)
                {
                    //mMapController.animateTo(currentLocation);
                    MovedDistance.add(currentLocation);
                }
                else
                {
                    float d = currentLocation.distanceTo(MovedDistance.get(MovedDistance.size()-1));

                    if (d>(float)1)
                    {
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
        }
        else if (navigationStarted == false)
        {
            MovedDistance.clear();
        }
    }

    private static String GetDistanceString(Double distance) {

        DecimalFormat df = new DecimalFormat("#");

        if (distance < 999.99999999999) return (df.format(distance) + " m");

        return df.format(distance / 1000) + "Km";
    }

    private static List<GeoPoint> getRoutLeft(List<GeoPoint> points) {

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

        GeoPoint currentGHPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
        //aktuelle position anfang von route
        points.add(0, currentGHPoint);

        return points;
    }

    private void startNavigation() {

        InitTourList();

         if (navigationStarted)
         {
            navigationStarted=false;
         }
         else
         {
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
                distanceofrout.setText(routName+" ("+GetDistanceString((double)calcDistanceOfRout(DatabaseCoordinates))+")");

                clearMap();

                map.getOverlays().add(mainPolyline);
                crateInstructions();
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

    private static void crateInstructions() {
        if(DatabaseCoordinates.size()>0 && navigationStarted && currentLocation != null)
        {
            //if(currentLocation.distanceTo(DatabaseCoordinates.get(0))>0 && currentLocation.distanceTo(DatabaseCoordinates.get(0))<300)
            //{
                distanceleft.setVisibility(View.VISIBLE);

                if(atStartOfRout)
                {
                    float distance = currentLocation.distanceTo(DatabaseCoordinates.get(0));
                    distanceleft.setText("Zum Ziel: "+GetDistanceString((DistanceToGoal)));
                    distanceleft.invalidate();
                }
                else
                {
                    float distance = currentLocation.distanceTo(DatabaseCoordinates.get(0));
                    distanceleft.setText("Zum Start: "+GetDistanceString(((double) distance)));
                    distanceleft.invalidate();
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
    }

    @Override
    public void onStop() {
        super.onStop();

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

    public static void gotOffCourse(GeoPoint current) {
    if (PolylineWaypoints.size()>0)
    {
    float distance = 0;
    int count = 0;
    for (GeoPoint i :PolylineWaypoints)
    {

        Location actualLocation = new Location("actualLocation");

        actualLocation.setLatitude(currentLocation.getLatitude());
        actualLocation.setLongitude(currentLocation.getLongitude());

        Location anyLocation = new Location("nextLoaction");

        anyLocation.setLatitude(i.getLatitude());
        anyLocation.setLongitude(i.getLongitude());

        if (count == 0)
        {
            distance = actualLocation.distanceTo(anyLocation);
        }

        if (actualLocation.distanceTo(anyLocation) < distance)
        {
            distance = actualLocation.distanceTo(anyLocation);
        }
        count++;
    }
    if(distance> 50)
    {
        distanceleft.setText("Sie verlassen die Route! " + distance);
    }
}


    }

    public void addStatistic(final String stats)
    {
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
    public void addFinishedTour(final int id)
    {
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        database.child("Users")
                .child(user.getUid())
                .child("whichTourFinished")
                .push().setValue(id);
    }
    public void addRangeToday(final double range)
    {
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
                if(dataSnapshot.getValue()!=null)
                    rangeNow = Double.parseDouble(dataSnapshot.getValue().toString());
                else
                    rangeNow = 0.0;
                rangeNow+=range;
                database.child("Users").child(user.getUid()).child("rangePerDay").child(f.format(today)).setValue(rangeNow);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Fehler beim Übertragen einer Statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }





        //Timer

    public void stopClick (){
        hideTimer();
        mHandler.removeCallbacks(startTimer);
        stopped = true;
    }

    private void hideTimer() {
        ((TextView)findViewById(R.id.timer)).setVisibility(View.INVISIBLE);
    }


    public void startClick (){
        if(stopped){
            startTime = System.currentTimeMillis() - elapsedTime;
        }
        else{
            startTime = System.currentTimeMillis();
        }
        mHandler.removeCallbacks(startTimer);
        mHandler.postDelayed(startTimer, 0);
    }


    public void resetClick (){
        stopped = false;
        secs = 0;
        mins = 0;
        hrs = 0;
        ((TextView)findViewById(R.id.timer)).setText("00:00:00");
    }


    private Runnable startTimer = new Runnable() {
        public void run() {
            elapsedTime = System.currentTimeMillis() - startTime;
            updateTimer(elapsedTime);
            mHandler.postDelayed(this,REFRESH_RATE);
        }
    };






    private void updateTimer (float time){
        secs = (long)(time/1000);
        mins = (long)((time/1000)/60);
        hrs = (long)(((time/1000)/60)/60);

		/* Convert the seconds to String
		 * and format to ensure it has
		 * a leading zero when required
		 */
        secs = secs % 60;
        seconds=String.valueOf(secs);
        if(secs == 0){
            seconds = "00";
        }
        if(secs <10 && secs > 0){
            seconds = "0"+seconds;
        }

		/* Convert the minutes to String and format the String */

        mins = mins % 60;
        minutes=String.valueOf(mins);
        if(mins == 0){
            minutes = "00";
        }
        if(mins <10 && mins > 0){
            minutes = "0"+minutes;
        }

    	/* Convert the hours to String and format the String */

        hours=String.valueOf(hrs);
        if(hrs == 0){
            hours = "00";
        }
        if(hrs <10 && hrs > 0){
            hours = "0"+hours;
        }

    	/* Although we are not using milliseconds on the timer in this example
    	 * I included the code in the event that you wanted to include it on your own
    	 */
        milliseconds = String.valueOf((long)time);
        if(milliseconds.length()==2){
            milliseconds = "0"+milliseconds;
        }
        if(milliseconds.length()<=1){
            milliseconds = "00";
        }
        milliseconds = milliseconds.substring(milliseconds.length()-3, milliseconds.length()-2);

		/* Setting the timer text to the elapsed time */
        ((TextView)findViewById(R.id.timer)).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.timer)).setText(hours + ":" + minutes);
    }
}