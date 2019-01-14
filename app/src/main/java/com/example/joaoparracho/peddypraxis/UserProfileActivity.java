package com.example.joaoparracho.peddypraxis;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.example.joaoparracho.peddypraxis.model.Utilizador;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileActivity extends AppCompatActivity {
    private TextView tvUserName;
    private TextView tvUserAge;
    private Button btnValidate;

    private String userName;
    private String userAge;

    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recoverpassword);
        setupUIViews();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

    }

    public void onCLickRestPass(View view) {
        userName = tvUserName.getText().toString();
        userAge = tvUserAge.getText().toString();
        if (userName.equals("") || userAge.equals("")) {
            Toast.makeText(UserProfileActivity.this, "Please all fields", Toast.LENGTH_SHORT).show();
        } else {
            sendData();
        }
    }

    private void setupUIViews() {
        tvUserName = findViewById(R.id.EditTextUserName);
        tvUserAge = findViewById(R.id.EditTextRecEmail);
        btnValidate = findViewById(R.id.BtnRestPass);

        tvUserAge.setHint("Age");
        tvUserName.setHint("Name");
        tvUserName.setVisibility(View.VISIBLE);
        btnValidate.setText("Enter");
    }

    private void sendData() {
        DatabaseReference myRef = firebaseDatabase.getReference(firebaseAuth.getUid());
        Singleton.getInstance().setCurrentUser(new Utilizador(userName,userAge));
        myRef.setValue(Singleton.getInstance().getCurrentUser());
        startActivity(new Intent(UserProfileActivity.this, GameScreenActivity.class));
        finish();
    }
}
