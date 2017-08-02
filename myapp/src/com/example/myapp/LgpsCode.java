package com.example.myapp;

import android.content.Context;
import android.util.Log;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by cyandr on 2016/8/13 0013.
 */
public class LgpsCode {

    private static final double EARTH_RADIUS = 6378137;
    int ZoomLevel;
    BaiduMap mbaiduMap;
    Context mContext;
    HashMap<Integer, Character> Hmap = new HashMap<>();
    HashMap<Integer, String> Lmap = new HashMap<>();
    boolean Rendering = true;
    CountDownLatch RenderingNow;
    HashMap<Character, Integer> PHmap = new HashMap<>();
    HashMap<String, Integer> PLmap = new HashMap<>();
    ArrayList<Integer> SCALE;
    String LOCAL_CODE;


    LgpsCode(BaiduMap baiduMap, Context context) {
        SCALE = new ArrayList<Integer>();
       /* Hmap.put(0, 'A');
        Hmap.put(1, 'B');
        Hmap.put(2, 'C');
        Hmap.put(3, 'D');
        Hmap.put(4, 'E');
        Hmap.put(5, 'F');
        Hmap.put(6, 'G');
        Hmap.put(7, 'H');*/

     /*   PHmap.put('A', 0);
        PHmap.put('B', 1);
        PHmap.put('C', 2);
        PHmap.put('D', 3);
        PHmap.put('E', 4);
        PHmap.put('F', 5);
        PHmap.put('G', 6);
        PHmap.put('H', 7);*/

      /*  Lmap.put(0, "01");
        Lmap.put(1, "02");
        Lmap.put(2, "04");
        Lmap.put(3, "08");
        Lmap.put(4, "10");
        Lmap.put(5, "20");
        Lmap.put(6, "40");
        Lmap.put(7, "80");*/

    /*    PLmap.put("01", 0);
        PLmap.put("02", 1);
        PLmap.put("04", 2);
        PLmap.put("08", 3);
        PLmap.put("10", 4);
        PLmap.put("20", 5);
        PLmap.put("40", 6);
        PLmap.put("80", 7);*/

        mbaiduMap = baiduMap;
        mContext = context;
    }

    LgpsCode() {

    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public void ListEarthCode(LRect rect, String s1b, LRect accurate, LatLng latlng) {
        double singleH = rect.getHeight() / 8;
        double singleL = rect.getWidth() / 8;
        for (int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                LRect arect = new LRect(rect.left + j * singleL, rect.top - i * singleH, rect.left + (j + 1) * singleL, rect.top - (i + 1) * singleH);

                LatLng pt1 = new LatLng(arect.bottom, arect.left);
                LatLng pt2 = new LatLng(arect.top, arect.left);
                LatLng pt3 = new LatLng(arect.top, arect.right);
                LatLng pt4 = new LatLng(arect.bottom, arect.right);

                List<LatLng> pts = new ArrayList<LatLng>();
                pts.add(pt1);
                pts.add(pt2);
                pts.add(pt3);
                pts.add(pt4);
                OverlayOptions ooPolygon = new PolygonOptions().points(pts)
                        .stroke(new Stroke(5, 0xFA00FF00)).fillColor(0x84FFFF00);
                mbaiduMap.addOverlay(ooPolygon);


                if (latlng.latitude > pt1.latitude && latlng.latitude < pt2.latitude && latlng.longitude > pt1.longitude && latlng.longitude < pt4.longitude) {
                    if (rect.getHeight() / accurate.getHeight() > 10 && rect.getWidth() / accurate.getWidth() > 10) {

                        //    if (i < 8 && j > 0) {
                           /* char hang = Hmap.get(i);
                            String lie = Lmap.get(8 - j);
                            String AA = lie.replace('0', hang);*/
                        String AA = String.valueOf(i + 1) + String.valueOf(j + 1);
                        s1b = s1b + AA;
                        LatLng llText = new LatLng((pt1.latitude + 0.5 * (pt2.latitude - pt1.latitude)), pt1.longitude + 0.5 * (pt4.longitude - pt1.longitude));
                        OverlayOptions ooText = new TextOptions().bgColor(0xAAFFFF00)
                                .fontSize(24).fontColor(0xFFFF00FF).text(s1b).rotate(0)
                                .position(llText);
                        LOCAL_CODE = s1b;
                        mbaiduMap.addOverlay(ooText);
                        //  }
                        ListEarthCode(arect, s1b, accurate, latlng);
                    }
                }
            }
        }
    }

