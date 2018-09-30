package com.senscape.hsdemo.sketchGuess;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.senscape.hsdemo.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class SketchGuessActivity extends Activity {

    private ImageView mImg_frame;
    private ImageView mImg_tensor;
    private ImageView mImgBorder;
    private TextView mTvResult1;
    private TextView mTvResult2;
    private TextView mTvResult3;
    private TextView mTvResult4;
    private TextView mTvResult5;
    private TextView mTvTarget;
    private int index = 1;
    private SketchGuessThread mSketchGuessThread;
    private UsbDevice mUsbDevice;
    private String[] mObjectNames = new String[345];
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 0) {
                mImgBorder.setVisibility(View.VISIBLE);
                Bitmap bitmap = (Bitmap) msg.obj;
                if (bitmap != null)
                    mImg_frame.setImageBitmap(bitmap);
            }
            if (msg.arg1 == 2) {
                Bitmap bitmap = (Bitmap) msg.obj;
                if (bitmap != null)
                    mImg_tensor.setImageBitmap(bitmap);
            }
            if (msg.arg1 == 3) {
                String[] result = (String[]) msg.obj;
                mTvResult1.setText(result[0]);
                mTvResult2.setText(result[1]);
                mTvResult3.setText(result[2]);
                mTvResult4.setText(result[3]);
                mTvResult5.setText(result[4]);
                for (String str : result) {
                    String[] strings = str.split(" ");
                    if (mObjectNames[index].equals(strings[0])) {
                        Toast.makeText(SketchGuessActivity.this, ("恭喜命题绘画 " + mObjectNames[index] + " 过关！！！"), Toast.LENGTH_SHORT).show();
                        index++;
                        mTvTarget.setText("命题：" + mObjectNames[index]);
                    }
                }
            }
            if (msg.arg1 == 100) {
                if (mUsbDevice != null) {
                    mSketchGuessThread = new SketchGuessThread(SketchGuessActivity.this, mUsbDevice, mHandler, mObjectNames);
                    mSketchGuessThread.start();
                }
                mTvTarget.setText("命题：" + mObjectNames[index]);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch_guess);
        initView();
        mUsbDevice = getIntent().getParcelableExtra("usbdevice");

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory().getAbsolutePath() + "/hs/class_list_chn.txt"));
                    for (int i = 0; i < 345; i++) {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            String[] strings = line.split(" ");
                            mObjectNames[i] = strings[0];
                        }
                    }
                    bufferedReader.close();
                    Message message = new Message();
                    message.arg1 = 100;
                    mHandler.sendMessage(message);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e("SketchGuessThread", "FileNotFoundException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("SketchGuessThread", "IOException");
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSketchGuessThread != null) mSketchGuessThread.closeDevice();
    }

    public void initView() {
        mImg_frame = findViewById(R.id.img_frame_sg);
        mImg_tensor = findViewById(R.id.img_tensor_sg);
        mImgBorder = findViewById(R.id.img_border_sg);
        mTvResult1 = findViewById(R.id.tv_result1_sg);
        mTvResult2 = findViewById(R.id.tv_result2_sg);
        mTvResult3 = findViewById(R.id.tv_result3_sg);
        mTvResult4 = findViewById(R.id.tv_result4_sg);
        mTvResult5 = findViewById(R.id.tv_result5_sg);
        mTvTarget = findViewById(R.id.tv_garget);


    }

}
