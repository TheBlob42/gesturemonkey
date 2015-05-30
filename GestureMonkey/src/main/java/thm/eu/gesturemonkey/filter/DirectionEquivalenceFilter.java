package thm.eu.gesturemonkey.filter;

/**
 * Created by Tobi on 23.10.2014.
 *
 * This filter class is designed to filter incoming sensor data, so that accelerations in the same direction
 * are not passed to the monkey several times
 */
public class DirectionEquivalenceFilter extends Filter {

    private double sensitivity;
    private float[] reference = new float[]{ 0.0f, 0.0f, 0.0f};

    public DirectionEquivalenceFilter(double sensitivity){
        this.sensitivity = sensitivity;
    }

    @Override
    public Float[] filter(Float[] values) {
        if(values != null){
            if(values[0] < reference[0] - sensitivity ||
               values[0] > reference[0] + sensitivity ||
               values[1] < reference[1] - sensitivity ||
               values[1] > reference[1] + sensitivity ||
               values[2] < reference[2] - sensitivity ||
               values[2] > reference[2] + sensitivity){
                reference = new float[]{values[0], values[1], values[2]};
                return values;
            }
        }

        return null;
    }
}
