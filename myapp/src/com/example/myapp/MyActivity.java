package com.example.myapp;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.*;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;

import java.io.IOException;
import java.util.List;

public class MyActivity extends Activity {

    private SegInfo mSegmentInfo, HistorySegment;
    private LgpsCode lgpsCode;
    private double S_map;
    private PopupWindow modeWindow;
    private TextView self_position;
    private LatLng liy;
    private TextView segment_type;
    private MyLocationData locData;
    private boolean codemapclicked = false;
    private String locaCode;
    private boolean PEOPLE_WALKING = true;

    private LocationClient mLocationClient = null;
    private BDLocationListener myListener = new MyLocationListener();
    private TextView net_status;
    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    private boolean isFirstLoc = true; // 是否首次定位
    private int scale = 10;
    private LgpsServer lgpsServer;
    private byte mWokingMode;
    private LgpsNet lgpsNet;
    private TextView txdirection;
    private TextView surronding;
    private TextView segbase_info;
    private int local_STEP_COUNT = 0, old_step = 0;
    private LinearLayout linearLayoutleft;
    private Button main_button;
    private ImageView ImgCompass, plate;
    private ImageView clearhistory;

    LgpsApp myapp;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case LGPS_CONST.HISTORY_CONCE_DISPLAY:
                    HistorySegment = lgpsServer.HistorysegInfo;
                    setOutputText(HistorySegment);
                    break;
                case LGPS_CONST.NO_THIS_SEGMENT:
                    Toast.makeText(MyActivity.this, "没有查询到该段相关信息！", Toast.LENGTH_SHORT).show();
                    break;
                case LGPS_CONST.SERVER_MSG:

