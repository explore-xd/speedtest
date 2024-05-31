package com.athena.speedtest.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.athena.speedtest.R;
import com.athena.speedtest.core.serverSelector.TestPoint;

/**
 * @Author :DSS
 * @Date :2024/5/31 15:34
 * @description NodeAdapter.java
 */
public class NodeAdapter extends BaseAdapter {
    private TestPoint[] testPoints;
    private Context context;
    private ViewHolder mViewHolder;

    public NodeAdapter(Context context, TestPoint[] testPoints) {
        this.context = context;
        this.testPoints = testPoints;
    }

    //item的总长度
    @Override
    public int getCount() {
        return testPoints.length;
    }

    //获取item的标识
    @Override
    public Object getItem(int position) {
        return testPoints[position];
    }

    //获取item的id
    @Override
    public long getItemId(int position) {
        return position;
    }

    //获取item视图
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //判断是否有可复用的view对象，没有的话走if，有的话走else
        if (convertView == null) {
            //找到我们自定义的行布局
            convertView = View.inflate(context, R.layout.layout_test_node_item, null);
            //实例化ViewHolder内部类
            mViewHolder = new ViewHolder();
            //给ViewHolder里的控件初始化，通过我们自定义的行布局
            mViewHolder.name = convertView.findViewById(R.id.name);
            //给convertView设置一个标签
            convertView.setTag(mViewHolder);
        } else {
            //获取我们设置过的标签，实现复用convertView
            mViewHolder = (ViewHolder) convertView.getTag();
        }
        mViewHolder.name.setText(testPoints[position].getName());
        //返回convertView对象
        return convertView;
    }

    //新建ViewHolder内部类，用来定义我们行布局中所用到的控件
    class ViewHolder {
        private TextView name;
    }
}
