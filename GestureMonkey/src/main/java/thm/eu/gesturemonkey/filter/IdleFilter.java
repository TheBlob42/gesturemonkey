package thm.eu.gesturemonkey.filter;

import android.hardware.SensorManager;

/**
 * Created by Tobi on 23.10.2014.
 *
 * This filter class is designed to filter away all data that represents the device in idle state.
 */
public class IdleFilter extends Filter {
    //the sensitivity of the filter-algorithm
    private double sensitivity;

    public IdleFilter(double sensitivity){
        this.sensitivity = sensitivity;
    }

    @Override
    public Float[] filter(Float[] values) {
        if(values != null){
            double absValue = Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2)) / SensorManager.STANDARD_GRAVITY;

            if(absValue > sensitivity || absValue < -sensitivity){
                return values;
            }
        }

        return null;
    }
}
