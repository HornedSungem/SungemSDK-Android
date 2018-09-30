package com.senscape.hsdemo.objectDetector;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hornedSungem.library.model.HornedSungemFrame;
import com.hornedSungem.library.thread.HsBaseThread;
import com.senscape.hsdemo.DrawView;
import com.senscape.hsdemo.R;

import java.util.ArrayList;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class ObjectDetectorActivity extends Activity {

    private HsBaseThread mHsThread;
    private TextView mTvTip;
    private DrawView mDrawView;
    private ImageView mImageView;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 0) {
                HornedSungemFrame frame = (HornedSungemFrame) msg.obj;
                mImageView.setImageBitmap(frame.getBitmap());
                ArrayList<HornedSungemFrame.ObjectInfo> objectInfos = frame.getObjectInfos();
                if (objectInfos != null && objectInfos.size() > 0) {
                    mDrawView.update(objectInfos);
                } else {
                    mDrawView.removeRect();
                }
            } else if (msg.arg1 == 1) {
                //初始化失败
                Toast.makeText(ObjectDetectorActivity.this, "初始化失败,errorCode=" + msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_demo);
        mImageView = findViewById(R.id.img_frame);
        mDrawView = findViewById(R.id.draw_view);
        mTvTip = findViewById(R.id.tv_tip);
        UsbDevice usbDevice = getIntent().getParcelableExtra("usbdevice");
        if (usbDevice != null) {
            mTvTip.setVisibility(View.GONE);
            mHsThread = new ObjectDetectorThread(ObjectDetectorActivity.this, usbDevice, mHandler);
            mHsThread.start();
        } else {
            mTvTip.setText("请返回主页重新插拔角蜂鸟允许权限");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHsThread != null) {
            mHsThread.closeDevice();
        }
    }


}
