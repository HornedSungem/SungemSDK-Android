package com.senscape.hsdemo.objectDetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import com.hornedSungem.library.ConnectBridge;
import com.hornedSungem.library.ConnectStatus;
import com.hornedSungem.library.HsBaseActivity;
import com.senscape.hsdemo.DrawView;
import com.senscape.hsdemo.R;

import java.io.IOException;
import java.util.List;

public class ObjectDetectorBySelfActivity extends HsBaseActivity implements Camera.PreviewCallback {
    private static final String TAG = ObjectDetectorBySelfActivity.class.getSimpleName();

    private TextView mTvTip;
    private ObjectDetectorInitThread mObjectDetectorInitThread;
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
                if (status == ConnectStatus.HS_OK) {
                    isInit = true;
                } else
                    Toast.makeText(ObjectDetectorBySelfActivity.this, "初始化失败,errorCode=" + msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };
    private int prevSizeW = 1280;
    private int prevSizeH = 720;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detector_byself);
        initView();
        initY2R();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            }
        }
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
                    Log.e(TAG, "Error stopping camera preview: " + e.getMessage());
                }

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    setCameraParms(mCamera);
                    mCamera.setPreviewCallback(ObjectDetectorBySelfActivity.this);
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

    private void initY2R() {
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.sv_face_detector);
        mTvTip = findViewById(R.id.tv_tip);
        mDrawView = findViewById(R.id.dv_face_detector);
    }

    @Override
    public void openSucceed(ConnectBridge connectBridge) {
        mTvTip.setVisibility(View.GONE);
        mObjectDetectorInitThread = new ObjectDetectorInitThread(this, connectBridge, mHandler);
        mObjectDetectorInitThread.start();
    }

    @Override
    public void openFailed() {
        mTvTip.setText("请重新插拔角蜂鸟允许权限");
    }

    @Override
    public void disConnected() {
        Toast.makeText(this, "断开连接", Toast.LENGTH_SHORT).show();
        mCamera.stopPreview();
        if (mObjectDetectorInitThread != null) {
            mObjectDetectorInitThread.close();
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mObjectDetectorInitThread != null && isInit) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(prevSizeW).setY(prevSizeH);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(data);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            ODFrameTask myAsyncTask = new ODFrameTask(mObjectDetectorInitThread, mDrawView);
            myAsyncTask.execute(out);
        }
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
}
