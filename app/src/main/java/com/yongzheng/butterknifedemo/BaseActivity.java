package com.yongzheng.butterknifedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.Unbinder;

/**
 * 基类Activity封装测试
 * Created by yongzheng on 17-8-5.
 */
public abstract class BaseActivity extends AppCompatActivity {

    public Unbinder unbinder;

    public abstract int getLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        unbinder = ButterKnife.bind(this);
        initView();
    }

    public abstract void initView();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
