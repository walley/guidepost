package org.walley.guidepost;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;


public class winfowindow extends MarkerInfoWindow
{

  private static final String TAG = "GP-winfowindow";

  public winfowindow(int layoutResId, MapView mapView)
  {
    super(layoutResId, mapView);
  }

  @Override
  public void onOpen(Object item)
  {
    OverlayWithIW overlay = (OverlayWithIW) item;
    String title = overlay.getTitle();
    if (title == null) {
      title = "";
    }

    Log.w(TAG, "marker onopen");

    TextView tv = ((TextView) mView.findViewById(R.id.cluster_bubble_text));
    if (tv != null) {
      tv.setText("test");
    } else {
      Log.e(TAG, "textview is null");
    }

    String html_text = overlay.getSnippet();
    WebView wv = mView.findViewById(R.id.cluster_bubble_webview);
    if (wv != null) {
      wv.loadData(html_text, "text/html; charset=utf-8", "utf-8");
      Log.i(TAG, "html:" + html_text);
      Log.i(TAG, "html vewbview created");
    } else {
      Log.e(TAG, "set_html temp is null");
    }

  }

  @Override
  public void onClose()
  {
    Log.i(TAG, "marker on close");

    WebView wv = mView.findViewById(R.id.cluster_bubble_webview);

    if (wv != null) {
      wv.loadUrl("about:blank");
      Log.i(TAG, "vewbview cleared");
    } else {
      Log.e(TAG, "webview is null");
    }
  }
}

