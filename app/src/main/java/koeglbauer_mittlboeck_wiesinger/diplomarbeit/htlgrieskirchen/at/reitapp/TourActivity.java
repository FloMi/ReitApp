package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TourActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "id";
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    ListView list;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        InitTourList();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object listItem = list.getItemAtPosition(position);
                startMap(listItem.toString());
                Toast.makeText(TourActivity.this, listItem.toString() + " clicked", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void startMap(String s) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(EXTRA_MESSAGE, s);
        startActivity(intent);
    }


    private void InitTourList() {
        list = (ListView) findViewById(R.id.tourList);
        adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        list.setAdapter(adapter);
        adapter.clear();
        adapter.add("Touren werden geladen");

        mDatabase.child("Paths").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String tourString = "";
                adapter.clear();
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    tourString=postSnapshot.getKey().toString();
                    tourString=tourString + ": " + postSnapshot.child("Name").getValue().toString();
                    adapter.add(tourString);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TourActivity.this, R.string.toast_show_tours_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
