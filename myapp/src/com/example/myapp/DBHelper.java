package com.example.myapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.baidu.mapapi.model.LatLng;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by wyt on 16-5-26.
 */
public class DBHelper {
    public static final String DB_NAME = "data.db"; //保存的数据库文件名
    public static final String PACKAGE_NAME = "com.example.myapp";
    public static final String DB_PATH = "/data" + Environment.getDataDirectory().getAbsolutePath() + "/" + PACKAGE_NAME;
    //在手机里存放数据库的位置
    public static final String OUT_PATH = Environment.getExternalStorageDirectory() + "/LGPSDataBase";

    public SQLiteDatabase datab;
    String dbfile = DB_PATH + "/" + DB_NAME;
    private Context mcontext;


    public DBHelper(Context context) {
        mcontext = context;
        System.out.println(Environment.getDataDirectory().getAbsolutePath());
        initDB();
    }

    public void open() {
        datab = SQLiteDatabase.openOrCreateDatabase(dbfile, null);
    }

    public boolean importDB() {
        try {
            if (new File(dbfile).exists()) {//判断数据库文件是否存在，若不存在则执行导入，否则直接打开数据库
                System.out.println("数据库存在****");
                File outdata = new File(OUT_PATH);
                if (!outdata.exists()) {
                  /* for (String file:outdata.list()){
                       File file1=new File(file);*/
                    outdata.mkdirs();
                    //}
                }
                File target = new File(OUT_PATH + "/" + DB_NAME);
                if (target.exists()) {
                    target.delete();
                } else {
                    target.createNewFile();
                }
                // InputStream is = mcontext.getResources().openRawResource(R.raw.data); //欲导入的数据库
                File dbin = new File(dbfile);
                FileInputStream fis = new FileInputStream(dbin);
                FileOutputStream fos = new FileOutputStream(OUT_PATH + "/" + DB_NAME);
                int BUFFER_SIZE = 40000;
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                fis.close();
            }
            System.out.println("数据库存在");

        } catch (FileNotFoundException e) {
            Log.e("Database", "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Database", "IO exception");
            e.printStackTrace();
        } catch (NullPointerException n) {
            System.out.println("还没有对象哦");
        }

        return false;
    }

