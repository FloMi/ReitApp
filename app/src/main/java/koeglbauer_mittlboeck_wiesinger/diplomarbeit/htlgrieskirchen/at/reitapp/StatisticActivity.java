package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class StatisticActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private View mProgressView;
    private View mTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth=FirebaseAuth.getInstance();
        mProgressView=findViewById(R.id.statistic_progress);
        mTable=findViewById(R.id.table);

        InitStats();
    }

    private void InitStats() {
        final TextView touren = (TextView) findViewById(R.id.textViewStats1);
        final TextView kilom = (TextView) findViewById(R.id.textViewStats2);
        final TextView gast = (TextView) findViewById(R.id.textViewStats3);
        final TextView kult = (TextView) findViewById(R.id.textViewStats4);

        touren.setText("wird geladen");
        kilom.setText("wird geladen");
        gast.setText("wird geladen");
        kult.setText("wird geladen");

        final DecimalFormat f = new DecimalFormat("#.###");

        showProgress(true);

        mDatabase.child("Users").child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    switch (postSnapshot.getKey())
                    {
                        case "finishedTour":touren.setText(postSnapshot.getValue().toString());break;
                        case "range":kilom.setText(f.format((double)postSnapshot.getValue())+" km");break;
                        case "gast":gast.setText(postSnapshot.getValue().toString());break;
                        case "kult":kult.setText(postSnapshot.getValue().toString());break;
                    }
                }
                showProgress(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void onClickStatisticLink(View view)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://reitapp.hol.es/Reitapp_Statistiken.html"));
        startActivity(browserIntent);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mTable.setVisibility(show ? View.GONE : View.VISIBLE);
            mTable.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTable.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mTable.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
