package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import org.osmdroid.util.GeoPoint;

import java.util.List;

/**
 * Created by FlorianM on 07.03.2017.
 */

public class Path {

    List<GeoPoint> whichTourFinished;
    String name;
    int range;

    public Path(List<GeoPoint> whichTourFinished, String name, int range) {
        this.whichTourFinished = whichTourFinished;
        this.name = name;
        this.range = range;
    }

    public Path() {
    }

    public List<GeoPoint> getWhichTourFinished() {
        return whichTourFinished;
    }

    public void setWhichTourFinished(List<GeoPoint> whichTourFinished) {
        this.whichTourFinished = whichTourFinished;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }
}
