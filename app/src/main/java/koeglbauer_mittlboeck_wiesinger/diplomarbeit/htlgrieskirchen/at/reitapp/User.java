package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import java.util.Date;
import java.util.List;

/**
 * Created by Simon on 05.01.2017.
 */

public class User {
    int finishedTour;
    int range;
    int gast;
    int kult;
    String email;
    List<Integer> whichTourFinished;
    Date status;

    public User()
    {

    }

    public User(Date status, int finishedTour, int range, int gast, int kult, String email, List<Integer> whichTourFinished) {
        this.status = status;
        this.finishedTour = finishedTour;
        this.range = range;
        this.gast = gast;
        this.kult = kult;
        this.email = email;
        this.whichTourFinished = whichTourFinished;
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

    public String getEmail() {
        return email;
    }

    public List<Integer> getWhichTourFinished() {
        return whichTourFinished;
    }

    public Date getStatus() {
        return status;
    }
}
