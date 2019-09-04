package com.holoview.hololauncher.http;

import com.hv.imlib.utils.TimeUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentIntercepter implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        int nonce = (int) (Math.random() * 100000);
        long timeStamp = TimeUtils.getCurrentTimeMillis() / 1000;
        Request request = chain.request().newBuilder()
                .addHeader("App-Key", "000001")
                .addHeader("Nonce", String.valueOf(nonce))
                .addHeader("Timestamp", timeStamp + "")
                .addHeader("Signature", shaEncrypt("1111111" + nonce + timeStamp))
                .build();

        return chain.proceed(request);
    }

    public static String shaEncrypt(String strSrc) {
        MessageDigest md = null;
        String strDes = "";
        byte[] bt = strSrc.getBytes();
        try {
            md = MessageDigest.getInstance("SHA-1");// 将此换成SHA-1、SHA-512、SHA-384等参数
            md.update(bt);
            strDes = bytes2Hex(md.digest()); // to HexString
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        return strDes;
    }

    /**
     * byte数组转换为16进制字符串
     *
     * @param bts 数据源
     * @return 16进制字符串
     */
    public static String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des += "0";
            }
            des += tmp;
        }
        return des;
    }

}
