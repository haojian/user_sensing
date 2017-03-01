package com.sensing.haojian.stepcounterandroid;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    private SensorManager sensorManager;
    private File path;
    private float[] prev = {0f,0f,0f};
    private File file;
    private Menu menu;

    private static final int NOISE = 2;
    private static final int ABOVE = 1;
    private static final int BELOW = 0;

    private static long last_below_time = -1;

    private static int CURRENT_STATE = 0;

    private static int PREVIOUS_STATE = BELOW;
    private LineGraphSeries<DataPoint> rawData;
    private LineGraphSeries<DataPoint> lpData;
    private GraphView graphView;
    private GraphView graphView1;
    private GraphView combView;
    private int rawPoints = 0;
    private int sampleCount = 0;
    private long startTime;
    boolean SAMPLING_ACTIVE = true;
    private long streakStartTime;
    private long streakPrevTime;

    private Button btn_reset;

    private TextView hjStepView;
    private TextView androidStepView;
    private float hjStepCount = 0;
    private float androidStepCount = 0;
    private float androidStepRef = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        btn_reset = (Button) findViewById(R.id.btn_reset);
        btn_reset.setOnClickListener(this);

        hjStepView = (TextView) findViewById(R.id.ourcount);
        androidStepView = (TextView) findViewById(R.id.androidcount);
        hjStepCount = 0;
        androidStepCount = 0;
        hjStepView.setText("0");
        androidStepView.setText("0");

        path =  this.getExternalFilesDir(null);

        rawData = new LineGraphSeries<>();
        rawData.setTitle("Raw Data");
        rawData.setColor(Color.RED);
        lpData = new LineGraphSeries<>();
        lpData.setTitle("Smooth Data");
        lpData.setColor(Color.BLUE);
        graphView = (GraphView) findViewById(R.id.rawGraph);
        graphView1 = (GraphView) findViewById(R.id.lpGraph);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(-40);
        graphView.getViewport().setMaxY(30);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(4);
        graphView.getViewport().setMaxX(80);
        graphView.addSeries(rawData);

        // set manual X bounds
        graphView1.getViewport().setYAxisBoundsManual(true);
        graphView1.getViewport().setMinY(-30);
        graphView1.getViewport().setMaxY(30);
        graphView1.getViewport().setXAxisBoundsManual(true);
        graphView1.getViewport().setMinX(4);
        graphView1.getViewport().setMaxX(80);

        graphView1.addSeries(lpData);

        combView = (GraphView) findViewById(R.id.combGraph);

        combView.getViewport().setYAxisBoundsManual(true);
        combView.getViewport().setMinY(-70);
        combView.getViewport().setMaxY(70);
        combView.getViewport().setXAxisBoundsManual(true);
        combView.getViewport().setMinX(4);
        combView.getViewport().setMaxX(80);

        combView.addSeries(rawData);
        combView.addSeries(lpData);
        streakPrevTime = System.currentTimeMillis() - 500;
    }

    @Override
    public void onClick(View v) {
        if (v == btn_reset){
            hjStepCount = 0;
            androidStepCount = 0;
            hjStepView.setText("" + (hjStepCount));
            androidStepView.setText("" + (androidStepCount));
        }
        return;
    }

    @Override
    protected void onResume() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.instrumentation) {
            SAMPLING_ACTIVE = true;
            sampleCount = 0;
            startTime = System.currentTimeMillis();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleAccelerEvent(SensorEvent event) {
        float offset = 10.5f;
        prev = lowPassFilter(event.values, prev);
        Accelerometer raw = new Accelerometer(event.values);
        Accelerometer data = new Accelerometer(prev);
        rawData.appendData(new DataPoint(rawPoints++,raw.R-offset), true,1000);
        lpData.appendData(new DataPoint(rawPoints, data.R-offset), true, 1000);

        if (data.R < 10) {
            CURRENT_STATE = BELOW;
            if (PREVIOUS_STATE == ABOVE) {
                Log.d("GAP", "" + data.R);
                streakStartTime = System.currentTimeMillis();
                if ((streakStartTime - streakPrevTime) <= 250f) {
                    streakPrevTime = System.currentTimeMillis();
                    return;
                }
                streakPrevTime = streakStartTime;
                Log.d("STATES:", "" + streakPrevTime + " " + streakStartTime);
                hjStepCount++;
            }
            PREVIOUS_STATE = CURRENT_STATE;
        } else if ( data.R >= 10 && data.R < 20) {
            CURRENT_STATE = ABOVE;
            PREVIOUS_STATE = CURRENT_STATE;
        } else if (raw.R >= 20) {
            CURRENT_STATE = NOISE;
            PREVIOUS_STATE = CURRENT_STATE;
        }
        
        hjStepView.setText(""+(hjStepCount));;
    }

    private void handleStepCounterEvent(SensorEvent event) {
        androidStepCount += 1;
        androidStepView.setText(""+(androidStepCount));
    }

    private float[] lowPassFilter(float[] input, float[] prev) {
        float ALPHA = 0.1f;
        if(input == null || prev == null) {
            return null;
        }
        for (int i=0; i< input.length; i++) {
            prev[i] = prev[i] + ALPHA * (input[i] - prev[i]);
        }
        return prev;
    }




    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccelerEvent(event);
            if(SAMPLING_ACTIVE) {
                sampleCount++;
                long now = System.currentTimeMillis();
                if (now >= startTime + 5000) {
                    double samplingRate = sampleCount / ((now - startTime) / 1000.0);
                    SAMPLING_ACTIVE = false;
//                    Toast.makeText(getApplicationContext(), "Sampling rate: " + samplingRate + "Hz", Toast.LENGTH_LONG).show();
                    MenuItem rate = menu.findItem(R.id.instrumentation);
                    rate.setTitle("Sampling Rate : " + samplingRate + "hz");
                }
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            handleStepCounterEvent(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
