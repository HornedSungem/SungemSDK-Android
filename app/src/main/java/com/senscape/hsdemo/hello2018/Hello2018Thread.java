package com.senscape.hsdemo.hello2018;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.ConnectStatus;
import com.hornedSungem.library.thread.HsBaseThread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */

public class Hello2018Thread extends HsBaseThread {

    private Handler mHandler;
    private Activity mActivity;
    public Hello2018Thread(Activity activity, ConnectBridge connectBridge, Handler handler) {
        super( connectBridge,true);
        mActivity=activity;
        mHandler=handler;
    }

    @Override
    public void run() {
        super.run();
        int status = allocateGraphByAssets(mActivity,"graph_mnist");
        if (status != ConnectStatus.HS_OK) {
            Message message = mHandler.obtainMessage();
            message.arg1 = 1;
            message.obj = status;
            mHandler.sendMessage(message);
            return;
        }
        try {
            for (int i = 1; i < 5; i++) {
                int[] ints = new int[28 * 28];
                try {
                    InputStream inputStream = mActivity.getAssets().open("hello/" + i + ".jpg");
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    bitmap.getPixels(ints, 0, 28, 0, 0, 28, 28);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                float[] float_tensor = new float[28 * 28];
                for (int j = 0; j < 28 * 28; j++) {
                    float_tensor[j] = Color.red(ints[j]) * 0.007843f - 1;
                }
                int status_load = loadTensor(float_tensor, float_tensor.length, 0);
                if (status_load == ConnectStatus.HS_OK) {
                    float[] result = getResult(0);
                    if (result != null) {
                        int max = getMaxPossible(result);
                        mHandler.sendEmptyMessage(max);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public int getMaxPossible(float[] arr) {
        int max = 0;
        float max_f = 0;
        for (int i = 0; i < arr.length; i++) {
            float temp = arr[i];
            if (temp > max_f) {
                max = i;
                max_f = temp;
            }
        }
        return max;
    }
}
