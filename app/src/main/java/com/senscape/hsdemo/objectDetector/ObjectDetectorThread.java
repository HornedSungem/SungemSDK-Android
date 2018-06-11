package com.senscape.hsdemo.objectDetector;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.ConnectStatus;
import com.hornedSungem.library.model.HornedSungemFrame;
import com.hornedSungem.library.thread.HsThread;

import org.bytedeco.javacpp.opencv_core;

import java.util.ArrayList;

import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2RGBA;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

/**
 * Created by looper.lu on 18/5/18.
 */

public class ObjectDetectorThread extends HsThread {
    String[] labels = {"aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};

    public ObjectDetectorThread(Activity activity, ConnectBridge connectBridge, Handler handler) {
        super(activity, connectBridge, handler);
    }

    @Override
    public void run() {
        super.run();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int status = allocateGraphByAssets("graph_object_SSD");
        if (status != ConnectStatus.HS_OK) {
            Message message = mHandler.obtainMessage();
            message.arg1 = 1;
            message.obj = status;
            mHandler.sendMessage(message);
            return;
        }
        while (true) {
            if (mHsApi != null && isRunning) {
                byte[] bytes = getImage(STD, MEAN, zoom);
                float[] result = getResult(0);
                if (bytes != null && result != null) {
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
                    HornedSungemFrame frame = getFrameResult(image, result);
                    Message message = mHandler.obtainMessage();
                    message.arg1 = 0;
                    message.obj = frame;
                    mHandler.sendMessage(message);
                } else {
                    continue;
                }
            } else {
                return;
            }
        }
    }

    public HornedSungemFrame getFrameResult(opencv_core.IplImage image, float[] floats) {
        //检测结果处理
        int num = (int) floats[0];
        ArrayList<HornedSungemFrame.ObjectInfo> objectInfos = new ArrayList<>();
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                HornedSungemFrame.ObjectInfo objectInfo = new HornedSungemFrame.ObjectInfo();
                int type = (int) (floats[7 * (i + 1) + 1]);
                int x1 = (int) (floats[7 * (i + 1) + 3] * FRAME_W);
                int y1 = (int) (floats[7 * (i + 1) + 4] * FRAME_H);
                int x2 = (int) (floats[7 * (i + 1) + 5] * FRAME_W);
                int y2 = (int) (floats[7 * (i + 1) + 6] * FRAME_H);
                int wight = x2 - x1;
                int height = y2 - y1;
                int percentage = (int) (floats[7 * (i + 1) + 2] * 100);
                if (type == 0) {
                    continue;
                }
                if (percentage <= MIN_SCORE_PERCENT) {
                    continue;
                }
                if (wight >= FRAME_W * 0.8 || height >= FRAME_H * 0.8) {
                    continue;
                }
                //如果有一个值为小于0，这组数据干掉不要
                if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0 || wight < 0 || height < 0) {
                    continue;
                }
                objectInfo.setType(labels[type - 1]);
                objectInfo.setRect(new Rect(x1 * 2, y1 * 2, x2 * 2, y2 * 2));//检测到的人脸矩形
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
        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(iplImage.width(), iplImage.height(),
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(iplImage.getByteBuffer());
        return bitmap;
    }
}
