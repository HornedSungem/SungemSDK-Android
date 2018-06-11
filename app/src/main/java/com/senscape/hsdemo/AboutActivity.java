package com.senscape.hsdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
/**
 * Copyright(c) 2018 HornedSungem Corporation.
 * License: Apache 2.0
 */
public class AboutActivity extends Activity implements View.OnClickListener {

    private static final String LOGTAG = "AboutActivity";
    private WebView mAboutWebText;
    private Button mStartButton;
    private TextView mAboutTextTitle;
    private String mLaunchType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_about);

        Bundle extras = getIntent().getExtras();
        String webText = extras.getString("ABOUT_TEXT");
        mLaunchType = extras.getString("ACTIVITY_TO_LAUNCH");

        mAboutWebText = findViewById(R.id.about_html_text);

        AboutWebViewClient aboutWebClient = new AboutWebViewClient();
        mAboutWebText.setWebViewClient(aboutWebClient);

        String aboutText = "";
        try {
            InputStream is = getAssets().open(webText);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));
            String line;

            while ((line = reader.readLine()) != null) {
                aboutText += line;
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "About html loading failed");
        }

        mAboutWebText.loadData(aboutText, "text/html", "UTF-8");

        mStartButton = findViewById(R.id.button_start);
        mStartButton.setOnClickListener(this);

        mAboutTextTitle = findViewById(R.id.about_text_title);
        mAboutTextTitle.setText(extras.getString("ABOUT_TEXT_TITLE"));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_start:
                startShowActivity();
                break;
        }
    }

    private void startShowActivity() {
        Intent i = new Intent();
        i.setClassName(getPackageName(), getPackageName() + "." + mLaunchType);
        startActivity(i);
    }

    private class AboutWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }
}
