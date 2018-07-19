package com.senscape.hsdemo.objectDetector;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.util.Log;

import com.hornedSungem.library.ConnectStatus;
import com.hornedSungem.library.model.HornedSungemFrame;
import com.senscape.hsdemo.DrawView;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

/**
 * Created by looper.lu on 18/6/20.
 */

public class ODFrameTask extends AsyncTask<Allocation, Integer, HornedSungemFrame> {
    String[] labels = {"aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    ObjectDetectorInitThread mObjectDetectorInitThread;
    private DrawView mDrawView;

    public ODFrameTask(ObjectDetectorInitThread objectDetectorInitThread, DrawView drawView) {
        mObjectDetectorInitThread = objectDetectorInitThread;
        mDrawView = drawView;
    }
    @Override
    protected HornedSungemFrame doInBackground(Allocation... allocations) {
        try {
            SoftReference<Bitmap> softRef = new SoftReference<>(Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888));
            Bitmap bitmap = softRef.get();
            allocations[0].copyTo(bitmap);
            Matrix matrix = new Matrix();
            matrix.postScale(300f / 1280, 300f / 720);
            // 得到新的图片
            Bitmap newbm = Bitmap.createBitmap(bitmap, 0, 0, 1280, 720, matrix,
                    true);
            int[] ints = new int[300 * 300];
            newbm.getPixels(ints, 0, 300, 0, 0, 300, 300);
            float[] float_tensor = new float[300 * 300 * 3];
            for (int j = 0; j < 300 * 300; j++) {
                float_tensor[j * 3] = Color.red(ints[j]) * 0.007843f - 1;
                float_tensor[j * 3 + 1] = Color.green(ints[j]) * 0.007843f - 1;
                float_tensor[j * 3 + 2] = Color.blue(ints[j]) * 0.007843f - 1;
            }
            int status_load = mObjectDetectorInitThread.loadTensor(float_tensor, float_tensor.length, 1);
            if (status_load == ConnectStatus.HS_OK) {
                float[] result = mObjectDetectorInitThread.getResult(0);
                if (result != null)
                    return getFrameResult(result);
            }
        } catch (Exception e) {
            Log.e("FDFrameTask", "The device may have been disconnected");
        }
        return null;
    }

    @Override
    protected void onPostExecute(HornedSungemFrame hornedSungemFrame) {
        super.onPostExecute(hornedSungemFrame);
        if (hornedSungemFrame != null) {
            ArrayList<HornedSungemFrame.ObjectInfo> objectInfos = hornedSungemFrame.getObjectInfos();
            if (objectInfos != null && objectInfos.size() > 0) {
                mDrawView.update(objectInfos);
            } else {
                mDrawView.removeRect();
            }
        }
    }
    public HornedSungemFrame getFrameResult(float[] floats) {
        //检测结果处理
        int num = (int) floats[0];
        ArrayList<HornedSungemFrame.ObjectInfo> objectInfos = new ArrayList<>();
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                HornedSungemFrame.ObjectInfo objectInfo = new HornedSungemFrame.ObjectInfo();
                int type = (int) (floats[7 * (i + 1) + 1]);
                int x1 = (int) (floats[7 * (i + 1) + 3] * 1280);
                int y1 = (int) (floats[7 * (i + 1) + 4] * 720);
                int x2 = (int) (floats[7 * (i + 1) + 5] * 1280);
                int y2 = (int) (floats[7 * (i + 1) + 6] * 720);
                int wight = x2 - x1;
                int height = y2 - y1;
                int percentage = (int) (floats[7 * (i + 1) + 2] * 100);
                if (type == 0) {
                    continue;
                }
                if (percentage <= 55) {
                    continue;
                }
                if (wight >= 1280 * 0.8 || height >= 720 * 0.8) {
                    continue;
                }
                //如果有一个值为小于0，这组数据干掉不要
                if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0 || wight < 0 || height < 0) {
                    continue;
                }
                objectInfo.setType(labels[type - 1]);
                objectInfo.setRect(new Rect(x1, y1, x2, y2));//检测到的人脸矩形
                objectInfo.setScore(percentage);
                objectInfos.add(objectInfo);
            }
        }
        return new HornedSungemFrame(null, objectInfos, num);
    }

}
