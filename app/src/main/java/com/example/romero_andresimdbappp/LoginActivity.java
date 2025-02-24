package com.example.romero_andresimdbappp;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    //Variables
    private static final String TAG = "LoginActivity";
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager callbackManager;
    private EditText Email, Password;
    private Button btnEmailLogin, btnRegister;
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        // Recuperamos la cuenta de Google
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
        // Inicializamos Facebook SDK y activamos AppEvents
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        setContentView(R.layout.activity_login);
        // Inicializamos FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        // Si ya hay un usuario logueado, se redirige a MainActivity
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity(currentUser);
            finish();
            return;
        }
        // Referencias de la UI
        SignInButton signInButton = findViewById(R.id.btnSign);
        LoginButton loginButton = findViewById(R.id.btnFacebookLogin);
        Email = findViewById(R.id.etEmail);
        Password = findViewById(R.id.etPassword);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnRegister = findViewById(R.id.btnRegister);
        // Configuración para Google
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setOnClickListener(v -> {
            // Configuramos las opciones para Google
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            // Iniciamos el intent para Google Sign-In
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });

        // Configuración para Facebook
        callbackManager = CallbackManager.Factory.create();
        loginButton.setPermissions("public_profile", "email");
        // Registramos el callback para manejar la respuesta de Facebook
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

        // Configuración del botón para iniciar sesión con Email y Password
        btnEmailLogin.setOnClickListener(v -> {
            String email = Email.getText().toString().trim();
            String pass = Password.getText().toString().trim();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(LoginActivity.this, "Completa email y contraseña", Toast.LENGTH_SHORT).show();
            } else {
                signInWithEmailAndPassword(email, pass);
            }
        });

        // Configuración del botón para registrar usuario Email y Password
        btnRegister.setOnClickListener(v -> {
            String email = Email.getText().toString().trim();
            String pass = Password.getText().toString().trim();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(LoginActivity.this, "Completa email y contraseña", Toast.LENGTH_SHORT).show();
            } else {
                createUserWithEmailAndPassword(email, pass);
            }
        });
    }

    // Método para iniciar sesión con Email/Password
    private void signInWithEmailAndPassword(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            // Actualización o inserción en la BD local
                            String userId = user.getUid();
                            UsersDatabase usersDB = new UsersDatabase(this);
                            boolean exists = usersDB.userExists(userId);
                            String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                            if (!exists) {
                                // Si el usuario no existe, se agrega
                                usersDB.addUser(
                                        userId,
                                        user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                                        user.getEmail(),
                                        loginTime,
                                        null,
                                        "",  // address
                                        "",  // phone
                                        ""   // image
                                );
                            } else {
                                // Si existe, se actualiza su información
                                usersDB.updateLoginTime(userId, loginTime);
                                usersDB.updateUser(
                                        userId,
                                        user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                                        user.getEmail(),
                                        loginTime,
                                        null,
                                        null,
                                        null,
                                        null
                                );
                            }
                            // Se redirige a MainActivity
                            navigateToMainActivity(user);
                        }
                    } else {
                        Toast.makeText(this,
                                "Error al iniciar sesión: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Método para registrar usuario con Email/Password
    private void createUserWithEmailAndPassword(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            // Asigna un nombre genérico al usuario
                            String name = "Usuario_" + userId.substring(0, 5);
                            String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
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
                            // Sincroniza con Firestore
                            Userssync usersSync = new Userssync();
                            usersSync.syncBasicUserToFirestore(userId, name, email, "", "", "");
                            Toast.makeText(LoginActivity.this,
                                    "Usuario registrado correctamente",
                                    Toast.LENGTH_SHORT).show();
                            navigateToMainActivity(firebaseUser);
                        }
                    } else {
                        Toast.makeText(this,
                                "Error al registrar usuario: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Método para autenticarse con Google usando el token de la cuenta
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

    // Método para manejar el acceso con Facebook
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //Instanciamos UsersDatabase para registrar/actualizar datos
                    UsersDatabase usersDB = new UsersDatabase(this);
                    String userId = user.getUid();
                    //Verificamos si ya existe en la tabla 'users'
                    boolean exists = usersDB.userExists(userId);
                    //Obtenemos la hora de login
                    String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    //Si no existe, se agrega
                    if (!exists) {
                        usersDB.addUser(
                                userId,
                                user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                                user.getEmail(),
                                loginTime,   // login_time
                                null,        // logout_time
                                "", "", ""   // address, phone, image
                        );
                    } else {
                        // Si sí existe, solo actualizamos login_time
                        usersDB.updateLoginTime(userId, loginTime);
                        usersDB.updateUser(
                                userId,
                                user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                                user.getEmail(),
                                loginTime,   // login_time
                                null,        // logout_time
                                null, null, null
                        );
                    }

                    //Navegamos a MainActivity
                    navigateToMainActivity(user);
                }
            } else {
                Log.w(TAG, "signInWithCredential(Facebook):failure", task.getException());
                Toast.makeText(this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Método para redirigir a MainActivity una vez autenticado el usuario
    private void navigateToMainActivity(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Usuario no válido", Toast.LENGTH_SHORT).show();
            return;
        }
        // Sincronización con Firestore y BD local
        Userssync usersSync = new Userssync();
        usersSync.syncCurrentUserToFirestore();
        UsersDatabase usersDB = new UsersDatabase(this);
        usersSync.syncUsersWithFirestore(usersDB);
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

    //El resultado de Facebook al callbackManager
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
