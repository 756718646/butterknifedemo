package com.yongzheng.butterknifedemo;
//com.yongzheng.butterknifedemo
//com.yongzheng.butterknifedemo
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.BindView;
import com.example.Unbinder;

public class MainActivity extends AppCompatActivity {

    //我的自定义注解
    @BindView(R.id.text)TextView text;

    private Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        text.setText("bind is ok");
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toTestBase();
            }
        });
    }

    private void toTestBase() {
        startActivity(new Intent(this,TestBaseActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
