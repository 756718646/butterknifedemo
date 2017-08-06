package com.yongzheng.butterknifedemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.example.BindView;

/**
 * 测试baseActivity
 */
public class TestBaseActivity extends BaseActivity {

    //我的自定义注解
    @BindView(R.id.text)TextView text;

    @Override
    public int getLayoutId() {
        return R.layout.activity_test_base;
    }

    @Override
    public void initView() {
        text.setText("TestBaseActivity");
    }
}
