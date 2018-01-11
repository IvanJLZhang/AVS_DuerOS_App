package com.wistron.demo.tool.teddybear.avs;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import static com.wistron.demo.tool.teddybear.avs.Common.CODE_CHALLENGE;
import static com.wistron.demo.tool.teddybear.avs.Common.CODE_VERIFIER;
import static com.wistron.demo.tool.teddybear.avs.Common.PRODUCT_DSN;
import static com.wistron.demo.tool.teddybear.avs.Common.PRODUCT_MATEDATA;

/**
 * Time：17-5-15 09:43
 * Author：bob
 */
public class InitializeProductMetadata {

    private Context context;

    private String codechallenge;
    private String productDsn;
    private String productId = "teddybear_avs";

    private static InitializeProductMetadata instance = null;

    public static InitializeProductMetadata getInstance(Context context) {
        if (instance == null) {
            instance = new InitializeProductMetadata(context);
        }
        return instance;
    }

    private InitializeProductMetadata(Context context) {
        this.context = context;
        initial();
    }

    private void initial() {
        saveCodeChallenge();
        saveProductDSN();
        saveProductId();
    }

    public String getCodechallenge() {
        return codechallenge;
    }

    public String getDsn() {
        return productDsn;
    }

    public String getProductId() {
        return productId;
    }

    private void saveProductId() {
        SharedPreferences preferences = context.getSharedPreferences(PRODUCT_MATEDATA, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Common.PRODUCT_ID, productId);
        editor.apply();
    }

    private String saveProductDSN() {
        SharedPreferences preferences = context.getSharedPreferences(PRODUCT_MATEDATA, Context.MODE_PRIVATE);

        if (preferences.contains(PRODUCT_DSN)) {
            return preferences.getString(PRODUCT_DSN, "");
        }

        String dsn = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PRODUCT_DSN, dsn);
        editor.apply();

        return dsn;
    }


    private void saveCodeChallenge() {
        codechallenge = getCodeChallenge();

        SharedPreferences preferences = context.getSharedPreferences(PRODUCT_MATEDATA, Context.MODE_PRIVATE);

        if (!preferences.contains(CODE_CHALLENGE)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(CODE_CHALLENGE, codechallenge);
            editor.apply();
        }
    }

    /**
     * Create a String hash based on the code verifier, this is used to verify the Token exchanges
     *
     * @return
     */
    private String getCodeChallenge() {
        String verifier = getCodeVerifier();
        return base64UrlEncode(getHash(verifier));
    }

    /**
     * Return our stored code verifier, which needs to be consistent, if this doesn't exist, we create a
     * new one and store the new result
     *
     * @return the String code verifier
     */
    private String getCodeVerifier() {

        SharedPreferences preferences = context.getSharedPreferences(PRODUCT_MATEDATA, Context.MODE_PRIVATE);

        if (preferences.contains(CODE_VERIFIER)) {
            return preferences.getString(CODE_VERIFIER, "");
        }

        //no verifier found, make and store the new one
        String verifier = createCodeVerifier();

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CODE_VERIFIER, verifier);
        editor.apply();

        return verifier;
    }

    public static String createCodeVerifier() {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 128; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String verifier = sb.toString();
        return verifier;
    }

    /**
     * Hash a string based on the SHA-256 message digest
     *
     * @param password
     * @return
     */
    private static byte[] getHash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        digest.reset();
        byte[] response = digest.digest(password.getBytes());

        return response;
    }

    /**
     * Encode a byte array into a string, while trimming off the last characters, as required by the Amazon
     * token server
     * <p>
     * See: http://brockallen.com/2014/10/17/base64url-encoding/
     *
     * @param arg our hashed string
     * @return a new Base64 encoded string based on the hashed string
     */
    private static String base64UrlEncode(byte[] arg) {
        String s = Base64.encodeToString(arg, 0); // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

}
