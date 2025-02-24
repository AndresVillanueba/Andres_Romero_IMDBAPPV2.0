package com.example.romero_andresimdbappp;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.bumptech.glide.Glide;
import com.example.romero_andresimdbappp.databinding.ActivityMainBinding;
import com.example.romero_andresimdbappp.database.UsersDatabase;
import com.example.romero_andresimdbappp.sync.Userssync;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private TextView navEmail, navInitial;
    private ImageView navProfileImage;
    private UsersDatabase usersDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // REFERENCIAS DEL NAV HEADER
        View headerView = binding.navView.getHeaderView(0);
        if (headerView != null) {
            navEmail = headerView.findViewById(R.id.nav_email);
            navInitial = headerView.findViewById(R.id.nav_header_initial);
            navProfileImage = headerView.findViewById(R.id.nav_profile_image);
        } else {
            // Maneja el error o muestra un log para saber que el headerView es nulo
            Log.e("MainActivity", "El headerView es nulo. Verifica que el NavigationView tenga un header definido.");
        }

        Button btnLogout = headerView.findViewById(R.id.btn_google_sign_out);

        // MOSTRAR DATOS DEL USUARIO (por defecto usando FirebaseUser)
        String email = currentUser.getEmail();
        String displayName = currentUser.getDisplayName();

        navEmail.setText(email != null ? email : "Sin email");
        if (displayName != null && !displayName.isEmpty()) {
            navInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
        } else {
            navInitial.setText("U");
        }

        // Cargamos la imagen del nav_header a partir de FirebaseUser
        // (por defecto, si no hay imagen actualizada en la BD local)
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.default_user_image)
                    .into(navProfileImage);
        } else {
            navProfileImage.setImageResource(R.drawable.default_user_image);
        }

        // Inicializamos la instancia de UsersDatabase para poder recargar la imagen actualizada
        usersDatabase = new UsersDatabase(this);

        // BOTÓN LOGOUT (HEADER)
        btnLogout.setVisibility(View.VISIBLE);
        btnLogout.setOnClickListener(v -> {
            // a) Actualizar logout_time en Firestore
            Userssync usersSync = new Userssync();
            usersSync.updateLogoutTime();

            // b) Actualizar logout_time en BD local
            final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            final String logoutTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            new UsersDatabase(MainActivity.this).updateLogoutTime(userId, logoutTime);

            // c) Cerrar sesión en Firebase y Facebook
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();

            // d) Ir a LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // CONFIGURACIÓN NAVIGATION DRAWER
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        ).setOpenableLayout(binding.drawerLayout)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    // Sobrescribimos onResume para actualizar la imagen en el nav_header con la imagen guardada
    @Override
    protected void onResume() {
        super.onResume();
        // Recuperamos los datos actualizados del usuario desde la BD local
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Map<String, String> userData = usersDatabase.getUser(userId);
            if (userData != null) {
                String imageStr = userData.get("image");
                if (imageStr != null && !imageStr.isEmpty()) {
                    Glide.with(this)
                            .load(imageStr)
                            .placeholder(R.drawable.default_user_image)
                            .into(navProfileImage);
                } else {
                    navProfileImage.setImageResource(R.drawable.default_user_image);
                }
            }
        }
    }

    // INFLAR EL MENÚ
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // GESTIONAR CLICS DEL MENÚ
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // Lógica para Settings...
            return true;
        } else if (id == R.id.action_edit_user) {
            Intent intent = new Intent(this, EditActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // NAVEGAR CON EL ICONO DE LA IZQUIERDA
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
