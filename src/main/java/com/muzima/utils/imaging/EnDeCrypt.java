package com.muzima.utils.imaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class EnDeCrypt {
	
	public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
	public static final String KEY_ALGORITHM = "AES"; 
	// This is the best algorithm but its not implemented in 2.2 and below
	public static final String PASSWORD_HASH_ALGORITHM = "PBKDF2WithHmacSHA1";
	
	public static final String PASSWORD_HASH_ALGORITHM_FROYO = "SHA1PRNG";
	public static final String TEMP_FOLDER =  Environment.getExternalStorageDirectory().getPath() + "/muzima/tmp/";
	private static String TAG = "EnDeCrypt";
	
	public static void encrypt(File plainFile, String password) {
		try {
			File tmpFolder = new File(TEMP_FOLDER);
			if (!tmpFolder.exists())
				tmpFolder.mkdirs();
			File tempFile = new File(TEMP_FOLDER + plainFile.getName());
		    FileInputStream fis = new FileInputStream(plainFile);
		    FileOutputStream fos = new FileOutputStream(tempFile);
	
		    Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		    //Froyo (API 8) and below have no implementation of the more secure PBKDF2 algorithm
		    if (Build.VERSION.SDK_INT <= 8 )
		    	cipher.init(Cipher.ENCRYPT_MODE, buildKeyFroyo(password));
		    else
		    	cipher.init(Cipher.ENCRYPT_MODE, buildKey(password));
		    	
		    CipherOutputStream cos = new CipherOutputStream(fos, cipher);
		    int b;
		    byte[] d = new byte[8];
		    while((b = fis.read(d)) != -1) {
		        cos.write(d, 0, b);
		    }
		    cos.flush();
		    cos.close();
		    fis.close();
		    
		    tempFile.renameTo(new File(plainFile.getAbsolutePath()));
		    Log.i(TAG, "Encrypted " + plainFile.getAbsolutePath());
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}
	
	public static void decrypt(File encryptedFile, String password) {
		try {
			File tmpFolder = new File(TEMP_FOLDER);
			if (!tmpFolder.exists())
				tmpFolder.mkdirs();
			File tempFile = new File(TEMP_FOLDER + encryptedFile.getName());
			FileInputStream fis = new FileInputStream(encryptedFile);
		    FileOutputStream fos = new FileOutputStream(tempFile);
		    
		    Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		    //Froyo (API 8) and below have no implementation of the more secure PBKDF2 algorithm
		    if (Build.VERSION.SDK_INT <= 8 )
		    	cipher.init(Cipher.DECRYPT_MODE, buildKeyFroyo(password));
		    else
		    	cipher.init(Cipher.DECRYPT_MODE, buildKey(password));
		    CipherInputStream cis = new CipherInputStream(fis, cipher);
		    int b;
		    byte[] d = new byte[8];
		    while((b = cis.read(d)) != -1) {
		        fos.write(d, 0, b);
		    }
		    fos.flush();
		    fos.close();
		    cis.close();
		    tempFile.renameTo(encryptedFile);
		    Log.i(TAG, "Decrypt " + encryptedFile.getAbsolutePath());
		}catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}
	
   private static Key buildKeyFroyo(String strPassword) throws Exception {
	   byte[] password = strPassword.getBytes();
       KeyGenerator kgen = KeyGenerator.getInstance(KEY_ALGORITHM);
       SecureRandom sr = SecureRandom.getInstance(PASSWORD_HASH_ALGORITHM_FROYO);
       sr.setSeed(password);
       kgen.init(128, sr); // 192 and 256 bits may not be available
       SecretKey skey = kgen.generateKey();
       return new SecretKeySpec(skey.getEncoded(), KEY_ALGORITHM);
   }
   
   private static Key buildKey(String password) throws Exception {
   
	   int iterationCount = 1000;
	   int saltLength = 32; // bytes; should be the same size as the output (256 / 8 = 32)
	   int keyLength = 256; // 256-bits for AES-256, 128-bits for AES-128, etc
	   byte[] salt; // Should be of saltLength
	
	   /* When first creating the key, obtain a salt with this: */
	   SecureRandom random = new SecureRandom();
	   salt = new byte[saltLength];
	   random.nextBytes(salt);
	
	   /* Use this to derive the key from the password: */
	   KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
	   SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(PASSWORD_HASH_ALGORITHM);
	   byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
	    
	   return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
   }
}