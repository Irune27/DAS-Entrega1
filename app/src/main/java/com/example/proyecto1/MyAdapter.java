package com.example.proyecto1;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private Context context;
    private ArrayList<String> names, images;
    private OnRecipeClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnRecipeClickListener {
        void onRecipeSelected(int position);
    }

    public MyAdapter (Context context, ArrayList<String> n, ArrayList<String> i, OnRecipeClickListener listener) {
        this.context = context;
        names=n;
        images=i;
        this.listener = listener;
    }

    public void updateData(ArrayList<String> newNames, ArrayList<String> newImages, OnRecipeClickListener listener) {
        names = newNames;
        images = newImages;
        this.listener = listener;
        // avisar de que hay que actualizar la vista
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemLayout= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false); //null
        MyViewHolder evh = new MyViewHolder(itemLayout);
        return evh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.text.setText(names.get(position));
        String imagePath = images.get(position);
        AppUtils.loadImage(context, imagePath, holder.image);

        // si el dispositivo está en horizontal, cambiar el color del item de la lista si se ha seleccionado
        int orientation = holder.itemView.getContext().getResources().getConfiguration().orientation;
        if (position == selectedPosition && orientation == Configuration.ORIENTATION_LANDSCAPE) {
            holder.relativeLayout.setBackgroundResource(R.color.myLandBackground);
        } else {
            holder.relativeLayout.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeSelected(position);
                setSelectedPosition(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }
}