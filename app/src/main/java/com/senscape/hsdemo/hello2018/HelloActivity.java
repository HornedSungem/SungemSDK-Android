package com.senscape.hsdemo.hello2018;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.HsBaseActivity;
import com.senscape.hsdemo.R;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class HelloActivity extends HsBaseActivity {

    private TextView mTextView;
    private Hello2018Thread mHello2018Thread;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mTextView.append("\r\n" + msg.what);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello);
        mTextView = findViewById(R.id.tv_hello);
    }

    @Override
    public void openSucceed(ConnectBridge connectBridge) {

        mHello2018Thread = new Hello2018Thread(this, connectBridge, mHandler);
        mHello2018Thread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void openFailed() {
    }

    @Override
    public void disConnected() {
        if (mHello2018Thread != null)
            mHello2018Thread.close();
    }
}
