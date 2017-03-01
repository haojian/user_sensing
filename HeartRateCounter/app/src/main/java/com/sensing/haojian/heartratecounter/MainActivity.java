package com.sensing.haojian.heartratecounter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

// Develop an optical heart rate monitoring app using the built in camera on your android phone.
// There is a technique called photoplethysmography (PPG), which consists of detecting changes in blood volume during a cardiac cycle.
// By illuminating the skin and measuring the observed optical changes, it is possible to extract heart rate.
// At a minimum you will need to low pass filter the data you will be extracting from the camera prior to using peak detection to extract heart rate.
// For the app, we ask you to both plot the heart rate signal on the interface and show the extracted heart rate in real time.
// You need to make sure that the app tells the user if the conditions are not right to detect heart rate.
// For example, the phone is moving too much, the finger is not covering the light or the lens properly.

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener, SensorEventListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
