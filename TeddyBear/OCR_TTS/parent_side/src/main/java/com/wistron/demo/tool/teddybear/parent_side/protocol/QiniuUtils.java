package com.wistron.demo.tool.teddybear.parent_side.protocol;

import android.util.Log;

import com.qiniu.android.utils.UrlSafeBase64;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 * http://developer.qiniu.com/article/index.html#kodo-api-handbook
 */
public class QiniuUtils {
    private static final String ACCESS_KEY = "mIg8vv7_aVSeFB2XDNYZ4-wH6Q6e14471ZQRG3i0";
    private static final String SECRET_KEY = "zXDUIrsxBWxwUJmfNpAaqAd8qq_hceomTA3ZXOa6";
    private static final String BUCKET_NAME = "teddy-bear";
    private static final String DOMAIN_NAME = "http://oe5phuhpo.bkt.clouddn.com/";

    public static final String TAG = "QiniuUtils";

    /**
     * http://developer.qiniu.com/code/v6/api/kodo-api/rs/delete.html
     * <p>
     * < SN >/.......
     */
    public static boolean deleteFile(String fileName) {
        try {
            String entryUrl = BUCKET_NAME + ":" + fileName;
            String encodedEntryURI = UrlSafeBase64.encodeToString(entryUrl.getBytes());
            String host = "http://rs.qiniu.com";
            String path = "/delete/" + encodedEntryURI;

            byte[] sign = hMacSHA1Encrypt(path + "\n", SECRET_KEY);
            String encodedSign = UrlSafeBase64.encodeToString(sign);
            String authorization = ACCESS_KEY + ':' + encodedSign;

            String url = host + path;
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost mothod = new HttpPost(url);
            mothod.setHeader("Content-Type", "application/x-www-form-urlencoded");
            mothod.setHeader("Authorization", "QBox " + authorization);
            // 连接超时时间
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
            // 读取超时时间
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
            HttpResponse response = httpClient.execute(mothod);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            Log.i(TAG, "statusCode = " + statusCode);
            if (statusCode == HttpStatus.SC_OK) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * http://developer.qiniu.com/article/developer/security/download-token.html
     *
     * @param fileName
     * @return
     */
    public static boolean downloadFile(String fileName, String localFileName) {
        HttpURLConnection connection = null;
        InputStream mInputStream = null;
        OutputStream mOutputStream = null;
        try {
            String downloadUrl = DOMAIN_NAME + fileName;
            long deadline = System.currentTimeMillis() / 1000 + (1 * 60 * 60);
            String encodedSign = UrlSafeBase64.encodeToString(hMacSHA1Encrypt(downloadUrl, SECRET_KEY));
            String token = ACCESS_KEY + ':' + encodedSign;
            String getParams = "e=" + deadline +
                    "&token=" + token;

            URL url = new URL(downloadUrl + "?" + getParams);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "statusCode = " + responseCode);
            if (responseCode == 200) {
                Log.i(TAG, "downloadFile responseCode = " + responseCode);

                File localFile = new File(localFileName);
                if (!localFile.getParentFile().exists()) {
                    localFile.getParentFile().mkdirs();
                }
                if (!localFile.exists()) {
                    localFile.createNewFile();
                }

                mInputStream = connection.getInputStream();
                mOutputStream = new FileOutputStream(localFile);
                byte[] result = new byte[1024];
                do {
                    int readSize = mInputStream.read(result);
                    if (readSize == -1) {
                        break;
                    }
                    mOutputStream.write(result, 0, readSize);
                } while (true);

                return true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (mInputStream != null) {
                    mInputStream.close();
                    mInputStream = null;
                }
                if (mOutputStream != null) {
                    mOutputStream.close();
                    mOutputStream = null;
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * http://developer.qiniu.com/code/v6/api/kodo-api/rs/list.html
     *
     * @return
     */
    public static JSONObject getFiles(String folderName, boolean isGetSubFolder) {
        JSONObject resultObject = null;
        try {
            String host = "http://rsf.qbox.me";
            String path = "/list?" + "bucket=" + BUCKET_NAME + "&prefix=" + folderName;
            if (isGetSubFolder) {
                path += "&delimiter=%2F";
            }
            String url = host + path;
            byte[] sign = hMacSHA1Encrypt(path + "\n", SECRET_KEY);
            String encodedSign = UrlSafeBase64.encodeToString(sign);
            String authorization = ACCESS_KEY + ':' + encodedSign;
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost mothod = new HttpPost(url);
            mothod.setHeader("Content-Type", "application/x-www-form-urlencoded");
            mothod.setHeader("Authorization", "QBox " + authorization);
            // 连接超时时间
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
            // 读取超时时间
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
            HttpResponse response = httpClient.execute(mothod);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            Log.i(TAG, "statusCode = " + statusCode);
            if (statusCode == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity());
                Log.i(TAG, "getFiles result = " + result);
                resultObject = new JSONObject(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultObject;
    }

    /**
     * http://developer.qiniu.com/code/v6/api/kodo-api/rs/stat.html
     *
     * @return
     */
    public static boolean existFile(String fileName) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            String uri = BUCKET_NAME + ":" + fileName;
            String encodedEntryURI = UrlSafeBase64.encodeToString(uri.getBytes());
            String path = "/stat/" + encodedEntryURI;
            String encodedSign = UrlSafeBase64.encodeToString(hMacSHA1Encrypt(path + "\n", SECRET_KEY));
            String token = "QBox " + ACCESS_KEY + ':' + encodedSign;

            String downloadUrl = "http://rs.qiniu.com" + path;
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", token);
            connection.setDoOutput(true);

            int responseCode = connection.getResponseCode();
            Log.i("King", "existFile responseCode = " + responseCode);
            if (responseCode == 200) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * http://developer.qiniu.com/code/v6/api/kodo-api/rs/move.html
     *
     * @return
     */
    public static boolean rename(String srcFile, String destFile) {
        try {
            String srcUri = BUCKET_NAME + ":" + srcFile;
            String destUri = BUCKET_NAME + ":" + destFile;
            String encodedEntryURISrc = UrlSafeBase64.encodeToString(srcUri.getBytes());
            String encodedEntryURIDest = UrlSafeBase64.encodeToString(destUri.getBytes());

            String host = "http://rs.qiniu.com";
            String path = "/move/" + encodedEntryURISrc + "/" + encodedEntryURIDest;
            byte[] sign = hMacSHA1Encrypt(path + "\n", SECRET_KEY);
            String encodedSign = UrlSafeBase64.encodeToString(sign);
            String authorization = ACCESS_KEY + ':' + encodedSign;

            String url = host + path;
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost mothod = new HttpPost(url);
            mothod.setHeader("Content-Type", "application/x-www-form-urlencoded");
            mothod.setHeader("Authorization", "QBox " + authorization);
            // 连接超时时间
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
            // 读取超时时间
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
            HttpResponse response = httpClient.execute(mothod);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            Log.i(TAG, "statusCode = " + statusCode);
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                Log.i(TAG, EntityUtils.toString(entity));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取上传文件的token
     */
    public static String getUploadToken(String uploadKey) {
        //构造上传策略
        JSONObject json = new JSONObject();
        long deadline = System.currentTimeMillis() / 1000 + (1 * 60 * 60);  // second
        String uploadToken = null;
        try {
            // 有效时间为一个小时
            json.put("deadline", deadline);
            json.put("scope", BUCKET_NAME + ":" + uploadKey);
            json.put("insertOnly", 0);
            String encodedPutPolicy = UrlSafeBase64.encodeToString(json.toString().getBytes());
            byte[] sign = hMacSHA1Encrypt(encodedPutPolicy, SECRET_KEY);
            String encodedSign = UrlSafeBase64.encodeToString(sign);
            uploadToken = ACCESS_KEY + ':' + encodedSign + ':' + encodedPutPolicy;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uploadToken;
    }

    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";

    /**
     * 使用 HMAC-SHA1 签名方法对对encryptText进行签名
     *
     * @param encryptText 被签名的字符串
     * @param encryptKey  密钥
     * @return
     * @throws Exception
     */
    public static byte[] hMacSHA1Encrypt(String encryptText, String encryptKey)
            throws Exception {
        byte[] data = encryptKey.getBytes(ENCODING);
        // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec(data, MAC_NAME);
        // 生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance(MAC_NAME);
        // 用给定密钥初始化 Mac 对象
        mac.init(secretKey);
        byte[] text = encryptText.getBytes(ENCODING);
        // 完成 Mac 操作
        return mac.doFinal(text);
    }

}
