package com.sensing.haojian.heartratecounter;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ddf.minim.effects.BandPass;

// Develop an optical heart rate monitoring app using the built in camera on your android phone.
// There is a technique called photoplethysmography (PPG), which consists of detecting changes in blood volume during a cardiac cycle.
// By illuminating the skin and measuring the observed optical changes, it is possible to extract heart rate.
// At a minimum you will need to low pass filter the data you will be extracting from the camera prior to using peak detection to extract heart rate.
// For the app, we ask you to both plot the heart rate signal on the interface and show the extracted heart rate in real time.
// You need to make sure that the app tells the user if the conditions are not right to detect heart rate.
// For example, the phone is moving too much, the finger is not covering the light or the lens properly.

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {
    private static final String TAG = "OCVSample::Activity";

    private FlashCameraView mOpenCvCameraView;
    private Button btn_flash;
    private TextView txt_debug;
    private TextView txt_hint;
    private TextView txt_count;

    private GraphView rawGraphView;
    private LineGraphSeries<DataPoint> rawData;

    private GraphView ppgGraphView;
    private LineGraphSeries<DataPoint> ppgData;

    BandPass bandpass = new BandPass(0.5f, 5, 30);

    private int rawPoints = 0;
    private List<Long> timestamps;

    public static enum SIGNAL_STATE_TYPE {
        NOISE, ABOVE, BELLOW
    };
    private static SIGNAL_STATE_TYPE cur_state = SIGNAL_STATE_TYPE.NOISE;
    private static SIGNAL_STATE_TYPE prev_state = SIGNAL_STATE_TYPE.NOISE;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (FlashCameraView) findViewById(R.id.camera_calibration_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        btn_flash = (Button) findViewById(R.id.btn_flash);
        btn_flash.setOnClickListener(this);

        txt_debug = (TextView) findViewById(R.id.txt_debug);
        txt_hint = (TextView) findViewById(R.id.txt_hint);
        txt_count = (TextView) findViewById(R.id.txt_count);

        rawData = new LineGraphSeries<>();
        rawData.setTitle("Raw Data");
        rawData.setColor(Color.RED);
        ppgData = new LineGraphSeries<>();
        ppgData.setTitle("PPG Data");
        ppgData.setColor(Color.BLUE);

        rawGraphView = (GraphView) findViewById(R.id.rawGraph);
        rawGraphView.getViewport().setXAxisBoundsManual(true);
        rawGraphView.getViewport().setMinX(4);
        rawGraphView.getViewport().setMaxX(80);
        rawGraphView.addSeries(rawData);

        ppgGraphView = (GraphView) findViewById(R.id.ppgGraph);
        ppgGraphView.getViewport().setXAxisBoundsManual(true);
        ppgGraphView.getViewport().setMinX(4);
        ppgGraphView.getViewport().setMaxX(80);
        ppgGraphView.addSeries(ppgData);

        timestamps = new ArrayList<Long>();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }


    public float[] calcHeartRate(List<Long> timestamps) {
        if (timestamps.size() > 25) {
            float[] rates = new float[3];
            rates[0] = (float) (timestamps.size() / ((timestamps.get(timestamps.size() - 1) - timestamps.get(0)) / 1000.0)) * 60;
            rates[1] = (float) (1 / ((timestamps.get(timestamps.size() - 1) - timestamps.get(timestamps.size() - 2)) / 1000.0)) * 60;
            rates[2] = (float) (15 / ((timestamps.get(timestamps.size() - 1) - timestamps.get(timestamps.size() - 16)) / 1000.0)) * 60;
            return rates;
        }
        return null;
    }

    float signal[] = new float[1] ;

    private void handleRednessEvent(final double redness) {
        rawPoints++;
        txt_debug.setText("" + (int)redness );
        if ( redness > 180) {
            rawData.appendData(new DataPoint(rawPoints, redness), true, 100);
            txt_hint.setText("detecting...");
            signal[0] = (float)redness;
            bandpass.process(signal);
            ppgData.appendData(new DataPoint(rawPoints, signal[0]), true, 100);

            float filtered = signal[0];
            if (filtered > 10) {
                cur_state = SIGNAL_STATE_TYPE.ABOVE;
                if (prev_state == SIGNAL_STATE_TYPE.BELLOW) {
                    long curtime = System.currentTimeMillis();
                    timestamps.add(curtime);

                    float[] rates = calcHeartRate(timestamps);
                    if (rates != null) {
                        txt_count.setText("" + (int)rates[2]);
                        Log.v(TAG, "" + Arrays.toString(rates));
                    }
                }
                prev_state = cur_state;
            } else if (filtered < 0 ){
                cur_state = SIGNAL_STATE_TYPE.BELLOW;
                prev_state = cur_state;
            }
        } else {
            rawData.appendData(new DataPoint(rawPoints, 0), true, 100);
            ppgData.appendData(new DataPoint(rawPoints, 0), true, 100);
            cur_state = SIGNAL_STATE_TYPE.NOISE;
            txt_hint.setText("no finger");
            timestamps.clear();
            txt_count.setText("" + 0);
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        List<Mat> rgba_channels = new ArrayList<Mat>();

        Mat raw_frame_rgba = inputFrame.rgba();
        Core.split(raw_frame_rgba, rgba_channels);

        if (rgba_channels.size() == 4) {
            final Scalar m_r = Core.mean(rgba_channels.get(0));

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double redness = m_r.val[0];
                    handleRednessEvent(redness);
                }
            });
        }

        for (Mat channel: rgba_channels) {
            channel.release();
        }
        return inputFrame.rgba();
    }

    @Override
    public void onClick(View v) {
        if (v == btn_flash){
            if (mOpenCvCameraView.getFlashMode()) {
                mOpenCvCameraView.setFlashONOFF(false);
            } else {
                mOpenCvCameraView.setFlashONOFF(true);
            }
        }
        return;
    }
}
