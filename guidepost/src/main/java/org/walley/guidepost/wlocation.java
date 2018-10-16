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

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class wlocation extends Service implements LocationListener
{

  private final Context mContext;

  boolean is_gps_enabled = false;
  boolean is_network_enabled = false;
  boolean can_get_location = false;
  Location location;
  double latitude;
  double longitude;
  private LocationManager location_manager;

  // The minimum distance to change Updates in meters
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;

  // The minimum time between updates in milliseconds
  private static final long MIN_TIME_BW_UPDATES = 1000;

  public wlocation(Context context)
  {
    this.mContext = context;
    setup_location();
  }

  public void setup_location()
  {
    is_gps_enabled = false;
    is_network_enabled = false;
    can_get_location = false;

    Log.i("GP", "Seting up location_manager");

    location_manager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

    try {
      is_gps_enabled = location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
      if (is_gps_enabled) {
        Log.i("GP", "GPS Enabled");
        can_get_location = true;
      } else {
        Log.i("GP", "GPS Disabled");
      }
    } catch (Exception e) {
      e.printStackTrace();
      Log.e("GP", "GPS state cannot be determined " + e.toString());
    }

    if (!is_gps_enabled) {
      is_network_enabled = location_manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
      if (is_network_enabled) {
        Log.i("GP", "GPS Enabled");
        can_get_location = true;
      } else {
        Log.i("GP", "GPS Disabled");

      }
    }
  }

/****************************************************************************/
  public boolean get_network_location()
/****************************************************************************/
  {
    location = null;

    if (is_network_enabled) {
      location_manager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER,
        MIN_TIME_BW_UPDATES,
        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
      Log.i("GP", "Network location");
      if (location_manager != null) {
        location = location_manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        return true;
      }
      return false;
    }
    return false;
  }

/****************************************************************************/
  public void get_gps_location()
/****************************************************************************/
  {
    location = null;

    if (is_gps_enabled) {
      if (location == null) {
        try {
          if (location_manager != null) {
            location = location_manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
              Log.i("GP", "get_gps_location(): "+location.getLatitude());
              latitude = location.getLatitude();
              longitude = location.getLongitude();
            } else {
              Log.i("GP", "get_gps_location(): location is null");
            }
          }
        } catch (SecurityException e) {
          e.printStackTrace();
          Log.e("GP", "getlocation(): gps permission error");
          return;
        }
      }
    }
  }


  /**
   * Stop using GPS listener
   * Calling this function will stop using GPS in your app
   * */
/****************************************************************************/
  public void stopUsingGPS()
/****************************************************************************/
  {
    if(location_manager != null) {
      location_manager.removeUpdates(wlocation.this);
    }
  }

/****************************************************************************/
  public double getLatitude()
/****************************************************************************/
  {
    if(location != null) {
      latitude = location.getLatitude();
    }

    return latitude;
  }

/****************************************************************************/
  public double getLongitude()
/****************************************************************************/
  {
    if(location != null) {
      longitude = location.getLongitude();
    }

    return longitude;
  }

  /**
   * Function to check GPS/wifi enabled
   * @return boolean
   * */
/****************************************************************************/
  public boolean can_get_location()
/****************************************************************************/
  {
    return this.can_get_location;
  }

  /**
   * Function to show settings alert dialog
   * On pressing Settings button will lauch Settings Options
   * */
  public void showSettingsAlert()
  {
    AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

    // Setting Dialog Title
    alertDialog.setTitle("GPS settings");

    // Setting Dialog Message
    alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

    // On pressing Settings button
    alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog,int which) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        mContext.startActivity(intent);
      }
    });

    // on pressing cancel button
    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
      }
    });

    // Showing Alert Message
    alertDialog.show();
  }

  @Override
  public void onLocationChanged(Location location)
  {
  }

  @Override
  public void onProviderDisabled(String provider)
  {
  }

  @Override
  public void onProviderEnabled(String provider)
  {
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras)
  {
  }

  @Override
  public IBinder onBind(Intent arg0)
  {
    return null;
  }

}
