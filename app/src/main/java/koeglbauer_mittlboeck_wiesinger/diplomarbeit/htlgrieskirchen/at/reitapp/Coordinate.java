package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

/**
 * Created by grego on 14.11.2016.
 */

public class Coordinate {
    public String referenceSystem;
    public String geoLength;
    public String geoWidth;
    public String eastingUTM;
    public String northingUTM;
    public int zone;

    public Coordinate()
    {
    }

    public Coordinate(String referenceSystem, String geoLength, String geoWidth, String eastingUTM, String northingUTM, int zone)
    {
        this.referenceSystem = referenceSystem;
        this.geoLength = geoLength;
        this.geoWidth = geoWidth;
        this.eastingUTM = eastingUTM;
        this.northingUTM = northingUTM;
        this.zone = zone;
    }
}
