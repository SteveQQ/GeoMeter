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

import com.google.gson.Gson;
import com.steveq.geometer.MainActivity;
import com.steveq.geometer.model.History;
import com.steveq.geometer.obs_pattern.Observable;
import com.steveq.geometer.obs_pattern.Observer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DistanceMeterService extends Service implements LocationListener, Observable {
    private static final String TAG = DistanceMeterService.class.getSimpleName();
    private final IBinder mBinder = new DistanceMeterBinder();
    private LocationManager mLocationManager;
    public static final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; //10 meters
    public static final int MIN_TIME_BW_UPDATES = 2 * 1000; //2 seconds
    private String mProvider;

    private ArrayList<Observer> mObservers;

    private Gson gson = new Gson();
    public History mHistory;
    public File outputJson;

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
        mObservers = new ArrayList<>();
        mHistory = new History();
        outputJson = new File(getFilesDir(), "history_loc.json");
        if(outputJson.exists()){
            mHistory = readHistory(outputJson);
        }
    }

    private History readHistory(File outputJson) {
        try {
            FileReader fr = new FileReader(outputJson);
            String jsonFileContent;
            StringBuilder builder = new StringBuilder();
            int readChar;
            while((readChar = fr.read()) != -1){
                builder.append((char)readChar);
            }
            jsonFileContent = builder.toString();
            return gson.fromJson(jsonFileContent, History.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteHistory(){
        mHistory = null;
        return outputJson.delete();
    }

    public void resetHistory(){
        mHistory = new History();
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
            obs.update(mHistory.getLast().getLatitude(), mHistory.getLast().getLongitude(), mHistory.getDistance());
        }
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

    @SuppressWarnings("MissingPermission")
    public boolean startLocationUpdates(int time, int distance){
        if(mProvider == null){
            return false;
        }
        if (mLocationManager.isProviderEnabled(mProvider)) {
            //mLocationManager.requestLocationUpdates(mProvider, 100000000, distance, this);
            mLocationManager.requestLocationUpdates(mProvider, time, distance, this);
            return true;
        }
        return false;
    }

    @SuppressWarnings("MissingPermission")
    public void stopUpdates() {
        mLocationManager.removeUpdates(this);
    }

    public boolean isProvider(){
        return mProvider!=null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "latitude: " + location.getLatitude());
        Log.d(TAG, "longitude: " + location.getLongitude());

        ArrayList<com.steveq.geometer.model.Location> tempHist = mHistory.getLocationHistory();
        tempHist.add(new com.steveq.geometer.model.Location(location.getLatitude(), location.getLongitude()));

        if(tempHist.size() > 1) {
            float[] results = new float[1];
            Location.distanceBetween(tempHist.get(tempHist.size() - 2).getLatitude(), tempHist.get(tempHist.size() - 2).getLongitude(),
                    mHistory.getLast().getLatitude(), mHistory.getLast().getLongitude(),
                    results);
            mHistory.setDistance(mHistory.getDistance() + (int)results[0]);
        }

        mHistory.setLocationHistory(tempHist);
        Log.d(TAG, gson.toJson(mHistory));

        try {
            FileWriter fw = new FileWriter(outputJson);
            fw.write(gson.toJson(mHistory));
            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        notifyObservers();
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
