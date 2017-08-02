package com.example.myapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;

import javax.microedition.khronos.opengles.GL10;
import java.util.*;

/**
 * Created by yxh on 16-6-23.
 * //假设在用户定位的过程中，网络是稳定的，只要用户还在使用，就会有网络数据产生
 * //而传感器数据时有时无，一般情况下，当传感器检测到用户一直在进行的运动状态改变时会发送标志，
 * //此时，获得用户改变后的状态，并且依靠网络数据对用户进行定位
 * //也就是说 整个过程中，只要进行联网，一定会有传感器数据，网络数据是传感器数据的基础，如果网络数没有，
 * //则函数不再判断
 */
public class LgpsServer implements BaiduMap.OnMapDrawFrameCallback {
    private byte status[] = new byte[4];    //传感器状态
    private long formSecondFromSeg = -1;
    private byte oldSegdataByEverytime[] = {0, 0, 0, 0, 0, 0, 0, 0};  //安防时的初变量值
    private boolean ISMOVING = true;                //用户是否还处在运动中，根据传感器状态值判断
    private RowRecord mRR;                          //用于写入本地数据库的当前数据值
    private boolean QUIT = false;                   //LGPS服务是否退出运行
    private Handler myhandler;          //用于和前台交互的消息句柄
    public SegInfo HistorysegInfo, NowSegMent;         //历史数据值中的段。现在的段
    private String MyLocCODE = String.valueOf(11);          //导航的起点
    private String DestCODE = "77";                       //导航的终点


    private boolean FRAMED = false, ACQUIRED_NET_DATA = false;
    private LatLng mylatlng;   //本人当前位置
    private DBHelper db;  //本地数据库

    private TextToSpeech mTextToSpeech;  //语音导航引擎
    private boolean IsMyPostionConfirmed = false;
    private byte mNetData[] = new byte[36];
    private int mPeopleStatus[] = new int[3];
    private Context mContext;
    private BaiduMap mBaiduMap;
    private Coordinate mSelfCoord;
    private double S_rect_J = 0;
    private double S_rect_W = 0;
    private double S_Jgap = 0;
    private double S_Wgap = 0;
    private LatLng zero_point = new LatLng(0, 0);
    private byte[] mSegdataByEverytime = {0, 0, 0, 0, 0, 0, 0, 0};
    private int CURRENT_MODE;
    private SoundPool sp;
    private HashMap<Integer, Integer> spMap;
    private String SentryPoint;
    private byte[] SecureSegment;
    private int NowDirection;

    LgpsServer(Handler apphandler, Context mcontext, BaiduMap baiduMap) {
        mBaiduMap = baiduMap;
        mSelfCoord = new Coordinate();
        mSelfCoord.type = LGPS_COLOR.Self;
        mSelfCoord.x = 100;
        mSelfCoord.y = 100;
        mSelfCoord.name = "YOU";
        IsMyPostionConfirmed = false;
        mContext = mcontext;
        text2Speech();
        sp = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        spMap = new HashMap<>();
        spMap.put(1, sp.load(mContext, R.raw.notice, 1));
        db = new DBHelper(mcontext);
        mRR = new RowRecord();
        myhandler = apphandler;
        db.open();
        NowSegMent = db.getSegInfo("E88EB88EH4");
        // db.updateKey(5,"我");
        db.close();
        initSegment(NowSegMent);
        NetD2SegD();
    }

    public void setlocalLATLNG(LatLng latLng) {
        mylatlng = latLng;
    }

