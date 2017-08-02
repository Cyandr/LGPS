package com.example.myapp;

/**
 * Created by cyandr on 2016/6/12 0012.
 */


import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class StepService extends Service {
    public static Boolean flag = false;
    private SensorManager sensorManager;
    private StepDetector stepDetector;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //这里开启了一个线程，因为后台服务也是在主线程中进行，这样可以安全点，防止主线程阻塞
        new Thread(new Runnable() {
            public void run() {
                try {
                    startStepDetector();
                } catch (UnknownError e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Log.i("服务", "传感器后台服务已启动。。。。。。");
    }


    private void startStepDetector() {
        flag = true;
        stepDetector = new StepDetector((LgpsApp) getApplication());
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);//获取传感器管理器的实例
        //获得传感器的类型，这里获得的类型是加速度传感器
        //此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        Sensor AccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor MagSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //sensorManager.registerListener(stepDetector, AccSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(stepDetector, AccSensor, 100);
        sensorManager.registerListener(stepDetector, MagSensor, 100);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        flag = false;
        if (stepDetector != null) {
            sensorManager.unregisterListener(stepDetector);
        }

    }

}