    private void initDB() {

        try {
            if (!(new File(dbfile).exists())) {//判断数据库文件是否存在，若不存在则执行导入，否则直接打开数据库
                System.out.println("数据库不存在");
                InputStream is = mcontext.getResources().openRawResource(R.raw.data); //欲导入的数据库
                FileOutputStream fos = new FileOutputStream(dbfile);
                int BUFFER_SIZE = 40000;
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            }
            System.out.println("数据库存在");
        } catch (FileNotFoundException e) {
            Log.e("Database", "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Database", "IO exception");
            e.printStackTrace();
        } catch (NullPointerException n) {
            System.out.println("还没有对象哦");
        }
    }
    public boolean insertRow(RowRecord rowRecord) {
        byte[] m = rowRecord.getDRR();
        datab.execSQL("insert into l_data (DSNum,DDate,Dtime,Dr1,Dr2,Dr3,Dr4,Dr5,Dr6,Dr7,Dr8,Dmypos,Dsentypos) " +
                "values(?,?,?,?,?,?,?,?,?,?,?,?,?)", new Object[]{rowRecord.getmSegNum(), rowRecord.getmRowDate(), rowRecord.getmRowTime(),
                m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], rowRecord.getmMyPos(), rowRecord.getmSentryPos()
        });
        Log.i("数据库记录：", rowRecord.getmRowDate() + rowRecord.getmRowTime() + "记录成功");
        return true;
    }

    public boolean insertKey(int keyposition, String value) {
        datab.execSQL("insert into l_keymap (key_id,key_value) values (?,?)", new Object[]{keyposition, value});
        return true;
    }

    public boolean updateKey(int keyposition, String value) {
        datab.execSQL("update l_keymap set key_value = ? where key_id=?", new Object[]{value, keyposition});
        return true;
    }

    public String readKey(int keyposition) {
        // datab.execSQL("update l_keymap set key_value = ? where key_id=?",new Object[]{value,keyposition});
        Cursor cursor = datab.rawQuery("select key_value from l_keymap where key_id=?", new String[]{String.valueOf(keyposition)});
        while (cursor.moveToNext()) {
            String keyValue = cursor.getString(cursor.getColumnIndex("key_value"));
            return keyValue;
        }
        return null;
    }
    public String readdest() {
        // datab.execSQL("update l_keymap set key_value = ? where key_id=?",new Object[]{value,keyposition});
        Cursor cursor = datab.rawQuery("select dest_point from l_dest where ddid = 1 ", null);
        while (cursor.moveToNext()) {
            String keyValue = cursor.getString(cursor.getColumnIndex("dest_point"));
            return keyValue;
        }
        return null;
    }
    public boolean updatedest(String value) {
        datab.execSQL("update l_dest set dest_point = ? where ddid= 1 ", new Object[]{value});
        return true;
    }
    public String readsentry() {
        // datab.execSQL("update l_keymap set key_value = ? where key_id=?",new Object[]{value,keyposition});
        Cursor cursor = datab.rawQuery("select sentry_point from l_sentry where ssid = 1 ", null);
        while (cursor.moveToNext()) {
            String keyValue = cursor.getString(cursor.getColumnIndex("sentry_point"));
            return keyValue;
        }
        return null;
    }
    public boolean updatesentry(String value) {
        datab.execSQL("update l_sentry set sentry_point = ? where ssid= 1 ", new Object[]{value});
        return true;
    }

    public List<String> ReadRowTime(String date) {
        List<String> aa = new ArrayList<>();
        int i = 0;
        Cursor cursor = datab.rawQuery("select * from l_data where DDate=?", new String[]{date});
        String oldsst = "";
        while (cursor.moveToNext()) {
            String ss = (cursor.getString(cursor.getColumnIndex("Dtime")));
            if (!Objects.equals(oldsst, ss))
                aa.add(ss);
            oldsst = ss;
        }
        return aa;
    }


    public List<String> ReadRowDates() {
        List<String> aa = new ArrayList<>();
        int i = 0;
        Cursor cursor = datab.rawQuery("select * from l_data ", null);
        String oldss = "";
        while (cursor.moveToNext()) {
            String ss = (cursor.getString(cursor.getColumnIndex("DDate")));
            if (!Objects.equals(oldss, ss))
                aa.add(ss);
            oldss = ss;
        }
        return aa;
    }

    public List<RowRecord> readRows() {
        List<RowRecord> Lrr = new ArrayList<>();
        Cursor cursor = datab.rawQuery("select * from l_data ", null);
        while (cursor.moveToNext()) {
            RowRecord rr = new RowRecord();
            byte[] m = new byte[0];
            rr.setmRowTime(cursor.getString(cursor.getColumnIndex("Dtime")));
            rr.setmRowDate(cursor.getString(cursor.getColumnIndex("DDate")));
            m[0] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr1")));
            m[1] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr2")));
            m[2] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr3")));
            m[3] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr4")));
            m[4] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr5")));
            m[5] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr6")));
            m[6] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr7")));
            m[7] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr8")));
            rr.setDRR(m);
            rr.setmMyPos((byte) cursor.getInt(cursor.getColumnIndex("Dmypos")));
            rr.setmSentryPos((byte) cursor.getInt(cursor.getColumnIndex("Dsentypos")));
            Lrr.add(rr);
        }
        return Lrr;
    }

    public List<RowRecord> readRowsData(String date, String time) {
        List<RowRecord> Lrr = new ArrayList<>();
        Log.i("数据库", date + time);
        Cursor cursor = datab.rawQuery("select DSNum,Dr1,Dr2,Dr3,Dr4,Dr5,Dr6,Dr7,Dr8,Dmypos,Dsentypos from l_data where DDate='" + date + "' and Dtime='" + time + "'", null);
        Log.i("数据库查询操作", "select Dr1,Dr2,Dr3,Dr4,Dr5,Dr6,Dr7,Dr8,Dmypos,Dsentypos from l_data where DDate='" + date + "' and Dtime='" + time + "'" + cursor.getCount() + cursor.toString());

        while (cursor.moveToNext()) {
            RowRecord rr = new RowRecord();
            byte[] m = new byte[8];
            rr.setmRowTime(time);
            rr.setmRowDate(date);
            rr.setmSegNum(cursor.getString(cursor.getColumnIndex("DSNum")));
            Log.i("段编号的数据库chsh", cursor.getString(cursor.getColumnIndex("DSNum")));
            m[0] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr1")));
            m[1] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr2")));
            m[2] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr3")));
            m[3] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr4")));
            m[4] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr5")));
            m[5] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr6")));
            m[6] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr7")));
            m[7] = ((byte) cursor.getInt(cursor.getColumnIndex("Dr8")));
            rr.setDRR(m);
            rr.setmMyPos((byte) cursor.getInt(cursor.getColumnIndex("Dmypos")));
            rr.setmSentryPos((byte) cursor.getInt(cursor.getColumnIndex("Dsentypos")));
            Lrr.add(rr);
        }
        return Lrr;
    }

    SegInfo getSegInfo(String Snnum) {

        Cursor cursor = datab.rawQuery("select SNetAddr,SLatLT,SLngLT,SLatRB,SLngRB,SAddr,SInclination,SElavation," +
                "SAngle,SLengthZ,SlengthH,SSumZ,SSUMH,SType " +
                "from l_segment where SNum ='" + Snnum + "'", null);

        Log.i("查询段操作", "select SNetAddr,SLatLT,SLngLT,SLatRB,SLngRB,SAddr,SInclination,SElavation," +
                "SAngle,SLengthZ,SlengthH,SSumZ,SSUMH,SType " +
                "from l_segment where SNum ='" + Snnum + "'");
        if (cursor.moveToNext()) {
            SegInfo segInfo = new SegInfo();
            segInfo.setRouterIp(cursor.getString(cursor.getColumnIndex("SNetAddr")));
            segInfo.setSegNumber(Snnum);
            segInfo.setLatLT(cursor.getDouble(cursor.getColumnIndex("SLatLT")));
            segInfo.setLngLT(cursor.getDouble(cursor.getColumnIndex("SLngLT")));
            segInfo.setLatRB(cursor.getDouble(cursor.getColumnIndex("SLatRB")));
            segInfo.setLngRB(cursor.getDouble(cursor.getColumnIndex("SLngRB")));
            segInfo.setS_Addr(cursor.getString(cursor.getColumnIndex("SAddr")));
            segInfo.setS_inclination(cursor.getInt(cursor.getColumnIndex("SInclination")));
            segInfo.setS_Elavation(cursor.getFloat(cursor.getColumnIndex("SElavation")));
            segInfo.setS_Angle(cursor.getInt(cursor.getColumnIndex("SAngle")));
            segInfo.setS_lengthZ(cursor.getInt(cursor.getColumnIndex("SLengthZ")));
            segInfo.setS_lengthH(cursor.getInt(cursor.getColumnIndex("SLengthH")));
            segInfo.setS_numZ(cursor.getInt(cursor.getColumnIndex("SSumZ")));
            segInfo.setS_numH(cursor.getInt(cursor.getColumnIndex("SSumH")));
            segInfo.setS_type(cursor.getString(cursor.getColumnIndex("SType")));
            Log.i("段的数据库", "已查询到该段");
            return segInfo;
        }
        return null;
    }

    SegInfo getSegInfo(LatLng latLng) {

        Cursor cursor = datab.rawQuery("select SNetAddr,SNum,SLatLT,SLngLT,SLatRB,SLngRB,SAddr,SInclination,SElavation," +
                "SAngle,SLengthZ,SlengthH,SSumZ,SSUMH,SType " +
                "from l_segment where SLatLT <= " + latLng.latitude + " and SLatRB >= " + latLng.latitude + " and " +
                "SLngLT <= " + latLng.longitude + " and SLngRB >= " + latLng.longitude, null);
        if (cursor.moveToNext()) {
            SegInfo segInfo = new SegInfo();
            segInfo.setRouterIp(cursor.getString(cursor.getColumnIndex("SNetAddr")));
            segInfo.setSegNumber(cursor.getString(cursor.getColumnIndex("SNum")));
            segInfo.setLatLT(cursor.getDouble(cursor.getColumnIndex("SLatLT")));
            segInfo.setLngLT(cursor.getDouble(cursor.getColumnIndex("SLngLT")));
            segInfo.setLatRB(cursor.getDouble(cursor.getColumnIndex("SLatRB")));
            segInfo.setLngRB(cursor.getDouble(cursor.getColumnIndex("SLngRB")));
            segInfo.setS_Addr(cursor.getString(cursor.getColumnIndex("SAddr")));
            segInfo.setS_inclination(cursor.getInt(cursor.getColumnIndex("SInclination")));
            segInfo.setS_Elavation(cursor.getFloat(cursor.getColumnIndex("SElavation")));
            segInfo.setS_Angle(cursor.getInt(cursor.getColumnIndex("SAngle")));
            segInfo.setS_lengthZ(cursor.getInt(cursor.getColumnIndex("SLengthZ")));
            segInfo.setS_lengthH(cursor.getInt(cursor.getColumnIndex("SlengthH")));
            segInfo.setS_numZ(cursor.getInt(cursor.getColumnIndex("SSumZ")));
            segInfo.setS_numH(cursor.getInt(cursor.getColumnIndex("SSUMH")));
            segInfo.setS_type(cursor.getString(cursor.getColumnIndex("SType")));
            return segInfo;
        }

        return null;
    }

  /*  List<Coordinate> getAllElement(String segID) {
        List<Coordinate> allelement = new ArrayList<>();
        Cursor cursor = datab.rawQuery("select Mname,MBeginPosition,MEndPosition from l_map where MSegNum='" +
                segID + "'", null);
        while (cursor.moveToNext()) {
            String ecodeB = cursor.getString(cursor.getColumnIndex("MBeginPosition"));
            String ecodeE = cursor.getString(cursor.getColumnIndex("MEndPosition"));
            String ename = cursor.getString(cursor.getColumnIndex("Mname"));

            Coordinate coordinate = new Coordinate();
            LgpsCode lgpsCode = new LgpsCode();
       *//*     int[] mm = lgpsCode.getPosition(ecodeB);
        *//*    int[] nn = lgpsCode.getPosition(ecodeE);*//**//*
            Log.i("地图记录：", String.valueOf(mm[0]) + " 行处" + String.valueOf(mm[1]) + " 列处 " + ename + "***" + cursor.getCount());
            Log.i("地图记录：", String.valueOf(nn[0]) + " 行处" + String.valueOf(nn[1]) + " 列处 " + ename + "***" + cursor.getCount());
            for (int i = 0; i <= (nn[0] - mm[0]); i++) {
                for (int j = 0; j <= (nn[1] - mm[1]); j++) {
                    coordinate.x = j;
                    coordinate.y = i;
                    coordinate.name = ename;
                    coordinate.type = LgpsServer.LGPS_COLOR.OtherStatic;
                    Log.i("地图记录：", j + " 行处" + i + " 列处 " + ename);
                    allelement.add(coordinate);
                }*//*

            }

        }

        return allelement;
    }*/

    public void close() {
        datab.close();
    }
}


