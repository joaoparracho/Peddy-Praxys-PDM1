package com.example.joaoparracho.peddypraxis;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.joaoparracho.peddypraxis.Recycle_adapter.Game_adapter_rec;
import com.example.joaoparracho.peddypraxis.Recycle_adapter.RecyclerTouchListener;
import com.example.joaoparracho.peddypraxis.model.Game;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.example.joaoparracho.peddypraxis.model.Utilizador;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class GameScreenActivity extends AppCompatActivity {
    public static final String TAG = "GameScreenActivity";
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private RecyclerView recyclerView;
    private Game_adapter_rec adapter_rec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamescreen);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        Log.d("xxxSingleton", Singleton.getInstance().getCurrentUser().getIdade() + Singleton.getInstance().getCurrentUser().getNumJogosInic());
        if (Singleton.getInstance().getManager().getGames().size() == 0)
            Singleton.getInstance().getManager().addGame(new Game(getString(R.string.gameTitle), getString(R.string.descrGame), getString(R.string.gameAuth), 60));

        recyclerView = findViewById(R.id.recycleView_games);
        adapter_rec = new Game_adapter_rec(this, Singleton.getInstance().getManager().getGames());
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter_rec);
        adapter_rec.notifyDataSetChanged();
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Singleton.getInstance().getCurrentUser().setNumJogosInic(Singleton.getInstance().getCurrentUser().getNumJogosInic() + 1);
                sendData();
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
        Log.d(TAG, "back button pressed");
        showLogoutDialog();
    }

    public void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(R.string.qstnLog)
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
                        Toast.makeText(GameScreenActivity.this, getString(R.string.lestPlay), Toast.LENGTH_SHORT).show();
                    }
                })
                .create().show();
    }

    private void sendData() {
        DatabaseReference myRef = firebaseDatabase.getReference(firebaseAuth.getUid());
        myRef.setValue(Singleton.getInstance().getCurrentUser());
    }

    public void onClickShowScoreboard(MenuItem item) {
        verifyPrvInfo();
    }

    public void onCLickShowStatus(MenuItem item) {
        showDescription();
    }

    public void showDescription() {
        float precVit=0;
        int sec=0;
        int min=0;
        if(Singleton.getInstance().getCurrentUser().getNumJogosTerm()!=0) {
            precVit = (Singleton.getInstance().getCurrentUser().getNumJogosTerm() * 100) / (Singleton.getInstance().getCurrentUser().getNumJogosInic());
            min = (int) (((Singleton.getInstance().getCurrentUser().getMelhorTempo()) / 1000) % 3600) / 60;
            sec = (int) (Singleton.getInstance().getCurrentUser().getMelhorTempo() / 1000) % 60;
        }

        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(getString(R.string.estatiTitle));

        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                getString(R.string.estatiTitle).length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        new AlertDialog.Builder(this)
                .setTitle(ssBuilder)
                .setMessage(getString(R.string.name) + Singleton.getInstance().getCurrentUser().getName()
                        +"\n"+getString(R.string.age) + Singleton.getInstance().getCurrentUser().getIdade()
                        +"\n"+getString(R.string.bestTime) + min + ":" + sec
                        +"\n"+getString(R.string.numGameStr) + Singleton.getInstance().getCurrentUser().getNumJogosInic()
                        +"\n"+getString(R.string.finishGame) + Singleton.getInstance().getCurrentUser().getNumJogosTerm()
                        +"\n"+getString(R.string.percVict) + precVit+"%")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    public void showDialogScoreboard(String scoreboard) {
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder("Ranking");

        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
               7,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        new AlertDialog.Builder(this)
                .setTitle(ssBuilder)
                .setMessage(scoreboard)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

    public void verifyPrvInfo() {
        final ArrayList<Utilizador> users = new ArrayList<>();
        final DatabaseReference databaseReference = firebaseDatabase.getReference();
        final StringBuilder sb = new StringBuilder();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Utilizador uInfo = new Utilizador();
                    uInfo.setName(ds.getValue(Utilizador.class).getName());
                    uInfo.setMelhorTempo(ds.getValue(Utilizador.class).getMelhorTempo());
                    users.add(uInfo);
                    //display all the information
                    Log.d(TAG, "xxxSingleton " + uInfo.getName());
                    Log.d(TAG, "xxxSingleton " + uInfo.getMelhorTempo());
                }
                Collections.sort(users);
                int min;
                int sec;
                for (int i = 0; i < users.size(); i++) {
                    min = (int) (((users.get(i).getMelhorTempo()) / 1000) % 3600) / 60;
                    sec = (int) (users.get(i).getMelhorTempo() / 1000) % 60;
                    sb.append((i + 1)).append(". ").append(users.get(i).getName()).append(" -> ").append(min).append(":").append(sec).append("\n");
                }
                showDialogScoreboard(sb.toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
