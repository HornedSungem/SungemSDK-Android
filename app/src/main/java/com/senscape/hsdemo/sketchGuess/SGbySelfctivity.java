package com.senscape.hsdemo.sketchGuess;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hornedSungem.library.ConnectStatus;
import com.senscape.hsdemo.R;

import org.bytedeco.javacpp.opencv_core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
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
public class SGbySelfctivity extends Activity implements Camera.PreviewCallback {

    private static final int REQUEST_CAMERA_CODE = 0xABCD;
    private SurfaceView mSurfaceView;
    private ImageView mImg_tensor;
    private ImageView mImgBorder;
    private TextView mTvResult1;
    private TextView mTvResult2;
    private TextView mTvResult3;
    private TextView mTvResult4;
    private TextView mTvResult5;
    private TextView mTvTarget;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    private boolean isInit = false;

    private int prevSizeW = 1280;
    private int prevSizeH = 720;


    private String[] mObjectNames = new String[345];
    private int index = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 100) {
                mTvTarget.setText("命题：" + mObjectNames[index]);
            }
            if (msg.arg1 == 2) {
                Bitmap bitmap = (Bitmap) msg.obj;
                if (bitmap != null)
                    mImg_tensor.setImageBitmap(bitmap);
            }
            if (msg.arg1 == 1) {
                //初始化失败
                int status = (int) msg.obj;
                if (status == ConnectStatus.HS_OK) {
                    isInit = true;
                    mImgBorder.setVisibility(View.VISIBLE);
                } else
                    Toast.makeText(SGbySelfctivity.this, "初始化失败,errorCode=" + msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };
    private SGInitThread mSGInitThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sgby_selfctivity);
        initView();
        initOjbectName();
        initY2R();
        UsbDevice usbDevice = getIntent().getParcelableExtra("usbdevice");
        if (usbDevice != null) {
            mSGInitThread = new SGInitThread(SGbySelfctivity.this, usbDevice, mHandler);
            mSGInitThread.start();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //23
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
                }
                return;
            }
            openSurfaceView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSGInitThread != null) mSGInitThread.closeDevice();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            }
        }
    }

    @SuppressLint("NewApi")
    private void initY2R() {
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    private void openSurfaceView() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mCamera == null) {
                    mCamera = Camera.open();
                    try {
                        mCamera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (holder.getSurface() == null) {
                    // preview surface does not exist
                    return;
                }
                try {
                    mCamera.stopPreview();

                } catch (Exception e) {
                    e.printStackTrace();
                    // ignore: tried to stop a non-existent preview
                }

                try {
                    mCamera.setPreviewDisplay(mSurfaceHolder);
                    setCameraParms(mCamera);
                    mCamera.setPreviewCallback(SGbySelfctivity.this);
                    mCamera.startPreview();
                } catch (Exception e) {
                    // ignore: tried to stop a non-existent preview
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        });

    }

    void initOjbectName() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory().getAbsolutePath() + "/hs/class_list_chn.txt"));
                    for (int i = 0; i < 345; i++) {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            String[] strings = line.split(" ");
                            mObjectNames[i] = strings[0];
                        }
                    }
                    bufferedReader.close();
                    Message message = new Message();
                    message.arg1 = 100;
                    mHandler.sendMessage(message);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e("SketchGuessThread", "FileNotFoundException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("SketchGuessThread", "IOException");
                }
            }
        }.start();
    }

    public void initView() {
        mSurfaceView = findViewById(R.id.sv_sg_self);
        mImg_tensor = findViewById(R.id.img_tensor_sg);
        mImgBorder = findViewById(R.id.img_border_sg);
        mTvResult1 = findViewById(R.id.tv_result1_sg);
        mTvResult2 = findViewById(R.id.tv_result2_sg);
        mTvResult3 = findViewById(R.id.tv_result3_sg);
        mTvResult4 = findViewById(R.id.tv_result4_sg);
        mTvResult5 = findViewById(R.id.tv_result5_sg);
        mTvTarget = findViewById(R.id.tv_garget);


    }

    @SuppressLint("NewApi")
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mSGInitThread != null && isInit) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(prevSizeW).setY(prevSizeH);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(data);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            new SGFrameTask().execute(out);
        }
    }

    public void setCameraParms(Camera camera) {
        // 获取摄像头支持的pictureSize列表
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        // 可支持的图像大小列表        选择需要分辨率
        Camera.Size pictureSize = getRequireSize(pictureSizeList);
        if (pictureSize != null)
            parameters.setPictureSize(pictureSize.width, pictureSize.height);

        // 获取摄像头支持的预览图Size列表   支持的比picturesize少
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size preSize = getRequireSize(previewSizeList);
        if (null != preSize) {
            parameters.setPreviewSize(preSize.width, preSize.height);
        }
        parameters.setJpegQuality(100);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            // 连续对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.cancelAutoFocus();
        camera.setParameters(parameters);
    }

    private Camera.Size getRequireSize(List<Camera.Size> pictureSizes) {
        Camera.Size result = null;
        for (Camera.Size size : pictureSizes) {
            if (size.width == prevSizeW && size.height == prevSizeH) {
                result = size;
                break;
            }
        }
        return result;
    }

    public class SGFrameTask extends AsyncTask<Allocation, Integer, String[]> {
        private double roi_ratio = 0.2;

        @Override
        protected String[] doInBackground(Allocation... allocations) {
            try {
                byte[] bytes = new byte[prevSizeW * prevSizeH * 4];
                allocations[0].copyTo(bytes);
                opencv_core.IplImage rgbaImage = opencv_core.IplImage.create(prevSizeW, prevSizeH, opencv_core.IPL_DEPTH_8U, 4);
                rgbaImage.getByteBuffer().put(bytes);
                //处理该图像
                int sg_weight = (int) (prevSizeW * roi_ratio);
                //crop
                opencv_core.CvRect cvRect = opencv_core.cvRect((int) (prevSizeW * (0.5 - roi_ratio / 2)), (int) (prevSizeH * 0.5 - sg_weight / 2), sg_weight, sg_weight);
                cvSetImageROI(rgbaImage, cvRect);
                opencv_core.IplImage cropped = cvCreateImage(cvGetSize(rgbaImage), rgbaImage.depth(), rgbaImage.nChannels());
                cvCopy(rgbaImage, cropped);
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
                int status_tensor = mSGInitThread.loadTensor(floats, floats.length, 0);
                if (status_tensor == ConnectStatus.HS_OK) {
                    float[] result = mSGInitThread.getResult(0);
                    if (result != null) {
                        return sortMax5(result);
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            if (result == null) {
                return;
            }
            mTvResult1.setText(result[0]);
            mTvResult2.setText(result[1]);
            mTvResult3.setText(result[2]);
            mTvResult4.setText(result[3]);
            mTvResult5.setText(result[4]);
            for (String str : result) {
                String[] strings = str.split(" ");
                if (mObjectNames[index].equals(strings[0])) {
                    Toast.makeText(SGbySelfctivity.this, ("恭喜命题绘画 " + mObjectNames[index] + " 过关！！！"), Toast.LENGTH_SHORT).show();
                    index++;
                    mTvTarget.setText("命题：" + mObjectNames[index]);
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
    }
}
