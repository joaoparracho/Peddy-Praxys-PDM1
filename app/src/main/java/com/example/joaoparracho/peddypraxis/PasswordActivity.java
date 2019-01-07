package com.example.joaoparracho.peddypraxis;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class PasswordActivity extends AppCompatActivity {

    private EditText passwordEmail;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recoverpassword);
        firebaseAuth= FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        passwordEmail= findViewById(R.id.EditTextRecEmail);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_gamescreen, menu);
        return true;
    }
    public void onClickReturn(MenuItem item) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void onCLickRestPass(View view) {
        String useremail = passwordEmail.getText().toString();
        if(useremail.equals("")){
            Toast.makeText(PasswordActivity.this, "Please enter your registered email ID", Toast.LENGTH_SHORT).show();
        }else{
            progressDialog.setMessage("Sending email!");
            progressDialog.show();
            firebaseAuth.sendPasswordResetEmail(useremail).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        progressDialog.dismiss();
                        Toast.makeText(PasswordActivity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                        finish();
                        startActivity(new Intent(PasswordActivity.this, LoginActivity.class));
                    }else{
                        progressDialog.dismiss();
                        Toast.makeText(PasswordActivity.this, "Error in sending password reset email", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }


}
