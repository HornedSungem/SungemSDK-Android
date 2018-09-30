package com.senscape.hsdemo.objectDetector;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.thread.HsBaseThread;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */

public class ObjectDetectorInitThread extends HsBaseThread {
    private Handler mHandler;
    private Activity mActivity;

    public ObjectDetectorInitThread(Activity activity, UsbDevice usbDevice, Handler handler) {
        super(activity, usbDevice, true);
        mActivity = activity;
        mHandler = handler;
    }

    @Override
    public void run() {
        super.run();
        int status = openDevice();
        allocateGraphByAssets(mActivity, "graph_object_SSD");
        Message message = mHandler.obtainMessage();
        message.arg1 = 1;
        message.obj = status;
        mHandler.sendMessage(message);

    }
}
