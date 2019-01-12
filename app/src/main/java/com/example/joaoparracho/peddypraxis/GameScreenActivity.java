package com.example.joaoparracho.peddypraxis;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.Recycle_adapter.Game_adapter_rec;
import com.example.joaoparracho.peddypraxis.Recycle_adapter.RecyclerTouchListener;
import com.example.joaoparracho.peddypraxis.model.Game;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.firebase.auth.FirebaseAuth;

public class GameScreenActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;

    private RecyclerView recyclerView;
    private Game_adapter_rec adapter_rec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamescreen);

        firebaseAuth = FirebaseAuth.getInstance();

        if (Singleton.getInstance().getManager().getGames().size() == 0) Singleton.getInstance().getManager().addGame(new Game("PeddyPraxis", "Melhor jogo de Sempre", "Parracho & Xavi", 60));

        recyclerView = findViewById(R.id.recycleView_games);
        adapter_rec = new Game_adapter_rec(this, Singleton.getInstance().getManager().getGames());
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter_rec);
        adapter_rec.notifyDataSetChanged();

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                startActivity(new Intent(GameScreenActivity.this, PreambuloActivity.class));
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));
        registerForContextMenu(recyclerView);
    }

    public void onClickReturn(MenuItem item) {
        showLogoutDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_gamescreen, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        Log.d("xxxfences", "back button pressed");
        showLogoutDialog();
    }

    public void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you really want to logout from your account?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        firebaseAuth.signOut();
                        finish();
                        Intent i = new Intent(GameScreenActivity.this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(GameScreenActivity.this, "So, let´s PLAAAYYYYYYYY", Toast.LENGTH_SHORT).show();
                    }
                })
                .create().show();
    }
}
