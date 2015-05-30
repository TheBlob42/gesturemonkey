package thm.eu.gesturemonkeyexporter;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener{

    private final String WEARABLE_ACC_PATH = "/wearable/acc_values";
    private final String WEARABLE_GYR_PATH = "/wearable/gyr_values";
    private final String ACCELEROMETER_DATA = "acc_data";
    private final String GYROSCOPE_DATA = "gyr_data";

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope;

    private GoogleApiClient googleClient;

    private boolean isConnected = false;

    private TextView tvX, tvY, tvZ, tvGyrX, tvGyrY, tvGyrZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                tvX = (TextView)stub.findViewById(R.id.tvX);
                tvY = (TextView)stub.findViewById(R.id.tvY);
                tvZ = (TextView)stub.findViewById(R.id.tvZ);

                tvGyrX = (TextView)stub.findViewById(R.id.tvGyrX);
                tvGyrY = (TextView)stub.findViewById(R.id.tvGyrY);
                tvGyrZ = (TextView)stub.findViewById(R.id.tvGyrZ);
            }
        });

        //initializing and checking for all necessary sensors
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                && mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        } else{
            Toast.makeText(this, "Not all necessary sensors available on this device (accelerometer & gyroscope)", Toast.LENGTH_SHORT).show();
            finish();
        }

        //initializing the GoogleApiClient
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause(){
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onStart(){
        super.onStart();
        googleClient.connect();
    }

    @Override
    protected void onStop(){
        if(googleClient != null && googleClient.isConnected()){
            googleClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        isConnected = true;
        Log.d("Wear/MainActivity", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Wear/MainActivity", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Wear/MainActivity", "onConnectionFailed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(isConnected){
            switch (event.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    new SendToDataLayerThread(WEARABLE_ACC_PATH, ACCELEROMETER_DATA, event.values).start();
                    tvX.setText("X: " + event.values[0]);
                    tvY.setText("Y: " + event.values[1]);
                    tvZ.setText("Z: " + event.values[2]);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    new SendToDataLayerThread(WEARABLE_GYR_PATH, GYROSCOPE_DATA, event.values).start();
                    tvGyrX.setText("X: " + event.values[0]);
                    tvGyrY.setText("Y: " + event.values[1]);
                    tvGyrZ.setText("Z: " + event.values[2]);
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private class SendToDataLayerThread extends Thread {
        private String path;
        private String dataType;
        private float[] data;

        public SendToDataLayerThread(String path, String dataType, float[] data){
            this.path = path;
            this.dataType = dataType;
            this.data = data;
        }

        @Override
        public void run() {
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.getDataMap().putFloatArray(dataType, data);
            PutDataRequest request = putDMR.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleClient, request);
        }
    }
}
