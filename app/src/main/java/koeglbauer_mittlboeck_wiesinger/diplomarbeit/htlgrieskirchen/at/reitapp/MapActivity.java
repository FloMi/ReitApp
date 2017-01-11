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
import android.widget.Toast;


import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;


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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MapActivity extends Activity {

    private static MapView map;
    private MapController mMapController;
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private GoogleApiClient client;
    private InstructionList instructionList;
    ArrayList<OverlayItem> overlayItemArray;
    LocationService mLocationService;
    //MyLocationOverlay myLocationOverlay = null;
    LocationManager locationManager;
    static GeoPoint currentLocation;
    private GeoPoint startingPoint;
    Polyline response;
    List<Marker> InstructionMarkerList = new ArrayList<Marker>();

    static Marker currentLocationMarker;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }

        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.start);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openUserActivity();
                stopService(new Intent(MapActivity.this,LocationService.class));

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


        Intent intent = new Intent(this, LocationService.class);
        startService(intent);


        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.point));
    }

    private void startNavigation() {



        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

        builder.setTitle("Start Punkt");

        builder.setMessage("Von wo navigieren?");
        builder.setPositiveButton("Aktueller Standpunkt ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SetGraphhopperActualPosition();


            }
        });
        builder.setNegativeButton("Startpunkt des Wegs ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SetGraphhopperOnStart();


            }
        });
        builder.show();
    }

    public static void displayMyCurrentLocationOverlay(GeoPoint Location)
    {
        currentLocation = Location;
        map.getOverlays().remove(currentLocationMarker);
        currentLocationMarker.setPosition(currentLocation);
        currentLocationMarker.setTitle("You");
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        map.getOverlays().add(currentLocationMarker);
    }


    private void openUserActivity() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    private void SetGraphhopperOnStart() {



        new AsyncTask <Void, Void, Polyline>() {
            float time;

            @Override
            protected void onPreExecute() {

                if (!isNetworkAvailable())
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                    builder.setTitle("Kein Internet");

                    builder.setMessage("Keine Verbindung");
                    builder.setPositiveButton("Enable Mobile Data", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                setMobileDataEnabled(getApplicationContext(),true);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            } catch (NoSuchFieldException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (NoSuchMethodException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }


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

                if (this.isCancelled())
                {
                    return null;
                }

                StopWatch sw = new StopWatch().start();

                List<GHPoint> points;
                points = new ArrayList<GHPoint>(6);

                GHPoint b = new GHPoint(48.468844, 13.733946);
                GeoPoint s = new GeoPoint(b.getLat(),b.getLon());
                startingPoint = s;
                GHPoint e = new GHPoint(48.472273, 13.742282);
                GHPoint f = new GHPoint(48.476809, 13.738836);
                GHPoint h = new GHPoint(48.468844, 13.733946);

                points.add(b);
                points.add(e);
                points.add(f);
                points.add(h);

                GHRequest req = new GHRequest(getRoutToStartPointFromPosition(points)).
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

                           crateInstructions();
                           Polyline line = new Polyline();
                           line.setColor(Color.argb(255, 0, 140, 255));
                           line.setWidth(20);


                           line.setPoints(createPolyline(resp.getBest()));

                           return line;

            }


            protected void onPostExecute(Polyline resp) {

                if(response != null)
                {
                    map.getOverlays().remove(response);
                    response = resp;
                    map.getOverlays().add(response);
                    crateInstructions();
                    map.invalidate();
                }
            }
        }.execute();
    }



    public boolean isNetworkAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com"); //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }

    }

    private void setMobileDataEnabled(Context context, boolean enabled) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass = Class.forName(conman.getClass().getName());
        final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);

        final Object iConnectivityManager = iConnectivityManagerField.get(conman);
        final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());

        final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);

        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
    }

    private void SetGraphhopperActualPosition() {

        new AsyncTask<Void, Void, Polyline>() {
            float time;

            @Override
            protected void onPreExecute() {

                if (!isNetworkAvailable())
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                    builder.setTitle("Kein Internet");

                    builder.setMessage("Keine Verbindung");
                    builder.setPositiveButton("Enable Mobile Data", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                setMobileDataEnabled(getApplicationContext(),true);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            } catch (NoSuchFieldException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (NoSuchMethodException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }


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

                if (this.isCancelled())
                {
                    return null;
                }

                StopWatch sw = new StopWatch().start();

                List<GHPoint> points;
                points = new ArrayList<GHPoint>(6);

                GHPoint b = new GHPoint(48.468844, 13.733946);
                GeoPoint s = new GeoPoint(b.getLat(),b.getLon());
                startingPoint = s;
                GHPoint e = new GHPoint(48.472273, 13.742282);
                GHPoint f = new GHPoint(48.476412, 13.742110);
                GHPoint g = new GHPoint(48.478475, 13.738012);


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
                if (isNetworkAvailable())
                {
                    GHResponse resp = gh.route(req);
                    time = sw.stop().getSeconds();

                    instructionList = resp.getBest().getInstructions();

                    crateInstructions();
                    Polyline line = new Polyline();
                    line.setColor(Color.argb(255, 0, 140, 255));
                    line.setWidth(20);


                    line.setPoints(createPolyline(resp.getBest()));

                    return line;
                }




                return null;
            }

            protected void onPostExecute(Polyline resp) {

                if(response != null)
                {
                    map.getOverlays().remove(response);
                    response = resp;
                    map.getOverlays().add(response);
                    crateInstructions();

                    map.invalidate();
                }
            }
        }.execute();
    }

        private List<GHPoint> getRoutToClosestPointFromPosition(List<GHPoint> points) {

        //currentLocation

        GHPoint nextpoint = points.get(0);
        float smalestDistance = 10000000;

        for (GHPoint i:points) {

            Location locationList = new Location("point List");

            locationList.setLatitude(i.getLat());
            locationList.setLongitude(i.getLon());

            Location locationCurrent = new Location("point currentlocatio");

            locationCurrent.setLatitude(currentLocation.getLatitude());
            locationCurrent.setLongitude(currentLocation.getLongitude());

            float distance = locationList.distanceTo(locationCurrent);

            if(distance < smalestDistance)
            {
                smalestDistance = distance;
                nextpoint = i;
            }
        }

        for (int l=0; l<=points.indexOf(nextpoint)-1; l++)
        {
            points.remove(l);

        }

        GHPoint currentGHPoint = new GHPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        points.add(0,currentGHPoint);

        return points;
    }

    private List<GHPoint> getRoutToStartPointFromPosition(List<GHPoint> points) {



        GHPoint currentGHPoint = new GHPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        points.add(0,currentGHPoint);

        return points;
    }

    private void SetMap() {

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mMapController = (MapController) map.getController();
        mMapController.setZoom(17);
        GeoPoint s = new GeoPoint(48.473191, 13.738506);
        mMapController.setCenter(s);
    }

    private  ArrayList<GeoPoint> createPolyline(PathWrapper response) {

        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();

        PointList tmp = response.getPoints();

        for (int i = 0; i < tmp.getSize(); i++) {

            GeoPoint g = new GeoPoint(tmp.getLatitude(i), tmp.getLongitude(i));
            mMapController.setCenter(g);
            waypoints.add(g);
        }

        return waypoints;
    }

    private void crateInstructions()
    {
        if(InstructionMarkerList!=null) {
            for (Marker i : InstructionMarkerList) {
                map.getOverlays().remove(i);
            }
        }
        InstructionMarkerList.clear();
        for(int i=0; i<instructionList.size(); i++){
            Marker m = new Marker(map);
            InstructionMarkerList.add(m);
            m.setIcon(getResources().getDrawable(R.drawable.marker));
            GeoPoint g = new GeoPoint(instructionList.get(i).getPoints().getLatitude(0),instructionList.get(i).getPoints().getLongitude(0));
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
        for (Marker i:InstructionMarkerList)
        {
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
        stopService(new Intent(MapActivity.this,LocationService.class));
        super.onDestroy();
    }
}