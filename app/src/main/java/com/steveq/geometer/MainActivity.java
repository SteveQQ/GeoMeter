package com.steveq.geometer;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.steveq.geometer.gps.DistanceMeterService;
import com.steveq.geometer.obs_pattern.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements Observer{

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;
    public static final int LOCATION_AUTO_START = 20;

    private ServiceConnection mConnection;
    private DistanceMeterService mDistanceMeterService;
    private boolean isBound = false;
    public static boolean isAllowed = false;
    private boolean isRecreated = false;
    private Intent startServiceIntent;

    @BindView(R.id.locateButton) Button mLocateButton;
    @BindView(R.id.stopButton) Button mStopButton;
    @BindView(R.id.latitudeTextView) TextView mLatitudeTextView;
    @BindView(R.id.longitudeTextView) TextView mLongitudeTextView;
    @BindView(R.id.mapButton) Button mMapButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(MainActivity.this);
        requestPermission();

        Log.d(TAG, "Activity created");

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Service connected");
                mDistanceMeterService = ((DistanceMeterService.DistanceMeterBinder) service).getDistanceMeterService();
                mDistanceMeterService.addObserver(MainActivity.this);
                isBound = true;
                if(!isRecreated) {
                    mDistanceMeterService.deleteHistory();
                } else if(mDistanceMeterService.outputJson.exists() && isAllowed){
                    mDistanceMeterService.startLocationUpdates();
                    mLatitudeTextView.setText(String.valueOf(mDistanceMeterService.mHistory.getLast().getLatitude()));
                    mLongitudeTextView.setText(String.valueOf(mDistanceMeterService.mHistory.getLast().getLongitude()));
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        startServiceIntent = new Intent(this, DistanceMeterService.class);
        bindService(startServiceIntent, mConnection, BIND_AUTO_CREATE);

        if(savedInstanceState != null){
            isRecreated = true;
        }

        mLocateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDistanceMeterService.isProvider() && isAllowed) {
                    mLongitudeTextView.setText("0.00");
                    mLatitudeTextView.setText("0.00");
                    mDistanceMeterService.startLocationUpdates(DistanceMeterService.MIN_TIME_BW_UPDATES, DistanceMeterService.MIN_DISTANCE_CHANGE_FOR_UPDATES);
                } else {
                    showAlert();
                }
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDistanceMeterService.stopUpdates();
                mDistanceMeterService.deleteHistory();
            }
        });

        mMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("initial_lat", mDistanceMeterService.getLatitude());
                intent.putExtra("initial_long", mDistanceMeterService.getLongitude());
                startActivityForResult(intent, LOCATION_AUTO_START);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOCATION_AUTO_START) {
            if (resultCode == RESULT_OK) {
                mDistanceMeterService.startLocationUpdates();
            }
        }
    }

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            isAllowed = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MY_PERMISSION_ACCESS_FINE_LOCATION){
            isAllowed = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(mConnection);
            mDistanceMeterService.deleteObserver(this);
            Log.d(TAG, "Service unbind");
            isBound = false;
        }
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is turned off")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(locationIntent);
                        mDistanceMeterService.configureGPS();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    @Override
    public void update(double latitude, double longitude) {
        mLatitudeTextView.setText(String.valueOf(latitude));
        mLongitudeTextView.setText(String.valueOf(longitude));
    }
}
