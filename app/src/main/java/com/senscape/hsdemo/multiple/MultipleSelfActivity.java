package com.senscape.hsdemo.multiple;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.senscape.hsdemo.DrawView;
import com.senscape.hsdemo.R;

import java.io.IOException;
import java.util.List;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class MultipleSelfActivity extends Activity implements Camera.PreviewCallback {

    private TextView mTvTip;
    private MultipleModelThread mMultipleModelThread;
    private SurfaceView mSurfaceView;
    private DrawView mDrawView;

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private static final int REQUEST_CAMERA_CODE = 0x100;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    private boolean isInit = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1) {
                //初始化失败
                int status = (int) msg.obj;
                if (status >= 0) {
                    isInit = true;
                } else
                    Toast.makeText(MultipleSelfActivity.this, "初始化失败,errorCode=" + msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };
    private int prevSizeW = 1280;
    private int prevSizeH = 720;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple_self);
        initView();
        initY2R();
        UsbDevice usbDevice = getIntent().getParcelableExtra("usbdevice");
        mTvTip.setVisibility(View.GONE);
        mMultipleModelThread = new MultipleModelThread(this, usbDevice, mHandler);
        mMultipleModelThread.start();
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
        if (mMultipleModelThread != null) mMultipleModelThread.closeDevice();
    }

    @SuppressLint("NewApi")
    private void initY2R() {
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.sv_multiple_model);
        mTvTip = findViewById(R.id.tv_multiple_model);
        mDrawView = findViewById(R.id.dv_multiple_model);
    }

    /**
     * 把摄像头的图像显示到SurfaceView
     */
    private void openSurfaceView() {
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new SurfaceHolder.Callback() {
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
                    // ignore: tried to stop a non-existent preview
                    Log.e("MultipleModelActivity", "Error stopping camera preview: " + e.getMessage());

                }

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    setCameraParms(mCamera);
                    mCamera.setPreviewCallback(MultipleSelfActivity.this);
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

    /**
     * 在摄像头启动前设置picture和preview参数
     *
     * @param camera
     */
    private void setCameraParms(Camera camera) {
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

    @SuppressLint("NewApi")
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (mMultipleModelThread != null && isInit) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(bytes.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(prevSizeW).setY(prevSizeH);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(bytes);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            MMFrameTask mmFrameTask = new MMFrameTask(mMultipleModelThread, mDrawView);
            mmFrameTask.execute(out);
        }
    }
}
