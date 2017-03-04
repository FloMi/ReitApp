package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import static koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp.R.id.time;

/**
 * Created by Florian on 04.03.2017.
 */

public class Timer {

    private Handler mHandler = new Handler();
    private long startTime;
    private long elapsedTime;
    private String hours, minutes, seconds, milliseconds;
    private long secs, mins, hrs;
    private boolean stopped = false;
    private final int REFRESH_RATE = 100;
    private Context context;
    private MapActivity activity;

    private Runnable startTimer = new Runnable() {
        public void run() {
            elapsedTime = System.currentTimeMillis() - startTime;
            updateTimer(elapsedTime);
            mHandler.postDelayed(this, REFRESH_RATE);
        }
    };


    public Timer(Context context) {
        this.context = context;
        this.activity = (MapActivity) context;
    }

    public void stopClick()  {
        hideTimer();
        mHandler.removeCallbacks(startTimer);
        stopped = true;
    }

    private void hideTimer() {
        ((TextView) activity.findViewById(R.id.timer)).setVisibility(View.INVISIBLE);
    }

    public void startClick() {
        if (stopped) {
            startTime = System.currentTimeMillis() - elapsedTime;
        } else {
            startTime = System.currentTimeMillis();
        }
        mHandler.removeCallbacks(startTimer);
        mHandler.postDelayed(startTimer, 0);
    }

    public void resetClick()  {
        stopped = false;
        secs = 0;
        mins = 0;
        hrs = 0;
        ((TextView) activity.findViewById(R.id.timer)).setText("00:00:00");
    }

    private void updateTimer(long time) {
        secs = (long) (time / 1000);
        mins = (long) ((time / 1000) / 60);
        hrs = (long) (((time / 1000) / 60) / 60);

		/* Convert the seconds to String
         * and format to ensure it has
		 * a leading zero when required
		 */
        secs = secs % 60;
        seconds = String.valueOf(secs);
        if (secs == 0) {
            seconds = "00";
        }
        if (secs < 10 && secs > 0) {
            seconds = "0" + seconds;
        }

		/* Convert the minutes to String and format the String */

        mins = mins % 60;
        minutes = String.valueOf(mins);
        if (mins == 0) {
            minutes = "00";
        }
        if (mins < 10 && mins > 0) {
            minutes = "0" + minutes;
        }

    	/* Convert the hours to String and format the String */

        hours = String.valueOf(hrs);
        if (hrs == 0) {
            hours = "00";
        }
        if (hrs < 10 && hrs > 0) {
            hours = "0" + hours;
        }

    	/* Although we are not using milliseconds on the timer in this example
    	 * I included the code in the event that you wanted to include it on your own
    	 */
        milliseconds = String.valueOf((long) time);
        if (milliseconds.length() == 2) {
            milliseconds = "0" + milliseconds;
        }
        if (milliseconds.length() <= 1) {
            milliseconds = "00";
        }
        milliseconds = milliseconds.substring(milliseconds.length() - 3, milliseconds.length() - 2);



		/* Setting the timer text to the elapsed time */
        ((TextView) activity.findViewById(R.id.timer)).setVisibility(View.VISIBLE);
        ((TextView) activity.findViewById(R.id.timer)).setText(hours + ":" + minutes);
    }

}
