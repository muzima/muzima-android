package com.muzima.model;

import android.content.Context;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;


import com.google.android.gms.common.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PassphraseStorage {
    private Context context;
    public PassphraseStorage(Context context){
        this.context = context;
    }

    public byte[] getPassphrase() throws GeneralSecurityException, IOException {
        String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        File file = new File(context.getFilesDir(), "secret_data");
        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                file, context, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        byte[] passPhrase;
        if(!file.exists()){
            passPhrase = generatePassphrase();
             FileOutputStream outputStream = encryptedFile.openFileOutput();
             outputStream.write(passPhrase);
             outputStream.close();
        } else {
            FileInputStream inputStream = encryptedFile.openFileInput();
            passPhrase = IOUtils.toByteArray(inputStream);
        }
        return passPhrase;
    }

    private byte[] generatePassphrase() throws NoSuchAlgorithmException {
        SecureRandom random = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            random = SecureRandom.getInstanceStrong();
        }
        byte[] result = new byte[32];
        random.nextBytes(result);
        return result;
    }
}
