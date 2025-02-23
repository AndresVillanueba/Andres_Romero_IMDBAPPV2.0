package com.example.romero_andresimdbappp;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
// etc.

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
// ...
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.romero_andresimdbappp.database.UsersDatabase;
import com.example.romero_andresimdbappp.sync.Userssync;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Firebase
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Facebook
    private CallbackManager callbackManager;

    // Vistas para Email/Password
    private EditText etEmail, etPassword;
    private Button btnEmailLogin, btnRegister;

    // Google Sign-In launcher
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Error en Google Sign-In", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());

        setContentView(R.layout.activity_login);

        // Instancia de Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        // Comprobar si hay usuario logueado
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity(currentUser);
            finish();
            return;
        }

        // Referencias de la UI
        SignInButton signInButton = findViewById(R.id.btnSign);         // Google
        LoginButton loginButton = findViewById(R.id.btnFacebookLogin);  // Facebook
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Configuración Google Sign-In
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setOnClickListener(v -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            // Lanzamos intent
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });

        // Facebook
        callbackManager = CallbackManager.Factory.create();
        // Pedir permisos
        loginButton.setPermissions("public_profile", "email");
        // Registrar callback
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Login con Facebook cancelado", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "Facebook login error", error);
                Toast.makeText(LoginActivity.this, "Error en Facebook Login: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Botón Iniciar Sesión con Email
        btnEmailLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Completa email y contraseña", Toast.LENGTH_SHORT).show();
            } else {
                signInWithEmailAndPassword(email, pass);
            }
        });

        // Botón Registrar (Email/Password)
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Completa email y contraseña", Toast.LENGTH_SHORT).show();
            } else {
                createUserWithEmailAndPassword(email, pass);
            }
        });
    }

    // ============ LOGIN con email ============
    private void signInWithEmailAndPassword(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            navigateToMainActivity(user);
                        }
                    } else {
                        Toast.makeText(this,
                                "Error al iniciar sesión: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ============ REGISTRO con email ============
    private void createUserWithEmailAndPassword(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            // Asigna un nombre "genérico" o crea un EditText para nombre
                            String name = "Usuario_" + userId.substring(0, 5);

                            // Guardar en BD local => login_time
                            String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(new Date());

                            UsersDatabase usersDB = new UsersDatabase(this);
                            usersDB.addUser(
                                    userId,
                                    name,
                                    email,
                                    loginTime,
                                    null, // logout_time
                                    "",   // address
                                    "",   // phone
                                    ""    // image
                            );

                            // Subir datos básicos a Firestore
                            Userssync usersSync = new Userssync();
                            usersSync.syncBasicUserToFirestore(userId, name, email, "", "", "");

                            Toast.makeText(LoginActivity.this,
                                    "Usuario registrado correctamente",
                                    Toast.LENGTH_SHORT).show();

                            // Navegar a MainActivity
                            navigateToMainActivity(firebaseUser);
                        }
                    } else {
                        Toast.makeText(this,
                                "Error al registrar usuario: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ============ Google SignIn ============
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        if (account == null) {
            Toast.makeText(this, "Cuenta de Google inválida", Toast.LENGTH_SHORT).show();
            return;
        }
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        navigateToMainActivity(user);
                    } else {
                        Log.w(TAG, "signInWithCredential(Google):failure", task.getException());
                        Toast.makeText(this, "Error al autenticar con Google", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============ Facebook SignIn ============
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                navigateToMainActivity(user);
            } else {
                Log.w(TAG, "signInWithCredential(Facebook):failure", task.getException());
                Toast.makeText(this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============ Navegar a MainActivity ============
    private void navigateToMainActivity(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Usuario no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Sincronizar login con Firestore
        Userssync usersSync = new Userssync();
        usersSync.syncCurrentUserToFirestore();

        // 2) Sincronizar con la BD local
        UsersDatabase usersDB = new UsersDatabase(this);
        usersSync.syncUsersWithFirestore(usersDB);

        // 3) Ir a MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_NAME", user.getDisplayName());
        intent.putExtra("USER_EMAIL", user.getEmail());
        intent.putExtra("USER_UID", user.getUid());
        if (user.getPhotoUrl() != null) {
            intent.putExtra("USER_PHOTO", user.getPhotoUrl().toString());
        }
        startActivity(intent);
        finish();
    }

    // Delegar resultado a callbackManager de Facebook
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
