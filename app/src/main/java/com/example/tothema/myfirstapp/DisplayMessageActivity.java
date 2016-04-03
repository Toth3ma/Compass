package com.example.tothema.myfirstapp;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DisplayMessageActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView image;
    private float currDegree = 0.0f;
    private float azimuthInDegrees = 0.0f;

    //Low pass variables
    private float smoothFactor = 0.95f;
    private float lastSin = 0.0f;
    private float lastCos = 0.0f;
    float filteredDegrees = 0.0f;


    private float[] magneticRotation = new float[9];
    private float[] orientation = new float[3];
    private float[] gravity = new float[3];
    private float[] magnetic = new float[3];

    private boolean newGravity = false;
    private boolean newMagnetic = false;

    private SensorManager sensManager;
    private Sensor acc;
    private Sensor mag;

    private TextView tvHeading;
    private TextView sinText;

    Handler handler = new Handler();



    private class UpdateTask implements Runnable{

        public void run(){

            tvHeading.setText("Heading: " + String.format("%.2f",filteredDegrees) + " degrees");
            sinText.setText("Acceleration: " + String.format("%.2f", gravity[0]) + " X " + String.format("%.2f", gravity[1]) + " Y " + String.format("%.2f", gravity[2]) + " Z");
            handler.postDelayed(this,500);

        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        tvHeading = (TextView) findViewById(R.id.tvHeading);
        sinText  = (TextView) findViewById(R.id.compassHeading);
        image = (ImageView) findViewById(R.id.compass);

        // initialize android device sensor capabilities
        sensManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        acc = sensManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sensManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MyActivity.EXTRA_MESSAGE);
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(message);

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);
        layout.addView(textView);



        handler.postDelayed(new UpdateTask(), 500);





    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            gravity = event.values.clone();
            newGravity = true;
        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic = event.values.clone();
            newMagnetic = true;
        }

        if(newGravity && newMagnetic){

            SensorManager.getRotationMatrix(magneticRotation, new float[9], gravity, magnetic);
            SensorManager.getOrientation(magneticRotation, orientation);

            float azimuthInRadians = orientation[0];
            azimuthInDegrees = (float)(Math.toDegrees(azimuthInRadians)+360.0f)%360.0f;

            // Apply a low pass filter to the new direction to ensure smoother motion
            lastSin = (float) (smoothFactor * lastSin + (1.0f - smoothFactor) * Math.sin(azimuthInRadians));
            lastCos = (float) (smoothFactor * lastCos + (1.0f - smoothFactor) * Math.cos(azimuthInRadians));
            filteredDegrees = (float) (Math.toDegrees(Math.atan2(lastSin,lastCos)) + 360.0f) %360.0f;




            RotateAnimation ra = new RotateAnimation(currDegree, -filteredDegrees, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
            ra.setDuration(210);

            // Set the animation after the end of the reservation status
            ra.setFillAfter(true);

            image.startAnimation(ra);
            currDegree = -filteredDegrees;

            newGravity = false;
            newMagnetic = false;

        }





    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //NaN
    }


    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        sensManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
        sensManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);

    }



    @Override

    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        sensManager.unregisterListener(this);

    }

};

