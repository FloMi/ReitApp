package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

/**
 * Created by Simon on 05.01.2017.
 */

public class User {
    int finishedTour;
    int range;
    int gast;
    int kult;

    public User()
    {

    }

    public User(int finishedTour, int range, int gast, int kult) {
        this.finishedTour = finishedTour;
        this.range = range;
        this.gast = gast;
        this.kult = kult;
    }

    public int getFinishedTour() {
        return finishedTour;
    }

    public int getRange() {
        return range;
    }

    public int getGast() {
        return gast;
    }

    public int getKult() {
        return kult;
    }
}
