package com.example.proyecto1;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class RecyclerViewFragment extends Fragment {

    public interface recipeListener {
        void onRecipeSelected(int pos);
    }

    private RecyclerView list;
    private MyAdapter adapter;
    private ArrayList<String> recipeNames;
    private ArrayList<String> images;
    private MyDB dbHelper;
    private recipeListener listener;

    public RecyclerViewFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        list = view.findViewById(R.id.recyclerViewFragment);
        recipeNames = new ArrayList<>();
        images = new ArrayList<>();
        dbHelper = MyDB.getInstance(requireContext());

        adapter = new MyAdapter(recipeNames, images, pos -> {
            if (listener != null) {
                listener.onRecipeSelected(pos);
            }
        });
        list.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);

        // actualizar las recetas que se muestran con la lista de recetas de la base de datos
        loadRecipes();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (recipeListener) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException("The class " + context.toString()
                    + " has to implement recipeListener");
        }
    }

    public void loadRecipes() {
        recipeNames.clear();
        images.clear();

        // recuperar todas las recetas de la base de datos, y mostrar el nombre y la imagen para
        // cada una
        Cursor cursor = dbHelper.getAllRecipes();
        if (cursor.moveToFirst()) {
            do {
                recipeNames.add(cursor.getString(1));
                String image = cursor.getString(2);
                if (image != null && !image.isEmpty()) {
                    images.add(image);
                } else {
                    images.add("");
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter.updateData(recipeNames, images, listener::onRecipeSelected);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.closeDatabase();
        }
    }
}
