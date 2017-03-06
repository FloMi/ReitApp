package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.content.Context;
import android.print.PrintAttributes;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.IOrientationConsumer;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

/**
 * Created by Florian on 05.03.2017.
 */

public class OrientationProvider extends InternalCompassOrientationProvider {

    private Context context;
    private MapActivity activity;
    private MapView mapView;
    private Marker currentlocationmarker;
    private  InternalCompassOrientationProvider compass;

    public OrientationProvider(Context context) {
        super(context);
    }

    public void getOrientation()
    {

        compass = new InternalCompassOrientationProvider(context);
        IOrientationConsumer iOrientationConsumer = new IOrientationConsumer() {
            @Override
            public void onOrientationChanged(float v, IOrientationProvider iOrientationProvider) {

            }
        };

        compass.startOrientationProvider(iOrientationConsumer);


    }
}
