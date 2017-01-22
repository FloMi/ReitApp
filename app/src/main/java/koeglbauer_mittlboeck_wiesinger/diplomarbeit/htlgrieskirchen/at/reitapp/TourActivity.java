package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class TourActivity extends AppCompatActivity {

    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    ListView list;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private View mProgressView;
    private View mTourView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);

        mProgressView = findViewById(R.id.tour_progress);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        InitTourList();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object listItem = list.getItemAtPosition(position);
                Toast.makeText(TourActivity.this, listItem.toString() + " clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void InitTourList() {
        list = (ListView) findViewById(R.id.tourList);
        adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        list.setAdapter(adapter);
        adapter.clear();
        adapter.add("Touren werden geladen");
        showProgress(true);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String tourString = "";
                adapter.clear();

                for(DataSnapshot postSnapshot: dataSnapshot.child("Paths").getChildren())
                {
                    boolean hasFinished = false;
                    String tourNumber = postSnapshot.getKey();
                    int tourDisplayNumber = Integer.parseInt(tourNumber);
                    tourDisplayNumber++;
                    tourString=tourDisplayNumber+": ";
                    tourString+=postSnapshot.child("Name").getValue().toString();

                    for(DataSnapshot postSnapshot2: dataSnapshot.child("Users").child(mAuth.getCurrentUser().getUid()).child("whichTourFinished").getChildren())
                    {
                        //Toast.makeText(TourActivity.this, postSnapshot2.getValue() +" " + tourNumber, Toast.LENGTH_SHORT).show();
                        if(Objects.equals(postSnapshot2.getValue().toString(), tourNumber))
                        {
                            hasFinished = true;
                        }
                    }
                    if(hasFinished)
                    {
                        tourString += " (finished)";
                    }
                    else
                    {
                        tourString += " (not finished)";
                    }
                    adapter.add(tourString);
                }
                showProgress(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TourActivity.this, R.string.toast_show_tours_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            list.setVisibility(show ? View.GONE : View.VISIBLE);
            list.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    list.setVisibility(show ? View.GONE : View.VISIBLE);
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
            list.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}