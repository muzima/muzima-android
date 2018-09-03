/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.os.Environment;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public class EnDeCrypt {
	

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String KEY_ALGORITHM = "AES";

    // This is the best algorithm but its not implemented in 2.2 and below
    private static final String PASSWORD_HASH_ALGORITHM = "PBKDF2WithHmacSHA1";

    private static final String PASSWORD_HASH_ALGORITHM_FROYO = "PBEWITHSHAAND256BITAES-CBC-BC";

    private static final String TEMP_FOLDER = Environment.getExternalStorageDirectory().getPath() + "/muzima/tmp/";

    private static final int SALT_LENGTH = 8;

    private static final SecureRandom random = new SecureRandom();

    private static final String DELIMITER = "]";


    public static void encrypt(File plainFile, String password) {
        try {
            File tmpFolder = new File(TEMP_FOLDER);
            if (!tmpFolder.exists())
                tmpFolder.mkdirs();

            File tempFile = new File(TEMP_FOLDER + plainFile.getName());
            FileInputStream fis = new FileInputStream(plainFile);
            FileOutputStream fos = new FileOutputStream(tempFile);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            byte[] iv;
            byte[] salt;

            salt = generateSalt();
            iv = generateIv(cipher.getBlockSize());
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            SecretKey key = deriveKey(password, salt);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);

            byte[] plainFileBytes = new byte[(int) plainFile.length()];
            fis.read(plainFileBytes);
            byte[] encryptedStream = cipher.doFinal(plainFileBytes);

            String cipherText;
            if (salt != null)
                cipherText = String.format("%s%s%s%s%s", MediaUtils.toBase64(salt), DELIMITER,
                        MediaUtils.toBase64(iv), DELIMITER, MediaUtils.toBase64(encryptedStream));
            else
                cipherText = MediaUtils.toBase64(encryptedStream);

            // write the encrypted stream to file together with salt and IV
            fos.write(cipherText.getBytes());

            // and clean up
            fos.flush();
            fos.close();

            // remove the temporary file by transferring the file as appropriate
            tempFile.renameTo(new File(plainFile.getAbsolutePath()));
            Log.i("En DeCrypt", "Encrypted " + plainFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("En DeCrypt", e.getMessage(), e);
        }
    }

    public static void decrypt(File encryptedFile, String password) {
        try {
            File tmpFolder = new File(TEMP_FOLDER);
            if (!tmpFolder.exists())
                tmpFolder.mkdirs();

            File tempFile = new File(TEMP_FOLDER + encryptedFile.getName());
            FileOutputStream fos = new FileOutputStream(tempFile);

            BufferedReader bufferedReader = new BufferedReader(new FileReader(encryptedFile));
            StringBuilder builder = new StringBuilder();
            String cipherText;
            String line;
            while ( (line = bufferedReader.readLine()) != null ) {
                builder.append(line);
            }

            cipherText = builder.toString();

            String[] fields = cipherText.split(DELIMITER);
            if (fields.length != 3) {
                throw new IllegalArgumentException("Invalid encrypted text format");
            }

            byte[] salt = MediaUtils.fromBase64(fields[0]);
            byte[] iv = MediaUtils.fromBase64(fields[1]);
            byte[] cipherBytes = MediaUtils.fromBase64(fields[2]);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), ivParams);
            byte[] plainText = cipher.doFinal(cipherBytes);

            // write the decrypted stream to file
            fos.write(plainText);

            fos.flush();
            fos.close();

            tempFile.renameTo(encryptedFile);
            Log.i("En DeCrypt", "Decrypted " + encryptedFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("En DeCrypt", e.getMessage(), e);
        }

    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        // minimum values recommended by PKCS#5
        int ITERATION_COUNT = 1000;
        int KEY_LENGTH = 256;

		/* Use this to securely derive the key from the password: */
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);

        SecretKeyFactory keyFactory;
        keyFactory = SecretKeyFactory.getInstance(PASSWORD_HASH_ALGORITHM);

        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    private static byte[] generateIv(int length) {
        byte[] ivBytes = new byte[length];
        random.nextBytes(ivBytes);

        return ivBytes;
    }

    private static byte[] generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH];
        random.nextBytes(saltBytes);

        return saltBytes;
    }
}