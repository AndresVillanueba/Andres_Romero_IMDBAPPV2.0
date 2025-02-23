package com.example.romero_andresimdbappp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;

public class AddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 300;
    private EditText etAddressInput;
    private Button btnSearch, btnConfirm;
    private GoogleMap mMap;
    private LatLng foundLatLng; // lat/lng resultante del geocoding
    private String chosenAddress; // dirección final

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        etAddressInput = findViewById(R.id.etAddressInput);
        btnSearch = findViewById(R.id.btnSearch);
        btnConfirm = findViewById(R.id.btnConfirm);

        // Fragmento del mapa
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // (Opcional) Pedir permiso de localización si quisieras MyLocation
        checkLocationPermission();

        // Al pulsar "BUSCAR DIRECCIÓN", hace geocoding y muestra marcador
        btnSearch.setOnClickListener(v -> {
            String typedAddress = etAddressInput.getText().toString().trim();
            if (typedAddress.isEmpty()) {
                Toast.makeText(this, "Introduce una dirección", Toast.LENGTH_SHORT).show();
                return;
            }
            geocodeAddress(typedAddress);
        });

        // Al pulsar "CONFIRMAR DIRECCIÓN", se devuelve la dirección a EditActivity
        btnConfirm.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            // Devuelve la dirección
            resultIntent.putExtra("SELECTED_ADDRESS", chosenAddress);
            // Si quieres, también lat/lng
            if (foundLatLng != null) {
                resultIntent.putExtra("LATITUDE", foundLatLng.latitude);
                resultIntent.putExtra("LONGITUDE", foundLatLng.longitude);
            }
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    /**
     * Cuando el mapa está listo
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Opcional: habilitar controles de zoom
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    /**
     * Convierte la dirección en lat/lng con Geocoder y actualiza el mapa.
     */
    private void geocodeAddress(String addressStr) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(this, "No se encontró la dirección", Toast.LENGTH_SHORT).show();
            } else {
                Address address = addresses.get(0);
                double lat = address.getLatitude();
                double lng = address.getLongitude();
                foundLatLng = new LatLng(lat, lng);

                // Toma la dirección completa que geocoder devolvió
                chosenAddress = address.getAddressLine(0);

                if (mMap != null) {
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions()
                            .position(foundLatLng)
                            .title(chosenAddress));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(foundLatLng, 15f));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al geocodificar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Maneja el permiso si quisieras habilitar MyLocation
    }
}
