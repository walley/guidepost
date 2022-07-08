/*
Copyright 2013-2022 Michal Grézl

This file is part of Guidepost android app.

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
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.exifinterface.media.ExifInterface;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.content.res.AssetFileDescriptor;

import java.io.FileNotFoundException;
import java.util.Objects;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
//import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/*START OF CLASS***************************************************************/
public class share extends AppCompatActivity
/*START OF CLASS***************************************************************/
{

  public static final String TAG = "GP-SHARE";
  EditText lat_coord;
  EditText lon_coord;
  Context context;
  ExifInterface exif = null;
  Drawable image = null;
  ImageView image_map;
  Button button_send;
  Button button_cancel;
  DisplayMetrics displaymetrics;
  ViewTreeObserver vto;
  int map_height;
  int map_width;
  wlocation gps;
  private OkHttpClient mHttpClient;
  private Uri uri;
  private EditText text_author;
  ProgressBar pb_upload;
  String server_uri = "https://api.openstreetmap.social/php/guidepost.php";

  /******************************************************************************/
  public String filename_from_uri(Uri content_uri)
  /******************************************************************************/
  {
    String res = null;
    String[] proj = {MediaStore.Images.Media.DATA};

    Cursor cursor = getContentResolver().query(content_uri, null, null, null, null);
    try {
      if (cursor.moveToFirst()) {
        int data_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int name_index = cursor.getColumnIndex("_display_name");
        int size_index = cursor.getColumnIndex("_size");

        if (data_index >= 0) {
          res = cursor.getString(data_index);
        } else if (name_index >= 0) {
          //FIXME dirty hack
          res = "/storage/emulated/0/Android/data/org.walley.guidepost/files/Pictures/" + cursor.getString(
                  name_index);
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "filename_from_uri() failed:" + e.toString());
      e.printStackTrace();
    }
    cursor.close();
    Log.i(TAG, "filename_from_uri() res:" + res);
    return res;
  }

// Read bitmap

  /******************************************************************************/
  public Bitmap read_and_scale(Uri uri)
  /******************************************************************************/
  {
    Bitmap bm = null;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 5;
    int height = options.outHeight;
    int width = options.outWidth;

    AssetFileDescriptor fd = null;

    try {
      fd = this.getContentResolver().openAssetFileDescriptor(uri, "r");
    } catch (FileNotFoundException e) {
      Log.e(TAG, "read_and_scale(): file not found" + e.toString());
    } finally {
      try {
        bm = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
        fd.close();
      } catch (IOException e) {
        Log.e(TAG, "read_and_scale(): io error while decoding" + e.toString());
      }
    }
    return bm;
  }


  /******************************************************************************/
  public void place_map(int map_width, int map_height)
  /******************************************************************************/
  {
    /* this need referrer:(
    String mapic_url = "http://staticmap.openstreetmap.de/staticmap.php?center="
                       + lat_coord.getText()
                       + ","
                       + lon_coord.getText()
                       + "&zoom=14&size="
                       + map_width + "x" + map_height
                       + "&maptype=mapnik&markers="
                       + lat_coord.getText()
                       + ","
                       + lon_coord.getText()
                       + ",lightblue1";
*/
    String mapic_url = ""
            + "https://open.mapquestapi.com/staticmap/v4/getmap?"
            + "key=Fmjtd%7Cluu22qu1nu%2Cbw%3Do5-h6b2h&"
            + "center="
            + lat_coord.getText()
            + ","
            + lon_coord.getText()
            + "&"
            + "zoom=15&size=200,200&type=map&imagetype=png&"
            + "pois=x,"
            + lat_coord.getText()
            + ","
            + lon_coord.getText();

    Log.i(TAG, "place_map:" + mapic_url);

//NOTE nice thing that does the same https://square.github.io/picasso/
    Ion.with(image_map)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .load(mapic_url);

  }

  /******************************************************************************/
  public void prepare_map()
  /******************************************************************************/
  {
    if (image_map.getViewTreeObserver().isAlive()) {
      image_map.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
      {
        @Override
        public void onGlobalLayout()
        {
          if (image_map.getViewTreeObserver().isAlive()) {
            int map_height = image_map.getMeasuredHeight();
            int map_width = image_map.getMeasuredWidth();
            Log.i(TAG, "prepare_map(): imageview dimensions:" + map_width + " x " + map_height);
            place_map(map_width, map_height);
            image_map.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        }
      });
    }
  }

  /******************************************************************************/
  protected void onCreate(Bundle savedInstanceState)
  /******************************************************************************/
  {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    String action = intent.getAction();
    Bitmap bitmap = null;
    setContentView(R.layout.share);

    context = this;

    button_send = (Button) findViewById(R.id.button_send);
    button_cancel = (Button) findViewById(R.id.button_cancel);

    lat_coord = (EditText) findViewById(R.id.lat_coord);
    lon_coord = (EditText) findViewById(R.id.lon_coord);
    text_author = (EditText) findViewById(R.id.text_author);
    image_map = (ImageView) findViewById(R.id.image_map);
    pb_upload = (ProgressBar) findViewById(R.id.pb_upload);

    pb_upload.setProgress(0);

    String author_pref = load_prefs("author");
    if (!Objects.equals(author_pref, "")) {
      text_author.setText(author_pref);
    }

    gps = new wlocation(share.this);

    if (!gps.is_gps_enabled) {
      Toast.makeText(this, "GPS Disabled", Toast.LENGTH_LONG).show();
    }

    button_send.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v)
      {
        String text_lat = (String) lat_coord.getText().toString();
        String text_lon = (String) lon_coord.getText().toString();

        if (get_file_size() > 10000000) {
          messagebox(getResources().getString(R.string.toobig));
          return;
        }

        if (text_lat.equals("0") && text_lon.equals("0")) {
          messagebox();
        } else {
          String author = (String) text_author.getText().toString();
          Log.d("GP", "upload " + author);
          upload_file_with_ion(context, author, text_lat, text_lon);
          //         upload_file_with_okhttp3(context, author, text_lat, text_lon);
          Log.d("GP", "after upload " + author);
        }
      }
    });

    button_cancel.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v)
      {
        Log.i(TAG, "cancel button");

        try {
          ((BitmapDrawable) image).getBitmap().recycle();
        } catch (Exception e) {
          Log.e(TAG, "exception recycle");
        }

        finish();
      }
    });

    /**** map click ****/
    image_map.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v)
      {
        Log.d("GP", "map click");

        if (ActivityCompat.checkSelfPermission(
                share.this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(
                  share.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1337);
          Log.i(TAG, "Permission");
          return;
        }

        if (gps.can_get_location()) {
          gps.get_gps_location();
          double latitude = gps.getLatitude();
          double longitude = gps.getLongitude();
          Toast.makeText(
                  getApplicationContext(),
                  "Your Location is - \nLat: " + latitude + "\nLong: " + longitude,
                  Toast.LENGTH_LONG
                        ).show();
          if (latitude > 0 && longitude > 0) {
            lat_coord.setText(Double.toString(latitude));
            lon_coord.setText(Double.toString(longitude));
          }
        } else {
          Toast.makeText(getApplicationContext(), "cannot get location", Toast.LENGTH_LONG).show();
          gps.showSettingsAlert();
        }
      }
    });

    if (Intent.ACTION_SEND.equals(action)) {
      //FIXME try ION
      assert extras != null;
      if (extras.containsKey(Intent.EXTRA_STREAM)) {

        uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
        get_exif_data(uri);

        try {
          image = new BitmapDrawable(getResources(), read_and_scale(uri));
        } catch (Exception e) {
          Log.e(TAG, "bitmap error");
        } catch (OutOfMemoryError e) {
          image = null; //getResources().getDrawable(R.drawable.oom);
          Toast.makeText(this, "Cannot show image, out of memory", Toast.LENGTH_LONG).show();
        }

        ImageView thumbnail = (ImageView) findViewById(R.id.image);

        thumbnail.setImageDrawable(image);

        image_map.post(new Runnable()
        {
          @Override
          public void run()
          {
            prepare_map();
          }
        });
      }
    }
  }

  /******************************************************************************/
  protected void onStop()
  /******************************************************************************/
  {
    super.onStop();
    gps.stop_using_gps();
  }

  /******************************************************************************/
  @SuppressLint("SetTextI18n")
  public void set_image_location()
  /******************************************************************************/
  {
    gps.get_gps_location();
    double latitude = gps.getLatitude();
    double longitude = gps.getLongitude();
    Toast.makeText(
            getApplicationContext(),
            "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG
                  ).show();
    if (latitude > 0 && longitude > 0) {
      lat_coord.setText(Double.toString(latitude));
      lon_coord.setText(Double.toString(longitude));
    }

  }

  /******************************************************************************/
  public void get_exif_data(Uri uri)
  /******************************************************************************/
  {
    float[] output = new float[2];
    String model;

    Toast.makeText(
            this, getResources().getString(R.string.selectedfile) + uri.toString(),
            Toast.LENGTH_LONG
                  ).show();
    Log.i(TAG, getResources().getString(R.string.selectedfile) + " " + uri.toString());

    try {
      exif = new ExifInterface(filename_from_uri(uri));
    } catch (Exception e) {
      Log.e(TAG, "exif interface error " + e.toString());
    }

    try {
      model = exif.getAttribute(ExifInterface.TAG_MODEL);
      Log.i(TAG, "get_exif_data(): get model:" + model);
    } catch (Exception e) {
      Log.e(TAG, "get_exif_data(): getattribute error: " + e.toString());
    }

    try {
      double[] x;
//      if (exif.getLatLong(output)) {
//        float lat = output[0];
//        float lon = output[1];
      if (exif.getLatLong() != null) {
        x = exif.getLatLong();
        double lat = x[0];
        double lon = x[1];
        lat_coord.setText(Double.toString(lat));
        lon_coord.setText(Double.toString(lon));
        Log.i(TAG, "get_exif_data():" + lat + "," + lon);
      } else {
        Log.i(TAG, "get_exif_data():getlatlong returned false");
        lat_coord.setText("0");
        lon_coord.setText("0");
      }
    } catch (Exception e) {
      Log.e(TAG, "get_exif_data(): getlatlong error: " + e.toString());
    }
  }


  /******************************************************************************/
  public void store_author(String author)
  /******************************************************************************/
  {
    store_prefs("author", author);
  }

  /******************************************************************************/
  public void post_execute(String data)
  /******************************************************************************/
  {
    Log.i(TAG, "post_execute(" + data + ")");
    String[] result = data.split("-");
    Log.i(TAG, "post_execute: " + result.toString());
    if (result[0].equals("0")) {
      Log.e(TAG, "upload error:" + result[1]);
      alert(
              getResources().getString(R.string.uploadfailed),
              getResources().getString(R.string.uploaderror) + " " + result[1] + "."
           );

    } else {
      Toast.makeText(
              getBaseContext(), getResources().getString(R.string.uploaded),
              Toast.LENGTH_LONG
                    ).show();

      try {
        ((BitmapDrawable) image).getBitmap().recycle();
      } catch (Exception e) {
        Log.e(TAG, "bitmap recycle error" + e.toString());
      }

      finish();
    }
  }

  /******************************************************************************/
  public long get_file_size()
  /******************************************************************************/
  {
    File fi = new File(filename_from_uri(uri));
    return fi.length();
  }

  public void upload_file_with_okhttp3(Context context, String author, String lat, String lon)
  {

    File file = new File(filename_from_uri(uri));

    OkHttpClient client = new OkHttpClient();

    try {

      RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
              .addFormDataPart(
                      "uploadedfile", file.getName(),
                      RequestBody.create(file, MediaType.parse("image/jpeg"))
                              )
              .addFormDataPart("action", "file")
              .addFormDataPart("source", "mobile")
              .addFormDataPart("author", author)
              .addFormDataPart("lat", lat)
              .addFormDataPart("lon", lon)
              .addFormDataPart("license", "CCBYSA4")
              .build();

      Request request = new Request.Builder()
              .url(server_uri)
              .post(requestBody)
              .build();

      client.newCall(request).enqueue(new Callback()
      {

        @Override
        public void onFailure(final Call call, final IOException e)
        {
          // Handle the error
        }

        @Override
        public void onResponse(final Call call, final Response result) throws IOException
        {
          Log.i(TAG, "upload_file_with_okhttp3():Sent, " + result.toString());
          int status = result.code();
          runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              Toast.makeText(getApplicationContext(), "SMS Sent!", Toast.LENGTH_SHORT).show();
              if (status == 200) {
                post_execute(result.toString());
                Log.i(TAG, "upload_file_with_okhttp3(): Done");
              } else {
                Log.i(TAG, "upload_file_with_okhttp3(): not successful");
              }
            }
          });
        }
      });

    } catch (Exception ex) {
      // Handle the error
    }
  }

  public void upload_file_with_okhttp3_and_progress()
  {
  }

  /******************************************************************************/
  public void upload_file_with_ion(Context context, String author, String lat, String lon)
  /******************************************************************************/
  {
    CheckBox check_remember = (CheckBox) findViewById(R.id.check_remember);
    if (check_remember.isChecked()) {
      store_author(author);
    }
    Log.d(TAG, "upload_ion(): a: " + author + " lat:" + lat + " lon:" + lon);
    File fi = new File(filename_from_uri(uri));

    Ion.with(context)
            .load("POST", server_uri)
            .setLogging(TAG + "ion", Log.VERBOSE)
            .uploadProgressHandler(new ProgressCallback()
            {
              @Override
              public void onProgress(long uploaded, long total)
              {
                float percent = (uploaded * 100) / total;
                //Log.d(TAG, "uploaded " + (int) uploaded + "/" + total + ":" + percent + "%");
                pb_upload.setProgress(Math.round(percent));
              }
            })
            .setMultipartParameter("yo", "yay")
            .setMultipartParameter("action", "file")
            .setMultipartParameter("source", "mobile")
            .setMultipartParameter("author", author)
            .setMultipartParameter("lat", lat)
            .setMultipartParameter("lon", lon)
            .setMultipartParameter("license", "CCBYSA4")
            .setMultipartFile("uploadedfile", fi)
            .asString()
            .setCallback(new FutureCallback<String>()
            {
              @Override
              public void onCompleted(Exception e, String result)
              {
                Log.i(TAG, "upload_file_with_ion():Completed");
                post_execute(result);
              }
            });
  }

  /******************************************************************************/
  public void store_prefs(String key, String value)
  /******************************************************************************/
  {
    SharedPreferences.Editor prefs;
    prefs = getApplicationContext().getSharedPreferences("guidepost", 0).edit();
    prefs.putString(key, value);
    prefs.apply();
  }

  /******************************************************************************/
  public String load_prefs(String key)
  /******************************************************************************/
  {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("guidepost", 0);
    return prefs.getString(key, "");
  }

  /******************************************************************************/
  public void alert(String title, String message)
  /******************************************************************************/
  {
    new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                    getResources().getString(R.string.tryagain),
                    new DialogInterface.OnClickListener()
                    {
                      public void onClick(DialogInterface dialog, int which)
                      {
                        dialog.cancel();
                      }
                    }
                              )
            .setNegativeButton(
                    getResources().getString(R.string.bye), new DialogInterface.OnClickListener()
                    {
                      public void onClick(DialogInterface dialog, int which)
                      {

                        try {
                          ((BitmapDrawable) image).getBitmap().recycle();
                        } catch (Exception e) {
                          Log.e(TAG, e.toString());
                        }

                        share.this.finish();
                      }
                    })
            .show();
  }

  /******************************************************************************/
  public void messagebox()
  /******************************************************************************/
  {
    new AlertDialog.Builder(this)
            .setMessage(getResources().getString(R.string.wrongcoords))
            .setCancelable(false)
            .setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int id)
              {
              }
            })
            .show();
  }

  /******************************************************************************/
  public void messagebox(String s)
  /******************************************************************************/
  {
    new AlertDialog.Builder(this)
            .setMessage(s)
            .setCancelable(false)
            .setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int id)
              {
              }
            })
            .show();
  }

  @Override
  public void onRequestPermissionsResult(int request, String permissions[], int[] results)
  {
//PERMISSION_GRANTED Constant Value: 0 (0x00000000)
//PERMISSION_DENIED Constant Value: -1 (0xffffffff)
    Log.i(TAG, "request:" + request);
    if (request == 1337) {
      Log.i(TAG, "Received response for contact permissions request.");
      Log.i(TAG, "l:" + permissions.length);
      for (int i = 0; i < permissions.length; i++) {
        Log.i(TAG, "perm,res:" + permissions[i] + results[i]);
        switch (permissions[i]) {
          case "android.permission.ACCESS_FINE_LOCATION":
            if (results[i] == PERMISSION_GRANTED) {
              set_image_location();
            }
            break;
          default:
            break;
        }
      }
    } else {
      Log.e(TAG, "not our request?");
      super.onRequestPermissionsResult(request, permissions, results);
    }
  }

}
/*end of class*/

