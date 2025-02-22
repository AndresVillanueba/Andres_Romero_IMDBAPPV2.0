package com.example.romero_andresimdbappp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private TextView navEmail, navInitial;
    private ImageView navProfileImage;

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

        // Header del NavigationView
        View headerView = binding.navView.getHeaderView(0);
        navEmail = headerView.findViewById(R.id.nav_email);
        navInitial = headerView.findViewById(R.id.nav_header_initial);
        navProfileImage = headerView.findViewById(R.id.nav_profile_image);
        // Obtenemos el botón de logout desde el header
        Button btnLogout = headerView.findViewById(R.id.btn_google_sign_out);

        // Mostrar datos de usuario
        String email = currentUser.getEmail();
        String displayName = currentUser.getDisplayName();
        navEmail.setText(email != null ? email : "Sin email");
        if (displayName != null && !displayName.isEmpty()) {
            navInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
        } else {
            navInitial.setText("U");
        }
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.default_user_image)
                    .into(navProfileImage);
        } else {
            navProfileImage.setImageResource(R.drawable.default_user_image);
        }

        // Hacemos visible y asignamos el listener al botón de logout
        btnLogout.setVisibility(View.VISIBLE);
        btnLogout.setOnClickListener(v -> {
            // Declaramos final
            final Userssync usersSync = new Userssync();
            final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            final String logoutTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());

            // 1) Actualizar logout_time en Firestore
            usersSync.updateLogoutTime();

            // 2) Actualizar logout_time en la base de datos local
            new UsersDatabase(MainActivity.this).updateLogoutTime(userId, logoutTime);

            // 3) Cerrar sesión
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();

            // 4) Ir a LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Configuración Navigation Drawer
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
        ).setOpenableLayout(binding.drawerLayout)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
