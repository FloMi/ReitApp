package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

/**
 * Created by grego on 14.11.2016.
 */

public class Coordinate {
    public double latitude;
    public double longitude;

    public Coordinate()
    {
    }

    public Coordinate(double latitude, double longitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;
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

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
