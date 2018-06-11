package com.senscape.hsdemo.objectDetector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.HsBaseActivity;
import com.hornedSungem.library.model.HornedSungemFrame;
import com.hornedSungem.library.thread.HsThread;
import com.senscape.hsdemo.DrawView;
import com.senscape.hsdemo.R;

import java.util.ArrayList;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class ObjectDetectorActivity extends HsBaseActivity {

    private HsThread mHsThread;
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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHsThread != null) {
            mHsThread.setRunning(false);
            mHsThread.close();
        }
    }

    @Override
    public void openSucceed(ConnectBridge connectBridge) {
        mTvTip.setVisibility(View.GONE);
        mHsThread = new ObjectDetectorThread(ObjectDetectorActivity.this, connectBridge, mHandler);
        mHsThread.setZoom(true);
        mHsThread.start();
    }

    @Override
    public void openFailed() {
        mTvTip.setText("请重新插拔角蜂鸟允许权限");
    }

    @Override
    public void disConnected() {
        Toast.makeText(this, "断开连接", Toast.LENGTH_SHORT).show();
        if (mHsThread != null) {
            mHsThread.setRunning(false);
            mHsThread.close();
        }
    }
}