    private void initSegment(SegInfo segInfo) {
        if (segInfo.getLatLT() != 0 || segInfo.getLngLT() != 0)
            zero_point = new LatLng(segInfo.getLatLT(), segInfo.getLngLT());
        double j = 5e-5 / 500;
        S_rect_J = j * (segInfo.getS_lengthZ());
        double w = 4e-5 / 500;
        S_rect_W = w * (segInfo.getS_lengthH());
        S_Jgap = S_rect_J / 50;
        S_Wgap = S_rect_W / 50;
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(zero_point).zoom(21.0f);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    public void setDestPoint() {
        db.open();
        DestCODE = db.readdest();
        db.close();
    }

    public void setSentryPoint( ) {
        db.open();
        SentryPoint = db.readsentry();
        db.close();
    }

    public void setSecureSegment(byte[] secureSegment) {
        SecureSegment = secureSegment;
    }

    public void Toquit() {

        setWorkingMode(8888);
        if (mBaiduMap != null) mBaiduMap.clear();
        mTextToSpeech.shutdown();
    }

    public void setmPeopleStatus(int[] mPeopleStatus) {
        this.NowDirection = mPeopleStatus[1];
    }

    public void playSounds() {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float volumnRatio = audioCurrentVolumn / audioMaxVolumn;
        sp.play(spMap.get(1), volumnRatio, volumnRatio, 1, 1, 1);
    }

    public void updateNetData(byte[] kk) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String date = year + "-" + month + "-" + day;
        mRR.setmRowDate(date);
        System.arraycopy(kk, 0, mNetData, 0, 36);
        System.arraycopy(kk, 36, status, 0, 4);
        ACQUIRED_NET_DATA = true;
    }

    SegInfo getSegInfo(LatLng latLng) {
        db.open();
        SegInfo segInfo = db.getSegInfo(latLng);
        db.close();
        return segInfo;
    }

    public boolean setWlakingstatus(long timebow) {
        long now = 0;
        if ((timebow - now) > 1000) {
            return false;
        }
        now = timebow;
        return true;
    }


    public void setWorkingMode(int mode) {
        CURRENT_MODE = mode;

    }

    private Coordinate SwitchMode(byte[] data, byte[] olddata) {
        Coordinate theVeryPoint = new Coordinate();
        switch (CURRENT_MODE) {
            case LgpsApp.LgpsMode.LOCATING_MODE:
                theVeryPoint = LocatingMode(NowDirection, data);
                break;
            case LgpsApp.LgpsMode.NAVAGATING_MODE:
                NavigateMode(LocatingMode(NowDirection, data).OutPut(), DestCODE);
                break;
            case LgpsApp.LgpsMode.SENTRY_MODE:
                setSentryPoint();
                theVeryPoint= SentryMode(data, SentryPoint);
                Log.i("模式选择", "哨兵模式");
                break;
            case LgpsApp.LgpsMode.SECURE_MODE:

                Coordinate cc = SecureMode(data, SecureSegment);
                if (cc != null) {
                    theVeryPoint = cc;
                    playSounds();
                    mTextToSpeech.speak("Warning! Security alert!", TextToSpeech.QUEUE_FLUSH, null);
                  /*  Toast.makeText(mContext, "警报！警报！有人入侵，请注意", Toast.LENGTH_LONG).show();*/
                }
                break;
            case LgpsApp.LgpsMode.INPUT_MODE:
                Coordinate c = InputMode(mylatlng, data);
                if (c != null) {
                    theVeryPoint = c;
                }
                break;
            default:
                break;
        }
        return theVeryPoint;
    }

    private Coordinate LocatingMode(int direction, byte[] mdata) {
        Coordinate coord = new Coordinate();
        coord.type = LGPS_COLOR.Self;
        if (IsMyPostionConfirmed) {
            coord.setXY(TrackingStep(direction, mdata));
            return coord;
        }
        coord.setXY(MappingSelf(direction, mdata));
        return coord;
    }

    private Coordinate InputMode(LatLng lll, byte[] data) {
        Coordinate coordinate = new Coordinate();
        coordinate.type = LGPS_COLOR.OtherStatic;
        String sdb="Hi:";
        for (int i = 0; i <= 7; i++) {
            byte m = data[i];
            for (int j = 0; j <= 7; j++) {
                int c = m>>j & 1;
                if (c == 1) {
                    coordinate.x = i+1;
                    coordinate.y = 8-j;
                    db.open();
                    sdb+=db.readKey((i + 1) * 8 + (1 + j));
                    db.close();

                    Message  msg=Message.obtain();
                    msg.what=LGPS_CONST.SERVER_MSG;
                    msg.arg1=1;
                    msg.obj=sdb;
                    myhandler.sendMessage(msg);
                    return coordinate;
                }
                m = (byte) (m >> 1);
            }
        }
        return null;
    }

