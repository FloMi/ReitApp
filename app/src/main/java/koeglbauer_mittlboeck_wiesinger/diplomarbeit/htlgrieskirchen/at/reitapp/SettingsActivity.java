package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    View mProgressView;
    TextView mExpireDate;
    EditText mPasswordView;
    EditText mPasswordConfirmationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mProgressView = findViewById(R.id.progressBarExpireDate);
        mExpireDate = (TextView) findViewById(R.id.userExpireTextView);
        mPasswordConfirmationView = (EditText) findViewById(R.id.newPasswordConfirmation);
        mPasswordView = (EditText) findViewById(R.id.newPassword);

        Button newPasswordButton = (Button) findViewById(R.id.new_password_button);
        newPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newPasswordClicked();
            }
        });




        showProgress(true);
        FirebaseDatabase.getInstance().getReference().child("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mExpireDate.setText("Benutzer läuft ab am: " + dataSnapshot.getValue());
                showProgress(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void newPasswordClicked() {
        mPasswordView.setError(null);
        mPasswordConfirmationView.setError(null);

        String password = mPasswordView.getText().toString();
        String passwordConfirmation = mPasswordConfirmationView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (!password.equals(passwordConfirmation)) {
            mPasswordConfirmationView.setError(getString(R.string.error_incorrect_password_confirmed));
            focusView = mPasswordConfirmationView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            FirebaseAuth.getInstance().getCurrentUser().updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(!task.isSuccessful())
                    {
                        Toast.makeText(SettingsActivity.this, "Fehler bei Passwort Änderung", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    private boolean isPasswordValid(String password) {
        return password.length() > 6;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

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
        }
    }
}
