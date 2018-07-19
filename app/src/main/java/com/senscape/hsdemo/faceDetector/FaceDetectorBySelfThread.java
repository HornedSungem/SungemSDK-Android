package com.senscape.hsdemo.faceDetector;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.thread.HsBaseThread;

/**
 * Created by looper.lu on 18/6/20.
 */

public class FaceDetectorBySelfThread extends HsBaseThread {

    private Handler mHandler;
    private Activity mActivity;

    public FaceDetectorBySelfThread(Activity activity, ConnectBridge connectBridge, Handler handler) {
        super(connectBridge,true);
        mActivity = activity;
        mHandler = handler;
    }

    @Override
    public void run() {
        super.run();
        //graph灌到鸟蜂鸟里
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int status = allocateGraphByAssets(mActivity, "graph_face_SSD");
        Message message = mHandler.obtainMessage();
        message.arg1 = 1;
        message.obj = status;
        mHandler.sendMessage(message);

    }

}
