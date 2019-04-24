/*
Copyright 2013-2019 Michal Grézl

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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.webkit.WebView;
import android.util.Log;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.webkit.WebSettings;
import android.widget.Toast;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapView;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class guidepost extends AppCompatActivity
{

  public static final String TAG = "GP";

  Context context = this;

  Button appexit;
  MapView map = null;
  MyLocationNewOverlay location_overlay;
  List<IGeoPoint> points = new ArrayList<>();
  RadiusMarkerClusterer gp_marker_cluster;
  GeoPoint startPoint = new GeoPoint(49.8583, 17.2944);
  Drawable poiIcon;
  int gp_overlay_number;
  Bitmap clusterIcon;

  void request_permission() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1338);
      Log.i(TAG, "write external storage Permission");
      return;
    }
  }

  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState)
  {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    request_permission();

    Ion.getDefault(context).configure().setLogging(TAG, Log.INFO);

    Drawable clusterIconD = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_poi_cluster, null);
    poiIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.marked_trail_red, null);
    clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();

    appexit = (Button) findViewById(R.id.appexit);
    appexit.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        finish();
      }
    });

    map = (MapView) findViewById(R.id.map);

    // If there is more than 200 millisecs no zoom/scroll update markers
    map.addMapListener(new DelayedMapListener(new MapListener() {
      @Override
      public boolean onScroll(ScrollEvent event) {
        Log.d(TAG, "onscroll overlays: " + map.getOverlays().size());
        reload_guideposts();
        return false;
      }

      @Override
      public boolean onZoom(ZoomEvent event) {
        Log.d(TAG, "onzoom overlays:" + map.getOverlays().size());
        reload_guideposts();
        return false;
      }
    }, 200));

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {

      create_gp_cluster_overlay();

      map.setTileSource(TileSourceFactory.MAPNIK);
      map.setBuiltInZoomControls(true);
      map.setMultiTouchControls(true);

      map.addMapListener(new MapListener() {
        @Override
        public boolean onScroll(ScrollEvent event) {
          Log.i(IMapView.LOGTAG, System.currentTimeMillis() + " onScroll " + event.getX() + "," +event.getY());
          //Toast.makeText(getActivity(), "onScroll", Toast.LENGTH_SHORT).show();
          //updateInfo();
          return true;
        }

        @Override
        public boolean onZoom(ZoomEvent event) {
          Log.i(IMapView.LOGTAG, System.currentTimeMillis() + " onZoom " + event.getZoomLevel());
          updateInfo();
          return true;
        }
      });

      IMapController mapController = map.getController();
      mapController.setZoom(14);
      mapController.setCenter(startPoint);

      location_overlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
      location_overlay.enableMyLocation();
      map.getOverlays().add(location_overlay);

// wrap them in a theme
      SimplePointTheme pt = new SimplePointTheme(points, true);

// create label style
      Paint textStyle = new Paint();
      textStyle.setStyle(Paint.Style.FILL);
      textStyle.setColor(Color.parseColor("#0000ff"));
      textStyle.setTextAlign(Paint.Align.CENTER);
      textStyle.setTextSize(24);

// set some visual options for the overlay
      SimpleFastPointOverlayOptions opt = SimpleFastPointOverlayOptions.getDefaultStyle()
              .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
              .setRadius(7).setIsClickable(true).setCellSize(15).setTextStyle(textStyle);

// create the overlay with the theme
      final SimpleFastPointOverlay sfpo = new SimpleFastPointOverlay(pt, opt);

// onClick callback
      sfpo.setOnClickListener(new SimpleFastPointOverlay.OnClickListener() {
        @Override
        public void onClick(SimpleFastPointOverlay.PointAdapter points, Integer point) {
          Toast.makeText(map.getContext()
                  , "You clicked " + ((LabelledGeoPoint) points.get(point)).getLabel()
                  , Toast.LENGTH_SHORT).show();
        }
      });

      map.getOverlays().add(sfpo);
    }

    int resource = R.raw.web;
    InputStream inputStream = null;
    inputStream = getResources().openRawResource(resource);
    String html = "error something should be here error";
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
    Log.e(TAG,"onRequestPermissionsResult request:" + request);
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

  private void reload_guideposts()
  {
    StringBuilder bbox_param = new StringBuilder();
    BoundingBox bbox = map.getBoundingBox();
    double minlat = bbox.getActualSouth();
    double maxlat = bbox.getActualNorth();
    double minlon = bbox.getLonWest();
    double maxlon = bbox.getLonEast();
    bbox_param.append(minlon)
            .append(",")
            .append(minlat)
            .append(",")
            .append(maxlon)
            .append(",")
            .append(maxlat);

    Log.i(TAG, bbox_param.toString());

    Ion.with(context)
            .load("https://api.openstreetmap.social/table/all?output=json&bbox="+bbox_param.toString())
            .asJsonArray()
            .setCallback(new FutureCallback<JsonArray>() {
              @Override
              public void onCompleted(Exception exception, JsonArray result) {

                try {
                  map.getOverlays().remove(gp_overlay_number);
                } catch (Exception e) {
                 Log.e(TAG, "canot remove overlay" + e.toString());
                }

                create_gp_cluster_overlay();

                JsonArray item_json;
                // do stuff with the result or error
                Log.i(TAG, "bbox json loaded" + result);
                Iterator it = result.iterator();
                while (it.hasNext()) {
                  JsonElement element = (JsonElement) it.next();
                  Log.i(TAG, "json");
                  item_json = element.getAsJsonArray();
                  try {
                        int id     = item_json.get(0).getAsInt();
                    double lat     = item_json.get(1).getAsDouble();
                    double lon     = item_json.get(2).getAsDouble();
                    String img     = item_json.get(3).getAsString();
                    //double lon = item_json.get(4).getAsDouble();
                    String author  = item_json.get(5).getAsString();
                    String ref     = item_json.get(6).getAsString();
                    String note    = item_json.get(7).getAsString();
                    String license = item_json.get(8).getAsString();
                    String tags    = item_json.get(9).getAsString();

                    points.add(new LabelledGeoPoint(lat, lon, "gp " + id + " " +ref));

                    GeoPoint poi_loc = new GeoPoint(lat,lon);

                    final Marker gp_marker = new Marker(map);
                    gp_marker.setTitle("gp: " + id);
                    gp_marker.setSubDescription(""+img);
                    gp_marker.setSnippet("img:" + img + "<br>author:" + author+ "<br>ref:" + ref + "<br>tags:" + tags);
                    gp_marker.setPosition(poi_loc);
                    gp_marker.setIcon(poiIcon);
                    //gp_marker.setImage(d_poi_icon);


                    Ion.with(context)
//                            .load("http://api.openstreetmap.social/p/phpThumb.php?h=150&src=http://api.openstreetmap.social" + img)
                            .load("http://api.openstreetmap.social/" + img)
                            .withBitmap()
                            .asBitmap()
                            .setCallback(new FutureCallback<Bitmap>() {
                              @Override
                              public void onCompleted(Exception exception, Bitmap b) {
                                Drawable d = new BitmapDrawable(getResources(), b);
                                gp_marker.setImage(d);
                              }
                            });

                    gp_marker_cluster.add(gp_marker);

                    Log.i(TAG, "added "+lat+lon+id);
                    map.invalidate();
                  } catch (Exception e) {
                    Log.i(TAG, "exception " + e.toString());

                  }

                }
                Log.i(TAG, "json done");

              }
            });

  }

private void create_gp_cluster_overlay()
{
  gp_marker_cluster = new RadiusMarkerClusterer(context);
  gp_marker_cluster.setIcon(clusterIcon);
  gp_marker_cluster.getTextPaint().setTextSize(12 * getResources().getDisplayMetrics().density);
  gp_marker_cluster.mAnchorV = Marker.ANCHOR_BOTTOM;
  gp_marker_cluster.mTextAnchorU = 0.70f;
  gp_marker_cluster.mTextAnchorV = 0.27f;

  map.getOverlays().add(gp_marker_cluster);
  gp_overlay_number = map.getOverlays().size() - 1;
}

  private void updateInfo()
  {
/*    return;
    IGeoPoint mapCenter = map.getMapCenter();
    Toast.makeText(context,
    mapCenter.getLatitude()+","+ mapCenter.getLongitude()+",zoom="+map.getZoomLevelDouble(),
    Toast.LENGTH_SHORT).sho();
  */}

}
