package com.bonyan.rtd;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESRestCrypto {
    private static final Logger logger = Logger.getLogger(AESRestCrypto.class.getName());
    private static final String ENCRYPTION_KEY = "0123456789abcdef";

    public static void main(String[] arg) {
        try {
            String st = "";
            if (arg[0].equals("-d")) {
                st = arg[1];
                if (arg[1].contains("@0") && arg[1].split("@0").length == 16) {
                    printDecryptedPassword(st);
                } else {

                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Not a valid AES Rest encrypted password: " + arg[1]);
                    }
                }
            } else {
                printEncryptedPassword(arg[0]);
            }
        } catch (Exception exception) {
            logger.info(exception.getMessage());
        }

    }

    public static String encryptPassword(String pass) {
        try {
            return Arrays.toString(encrypt(pass, ENCRYPTION_KEY))
                    .replace("\\[", "").replace("\\]", "")
                    .replace(" ", "").replace(",", "@0");
        } catch (Exception var2) {
            return "";
        }
    }

    public static void printEncryptedPassword(String pass) {
        try {
            String[] encryptedPass = encryptPassword(pass).split("@0");
            StringBuilder newPass = new StringBuilder();
            int encryptedPassArrayLength = encryptedPass.length;

            for(int i = 0; i < encryptedPassArrayLength; ++i) {
                String el = encryptedPass[i];
                newPass.append(newPass).append(el).append("@0");
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("EncryptedPass: " + newPass);
            }

        } catch (Exception exception) {
            logger.info(exception.getMessage());
        }

    }

    public static void printDecryptedPassword(String encryptedPass) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("DecryptedPass: " + decryptPassword(encryptedPass));
        }
    }

    public static String decryptPassword(String encryptedPass) {
        String[] newArray = encryptedPass.split("@0");
        ByteBuffer b = ByteBuffer.allocate(newArray.length);
        String[] var6 = newArray;
        int var5 = newArray.length;

        for(int var4 = 0; var4 < var5; ++var4) {
            String array = var6[var4];
            b.put((new Integer(array)).byteValue());
        }

        try {
            return decrypt(b.array(), ENCRYPTION_KEY);
        } catch (Exception var7) {
            return "";
        }
    }

    private static byte[] encrypt(String plainText, String encryptionKey)
            throws InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException,
            NoSuchPaddingException, NoSuchAlgorithmException {
        // Generate a random IV
        byte[] iv = new byte[16]; // 16 bytes for AES
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Create a SecretKeySpec for the encryption key
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");

        // Initialize the Cipher with AES/CBC/PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

        // Encrypt the plaintext
        byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Prepend the IV to the encrypted data
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        return byteBuffer.array();
    }

    private static String decrypt(byte[] encryptedData, String encryptionKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

        byte[] iv = new byte[16];
        byteBuffer.get(iv);

        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

        byte[] decryptedData = cipher.doFinal(cipherText);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}
