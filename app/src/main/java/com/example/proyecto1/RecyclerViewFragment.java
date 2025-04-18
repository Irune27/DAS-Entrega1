package com.example.proyecto1;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RecyclerViewFragment extends Fragment {

    public interface recipeListener {
        void onRecipeSelected(int pos);
    }

    private RecyclerView list;
    private MyAdapter adapter;
    private ArrayList<String> recipeNames, images;
    private recipeListener listener;

    public RecyclerViewFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        list = view.findViewById(R.id.recyclerView);
        recipeNames = new ArrayList<>();
        images = new ArrayList<>();

        SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        adapter = new MyAdapter(getContext(), recipeNames, images, pos -> {
            if (listener != null) {
                listener.onRecipeSelected(pos);
            }
        });
        list.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);

        // actualizar las recetas que se muestran con la lista de recetas del servidor
        if (userId != -1) fetchRecipesFromServer(userId);
            // actualizar las recetas que se muestran con la lista de recetas de la base de datos local
        else loadRecipes();

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

    private void loadRecipes() {
        AppUtils.loadRecipesFromLocal(requireContext(), (names, imgs) -> {
            recipeNames.clear();
            // se devuelve la lista entera de nombres de recetas
            recipeNames.addAll(names);

            images.clear();
            // se devuelve la lista entera de rutas a las imágenes de las recetas
            images.addAll(imgs);

            adapter.updateData(recipeNames, images, listener::onRecipeSelected);
        });
    }

    private void fetchRecipesFromServer(int userId) {
        AppUtils.fetchRecipesFromServer(requireContext(), getViewLifecycleOwner(), userId, (names, imgs) -> {
            recipeNames.clear();
            // se devuelve la lista entera de nombres de recetas
            recipeNames.addAll(names);

            images.clear();
            // se devuelve la lista entera de rutas a las imágenes de las recetas
            images.addAll(imgs);

            adapter.updateData(recipeNames, images, listener::onRecipeSelected);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
