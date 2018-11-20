/*
Copyright 2013-2018 Michal Grézl

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
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class basic extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{

  static final int REQUEST_IMAGE_CAPTURE = 1;
  public static final String TAG = "GP-B";
  private static final int RESULT_SETTINGS = 1;

  ActionBarDrawerToggle toggle;
  Bitmap b_cluster_icon;
  Context context = this;
  Drawable d_cluster_icon;
  Drawable d_poi_icon;
  DrawerLayout drawer;
  FloatingActionButton fab;
  GeoPoint start_point = new GeoPoint(49.5, 17.0);
  IMapController map_controller;
  int gp_overlay_number;
  List<IGeoPoint> points = new ArrayList<>();
  MapView map = null;
  MyLocationNewOverlay location_overlay;
  NavigationView navigationView;
  //  RadiusMarkerClusterer gp_marker_cluster;
  wclusterer gp_marker_cluster;
  Toolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.basic);

    Ion.getDefault(context).configure().setLogging(TAG, Log.INFO);
    request_permission();
    create_ui();

    if (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
      Toast.makeText(context, "I need WRITE_EXTERNAL_STORAGE, sorry", Toast.LENGTH_SHORT).show();
      //  finish();
    } else {
      create_map();
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
      map_controller.zoomTo(14.0);
      map_controller.setCenter(start_point);
    } else if (id == R.id.nav_slideshow) {

    } else if (id == R.id.nav_manage) {

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
            != PackageManager.PERMISSION_GRANTED)

    {

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
    } else

    {
      // Permission has already been granted
    }
  }

  private void create_gp_cluster_overlay()
  {
//    gp_marker_cluster = new RadiusMarkerClusterer(context);
    gp_marker_cluster = new wclusterer(context);
    gp_marker_cluster.setIcon(b_cluster_icon);
    gp_marker_cluster.getTextPaint().setTextSize(12 * getResources().getDisplayMetrics().density);
    gp_marker_cluster.mAnchorV = Marker.ANCHOR_BOTTOM;
    gp_marker_cluster.mTextAnchorU = 0.70f;
    gp_marker_cluster.mTextAnchorV = 0.27f;

    map.getOverlays().add(gp_marker_cluster);
    gp_overlay_number = map.getOverlays().size() - 1;
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
            .load("https://api.openstreetmap.social/table/all?output=json&bbox=" + bbox_param.toString())
            .asJsonArray()
            .setCallback(new FutureCallback<JsonArray>()
            {
              @Override
              public void onCompleted(Exception ione, JsonArray result)
              {

                if (ione != null) {
                  Toast.makeText(context, "ion json Error", Toast.LENGTH_LONG).show();
                  Log.e(TAG, "ion exception" + ione.toString());
                  return;
                }

                try {
                  map.getOverlays().remove(gp_overlay_number);
                } catch (Exception e) {
                  Log.e(TAG, "cannot remove overlay" + e.toString());
                }

                create_gp_cluster_overlay();

                JsonArray item_json;
                // do stuff with the result or error
                Log.i(TAG, "bbox json loaded ");
                if (result == null) {
                  Log.i(TAG, "bbox json is null");
                }
                Iterator it = result.iterator();
                while (it.hasNext()) {
                  JsonElement element = (JsonElement) it.next();
                  item_json = element.getAsJsonArray();
                  try {
                    int id = item_json.get(0).getAsInt();
                    double lat = item_json.get(1).getAsDouble();
                    double lon = item_json.get(2).getAsDouble();
                    String img = item_json.get(3).getAsString();
                    //double lon = item_json.get(4).getAsDouble();
                    String author = item_json.get(5).getAsString();
                    String ref = item_json.get(6).getAsString();
                    String note = item_json.get(7).getAsString();
                    String license = item_json.get(8).getAsString();
                    String tags = item_json.get(9).getAsString();

                    points.add(new LabelledGeoPoint(lat, lon, "gp " + id + " " + ref));

                    GeoPoint poi_loc = new GeoPoint(lat, lon);

                    final Marker gp_marker = new Marker(map);
                    gp_marker.setTitle("gp: " + id);
                    gp_marker.setSubDescription("" + img);
                    gp_marker.setSnippet(
                            "img:" + img + "<br>author:" + author + "<br>ref:" + ref + "<br>tags:" + tags);
                    gp_marker.setPosition(poi_loc);
                    gp_marker.setIcon(d_poi_icon);
                    gp_marker.setImage(d_poi_icon);

/*                    Ion.with(context)
                       .load("http://api.openstreetmap.social/p/phpThumb.php?h=150&src=http://api.openstreetmap.social/" + img)
//                            .load("http://api.openstreetmap.social/" + img)
                       .withBitmap()
                       .asBitmap()
                       .setCallback(new FutureCallback<Bitmap>() {
                         @Override
                         public void onCompleted(Exception exception, Bitmap b) {
                          Drawable d = new BitmapDrawable(getResources(), b);
                          gp_marker.setImage(d);
                         }
                       });
*/
                    gp_marker_cluster.add(gp_marker);

//                    Log.i(TAG, "added latlon:" + lat + "," + lon + ":" + id);
                    map.invalidate();
                  } catch (Exception e) {
                    Log.e(TAG, "exception adding " + e.toString());

                  }

                }
                Log.i(TAG, "json done");

              }
            });

  }

  private void create_map()
  {
    map = findViewById(R.id.map);
    map_controller = map.getController();
    map_controller.zoomTo(14.0);
    map_controller.setCenter(start_point);

    location_overlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
    location_overlay.enableMyLocation();
    map.getOverlays().add(location_overlay);

    map.setTileSource(TileSourceFactory.MAPNIK);
    map.setBuiltInZoomControls(true);
    map.setMultiTouchControls(true);

    map.addMapListener(new DelayedMapListener(new MapListener()
    {
      @Override
      public boolean onScroll(ScrollEvent event)
      {
        Log.i(TAG, "onScroll " + event.getX() + "," + event.getY());
        Toast.makeText(context, "after " + map.getOverlays().size(), Toast.LENGTH_SHORT).show();
        reload_guideposts();
        return false;
      }

      @Override
      public boolean onZoom(ZoomEvent event)
      {
        Log.i(TAG, " onZoom " + event.getZoomLevel());
        Toast.makeText(
                context, "after zoom " + map.getOverlays().size(), Toast.LENGTH_SHORT).show();
        reload_guideposts();
        return false;
      }
    }, 200));


    map.addMapListener(new MapListener()
    {
      @Override
      public boolean onScroll(ScrollEvent event)
      {
        return true;
      }

      @Override
      public boolean onZoom(ZoomEvent event)
      {
        return true;
      }
    });

    create_gp_cluster_overlay();

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
        Snackbar.make(view, "Yo! this will launch camera", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
          startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
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
    d_poi_icon = ResourcesCompat.getDrawable(getResources(), R.drawable.marked_trail_red, null);
    b_cluster_icon = ((BitmapDrawable) d_cluster_icon).getBitmap();
  }
}
