/* This class is used to draw and update the in-game map
 */

package com.seankelly001.assassin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapUtils {

    final static String TAG = "ASSASSIN";
    private GoogleMap mMap;
    private Location mLastLocation;
    private int zoom_level = 18;
    private float old_heading = 0;
    private Marker arrow_marker, direction_marker;
    private Location destLocation;
    private boolean destReceived = false;
    private final boolean ROTATION_VECTOR_SUPPORTED;
    private final Context context;

    public MapUtils(Context context, GoogleMap mMap, boolean ROTATION_VECTOR_SUPPORTED) {

        this.context = context;
        this.mMap = mMap;
        this.ROTATION_VECTOR_SUPPORTED = ROTATION_VECTOR_SUPPORTED;
        destLocation = new Location("");
    }


    //Set coordinates to destination i.e. your target
    public void setDestCoordinates(double lat, double lng) {

        destLocation.setLatitude(lat);
        destLocation.setLongitude(lng);
        destReceived = true;
    }


    //Update the map with location
    public void updateMap(Location current_location) {

        Log.v(TAG, "UPDATING MAP");
        if(mMap != null && current_location != null){

            mLastLocation = current_location;
            updateMapMarkers(current_location);
            centerCamera(current_location);
        }
    }


    //Update the makers on the map
    private void updateMapMarkers(Location current_location) {

        LatLng current_lat_lang = new LatLng(current_location.getLatitude(), current_location.getLongitude());

        //If markers are not drawn yet
        if(arrow_marker == null) {

            Bitmap small_b;
            //Only some phones can display your direction
            if(ROTATION_VECTOR_SUPPORTED) {

                Drawable d = context.getResources().getDrawable(R.drawable.arrow);
                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap b = bd.getBitmap();
                small_b = Bitmap.createScaledBitmap(b, b.getWidth() / 6, b.getHeight() / 6, false);
            }
            //For phones that can't display direction, draw a dot instead
            else {

                Drawable d = context.getResources().getDrawable(R.drawable.position_icon_orange);
                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap b = bd.getBitmap();
                small_b = Bitmap.createScaledBitmap(b, b.getWidth() / 20, b.getHeight() / 20, false);
            }

            //Draw the arrow (or dot)
            arrow_marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(current_lat_lang)
                            .title("Current Location")
                            .icon(BitmapDescriptorFactory.fromBitmap(small_b)));

        }
        //If already drawn, update its position
        else
            arrow_marker.setPosition(current_lat_lang);

        if(destReceived) {

            //Calculate bearing from current location to destination
            float bearing = current_location.bearingTo(destLocation);
            bearing = (bearing + 360) % 360;

            //Calculate how much rotation is needed
            float rotation = bearing - old_heading;

            //If the direction marker isn't made yet, draw it
            if (direction_marker == null) {

                Drawable d = context.getResources().getDrawable(R.drawable.arc_final);
                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap b = bd.getBitmap();
                Bitmap small_b = Bitmap.createScaledBitmap(b, b.getWidth() / 6, b.getHeight() / 6, false);

                direction_marker = mMap.addMarker(
                        new MarkerOptions()
                                .title("Current Location")
                                .icon(BitmapDescriptorFactory.fromBitmap(small_b))
                                .rotation(rotation)
                                .position(current_lat_lang)
                );
            } else {

                direction_marker.setRotation(rotation);
                direction_marker.setPosition(current_lat_lang);
            }
        }

    }


    //Center the camera on your position
    private void centerCamera(Location current_location) {

        LatLng current_lat_lng = new LatLng(current_location.getLatitude(), current_location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current_lat_lng));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoom_level);
        mMap.animateCamera(zoom);
    }


    //Called when rotation changes (if applicable)
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(mMap != null && mLastLocation != null) {
            synchronized (this) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

                    //Calculate your heading
                    float[] mRotationMatrix = new float[16];
                    SensorManager.getRotationMatrixFromVector(
                            mRotationMatrix, sensorEvent.values);
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(mRotationMatrix, orientation);
                    float heading = (float) Math.toDegrees(orientation[0]);

                    //Only change rotation if rotation changes by more than 1 degree to prevent lag
                    if (Math.abs(heading - old_heading) > 1) {

                        //Update camera rotation
                        old_heading = heading;
                        updateCameraRotation(heading);
                        updateMap(mLastLocation);
                    }
                }
            }
        }
    }


    //Change the camera rotation so that it corresponds to the direction you are facing
    private void updateCameraRotation(float bearing) {

        if(mMap != null) {
            CameraPosition oldPos = mMap.getCameraPosition();
            CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
        }
    }
}
