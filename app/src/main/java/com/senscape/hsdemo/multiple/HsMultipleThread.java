package com.senscape.hsdemo.multiple;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.ConnectStatus;
import com.hornedSungem.library.model.HornedSungemFrame;
import com.hornedSungem.library.thread.HsBaseThread;

import org.bytedeco.javacpp.opencv_core;

import java.util.ArrayList;

import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2RGBA;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class HsMultipleThread extends HsBaseThread {
    private Handler mHandler;
    private Context mContext;
    private int FRAME_W = 640;
    private int FRAME_H = 360;
    private float STD = 0.007843f;
    private float MEAN = -0.9999825f;
    String[] labels = {"aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};

    public HsMultipleThread(Context context, UsbDevice usbDevice, Handler handler) {
        super(context, usbDevice, true);
        mHandler = handler;
        mContext = context;
    }

    @Override
    public void run() {
        super.run();
        int status = openDevice();
        if (status != ConnectStatus.HS_OK) {
            Message message = mHandler.obtainMessage();
            message.arg1 = 1;
            message.obj = status;
            mHandler.sendMessage(message);
            return;
        }
        int face_id = allocateGraphByAssets(mContext, "graph_face_SSD");
        int object_id = allocateGraphByAssets(mContext, "graph_object_SSD");
        if (face_id < 0 || object_id < 0) {
            return;
        }
        while (true) {
            try {
                byte[] bytes = getImage(STD, MEAN, face_id);
                float[] result_face = getResult(face_id);
                if (bytes != null && result_face != null) {
                    opencv_core.IplImage bgrImage = null;
                    if (zoom) {
                        FRAME_W = 640;
                        FRAME_H = 360;
                        bgrImage = opencv_core.IplImage.create(FRAME_W, FRAME_H, opencv_core.IPL_DEPTH_8U, 3);
                        bgrImage.getByteBuffer().put(bytes);
                    } else {
                        FRAME_W = 1920;
                        FRAME_H = 1080;
                        byte[] bytes_rgb = new byte[FRAME_W * FRAME_H * 3];
                        for (int i = 0; i < FRAME_H * FRAME_W; i++) {
                            bytes_rgb[i * 3 + 2] = bytes[i];//r
                            bytes_rgb[i * 3 + 1] = bytes[FRAME_W * FRAME_H + i];//g
                            bytes_rgb[i * 3] = bytes[FRAME_W * FRAME_H * 2 + i];//b
                        }
                        bgrImage = opencv_core.IplImage.create(FRAME_W, FRAME_H, opencv_core.IPL_DEPTH_8U, 3);
                        bgrImage.getByteBuffer().put(bytes_rgb);
                    }
                    opencv_core.IplImage image = opencv_core.IplImage.create(FRAME_W, FRAME_H, opencv_core.IPL_DEPTH_8U, 4);
                    cvCvtColor(bgrImage, image, CV_BGR2RGBA);
                    Bitmap bitmap = IplImageToBitmap(image);
                    Matrix matrix = new Matrix();
                    matrix.postScale(300f / 640, 300f / 360);
                    // 得到新的图片
                    Bitmap newbm = Bitmap.createBitmap(bitmap, 0, 0, 640, 360, matrix,
                            true);
                    int[] ints = new int[300 * 300];
                    newbm.getPixels(ints, 0, 300, 0, 0, 300, 300);
                    float[] float_tensor = new float[300 * 300 * 3];
                    for (int j = 0; j < 300 * 300; j++) {
                        float_tensor[j * 3] = Color.red(ints[j]) * 0.007843f - 1;
                        float_tensor[j * 3 + 1] = Color.green(ints[j]) * 0.007843f - 1;
                        float_tensor[j * 3 + 2] = Color.blue(ints[j]) * 0.007843f - 1;
                    }
                    int status_load = loadTensor(float_tensor, float_tensor.length, object_id);
                    float[] result_object = null;
                    if (status_load == ConnectStatus.HS_OK) {
                        result_object = getResult(object_id);
                    }
                    HornedSungemFrame frame = getFrameResult(image, result_face, result_object);
                    Message message = mHandler.obtainMessage();
                    message.arg1 = 0;
                    message.obj = frame;
                    mHandler.sendMessage(message);
                } else {
                    continue;
                }
            } catch (Exception e) {
                return;
            }
        }
    }

    public HornedSungemFrame getFrameResult(opencv_core.IplImage image, float[] face_result, float[] object_result) {
        //结果处理
        int num = (int) face_result[0];//第一个数为检测到的个数

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
                //如果有值不满足条件，这组数据干掉
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
        return new HornedSungemFrame(IplImageToBitmap(image), objectInfos, num);
    }

    /**
     * @param iplImage
     * @return
     */
    public Bitmap IplImageToBitmap(opencv_core.IplImage iplImage) {
        if (iplImage == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(iplImage.width(), iplImage.height(),
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(iplImage.getByteBuffer());
        return bitmap;
    }
}

