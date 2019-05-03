package org.walley.guidepost;

import android.content.Context;

import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * {@link org.osmdroid.views.overlay.infowindow.MarkerInfoWindow} is the default
 * implementation of {@link org.osmdroid.views.overlay.infowindow.InfoWindow} for a
 * {@link org.osmdroid.views.overlay.Marker}.
 * <p>
 * It handles
 * <p>
 * R.id.bubble_title          = {@link org.osmdroid.views.overlay.OverlayWithIW#getTitle()},
 * R.id.bubble_subdescription = {@link org.osmdroid.views.overlay.OverlayWithIW#getSubDescription()},
 * R.id.bubble_description    = {@link org.osmdroid.views.overlay.OverlayWithIW#getSnippet()},
 * R.id.bubble_image          = {@link org.osmdroid.views.overlay.Marker#getImage()}
 * <p>
 * Description and sub-description interpret HTML tags (in the limits of the Html.fromHtml(String) API).
 * Clicking on the bubble will close it.
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='./doc-files/marker-infowindow-classes.png' />
 *
 * @author M.Kergall
 */
public class winfowindow extends BasicInfoWindow
{

  private static final String TAG = "GP-winfowindow";
  String html_text;
  WebView wv;

  protected Marker mMarkerRef; //reference to the Marker on which it is opened. Null if none.

  /**
   * @param layoutResId layout that must contain these ids: bubble_title,bubble_description,
   *                    bubble_subdescription, bubble_image
   * @param mapView
   */

  public winfowindow(int layoutResId, MapView mapView)
  {
    super(layoutResId, mapView);
    //mMarkerRef = null;
    Log.d(TAG, "winfowindow contructor");
  }

  /**
   * reference to the Marker on which it is opened. Null if none.
   *
   * @return
   */
  public Marker getMarkerReference()
  {
    return mMarkerRef;
  }

  @Override
  public void onOpen(Object item)
  {

    //super.onOpen(item);
    OverlayWithIW overlay = (OverlayWithIW) item;
    String title = overlay.getTitle();
    if (title == null)
      title = "";
    if (mView == null) {
      Log.w(IMapView.LOGTAG, "Error trapped, BasicInfoWindow.open, mView is null!");
      return;
    }

    Log.w(TAG, "marker onopen");

    mMarkerRef = (Marker) item;
    if (mView == null) {
      Log.w(TAG, "InfoWindow.open, mView is null!");
      return;
    }
    //handle image

    wv = mView.findViewById(R.id.cluster_bubble_webview);
    if (wv != null) {
      wv.loadData(html_text, "text/html", "UTF-8");
      Log.i(TAG, "html vewbview created");
    } else {
      Log.e(TAG, "set_html temp is null");
    }

    ImageView imageView = (ImageView) mView.findViewById(R.id.bubble_image);

    Drawable image = mMarkerRef.getImage();
    if (image != null) {
      imageView.setImageDrawable(image); //or setBackgroundDrawable(image)?
      imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      imageView.setVisibility(View.VISIBLE);
    } else
      imageView.setVisibility(View.GONE);
  }

  public void set_text(String t)
  {
    TextView temp = ((TextView) mView.findViewById(R.id.cluster_bubble_text));
    if (temp != null) {
      temp.setText(t);
      Log.i(TAG, "settext:" + t);

    } else {
      Log.e(TAG, "settext: textview is null");
    }
  }

  public void set_html(String t)
  {
    html_text = t;
  }

  @Override
  public void onClose()
  {
    super.onClose();
    mMarkerRef = null;

    Log.i(TAG, "marker on close");

    if (wv != null) {
      wv.destroy();
      Log.i(TAG, "vewbview destroyed");
    } else {
      Log.e(TAG, "webview is null");
    }

    //by default, do nothing else
  }

}

