package ie.dcu.kelly224.sean.assassin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

public class GameMapActivity extends FragmentActivity implements OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener, LocationListener, SensorEventListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation;
    private int zoom_level = 15;
    private GeomagneticField geoField;
    private LocationListener listener;

    private TextView magneticX;
    private TextView magneticY;
    private TextView magneticZ;
    private SensorManager sensorManager = null;

    private float mDeclination;
    private float old_heading = 0;

    private Marker arrow_marker;
    private Marker direction_marker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //==================================
        // Create an instance of GoogleAPIClient.

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        //==================================


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        setContentView(R.layout.game_map_layout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        //==================================================================

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Capture magnetic sensor related view elements
        magneticX = (TextView) findViewById(R.id.valMag_X);
        magneticY = (TextView) findViewById(R.id.valMag_Y);
        magneticZ = (TextView) findViewById(R.id.valMag_Z);

        // Register magnetic sensor
      /*  sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL); */

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        /*
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney)); */
    }


    protected void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onPause() {

        // Unregister the listener
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    protected void onStart() {

        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {

        mGoogleApiClient.disconnect();
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
    }



    @Override
    public void onConnected(Bundle bundle) {

        Toast.makeText(this, "connected", Toast.LENGTH_LONG).show();
        Location current_location = getLocation();
        centerCamera(current_location);
        updateMap(current_location);
    }


    private void updateMap(Location current_location) {

        if(current_location != null) {

            LatLng current_lat_lang = new LatLng(current_location.getLatitude(), current_location.getLongitude());

            if(arrow_marker == null) {

                arrow_marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(current_lat_lang)
                                .title("Current Location")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow3)));
            }
            else {

                arrow_marker.setPosition(current_lat_lang);
            }

            //=====================================================================================

            float heading = 0;

            Log.e("HEADING", "old heading: " + heading);
            Log.e("HEADING", "current location: " + current_location.getLatitude() + ", " + current_location.getLongitude());

            Location galwayL = new Location("");
            galwayL.setLatitude(53.2750164596357);
            galwayL.setLongitude(-9.052734370000053);

            Location dcuL = new Location("");
            dcuL.setLatitude(53.385381);
            dcuL.setLongitude(-6.258854);

            float bearing = current_location.bearingTo(dcuL);
            Log.e("HEADING", "bearing 1: " + bearing);

            //bearing = Math.round((-bearing / 360) + 180);
            bearing = (bearing + 360) % 360;
            Log.e("HEADING", "bearing 2: " + bearing);
            /*
            geoField = new GeomagneticField(
                    (float) current_location.getLatitude(),
                    (float) current_location.getLongitude(),
                    (float) current_location.getAltitude(),
                    System.currentTimeMillis()
            );

            heading += geoField.getDeclination();
            Log.e("HEADING", "declination: " + geoField.getDeclination());
            heading = bearing - (bearing + heading);
           // heading = Math.round(-heading / (360 + 180));
            */



            float rotation = bearing - old_heading;
            Log.e("HEADING", "rotation: " + rotation);

            if(direction_marker == null) {

                direction_marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(current_lat_lang)
                                .title("Current Location")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arc))
                                .rotation(rotation)
                );
            }
            else {

                direction_marker.setRotation(rotation);
                direction_marker.setPosition(current_lat_lang);
            }


            double x1 = current_location.getLatitude();
            double y1 = current_location.getLongitude();

            double x2 = dcuL.getLatitude();
            double y2 = dcuL.getLongitude();
            double distance = euclidean_distance(x1, y1, x2, y2);
            toast("distance:" + distance);
        }

        else {
            Toast.makeText(this, "location is null", Toast.LENGTH_LONG).show();
        }
        // centerCamera();
    }


    private double euclidean_distance(double x1, double y1, double x2, double y2) {

        double x = Math.pow((x1 - x2), 2);
        double y = Math.pow((y1 - y2), 2);
        double result = Math.sqrt(x + y);
        return result;
    }

    private Location getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "NULLLLL", Toast.LENGTH_LONG).show();
            return null;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        return mLastLocation;
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


//LocationListener Method
//--------------------------------------------------------------------------------------------------
    @Override
    public void onLocationChanged(Location current_location) {

        Toast.makeText(this, "Location changed", Toast.LENGTH_LONG);
        centerCamera(current_location);
        updateMap(current_location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        Toast.makeText(this, "status changed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {

        Toast.makeText(this, "provider enabled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

//--------------------------------------------------------------------------------------------------

//====================================================================================
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        synchronized (this) {

            if(sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

                float[] mRotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(
                        mRotationMatrix, sensorEvent.values);
                float[] orientation = new float[3];
                SensorManager.getOrientation(mRotationMatrix, orientation);
                float heading = (float) Math.toDegrees(orientation[0]) + mDeclination;


                if(Math.abs(heading - old_heading) > 1) {

                    old_heading = heading;
                    magneticX.setText(Float.toString(heading));
                    updateCameraRotation(heading);

                    Location current_location = getLocation();
                    centerCamera(current_location);
                    updateMap(current_location);
                }
            }

            /*
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticX.setText(Float.toString(sensorEvent.values[0]));
                magneticY.setText(Float.toString(sensorEvent.values[1]));
            }*/
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void updateCameraRotation(float bearing) {

        if(mMap != null) {
            CameraPosition oldPos = mMap.getCameraPosition();
            CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
        }
    }


//==================================================================================================
    public void refresh(View v) {

        //updateMap();
    }


    public void centerCameraClick(View v) {

       // centerCamera();
    }

    private void centerCamera(Location current_location) {

        if(current_location != null) {

            LatLng current_lat_lng = new LatLng(current_location.getLatitude(), current_location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(current_lat_lng));
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoom_level);
            mMap.animateCamera(zoom);
        }
    }

    public void zoomInClick(View v) {

        zoom_level++;
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoom_level);
        mMap.animateCamera(zoom);
    }

    public void zoomOutClick(View v) {

        zoom_level--;
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoom_level);
        mMap.animateCamera(zoom);
    }



}
