package com.example.myapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.w3c.dom.Text;

/**
 * Created by cyandr on 2016/8/23 0023.
 */
public class GridAdapter extends BaseAdapter {
    Context mContext;
    LayoutInflater mInflator;
    int MworkingMode;
    int SENTRY_MODE = 1;
    int KEYBOARD_MODE = 2;
    String[] KEY_VALUES = new String[64];
    DBHelper db;

    GridAdapter(Context context, int workingMode) {
        mContext = context;
        MworkingMode = workingMode;
        db = new DBHelper(mContext);
        db.open();
        String m;
        for (int i = 1; i <= 64; i++) {
            m = db.readKey(i);
            if (m != null)
                KEY_VALUES[i - 1] = m;
        }
        db.close();
    }

    @Override
    public int getCount() {
        return 64;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();
        if (convertView == null) {
            mInflator = LayoutInflater.from(mContext);
            convertView = mInflator.inflate(R.layout.grid_item, null);
            viewHolder.sentrypoint = (TextView) convertView.findViewById(R.id.sentry_pot);
            viewHolder.keyvalue = (TextView) convertView.findViewById(R.id.key_value);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final ViewHolder finalViewHolder = viewHolder;
        finalViewHolder.keyvalue.setText(KEY_VALUES[position]);
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MworkingMode == LgpsApp.LgpsMode.SENTRY_MODE) {
                    finalViewHolder.sentrypoint.setText("监控");
                    db.open();
                    db.updatesentry(String.valueOf(1+position/8)+String.valueOf(1+position%8));
                    db.close();
                    finalViewHolder.sentrypoint.setBackground(mContext.getResources().getDrawable(R.color.lcyan));
                } else if (MworkingMode == LgpsApp.LgpsMode.INPUT_MODE) {

                    final EditText inputServer = new EditText(mContext);
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("自定义键值").setMessage("请输入您希望该位置输出的字符值：").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                            .setNegativeButton("取消", null);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            String m = inputServer.getText().toString();
                            db.open();
                            db.updateKey(position+1, m);
                            db.close();
                            Toast.makeText(mContext, "您输入的是：" + m + "!", Toast.LENGTH_SHORT).show();
                            finalViewHolder.keyvalue.setText(m);
                        }
                    });
                    builder.show();
                }
            }
        });
        return convertView;
    }

    private class ViewHolder {
        TextView sentrypoint;
        TextView keyvalue;
    }
}
