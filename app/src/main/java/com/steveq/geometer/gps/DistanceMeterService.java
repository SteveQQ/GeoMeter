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

import com.steveq.geometer.obs_pattern.Observable;
import com.steveq.geometer.obs_pattern.Observer;

import java.util.ArrayList;
import java.util.List;

public class DistanceMeterService extends Service implements LocationListener, Observable {
    private static final String TAG = DistanceMeterService.class.getSimpleName();
    private final IBinder mBinder = new DistanceMeterBinder();
    private LocationManager mLocationManager;
    public static final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; //10 meters
    public static final int MIN_TIME_BW_UPDATES = 2 * 1000; //2 seconds
    private String mProvider;

    private double longitude;
    private double latitude;
    private ArrayList<Observer> mObservers;

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
        mObservers = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUpdates();
    }

    @Override
    public void addObserver(Observer observer) {
        mObservers.add(observer);
    }

    @Override
    public void deleteObserver(Observer observer) {
        mObservers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for(Observer obs : mObservers){
            obs.update(this.latitude, this.longitude);
        }
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
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

    public boolean startLocationUpdates(){
        return startLocationUpdates(MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES);
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

    @Override
    public void onLocationChanged(Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        notifyObservers();
        Log.d(TAG, "latitude: " + latitude);
        Log.d(TAG, "longitude: " + longitude);
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
