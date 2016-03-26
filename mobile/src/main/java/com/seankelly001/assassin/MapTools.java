package com.seankelly001.assassin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Sean on 01/03/2016.
 */
public class MapTools {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;
    private int zoom_level = 18;
    private float old_heading = 0;
    private Marker arrow_marker;
    private Marker direction_marker;
    private float mDeclination;
    private Location destLocation;
    private boolean destReceived = false;
    private final boolean ROTATION_VECTOR_SUPPORTED;
    private final Context context;

    public MapTools(Context context, GoogleMap mMap, GoogleApiClient mGoogleApiClient, boolean ROTATION_VECTOR_SUPPORTED) {

        this.context = context;
        this.mMap = mMap;
        this.mGoogleApiClient = mGoogleApiClient;
        this.ROTATION_VECTOR_SUPPORTED = ROTATION_VECTOR_SUPPORTED;
        destLocation = new Location("");
    }


    public void setDestCoordinates(double lat, double lng) {

        destLocation.setLatitude(lat);
        destLocation.setLongitude(lng);
        destReceived = true;

    }


    public void setmLastLocation(Location mLastLocation) {
        this.mLastLocation = mLastLocation;
    }


    public void updateMap(Location current_location) {

        Log.e("#######", "UPDATING MAP");

        if(mMap == null)  Log.e("#######","MAP IS NULL");
        if(current_location == null)  Log.e("#######","LOCATION IS NULL");

        if(mMap != null && current_location != null){

            mLastLocation = current_location;
            updateMapMarkers(current_location);
            centerCamera(current_location);
        }
    }


    private void updateMapMarkers(Location current_location) {


        LatLng current_lat_lang = new LatLng(current_location.getLatitude(), current_location.getLongitude());

        if(arrow_marker == null) {

            Bitmap small_b;
            if(ROTATION_VECTOR_SUPPORTED) {

                Drawable d = context.getResources().getDrawable(R.drawable.arrow);
                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap b = bd.getBitmap();
                small_b = Bitmap.createScaledBitmap(b, b.getWidth() / 6, b.getHeight() / 6, false);
            }
            else {

                Drawable d = context.getResources().getDrawable(R.drawable.position_icon_orange);
                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap b = bd.getBitmap();
                small_b = Bitmap.createScaledBitmap(b, b.getWidth() / 20, b.getHeight() / 20, false);
            }

                arrow_marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(current_lat_lang)
                                .title("Current Location")
                                .icon(BitmapDescriptorFactory.fromBitmap(small_b)));

        }
        else {

            arrow_marker.setPosition(current_lat_lang);
        }

        //=====================================================================================
        //Log.e("HEADING", "old heading: " + heading);
        //Log.e("HEADING", "current location: " + current_location.getLatitude() + ", " + current_location.getLongitude());

        Location galwayL = new Location("");
        galwayL.setLatitude(53.2750164596357);
        galwayL.setLongitude(-9.052734370000053);

        Location dcuL = new Location("");
        dcuL.setLatitude(53.385381);
        dcuL.setLongitude(-6.258854);

        if(destReceived) {
            float bearing = current_location.bearingTo(destLocation);
            Log.e("HEADING", "bearing 1: " + bearing);

            //bearing = Math.round((-bearing / 360) + 180);
            bearing = (bearing + 360) % 360;
            Log.e("HEADING", "bearing 2: " + bearing);

            float rotation = bearing - old_heading;
            Log.e("HEADING", "rotation: " + rotation);

            if (direction_marker == null) {

                direction_marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(current_lat_lang)
                                .title("Current Location")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arc))
                                .rotation(rotation)
                );
            } else {

                direction_marker.setRotation(rotation);
                direction_marker.setPosition(current_lat_lang);
            }
        }

    }


    private void centerCamera(Location current_location) {

        LatLng current_lat_lng = new LatLng(current_location.getLatitude(), current_location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current_lat_lng));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoom_level);
        mMap.animateCamera(zoom);
    }


    //====================================================================================
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(mMap != null && mLastLocation != null) {
            synchronized (this) {

                if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

                    float[] mRotationMatrix = new float[16];
                    SensorManager.getRotationMatrixFromVector(
                            mRotationMatrix, sensorEvent.values);
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(mRotationMatrix, orientation);
                    float heading = (float) Math.toDegrees(orientation[0]) + mDeclination;


                    if (Math.abs(heading - old_heading) > 1) {

                        old_heading = heading;
                        updateCameraRotation(heading);
                        updateMap(mLastLocation);
                    }
                }

                /*
                if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    magneticX.setText(Float.toString(sensorEvent.values[0]));
                    magneticY.setText(Float.toString(sensorEvent.values[1]));
                }*/
            }
        }
    }


    private void updateCameraRotation(float bearing) {

        if(mMap != null) {
            CameraPosition oldPos = mMap.getCameraPosition();
            CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
        }
    }


    private double euclidean_distance(double x1, double y1, double x2, double y2) {

        double x = Math.pow((x1 - x2), 2);
        double y = Math.pow((y1 - y2), 2);
        double result = Math.sqrt(x + y);
        return result;
    }



}
