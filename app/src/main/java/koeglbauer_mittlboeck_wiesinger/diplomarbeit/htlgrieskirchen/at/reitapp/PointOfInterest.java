package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import org.osmdroid.util.GeoPoint;

/**
 * Created by FlorianM on 28.02.2017.
 */

public class PointOfInterest {

    String id;
    double latitude;
    double longitude;
    String name;

    public PointOfInterest(String id, double latitude, double longitude, String name) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public PointOfInterest() {

    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
