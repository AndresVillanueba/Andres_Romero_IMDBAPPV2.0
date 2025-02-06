package com.example.romero_andresimdbappp;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.romero_andresimdbappp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    //Variables declaradas mas las que vienen por defecto
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private TextView CorreoUsuario;
    private TextView InicialUsuario;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recibe los datos enviados desde LoginActivity (si existen)
        Intent intent = getIntent();
        String usuario = intent.getStringExtra("USER_NAME");
        String email = intent.getStringExtra("USER_EMAIL");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);
        // Configuración de Google Sign-In y Firebase Auth
        configureGoogleSignIn();
        mAuth = FirebaseAuth.getInstance();
        // Obtener las vistas del header
        View headerView = binding.navView.getHeaderView(0);
        CorreoUsuario = headerView.findViewById(R.id.nav_email);
        InicialUsuario = headerView.findViewById(R.id.nav_header_initial);
        // Configurar Navigation
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(binding.drawerLayout)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        // Configurar los botones del Header para Sign In y Sign Out
        headerView.findViewById(R.id.btn_google_sign_in).setOnClickListener(v -> signIn());
        headerView.findViewById(R.id.btn_google_sign_out).setOnClickListener(v -> signOut());
        // Actualiza la UI del Header según se hayan recibido datos de usuario
        if (email != null) {
            CorreoUsuario.setText(email);
            if (usuario != null && !usuario.isEmpty()) {
                InicialUsuario.setText(String.valueOf(usuario.charAt(0)).toUpperCase());
            }
            headerView.findViewById(R.id.btn_google_sign_in).setVisibility(View.GONE);
            headerView.findViewById(R.id.btn_google_sign_out).setVisibility(View.VISIBLE);
        } else {
            headerView.findViewById(R.id.btn_google_sign_in).setVisibility(View.VISIBLE);
            headerView.findViewById(R.id.btn_google_sign_out).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Infla el menú; añade elementos a la action bar si están presentes.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String email = user.getEmail();
                            String displayName = user.getDisplayName();
                            CorreoUsuario.setText(email);
                            if (displayName != null && !displayName.isEmpty()) {
                                String initial = String.valueOf(displayName.charAt(0)).toUpperCase();
                                InicialUsuario.setText(initial);
                            }
                            findViewById(R.id.btn_google_sign_in).setVisibility(View.GONE);
                            findViewById(R.id.btn_google_sign_out).setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

}
