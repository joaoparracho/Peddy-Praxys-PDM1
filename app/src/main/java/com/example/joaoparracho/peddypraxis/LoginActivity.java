package com.example.joaoparracho.peddypraxis;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.model.Manager;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.example.joaoparracho.peddypraxis.model.Utilizador;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private CheckBox checkRememberUser;
    TextView userEmail, userPassword;
    String email, password;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupUIViews();
        readLoginInfo();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        progressDialog = new ProgressDialog(this);
    }

    public void onClickLogin(View v) {
        if (validate()) validate(email, password);
    }

    public void onCLickSignUp(View v) {
        if(validate()) {
            TextView userEmail = findViewById(R.id.TextViewEmail);
            TextView userPassword = findViewById(R.id.TextViewPass);

            String email = userEmail.getText().toString();
            String pass = userPassword.getText().toString();

            firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, getString(R.string.RegSuc), Toast.LENGTH_SHORT).show();
                    }
                    else{
                        FirebaseAuthException e = (FirebaseAuthException )task.getException();
                        Toast.makeText(LoginActivity.this, getString(R.string.RegFail)+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void setupUIViews() {
        userPassword = findViewById(R.id.TextViewPass);
        userEmail = findViewById(R.id.TextViewEmail);
        checkRememberUser = findViewById(R.id.checkRemUsr);
    }

    private Boolean validate() {
        Boolean result = true;

        password = userPassword.getText().toString();
        email = userEmail.getText().toString();

        if (password.isEmpty() || email.isEmpty()) {
            if (password.isEmpty()) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getString(R.string.EmpPass));
                userPassword.setError(spannableStringBuilder);
            }
            if (email.isEmpty()) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getString(R.string.empEmail));
                userEmail.setError(spannableStringBuilder);
            }
            Toast.makeText(this, getString(R.string.enterAllFields), Toast.LENGTH_SHORT).show();
            result = false;
        }
        return result;
    }

    private void validate(String userName, String userPassword) {
        progressDialog.setMessage(getString(R.string.loginIN));
        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(userName, userPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    if (checkRememberUser.isChecked()) {
                        saveLoginInfo();
                    }
                    verifyPrvInfo();
                    Log.d("xxxSingleton", firebaseAuth.getUid());
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.logFail), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });

    }

    public void verifyPrvInfo() {
        final DatabaseReference databaseReference = firebaseDatabase.getReference(firebaseAuth.getUid());
        Log.d("xxxSingleton", firebaseAuth.getUid());
        Toast.makeText(LoginActivity.this, getString(R.string.logSuc), Toast.LENGTH_SHORT).show();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                Singleton.getInstance().setCurrentUser(dataSnapshot.getValue(Utilizador.class));
                Log.d("xxxSingleton", "Hey");
                progressDialog.dismiss();
                if (Singleton.getInstance().getCurrentUser() == null) {
                    startActivity(new Intent(LoginActivity.this, UserProfileActivity.class));
                    finish();
                }else {
                    startActivity(new Intent(LoginActivity.this, GameScreenActivity.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void onClickResetPassword(View view) {
        startActivity(new Intent(LoginActivity.this, PasswordActivity.class));
    }

    public void saveLoginInfo() {
        try {
            FileOutputStream fileOutputStream =
                    openFileOutput("userinfopeddypraxis.bin", Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream =
                    new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(email);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Snackbar.make(getCurrentFocus(), R.string.errLerDados, Snackbar.LENGTH_LONG).show();
        }
    }

    public void readLoginInfo() {
        try {
            FileInputStream fileInputStream = openFileInput("userinfopeddypraxis.bin");
            ObjectInputStream objectInputStream = new
                    ObjectInputStream(fileInputStream);
            userEmail.setText((String) objectInputStream.readObject());
            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Snackbar.make(findViewById(android.R.id.content),  R.string.errLerDados, Snackbar.LENGTH_LONG).show();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Snackbar.make(findViewById(android.R.id.content),  R.string.errLerDados, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_language, menu);
        return true;
    }

    public void onClick_Language(MenuItem item) {
        final String[] listItems = {"Portugues", "English"};
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(LoginActivity.this);
        mBuilder.setTitle("Linguagem");
        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0)
                    setLocale("pt");
                else if (i == 1)
                    setLocale("en");
                dialogInterface.dismiss();
                recreate();
            }
        });
        AlertDialog mDialog = mBuilder.create();

        mDialog.show();
    }
    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;

        getBaseContext().getResources().updateConfiguration(config, null);

        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();

        editor.putString("My_lang", lang);
        editor.apply();
    }



}
