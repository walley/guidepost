//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.walley.guidepost;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.Iterator;
import org.osmdroid.bonuspack.R.drawable;
import org.osmdroid.bonuspack.clustering.MarkerClusterer;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

public class wclusterer extends MarkerClusterer {
  protected int mMaxClusteringZoomLevel = 17;
  protected int mRadiusInPixels = 100;
  protected double mRadiusInMeters;
  protected Paint mTextPaint = new Paint();
  private ArrayList<Marker> mClonedMarkers;
  public float mAnchorU = 0.5F;
  public float mAnchorV = 0.5F;
  public float mTextAnchorU = 0.5F;
  public float mTextAnchorV = 0.5F;

  public wclusterer(Context ctx) {
    this.mTextPaint.setColor(-1);
    this.mTextPaint.setTextSize(15.0F * ctx.getResources().getDisplayMetrics().density);
    this.mTextPaint.setFakeBoldText(true);
    this.mTextPaint.setTextAlign(Align.CENTER);
    this.mTextPaint.setAntiAlias(true);
    Drawable clusterIconD = ctx.getResources().getDrawable(drawable.marker_cluster);
    Bitmap clusterIcon = ((BitmapDrawable)clusterIconD).getBitmap();
    this.setIcon(clusterIcon);
  }

  public Paint getTextPaint() {
    return this.mTextPaint;
  }

  public void add(Marker marker) {
    this.mItems.add(marker);
  }

  public void setRadius(int radius) {
    this.mRadiusInPixels = radius;
  }

  public void setMaxClusteringZoomLevel(int zoom) {
    this.mMaxClusteringZoomLevel = zoom;
  }

  public ArrayList<StaticCluster> clusterer(MapView mapView) {
    ArrayList<StaticCluster> clusters = new ArrayList();
    this.convertRadiusToMeters(mapView);
    this.mClonedMarkers = new ArrayList(this.mItems);

    while(!this.mClonedMarkers.isEmpty()) {
      Marker m = (Marker)this.mClonedMarkers.get(0);
      StaticCluster cluster = this.createCluster(m, mapView);
      clusters.add(cluster);
    }

    return clusters;
  }

  private StaticCluster createCluster(Marker m, MapView mapView) {
    GeoPoint clusterPosition = m.getPosition();
    StaticCluster cluster = new StaticCluster(clusterPosition);
    cluster.add(m);
    this.mClonedMarkers.remove(m);
    if (mapView.getZoomLevel() > this.mMaxClusteringZoomLevel) {
      return cluster;
    } else {
      Iterator it = this.mClonedMarkers.iterator();

      while(it.hasNext()) {
        Marker neighbour = (Marker)it.next();
        double distance = clusterPosition.distanceToAsDouble(neighbour.getPosition());
        if (distance <= this.mRadiusInMeters) {
          cluster.add(neighbour);
          it.remove();
        }
      }

      return cluster;
    }
  }

  public Marker buildClusterMarker(StaticCluster cluster, MapView mapView) {
    Marker m = new Marker(mapView);
    m.setPosition(cluster.getPosition());
    winfowindow wi = new winfowindow(R.layout.bonuspack_bubble,mapView);
    m.setInfoWindow(wi);
    m.setAnchor(this.mAnchorU, this.mAnchorV);
    Bitmap finalIcon = Bitmap.createBitmap(this.mClusterIcon.getWidth(), this.mClusterIcon.getHeight(), this.mClusterIcon.getConfig());
    Canvas iconCanvas = new Canvas(finalIcon);
    iconCanvas.drawBitmap(this.mClusterIcon, 0.0F, 0.0F, (Paint)null);
    String text = "" + cluster.getSize();
    int textHeight = (int)(this.mTextPaint.descent() + this.mTextPaint.ascent());
    iconCanvas.drawText(text, this.mTextAnchorU * (float)finalIcon.getWidth(), this.mTextAnchorV * (float)finalIcon.getHeight() - (float)(textHeight / 2), this.mTextPaint);
    m.setIcon(new BitmapDrawable(mapView.getContext().getResources(), finalIcon));
    return m;
  }

  public void renderer(ArrayList<StaticCluster> clusters, Canvas canvas, MapView mapView) {
    Iterator var4 = clusters.iterator();

    while(var4.hasNext()) {
      StaticCluster cluster = (StaticCluster)var4.next();
      if (cluster.getSize() == 1) {
        cluster.setMarker(cluster.getItem(0));
      } else {
        Marker m = this.buildClusterMarker(cluster, mapView);
        cluster.setMarker(m);
      }
    }

  }

  private void convertRadiusToMeters(MapView mapView) {
    Rect mScreenRect = mapView.getIntrinsicScreenRect((Rect)null);
    int screenWidth = mScreenRect.right - mScreenRect.left;
    int screenHeight = mScreenRect.bottom - mScreenRect.top;
    BoundingBox bb = mapView.getBoundingBox();
    double diagonalInMeters = bb.getDiagonalLengthInMeters();
    double diagonalInPixels = Math.sqrt((double)(screenWidth * screenWidth + screenHeight * screenHeight));
    double metersInPixel = diagonalInMeters / diagonalInPixels;
    this.mRadiusInMeters = (double)this.mRadiusInPixels * metersInPixel;
  }
}

