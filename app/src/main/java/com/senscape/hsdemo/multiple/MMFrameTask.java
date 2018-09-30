package com.senscape.hsdemo.multiple;

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
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class MMFrameTask extends AsyncTask<Allocation, Integer, HornedSungemFrame> {
    private static final String TAG = MMFrameTask.class.getSimpleName();
    private DrawView mDrawView;
    private MultipleModelThread mMultipleModelThread;
    String[] labels = {"aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};

    public MMFrameTask(MultipleModelThread multipleModelThread, DrawView drawView) {
        mMultipleModelThread = multipleModelThread;
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
            int status = mMultipleModelThread.loadTensor(float_tensor, float_tensor.length, mMultipleModelThread.getGraph_face_id());
            if (status == ConnectStatus.HS_OK) {
                float[] face_result = mMultipleModelThread.getResult(mMultipleModelThread.getGraph_face_id());
                if (face_result != null) {
                    int status_object = mMultipleModelThread.loadTensor(float_tensor, float_tensor.length, mMultipleModelThread.getGraph_object_id());
                    if (status_object == ConnectStatus.HS_OK) {
                        float[] object_result = mMultipleModelThread.getResult(mMultipleModelThread.getGraph_object_id());
                        if (object_result != null)
                            return getFrameResult(face_result, object_result);
                        else Log.e(TAG, "Object graph getResult error");
                    } else Log.e(TAG, "Object Graph load error");

                } else Log.e(TAG, "Face graph getResult error");

            } else Log.e(TAG, "Face graph loadTensor error");
        } catch (Exception e) {
            Log.e("FDFrameTask", "The device may have been disconnected");
        }
        return null;
    }

    public HornedSungemFrame getFrameResult(float[] face_result, float[] object_result) {
        //结果处理
        int num = (int) face_result[0];
        ArrayList<HornedSungemFrame.ObjectInfo> objectInfos = new ArrayList<>();
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                HornedSungemFrame.ObjectInfo objectInfo = new HornedSungemFrame.ObjectInfo();
                int x1 = (int) (face_result[7 * (i + 1) + 3] * 1280);
                int y1 = (int) (face_result[7 * (i + 1) + 4] * 720);
                int x2 = (int) (face_result[7 * (i + 1) + 5] * 1280);
                int y2 = (int) (face_result[7 * (i + 1) + 6] * 720);
                int wight = x2 - x1;
                int height = y2 - y1;
                int percentage = (int) (face_result[7 * (i + 1) + 2] * 100);
                if (percentage <= 55) {
                    continue;
                }
                if (wight >= 1280 * 0.8 || height >= 720 * 0.8) {
                    continue;
                }
                if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0 || wight < 0 || height < 0) {
                    continue;
                }
                objectInfo.setType("face");
                objectInfo.setRect(new Rect(x1, y1, x2, y2));
                objectInfo.setScore(percentage);
                objectInfos.add(objectInfo);
            }
        }
        int num_object = (int) object_result[0];
        if (num_object > 0) {
            for (int i = 0; i < num_object; i++) {
                HornedSungemFrame.ObjectInfo objectInfo = new HornedSungemFrame.ObjectInfo();
                int type = (int) (object_result[7 * (i + 1) + 1]);
                int x1 = (int) (object_result[7 * (i + 1) + 3] * 1280);
                int y1 = (int) (object_result[7 * (i + 1) + 4] * 720);
                int x2 = (int) (object_result[7 * (i + 1) + 5] * 1280);
                int y2 = (int) (object_result[7 * (i + 1) + 6] * 720);
                int wight = x2 - x1;
                int height = y2 - y1;
                int percentage = (int) (object_result[7 * (i + 1) + 2] * 100);
                if (type == 0) {
                    continue;
                }
                if (percentage <= 55) {
                    continue;
                }
                if (wight >= 1280 * 0.8 || height >= 720 * 0.8) {
                    continue;
                }
                if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0 || wight < 0 || height < 0) {
                    continue;
                }
                objectInfo.setType(labels[type - 1]);
                objectInfo.setRect(new Rect(x1, y1, x2, y2));
                objectInfo.setScore(percentage);
                objectInfos.add(objectInfo);
            }
        }
        return new HornedSungemFrame(null, objectInfos, num);
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
}
