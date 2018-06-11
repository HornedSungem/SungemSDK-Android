package com.senscape.hsdemo;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class MainActivity extends ListActivity {

    private String mTypes[] = {"Hello 2018", "人脸检测", "物体识别",
            "手绘识别"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.activities_list_text_view, mTypes);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, AboutActivity.class);
        intent.putExtra("ABOUT_TEXT_TITLE", mTypes[position]);
        switch (position) {
            case 0:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "hello2018.HelloActivity");
                intent.putExtra("ABOUT_TEXT", "hello/about.html");
                break;
            case 1:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "faceDetector.FaceDetectorActivity");
                intent.putExtra("ABOUT_TEXT", "face/about.html");
                break;
            case 2:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "objectDetector.ObjectDetectorActivity");
                intent.putExtra("ABOUT_TEXT", "object/about.html");
                break;
            case 3:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "sketchGuess.SketchGuessActivity");
                intent.putExtra("ABOUT_TEXT", "sketchguess/about.html");
                break;
        }

        startActivity(intent);

    }
}
