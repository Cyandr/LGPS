package com.example.myapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

/**
 * Created by cyandr on 2016/7/4 0004.
 */
public class SettingPage extends Activity {
    Intent intent;
    MyGridView gridView;
    ScrollView scrollView;
    LgpsApp myapp;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        final EditText segNum = (EditText) findViewById(R.id.seg_input);
        myapp = (LgpsApp) getApplication();
        setkeyBoard();
        final EditText segAddress = (EditText) findViewById(R.id.segmentinfo_input);
        String in=myapp.getNETIP();
        if (in!=null) segAddress.setText(in);
        intent = new Intent();
        Button confirm = (Button) findViewById(R.id.confirm_setting);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String address = segAddress.getText().toString();
                String num = segNum.getText().toString();
                String scale = String.valueOf(10);
                boolean input_o = true;

                Log.i("输入信息为", address);

                intent.putExtra("IP", address);
                try {


                } catch (NumberFormatException e) {
                    input_o = false;
                    Toast.makeText(SettingPage.this, "您输入的端口信息有误，请检查！", Toast.LENGTH_SHORT).show();
                }
                try {

                    intent.putExtra("SEGNUM", num);
                } catch (StringIndexOutOfBoundsException e) {
                    input_o = false;
                    Toast.makeText(SettingPage.this, "您输入的段编号信息有误，请检查！", Toast.LENGTH_SHORT).show();
                }

                if (input_o) {
                    SettingPage.this.setResult(1, intent);
                    SettingPage.this.finish();
                }
            }

        });
        Button cancel = (Button) findViewById(R.id.cancel_setting);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("IP", "192.168.0.100");
                intent.putExtra("SEGNUM", 0);
                intent.putExtra("PORTAL", 0);
                intent.putExtra("SCALE", 10);
                SettingPage.this.setResult(1, intent);
                finish();
            }
        });
        CheckBox importcheck = (CheckBox) findViewById(R.id.import_yes);
        importcheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                importDB(isChecked);
            }
        });
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        initActionBar();

    }

    private void setkeyBoard() {
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        gridView = (MyGridView) findViewById(R.id.gridView);
       /* scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                findViewById(R.id.gridView).getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
        });
        gridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });*/
        Intent intent = getIntent();
        int woeringmode = myapp.getNOW_MODE();
        GridAdapter gridAdapter = new GridAdapter(SettingPage.this, woeringmode);
        gridView.setAdapter(gridAdapter);


    }

    private void importDB(boolean yes) {
        if (yes) {
            DBHelper dbHelper = new DBHelper(this);
            dbHelper.importDB();
            Toast.makeText(this, "导出完毕！", Toast.LENGTH_SHORT).show();
            dbHelper = null;
        }
    }

    private void initActionBar() {
// 自定义标题栏
       /* getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.touming));
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowCustomEnabled(true);
        final LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mTitleView = mInflater.inflate(R.layout.custom_action_bar_layout, null);
        getActionBar().setCustomView(mTitleView, new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));*/
        if (getActionBar() != null)
            getActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.touming));
        int actionBarHeight = 40;
        TypedValue tv = new TypedValue();
        if (this.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, this.getResources().getDisplayMetrics());
            Log.i("ACTIONBAR高度****：", String.valueOf(actionBarHeight));
        }

        final LinearLayout ll = (LinearLayout) findViewById(R.id.space3);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ll.getLayoutParams();
        lp.height = getStatusBarHeight() + actionBarHeight + 10;
        ll.setLayoutParams(lp);
      /*  ViewTreeObserver vto = ll.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

            }
        });*/

    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}