package com.example.joaoparracho.peddypraxis;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        firebaseAuth= FirebaseAuth.getInstance();
    }
    public void onClickLogin(View v){
        TextView userEmail = findViewById(R.id.TextViewEmail);
    }
    public void onCLickSignUp(View v){

    }

}
