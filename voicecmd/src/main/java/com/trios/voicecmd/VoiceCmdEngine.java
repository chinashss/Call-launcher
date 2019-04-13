package com.trios.voicecmd;

/**
 * Created by liuhongyu on 2016/12/1.
 */

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.Setting;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;
import com.iflytek.speech.util.FucUtil;
import com.iflytek.speech.util.JsonParser;
import com.realview.commonlibrary.audiorecord.XAudioRecord;
import com.realview.commonlibrary.audiorecord.XAudioRecordMgr;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class VoiceCmdEngine extends XAudioRecord {

    public static final int VoiceCmd_OK = 100;    //  确认
    public static final int VoiceCmd_CANCEL = 101;    //  取消
    public static final int VoiceCmd_NEXT = 102;    //  下一步
    public static final int VoiceCmd_PREV = 103;    //  上一步
    public static final int VoiceCmd_CALL = 104;    //  请求呼叫远程视频
    public static final int VoiceCmd_HOME = 105;    //  返回主界面（从任何一个界面）
    public static final int VoiceCmd_Stop = 109;    // 停止播放视频或者图片显示
    public static final int VoiceCmd_STARTWORK = 113;
    public static final int VoiceCmd_Setting = 114;
    public static final int VoiceCmd_HangUp = 115;
    public static final int VoiceCmd_CleanScren = 116;
    public static final int VoiceCmd_ShowScren = 117;
    public static final int VoiceCmd_PHOTOGRAPH = 118;
    public static final int VoiceCmd_SWITCHTASK = 119;
    public static final int VoiceCmd_ERROR = 9999;

    private boolean is_initialize = false;
    private boolean is_listening = false;
    private String keep_alive = "1";
    private String ivwNetMode = "0";

    public static final String TAG = "voCmdEng";
    private Context mContext = null;

    private int curThresh = 1450;
    private String mLocalGrammarID;

    // 语音识别对象
    private SpeechRecognizer mAsr;
    // 语音唤醒对象
    private VoiceWakeuper mIvw;

    // 本地语法文件
    private String mLocalGrammar = null;

    //
    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/test";
    // 返回结果格式，支持：xml,json
    private String mResultType = "json";

    private final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
    private final String GRAMMAR_TYPE_ABNF = "abnf";
    private final String GRAMMAR_TYPE_BNF = "bnf";

    private final String CMD_GRAMMAR_ID = "call";

    //private String mEngineType = SpeechConstant.TYPE_LOCAL;

    private static VoiceCmdEngine instance = new VoiceCmdEngine();

    public static VoiceCmdEngine getInstance() {
        return instance;
    }

    private XAudioRecordMgr mAudioMgr = null;

    private boolean voiceCMdListing;
    private Map<Integer, Vector<Handler>> mapHandlers = null;

    VoiceCmdEngine() {
        mapHandlers = new HashMap<Integer, Vector<Handler>>();
    }

    private byte[] mAudioData = null;

    public boolean InitEngine(Context cxt) {
        // 初始化识别对象
        if (is_initialize == false) {

            mContext = cxt;

            mAudioMgr = XAudioRecordMgr.newInstance(cxt);
            Setting.setShowLog(false);
            StringBuffer param = new StringBuffer();
            param.append("appid=" + mContext.getString(R.string.app_id));
            param.append(",");
            // 设置使用v5+
            param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
            SpeechUtility.createUtility(cxt, param.toString());

            is_initialize = true;

            if (mAudioData == null) {
                final int bytesPerFrame = 2;
                final int framesPerBuffer = 160;
                //byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
                int MiniBufferSize = bytesPerFrame * framesPerBuffer;

                mAudioData = new byte[MiniBufferSize];
            }
            if (mAudioMgr.isAudioRecorded() == false) {
                mAudioMgr.Subscribe(this);
                mAudioMgr.startRecording();
            } else {
                mAudioMgr.Subscribe(this);
            }

            Init();

            if (!buildLocalGrammar()) {
                Log.d(TAG, "构建语法失败。");
                return false;
            }

            return true;
        } else {
            Log.d(TAG, "引擎无法重复初始化。");
            return false;
        }
    }

    ;

    private void Init() {
        mIvw = VoiceWakeuper.createWakeuper(mContext, null);
        // 初始化识别对象---唤醒+识别,用来构建语法
        mAsr = SpeechRecognizer.createRecognizer(mContext, null);

        mLocalGrammar = FucUtil.readFile(mContext, "call.bnf", "utf-8");
        Log.d(TAG, mLocalGrammar);
    }

    public void UnInit() {
        if (mAudioMgr.isAudioRecorded() == true) {
            //mAudioMgr.stopRecording();
            mAudioMgr.UnSubscribe(this);
        }
    }

    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            Log.d(TAG, "onResult");
        }

        @Override
        public void onError(SpeechError error) {
            Log.d(TAG, "onError:" + error.toString());
            //NotifyAllHandler(VoiceCmd_ERROR,"没有识别到相应命令");
            //mIvw.startListening(mWakeuperListener);
        }

        @Override
        public void onBeginOfSpeech() {
            Log.d(TAG, "onBeginOfSpeech");
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            Log.d(TAG, "eventType:" + eventType + "arg1:" + isLast + "arg2:" + arg2);
            // 识别结果
            if (SpeechEvent.EVENT_IVW_RESULT == eventType) {
                RecognizerResult result = ((RecognizerResult) obj.get(SpeechEvent.KEY_EVENT_IVW_RESULT));
                //String recoString += JsonParser.parseGrammarResult(reslut.getResultString());
                if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                    Log.d(TAG, "recognizer result：" + result.getResultString());
                    int command_id = -1;
                    if (mResultType.equals("json")) {
                        command_id = JsonParser.parseCommandResult2(result.getResultString());
                        if (command_id > 0) {
                            AudioOrderMessage message = new AudioOrderMessage();
                            message.setType(command_id);
                            EventBus.getDefault().postSticky(message);
//                           NotifyAllHandler(command_id, JsonParser.parseIatResult(result.getResultString()));
                        }
                    }
                    // 显示
                } else {
                    Log.d(TAG, "recognizer result : null");
                    NotifyAllHandler(VoiceCmd_ERROR, "没有识别到数据");
                }
            }

            mIvw.startListening(mWakeuperListener);
        }

        @Override
        public void onVolumeChanged(int volume) {
            // TODO Auto-generated method stub

        }

    };

    public void UnInitEngine() {
        if (is_initialize) {
            mAsr.cancel();
            mAsr.destroy();
            is_initialize = false;
        }
    }

    ;

    public boolean startListenCommand(boolean voiceActive) {
        int ret = ErrorCode.SUCCESS;
        if (is_initialize) {
            mIvw = VoiceWakeuper.getWakeuper();
            if (mIvw != null) {
                final String resPath = ResourceUtil.generateResourcePath(mContext, RESOURCE_TYPE.assets, "ivw/" + mContext.getString(R.string.app_id) + ".jet");
                // 清空参数
                mIvw.setParameter(SpeechConstant.PARAMS, null);
                // 设置识别引擎
                mIvw.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);

                // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
                mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
                // 设置唤醒模式
                mIvw.setParameter(SpeechConstant.IVW_SST, "oneshot");//wakeup  oneshot
                //mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
                // 设置返回结果格式
                mIvw.setParameter(SpeechConstant.RESULT_TYPE, "json");
                // 设置持续进行唤醒
                mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
                // 设置闭环优化网络模式
                mIvw.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
                // 设置唤醒资源路径
                mIvw.setParameter(SpeechConstant.IVW_RES_PATH, resPath);
                // 设置唤醒录音保存路径，保存最近一分钟的音频
                mIvw.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
                mIvw.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
                // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
                //mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );

                // 设置自定义音频源
                mIvw.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");

                // ASR
                // 设置本地识别资源
                mIvw.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
                // 设置语法构建路径
                mIvw.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);

                // 设置本地识别使用语法id
                mIvw.setParameter(SpeechConstant.LOCAL_GRAMMAR, mLocalGrammarID);//wake

                // 启动唤醒
                ret = mIvw.startListening(mWakeuperListener);
                if (ret != ErrorCode.SUCCESS) {
                    Log.d(TAG, "识别失败,错误码: " + ret);
                    is_listening = false;
                } else {
                    is_listening = true;
                }
            } else {
                //showTip("唤醒未初始化");
                is_listening = false;
            }

            return is_listening;
        } else {
            Log.d(TAG, "引擎未初始化.");
            return false;
        }
    }

    ;

    String mContent;// 语法、词典临时变量

    private boolean buildLocalGrammar() {
        int ret = 0;
        mContent = new String(mLocalGrammar);

        mAsr.setParameter(SpeechConstant.PARAMS, null);
        // 设置文本编码格式
        mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        // 设置引擎类型
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        // 设置语法构建路径
        mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        //使用8k音频的时候请解开注释
//					mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
        // 设置资源路径
        mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());

        ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mContent, grammarListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.d(TAG, "语法构建失败,错误码：" + ret);

            return false;
        }

        return true;
    }

    public void stopListen() {
        if (is_listening) {
            mIvw.stopListening();
            Log.d(TAG, "停止识别");
            is_listening = false;
        } else {
            Log.d(TAG, "未处于监听状态");
        }
    }

    public void RemoveAllHandlers() {
        mapHandlers.clear();
    }

    ;

    public void RegisterHandler(Handler handler, int cmdID) {
        Vector<Handler> v = null;
        if (mapHandlers.containsKey(cmdID)) {
            v = mapHandlers.get(cmdID);
            if (v == null) {
                v = new Vector<Handler>();
                v.add(handler);
                mapHandlers.put(cmdID, v);
            } else {
                v.add(handler);
            }
        } else {
            v = new Vector<Handler>();
            v.add(handler);
            mapHandlers.put(cmdID, v);
        }
    }

    ;

    public void UnRegisterHandler(Handler handler, int cmdID) {
        Vector<Handler> v = null;
        if (mapHandlers.containsKey(cmdID)) {
            v = mapHandlers.get(cmdID);
            if (v != null) {
                v.remove(handler);
            }
        }
    }

    ;

    private void NotifyAllHandler(int cmdID, String str_) {
        Vector<Handler> v = null;
        if (mapHandlers.containsKey(cmdID)) {
            v = mapHandlers.get(cmdID);
            if (v != null) {
                for (int i = 0; i < v.size(); i++) {
                    Handler h = v.get(i);
                    if (h != null) {
                        Message msg = new Message();
                        msg.what = cmdID;
                        msg.obj = str_;
                        h.sendMessage(msg);
                    }
                }
            }
        }
    }

    /**
     * 构建语法监听器。
     */
    GrammarListener grammarListener = new GrammarListener() {

        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if (error == null) {
                mLocalGrammarID = grammarId;
                Log.d(TAG, "语法构建成功：" + grammarId);
                StartListener();
            } else {
                Log.d(TAG, "语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };

    //获取识别资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(mContext, RESOURCE_TYPE.assets, "asr/common.jet"));
        //识别8k资源-使用8k的时候请解开注释
//		tempBuffer.append(";");
//		tempBuffer.append(ResourceUtil.generateResourcePath(this, RESOURCE_TYPE.assets, "asr/common_8k.jet"));
        return tempBuffer.toString();
    }

    @Override
    public void onAudioData(byte[] audioData) {
        if (mIvw != null && is_listening && mAudioData != null) {
            mAudioData = audioData;
            mIvw.writeAudio(audioData, 0, audioData.length);
        }
        if (isSubscribe && mOnAudioDataListener != null) {
            mOnAudioDataListener.onAudioData(audioData);
        }
    }

    public void subscribe(OnAudioDataListener mOnAudioDataListener) {
        this.mOnAudioDataListener = mOnAudioDataListener;
        this.isSubscribe = true;
    }

    public void unUubscribe() {
        this.isSubscribe = false;
    }

    private boolean isSubscribe = false;


    private OnAudioDataListener mOnAudioDataListener;

    public interface OnAudioDataListener {
        void onAudioData(byte[] audioData);
    }


    public void RegisterVoiceCmd(Handler handler, int cmdId) {
        Vector<Handler> v = null;
        if (mapHandlers.containsKey(cmdId)) {
            v = mapHandlers.get(cmdId);
            if (v == null) {
                v = new Vector<Handler>();
                v.add(handler);
                mapHandlers.put(cmdId, v);
            } else {
                v.add(handler);
            }
        } else {
            v = new Vector<Handler>();
            v.add(handler);
            mapHandlers.put(cmdId, v);
        }
    }

    public void UnRegisterVoiceCmd(Handler handler, int cmdID) {
        Vector<Handler> v = null;
        if (mapHandlers.containsKey(cmdID)) {
            v = mapHandlers.get(cmdID);
            if (v != null) {
                v.remove(handler);
            }
        }
    }

    private void NotifyAllHandler(Message msg) {
        Vector<Handler> v = null;
        if (mapHandlers.containsKey(msg.what)) {
            v = mapHandlers.get(msg.what);
            if (v != null) {
                for (int i = 0; i < v.size(); i++) {
                    Handler h = v.get(i);
                    if (h != null) {
                        Message msg_ = Message.obtain(msg);
                        h.sendMessage(msg_);
                    }
                }
            }
        }
    }

    public void StartListener() {
        if (voiceCMdListing)
            return;
        voiceCMdListing = true;
        Message msg = new Message();
        msg.what = 0;
        NotifyAllHandler(msg);

        startListenCommand(false);
    }

    public void InitVoice(Context ctx) {
        InitEngine(ctx);
    }

}
