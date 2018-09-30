package com.senscape.hsdemo;

import android.app.ListActivity;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.hornedSungem.library.HsDevice;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class MainActivity extends ListActivity {

    private String mTypes[] = {"Hello 2018", "人脸检测", "物体识别",
            "手绘识别", "多模型"};
    private CheckBox mCheckBox;
    private boolean isSelfCamera = false;
    private HsDevice mHsDevice;
    private UsbDevice mUsbDevice;
    private CheckBox mCbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCheckBox = findViewById(R.id.cb_type);
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mCheckBox.setText("使用设备摄像头");
                    isSelfCamera = true;
                } else {
                    mCheckBox.setText("使用角蜂鸟摄像头");
                    isSelfCamera = false;
                }
            }
        });
        mCbDevice = findViewById(R.id.cv_device);
        mHsDevice = new HsDevice(this) {
            @Override
            public void openSucceed(UsbDevice usbDevice) {
                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                mUsbDevice = usbDevice;
                mCbDevice.setChecked(true);
            }

            @Override
            public void openFailed() {
                mCbDevice.setChecked(false);
                Toast.makeText(MainActivity.this, "请重新插拔角蜂鸟允许权限", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void disConnected() {
                mCbDevice.setChecked(false);
                Toast.makeText(MainActivity.this, "断开连接", Toast.LENGTH_SHORT).show();
            }
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.activities_list_text_view, mTypes);
        setListAdapter(adapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, AboutActivity.class);
        intent.putExtra("ABOUT_TEXT_TITLE", mTypes[position]);
        intent.putExtra("usbdevice", mUsbDevice);
        switch (position) {
            case 0:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "hello2018.HelloActivity");
                intent.putExtra("ABOUT_TEXT", "hello/about.html");
                break;
            case 1:
                if (isSelfCamera)
                    intent.putExtra("ACTIVITY_TO_LAUNCH",
                            "faceDetector.FaceDetectorBySelfActivity");
                else
                    intent.putExtra("ACTIVITY_TO_LAUNCH",
                            "faceDetector.FaceDetectorActivity");

                intent.putExtra("ABOUT_TEXT", "face/about.html");
                break;
            case 2:
                if (isSelfCamera)
                    intent.putExtra("ACTIVITY_TO_LAUNCH",
                            "objectDetector.ObjectDetectorBySelfActivity");
                else
                    intent.putExtra("ACTIVITY_TO_LAUNCH",
                            "objectDetector.ObjectDetectorActivity");
                intent.putExtra("ABOUT_TEXT", "object/about.html");
                break;
            case 3:
                if (isSelfCamera)
                    intent.putExtra("ACTIVITY_TO_LAUNCH",
                            "sketchGuess.SGbySelfctivity");
                else
                    intent.putExtra("ACTIVITY_TO_LAUNCH",
                            "sketchGuess.SketchGuessActivity");
                intent.putExtra("ABOUT_TEXT", "sketchguess/about.html");
                break;
            case 4:
                if (isSelfCamera) intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "multiple.MultipleSelfActivity");
                else intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "multiple.MultipleActivity");
                intent.putExtra("ABOUT_TEXT", "multiple/about.html");
        }

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHsDevice.onDestroy();
    }
}
