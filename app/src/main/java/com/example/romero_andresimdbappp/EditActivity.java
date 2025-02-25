package com.example.romero_andresimdbappp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.romero_andresimdbappp.database.UsersDatabase;
import com.example.romero_andresimdbappp.sync.Userssync;
import com.facebook.login.LoginManager;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hbb20.CountryCodePicker;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class EditActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Constantes para permisos
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_LOCATION_PERMISSION = 300;
    // Declaración de variables
    private EditText Name, Email, Phone, Address;
    private ImageView ivProfileImage;
    private Button btnCamera, btnGallery, btnUrl, btnAddress, btnSave;
    private CountryCodePicker ccp;
    private Uri imageUri = null;
    private Map<String, String> currentUserData;
    private String userId;
    private UsersDatabase usersDatabase;
    private Userssync usersSync;
    private GoogleMap mMap;

    // Lanzador para abrir la galería
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        imageUri = selectedUri;
                        Glide.with(this).load(imageUri).into(ivProfileImage);
                    }
                }
            });

    // Lanzador para la cámara
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && imageUri != null) {
                    Glide.with(this).load(imageUri).into(ivProfileImage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        // Referencias a las vistas
        Name = findViewById(R.id.etName);
        Email = findViewById(R.id.etEmail);
        Phone = findViewById(R.id.etPhone);
        Address = findViewById(R.id.etAddress);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        btnUrl = findViewById(R.id.btnUrl);
        btnAddress = findViewById(R.id.btnPickAddress);
        btnSave = findViewById(R.id.btnSave);
        // Configurar CountryCodePicker para teléfono
        ccp = findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(Phone);

        // Inicializamos BD local y sincronizador
        usersDatabase = new UsersDatabase(this);
        usersSync = new Userssync();

        // Verificamos que haya un usuario logueado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();

        // Cargamos datos actuales del usuario desde la BD local
        currentUserData = usersDatabase.getUser(userId);
        if (currentUserData != null) {
            Log.d("EditActivity", "Datos usuario: " + currentUserData.toString());
            Name.setText(currentUserData.get("name") != null ? currentUserData.get("name") : "");
            Email.setText(currentUserData.get("email") != null ? currentUserData.get("email") : "");
            Address.setText(currentUserData.get("address") != null ? currentUserData.get("address") : "");
            String phoneFull = currentUserData.get("phone") != null ? currentUserData.get("phone") : "";
            if (!phoneFull.isEmpty()) {
                ccp.setFullNumber(phoneFull);
            }
            String imageStr = currentUserData.get("image") != null ? currentUserData.get("image") : "";
            if (!imageStr.isEmpty()) {
                imageUri = Uri.parse(imageStr);
                Glide.with(this).load(imageStr).into(ivProfileImage);
            }
        }

        // Configuramos el fragmento del mapa
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Verificamos permiso de localización
        checkLocationPermission();

        // Configuramos listeners de botones
        btnCamera.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                openCamera();
            }
        });

        btnGallery.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
                } else {
                    openGallery();
                }
            } else {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                } else {
                    openGallery();
                }
            }
        });

        btnUrl.setOnClickListener(v -> showUrlDialog());

        final ActivityResultLauncher<Intent> addressSelectLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String selectedAddress = result.getData().getStringExtra("SELECTED_ADDRESS");
                        if (selectedAddress != null) {
                            Address.setText(selectedAddress);
                        }
                        // Se pueden extraer latitud/longitud si es necesario
                    }
                });
        btnAddress.setOnClickListener(v -> {
            Intent intent = new Intent(EditActivity.this, AddressActivity.class);
            addressSelectLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveData());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    // Método para abrir la cámara
    private void openCamera() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Camera Image");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraLauncher.launch(cameraIntent);
    }

    // Método para abrir la galería
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // Diálogo para introducir URL de imagen
    private void showUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Introduce URL de imagen");
        final EditText input = new EditText(this);
        input.setHint("https://ejemplo.com/imagen.jpg");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                imageUri = Uri.parse(url);
                Glide.with(EditActivity.this).load(url).into(ivProfileImage);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Método que valida el número de teléfono usando libphonenumber
    private boolean isValidPhoneNumber(String phoneNumber, String countryCode) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            PhoneNumber numberProto = phoneUtil.parse(phoneNumber, countryCode);
            return phoneUtil.isValidNumberForRegion(numberProto, countryCode);
        } catch (NumberParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Guarda los datos actualizados del usuario
    private void saveData() {
        String newName = Name.getText().toString().trim();
        String newEmail = Email.getText().toString().trim();
        // Se limpia el número: elimina espacios extra
        String newPhoneFull = ccp.getFullNumberWithPlus().trim().replaceAll("\\s+", "");
        String newAddress = Address.getText().toString().trim();
        String oldImage = (currentUserData != null) ? currentUserData.get("image") : "";
        if (oldImage == null) oldImage = "";
        String newImage = (imageUri != null) ? imageUri.toString() : oldImage;

        // Validación del teléfono
        String countryCode = ccp.getSelectedCountryCode();        // Ejemplo: "34" para España
        String countryNameCode = ccp.getSelectedCountryNameCode();    // Ejemplo: "ES"

        boolean isLibPhoneValid = true;
        if (!newPhoneFull.isEmpty()) {
            isLibPhoneValid = isValidPhoneNumber(newPhoneFull, countryNameCode);
        }

        // Validación extra por switch: comprobamos la longitud del número (sin prefijo)
        boolean isSwitchValid = false;
        if (!newPhoneFull.isEmpty()) {
            String expectedPrefix = "+" + countryCode; // Ejemplo: "+34"
            if (newPhoneFull.startsWith(expectedPrefix)) {
                // Obtenemos la parte numérica sin el prefijo
                String numericPart = newPhoneFull.substring(expectedPrefix.length());
                int expectedLength = 0;
                switch (countryNameCode) {
                    case "ES":
                        expectedLength = 9; // España: 9 dígitos
                        break;
                    case "US":
                        expectedLength = 10; // Estados Unidos: 10 dígitos
                        break;
                    // Agrega más casos según las necesidades...
                    default:
                        // Si no se define, aceptamos el valor actual
                        expectedLength = numericPart.length();
                        break;
                }
                if (numericPart.length() == expectedLength) {
                    isSwitchValid = true;
                }
            }
        }

        // Si ninguno de los métodos de validación acepta el número, se muestra error.
        if (!newPhoneFull.isEmpty() && !isLibPhoneValid && !isSwitchValid) {
            Toast.makeText(this, "Número de teléfono inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizamos la BD local
        usersDatabase.updateUser(
                userId,
                newName,
                newEmail,
                null,
                null,
                newAddress,
                newPhoneFull,
                newImage
        );

        // Sincronizamos con Firestore
        usersSync.syncBasicUserToFirestore(
                userId,
                newName,
                newEmail,
                newAddress,
                newPhoneFull,
                newImage
        );

        Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show();
        finish();
    }



    // Método para solicitar permiso de localización
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    // Gestión de resultados de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
            } else {
                Toast.makeText(this, "Permiso de localización denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
