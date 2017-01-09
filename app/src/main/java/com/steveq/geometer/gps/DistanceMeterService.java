package com.steveq.geometer.gps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class DistanceMeterService extends Service implements LocationListener{
    private static final String TAG = DistanceMeterService.class.getSimpleName();
    private final IBinder mBinder = new DistanceMeterBinder();
    private LocationManager mLocationManager;
    public static final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; //10 meters
    public static final int MIN_TIME_BW_UPDATES = 2 * 1000; //2 seconds
    private String mProvider;

    private double longitude;
    private double latitude;

    public DistanceMeterService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        configureGPS();
        //startLocationUpdates(MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUpdates();
    }
    public void configureGPS() {
        Criteria configuration = buildCriteria();
        List<String> providers = mLocationManager.getProviders(configuration, true);
        if(providers == null || providers.size() == 0){
            return;
        }
        mProvider = mLocationManager.getBestProvider(configuration, true);
    }

    private Criteria buildCriteria() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
//        criteria.setAltitudeRequired(false);
//        criteria.setBearingRequired(false);
//        criteria.setCostAllowed(true);
//        criteria.setPowerRequirement(Criteria.POWER_LOW);

        return criteria;
    }

    public boolean startLocationUpdates(int time, int distance){
        if(mProvider == null){
            return false;
        }
        if (mLocationManager.isProviderEnabled(mProvider)) {
            mLocationManager.requestLocationUpdates(mProvider, time, distance, this);
            return true;
        }
        return false;
    }

    public void stopUpdates() {
        mLocationManager.removeUpdates(this);
    }

    public boolean isProvider(){
        return mProvider!=null;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude(){
        return this.longitude;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "latitude: " + location.getLatitude());
        Log.d(TAG, "longitude: " + location.getLongitude());
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public class DistanceMeterBinder extends Binder {
        public DistanceMeterService getDistanceMeterService(){
            return DistanceMeterService.this;
        }
    }
}
