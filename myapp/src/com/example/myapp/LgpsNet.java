package com.example.myapp;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.*;

/**
 * Created by cyandr on 2016/8/11 0011.
 */
class LgpsNet extends Thread {

    static final private int SEGMENT_SERVER_PORT = 9000;
    private final static int Local_PORT = 8081;
    private byte[] lMsg = new byte[40];
    private byte segmentNumber;
    private InetAddress SegIP;
    private boolean still = true;
    private Handler handler;
    private String SEGIP;

    public LgpsNet(String Segcode, String IP, Handler ahandler) throws IOException {
        int i;
        for (i = 0; i <= 35; i++) lMsg[i] = 0;
        segmentNumber = getSegNum(16);
        SEGIP = IP;
        still = true;
        handler = ahandler;

    }

    private byte getSegNum(int Segcode) {
        return (byte) (Segcode % 4);
    }

    public void run() {

        while (still) {
            try {
                SegIP = InetAddress.getByName(SEGIP);
                SendRequest(segmentNumber);
                RecvData();
                Log.i("网络", "发送后****" + String.valueOf(still));
                Thread.sleep(800);

                Log.i("网络", "接收后****" + String.valueOf(still));
            } catch (IOException e1) {
                e1.printStackTrace();
                Message msg = Message.obtain();
                msg.what = LGPS_CONST.CELL2SEG_ERROR;
                handler.sendMessage(msg);
                Log.i("网络", "**********已退出运行1**********");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.i("网络", "**********已退出运行2**********");
            }
        }
    }

    private void RecvData() throws IOException {
        DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length); // 新建一个 DatagramSocket 类
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket(null);
            ds.setReuseAddress(true);
            ds.bind(new InetSocketAddress(Local_PORT));
            //  ds = new DatagramSocket(Local_PORT);

            Log.i("网络", "接收中。。。" + ds.getLocalPort());
            ds.receive(dp);
            if (lMsg[lMsg.length - 1] != 0) {
                Message msg = Message.obtain();
                msg.what = LGPS_CONST.RECEIVED_NET_DATA;
                msg.obj = lMsg;
                handler.sendMessage(msg);
                Log.i("网络", "*********已发送消息**********");
                ds.close();
            } else {
                Log.i("网络", "段没有响应。。。。");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("网络", "出现错误IOException");
            ds.close();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
    }

    public void SetQuit() {
        still = false;
    }

    private void SendRequest(byte portcode) throws IOException {
        byte mode = 125;
        byte attribute = 90;
        DatagramSocket ds;
        byte[] udpMsg = {portcode, attribute, mode}; // 新建一个 DatagramSocket 对象
        try {// 初始化 DatagramSocket 对象
            ds = new DatagramSocket(); // 初始化 InetAddress 对象
            // 初始化 DatagramPacket 对象
            DatagramPacket dp = new DatagramPacket(udpMsg, 3, SegIP, SEGMENT_SERVER_PORT);
            // 发送
            ds.send(dp);
            Log.i("网络", "请求已发送" + SegIP + "  " + SEGMENT_SERVER_PORT);
        } // 异常处理 // Socket 连接异常
        catch (SocketException e) {
            e.printStackTrace();
            // 不能连接到主机
        }
    }
}
