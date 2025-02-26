package com.example.romero_andresimdbappp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager callbackManager;
    private EditText Email, Password;
    private Button btnEmailLogin, btnRegister;

    // Lanzador para Google Sign-In
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
        // Inicializar Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        setContentView(R.layout.activity_login);

        // Inicializar FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity(currentUser);
            finish();
            return;
        }

        // Referencias de la UI
        SignInButton btnGoogleSignIn = findViewById(R.id.btnSign);
        LoginButton btnFacebookLogin = findViewById(R.id.btnFacebookLogin);
        Email = findViewById(R.id.etEmail);
        Password = findViewById(R.id.etPassword);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Configurar botón de Google
        btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
        btnGoogleSignIn.setOnClickListener(v -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });

        // Configurar Facebook
        callbackManager = CallbackManager.Factory.create();
        btnFacebookLogin.setPermissions("public_profile", "email");
        btnFacebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
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

        // Login con email y contraseña
        btnEmailLogin.setOnClickListener(v -> {
            String email = Email.getText().toString().trim();
            String password = Password.getText().toString().trim();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Completa email y contraseña", Toast.LENGTH_SHORT).show();
            } else {
                signInWithEmailAndPassword(email, password);
            }
        });

        // Registro de usuario con email y contraseña
        btnRegister.setOnClickListener(v -> {
            String email = Email.getText().toString().trim();
            String password = Password.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "El correo está vacío. Ejemplo: usuario@dominio.com", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "Correo inválido. Ejemplo: usuario@dominio.com", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                Toast.makeText(LoginActivity.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
            createUserWithEmailAndPassword(email, password);
        });
    }

    private void signInWithEmailAndPassword(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            actualizarUsuarioLocal(user);
                            navigateToMainActivity(user);
                        }
                    } else {
                        Exception e = task.getException();
                        if (e != null) {
                            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(this, "Correo o contraseña incorrectos.", Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseAuthInvalidUserException) {
                                Toast.makeText(this, "No existe una cuenta con este correo.", Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseNetworkException) {
                                Toast.makeText(this, "No hay conexión a internet.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Error desconocido al iniciar sesión.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void createUserWithEmailAndPassword(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            // Nombre provisional basado en el UID
                            String name = "Usuario_" + userId.substring(0, 5);
                            String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                            UsersDatabase usersDB = new UsersDatabase(this);
                            usersDB.addUser(userId, name, email, loginTime, null, "", "", "");
                            // Sincronizar con Firestore
                            new Userssync().syncBasicUserToFirestore(userId, name, email, "", "", "");
                            Toast.makeText(LoginActivity.this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity(firebaseUser);
                        }
                    } else {
                        Toast.makeText(this, "Error al registrar usuario: " +
                                (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                    }
                });
    }

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
                        if (user != null) {
                            actualizarUsuarioLocal(user);
                            navigateToMainActivity(user);
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential(Google):failure", task.getException());
                        Toast.makeText(this, "Error al autenticar con Google", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            actualizarUsuarioLocal(user);
                            navigateToMainActivity(user);
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential(Facebook):failure", task.getException());
                        Toast.makeText(this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Actualiza o inserta información del usuario en la BD local
    private void actualizarUsuarioLocal(FirebaseUser user) {
        String userId = user.getUid();
        String displayName = (user.getDisplayName() != null) ? user.getDisplayName() : "Usuario";
        String email = user.getEmail();
        String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        UsersDatabase usersDB = new UsersDatabase(this);
        if (usersDB.userExists(userId)) {
            usersDB.updateLoginTime(userId, loginTime);
            usersDB.updateUser(userId, displayName, email, loginTime, null, null, null, null);
        } else {
            usersDB.addUser(userId, displayName, email, loginTime, null, "", "", "");
        }
    }

    private void navigateToMainActivity(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Usuario no válido", Toast.LENGTH_SHORT).show();
            return;
        }
        // Sincronización con Firestore y BD local
        new Userssync().syncCurrentUserToFirestore();
        new Userssync().syncUsersWithFirestore(new UsersDatabase(this));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Delegar el resultado a Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
