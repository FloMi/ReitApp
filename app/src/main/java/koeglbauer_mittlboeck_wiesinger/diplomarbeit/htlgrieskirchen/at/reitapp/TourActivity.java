package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
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
import java.util.List;
import java.util.Objects;

public class TourActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "id";
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    ListView list;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private View mProgressView;
    private SharedPreferences pref;
    Object listItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        mProgressView = findViewById(R.id.tour_progress);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        InitTourList();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                listItem = list.getItemAtPosition(position) ;


                if(pref.getBoolean("navigationStarted", false))
                {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(TourActivity.this);

                    builder.setTitle("Neue Tour?");
                    builder.setMessage("Ihre aktuelle Tour wird überschrieben");
                    builder.setPositiveButton("Neue Tour", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    pref.edit().putBoolean("navigationStarted", false).apply();
                                    pref.edit().putBoolean("wasAtStart", false).apply();
                                    SQLiteDatabase db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();
                                    db.execSQL(TablePoints.SQL_DROP);
                                    startMap(listItem.toString());
                                                                  }
                            });
                    builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            });
                    builder.setIcon(R.drawable.ic_alert);
                    builder.show();
                }
                else
                {
                    startMap(listItem.toString());
                }
            }
        });
    }

    private void startMap(String s) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(EXTRA_MESSAGE, s);
        startActivity(intent);
        finish();
    }


    private void InitTourList() {
        list = (ListView) findViewById(R.id.tourList);

        /*adapter=new ArrayAdapter<String>(this, R.layout.tour_list, listItems);
        list.setAdapter(adapter);
        adapter.clear();
        adapter.add("Touren werden geladen");*/
        showProgress(true);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String tourString = "";
                List<String> itemname = new ArrayList<String>();
                List<String> itemdesc = new ArrayList<String>();
                List<Integer>  imgid = new ArrayList<Integer>();
                List<String> itemnumb = new ArrayList<String>();
                List<Integer> backgroundid = new ArrayList<Integer>();

                //adapter.clear();

                for(DataSnapshot postSnapshot: dataSnapshot.child("Paths").getChildren())
                {

                    boolean hasFinished = false;
                    String tourNumber = postSnapshot.getKey();
                    String tourDisplayNumber = tourNumber;
                    itemnumb.add(tourDisplayNumber);
                    if(postSnapshot.child("Name").getValue()==null)
                        tourString=postSnapshot.child("name").getValue().toString();
                    else
                        tourString=postSnapshot.child("Name").getValue().toString();

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
                        //tourString = "☑ " + tourString;
                        imgid.add(R.mipmap.ic_tour_done);
                    }
                    else
                    {
                        //tourString = "☐ " + tourString;
                        imgid.add(R.mipmap.ic_tour_cross);
                    }
                    //adapter.add(tourString);
                    itemname.add(tourString);
                    if(postSnapshot.child("Range").getValue()==null)
                        itemdesc.add(postSnapshot.child("range").getValue().toString()+ " m");
                    else
                        itemdesc.add(postSnapshot.child("Range").getValue().toString()+ " m");
                    if(itemname.size()%2==0)
                        backgroundid.add(R.color.colorTourListFirst);
                    else
                    {
                        backgroundid.add(R.color.colorTourListSecond);
                    }
                }
                String[] arr1 = new String[itemname.size()];
                String[] arr2 = new String[itemname.size()];
                String[] arr4 = new String[itemname.size()];
                Integer[] arr3 = new Integer[imgid.size()];
                Integer[] arr5 = new Integer[imgid.size()];
                TourListAdapter listAdapter=new TourListAdapter(TourActivity.this, itemname.toArray(arr1), imgid.toArray(arr3), itemdesc.toArray(arr2), itemnumb.toArray(arr4), backgroundid.toArray(arr5));
                list.setAdapter(listAdapter);

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