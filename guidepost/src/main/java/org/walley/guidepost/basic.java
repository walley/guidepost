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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class basic extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{

  public static final String TAG = "GP-B";

  private static final int REQUEST_IMAGE_CAPTURE = 1;
  private static final int RESULT_SETTINGS = 2;

  ActionBarDrawerToggle toggle;
  Bitmap b_cluster_icon;
  Context context = this;
  Drawable d_cluster_icon;
  Drawable d_poi_icon;
  DrawerLayout drawer;
  FloatingActionButton fab;
  GeoPoint start_point;
  GeoPoint current_point;
  IMapController map_controller;
  int gp_overlay_number;
  MapView map = null;
  MyLocationNewOverlay location_overlay;
  NavigationView navigationView;
  Toolbar toolbar;
  wlocation gps;
  String current_photo;
  wclusterer gp_marker_cluster;
  winfowindow wi;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.basic);

    Ion.getDefault(context).configure().setLogging(TAG, Log.INFO);
    request_permission();

    create_ui();

    gp_marker_cluster = new wclusterer(context);

    if (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
      Toast.makeText(context, "I need WRITE_EXTERNAL_STORAGE, sorry", Toast.LENGTH_SHORT).show();
      //  finish();
    } else {
      create_map();
      move_to_default_position();
    }

    gps = new wlocation(basic.this);

    if (!gps.is_gps_enabled) {
      Toast.makeText(this, "GPS Disabled", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case RESULT_SETTINGS:
        //    validate_settings();
        //    showUserSettings();
        break;
      case REQUEST_IMAGE_CAPTURE:
        if (resultCode == RESULT_OK) {
          try {
            File file = new File(current_photo);
//            Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", current_photo);
            Uri uri = FileProvider.getUriForFile(context, "org.walley.guidepost", file);
            Log.i(TAG, "onActivityResult(REQUEST_IMAGE_CAPTURE) currphoto:" + current_photo);
            Log.i(TAG, "onActivityResult(REQUEST_IMAGE_CAPTURE) uri:" + uri.toString());
            Intent share = new Intent();
            share.setAction(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setType("image/jpeg");
            share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(share);
          } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "onActivityResult(REQUEST_IMAGE_CAPTURE):" + e.toString());
          }
        }
        break;

    }
  }

  @Override
  public void onBackPressed()
  {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      Intent i_preferences = new Intent();
      i_preferences.setClass(context, preferences.class);
      startActivityForResult(i_preferences, RESULT_SETTINGS);
    }

    return super.onOptionsItemSelected(item);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item)
  {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    if (id == R.id.nav_location) {
      move_to_current_position();
    } else if (id == R.id.nav_gallery) {
      Intent intent = new Intent();
      intent.setAction(android.content.Intent.ACTION_VIEW);
      intent.setType("image/*");
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    } else if (id == R.id.nav_share) {

    } else if (id == R.id.nav_send) {

    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  @Override
  public void onRequestPermissionsResult(int request, String permissions[], int[] results)
  {
//PERMISSION_GRANTED Constant Value: 0 (0x00000000)
//PERMISSION_DENIED Constant Value: -1 (0xffffffff)
    Log.e(TAG, "request:" + request);
    if (request == 1337) {
      Log.i(TAG, "Received response for contact permissions request.");
      Log.i(TAG, "l:" + permissions.length);
      for (int i = 0; i < permissions.length; i++) {
        Log.i(TAG, "perm,res:" + permissions[i] + results[i]);
        switch (permissions[i]) {
          case "android.permission.ACCESS_FINE_LOCATION":
            if (results[i] == PERMISSION_GRANTED) {
            }
            break;
          case "android.permission.WRITE_EXTERNAL_STORAGE":
            if (results[i] == PERMISSION_GRANTED) {
              create_map();
              move_to_default_position();
            }
            break;
          default:
            break;
        }
      }
    } else {
      Log.e("GP", "not our request?");
      super.onRequestPermissionsResult(request, permissions, results);
    }
  }

  @Override
  protected void onStop()
  {
    super.onStop();
/*      final MapTileProviderBase mapTileProvider = this.mMapView.getTileProvider();
      mapTileProvider.clearTileCache();*/
  }

  public void onResume()
  {
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

  public void onPause()
  {
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

  /******************************************************************************/
  /******************************************************************************/
  /******************************************************************************/

  void request_permission()
  {
    List<String> permissions = new ArrayList<>();

    if (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
      permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    if (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
      permissions.add(android.Manifest.permission.CAMERA);
    }
    if (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
      permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
    }

    ActivityCompat.requestPermissions(
            this, permissions.toArray(new String[permissions.size()]), 1337);
    Log.i(TAG, "Permission request");
    return;
  }

  void request_permissionx()
  {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

      // Permission is not granted
      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(
              this,
              Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                             )) {
        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
      } else {
        // No explanation needed; request the permission
        ActivityCompat.requestPermissions(
                this,
                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1337
                                         );

      }
    } else {
      Log.i(TAG, "Permission has already been granted");
    }
  }

  private void remove_overlays()
  {
    Log.i(TAG, "number of overlays from manager: " + map.getOverlayManager().size());
    Log.i(TAG, "number of overlays from overlays: " + map.getOverlays().size());
//  for (int i=0; i < map.getOverlays().size(); i++) {
//    map.getOverlays().get(i)...remove();
//  }
//  map.getOverlays().clear();

    Log.i(TAG, "number of overlays from manager: " + map.getOverlayManager().size());
    Log.i(TAG, "number of overlays from overlays: " + map.getOverlays().size());
  }

  //  private void create_gp_cluster_overlay(wclusterer gp_marker_cluster)
  private void create_gp_cluster_overlay()
  {
    Log.i(TAG, "number of overlays from manager: " + map.getOverlayManager().size());
    Log.i(TAG, "number of overlays from overlays: " + map.getOverlays().size());

    Log.i(TAG, "gp_overlay_number: " + gp_overlay_number);

    Log.i(TAG, "clearing markers");
//    gp_marker_cluster.clear_stuff(map);
    Log.i(TAG, "done");

    try {
      map.getOverlays().remove(gp_overlay_number);
      map.invalidate();
    } catch (Exception e) {
      Log.e(TAG, "cannot remove overlay" + e.toString());
    }

    gp_marker_cluster.setIcon(b_cluster_icon);
    gp_marker_cluster.getTextPaint().setTextSize(12 * getResources().getDisplayMetrics().density);
    gp_marker_cluster.mAnchorV = Marker.ANCHOR_BOTTOM;
    gp_marker_cluster.mTextAnchorU = 0.70f;
    gp_marker_cluster.mTextAnchorV = 0.27f;

    map.getOverlays().add(gp_marker_cluster);
    gp_overlay_number = map.getOverlays().size() - 1;
    Log.i(TAG, "gp_overlay_number: " + gp_overlay_number);
    Log.i(TAG, "number of overlays from manager: " + map.getOverlayManager().size());
    Log.i(TAG, "number of overlays from overlays: " + map.getOverlays().size());
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
    Ion.getDefault(context).cancelAll(context);

    Log.i(TAG, bbox_param.toString());

    Ion.with(context)
            .load("https://api.openstreetmap.social/table/all?output=json&bbox=" + bbox_param.toString())
            .asJsonArray()
            .setCallback(new FutureCallback<JsonArray>()
            {
              @Override
              public void onCompleted(Exception ione, JsonArray result)
              {
                int id = 0;
                double lat = 0;
                double lon = 0;
                String img = null;
                //double lon = item_json.get(4).getAsDouble();
                String author = null;
                String ref = null;
                String note = null;
                String license = null;
                String tags = null;

                if (ione != null) {
                  Toast.makeText(context, "ion json Error", Toast.LENGTH_LONG).show();
                  Log.e(TAG, "ion exception" + ione.toString());
                  return;
                }

//                create_gp_cluster_overlay(gp_marker_cluster);
//                create_gp_cluster_overlay();
                remove_overlays();
                JsonArray item_json;
                // do stuff with the result or error
                Log.i(TAG, "bbox json loaded ");
                if (result == null) {
                  Log.i(TAG, "bbox json is null");
                }

                Iterator it = result.iterator();
                int ids_added = 0;
                while (it.hasNext()) {
                  JsonElement element = (JsonElement) it.next();
                  item_json = element.getAsJsonArray();

                  id = 0;

                  try {
                    id = item_json.get(0).getAsInt();
                    lat = item_json.get(1).getAsDouble();
                    lon = item_json.get(2).getAsDouble();
                    img = item_json.get(3).getAsString();
                    //double lon = item_json.get(4).getAsDouble();
                    author = item_json.get(5).getAsString();
                    ref = item_json.get(6).toString();
                    if (ref.equals("null")) {
                      ref = "";
                    }
                    //note = item_json.get(7).getAsString();
                    license = item_json.get(8).getAsString();

                    if (item_json.get(9).toString().equals("null")) {
                      tags = "";
                    } else {
                      tags = item_json.get(9).getAsString();
                    }
                  } catch (Exception e) {
                    Log.e(TAG, "exception parsing json " + e.toString());
                    continue;
                  }

                  if (id == 0) {
                    Log.e(TAG, "id is 0 which is wrong");
                    continue;
                  }

                  final StringBuilder snippet = new StringBuilder();

                  try {
                    snippet.append("<html><body>");
                    snippet.append("<a href='https://api.openstreetmap.social/").append(img).append(
                            "'>");
                    snippet.append(
                            "<img height='150' src='http://api.openstreetmap.social/p/phpThumb.php?h=150&src=http://api.openstreetmap.social/").append(
                            img).append("'>");
                    snippet.append("</a> <br>");

                    snippet.append("<a href='https://api.openstreetmap.social/table/id/").append(
                            id).append(
                            "'>id ").append(id);
                    snippet.append("</a><br>");
                    snippet.append("<ul>");
                    snippet.append("<li>image:").append(img);
                    snippet.append("<li>author:").append(author);
                    snippet.append("<li>ref:").append(ref);
                    snippet.append("<li>license:").append(license);
                    snippet.append("</ul>");
                    snippet.append("<h3>tags:</h3>").append(tags);
                    snippet.append("</body></html>");
                  } catch (Exception e) {
                    Log.e(TAG, "exception setting " + e.toString());
                  }

                  GeoPoint poi_loc = new GeoPoint(lat, lon);
                  final Marker gp_marker = new Marker(map);

                  try {
//                    wi.set_text("Guidepost id " + id);
//                    wi.set_html(snippet.toString());
                    gp_marker.setSnippet(snippet.toString());
                    gp_marker.setInfoWindow(wi);

                    gp_marker.setTitle("Guidepost id " + id);
                    gp_marker.setSubDescription("" + id);
                    gp_marker.setPosition(poi_loc);
                    poi_loc = null;
                    gp_marker.setIcon(d_poi_icon);
                    //gp_marker.setImage(d_poi_icon);
                    //gp_marker_cluster.add(gp_marker);
                    map.getOverlays().add(gp_marker);
                    //gp_marker.remove(map);

                    ids_added++;
                  } catch (Exception e) {
                    Log.e(TAG, "exception adding " + e.toString());
                  }
                }
                Log.i(TAG, "json done, markers added " + ids_added);
                map.invalidate();
              }
            });
  }

  private void create_map()
  {

    start_point = new GeoPoint(49.5, 17.0);

    map = findViewById(R.id.map);
    map_controller = map.getController();

    wi = new winfowindow(R.layout.cluster_bubble, map);

    location_overlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
    location_overlay.enableMyLocation();
    map.getOverlays().add(location_overlay);

    Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

    map.setTilesScaledToDpi(true);
    map.setTileSource(TileSourceFactory.MAPNIK);
    map.setBuiltInZoomControls(true);
    map.setMultiTouchControls(true);

    map.addMapListener(new DelayedMapListener(new MapListener()
    {
      @Override
      public boolean onScroll(ScrollEvent event)
      {
        Log.i(TAG, "onScroll " + event.getX() + "," + event.getY());
        reload_guideposts();
        map.invalidate();
        return false;
//return true;
      }

      @Override
      public boolean onZoom(ZoomEvent event)
      {
        Log.i(TAG, " onZoom level:" + event.getZoomLevel());
        reload_guideposts();
        return false;
      }
    }, 200));

//    create_gp_cluster_overlay(wclusterer);
  }

  void move_to_default_position()
  {
    map_controller.setZoom(13);
    map_controller.zoomTo(13.0);
    map_controller.setCenter(start_point);
    //map_controller.animateTo(start_point);
  }

  void move_to_current_position()
  {
    current_point = start_point;

    if (gps.can_get_location()) {
      gps.get_gps_location();
      double latitude = gps.getLatitude();
      double longitude = gps.getLongitude();
      current_point = new GeoPoint(latitude, longitude);
    }

    map_controller.setZoom(13);
    map_controller.zoomTo(13.0);
    map_controller.setCenter(current_point);
    //map_controller.animateTo(current_point);
  }

  private void create_ui()
  {
    toolbar = findViewById(R.id.toolbar);

    setSupportActionBar(toolbar);

    fab = findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        launch_camera();
      }
    });

    drawer = findViewById(R.id.drawer_layout);

    toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
    );
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    d_cluster_icon = ResourcesCompat.getDrawable(
            getResources(), R.drawable.marker_poi_cluster, null);
    d_poi_icon = ResourcesCompat.getDrawable(getResources(), R.drawable.guidepost, null);
    b_cluster_icon = ((BitmapDrawable) d_cluster_icon).getBitmap();
  }

  private File create_image_file() throws IOException
  {
    File image = null;

    // Create an image file name
    String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
    String image_filename = "GP_" + date + "_";
    File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    try {
      image = File.createTempFile(image_filename, ".jpg", directory);
    } catch (Exception e) {
      Log.e(TAG, "create_image_file(): error " + e.toString());
      e.printStackTrace();
    }
    // Save a file: path for use with ACTION_VIEW intents
    current_photo = image.getAbsolutePath();
    Log.i(TAG, "create_image_file(): " + current_photo);
    return image;
  }

  private void launch_camera()
  {
    Uri photoURI = null;

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      Log.i(TAG, "launch_camera(): launching camera");
      // Create the File where the photo should go
      File image_file = null;
      try {
        image_file = create_image_file();
      } catch (IOException e) {
        // Error occurred while creating the File
        Log.e(TAG, "launch_camera(): error  " + e.getMessage());
      }
      // Continue only if the File was successfully created
      if (image_file != null) {

        try {
          photoURI = FileProvider.getUriForFile(
                  this,
                  "org.walley.guidepost",
                  image_file
                                               );
        } catch (Exception e) {
          Log.e(TAG, "launch_camera(): error  " + e.toString());
        }

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      }
    } else {
      Log.e(TAG, "launch_camera(): no camera");
    }

  }

}
