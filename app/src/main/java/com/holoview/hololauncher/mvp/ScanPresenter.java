package com.holoview.hololauncher.mvp;


import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.holoview.hololauncher.HoloLauncherApp;
import com.holoview.hololauncher.basic.BasePresenterImpl;
import com.holoview.hololauncher.bean.Constants;
import com.holoview.hololauncher.http.HttpLoggerInterceptor;
import com.holoview.hololauncher.http.UserAgentIntercepter;
import com.hv.imlib.DB.sp.SystemConfigSp;
import com.hv.imlib.imservice.manager.IMNaviManager;
import com.hv.imlib.protocol.ProtoConstant;
import com.hv.imlib.protocol.http.NaviReq;
import com.hv.imlib.protocol.http.NaviRes;
import com.hv.imlib.utils.TimeUtils;
import com.realview.commonlibrary.server.http.HttpConstant;
import com.realview.commonlibrary.server.manager.CommLib;
import com.realview.commonlibrary.server.request.QROperRequest;
import com.realview.commonlibrary.server.response.QROperResponse;
import com.tencent.mmkv.MMKV;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Mr.kk on 2018/8/15.
 * This Project was duckchat-android
 */
public class ScanPresenter extends BasePresenterImpl<ScanContract.View> implements ScanContract.Presenter {
    public static final MediaType JSON_TYPE
            = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void naviForUrl(final String QRResult) {
        final String[] arryText = QRResult.split("\\;");
        NaviReq naviReq = new NaviReq();
        naviReq.setAppid(arryText[1].isEmpty() ? "000001" : arryText[1]);
        naviReq.setEnv(0);
        ProtoConstant.APP_ID = arryText[1].isEmpty() ? "000001" : arryText[1];
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new HttpLoggerInterceptor());//创建拦截对象
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);//这一句一定要记得写，否则没有数据输出

        String url = ProtoConstant.NAVI_SERVER_ADDRESS + ProtoConstant.HTTP_PROTO_VER + "/"
                + ProtoConstant.HTTP_PROTO_MODEL_NAV + "/" + ProtoConstant.HTTP_PROTO_METHOD_NAV;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new UserAgentIntercepter())
                .addNetworkInterceptor(logInterceptor)
                .build();
        RequestBody body = RequestBody.create(JSON_TYPE, JSON.toJSONString(naviReq));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                NaviRes naviRes = new Gson().fromJson(result, NaviRes.class);
//                IMNaviManager.instance().setNaviRes(naviRes);
                CommLib.instance().setNaviRes(naviRes);
                SystemConfigSp.instance().setStrConfig(SystemConfigSp.SysCfgDimension.NAVIINFO, result);
                if (mView != null) {
                    mView.onNaviForUrlSuccess(arryText[0]);
                }
            }
        });
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

    public static String shaEncrypt(String strSrc) {
        MessageDigest md = null;
        String strDes = null;
        byte[] bt = strSrc.getBytes();
        try {
            md = MessageDigest.getInstance("SHA-1");// 将此换成SHA-1、SHA-512、SHA-384等参数
            md.update(bt);
            strDes = bytes2Hex(md.digest()); // to HexString
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return strDes;
    }

    @Override
    public void login(String qrId) {
        QROperRequest qrOperRequest = new QROperRequest();
        qrOperRequest.setQrid(qrId);

        NaviRes naviRes = CommLib.instance().getNaviRes();

        String url = CommLib.instance().getAppSrvUrl() + HttpConstant.HTTP_PROTO_VER + "/"
                + HttpConstant.HTTP_PROTO_MODEL_UTIL + "/" + HttpConstant.HTTP_PROTO_METHOD_GETQROPER;


        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new HttpLoggerInterceptor());//创建拦截对象

        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);//这一句一定要记得写，否则没有数据输出


        String screctKey = "1111111";
        int nonce = (int) (Math.random() * 100000);
        long timeStamp = TimeUtils.getCurrentTimeMillis() / 1000;


        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new UserAgentIntercepter())
                .addNetworkInterceptor(logInterceptor)
                .build();
        RequestBody body = RequestBody.create(JSON_TYPE, JSON.toJSONString(qrOperRequest));
        Request request = new Request.Builder()
                .addHeader("App-Key", "000001")
                .addHeader("Nonce", nonce + "")
                .addHeader("Timestamp", timeStamp + "")
                .addHeader("Signature", shaEncrypt(screctKey + nonce + timeStamp))
                .url(url)
                .post(body)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                QROperResponse qrOperResponse = JSON.parseObject(response.body().string(), QROperResponse.class);
                QROperResponse.ResultBean resultBean = qrOperResponse.getResult();
                String[] arryText = resultBean.getOper().split("\\;");
                String token = arryText[0];
                String roomId = arryText[1];
                HoloLauncherApp.token = token;
                HoloLauncherApp.roomId = Long.parseLong(roomId);
                HoloLauncherApp.converstaiontype = Integer.parseInt(arryText[2]);
                HoloLauncherApp.call_list.clear();
                for (int i = 3; i < arryText.length; i++) {
                    HoloLauncherApp.call_list.add(Long.parseLong(arryText[i]));
                }
                if (mView != null) {
                    mView.loginSuccess();
                }
            }
        });
    }
}