    private void NavigateMode(String nowPosition, String destination) {
        setDestPoint();
        String tobeSpeech = null;
        int[] source = new int[2], dest = new int[2];
        mTextToSpeech.speak("Navigation Mode Initialized!There is no segment available now, Please try later", TextToSpeech.QUEUE_FLUSH, null);
        source[0] = nowPosition.charAt(0) - 48;
        source[1] = nowPosition.charAt(1) - 48;
        dest[0] = destination.charAt(0) - 48;
        dest[1] = destination.charAt(1) - 48;
        int HH = dest[0] - source[0];
        int LL = dest[1] - source[1];
        if (HH > 0) {
            if (LL > 0) {                       //目标在自身左后方
                if ((mSegdataByEverytime[source[0] + 1] & (byte) Math.pow(2, 7 - source[1])) == 0) {
                    tobeSpeech = "obstacle behind,left first recommended ";
                } else if ((mSegdataByEverytime[source[0] + 1] & (byte) Math.pow(2, 7 - source[1] + 1)) == 0) {
                    tobeSpeech = "obstacle left behind,behind or left first recommended";
                } else if ((mSegdataByEverytime[source[0]] & (byte) Math.pow(2, 7 - source[1] + 1)) == 0) {
                    tobeSpeech = "obstacle left,behind fisrt recommended ";
                } else {
                    tobeSpeech = "left behind recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            } else if (LL < 0) {          //目标在自身右后方
                if ((mSegdataByEverytime[source[0] + 1] & (byte) Math.pow(2, 7 - source[1])) == 0) {
                    tobeSpeech = "obstacle  behind,right first recommended ";
                } else if ((mSegdataByEverytime[source[0] + 1] & (byte) Math.pow(2, 7 - source[1] - 1)) == 0) {
                    tobeSpeech = "obstacle right behind,behind or right first recommended";
                } else if ((mSegdataByEverytime[source[0]] & (byte) Math.pow(2, 7 - source[1] - 1)) == 0) {
                    tobeSpeech = "obstacle right,behind fisrt recommended ";
                } else {
                    tobeSpeech = "right behind recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            } else {                                         //目标在自身正后方
                if ((mSegdataByEverytime[source[0] + 1] & (byte) Math.pow(2, 7 - source[1])) == 0) {
                    tobeSpeech = "obstacle right behind,left or right recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            }

        } else if (HH < 0) {
            if (LL > 0) {                  //目标在自身左前方
                if ((mSegdataByEverytime[source[0] - 1] & (byte) Math.pow(2, 7 - source[1])) == 0) {
                    tobeSpeech = "obstacle front,left first recommended ";
                } else if ((mSegdataByEverytime[source[0] - 1] & (byte) Math.pow(2, 7 - source[1] + 1)) == 0) {
                    tobeSpeech = "obstacle left front,front or left first recommended";
                } else if ((mSegdataByEverytime[source[0]] & (byte) Math.pow(2, 7 - source[1] + 1)) == 0) {
                    tobeSpeech = "obstacle left,front fisrt recommended ";
                } else {
                    tobeSpeech = "left front recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            } else if (LL < 0) {            //目标在自身右前方
                if ((mSegdataByEverytime[source[0] - 1] & (byte) Math.pow(2, 7 - source[1])) == 0) {
                    tobeSpeech = "obstacle front,right first recommended ";
                } else if ((mSegdataByEverytime[source[0] - 1] & (byte) Math.pow(2, 7 - source[1] - 1)) == 0) {
                    tobeSpeech = "obstacle right front,front or right first recommended";
                } else if ((mSegdataByEverytime[source[0]] & (byte) Math.pow(2, 7 - source[1] - 1)) == 0) {
                    tobeSpeech = "obstacle right,front fisrt recommended ";
                } else {
                    tobeSpeech = "right front recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            } else {                                             //目标在自身正前方
                if ((mSegdataByEverytime[source[0] - 1] & (byte) Math.pow(2, 7 - source[1])) == 0) {
                    tobeSpeech = "obstacle front,left or right recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            }

        } else if (HH == 0) {                        //目标在自身同行
            if (LL > 0) {                          //目标在自身同行左边
                if ((mSegdataByEverytime[source[0]] & (byte) Math.pow(2, 7 - source[1] + 1)) == 0) {
                    tobeSpeech = "obstacle left,others recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);

            } else if (LL < 0) {                   //目标在自身同行右边
                if ((mSegdataByEverytime[source[0]] & (byte) Math.pow(2, 7 - source[1] - 1)) == 0) {
                    tobeSpeech = "obstacle right,others recommended ";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            } else {
                if ((mSegdataByEverytime[source[0]] & (byte) Math.pow(2, 7 - source[1] - 1)) == 0) {
                    tobeSpeech = "congratulations,you have reached the spot";
                }
                mTextToSpeech.speak(tobeSpeech, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }


    private Coordinate SentryMode(byte[] mm, String sucrepoint) {

        Log.i("哨兵模式：",sucrepoint);
        int c = Integer.valueOf(sucrepoint);
        int LH = c / 10;
        int LL = c % 10;
        Log.i("哨兵模式：", String.valueOf(LH)+LL);
        Coordinate noi = new Coordinate();
        noi.x=LH;
        noi.y=LL;
        if (LH <= 8 && LL <= 8) {
            int LIE = 8 - LL;

            for (int i = LH-2; i <= LH; i++) {
                byte CM=mm[i];
                for (int j=LIE-1;j<=LIE+1;j++){
                      int J=  CM >>j & 1;
                    if (J==1){
                        noi.x=1+i;
                        noi.y=8-j;
                        noi.type = LGPS_COLOR.Warning;
                        playSounds();
                        mTextToSpeech.speak("Warning! Approaching alert!", TextToSpeech.QUEUE_FLUSH, null);
                        return noi;
                    }

                }
            }

        }
        return noi;
    }

    private Coordinate SecureMode(byte[] mmm, byte[] thebeginning) {
        byte mi;
        Coordinate nop = new Coordinate();
        for (int i = 0; i <= 7; i++) {
            if ((mi = (byte) Math.abs(mmm[i] - thebeginning[i])) != 0) {
                int C= (int) (Math.log(mi)/Math.log(2));
                nop.x = i+1;
                nop.y = 8-(C);
                nop.type = LGPS_COLOR.Warning;
                nop.name = "入侵者";

                return nop;
            }
        }
        return null;
    }

    void showHistory(List<RowRecord> rr) {
        new Thread(new Runnable() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                String mlastSeg = "0";
                db.open();
                //     List<Coordinate> historirs = db.getAllElement("E88EB88EH4");
                db.close();
                for (int i = 0; i <= rr.size() - 1; i++) {
                    for (int n = 0; n <= rr.size() - 1; n++) {
                        Coordinate my = new Coordinate();
                        my= rr.get(i).getmMyPos();
                        my.type = LGPS_COLOR.Self;
                        //historirs.add(my);
                    }
                    String segnum = rr.get(i).getmSegNum();
                    if (Objects.equals(segnum, mlastSeg)) {
                        mBaiduMap.clear();
                    }
                    mlastSeg = segnum;
                    db.open();
                    HistorysegInfo = db.getSegInfo(segnum);
                    db.close();
                    if (HistorysegInfo != null) {
                        initSegment(HistorysegInfo);
                         addgrid(rr.get(i).getDRR(),rr.get(i).getmMyPos());
                        Message msg = Message.obtain();
                        msg.what = LGPS_CONST.HISTORY_CONCE_DISPLAY;
                        myhandler.sendMessage(msg);
                        try {
                            Thread.sleep(180);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Message msg = Message.obtain();
                        msg.what = LGPS_CONST.NO_THIS_SEGMENT;
                        myhandler.sendMessage(msg);
                    }
                }
            }
        }).start();
    }

    private boolean NetD2SegD() {
        new Thread() {
            @Override
            public void run() {
                while (!QUIT) {
                    try {
                        if (ACQUIRED_NET_DATA) {
                            if (mNetData[33] * 60 + mNetData[34] * 3600 + mNetData[32] > formSecondFromSeg) {
                                for (int i = 0; i <= 3; i++) {
                                    System.arraycopy(mNetData, 8 * i, mSegdataByEverytime, 0, 8);
                                    if (!Arrays.equals(mSegdataByEverytime, oldSegdataByEverytime)) {
                                        setSecureSegment(oldSegdataByEverytime);
                                        Coordinate coordinate = SwitchMode(mSegdataByEverytime, oldSegdataByEverytime);
                                        mBaiduMap.clear();
                                        addgrid(mSegdataByEverytime, coordinate);
                                        mRR.setmSegNum("E88EB88EH4");
                                        mRR.setmRowTime(mNetData[34] + ":" + mNetData[33] + ":" + mNetData[32]);
                                        mRR.setmMyPos((byte) (mSelfCoord.x * 8 + mSelfCoord.y));
                                        mRR.setDRR(mSegdataByEverytime);
                                        Log.i("数据库记录：", mRR.getmRowDate() + mRR.getmRowTime() + "开始记录******");
                                        insertToDB(mRR);
                                        System.arraycopy(mSegdataByEverytime, 0, oldSegdataByEverytime, 0, 8);
                                        Thread.sleep(180);

                                    }
                                }
                                formSecondFromSeg = mNetData[33] * 60 + mNetData[34] * 3600 + mNetData[32];
                            }
                            ACQUIRED_NET_DATA = false;
                        } else {
                            LatLng llText;
                            if (mylatlng == null) {
                                llText = new LatLng(zero_point.latitude + S_rect_W, zero_point.longitude + S_rect_J);
                            } else {
                                llText = mylatlng;
                            }
                            OverlayOptions ooText = new TextOptions().bgColor(0xAAFFFFFF)
                                    .fontSize(25).fontColor(0xFFFF00FF).text("等待段的连接中。。。").rotate(0)
                                    .position(llText);
                            mBaiduMap.addOverlay(ooText);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    FRAMED = false;
                }
            }
        }.start();
        return true;
    }

    private void insertToDB(RowRecord record) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.open();
                try {
                    db.insertRow(record);
                } catch (UnknownError error) {
                    error.printStackTrace();
                }
                db.close();
            }
        }).start();
    }


    private void addgrid(byte[] data, Coordinate coord) {

        for (int i = 0; i <= 7; i++) {
            byte C = data[i];
            for (int j = 0; j <= 7; j++) {
                LatLng zuoxia = new LatLng(zero_point.latitude - i * (S_Wgap + S_rect_W), zero_point.longitude - j * (S_rect_J + S_Jgap));
                byte f = (byte) (C & 1);
                C = (byte) (C >> 1);
                if (f == 1) {
                    drawRect(zuoxia, LGPS_COLOR.UnKown);
                } else if (f == 0) {
                    drawRect(zuoxia, LGPS_COLOR.Clear);
                }
                if (coord.x == i+1 && coord.y ==8-j) {
                    drawRect(zuoxia, coord.type);
                }
            }
        }
    }


    private void drawRect(LatLng zuoxia, int acolor) {
        LatLng point1 = new LatLng(zuoxia.latitude, zuoxia.longitude + S_rect_J);
        LatLng point2 = new LatLng(zuoxia.latitude + S_rect_W, zuoxia.longitude + S_rect_J);
        LatLng point3 = new LatLng(zuoxia.latitude + S_rect_W, zuoxia.longitude);
        List<LatLng> rect = new ArrayList<>(4);
        rect.add(zuoxia);
        rect.add(point1);
        rect.add(point2);
        rect.add(point3);
        OverlayOptions ooPolygon = new PolygonOptions().points(rect).stroke(new Stroke(5, 0x0A007F00)).fillColor(acolor);
        mBaiduMap.addOverlay(ooPolygon);
    }

    private void drawRect(LatLng zuoxia, int acolor, String name) {
        LatLng point1 = new LatLng(zuoxia.latitude, zuoxia.longitude + S_rect_J);
        LatLng point2 = new LatLng(zuoxia.latitude + S_rect_W, zuoxia.longitude + S_rect_J);
        LatLng point3 = new LatLng(zuoxia.latitude + S_rect_W, zuoxia.longitude);
        List<LatLng> rect = new ArrayList<>(4);
        rect.add(zuoxia);
        rect.add(point1);
        rect.add(point2);
        rect.add(point3);
        OverlayOptions ooPolygon = new PolygonOptions().points(rect).stroke(new Stroke(5, 0x0A007F00)).fillColor(acolor);
        mBaiduMap.addOverlay(ooPolygon);

        LatLng llText = new LatLng(zuoxia.latitude + 0.5 * S_rect_W, zuoxia.longitude + 0.5 * S_rect_J);
        OverlayOptions ooText = new TextOptions().bgColor(0xAAFFFFFF)
                .fontSize(20).fontColor(0xFFFF00FF).text(name).rotate(0)
                .position(llText);
        mBaiduMap.addOverlay(ooText);
    }


    private int[] MappingSelf(int direction, byte[] netData) {


        byte[] dara1 = {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] result = {0, 0, 0, 0, 0, 0, 0, 0};
        int h = 100, l = 100;
        int[] looop = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 1; i <= 7; i++) {
            result[i - 1] = (byte) (netData[i] - dara1[i]);
            if (result[i - 1] != 0) {
                looop[i - 1] = i;
            }
        }
        switch (direction) {
            case 7:
            case -7:
                for (int i = 0; i <= 7; i++) {
                    if (looop[i] != 0 && looop[i + 1] != 0 && result[looop[i]] > 0) {
                        if (result[looop[i]] == -result[looop[i + 1]]) {
                            h = looop[i];
                            l = 8 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                        }
                    }
                }
                break;
            case -5:
            case -6:
                for (int i = 0; i <= 7; i++)
                    if (looop[i] != 0 && looop[i + 1] != 0 && result[looop[i]] > 0) {
                        if (result[looop[i]] == -0.5 * result[looop[i + 1]]) {
                            h = looop[i];
                            l = 8 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                        }
                    }
                break;
            case -3:
            case -4:
                for (int i = 0; i <= 7; i++) {
                    if (looop[i] != 0 && result[looop[i]] < 0) {
                        h = looop[i];
                        l = 8 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                    }
                }
                break;
            case -1:
            case -2:
                for (int i = 0; i <= 7; i++) {
                    if (looop[i] != 0 && looop[i + 1] != 0 && result[looop[i]] < 0) {
                        if (result[looop[i]] == -0.5 * result[looop[i + 1]]) {
                            h = looop[i];
                            l = 8 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                        }
                    }
                }
                break;
            case 0:

                for (int i = 0; i <= 7; i++) {
                    if (looop[i] != 0 && looop[i + 1] != 0 && result[looop[i]] < 0) {
                        if (result[looop[i]] == -result[looop[i + 1]]) {
                            h = looop[i];
                            l = 8 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                        }
                    }
                }
                break;
            case 1:
            case 2:
                for (int i = 0; i <= 7; i++) {
                    if (looop[i] != 0 && looop[i + 1] != 0 && result[looop[i]] < 0) {
                        if (result[looop[i]] == -2 * result[looop[i + 1]]) {
                            h = looop[i];
                            l = 8 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                        }
                    }
                }
                break;
            case 3:
            case 4:
                for (int i = 0; i <= 7; i++) {
                    if (looop[i] != 0 && result[looop[i]] > 0) {
                        h = looop[i];
                        l = 9 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                    }
                }
                break;
            case 5:
            case 6:
                for (int i = 0; i <= 7; i++)
                    if (looop[i] != 0 && looop[i + 1] != 0 && result[looop[i]] > 0) {
                        if (result[looop[i]] == -2 * result[looop[i + 1]]) {
                            h = looop[i];
                            l = 8 - (int) (Math.log(result[looop[i]]) / Math.log(2));
                        }
                    }
                break;
        }
        dara1 = netData;
        if (h == 100 || l == 100) {
            IsMyPostionConfirmed = false;
            return new int[]{h, l};
            //数据有异常
        } else {
            IsMyPostionConfirmed = true;
            return new int[]{h, l};
        }
    }

    private int[] TrackingStep(int direction, byte[] data) {

        byte[] dara1 = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        Log.i("计算", "跟踪算法");
        //
        int coord[] = new int[]{100, 100};

        switch (direction) {
            case 7:
            case -7:
                if (Math.log(data[coord[0] - 1] - dara1[coord[0]]) / Math.log(2) / 2 == 0) {
                    coord[0]--;
                }
                break;
            case -5:
            case -6:
                if (Math.log(data[coord[0] - 1] - dara1[coord[0]]) / Math.log(2) == -(8 - coord[1])) {
                    coord[0]--;
                    coord[1]++;
                }
                break;
            case -3:
            case -4:
                if (Math.log(data[coord[0]] - dara1[coord[0]]) / Math.log(2) == -(7 - coord[1])) {
                    coord[0]++;
                }
                break;
            case -1:
            case -2:
                if (Math.log(data[coord[0] + 1] - dara1[coord[0]]) / Math.log(2) == -(7 - coord[1])) {
                    coord[0]++;
                    coord[1]++;
                }
                break;
            case 0:
                if (Math.log(data[coord[0] + 1] - dara1[coord[0]]) / Math.log(2) / 2 == 0) {
                    coord[0]++;
                }
                break;
            case 1:
            case 2:
                if (Math.log(data[coord[0] + 1] - dara1[coord[0]]) / Math.log(2) == (8 - coord[1])) {
                    coord[0]++;
                    coord[1]--;
                }
                break;
            case 3:
            case 4:
                if (Math.log(data[coord[0]] - dara1[coord[0]]) / Math.log(2) == (8 - coord[1])) {
                    coord[0]--;
                }
                break;
            case 5:
            case 6:
                if (Math.log(data[coord[0] - 1] - dara1[coord[0]]) / Math.log(2) == (8 - coord[1])) {
                    coord[0]--;
                    coord[1]--;
                }
                break;
        }
        if (coord[0] == 100 || coord[1] == 100) {
            IsMyPostionConfirmed = false;
            return coord;
        }
        IsMyPostionConfirmed = true;
        return coord;
    }


    void text2Speech() {
        mTextToSpeech = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    //设置朗读语言
                    int supported = mTextToSpeech.setLanguage(Locale.US);
                    if ((supported != TextToSpeech.LANG_AVAILABLE) && (supported != TextToSpeech.LANG_COUNTRY_AVAILABLE)) {
                        Toast.makeText(mContext, "不支持当前语言！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onMapDrawFrame(GL10 gl10, MapStatus mapStatus) {

    }

    public static class LGPS_COLOR {
        static int UnKown = Color.argb(0x8f, 50, 50, 50);
        static int Clear = Color.argb(0x8f, 0, 0, 0x5f);
        static int Self = Color.argb(0x8f, 85, 0, 63);
        static int Obstacle = Color.argb(0x8f, 200, 21, 12);
        static int OtherStatic = Color.argb(0x8f, 200, 200, 20);
        static int OtherMoving = Color.argb(0x8f, 0x8f, 0, 0);
        static int Warning = Color.argb(0x8f, 0xef, 0, 0);
        static int SentryArea = Color.argb(0x8f, 0xef, 0x5f, 0);
        static int ScureArea = Color.argb(0x8f, 0xef, 00, 0xf1);
    }

}