    public String getLocalCode(LatLng l) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String sb = "";
        LRect lrect = new LRect(0, 90, 180, 0);
        LRect arect = new LRect(0, 1e-5, 1e-5, 0);
        final String[] a = new String[1];
        new Thread(new Runnable() {
            @Override
            public void run() {
                RenderingNow = new CountDownLatch(1);
                ListEarthCode(lrect, sb, arect, l);
                RenderingNow.countDown();
                countDownLatch.countDown();
            }
        }).start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return LOCAL_CODE;
    }

    public LatLng LparseCode(String code) {


        LRect errth = new LRect(0, 90, 180, 0);
        for (int i = 0; i <= code.length() - 2; i += 2) {
            int m = (int) code.charAt(i) - 48;
            int n = code.charAt(i + 1) - 48;
            Log.i("数据解码中", "第" + i + "个为：" + m + "  第二个为：" + n + "  ");
            errth = getRectA(errth, m, n);
        }


        return new LatLng(errth.getCentre().x, errth.getCentre().y);
    }

    LRect getRectA(LRect lRect, int H, int L) {
        double single_H = lRect.getHeight() / 8;
        double single_L = lRect.getWidth() / 8;
        Log.i("数据解码中坐标", String.valueOf(lRect.left) + "  " + lRect.top + "   " + lRect.right + "   " + lRect.bottom);
        Log.i("数据解码中大小", "height  " + lRect.getHeight() + "  " + "width  " + lRect.getWidth());
        Log.i("数据解码中间隔", "height  " + lRect.getHeight() / 8 + "  " + "width  " + lRect.getWidth() / 8);
        Log.i("数据解码中", "---------------------------------------------------------------");
        Log.i("数据解码中left：", String.valueOf(lRect.left) + "+" + L + "*" + single_L);
        Log.i("数据解码中top：", String.valueOf(lRect.top) + "-" + (H - 1) + "*" + single_H);
        Log.i("数据解码中right：", String.valueOf(lRect.left) + "+" + (L + 1) + "*" + single_L);
        Log.i("数据解码中bottom：", String.valueOf(lRect.top) + "-" + (H) + "*" + single_H);
        return new LRect(lRect.left + (L - 1) * single_L, lRect.top - (H - 1) * single_H, lRect.left + L * single_L, lRect.top - H * single_H);
    }

    public void clearCodeView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (RenderingNow != null) {
                    try {
                        RenderingNow.await();
                        mbaiduMap.clear();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    mbaiduMap.clear();
                }
            }
        }).start();

    }

    public double GetDistance(double lng1, double lat1, double lng2, double lat2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000 / 10e3;
        return s;
    }

    class LPoint {
        double x;
        double y;

        LPoint() {
            x = 0;
            y = 0;
        }

    }

    class LRect {
        double left;
        double right;
        double top;
        double bottom;

        LRect(double aleft, double atop, double aright, double abottom) {
            left = aleft;
            top = atop;
            right = aright;
            bottom = abottom;
        }

        double getHeight() {
            return Math.abs(bottom - top);
        }

        double getWidth() {
            return Math.abs(right - left);
        }

        LPoint getCentre() {
            LPoint point = new LPoint();
            point.x = (left + right) / 2;
            point.y = (top + bottom) / 2;
            return point;
        }
    }

}
