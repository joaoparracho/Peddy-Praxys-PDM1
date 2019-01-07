package com.example.joaoparracho.peddypraxis;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    TextView userEmail, userPassword;
    String email,password;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupUIViews();
        firebaseAuth= FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
    }
    public void onClickLogin(View v){
       /* if(validate()){
            validate(email,password);
        }*/
        Intent i = new Intent(LoginActivity.this, GameScreenActivity.class);
        startActivity(i);
    }
    public void onCLickSignUp(View v){
        if(validate()) {
            TextView userEmail = findViewById(R.id.TextViewEmail);
            TextView userPassword = findViewById(R.id.TextViewPass);

            String email = userEmail.getText().toString();
            String pass = userPassword.getText().toString();

            firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        FirebaseAuthException e = (FirebaseAuthException )task.getException();
                        Toast.makeText(LoginActivity.this, "Failed Registration: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    private void setupUIViews(){
        userPassword = findViewById(R.id.TextViewPass);
        userEmail = findViewById(R.id.TextViewEmail);
    }
    private Boolean validate(){
        Boolean result = true;

        password = userPassword.getText().toString();
        email = userEmail.getText().toString();


        if( password.isEmpty() || email.isEmpty()){
            //ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.errorColor));

            if(password.isEmpty()){
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("User password can't be empty");
                userPassword.setError(spannableStringBuilder);
            }

            if(email.isEmpty()){
               SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("User email can´t be empty");
               userEmail.setError(spannableStringBuilder);
            }

            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            result=false;
        }
        return result;
    }
    private void validate(String userName, String userPassword) {

        progressDialog.setMessage("Login into the best game EVAARRR!");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(userName, userPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(LoginActivity.this, GameScreenActivity.class);
                    startActivity(i);
                }else{
                    Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });

    }

    public void onClickResetPassword(View view) {
        startActivity(new Intent(LoginActivity.this, PasswordActivity.class));
    }
}
