package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

/**
 * Created by grego on 14.11.2016.
 */

public class Coordinate {
    public String geoLength;
    public String geoWidth;

    public Coordinate()
    {
    }

    public Coordinate(String referenceSystem, String geoLength, String geoWidth, String eastingUTM, String northingUTM, int zone)
    {
        this.geoLength = geoLength;
        this.geoWidth = geoWidth;
    }
}
