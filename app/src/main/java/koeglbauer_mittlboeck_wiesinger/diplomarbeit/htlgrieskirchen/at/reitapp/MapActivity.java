package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MapActivity extends Activity {
    MapView map;
    private MapController mMapController;
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    RoadManager roadManager;
    private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }

        OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);
        roadManager = new OSRMRoadManager(this);
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mMapController = (MapController) map.getController();
        mMapController.setZoom(17);
        GeoPoint s = new GeoPoint(48.090075, 13.566833);
        mMapController.setCenter(s);

        SetGraphhopper();


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void SetGraphhopper() {


        new AsyncTask<Void, Void, PathWrapper>() {
            float time;

            protected PathWrapper doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(48.090075, 13.566833, 48.084804, 13.568171).
                        setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
                req.getHints().
                        put(Parameters.Routing.INSTRUCTIONS, "false");
                GraphHopperWeb gh = new GraphHopperWeb();
                gh.setKey("32565c22-5144-4700-b089-a78f30b6044a");
                gh.setDownloader(new OkHttpClient.Builder().
                        connectTimeout(5, TimeUnit.SECONDS).
                        readTimeout(5, TimeUnit.SECONDS).build());
                GHResponse resp = gh.route(req);
                time = sw.stop().getSeconds();
                return resp.getBest();
            }

            protected void onPostExecute(PathWrapper resp) {
                if (!resp.hasErrors()) {


                    Road road = roadManager.getRoad(createPolyline(resp));
                    Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                    map.getOverlays().add(roadOverlay
                    );
                    map.invalidate();
                } else {
                }
            }
        }.execute();


    }


    private  ArrayList<GeoPoint> createPolyline(PathWrapper response) {

        Polyline line = new Polyline(this);

        line.setTitle("Test");
        line.setSubDescription(Polyline.class.getCanonicalName());
        line.setWidth(20f);

        //List<GeoPoint> geoPoints = new ArrayList<>();


        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();


        PointList tmp = response.getPoints();

        for (int i = 0; i < response.getPoints().getSize(); i++) {

            GeoPoint g = new GeoPoint(tmp.getLatitude(i), tmp.getLongitude(i));
            mMapController.setCenter(g);
            waypoints.add(g);

            //geoPoints.add(g);
        }

        return waypoints;
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
}