/*
Copyright 2013-2018 Michal Grézl

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
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import org.walley.guidepost.cme.ProgressListener;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.content.res.AssetFileDescriptor;
import java.io.FileNotFoundException;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.util.DisplayMetrics;

/*START OF CLASS***************************************************************/
public class share extends Activity
/*START OF CLASS***************************************************************/
{

  private DefaultHttpClient mHttpClient;
  private Uri uri;
  EditText lat_coord;
  EditText lon_coord;
  Context context;
  private EditText text_author;
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

  /******************************************************************************/
  public void entity_consume_content(HttpEntity entity)
  /******************************************************************************/
  {
    if (entity != null) {
      try {
        entity.consumeContent();
      } catch (IOException e) {
        Log.e("WC","entity_consume_content error " + e.toString());
      }
    }
  }

  /******************************************************************************/
  public String filename_from_uri(Uri content_uri)
  /******************************************************************************/
  {
    try {
      String[] proj = {MediaStore.Images.Media.DATA};
      Cursor cursor = managedQuery(content_uri, proj, null, null, null);
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
    } catch (Exception e) {
      Log.e("GP","filename_from_uri failed");
      return content_uri.getPath();
    }
  }

  /******************************************************************************/
  public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
  /******************************************************************************/
  {
    // Raw height and width of image - this does not work yet
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
      final int halfHeight = height / 2;
      final int halfWidth = width / 2;
      // Calculate the largest inSampleSize value that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqHeight
             && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }

// Read bitmap
  /******************************************************************************/
  public Bitmap read_and_scale(Uri uri)
  /******************************************************************************/
  {
    Bitmap bm = null;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 5; //calculateInSampleSize(options, 100, 100);
    int height = options.outHeight;
    int width = options.outWidth;

    AssetFileDescriptor fd = null;

    try {
      fd = this.getContentResolver().openAssetFileDescriptor(uri, "r");
    } catch (FileNotFoundException e) {
      Log.e("GP", "read_and_scale(): file not found" + e.toString());
    } finally {
      try {
        bm = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
        fd.close();
      } catch (IOException e) {
        Log.e("GP", "read_and_scale(): io error while decoding" + e.toString());
      }
    }
    return bm;
  }


  /******************************************************************************/
  public void place_map(int map_width, int map_height)
  /******************************************************************************/
  {
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

    Log.i("GP", mapic_url);

    UrlImageViewHelper.setUrlDrawable(image_map, mapic_url, R.drawable.placeholder);
  }

  /******************************************************************************/
  public void prepare_map()
  /******************************************************************************/
  {
    if (image_map.getViewTreeObserver().isAlive()) {
      image_map.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          if(image_map.getViewTreeObserver().isAlive()) {
            int map_height = image_map.getMeasuredHeight();
            int map_width = image_map.getMeasuredWidth();
            Log.i("GP", "1 imageview dimensions:" + map_width + " x " + map_height);
            place_map(map_width, map_height);
            image_map.getViewTreeObserver().removeGlobalOnLayoutListener(this);
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

    String author_pref = load_prefs("author");
    if (author_pref != "") {
      text_author.setText(author_pref);
    }

    gps = new wlocation(share.this);

    if (!gps.is_gps_enabled) {
      Toast.makeText(this, "GPS Disabled", Toast.LENGTH_LONG).show();
    }

    button_send.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
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
          Log.d("GP","upload " + author);
          upload_file(context, author, text_lat, text_lon);
          Log.d("GP","after upload " + author);
        }
      }
    });

    button_cancel.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Log.d("GP","cancel button");

        try {
          ((BitmapDrawable)image).getBitmap().recycle();
        } catch(Exception e) {
        }

        finish();
      }
    });

    image_map.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Log.d("GP","map click");

        if (ActivityCompat.checkSelfPermission(share.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(share.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
          Log.i("GP", "Permission");
          return;
        }//FIXME - permission

        if (gps.canGetLocation()) {
           double latitude = gps.getLatitude();
           double longitude = gps.getLongitude();
           Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
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
      if (extras.containsKey(Intent.EXTRA_STREAM)) {

        uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
        get_exif_data(uri);

        try {
          image = new BitmapDrawable(getResources(), read_and_scale(uri));
//        image = null;
//        image = new BitmapDrawable(getResources(), MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri));
        } catch(Exception e) {
          Log.e("GP","bitmap error");
        } catch (OutOfMemoryError e) {
          image = null; //getResources().getDrawable(R.drawable.oom);
          Toast.makeText(this, "Cannot show image, out of memory", Toast.LENGTH_LONG).show();
        }

        ImageView thumbnail = (ImageView) findViewById(R.id.image);

        thumbnail.setImageDrawable(image);

        image_map.post(new Runnable() {
          @Override
          public void run() {
            prepare_map();
          }
        });

      }
    }
  }

  /******************************************************************************/
  public void get_exif_data(Uri uri)
  /******************************************************************************/
  {
    float[] output = new float[2];
    String model;

    Toast.makeText(this, getResources().getString(R.string.selectedfile) + uri.toString(), Toast.LENGTH_LONG).show();

    try {
      exif = new ExifInterface(filename_from_uri(uri));
    } catch(Exception e) {
      Log.e("GP","exif interface error " + e.toString());
    }

    try {
      model = exif.getAttribute(ExifInterface.TAG_MODEL);
      Log.i("GP","exif get model:" + model);
    } catch(Exception e) {
      Log.e("GP","exif getattribute error: " + e.toString());
    }

    try {
      if (exif.getLatLong(output)) {
        float lat = output[0];
        float lon = output[1];
        lat_coord.setText(Float.toString(lat));
        lon_coord.setText(Float.toString(lon));
      } else {
        Log.i("GP","getlatlong returned false");
        lat_coord.setText("0");
        lon_coord.setText("0");
      }
    } catch(Exception e) {
      Log.e("GP","exif getlatlong error: " + e.toString());
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
    Log.i("GP", "in mainactivity" + data);
    String[] result = data.split("-");
    if (result[0].equals("0")) {
      Log.e("GP", "upload error:" + result[1]);
      alert(getResources().getString(R.string.uploadfailed),
            getResources().getString(R.string.uploaderror)  + " " + result[1] + "."
           );

    } else {
      Toast.makeText(getBaseContext(), getResources().getString(R.string.uploaded), Toast.LENGTH_LONG).show();

      try {
        ((BitmapDrawable)image).getBitmap().recycle();
      } catch(Exception e) {
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

  /******************************************************************************/
  public void upload_file(Context context, String author, String lat, String lon)
  /******************************************************************************/
  {
    CheckBox check_remember = (CheckBox) findViewById(R.id.check_remember);
    if (check_remember.isChecked()) {
      store_author(author);
    }

    if (context == null) {
      Log.e("GP","null context");
    }

    File fi = new File(filename_from_uri(uri));

    HttpMultipartPost hmp = new HttpMultipartPost(context, author, lat, lon, fi);
    hmp.execute();
    Log.d("GP","after execute");
  }

  /******************************************************************************/
  public void store_prefs(String key, String value)
  /******************************************************************************/
  {
    SharedPreferences.Editor prefs;
    prefs = getApplicationContext().getSharedPreferences("guidepost", 0).edit();
    prefs.putString(key, value);
    prefs.commit();
  }

  /******************************************************************************/
  public String load_prefs(String key)
  /******************************************************************************/
  {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("guidepost", 0);
    return prefs.getString(key, "");
  }

  /******************************************************************************/
  public class HttpMultipartPost extends AsyncTask<String, Integer, String>
  /******************************************************************************/
  {
    private Context context;
    private String author;
    private String lat;
    private String lon;
    private File image;
    private ProgressDialog pd;
    private long post_total_size;

    private HttpClient httpClient;
    private HttpContext httpContext;
    private HttpPost post_request;
    private HttpResponse response;

    public HttpMultipartPost(Context context, String author, String lat, String lon, File image)
    {
      this.context = context;
      this.author = author;
      this.lat = lat;
      this.lon = lon;
      this.image = image;
    }

    @Override
    protected void onPreExecute()
    {

      if (context == null) {
        Log.e("GP","null context");
      }

      pd = new ProgressDialog(context);
      pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      String uploading_string = getResources().getString(R.string.uploading);
      pd.setMessage(uploading_string);
      pd.setCancelable(false);

      // Set a click listener for progress dialog cancel button
      pd.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          pd.dismiss();
          try {
            post_request.abort();
          } catch(Exception e) {
            Log.e("GP","post abort failed: " + e.toString());
          }
        }
      });

      pd.show();
    }

    @Override
    protected String doInBackground(String... params)
    {
      String serverResponse = null;
      try {
        httpClient = new DefaultHttpClient();
        httpContext = new BasicHttpContext();
        HttpEntity entity = null;

        String serverPath = "http://api.openstreetmap.social/php/guidepost.php";
        post_request = new HttpPost(serverPath);

        cme multipart_content = new cme(
        new ProgressListener() {
          @Override
          public void transferred(long value) {
            publishProgress((int) ((value / (float) post_total_size) * 100));
          }
        });

        multipart_content.addPart("action", new StringBody("file"));
        multipart_content.addPart("source", new StringBody("mobile"));
        multipart_content.addPart("author", new StringBody(author));
        multipart_content.addPart("lat", new StringBody(lat));
        multipart_content.addPart("lon", new StringBody(lon));
        multipart_content.addPart("license", new StringBody("CCBYSA4"));
        multipart_content.addPart("uploadedfile", new FileBody(image));

        post_total_size = multipart_content.getContentLength();

        post_request.setEntity(multipart_content);
        response = httpClient.execute(post_request, httpContext);
        entity = response.getEntity();
        serverResponse = EntityUtils.toString(entity);
        entity_consume_content(entity);

        Log.i("GP", "doInBackground server response:" + serverResponse);

      } catch (Exception e) {
        e.printStackTrace();
        serverResponse = "0-client side error uploading";
      }

      return serverResponse;
    }

    @Override
    protected void onProgressUpdate(Integer... progress)
    {
      pd.setProgress((int) (progress[0]));
    }

    @Override protected void onPostExecute(String result)
    {
      Log.i("GP", "onPostExecute" + result);
      pd.dismiss();
      post_execute(result);
    }

    @Override
    protected void onCancelled()
    {
      post_execute("0-canceled");
    }

  }

  /******************************************************************************/
  public void alert(String title, String message)
  /******************************************************************************/
  {
    new AlertDialog.Builder(this)
    .setTitle(title)
    .setMessage(message)
    .setPositiveButton(getResources().getString(R.string.tryagain), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
      }
    })
    .setNegativeButton(getResources().getString(R.string.bye), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {

        try {
          ((BitmapDrawable)image).getBitmap().recycle();
        } catch(Exception e) {
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
    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
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
    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
      }
    })
    .show();
  }

  /*end of class*/
}
/*end of class*/

