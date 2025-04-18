package com.example.proyecto1;

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MyViewHolder extends RecyclerView.ViewHolder implements RecipeFragment.recipeListener {
    public TextView text;
    public ImageView image;
    RelativeLayout relativeLayout;
    public RecipeFragment.recipeListener listener;

    public MyViewHolder (@NonNull View itemView){
        super(itemView);
        text = itemView.findViewById(R.id.texto);
        image = itemView.findViewById(R.id.foto);
        relativeLayout = itemView.findViewById(R.id.relativeLayout);
        itemView.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onRecipeSelected(position);
            }
        });
    }

    @Override
    public void onRecipeSelected(int pos) {

    }
}