                    segbase_info.setText((String) msg.obj);
                    break;
                case LGPS_CONST.GOE_CODE_ACQUIRED:
                    Toast.makeText(MyActivity.this, "当前位置编码为：" + locaCode, Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder ab = new AlertDialog.Builder(MyActivity.this);
                    ab.setTitle("验证编码：" + locaCode);

                    LatLng L = lgpsCode.LparseCode(locaCode);
                    java.text.DecimalFormat df = new java.text.DecimalFormat("#.00000");
                    String lat = df.format(L.latitude);
                    String lng = df.format(L.longitude);
                    ab.setMessage("确定验证当前编码？\nGPS：" + liy.toString() + "\n  编码解析的经度：" +lat + "纬度：" + lng + "?");
                    ab.setPositiveButton("验证", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MapStatus.Builder builder = new MapStatus.Builder();
                            builder.target(L).zoom(21.0f);
                            // Toast.makeText(MyActivity.this, "已确定位置", Toast.LENGTH_SHORT).show();
                            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                        }
                    });
                    ab.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    ab.show();
                    break;
                case LGPS_CONST.PEOPLE_STOPING:
                    PEOPLE_WALKING = false;
                    break;
                case LGPS_CONST.ONESTEPWALKED:

                    self_position.setText((String) msg.obj);
                    local_STEP_COUNT++;
                    changebKGColor();
                    break;
                case LGPS_CONST.SENSOR_DIRECTION_CHANGED:
                    int di8rection = msg.arg1;
                    String m = (String) msg.obj;
                    String[] mm = m.split(":");
                    self_position.setText((String) msg.obj);
                    if (local_STEP_COUNT > old_step) {
                        int[] status = {1, msg.arg1, Integer.parseInt(mm[2])};
                        lgpsServer.setmPeopleStatus(status);
                        old_step = local_STEP_COUNT;
                    }

                    txdirection.setText(setdirection(di8rection));
                    //
                    AniRotateImage(-(float) msg.arg2);
                    break;
                case LGPS_CONST.RECEIVED_NET_DATA:
                    Log.i("网络", "接收到网络数据");
                    lgpsServer.updateNetData((byte[]) msg.obj);
                    net_status.setText("已接收到数据");
                    clearhistory.setEnabled(true);
                    clearhistory.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myapp = (LgpsApp) getApplication();
        myapp.setHandler(handler);

        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.main);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mSegmentInfo = new SegInfo();

        initActionBar();
        initView();
        initMap();
        lgpsServer = new LgpsServer(handler, MyActivity.this, mBaiduMap);


        Intent intent = new Intent(this, StepService.class);
        this.startService(intent);
        main_button = (Button) findViewById(R.id.main_button);
        main_button.setBackgroundColor(getResources().getColor(R.color.cyan));
        main_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                OnHistoryReview();
                return false;
            }
        });
        main_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnModeWorking();
            }
        });
    }


    private void initView() {

        txdirection = (TextView) findViewById(R.id.direction);
        ImgCompass = (ImageView) findViewById(R.id.l_compass);
        linearLayoutleft = (LinearLayout) findViewById(R.id.lefet1);
        plate = (ImageView) findViewById(R.id.l_platte);

        net_status = (TextView) findViewById(R.id.netState);
        segment_type = (TextView) findViewById(R.id.segmentInfo);
        surronding = (TextView) findViewById(R.id.surroundings);
        self_position = (TextView) findViewById(R.id.self_position);
        segbase_info = (TextView) findViewById(R.id.segment_base);
        Button showThecode = (Button) findViewById(R.id.showcodes);

        showThecode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!codemapclicked) {
                    if (liy != null && liy.longitude != 0 && liy.latitude != 0) {

                        locaCode = lgpsCode.getLocalCode(liy);
                        Message message = Message.obtain();
                        message.what = LGPS_CONST.GOE_CODE_ACQUIRED;
                        handler.sendMessage(message);
                        codemapclicked = true;
                    } else {
                        Toast.makeText(MyActivity.this, "正在定位中，请等待！", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    lgpsCode.clearCodeView();
                    codemapclicked = false;
                }
            }
        });

        clearhistory = (ImageView) findViewById(R.id.clear_history);
        clearhistory.setEnabled(false);
        clearhistory.setVisibility(View.INVISIBLE);
        clearhistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnBaiduMapClear();
                //  clearhistory.setEnabled(false);
                // clearhistory.setVisibility(View.INVISIBLE);
                setOutputText(mSegmentInfo);
            }
        });
        final ImageView locate = (ImageView) findViewById(R.id.locate);
        locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locData != null) {
                    LatLng latLng = new LatLng(locData.latitude, locData.longitude);
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.target(latLng).zoom(21.0f);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                } else {
                    Toast.makeText(MyActivity.this, "正在定位中，请等待！", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initMap() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mMapView.setLogoPosition(LogoPosition.logoPostionleftBottom);

        ViewTreeObserver viewTreeObserver = linearLayoutleft.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heoght = mMapView.getScaleControlViewHeight() * 2 + 30;
                mMapView.setPadding(0, 0, 2, linearLayoutleft.getHeight() + heoght);
            }
        });


        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        mLocationClient = new LocationClient(this);
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(0);

        option.isNeedPoiRegion = true;
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
        S_map = mMapView.getMapLevel();
        mLocationClient.start();

        lgpsCode = new LgpsCode(mBaiduMap, MyActivity.this);
    }

    private void OnModeWorking() {
        if (main_button.getText().equals("模式")) {
            LayoutInflater inflater_short = (LayoutInflater) MyActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final LinearLayout popWindow = (LinearLayout) inflater_short.inflate(R.layout.modes, (ViewGroup) findViewById(R.id.frame));
            Button locate = (Button) popWindow.findViewById(R.id.locating);
            locate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    initSegment(0);
                    lgpsServer.setWorkingMode(LgpsApp.LgpsMode.LOCATING_MODE);
                    myapp.setNOW_MODE(LgpsApp.LgpsMode.LOCATING_MODE);
                    modeWindow.dismiss();
                    main_button.setText("定位模式正在运行中");
                    main_button.setBackgroundColor(getResources().getColor(R.color.qq_color));
                }
            });
            Button navagate = (Button) popWindow.findViewById(R.id.navagate);
            navagate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    initSegment(0);
                    lgpsServer.setWorkingMode(LgpsApp.LgpsMode.NAVAGATING_MODE);
                    myapp.setNOW_MODE(LgpsApp.LgpsMode.NAVAGATING_MODE);
                    modeWindow.dismiss();
                    main_button.setText("导航模式正在运行中");
                    main_button.setBackgroundColor(getResources().getColor(R.color.qq_color));
                }
            });
            Button sentry = (Button) popWindow.findViewById(R.id.sentry);
            sentry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initSegment(1);
                    lgpsServer.setWorkingMode(LgpsApp.LgpsMode.SENTRY_MODE);
                    myapp.setNOW_MODE(LgpsApp.LgpsMode.SENTRY_MODE);
                    Toast.makeText(MyActivity.this, "请点击选择监控点！", Toast.LENGTH_SHORT).show();
                    modeWindow.dismiss();
                    main_button.setText("哨兵模式正在运行中");
                    main_button.setBackgroundColor(getResources().getColor(R.color.qq_color));
                }
            });
            Button secure = (Button) popWindow.findViewById(R.id.secure);
            secure.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initSegment(0);
                    lgpsServer.setWorkingMode(LgpsApp.LgpsMode.SECURE_MODE);
                    myapp.setNOW_MODE(LgpsApp.LgpsMode.SECURE_MODE);
                    modeWindow.dismiss();
                    main_button.setText("安防模式正在运行中");
                    main_button.setBackgroundColor(getResources().getColor(R.color.qq_color));

                }
            });
            Button input = (Button) popWindow.findViewById(R.id.input);
            input.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    initSegment(2);
                    lgpsServer.setWorkingMode(LgpsApp.LgpsMode.INPUT_MODE);
                    myapp.setNOW_MODE(LgpsApp.LgpsMode.INPUT_MODE);
                    modeWindow.dismiss();
                    main_button.setText("输入模式正在运行中");
                    main_button.setBackgroundColor(getResources().getColor(R.color.qq_color));

                }
            });
            modeWindow = new PopupWindow(popWindow, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            modeWindow.setFocusable(true);
            modeWindow.setOutsideTouchable(true);
            modeWindow.update();
            modeWindow.setBackgroundDrawable(getResources().getDrawable(R.color.touming));

            modeWindow.showAtLocation(MyActivity.this.findViewById(R.id.main_button), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, main_button.getHeight() + 5);
        } else {
            String[] aa = ((String) main_button.getText()).split("正在运行中");

            final AlertDialog.Builder cancel = new AlertDialog.Builder(MyActivity.this);
            cancel.setIcon(android.R.drawable.ic_dialog_alert);
            cancel.setTitle("确定要退出" + aa[0] + "吗？");
            cancel.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // lgpsNet.SetQuit();
                    myapp.setNOW_MODE(LgpsApp.LgpsMode.QUIT_MODE);
                    Log.i("大按钮", "退出了网络连接");
                    self_position.setText(" ");
                    if (lgpsNet != null) {
                        lgpsNet.SetQuit();
                        // lgpsNet = null;
                    }
                    lgpsServer.Toquit();

                    main_button.setText("模式");
                    main_button.setBackground(getResources().getDrawable(R.color.cyan));

                }
            });
            cancel.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            cancel.create().show();

        }
    }


    private void OnHistoryReview() {

        LayoutInflater inflater_long = (LayoutInflater) MyActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout popList = (LinearLayout) inflater_long.inflate(R.layout.data_list, null);
        ListView data_date = (ListView) popList.findViewById(R.id.data_date);
        final ListView data_time = (ListView) popList.findViewById(R.id.data_time);
        final DBHelper dbHelper = new DBHelper(MyActivity.this);
        dbHelper.open();
        final List<String> dates = dbHelper.ReadRowDates();
        dbHelper.close();
        ArrayAdapter<? extends String> adapter = new ArrayAdapter<>(MyActivity.this, R.layout.list_items_history, dates);
        data_date.setAdapter(adapter);
        PopupWindow history = new PopupWindow(popList, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        history.setFocusable(true);
        history.setOutsideTouchable(true);
        history.update();
        history.setBackgroundDrawable(getResources().getDrawable(R.color.touming));
        history.showAtLocation(MyActivity.this.findViewById(R.id.main_button), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, main_button.getHeight() + 5);
        data_date.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dbHelper.open();
                final List<String> times = dbHelper.ReadRowTime(dates.get(position));
                Log.i("数据库时间", String.valueOf(times));
                final int m = position;
                dbHelper.close();
                ArrayAdapter<? extends String> adapter1 = new ArrayAdapter<>(MyActivity.this, R.layout.list_items_history, times);
                data_time.setAdapter(adapter1);
                data_time.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        dbHelper.open();
                        List<RowRecord> rr = dbHelper.readRowsData(dates.get(m), times.get(position));
                        dbHelper.close();
                        String myseg = rr.get(0).getmSegNum();
                        lgpsServer.showHistory(rr);
                        clearhistory.setEnabled(true);
                     /*   clearhistory.setVisibility(View.VISIBLE);
                      //  LgpsCode.LRect mycoord= lgpsCode.parseCode(myseg);
                        LatLng llText = new LatLng(liy.getCentre().x,mycoord.getCentre().y);
                        OverlayOptions ooText = new TextOptions().bgColor(0xAAFFFF00)
                                .fontSize(24).fontColor(0xFFFF00FF).text(myseg+"您当时在此地").rotate(0)
                                .position(llText);
                        mBaiduMap.addOverlay(ooText);
                        Toast.makeText(MyActivity.this, llText+"您当时在此地", Toast.LENGTH_SHORT).show();*/

                        history.dismiss();

                    }
                });

            }
        });

    }

    private void setOutputText(SegInfo segInfo) {
        segment_type.setText(segInfo.getS_type());
        segbase_info.setText(segInfo.getS_Addr());
        net_status.setText(segInfo.getRouterIp());
    /*    segbase_info.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        segbase_info.setSingleLine(true);
        segbase_info.setMarqueeRepeatLimit(6);*/
    }

    private void OnBaiduMapClear() {
        mBaiduMap.clear();

    }

    private void changebKGColor() {
        int CYAN = 0xff80ffff;
        int TOU = 0x008080FF;
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(this, "backgroundColor", CYAN);
        colorAnim.setTarget(linearLayoutleft);
        colorAnim.setIntValues(CYAN, TOU);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setRepeatCount(1);
        colorAnim.setRepeatMode(ValueAnimator.INFINITE);
        colorAnim.setDuration(200);
        colorAnim.start();
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void AniRotateImage(float fDegress) {
        float DegressQuondam = 0.0f;
        RotateAnimation myAni = new RotateAnimation(DegressQuondam, fDegress,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        myAni.setDuration(300);
        myAni.setFillAfter(true);

        plate.startAnimation(myAni);
        DegressQuondam = fDegress;
    }

    private void initActionBar() {
        // 自定义标题栏
        if (getActionBar() != null) {
            getActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.touming));
            int actionBarHeight = 40;
            TypedValue tv = new TypedValue();
            if (this.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, this.getResources().getDisplayMetrics());
                Log.i("ACTIONBAR高度****：", String.valueOf(actionBarHeight));
            }
            final LinearLayout ll = (LinearLayout) findViewById(R.id.space);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ll.getLayoutParams();
            lp.height = getStatusBarHeight() + actionBarHeight + 10;
            ll.setLayoutParams(lp);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mSegmentInfo = new SegInfo();
        if (requestCode == 15) {
            if (resultCode == 1) {
                String netad = data.getStringExtra("IP");
                String aa[] = netad.split("\\.");
                int m = 0;
                if (aa.length == 4) {
                    for (int i = 0; i <= 3; i++) {
                        if (Integer.parseInt(aa[i]) <= 255 && Integer.parseInt(aa[i]) >= 0) {
                            m++;
                        }
                    }
                }
                if (m == 4) {
                    myapp.setNETIP(data.getStringExtra("IP"));
                    mSegmentInfo.setRouterIp(data.getStringExtra("IP"));

                } else {
                    Toast.makeText(MyActivity.this, "您输入的地址信息有误，请重新输入", Toast.LENGTH_SHORT).show();
                }

                mSegmentInfo.setSegNumber("4878487");
                // initNet(mSegmentInfo);
                setOutputText(mSegmentInfo);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.lgpsactions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuitem) {
        switch (menuitem.getItemId()) {
            case R.id.lgps_search:
                Intent intent = new Intent(MyActivity.this, LgpsIntruduction.class);
                startActivity(intent);
                break;
            case R.id.action_setting:
                Intent setting = new Intent(MyActivity.this, SettingPage.class);
                startActivityForResult(setting, 15);
                break;
            case R.id.action_quit:
                this.stopService(new Intent(this, StepService.class));
                if (lgpsNet != null) {
                    lgpsNet.SetQuit();
                    lgpsServer.Toquit();
                    lgpsNet = null;
                }

                this.finish();
                break;
        }
        return false;
    }

    private void initSegment(int mode) {
        if (!mSegmentInfo.isAvailable()) {
            Toast.makeText(MyActivity.this, "段编号没有能通过定位获得，请输入段编号或者网络地址！", Toast.LENGTH_LONG).show();

            Intent setting = new Intent(MyActivity.this, SettingPage.class);
            setting.putExtra("MODE", mode);
            startActivityForResult(setting, 15);
        } else {
            liy = new LatLng(mSegmentInfo.getLatLT(), mSegmentInfo.getLngLT());

            initNet(mSegmentInfo);
        }
    }

    private void initNet(SegInfo segInfo) {

        if (segInfo.isAvailable()) {
            if (lgpsNet == null) {
                try {
                    lgpsNet = new LgpsNet("56", segInfo.getRouterIp(), handler);
                    lgpsNet.start();
                    Log.i("网络", "新建调用");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                lgpsNet = null;
                try {
                    Log.i("网络", "复用调用");
                    lgpsNet = new LgpsNet(segInfo.getSegNumber(), segInfo.getRouterIp(), handler);
                    lgpsNet.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void onPause() {
        mMapView.onPause();
        //  lgpsNet = null;
        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
        this.startService(new Intent(this, StepService.class));
        mMapView.onResume();

    }

    @Override
    public void onStop() {
        mMapView.onPause();
        this.stopService(new Intent(this, StepService.class));
        mLocationClient.unRegisterLocationListener(myListener);
        mLocationClient.stop();
        lgpsNet = null;
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        lgpsServer.Toquit();
        this.stopService(new Intent(this, StepService.class));
        mLocationClient.unRegisterLocationListener(myListener);
        mLocationClient.stop();

        lgpsNet = null;
        mMapView.onDestroy();
        super.onDestroy();
    }

    private String setdirection(int direction) {
        if (direction > 0 || direction == 0) {
            switch (direction) {
                case 0:
                    return "北";
                case 1:
                case 2:
                    return "东北";
                case 3:
                case 4:
                    return "东";
                case 5:
                case 6:
                    return "东南";
                case 7:
                    return "南";
            }
        } else if (direction < 0) {
            switch (direction) {

                case -1:
                case -2:
                    return "西北";
                case -3:
                case -4:
                    return "西";
                case -5:
                case -6:
                    return "西南";
                case -7:
                    return "南";
            }
        }
        return null;
    }


    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            Toast.makeText(MyActivity.this, location.getAddress().address, Toast.LENGTH_SHORT).show();
            // Log.i("****本人精度纬度***", location.getAddress().address + location.getCity());
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(21.0f);
                // Toast.makeText(MyActivity.this, "已确定位置", Toast.LENGTH_SHORT).show();
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                // parseSegNumber(ll);
                Log.i("本人精度纬度*/*/*/*", ll.toString() + location.getAddress().address);
                //mSegmentInfo = lgpsServer.getSegInfo(ll);
            }
            liy = ll;
            lgpsServer.setlocalLATLNG(liy);
            mSegmentInfo.setS_type("该区域地面没有查询到段");
            mSegmentInfo.setS_Addr(location.getAddress().address);
            setOutputText(mSegmentInfo);
        }
    }
}
