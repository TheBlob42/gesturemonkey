package thm.eu.gesturemonkeyexporter;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

import thm.eu.gesturemonkey.GestureMonkey;
import thm.eu.gesturemonkey.filter.DirectionEquivalenceFilter;
import thm.eu.gesturemonkey.filter.IdleFilter;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getActionBar().setTitle("MonkeyExporter");

        //initialize GestureMonkey with filters
        GestureMonkey monkey = GestureMonkey.getInstance();
        monkey.addFilter(new IdleFilter(1.2));
        monkey.addFilter(new DirectionEquivalenceFilter(0.3));

        //check for necessary sensors
        SensorManager manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if(manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null || manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null){
            Toast.makeText(this, "This device has not all necessary sensors (accelerometer & gyroscope)", Toast.LENGTH_SHORT).show();
            finish();
        }

        if(savedInstanceState != null){
            return;
        }

        GestureListFragment firstFragment = new GestureListFragment();

        firstFragment.setArguments(getIntent().getExtras());

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        //only on app-start fade in the first fragment
        transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        transaction.add(R.id.fragmentContainer, firstFragment);
        transaction.commit();
    }
}
