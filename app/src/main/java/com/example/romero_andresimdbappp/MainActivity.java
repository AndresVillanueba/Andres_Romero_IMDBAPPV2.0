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
import com.example.romero_andresimdbappp.database.UsersDatabase;
import com.example.romero_andresimdbappp.databinding.ActivityMainBinding;
import com.example.romero_andresimdbappp.sync.Userssync;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    //Variables
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    private TextView navEmail, navInitial;
    private ImageView navProfileImage;
    private UsersDatabase Usersdatabase;

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

        // Referencias del header en el NavigationView
        View headerView = binding.navView.getHeaderView(0);
        if (headerView != null) {
            navEmail = headerView.findViewById(R.id.nav_email);
            navInitial = headerView.findViewById(R.id.nav_user_name);
            navProfileImage = headerView.findViewById(R.id.nav_profile_image);
        } else {
            Log.e("MainActivity", "El headerView es nulo. Verifica que el NavigationView tenga un header definido.");
        }

        // Botón Logout en el header
        Button btnLogout = headerView.findViewById(R.id.btn_google_sign_out);
        btnLogout.setVisibility(View.VISIBLE);
        btnLogout.setOnClickListener(v -> {
            // Actualizar logout_time en Firestore y en local
            Userssync usersSync = new Userssync();
            usersSync.updateLogoutTime();

            final String userId = currentUser.getUid();
            final String logoutTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            new UsersDatabase(MainActivity.this).updateLogoutTime(userId, logoutTime);

            // Cerrar sesión en Firebase y Facebook
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();

            // Volver a LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Inicializamos la BD local
        Usersdatabase = new UsersDatabase(this);

        // Config Nav
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

    @Override
    protected void onResume() {
        super.onResume();
        // Cargamos usuario actual de FirebaseAuth
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // Obtenemos datos de la BD local
            Map<String, String> userData = Usersdatabase.getUser(userId);
            if (userData != null) {
                // Recuperamos "name" guardado (lo que el usuario escribió en EditActivity)
                String fullName = userData.get("name");
                // Recuperamos el email de la BD local (o de Firebase, equivaldría)
                String emailBD = userData.get("email");

                // Lógica para mostrar un nombre provisional si no hay nada en BD
                // Si fullName existe y no está vacío, lo mostramos
                if (fullName != null && !fullName.isEmpty()) {
                    navInitial.setText(fullName);
                } else {
                    // Si no hay un nombre, tomamos la parte antes de la '@' del email
                    if (emailBD != null && !emailBD.isEmpty()) {
                        int idxArroba = emailBD.indexOf("@");
                        if (idxArroba > 0) {
                            // Ej: andresjulian6a@gmail.com andresjulian6a
                            navInitial.setText(emailBD.substring(0, idxArroba));
                        } else {
                            // Si no hay arroba, mostramos el email completo
                            navInitial.setText(emailBD);
                        }
                    } else {
                        // Si ni siquiera hay email, mostramos "Usuario"
                        navInitial.setText("Usuario");
                    }
                }

                // Mostramos email
                navEmail.setText(emailBD != null && !emailBD.isEmpty() ? emailBD : "Sin email");
                // Imagen de perfil
                String imageStr = userData.get("image");
                if (imageStr != null && !imageStr.isEmpty()) {
                    Glide.with(this)
                            .load(imageStr)
                            .placeholder(R.drawable.default_user_image)
                            .into(navProfileImage);
                } else {
                    // Si no hay imagen, placeholder
                    navProfileImage.setImageResource(R.drawable.default_user_image);
                }
            } else {
                // Si userData es nulo (no hay en BD local), ponemos algo genérico
                navInitial.setText("Usuario");
                navEmail.setText("Sin email");
                navProfileImage.setImageResource(R.drawable.default_user_image);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflamos el menú
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Gestión de clics en el menú
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // Opciones de configuración
            return true;
        } else if (id == R.id.action_edit_user) {
            // Abrir EditActivity para editar datos
            Intent intent = new Intent(this, EditActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Gestión del ícono de la izquierda
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
