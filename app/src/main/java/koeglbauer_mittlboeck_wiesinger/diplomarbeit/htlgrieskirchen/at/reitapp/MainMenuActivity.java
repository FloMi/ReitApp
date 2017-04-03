package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home : onBackPressed(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);
    }
    public void onClickStatistic(View view){
        Intent intent = new Intent(this, StatisticActivity.class);
        startActivity(intent);
    }
    public void onClickSecurity(View view){
        Intent intent = new Intent(this, SecurityActivity.class);
        startActivity(intent);
    }
    public void onClickSettings(View view){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    public void onClickLogout(View view){
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, R.string.loggedout_toast, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        finish();
        startActivity(intent);
    }
    public void onClickTour(View view){
        Intent intent = new Intent(this, TourActivity.class);
        startActivity(intent);
    }
    public void onClickTuto(View view){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://reitapp.hol.es/Reitapp_Tutorial.html"));
        startActivity(browserIntent);
    }
    public void onClickHelp(View view){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://reitapp.hol.es/Reitapp_Hilfe.html"));
        startActivity(browserIntent);
    }
}
