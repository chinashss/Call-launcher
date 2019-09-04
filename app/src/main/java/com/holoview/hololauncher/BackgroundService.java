package com.holoview.hololauncher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.holoview.aidl.AudioMessage;
import com.holoview.aidl.ProcessServiceIAidl;
import com.holoview.hololauncher.bean.ConnEvent;
import com.holoview.hololauncher.bean.ImEvent;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.ImLib;
import com.hv.imlib.model.Message;
import com.hv.imlib.model.message.ImageMessage;
import com.hv.imlib.model.message.custom.KickedNotificationMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import cn.holo.call.bean.message.ArMarkMessage;
import cn.holo.call.bean.message.CallAcceptMessage;
import cn.holo.call.bean.message.CallHangupMessage;
import cn.holo.call.bean.message.CallInviteMessage;
import cn.holo.call.bean.message.CallModifyMediaMessage;
import cn.holo.call.bean.message.CallModifyMemberMessage;
import cn.holo.call.bean.message.CallRingingMessage;
import cn.holo.call.bean.message.CallSTerminateMessage;
import cn.holo.call.bean.message.CallSummaryMessage;


/**
 * Created by admin on 2019/1/25.
 */

public class BackgroundService extends Service implements ImLib.OnReceiveMessageListener {
    private static final String APP_KEY = "qd46yzrfqu7gf";
    private static final String TAG = "BackgroundService";
    private static boolean isInit = false;
    private ProcessServiceIAidl mProcessAidl;

