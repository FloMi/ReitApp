package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import com.graphhopper.util.PointList;
import com.graphhopper.PathWrapper;


import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.R.id.content;
import static java.lang.Integer.valueOf;

public class MapActivity extends Activity {

    static GeoPoint currentLocation;
    static List<GeoPoint> MovedDistance = new ArrayList<>();
    static ArrayList<GeoPoint> PolylineWaypoints = new ArrayList<>();
    static TextView togoal;
    static TextView currentInstruction;
    static private Double DistanceToGoal = 0.0;
    static Marker currentLocationMarker;

    static Polyline CoverdTrack;
    private static MapView map;
    private static MapController mMapController;
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private GoogleApiClient client;

    private static boolean navigationStarted = false;
    private static boolean recordingStarted = false;

    private DatabaseReference mDatabase;

    Polyline response;

    static List<GeoPoint> DatabaseCoordinates = new ArrayList<>();
    private String routID = "0";
    private String routName = "nan";

    Polyline mainPolyline = new Polyline();

     FloatingActionButton startRecord;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        togoal = (TextView) findViewById(R.id.togoal);
        currentInstruction = (TextView) findViewById(R.id.currentInstruction);



        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }
        startRecord = (FloatingActionButton) findViewById(R.id.startrecording);

        startRecord.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(recordingStarted)
                {
                    exportToKml();
                }
                else
                {
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


        final FloatingActionButton startNav = (FloatingActionButton) findViewById(R.id.startrout);
        startNav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(navigationStarted)
                {
                    startNav.setImageResource(R.drawable.ic_navigation_arrow);
                    navigationStarted=false;

                    togoal.setText("");
                    currentInstruction.setText("");

                    clearMap();

                    map.invalidate();
                }
                else
                {
                    startNavigation();
                    startNav.setImageResource(R.drawable.ic_stopnav);
                    navigationStarted=true;

                }


            }
        });

        routID = intent.getStringExtra(TourActivity.EXTRA_MESSAGE);
        if(routID != null) {

            String[] s = routID.split(";");

            routID = (valueOf(s[0]) - 1) + "";
            routName = s[1].split(":")[1];

            startNav.setVisibility(View.VISIBLE);
        }
        else startNav.setVisibility(View.INVISIBLE);


        OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

        SetMap();


        map.setBuiltInZoomControls(false);

        Intent i = new Intent(this, LocationService.class);
        startService(i);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.point));
    }


    private void exportToKml() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                StringBuilder str = new StringBuilder();
                File file;
                String s = input.getText().toString();
                String coordinatesString ="";
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(c.getTime());


                KmlDocument kmlDocument = new KmlDocument();

                kmlDocument.mKmlRoot.addOverlay(CoverdTrack,kmlDocument);


                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/saved_routs");
                myDir.mkdirs();

                file = new File(myDir, s+".kml");

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

    private void writeToFile(String  filename,String data) {
        File file;
        FileOutputStream outputStream;
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/saved_routs");
            myDir.mkdirs();

            file = new File(myDir, filename+".kml");

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
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    tourString=valueOf(postSnapshot.getKey());
                    String[] parts = routID.split(":");
                    if(tourString == valueOf(parts[0]))
                    {
                        postSnapshot.child("Coordinates").getChildren();

                        for(DataSnapshot ps: postSnapshot.child("Coordinates").getChildren())
                        {
                            Double l = Double.parseDouble(ps.child("geoLength").getValue().toString());
                            Double w = Double.parseDouble(ps.child("geoWidth").getValue().toString());

                            GeoPoint g = new GeoPoint(w,l);
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
        currentLocation = Location;
        map.getOverlays().remove(currentLocationMarker);
        currentLocationMarker.setPosition(currentLocation);
        currentLocationMarker.setTitle("You");
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        map.getOverlays().add(currentLocationMarker);
        mMapController.animateTo(Location);
        if(navigationStarted && DatabaseCoordinates.size()>0)
        {
            crateInstructions();
        }
    }

    public static void calcWayToGoal(GeoPoint currentLocation) {

        //if (!navigationStarted) return;

        DistanceToGoal = 0.0;

        if (PolylineWaypoints.size() > 0) {
            List<GeoPoint> PointsToFinish = getRoutLeft(PolylineWaypoints);

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

        if (DistanceToGoal != 0) {
            String s = GetDistanceString(DistanceToGoal);
            togoal.setText(s);
            togoal.invalidate();
        } else {
            togoal.setText("");
            togoal.invalidate();
        }
    }

    public void checkIfTourFinished()
    {
        if(Math.abs(calcDistanceOfRout(MovedDistance) -    calcDistanceOfRout(DatabaseCoordinates)) < 50)
        {
                
        }
    }

    private float calcDistanceOfRout(List<GeoPoint> rout) {

        float distanceInMeters = 0;

        for ( int i = 0;i<= rout.size()-1;i++){

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

                        movedForward(cl);
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
                        movedForward(cl);
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

    private static void movedForward(GeoPoint cl) {


    }

    private static String GetDistanceString(Double distance) {

        DecimalFormat df = new DecimalFormat("#.##");

        if (distance < 999.99999999999) return (df.format(distance) + " m");

        return df.format(distance / 1000) + "Km";
    }

    private static List<GeoPoint> getRoutLeft(List<GeoPoint> points) {

        GeoPoint nextpoint = points.get(0);
        float smalestDistance = 10000000;

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

        for (int l = 0; l <= points.indexOf(nextpoint) - 1; l++) {
            points.remove(l);
        }

        GeoPoint currentGHPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

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
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    private void setMobileDataEnabled(Context context, boolean enabled) {
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass;
        try {
            conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());

            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);

            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void drawPath() {

                mainPolyline.setColor(Color.argb(255, 138, 152, 31));
                mainPolyline.setWidth(20);

                mainPolyline.setPoints(DatabaseCoordinates);

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

    private ArrayList<GeoPoint> createPolyline(PathWrapper response) {

        PolylineWaypoints.clear();

        PointList tmp = response.getPoints();

        for (int i = 0; i < tmp.getSize(); i++) {

            GeoPoint g = new GeoPoint(tmp.getLatitude(i), tmp.getLongitude(i));
            mMapController.setCenter(g);
            PolylineWaypoints.add(g);
        }

        return PolylineWaypoints;
    }

    private static void crateInstructions() {

        if(currentLocation.distanceTo(DatabaseCoordinates.get(0))>20)
        {
            currentInstruction.setVisibility(View.VISIBLE);
            float distance = currentLocation.distanceTo(DatabaseCoordinates.get(0));
            String toStart = String.format("%.2f", distance);
            currentInstruction.setText("Begeben sie sich zum Start, entfernung: "+toStart);
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
        currentInstruction.setText("Sie verlassen die Route! " + distance);
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
}