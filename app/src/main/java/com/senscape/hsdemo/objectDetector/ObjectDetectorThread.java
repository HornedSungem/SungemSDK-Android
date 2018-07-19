package com.senscape.hsdemo.objectDetector;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.ConnectStatus;
import com.hornedSungem.library.model.HornedSungemFrame;
import com.hornedSungem.library.thread.HsBaseThread;

import org.bytedeco.javacpp.opencv_core;

import java.util.ArrayList;

import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2RGBA;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

/**
 * Created by looper.lu on 18/5/18.
 */

public class ObjectDetectorThread extends HsBaseThread {
    String[] labels = {"aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    private Handler mHandler;
    private Activity mActivity;
    private int FRAME_W = 640;
    private int FRAME_H = 360;
    private float STD = 0.007843f;
    private float MEAN = 0.9999825f;

    public ObjectDetectorThread(Activity activity, ConnectBridge connectBridge, Handler handler) {
        super(connectBridge,true);
        mActivity = activity;
        mHandler = handler;
    }

    /**
     *
     */
    @Override
    public void run() {
        super.run();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int status = allocateGraphByAssets(mActivity, "graph_object_SSD");
        if (status != ConnectStatus.HS_OK) {
            Message message = mHandler.obtainMessage();
            message.arg1 = 1;
            message.obj = status;
            mHandler.sendMessage(message);
            return;
        }
        while (true) {
                try {
                    byte[] bytes = getImage(STD, MEAN);
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
                } catch (Exception e) {
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
