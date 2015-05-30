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
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
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

import thm.eu.gesturemonkey.GestureMonkey;

/**
 * Created by Tobi on 13.01.2015.
 */
public class NewGestureFragment extends Fragment implements View.OnClickListener, SensorEventListener, SaveDialogFragment.SaveDialogListener, DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GestureMonkey monkey;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope;

    private EditText etGestureName;
    private Button btnStartTraining, btnStopTraining, btnStartSequence, btnStopSequence;
    private TextView tvTrainingInfo, tvAccX, tvAccY, tvAccZ, tvGyrX, tvGyrY, tvGyrZ;
    private RadioGroup rgDevice;

    private Device selectedDevice = Device.SMARTPHONE;

    private GoogleApiClient googleClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        monkey = GestureMonkey.getInstance();

        //stops the layout from resizing when the keyboard is open
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        googleClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        View view = inflater.inflate(R.layout.fragment_newgesture, container, false);

        etGestureName = (EditText)view.findViewById(R.id.etGestureName);
        rgDevice = (RadioGroup)view.findViewById(R.id.rgDevice);
        rgDevice.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.rbNewSmartphone:
                        selectedDevice = Device.SMARTPHONE;
                        break;
                    case R.id.rbNewSmartwatch:
                        selectedDevice = Device.SMARTWATCH;
                        break;
                }
            }
        });
        tvAccX = (TextView)view.findViewById(R.id.tvAccX);
        tvAccY = (TextView)view.findViewById(R.id.tvAccY);
        tvAccZ = (TextView)view.findViewById(R.id.tvAccZ);
        tvGyrX = (TextView)view.findViewById(R.id.tvGyrX);
        tvGyrY = (TextView)view.findViewById(R.id.tvGyrY);
        tvGyrZ = (TextView)view.findViewById(R.id.tvGyrZ);

        tvTrainingInfo = (TextView)view.findViewById(R.id.tvTrainingInfo);
        btnStartTraining = (Button)view.findViewById(R.id.btnStartTraining);
        btnStopTraining = (Button)view.findViewById(R.id.btnStopTraining);
        btnStartSequence = (Button)view.findViewById(R.id.btnStartRecording);
        btnStopSequence = (Button)view.findViewById(R.id.btnStopRecording);
        btnStartTraining.setOnClickListener(this);
        btnStopTraining.setOnClickListener(this);
        btnStartSequence.setOnClickListener(this);
        btnStopSequence.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStart(){
        super.onStart();
        googleClient.connect();
    }

    @Override
    public void onStop(){
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
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnStartTraining:
                String name = etGestureName.getText().toString();
                if(name.equals("")){
                    Toast.makeText(getActivity(), "Please give your gesture a name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                etGestureName.setEnabled(false);
                btnStartTraining.setEnabled(false);
                btnStopTraining.setEnabled(true);
                btnStartSequence.setEnabled(true);
                monkey.startTraining(name);
                break;
            case R.id.btnStopTraining:
                showSaveDialog();
                break;
            case R.id.btnStartRecording:
                btnStartSequence.setEnabled(false);
                btnStopSequence.setEnabled(true);
                monkey.startTrainingSequence();
                break;
            case R.id.btnStopRecording:
                btnStartSequence.setEnabled(true);
                btnStopSequence.setEnabled(false);
                int sequenceCount = monkey.stopTrainingSequence(false);

                tvTrainingInfo.setText("Number of sequences: " + sequenceCount);
                break;
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

    //### SAVE DIALOG ###

    private void showSaveDialog(){
        DialogFragment dialog = new SaveDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "SaveGestureDialog");
    }

    @Override
    public void onDialogPositiveClick() {
        monkey.stopTraining(false);
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDialogNegativeClick() {
        monkey.stopTraining(true);
        getFragmentManager().popBackStack();
    }

    //### GOOGLE API CLIENT ###

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("App/NewGestureFragment", "onConnected");
        Wearable.DataApi.addListener(googleClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("App/NewGestureFragment", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("App/NewGestureFragment", "onConnectionFailed");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("App/NewGestureFragment", "onDataChanged");

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
