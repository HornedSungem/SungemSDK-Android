package com.senscape.hsdemo.multiple;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.Toast;

import com.hornedSungem.library.model.HornedSungemFrame;
import com.senscape.hsdemo.DrawView;
import com.senscape.hsdemo.R;

import java.util.ArrayList;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class MultipleActivity extends Activity {
    private DrawView mDrawView;
    private ImageView mImageView;
    private HsMultipleThread mHsMultipleThread;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 0) {
                HornedSungemFrame frame = (HornedSungemFrame) msg.obj;
                if (frame != null) {
                    mImageView.setImageBitmap(frame.getBitmap());
                    ArrayList<HornedSungemFrame.ObjectInfo> objectInfos = frame.getObjectInfos();
                    if (objectInfos != null && objectInfos.size() > 0) {
                        mDrawView.update(objectInfos);
                    } else {
                        mDrawView.removeRect();
                    }
                }
            } else if (msg.arg1 == 1) {
                Toast.makeText(MultipleActivity.this, "初始化失败,errorCode=" + msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple);
        mImageView = findViewById(R.id.img_frame);
        mDrawView = findViewById(R.id.draw_view);
        UsbDevice usbDevice = getIntent().getParcelableExtra("usbdevice");
        if (usbDevice != null) {
            mHsMultipleThread = new HsMultipleThread(MultipleActivity.this, usbDevice, mHandler);
            mHsMultipleThread.start();
        } else {
            Toast.makeText(this, "请返回主页重新插拔角蜂鸟允许权限", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHsMultipleThread != null) {
            mHsMultipleThread.closeDevice();
        }
    }
}
