package com.example.romero_andresimdbappp.ui.gallery;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import com.example.romero_andresimdbappp.R;
import com.example.romero_andresimdbappp.adapters.MovieAdapter;
import com.example.romero_andresimdbappp.database.FavoritesManager;
import com.example.romero_andresimdbappp.models.Movie;

//Muestra las películas favoritas guardadas por el usuario.
//Además, permite compartir la lista en formato JSON mediante Bluetooth.
public class GalleryFragment extends Fragment {
    private RecyclerView recyclerView;
    private MovieAdapter Movieadapter;
    private List<Movie> Favoritemovies = new ArrayList<>();
    private FavoritesManager Favoritesmanager;
    private TextView vistavacia;
    private Button btncompartir;
    private String jsons; // Variable para el JSON de favoritos
    // Solicitud de permisos para Bluetooth en versiones modernas de Android
    private final ActivityResultLauncher<String[]> bluetoothPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = !result.containsValue(false);
                if (granted) {
                    shareFavoritesViaBluetooth(jsons);
                } else {
                    Toast.makeText(requireContext(), "Permisos necesarios para compartir vía Bluetooth.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);
        recyclerView = root.findViewById(R.id.recyclerView);
        vistavacia = root.findViewById(R.id.emptyView);
        btncompartir = root.findViewById(R.id.btnShare);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        Favoritesmanager = new FavoritesManager(requireContext());
        cargarPeliculasFavoritas();
        // Configurar el botón de compartir
        btncompartir.setOnClickListener(v -> compartirPeliculas());
        return root;
    }
    //Obtiene las películas favoritas del usuario y las muestra en la lista.
    private void cargarPeliculasFavoritas() {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Favoritemovies = Favoritesmanager.getFavorites(userEmail);
        actualizarVistaVacia();
        Movieadapter = new MovieAdapter(Favoritemovies, true);
        recyclerView.setAdapter(Movieadapter);
    }
    //Comparte las películas favoritas en formato JSON a través de Bluetooth.
    private void compartirPeliculas() {
        if (Favoritemovies.isEmpty()) {
            Toast.makeText(requireContext(), "No hay favoritos para compartir.", Toast.LENGTH_SHORT).show();
            return;
        }
        Gson gson = new Gson();
        jsons = gson.toJson(Favoritemovies);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            bluetoothPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }
    //Muestra un diálogo con el JSON antes de enviarlo por Bluetooth.
    private void shareFavoritesViaBluetooth(String jsonFavorites) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Lista de Favoritos en JSON")
                .setMessage(jsonFavorites)
                .setPositiveButton("Compartir", (dialog, which) -> iniciarEnvioBluetooth(jsonFavorites))
                .setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .show();
    }
    //Prepara y envía el JSON mediante una app de Bluetooth disponible en el dispositivo.
    private void iniciarEnvioBluetooth(String jsonFavorites) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, jsonFavorites);
        intent.setType("text/plain");

        PackageManager packageManager = requireActivity().getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo.packageName.toLowerCase().contains("bluetooth")) {
                intent.setPackage(resolveInfo.activityInfo.packageName);
                startActivity(intent);
                return;
            }
        }

        Toast.makeText(requireContext(), "No se encontró una app de Bluetooth.", Toast.LENGTH_SHORT).show();
    }
    //Muestra o esconde el mensaje de "Lista vacía" según el contenido de favoritos.
    private void actualizarVistaVacia() {
        if (Favoritemovies.isEmpty()) {
            vistavacia.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            vistavacia.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    //Recarga la lista de favoritos cuando el fragmento se reanuda.
    @Override
    public void onResume() {
        super.onResume();
        cargarPeliculasFavoritas();
    }
    //Evita fugas de memoria eliminando referencias innecesarias.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        vistavacia = null;
    }
}
