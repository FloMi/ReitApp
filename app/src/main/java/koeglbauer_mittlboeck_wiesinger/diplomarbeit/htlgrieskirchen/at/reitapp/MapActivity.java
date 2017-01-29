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
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
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
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.api.GraphHopperWeb;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.Translation;
import com.graphhopper.util.shapes.GHPoint;


import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static java.lang.Integer.valueOf;

public class MapActivity extends Activity {

    static GeoPoint currentLocation;
    static List<GeoPoint> MovedDistance = new ArrayList<>();
    static ArrayList<GeoPoint> PolylineWaypoints = new ArrayList<>();
    static TextView togoal;
    static TextView currentInstruction;
    static private Double DistanceToGoal = 0.0;
    static Marker currentLocationMarker;
    static private InstructionList instructionList;
    static Polyline CoverdTrack;
    private static MapView map;
    private static MapController mMapController;
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private GHPoint startingPoint;
    private GoogleApiClient client;
    private static boolean navigationStartet = false;
    private DatabaseReference mDatabase;
    ArrayList<OverlayItem> overlayItemArray;
    LocationService mLocationService;
    LocationManager locationManager;
    Polyline response;
    List<Marker> InstructionMarkerList = new ArrayList<>();
    List<GeoPoint> DatabaseCoordinates = new ArrayList<>();
    private String message = "0";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();
        message = intent.getStringExtra(TourActivity.EXTRA_MESSAGE);
        //message = (valueOf(message)-1)+"";
        mDatabase = FirebaseDatabase.getInstance().getReference();
        togoal = (TextView) findViewById(R.id.togoal);
        currentInstruction = (TextView) findViewById(R.id.currentInstruction);



        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }

        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.start);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openUserActivity();
                stopService(new Intent(MapActivity.this, LocationService.class));
            }
        });
        Button startNav = (Button) findViewById(R.id.startrout);
        startNav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startNavigation();
            }
        });

        OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

        SetMap();

        Intent i = new Intent(this, LocationService.class);
        startService(i);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.point));
    }

    private void InitTourList() {

        mDatabase.child("Paths").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int tourString;
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    tourString=valueOf(postSnapshot.getKey());
                    String[] parts = message.split(":");
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, R.string.toast_show_tours_failed, Toast.LENGTH_SHORT).show();
            }
        });
        drawPath();
    }

    public static void displayMyCurrentLocationOverlay(GeoPoint Location) {
        currentLocation = Location;
        map.getOverlays().remove(currentLocationMarker);
        currentLocationMarker.setPosition(currentLocation);
        currentLocationMarker.setTitle("You");
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        map.getOverlays().add(currentLocationMarker);
    }

    public static void calcWayToGoal(GeoPoint currentLocation) {

        //if (!navigationStartet) return;

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

    public static void startRecordingHike(GeoPoint cl, String s) {



        if (navigationStartet)
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

                    if (d>(float)3)
                    {

                        movedForward(cl);
                        //mMapController.animateTo(currentLocation);

                        MovedDistance.add(currentLocation);
                    }
                }
            }

            Polyline l = new Polyline();
            l.setColor(Color.argb(255, 255, 0, 0));
            l.setWidth(20);

            l.setPoints(MovedDistance);

            map.getOverlays().remove(l);
            CoverdTrack = l;
            map.getOverlays().add(CoverdTrack);
            map.invalidate();
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

                navigationStartet = true;
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

                Polyline line = new Polyline();
                line.setColor(Color.argb(255, 0, 200, 255));
                line.setWidth(20);

                line.setPoints(DatabaseCoordinates);

                map.getOverlays().remove(line);
                map.getOverlays().add(line);
                //crateInstructions();
                map.invalidate();


    }

    private void SetGraphhopperActualPosition() {

        new AsyncTask<Void, Void, Polyline>() {
            float time;


            @Override
            protected void onPreExecute() {

                if (!isNetworkAvailable()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                    builder.setTitle("Kein Internet");

                    builder.setMessage("Stellen sie eine einternet verbindung her");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            return;
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
                    builder.show();

                    this.cancel(true);
                }

                super.onPreExecute();

            }

            protected Polyline doInBackground(Void... v) {

                if (this.isCancelled()) {
                    return null;
                }

                StopWatch sw = new StopWatch().start();

                List<GHPoint> points;
                points = new ArrayList<GHPoint>(6);

                GHPoint b = new GHPoint(48.092896, 13.565618);
                GHPoint s = new GHPoint(b.getLat(), b.getLon());
                startingPoint = s;
                GHPoint e = new GHPoint(48.090331, 13.571295);
                GHPoint f = new GHPoint(48.093433, 13.571091);
                GHPoint g = new GHPoint(48.092896, 13.565618);


                points.add(b);
                points.add(e);
                points.add(f);
                points.add(g);

                GHRequest req = new GHRequest(getRoutToClosestPointFromPosition(points)).
                        setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
                req.getHints().
                        put(Parameters.Routing.INSTRUCTIONS, "true");
                GraphHopperWeb gh = new GraphHopperWeb();
                gh.setKey("32565c22-5144-4700-b089-a78f30b6044a");
                gh.setDownloader(new OkHttpClient.Builder().
                        connectTimeout(5, TimeUnit.SECONDS).
                        readTimeout(5, TimeUnit.SECONDS).build());
                GHResponse resp = gh.route(req);
                time = sw.stop().getSeconds();

                instructionList = resp.getBest().getInstructions();

                DistanceToGoal = resp.getBest().getDistance();

                crateInstructions();
                Polyline line = new Polyline();
                line.setColor(Color.argb(255, 0, 140, 255));
                line.setWidth(20);

                line.setPoints(createPolyline(resp.getBest()));

                return line;
            }

            protected void onPostExecute(Polyline resp) {

                map.getOverlays().remove(response);
                response = resp;
                map.getOverlays().add(response);
                crateInstructions();

                map.invalidate();

            }
        }.execute();
    }

    private List<GHPoint> getRoutToClosestPointFromPosition(List<GHPoint> points) {

        GHPoint nextpoint = points.get(0);
        float smalestDistance = 10000000;

        for (GHPoint i : points) {

            Location locationList = new Location("point List");

            locationList.setLatitude(i.getLat());
            locationList.setLongitude(i.getLon());

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

        GHPoint currentGHPoint = new GHPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        points.add(0, currentGHPoint);

        return points;
    }

    private List<GHPoint> getRoutToStartPointFromPosition(List<GHPoint> points) {

        GHPoint currentGHPoint = new GHPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        points.add(0, currentGHPoint);

        return points;
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

    private void crateInstructions() {
        if (InstructionMarkerList != null) {
            for (Marker i : InstructionMarkerList) {
                map.getOverlays().remove(i);
            }
        }
        InstructionMarkerList.clear();
        for (int i = 0; i < instructionList.size(); i++) {
            Marker m = new Marker(map);
            InstructionMarkerList.add(m);
            m.setIcon(getResources().getDrawable(R.drawable.marker));
            GeoPoint g = new GeoPoint(instructionList.get(i).getPoints().getLatitude(0), instructionList.get(i).getPoints().getLongitude(0));
            double a = instructionList.get(i).getDistance();
            m.setPosition(g);

            Translation translation = new Translation() {
                @Override
                public String tr(String key, Object... params) {
                    return null;
                }

                @Override
                public Map<String, String> asMap() {
                    return null;
                }

                @Override
                public Locale getLocale() {
                    return null;
                }

                @Override
                public String getLanguage() {
                    return null;
                }
            };

            m.setTitle(instructionList.get(i).getTurnDescription(translation));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }
        for (Marker i : InstructionMarkerList) {
            map.getOverlays().add(i);
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