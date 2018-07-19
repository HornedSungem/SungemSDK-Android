package com.senscape.hsdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.hornedSungem.library.model.HornedSungemFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class DrawView extends View {

    private Paint paint;
    private List<HornedSungemFrame.ObjectInfo> mObjectInfos;

    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setTextSize(25);
        mObjectInfos = new ArrayList<>();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mObjectInfos != null && mObjectInfos.size() > 0) {
            for (HornedSungemFrame.ObjectInfo objectInfo : mObjectInfos) {
                if (objectInfo == null) break;
                canvas.drawRect(objectInfo.getRect(), paint);
                // 因为旋转了画布矩阵，所以字体也跟着旋转
                canvas.drawText(String.valueOf("  " + objectInfo.getType() + "\n置信度:" + objectInfo.getScore()), objectInfo.getRect().left, objectInfo.getRect().bottom - 20, paint);
            }
        }
    }

    public void update(List<HornedSungemFrame.ObjectInfo> rect) {
        mObjectInfos = rect;
        invalidate();
    }

    /**
     * 清除已经画上去的框
     */
    public void removeRect() {
        mObjectInfos = null;
        invalidate();
    }
}
