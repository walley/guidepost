/*
Copyright 2013-2016 Michal Grezl

This file is part of Guidepost.

Guidepost is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Guidepost is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Guidepost.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.walley.guidepost;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.webkit.WebView;
import android.util.Log;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import android.webkit.WebSettings;

public class guidepost extends Activity
{
  Button appexit;

  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    appexit = (Button) findViewById(R.id.appexit);

    appexit.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        finish();
      }
    });

    int resource = R.raw.web;
    InputStream inputStream = null;
    inputStream = getResources().openRawResource(resource);
    String html = "error something should be here error";
    try {
      html = IOUtils.toString(inputStream);
    } catch (IOException e) {
      Log.e("GP","read script " + e.toString());
      e.printStackTrace();
    }

    Log.e("GP", html);

    WebView webView;
    webView = (WebView) findViewById(R.id.webview);
    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);

    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

  }
}
