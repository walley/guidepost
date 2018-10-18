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

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.support.v13.app.FragmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.webkit.WebView;
import android.util.Log;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import android.webkit.WebSettings;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.config.Configuration;
//import org.osmdroid.simplemap.R;
import org.osmdroid.views.MapView;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class guidepost extends AppCompatActivity implements FragmentCompat.OnRequestPermissionsResultCallback
{

  public static final String TAG = "GP";

  Button appexit;
  MapView map = null;

  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1338);
      Log.i(TAG, "write external storage Permission");
      return;
    }

    appexit = (Button) findViewById(R.id.appexit);

    appexit.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        finish();
      }
    });

    map = (MapView) findViewById(R.id.map);

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
      map.setTileSource(TileSourceFactory.MAPNIK);
      map.setBuiltInZoomControls(true);
      map.setMultiTouchControls(true);

      IMapController mapController = map.getController();
      mapController.setZoom(9);
      GeoPoint startPoint = new GeoPoint(48.8583, 2.2944);
      mapController.setCenter(startPoint);
    }

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

  public void onResume(){
    super.onResume();
    //this will refresh the osmdroid configuration on resuming.
    //if you make changes to the configuration, use
    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    try {
      map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    } catch (Exception e) {
      Log.i(TAG, "on_resume map is null:" + e.toString());
    }
  }

    public void onPause(){
      super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
      try {
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
      } catch (Exception e) {
        Log.i(TAG, "onpause map is null:" + e.toString());
      }
    }

  @Override
  public void onRequestPermissionsResult(int request, String permissions[], int[] results)
  {
//PERMISSION_GRANTED Constant Value: 0 (0x00000000)
//PERMISSION_DENIED Constant Value: -1 (0xffffffff)
    Log.e(TAG,"request:"+request);
    if (request == 1337) {
      Log.i(TAG, "Received response for contact permissions request.");
      Log.i(TAG, "l:" + permissions.length);
      for(int i = 0; i < permissions.length; i++) {
        Log.i(TAG, "perm,res:" + permissions[i] + results[i]);
        switch (permissions[i]){
          case "android.permission.ACCESS_FINE_LOCATION":
            if (results[i] == PERMISSION_GRANTED) {
            }
            break;
          default:
            break;
        }
      }
    } else {
      Log.e("GP","not our request?");
      super.onRequestPermissionsResult(request, permissions, results);
    }
  }
}
