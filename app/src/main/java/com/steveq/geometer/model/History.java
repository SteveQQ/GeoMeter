package com.steveq.geometer.model;


import java.util.ArrayList;

public class History {
    private ArrayList<Location> locationHistory;

    public History() {
        this.locationHistory = new ArrayList<>();
    }

    public ArrayList<Location> getLocationHistory() {
        return locationHistory;
    }

    public void setLocationHistory(ArrayList<Location> locationHistory) {
        this.locationHistory = locationHistory;
    }

    public Location getLast(){
        return locationHistory.get(locationHistory.size()-1);
    }
}
