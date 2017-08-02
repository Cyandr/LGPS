package com.example.myapp;

/**
 * Created by cyandr on 2016/6/12 0012.
 */

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;


public class StepDetector implements SensorEventListener {

    public static int CURRENT_SETP = 0;
    final int valueNum = 4;
    //动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    final float initialValue = (float) 1.3;
    float Mag[] = new float[3];
    //存放三轴数据
    float[] oriValues = new float[3];
    //用于存放计算阈值的波峰波谷差值
    float[] tempValue = new float[valueNum];
    int tempCount = 0;
    //是否上升的标志位
    boolean isDirectionUp = false;
    //持续上升次数
    int continueUpCount = 0;
    //上一点的持续上升的次数，为了记录波峰的上升次数
    int continueUpFormerCount = 0;
    //上一点的状态，上升还是下降
    boolean lastStatus = false;
    //波峰值
    float peakOfWave = 0;
    //波谷值
    float valleyOfWave = 0;
    //此次波峰的时间
    long timeOfThisPeak = 0;
    //上次波峰的时间
    long timeOfLastPeak = 0;
    //当前的时间
    long timeOfNow = 0;
    //当前传感器的值
    float gravityNew = 0;
    //上次传感器的值
    float gravityOld = 0;
    //初始阈值
    float ThreadValue = (float) 2.0;
    private LgpsApp myapp;
    private Handler handler;
    private float Acc[] = new float[3];
    private float PreAngle;


    /**
     * 传入上下文的构造函数
     */
    public StepDetector(LgpsApp app) {
        super();
        myapp = app;
        handler = myapp.getHandler();
    }

    //当传感器检测到的数值发生变化时就会调用这个方法
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            Mag = sensorEvent.values;
            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, Acc, Mag);
            SensorManager.getOrientation(R, values);// 要经过一次数据格式的转换，转换为度
            float RelativeAngle = (float) Math.toDegrees(values[0]);
            int NowDirection = (int) (RelativeAngle / 22.5);

            if (Math.abs(RelativeAngle - PreAngle) > 20) {
                timeOfNow = System.currentTimeMillis();
                passDirectionWhenChange(NowDirection, timeOfNow);
                PreAngle = RelativeAngle;
            }
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Acc = sensorEvent.values;
            System.arraycopy(sensorEvent.values, 0, oriValues, 0, 3);
            gravityNew = (float) Math.sqrt(oriValues[0] * oriValues[0]
                    + oriValues[1] * oriValues[1] + oriValues[2] * oriValues[2]);
            DetectorNewStep(gravityNew);
        }
    }

    private void passDirectionWhenChange(int nowDirection, long dire) {
        Message sensormessage = Message.obtain();
        sensormessage.what = LGPS_CONST.SENSOR_DIRECTION_CHANGED;
        sensormessage.arg1 = nowDirection;

        int[] mm = calSyncTime(dire);
        String tt = mm[0] + ":" + mm[1] + ":" + mm[2]+":"+mm[3];
        sensormessage.obj = tt;
        if (handler != null) {
            handler.sendMessage(sensormessage);
        }
    }

    private void passWalkingStatus(int nowDirection, float time) {
        Message sensormessage = Message.obtain();
        sensormessage.what = LGPS_CONST.ONESTEPWALKED;
        sensormessage.arg1 = nowDirection;
        sensormessage.arg2 = CURRENT_SETP;
        sensormessage.obj = time;
        if (handler != null) {
            handler.sendMessage(sensormessage);
        }
    }

    private void passOneStepWalked(long mills) {
        Message sensormessage = Message.obtain();
        sensormessage.what = LGPS_CONST.ONESTEPWALKED;
        int[] mm = calSyncTime(mills);
        String tt = mm[0] + ":" + mm[1] + ":" + mm[2]+":"+mm[3];
        sensormessage.obj = tt;
        if (handler != null) {
            handler.sendMessage(sensormessage);
        }
    }

    //当传感器的经度发生变化时就会调用这个方法，在这里没有用
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    /*
     * 检测步子，并开始计步
	 * 1.传入sersor中的数据
	 * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
	 * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
	 * */
    public void DetectorNewStep(float values) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (DetectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();
                if (timeOfNow - timeOfLastPeak >= 250
                        && (peakOfWave - valleyOfWave >= ThreadValue)) {
                    timeOfThisPeak = timeOfNow;
                    /*
                     * 更新界面的处理，不涉及到算法
					 * 一般在通知更新界面之前，增加下面处理，为了处理无效运动：
					 * 1.连续记录10才开始计步
					 * 2.例如记录的9步用户停住超过3秒，则前面的记录失效，下次从头开始
					 * 3.连续记录了9步用户还在运动，之前的数据才有效
					 * */
                    passOneStepWalked(timeOfNow);
                }
                if (timeOfNow - timeOfLastPeak >= 250 && (peakOfWave - valleyOfWave >= initialValue)) {
                    timeOfThisPeak = timeOfNow;
                    ThreadValue = Peak_Valley_Thread(peakOfWave - valleyOfWave);
                }
            }
        }
        gravityOld = values;
    }

    /*
     * 检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于20
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     * */
    public boolean DetectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 || oldValue >= 20)) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    /*
     * 阈值的计算
     * 1.通过波峰波谷的差值计算阈值
     * 2.记录4个值，存入tempValue[]数组中
     * 3.在将数组传入函数averageValue中计算阈值
     * */
    public float Peak_Valley_Thread(float value) {
        float tempThread = ThreadValue;
        if (tempCount < valueNum) {
            tempValue[tempCount] = value;
            tempCount++;
        } else {
            tempThread = averageValue(tempValue, valueNum);
            System.arraycopy(tempValue, 1, tempValue, 0, valueNum - 1);
            tempValue[valueNum - 1] = value;
        }
        return tempThread;

    }

    /*
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     * */
    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / valueNum;
        if (ave >= 8)
            ave = (float) 4.3;
        else if (ave >= 7 && ave < 8)
            ave = (float) 3.3;
        else if (ave >= 4 && ave < 7)
            ave = (float) 2.3;
        else if (ave >= 3 && ave < 4)
            ave = (float) 2.0;
        else {
            ave = (float) 1.3;
        }
        return ave;
    }

    private int[] calSyncTime(long secsSince1900) {
        long MsecsSince1900 = secsSince1900 / 1000;
        long seventyYears = 2208988800L;// subtract seventy years:
        long epoch = MsecsSince1900 - seventyYears + 8 * 3600;// print Unix time:
        int mHour = 23 + (int) ((epoch % 86400L) / 3600);
        int mMinute = 59 + (int) ((epoch % 3600) / 60);
        int mSecond = 59 + (int) (epoch % 60);
        int nano = (int) (secsSince1900 % 1000)/200;
        return new int[]{mHour, mMinute, mSecond,nano};
    }

}
