package com.warn.util;

import com.warn.util.common.Const;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Commons {
    public static final String ALGORITHM = "RSA";
    private static final String publicKey = Const.publicKey;
    private static final String privateKey = Const.privateKey;

    public static byte[] encrypt(String text, PublicKey key){
        byte[] cipherText = null;
        try{
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,key);
            cipherText = cipher.doFinal(text.getBytes());
        }catch(Exception e){
            e.printStackTrace();
        }
        return cipherText;
    }
    public static String decrypt(byte[] text, PrivateKey privateKey) throws Exception{
        byte[] dectyptedText = null;
        try{
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,privateKey);
            dectyptedText = cipher.doFinal(text);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return new String(dectyptedText,"utf-8");
    }
    public static PrivateKey getPrivateKey(String key) throws NoSuchAlgorithmException,InvalidKeySpecException {
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(key));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(privateSpec);
    }

    public static PublicKey getPublicKey(String key) throws Exception{
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(key));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    public static String longToDate(Long lo){
        Date date = new Date(lo);
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sd.format(date);
    }



}
