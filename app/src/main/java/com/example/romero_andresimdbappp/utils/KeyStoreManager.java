package com.example.romero_andresimdbappp.utils;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Clase para gestionar la generación de la clave (AES) en el Android KeyStore y
 * el cifrado/descifrado de datos sensibles (ej: dirección, teléfono).
 */
public class KeyStoreManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    // Alias que usarás para identificar la clave dentro del Keystore
    private static final String KEY_ALIAS = "MyKeyAlias";

    // Modo/Ajustes de cifrado
    private static final String TRANSFORMATION =
            KeyProperties.KEY_ALGORITHM_AES + "/" +
                    KeyProperties.BLOCK_MODE_GCM + "/" +
                    KeyProperties.ENCRYPTION_PADDING_NONE;

    /**
     * Método principal para cifrar un texto cualquiera (String).
     * Devuelve la cadena codificada en Base64 que incluye el IV y el texto cifrado.
     *
     * @param plainText Texto en claro a cifrar
     * @return Texto cifrado en base64 con IV incluido, o null si hay error
     */
    public static String encryptData(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            // Obtenemos o generamos la clave
            Key secretKey = getOrCreateKey();

            // Configuramos el Cipher en modo ENCRYPT
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Obtenemos el IV que generó GCM
            byte[] iv = cipher.getIV();
            // Ciframos
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Unimos IV + texto cifrado en un solo array de bytes
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            // Codificamos todo a Base64
            return Base64.encodeToString(combined, Base64.DEFAULT);

        } catch (Exception e) {
            Log.e("KeyStoreManager", "Error cifrando datos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Método para descifrar un String en Base64 que contenga [IV + Texto cifrado].
     *
     * @param encryptedBase64 Cadena con IV + Texto cifrado (codificada en Base64)
     * @return Texto en claro, o null si hay error
     */
    public static String decryptData(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return null;
        }

        try {
            // Decodificamos Base64
            byte[] combined = Base64.decode(encryptedBase64, Base64.DEFAULT);

            // El IV de GCM suele ser de 12 bytes (o 16), según configuración
            // Por defecto en Android KeyStore = 12 bytes para GCM
            int ivLength = 12;
            byte[] iv = new byte[ivLength];
            byte[] encryptedBytes = new byte[combined.length - ivLength];

            System.arraycopy(combined, 0, iv, 0, ivLength);
            System.arraycopy(combined, ivLength, encryptedBytes, 0, encryptedBytes.length);

            // Obtenemos la clave del Keystore
            Key secretKey = getOrCreateKey();

            // Configuramos el Cipher en modo DECRYPT con el IV
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            // Desciframos
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, "UTF-8");

        } catch (Exception e) {
            Log.e("KeyStoreManager", "Error descifrando datos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Devuelve la clave AES del Keystore si ya existe, o la genera en caso contrario.
     */
    private static Key getOrCreateKey()
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, KeyStoreException,
            CertificateException, IOException, UnrecoverableKeyException {

        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        Key existingKey = keyStore.getKey(KEY_ALIAS, null);
        if (existingKey != null) {
            // Ya existe la clave generada
            return existingKey;
        }

        // Creamos la clave si no existe
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256) // AES 256 bits
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false) // Ajusta según tu necesidad
                .build();

        keyGenerator.init(keyGenParameterSpec);
        return keyGenerator.generateKey();
    }

    /**
     * (OPCIONAL) Ejemplo de cómo podrías generar una clave simétrica "manualmente"
     * fuera del Keystore, si quisieras un fallback. Pero NO se recomienda
     * almacenar la clave manualmente en disco sin cifrar.
     */
    private static Key generateSymmetricKeyFallback() {
        try {
            byte[] keyBytes = new byte[32]; // 256 bits
            SecureRandom random = new SecureRandom();
            random.nextBytes(keyBytes);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
