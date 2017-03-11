package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

/**
 * Created by FlorianM on 10.03.2017.
 */

public class TablePoints {
    static final String TABLE_NAME = "Points";

    static final String PointId = "PointId";
    static final String Latitude = "Latitude";
    static final String Longitude = "Longitude";


    static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
    static final String SQL_CREATE = "CREATE TABLE" + TABLE_NAME + "(" + PointId + " INT PRIMARY KEY NOT NULL," +  Latitude + "REAL NOT NULL," + Longitude + "REAL NOT NULL)";

    static final String SQL_ENPTY = "DELETE FROM " + TABLE_NAME;

}
