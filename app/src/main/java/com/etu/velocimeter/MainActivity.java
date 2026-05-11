package com.etu.velocimeter;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

// this is my main activity class for the velocimeter app
// i made this for my project 
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // i need a sensor manager to get sensor data
    SensorManager sensorManager;
    Sensor linearAccelSensor;
    Sensor gravitySensor;

    // gravity values, starting with earth gravity
    float[] gravity = {0f, 9.8f, 0f};

    // the gauge view
    GaugeView gaugeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the status bar color to match the background
        getWindow().setStatusBarColor(Color.parseColor("#0F0F1A"));

        // keep the screen on so it doesnt turn off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set the layout
        setContentView(R.layout.activity_main);

        // find the gauge view from the layout
        gaugeView = (GaugeView) findViewById(R.id.gaugeView);

        // get the sensor manager from system
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // get linear acceleration sensor (this is the one without gravity)
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // get gravity sensor to know which way is up
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        Log.d("Velocimeter", "Sensors initialized");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register sensor listeners
        if (linearAccelSensor != null) {
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gravitySensor != null) {
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister sensors when app goes to background
        sensorManager.unregisterListener(this);
    }

    // this is called every time sensor data changes
    @Override
    public void onSensorChanged(SensorEvent event) {
        // save gravity vector when gravity sensor fires
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // dont need this but have to implement it
    }
}
