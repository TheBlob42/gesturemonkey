package thm.eu.gesturemonkey;

import android.hardware.SensorManager;
import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Tobi on 13.01.2015.
 */
public class MotionDetector {
    //sensitivity if a motion is detect as motion or idle
    private final double sensitivity;
    //delay after the MotionDetector stops checking for incoming values
    private final long delay;

    private Timer timer;

    //device is right now in motion or not
    private boolean inMotion = false;

    //handler that gets called when the motion ends
    private Handler stopRecognitionHandler;

    public MotionDetector(Handler handler, double sensitivity, long delay){
        this.stopRecognitionHandler = handler;
        this.sensitivity = sensitivity;
        this.delay = delay;
    }

    /**
     * Checks if the device is actual in motion, if not the vector is getting filtered away
     * @param vector Actual Vector of acceleration data
     * @return The given vector (if the device is in motion) or "null" if not
     */
    public Float[] checkForMotion(Float[] vector){
        //calculate the absolute value of the passed vector
        double absValue = -1;
        if(vector != null)
            absValue = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2)) / SensorManager.STANDARD_GRAVITY;
        else
            return null;

        //if the absolute value is big enough (sensitivity) motion was started or is going on
        if(absValue > (1 + sensitivity) || absValue < -(1 + sensitivity)){
            inMotion = true;

            //reset and restart timer if its already running
            if(timer != null){
                timer.cancel();
                timer = null;
            }
            timer = new Timer();

            //if timer runs out send an empty message to the given handler
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    inMotion = false;
                    stopRecognitionHandler.sendEmptyMessage(1);
                }
            }, delay);
            return vector;
        } else{
            if(inMotion){
                return vector;
            } else{
                return null;
            }
        }
    }
}
