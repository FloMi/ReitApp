package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatisticActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth=FirebaseAuth.getInstance();

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

        mDatabase.child("Users").child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    switch (postSnapshot.getKey())
                    {
                        case "finishedTour":touren.setText(postSnapshot.getValue().toString());break;
                        case "range":kilom.setText(postSnapshot.getValue().toString());break;
                        case "gast":gast.setText(postSnapshot.getValue().toString());break;
                        case "kult":kult.setText(postSnapshot.getValue().toString());break;
                        default:
                            Toast.makeText(StatisticActivity.this, R.string.toast_show_stats_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
