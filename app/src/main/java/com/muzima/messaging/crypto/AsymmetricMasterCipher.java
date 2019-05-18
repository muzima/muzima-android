package com.muzima.messaging.crypto;

import com.muzima.messaging.utils.Conversions;
import com.muzima.messaging.utils.Util;
import com.muzima.utils.Base64;

import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AsymmetricMasterCipher {
    private final AsymmetricMasterSecret asymmetricMasterSecret;

    public AsymmetricMasterCipher(AsymmetricMasterSecret asymmetricMasterSecret) {
        this.asymmetricMasterSecret = asymmetricMasterSecret;
    }

    public byte[] encryptBytes(byte[] body) {
        try {
            ECPublicKey theirPublic        = asymmetricMasterSecret.getDjbPublicKey();
            ECKeyPair ourKeyPair         = Curve.generateKeyPair();
            byte[]       secret             = Curve.calculateAgreement(theirPublic, ourKeyPair.getPrivateKey());
            MasterCipher masterCipher       = getMasterCipherForSecret(secret);
            byte[]       encryptedBodyBytes = masterCipher.encryptBytes(body);

            PublicKey ourPublicKey       = new PublicKey(31337, ourKeyPair.getPublicKey());
            byte[]       publicKeyBytes     = ourPublicKey.serialize();

            return Util.combine(publicKeyBytes, encryptedBodyBytes);
        } catch (InvalidKeyException e) {
            throw new AssertionError(e);
        }
    }

    public byte[] decryptBytes(byte[] combined) throws IOException, InvalidMessageException {
        try {
            byte[][]  parts          = Util.split(combined, PublicKey.KEY_SIZE, combined.length - PublicKey.KEY_SIZE);
            PublicKey theirPublicKey = new PublicKey(parts[0], 0);

            ECPrivateKey ourPrivateKey = asymmetricMasterSecret.getPrivateKey();
            byte[]       secret        = Curve.calculateAgreement(theirPublicKey.getKey(), ourPrivateKey);
            MasterCipher masterCipher  = getMasterCipherForSecret(secret);

            return masterCipher.decryptBytes(parts[1]);
        } catch (InvalidKeyException e) {
            throw new InvalidMessageException(e);
        }
    }

    public String decryptBody(String body) throws IOException, InvalidMessageException {
        byte[] combined = Base64.decode(body);
        return new String(decryptBytes(combined));
    }

    public String encryptBody(String body) {
        return Base64.encodeBytes(encryptBytes(body.getBytes()));
    }

    private MasterCipher getMasterCipherForSecret(byte[] secretBytes) {
        SecretKeySpec cipherKey   = deriveCipherKey(secretBytes);
        SecretKeySpec macKey      = deriveMacKey(secretBytes);
        MasterSecret masterSecret = new MasterSecret(cipherKey, macKey);

        return new MasterCipher(masterSecret);
    }

    private SecretKeySpec deriveMacKey(byte[] secretBytes) {
        byte[] digestedBytes = getDigestedBytes(secretBytes, 1);
        byte[] macKeyBytes   = new byte[20];

        System.arraycopy(digestedBytes, 0, macKeyBytes, 0, macKeyBytes.length);
        return new SecretKeySpec(macKeyBytes, "HmacSHA1");
    }

    private SecretKeySpec deriveCipherKey(byte[] secretBytes) {
        byte[] digestedBytes  = getDigestedBytes(secretBytes, 0);
        byte[] cipherKeyBytes = new byte[16];

        System.arraycopy(digestedBytes, 0, cipherKeyBytes, 0, cipherKeyBytes.length);
        return new SecretKeySpec(cipherKeyBytes, "AES");
    }

    private byte[] getDigestedBytes(byte[] secretBytes, int iteration) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            return mac.doFinal(Conversions.intToByteArray(iteration));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new AssertionError(e);
        }
    }
}
