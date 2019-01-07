package com.example.joaoparracho.peddypraxis.Recycle_adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.joaoparracho.peddypraxis.R;
import com.example.joaoparracho.peddypraxis.model.Game;

import java.util.ArrayList;


public class Game_adapter_rec extends RecyclerView.Adapter<Game_adapter_rec.ViewHolder> {
    private ArrayList<Game> Games;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context context;

    // data is passed into the constructor
    public Game_adapter_rec(Context context, ArrayList<Game> games) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.Games = games;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.cardview_item_game, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Game game = Games.get(position);
        holder.name.setText("Title: " + game.getGametitle());
        holder.desc.setText("Auth: " + game.getAutor());
        holder.order.setText("Duration: " + game.getDuration());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return Games.size();
    }

    // convenience method for getting data at click position
    public Game getPosition(int id) {
        return Games.get(id);
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener , View.OnCreateContextMenuListener{
        TextView name, desc, order;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textView_game);
            desc = itemView.findViewById(R.id.textView_Author);
            order = itemView.findViewById(R.id.textView_Order);
            cardView = itemView.findViewById(R.id.mCardView);
            cardView.setOnCreateContextMenuListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) { if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition()); }
        //Menu Flutuante
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            //menu.add(Menu.NONE, R.id.action_share, 1, context.getString(R.string.share));
           // menu.add(Menu.NONE, R.id.action_delete, 2, context.getString(R.string.delete));
        }
    }

}
