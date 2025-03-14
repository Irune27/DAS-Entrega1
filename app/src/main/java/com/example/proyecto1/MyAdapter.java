package com.example.proyecto1;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private ArrayList<String> names;
    private ArrayList<String> images;
    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeSelected(int position);
    }
    public MyAdapter (ArrayList<String> n, ArrayList<String> i, OnRecipeClickListener listener) {
        names=n;
        images=i;
        this.listener = listener;
    }

    public void updateData(ArrayList<String> newNames, ArrayList<String> newImages, OnRecipeClickListener listener) {
        names = newNames;
        images = newImages;
        this.listener = listener;
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
        if (imagePath.matches("\\d+")) {
            holder.image.setImageResource(Integer.parseInt(imagePath));
        }
        else if (!imagePath.isEmpty()) {
            holder.image.setImageBitmap(BitmapFactory.decodeFile(images.get(position)));
        } else {
            holder.image.setImageResource(R.drawable.default_image);
        }
        holder.itemView.setOnClickListener(v -> listener.onRecipeSelected(position));
    }

    @Override
    public int getItemCount() {
        return names.size();
    }
}