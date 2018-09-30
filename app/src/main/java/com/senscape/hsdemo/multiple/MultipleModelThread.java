package com.senscape.hsdemo.multiple;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.thread.HsBaseThread;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class MultipleModelThread extends HsBaseThread {
    private Context mContext;
    private Handler mHandler;
    private int graph_face_id;

    public int getGraph_face_id() {
        return graph_face_id;
    }

    public int getGraph_object_id() {
        return graph_object_id;
    }

    private int graph_object_id;

    public MultipleModelThread(Context context, UsbDevice usbDevice, Handler handler) {
        super(context, usbDevice, true);
        mHandler = handler;
        mContext = context;
    }

    @Override
    public void run() {
        super.run();
        int status = openDevice();
        graph_face_id = allocateGraphByAssets(mContext, "graph_face_SSD");
        graph_object_id = allocateGraphByAssets(mContext, "graph_object_SSD");
        if (graph_face_id < 0 || graph_object_id < 0) return;
        Message message = mHandler.obtainMessage();
        message.arg1 = 1;
        message.obj = status;
        mHandler.sendMessage(message);
    }
}
