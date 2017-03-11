package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import org.osmdroid.util.GeoPoint;

import java.security.PublicKey;
import java.util.List;

/**
 * Created by FlorianM on 07.03.2017.
 */

public class Path {

    private List<Coordinate> Coordinates;
    private String Name;
    private int Range;

    public Path(List<Coordinate> Coordinates, String Name, int Range) {
        this.Coordinates = Coordinates;
        this.Name = Name;
        this.Range = Range;
    }

    public Path() {
    }

    public List<Coordinate> getCoordinates() {
        return Coordinates;
    }

    public void setCoordinates(List<Coordinate> coordinates) {
        Coordinates = coordinates;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getRange() {
        return Range;
    }

    public void setRange(int range) {
        Range = range;
    }
}
