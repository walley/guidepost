package org.walley.guidepost;

import android.content.Context;

import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
    Log.w("GP", "marker contructor");
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

    super.onOpen(item);

    Log.w("GP", "marker onopen");

    mMarkerRef = (Marker) item;
    if (mView == null) {
      Log.w("GP", "InfoWindow.open, mView is null!");
      return;
    }
    //handle image
    ImageView imageView = (ImageView) mView.findViewById(R.id.bubble_image);
    Drawable image = mMarkerRef.getImage();
    if (image != null) {
      imageView.setImageDrawable(image); //or setBackgroundDrawable(image)?
      imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      imageView.setVisibility(View.VISIBLE);
    } else
      imageView.setVisibility(View.GONE);
  }

  @Override
  public void onClose()
  {
    super.onClose();
    mMarkerRef = null;

    Log.w("GP", "marker on close");


    //by default, do nothing else
  }

}

