package thm.eu.gesturemonkeyexporter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

import thm.eu.gesturemonkey.Gesture;
import thm.eu.gesturemonkey.GestureMonkey;
import thm.eu.gesturemonkey.OnGestureListener;

/**
 * Created by Tobi on 13.01.2015.
 */
public class TestGesturesFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, OnGestureListener, SensorEventListener, DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GestureMonkey monkey;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope;

    private GoogleApiClient googleClient;

    private Menu mMenu;

    private CheckBox chkAutoRecognition;
    private EditText etAutoDelay;
    private Button btnStartRecognition, btnStopRecognition;
    private TextView tvSelectedGestures, tvAccX, tvAccY, tvAccZ, tvGyrX, tvGyrY, tvGyrZ;
    private RadioGroup rgDevices;

    private Device selectedDevice = Device.SMARTPHONE;

    private ArrayList<String> selectedGestures;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        setHasOptionsMenu(true);
        monkey = GestureMonkey.getInstance();

        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        googleClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Bundle arguments = getArguments();
        selectedGestures = arguments.getStringArrayList("SelectedGestures");

        //TEST
        monkey.addRestrictionList(selectedGestures);

        View view = inflater.inflate(R.layout.fragment_testgestures, container, false);

        tvSelectedGestures = (TextView)view.findViewById(R.id.tvSelectedGestures);
        tvSelectedGestures.setText("");
        for(String s : selectedGestures){
            tvSelectedGestures.setText(tvSelectedGestures.getText() + s + "\n");
        }
        tvAccX = (TextView)view.findViewById(R.id.tvAccX);
        tvAccY = (TextView)view.findViewById(R.id.tvAccY);
        tvAccZ = (TextView)view.findViewById(R.id.tvAccZ);
        tvGyrX = (TextView)view.findViewById(R.id.tvGyrX);
        tvGyrY = (TextView)view.findViewById(R.id.tvGyrY);
        tvGyrZ = (TextView)view.findViewById(R.id.tvGyrZ);

        rgDevices = (RadioGroup)view.findViewById(R.id.rgDevice);
        rgDevices.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.rbSmartphone:
                        selectedDevice = Device.SMARTPHONE;
                        break;
                    case R.id.rbSmartwatch:
                        selectedDevice = Device.SMARTWATCH;
                        break;
                }
            }
        });

        chkAutoRecognition = (CheckBox)view.findViewById(R.id.chkAutoRecognition);
        chkAutoRecognition.setOnCheckedChangeListener(this);
        etAutoDelay = (EditText)view.findViewById(R.id.etAutoDelay);

        btnStartRecognition = (Button)view.findViewById(R.id.btnStartRecognition);
        btnStopRecognition = (Button)view.findViewById(R.id.btnStopRecgnition);
        btnStartRecognition.setOnClickListener(this);
        btnStopRecognition.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI);

        monkey.addOnGestureListener(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);

        monkey.removeOnGestureListener(this);
    }

    @Override
    public void onStart(){
        super.onStart();
        googleClient.connect();
    }

    @Override
    public void onStop(){
        //TEST
        monkey.removeRestrictionList();

        if(googleClient != null && googleClient.isConnected()){
            Wearable.DataApi.removeListener(googleClient, this);
            googleClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim){
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float displayWidth = size.x;

        Animator animator = null;
        if(enter) {
            animator = ObjectAnimator.ofFloat(this, "translationX", displayWidth, 0);
        } else {
            animator = ObjectAnimator.ofFloat(this, "translationX", 0, displayWidth);
        }

        animator.setDuration(300);

        return animator;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        this.mMenu = menu;
        menuInflater.inflate(R.menu.testgestures, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_test_export){
            //TODO Export auch vom TestFragment aus anbieten
            DialogFragment dialog = new ExportDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putStringArray("SelectedGestures", selectedGestures.toArray(new String[0]));
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "ExportDialog");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnStartRecognition:
                monkey.startRecognition();
                btnStartRecognition.setEnabled(false);
                btnStopRecognition.setEnabled(true);
                break;
            case R.id.btnStopRecgnition:
                monkey.stopRecognition();
                btnStartRecognition.setEnabled(true);
                btnStopRecognition.setEnabled(false);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked){
            monkey.startRecognition();
            monkey.stopRecognition();

            if(etAutoDelay.getText().toString().equals("")) {
                etAutoDelay.setText("500");
            }
            etAutoDelay.setEnabled(false);
            int delay = Integer.valueOf(etAutoDelay.getText().toString());

            monkey.enableAutoRecognition(0.5, delay);
            btnStartRecognition.setEnabled(false);
            btnStopRecognition.setEnabled(false);
        } else {
            monkey.disableAutoRecognition();
            etAutoDelay.setEnabled(true);
            btnStartRecognition.setEnabled(true);
            btnStopRecognition.setEnabled(false);
        }
    }

    @Override
    public void onGestureRecognized(Gesture g) {
        if(g != null && selectedGestures.contains(g.name)){
            Toast.makeText(getActivity(), "Gesture: " + g.name, Toast.LENGTH_SHORT).show();
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(selectedDevice == Device.SMARTPHONE){
            switch (event.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    Float[] values = new Float[3];
                    values[0] = event.values[0];
                    values[1] = event.values[1];
                    values[2] = event.values[2];

                    monkey.sendAccData(values);

                    tvAccX.setText("X: " + values[0]);
                    tvAccY.setText("Y: " + values[1]);
                    tvAccZ.setText("Z: " + values[2]);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    Float[] gyrValues = new Float[3];
                    gyrValues[0] = event.values[0];
                    gyrValues[1] = event.values[1];
                    gyrValues[2] = event.values[2];

                    monkey.sendGyrData(gyrValues);

                    tvGyrX.setText("X: " + gyrValues[0]);
                    tvGyrY.setText("Y: " + gyrValues[1]);
                    tvGyrZ.setText("Z: " + gyrValues[2]);
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    //### GOOGLE API CLIENT ###

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("App/TestGesturesFragment", "onConnected");
        Wearable.DataApi.addListener(googleClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("App/TestGesturesFragment", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("App/TestGesturesFragment", "onConnectionFailed");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
//        Log.d("App/TestGesturesFragment", "onDataChanged");
        //TODO
        if(selectedDevice == Device.SMARTWATCH){
            for(DataEvent event : dataEvents){
                if(event.getType() == DataEvent.TYPE_CHANGED){
                    String path = event.getDataItem().getUri().getPath();
                    if(path.equals(WearableConstants.WEARABLE_ACC_PATH)){
                        DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                        final float[] values = dataMap.getFloatArray(WearableConstants.ACCELEROMETER_DATA);
                        Float[] accValues = new Float[3];
                        accValues[0] = values[0];
                        accValues[1] = values[1];
                        accValues[2] = values[2];
                        monkey.sendAccData(accValues);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvAccX.setText("X: " + values[0]);
                                tvAccY.setText("Y: " + values[1]);
                                tvAccZ.setText("Z: " + values[2]);
                            }
                        });
                    }
                    else if(path.equals(WearableConstants.WEARABLE_GYR_PATH)){
                        DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                        final float[] values = dataMap.getFloatArray(WearableConstants.GYROSCOPE_DATA);
                        Float[] gyrValues = new Float[3];
                        gyrValues[0] = values[0];
                        gyrValues[1] = values[1];
                        gyrValues[2] = values[2];
                        monkey.sendGyrData(gyrValues);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvGyrX.setText("X: " + values[0]);
                                tvGyrY.setText("Y: " + values[1]);
                                tvGyrZ.setText("Z: " + values[2]);
                            }
                        });
                    }
                }
            }
        }
    }
}
