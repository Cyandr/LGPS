package com.example.myapp;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * Created by cyandr on 2016/7/9 0009.
 */
public class LgpsIntruduction extends Activity {

    private String[] notice = {};

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction);
        String[] help = {getString(R.string.introduction_locatingmode),
                getString(R.string.introduction_navigatemode),
                getString(R.string.introduction_sentrymode),
                getString(R.string.introduction_securemode),
                getString(R.string.introduction_inputmode),
                getString(R.string.introduction_code),
                getString(R.string.introduction_notice1),
                getString(R.string.introduction_notice2),
                getString(R.string.on3)
        };
        ListView lshelp = (ListView) findViewById(R.id.help);
        ArrayAdapter<? extends String> adapter = new ArrayAdapter<String>(this, R.layout.list_items, help);
        lshelp.setAdapter(adapter);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        initActionBar();
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
        ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.touming));
        int actionBarHeight = 40;
        TypedValue tv = new TypedValue();
        if (this.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, this.getResources().getDisplayMetrics());
            Log.i("ACTIONBAR高度****：", String.valueOf(actionBarHeight));
        }

        final LinearLayout ll = (LinearLayout) findViewById(R.id.space2);
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