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

    // velocity starts at zero
    float velocity = 0f;
    long lastTimestamp = 0;
    float stopSign = 0f;
    int graceFrames = 0;

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

        // reset values when app comes back
        velocity = 0f;
        lastTimestamp = 0;
        stopSign = 0f;
        graceFrames = 0;
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
        // check if this is the gravity sensor
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            // save gravity values
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];
            return;
        }

        // if it's not linear acceleration sensor, ignore it
        if (event.sensor.getType() != Sensor.TYPE_LINEAR_ACCELERATION) {
            return;
        }

        // first reading - just save the timestamp and wait
        if (lastTimestamp == 0) {
            lastTimestamp = event.timestamp;
            return;
        }

        // calculate time since last reading in seconds
        // timestamp is in nanoseconds so divide by 1 billion
        float dt = (event.timestamp - lastTimestamp) / 1000000000f;
        lastTimestamp = event.timestamp;

        // skip if time is weird
        if (dt <= 0 || dt > 0.1f) {
            return;
        }

        // get gravity components
        float gx = gravity[0];
        float gy = gravity[1];
        float gz = gravity[2];

        // calculate the magnitude of gravity vector
        float gMag = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);

        // if gravity is too small something is wrong
        if (gMag < 0.5f) {
            return;
        }

        // calculate unit vector pointing up
        float upX = gx / gMag;
        float upY = gy / gMag;
        float upZ = gz / gMag;

        // dot product to get vertical acceleration component
        float vertAcc = event.values[0] * upX + event.values[1] * upY + event.values[2] * upZ;

        // filter out small noise values (below 0.15 is probably just noise)
        if (Math.abs(vertAcc) < 0.15f) {
            vertAcc = 0f;
        }

        // integrate acceleration to get velocity
        // also apply decay (0.88) to make it slow down naturally
        float newVelocity = velocity * 0.88f + vertAcc * dt;

        // check if velocity changed direction (means we stopped)
        if (velocity * newVelocity < 0) {
            stopSign = Math.signum(velocity);
            graceFrames = 6;
            newVelocity = 0f;
        } else if (graceFrames > 0) {
            graceFrames--;
            // prevent bouncing back wrong direction
            if ((stopSign > 0 && newVelocity < 0) || (stopSign < 0 && newVelocity > 0)) {
                newVelocity = 0f;
            }
        }

        // clamp velocity to max speed (3.0)
        if (newVelocity > 3f) {
            newVelocity = 3f;
        }
        if (newVelocity < -3f) {
            newVelocity = -3f;
        }
        velocity = newVelocity;

        gaugeView.setSpeed(velocity);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // dont need this but have to implement it
    }
}
