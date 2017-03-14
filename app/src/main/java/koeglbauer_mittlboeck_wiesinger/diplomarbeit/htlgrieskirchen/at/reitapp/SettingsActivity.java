package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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
    ViewGroup mSettingsView;
    TextView mExpireDate;
    EditText mPasswordView;
    EditText mPasswordConfirmationView;
    CheckBox mNewsletterCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mProgressView = findViewById(R.id.progressBarExpireDate);
        mExpireDate = (TextView) findViewById(R.id.userExpireTextView);
        mPasswordConfirmationView = (EditText) findViewById(R.id.newPasswordConfirmation);
        mPasswordView = (EditText) findViewById(R.id.newPassword);
        mSettingsView= (ViewGroup) findViewById(R.id.settings_view);
        mNewsletterCheck = (CheckBox) findViewById(R.id.newsletterCheck);
        mNewsletterCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newsLetterCheckClicked();
            }
        });

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
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mExpireDate.setText("Benutzer läuft ab am: " + dataSnapshot.child("status").getValue());
                try{
                    mNewsletterCheck.setChecked((Boolean)dataSnapshot.child("newsletter").getValue());
                }
                catch (Exception exc){
                    mNewsletterCheck.setChecked(false);
                }
                showProgress(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void newsLetterCheckClicked() {
        FirebaseDatabase.getInstance().getReference().child("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("newsletter").setValue(mNewsletterCheck.isChecked());
    }

    private void newPasswordClicked() {
        mPasswordView.setError(null);
        mPasswordConfirmationView.setError(null);

        String password = mPasswordView.getText().toString();
        String passwordConfirmation = mPasswordConfirmationView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        if (TextUtils.isEmpty(passwordConfirmation)) {
            mPasswordConfirmationView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordConfirmationView;
            cancel = true;
        } else if (!isPasswordValid(passwordConfirmation)) {
            mPasswordConfirmationView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordConfirmationView;
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
            showProgress(true);
            FirebaseAuth.getInstance().getCurrentUser().updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    showProgress(false);
                    if(!task.isSuccessful())
                    {
                        //Toast.makeText(SettingsActivity.this, "Fehler bei Passwort Änderung. Bitte loggen Sie sich erneut ein.", Toast.LENGTH_SHORT).show();
                        new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle("Passwort Änderung Fehlgeschlagen")
                                .setMessage("Bitten loggen Sie sich erneut ein, um das Passwort ändern zu können.")
                                .setPositiveButton("Ausloggen", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        FirebaseAuth.getInstance().signOut();
                                        Toast.makeText(SettingsActivity.this, R.string.loggedout_toast, Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                        finish();
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton("Zurück", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                    else
                    {
                        Toast.makeText(SettingsActivity.this, "Passwort erfolgreich geändert", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            mPasswordConfirmationView.setText("");
            mPasswordView.setText("");
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

            mSettingsView.setVisibility(show ? View.GONE : View.VISIBLE);
            mSettingsView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSettingsView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mSettingsView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
