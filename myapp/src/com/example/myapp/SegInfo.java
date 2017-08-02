package com.example.myapp;

/**
 * Created by yxh on 16-6-13.
 */
public class SegInfo {
    public final static String
            ntpIP = "202.120.2.101";
    public int SegPotal;
    private String SegNumber;
    private double LatLT;
    private double LngLT;
    private double LatRB;
    private double LngRB;
    private int S_inclination;
    private String RouterIp = null;
    private String S_Addr;
    private float S_Elavation;
    int m;
    private int S_Angle;
    private int S_lengthZ;
    private int S_lengthH;
    private int S_numZ;
    private int S_numH;
    private String S_type;

    public SegInfo() {


    }

    public SegInfo(String segNumber, int segPotal, double latLT, double lngLT, float S_inclination, String routerIp) {
        this.SegNumber = segNumber;
        this.SegPotal = segPotal;
        this.LatLT = latLT;
        this.LngLT = lngLT;
        this.S_inclination = (int) S_inclination;
        this.RouterIp = routerIp;
    }

    public static String getNtpIP() {
        return ntpIP;
    }

    public boolean isAvailable() {
        return !(SegNumber == null || RouterIp == null);

    }

    public double getLatRB() {
        return LatRB;
    }

    public void setLatRB(double latRB) {
        LatRB = latRB;
    }

    public double getLngRB() {
        return LngRB;
    }

    public void setLngRB(double lngRB) {
        LngRB = lngRB;
    }

    public int getS_Angle() {
        return S_Angle;
    }

    public void setS_Angle(int s_Angle) {
        S_Angle = s_Angle;
    }

    public float getS_Elavation() {
        return S_Elavation;
    }

    public void setS_Elavation(float s_Elavation) {
        S_Elavation = s_Elavation;
    }

    public int getS_lengthH() {
        return S_lengthH;
    }

    public void setS_lengthH(int s_lengthH) {
        S_lengthH = s_lengthH;
    }

    public int getS_lengthZ() {
        return S_lengthZ;
    }

    public void setS_lengthZ(int s_lengthZ) {
        S_lengthZ = s_lengthZ;
    }

    public int getS_numH() {
        return S_numH;
    }

    public void setS_numH(int s_numH) {
        S_numH = s_numH;
    }

    public int getS_numZ() {
        return S_numZ;
    }

    public void setS_numZ(int s_numZ) {
        S_numZ = s_numZ;
    }

    public String getS_Addr() {
        return S_Addr;
    }

    public void setS_Addr(String s_Addr) {
        S_Addr = s_Addr;
    }

    public String getS_type() {
        return S_type;
    }

    public void setS_type(String s_type) {
        S_type = s_type;
    }

    public String getSegNumber() {
        return this.SegNumber;
    }

    public void setSegNumber(String segNumber) {
        SegNumber = segNumber;
    }

    public int getSegPotal() {
        return this.SegPotal;
    }

    public void setSegPotal(int segPotal) {
        SegPotal = segPotal;
    }

    public double getLatLT() {
        return this.LatLT;

    }

    public void setLatLT(double latLT) {
        LatLT = latLT;
    }

    public double getLngLT() {
        return this.LngLT;

    }

    public void setLngLT(double lngLT) {
        LngLT = lngLT;
    }

    public float getS_inclination() {
        return this.S_inclination;
    }

    public void setS_inclination(int s_inclination) {
        S_inclination = s_inclination;
    }

    public String getRouterIp() {

        return this.RouterIp;
    }

    public void setRouterIp(String IP) {
        RouterIp = IP;

    }

}
