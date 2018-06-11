package com.senscape.hsdemo.sketchGuess;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.ConnectStatus;
import com.hornedSungem.library.thread.HsThread;

import org.bytedeco.javacpp.opencv_core;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;

import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2RGBA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_GRAY2RGBA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_SHAPE_RECT;
import static org.bytedeco.javacpp.opencv_imgproc.cvCanny;
import static org.bytedeco.javacpp.opencv_imgproc.cvCreateStructuringElementEx;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDilate;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;


/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */

public class SketchGuessThread extends HsThread {
    private double roi_ratio = 0.2;
    private String[] mObjectNames = new String[345];

    public SketchGuessThread(Activity activity, ConnectBridge connectBridge, Handler handler, String[] names) {
        super(activity, connectBridge, handler);
        mObjectNames = names;
    }

    @Override
    public void run() {
        super.run();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int status = allocateGraphByAssets("graph_sg");
        if (status != ConnectStatus.HS_OK) {
            Message message = mHandler.obtainMessage();
            message.arg1 = 1;
            message.obj = status;
            mHandler.sendMessage(message);
            return;
        }
        while (isRunning) {
            byte[] bytes_frame = deviceGetImage(true);
            if (bytes_frame != null) {
                opencv_core.IplImage bgrImage = opencv_core.IplImage.create(FRAME_W, FRAME_H, opencv_core.IPL_DEPTH_8U, 3);
                bgrImage.getByteBuffer().put(bytes_frame);
                opencv_core.IplImage image = opencv_core.IplImage.create(FRAME_W, FRAME_H, opencv_core.IPL_DEPTH_8U, 4);
                cvCvtColor(bgrImage, image, CV_BGR2RGBA);
                Message message = mHandler.obtainMessage();
                message.arg1 = 0;
                message.obj = IplImageToBitmap(image);
                mHandler.sendMessage(message);
                //处理该图像
                int sg_weight = (int) (FRAME_W * roi_ratio);
                //crop
                opencv_core.CvRect cvRect = opencv_core.cvRect((int) (FRAME_W * (0.5 - roi_ratio / 2)), (int) (FRAME_H * 0.5 - sg_weight / 2), sg_weight, sg_weight);
                cvSetImageROI(bgrImage, cvRect);
                opencv_core.IplImage cropped = cvCreateImage(cvGetSize(bgrImage), bgrImage.depth(), bgrImage.nChannels());
                cvCopy(bgrImage, cropped);
                //canny
                opencv_core.IplImage image_canny = opencv_core.IplImage.create(sg_weight, sg_weight, opencv_core.IPL_DEPTH_8U, 1);
                cvCanny(cropped, image_canny, 120, 45);
                //dilate
                opencv_core.IplImage image_dilate = opencv_core.IplImage.create(sg_weight, sg_weight, opencv_core.IPL_DEPTH_8U, 1);
                //kernel = np.ones((4,4),np.uint8)
                opencv_core.IplConvKernel iplConvKernel = cvCreateStructuringElementEx(4, 4, 0, 0, CV_SHAPE_RECT);
                cvDilate(image_canny, image_dilate, iplConvKernel, 1);
                opencv_core.IplImage image_dilate_rgba = opencv_core.IplImage.create(sg_weight, sg_weight, opencv_core.IPL_DEPTH_8U, 4);
                cvCvtColor(image_dilate, image_dilate_rgba, CV_GRAY2RGBA);
                //resize
                opencv_core.IplImage image_load = opencv_core.IplImage.create(28, 28, opencv_core.IPL_DEPTH_8U, 4);
                cvResize(image_dilate_rgba, image_load);
                //把该image_dilate转化成float类型传送给角蜂鸟
                Bitmap bitmap_tensor = IplImageToBitmap(image_load);
                Message message1 = new Message();
                message1.arg1 = 2;
                message1.obj = bitmap_tensor;
                mHandler.sendMessage(message1);
                int[] pixels = new int[28 * 28];
                bitmap_tensor.getPixels(pixels, 0, 28, 0, 0, 28, 28);
                float[] floats = new float[28 * 28 * 3];
                for (int i = 0; i < 28 * 28; i++) {
                    floats[i] = Color.red(pixels[i]) * 0.007843f - 1;
                    floats[3 * i + 1] = Color.green(pixels[i]) * 0.007843f - 1;
                    floats[3 * i + 2] = Color.blue(pixels[i]) * 0.007843f - 1;
                }
                int status_tensor = loadTensor(floats, floats.length, 0);
                if (status_tensor == ConnectStatus.HS_OK) {
                    float[] result = getResult(0);
                    if (result != null) {
                        Message message2 = new Message();
                        message2.arg1 = 3;
                        message2.obj = sortMax5(result);
                        mHandler.sendMessage(message2);
                    }
                }

            }
        }
    }

    /**
     * @param iplImage
     * @return
     */
    public Bitmap IplImageToBitmap(opencv_core.IplImage iplImage) {
        Bitmap bitmap = Bitmap.createBitmap(iplImage.width(), iplImage.height(),
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(iplImage.getByteBuffer());
        return bitmap;
    }

    /**
     * @param iplImage
     * @return
     */
    public Bitmap IplImageToBitmapRGB(opencv_core.IplImage iplImage) {
        Bitmap bitmap = Bitmap.createBitmap(iplImage.width(), iplImage.height(),
                Bitmap.Config.RGB_565);
        bitmap.copyPixelsFromBuffer(iplImage.getByteBuffer());
        return bitmap;
    }

    public String[] sortMax5(float[] result) {

        HashMap<Integer, Float> integerFloatHashMap = new HashMap<>();
        String[] object_names = new String[5];
        for (int i = 0; i < result.length; i++) {
            integerFloatHashMap.put(i, result[i]);
        }
        Arrays.sort(result);
        for (int i = 0; i < result.length; i++) {
            if (integerFloatHashMap.get(i) == result[result.length - 1]) {
                object_names[0] = mObjectNames[i] + " " + keep_2digit_decimal(result[result.length - 1] * 100) + "%";
            }
            if (integerFloatHashMap.get(i) == result[result.length - 2]) {
                object_names[1] = mObjectNames[i] + " " + keep_2digit_decimal(result[result.length - 2] * 100) + "%";
            }
            if (integerFloatHashMap.get(i) == result[result.length - 3]) {
                object_names[2] = mObjectNames[i] + " " + keep_2digit_decimal(result[result.length - 3] * 100) + "%";
            }
            if (integerFloatHashMap.get(i) == result[result.length - 4]) {
                object_names[3] = mObjectNames[i] + " " + keep_2digit_decimal(result[result.length - 4] * 100) + "%";
            }
            if (integerFloatHashMap.get(i) == result[result.length - 5]) {
                object_names[4] = mObjectNames[i] + " " + keep_2digit_decimal(result[result.length - 5] * 100) + "%";
            }
        }
        return object_names;
    }

    public float keep_2digit_decimal(float f) {
        BigDecimal b = new BigDecimal(f);
        return b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
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
