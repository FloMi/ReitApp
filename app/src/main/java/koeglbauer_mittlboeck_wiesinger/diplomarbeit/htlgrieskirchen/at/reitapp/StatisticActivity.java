package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class StatisticActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic);
        InitStats();
    }

    private void InitStats() {
        TextView touren = (TextView) findViewById(R.id.textViewStats1);
        TextView kilom = (TextView) findViewById(R.id.textViewStats2);
        TextView gast = (TextView) findViewById(R.id.textViewStats3);
        TextView kult = (TextView) findViewById(R.id.textViewStats4);

        //Todo Datenbankzugriff und Daten anzeigen
    }
}
