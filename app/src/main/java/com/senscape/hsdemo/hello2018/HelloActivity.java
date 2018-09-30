package com.senscape.hsdemo.hello2018;


import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.senscape.hsdemo.R;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class HelloActivity extends Activity {

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
        UsbDevice usbDevice = getIntent().getParcelableExtra("usbdevice");
        mHello2018Thread = new Hello2018Thread(this, usbDevice, mHandler);
        mHello2018Thread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHello2018Thread != null) {
            mHello2018Thread.closeDevice();
        }
    }
}
