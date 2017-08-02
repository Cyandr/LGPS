package com.example.myapp;

/**
 * Created by cyandr on 2016/8/13 0013.
 */
class Coordinate {
    Coordinate() {
        name = "空";
    }

    public String name;
    public int type;
    public int x = 0;
    public int y = 0;
    public void setXY(int[] m){
        x=m[0];
        y=m[1];
    }
    public String OutPut(){
        return String.valueOf(x)+String.valueOf(y);
    }


}
