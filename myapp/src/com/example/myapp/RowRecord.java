package com.example.myapp;

/**
 * Created by cyandr on 2016/8/7 0007.
 */
public class RowRecord {
    private String mRowDate;
    private String mRowTime;
    private String mSegNum;
    private byte mMyPos;
    private byte mSentryPos;
    private byte[] DRR;

    RowRecord() {

    }

    public String getmSegNum() {
        return mSegNum;
    }

    public void setmSegNum(String mSegNum) {
        this.mSegNum = mSegNum;
    }

    public String getmRowDate() {
        return mRowDate;
    }

    public void setDRR(byte[] DRR) {
        this.DRR = DRR;
    }

    public void setmRowDate(String mRowDate) {
        this.mRowDate = mRowDate;
    }

    public byte[] getDRR() {
        return DRR;
    }


    public String getmRowTime() {
        return mRowTime;
    }


    public Coordinate getmMyPos() {
        Coordinate coordinate=new Coordinate();
        coordinate.x=mMyPos/8;coordinate.y=mMyPos%8;
        return coordinate;
    }

    public byte getmSentryPos() {
        return mSentryPos;
    }

    public void setmMyPos(byte mMyPos) {
        this.mMyPos = mMyPos;
    }

    public void setmRowTime(String mRowTime) {
        this.mRowTime = mRowTime;
    }

    public void setmSentryPos(byte mSentryPos) {
        this.mSentryPos = mSentryPos;
    }
}