    private boolean isConnWifi = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("launcher", "launcher",
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(getApplicationContext(), "launcher").build();
            startForeground(9927, notification);
        }
        createServer();
    }


    private void createServer() {
        Log.i("lipengfei", "isInit" + isInit);
        if (isInit) {
            connetImServer();
            return;
        }

        if (!isConnWifi) {
            Log.i("lipengfei", "Wifi con't conn");
            return;
        }


        ImLib.instance().init(this, APP_KEY, new ImLib.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                Log.d(TAG, "SDK init success");
                isInit = true;
                ImLib.instance().setOnReceiveMessageListener(BackgroundService.this);
                connetImServer();
            }

            @Override
            public void onError(ImLib.ErrorCode errorCode) {
                Log.d(TAG, "SDK init onError");
            }
        });


    }

    private void connetImServer() {
        Log.i("lipengfei", "connetImServer");
        if (TextUtils.isEmpty(HoloLauncherApp.token)) {
            Log.i("lipengfei", "token is null");
            return;
        }
        ImLib.instance().connect(HoloLauncherApp.token, new ImLib.ConnectCallback() {
            @Override
            public void onLocalSuccess(long userid) {
                Log.i("lipengfei", "onLocalSuccess");
            }

            @Override
            public void onSuccess(long userid) {
                Log.i("lipengfei", "onSuccess");
                ImEvent imEvent = new ImEvent();
                Bundle bundle = new Bundle();
                bundle.putLong("userId", userid);
                HoloLauncherApp.userSelfId = userid;
                imEvent.setAction(3051);
                imEvent.setData(bundle);
                EventBus.getDefault().post(imEvent);
            }

            @Override
            public void onFailure(ImLib.ErrorCode err) {
                Log.i("lipengfei", "onFailure");
            }

            @Override
            public void onTokenIncorrect() {
                Log.i("lipengfei", "onTokenIncorrect");
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImEvent(ConnEvent connEvent) {
        if (connEvent.getAction() == 1) {
            isConnWifi = true;
            createServer();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createServer();
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (mProcessAidl != null) {
            unbindService(conn);
        }
        ImLib.instance().disconnect();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public boolean onReceive(Message message) {
        if (message.getMessageContent() instanceof KickedNotificationMessage) {
            ImEvent imEvent = new ImEvent();
            Bundle bundle = new Bundle();
            imEvent.setAction(3052);
            imEvent.setData(bundle);
            EventBus.getDefault().post(imEvent);
            return false;
        }

        String action = message.getMessageContent().getClass().getSimpleName();
        HoloMessage holoMessage = new HoloMessage();
        holoMessage.setMessage(message);
        holoMessage.setAction(action);
        try {
            if (mProcessAidl != null) {
                String json = JSON.toJSONString(holoMessage);
                Log.i("lipengfei", json);
                mProcessAidl.sendMessage(json);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 接收到其他app来的消息
     */
    Binder binder = new ProcessServiceIAidl.Stub() {


        @Override
        public void sendMessage(String json) throws RemoteException {
            HoloMessage holoMessage = JSON.parseObject(json, HoloMessage.class);

            String action = holoMessage.getAction();
            Message message = holoMessage.getMessage();
            try {
                JSONObject object = new JSONObject(json);
                if (action.equals("CallAcceptMessage")) {
                    JSONObject content = object.getJSONObject("message").getJSONObject("messageContent");
                    CallAcceptMessage callAcceptMessage = JSON.parseObject(content.toString(), CallAcceptMessage.class);
                    message.setMessageContent(callAcceptMessage);
                } else if (action.equals("CallHangupMessage")) {
                    JSONObject content = object.getJSONObject("message").getJSONObject("messageContent");
                    CallHangupMessage callHangupMessage = JSON.parseObject(content.toString(), CallHangupMessage.class);
                    message.setMessageContent(callHangupMessage);
                } else if (action.equals("CallInviteMessage")) {
                    JSONObject content = object.getJSONObject("message").getJSONObject("messageContent");
                    CallInviteMessage callInviteMessage = JSON.parseObject(content.toString(), CallInviteMessage.class);
                    message.setMessageContent(callInviteMessage);
                } else if (action.equals("CallModifyMemberMessage")) {
                    JSONObject content = object.getJSONObject("message").getJSONObject("messageContent");
                    CallModifyMemberMessage callModifyMemberMessage = JSON.parseObject(content.toString(), CallModifyMemberMessage.class);
                    message.setMessageContent(callModifyMemberMessage);
                } else if (action.equals("ImageMessage")) {
                    JSONObject content = object.getJSONObject("message").getJSONObject("messageContent");
                    ImageMessage imageMessage = JSON.parseObject(content.toString(), ImageMessage.class);
                    message.setMessageContent(imageMessage);
                }
                ImLib.instance().sendMessage(message, new ImLib.SendImageMessageCallback() {
                    @Override
                    public void onProgress(Message message, double v) {
                        message.getId();
                    }

                    @Override
                    public void onAttached(Message message) {
                        message.getId();
                    }

                    @Override
                    public void onSuccess(Message message) {
                        String mediaId = getMediaIdBySentTime(message.getUpdated());
                    }

                    @Override
                    public void onError(Message message, ImLib.ErrorCode errorCode) {
                        message.getId();
                    }
                });

            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        /**
         * 使用callLib必须初始化这些消息类型
         * @throws RemoteException
         */
        @Override
        public void initCallMessage() throws RemoteException {
            ImLib.instance().getmHandler().registerMessageType(CallInviteMessage.class);
            ImLib.instance().getmHandler().registerMessageType(CallRingingMessage.class);
            ImLib.instance().getmHandler().registerMessageType(CallAcceptMessage.class);
            ImLib.instance().getmHandler().registerMessageType(CallHangupMessage.class);
            ImLib.instance().getmHandler().registerMessageType(CallSummaryMessage.class);

            ImLib.instance().getmHandler().registerMessageType(CallModifyMediaMessage.class);
            ImLib.instance().getmHandler().registerMessageType(CallModifyMemberMessage.class);
            ImLib.instance().getmHandler().registerMessageType(CallSTerminateMessage.class);
            ImLib.instance().getmHandler().registerMessageType(ArMarkMessage.class);
        }

        /**
         * 绑定其他app的Service
         * @param packageName
         * @param serviceName
         * @throws RemoteException
         */
        @Override
        public void onBindSuccess(String packageName, String serviceName) throws RemoteException {
            bindOrderService(packageName, serviceName);
        }

        @Override
        public void onAudioData(AudioMessage audio) throws RemoteException {

        }

    };

    private String getMediaIdBySentTime(long sentTime) {
        return (sentTime & 2147483647L) + "";
    }


    private void bindOrderService(String packageName, String serviceName) {
        Intent intent = new Intent(serviceName);
        intent.setPackage(packageName);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mProcessAidl != null) {
            unbindService(conn);
        }

        return super.onUnbind(intent);
    }

    ServiceConnection conn = new ServiceConnection() {//这个最重要，用于连接Service
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("Launcher", "Launcher conn to app success");
            ProcessServiceIAidl aidl = ProcessServiceIAidl.Stub.asInterface(service);
            mProcessAidl = aidl;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("Launcher", "Launcher disConn to app success");
            mProcessAidl = null;
        }
    };
}
