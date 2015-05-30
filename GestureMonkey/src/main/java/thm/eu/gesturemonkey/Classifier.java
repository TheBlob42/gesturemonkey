package thm.eu.gesturemonkey;

import java.util.Vector;

/**
 * Created by Tobi on 12.01.2015.
 *
 * This class represents the Classifier which determine which gestures gets recognized
 */
public class Classifier {

    /**
     * Returns the gesture which is the most likely the recognizable gesture for the given data<br/>
     * This classifier uses the "Bayes-Classification" to accomplish that
     * @param gestures All gestures that should be compared to the given data
     * @param accSequence The acceleration data given from the sensors
     * @param gyrSequence The gyroscope data given from the sensors
     * @return The gesture that was recognized. "Null" is returned if no gesture was recognized
     */
    public Gesture getRecognizedGesture(Vector<Gesture> gestures, Vector<Float[]> accSequence, Vector<Float[]> gyrSequence){
        double probability = 0.0;
        Gesture g = null;

        double denominator = calculateDenominator(gestures, accSequence, gyrSequence);

        for(Gesture gesture : gestures){
            double tmpProb = gesture.getDefaultProbability() * gesture.match(accSequence, gyrSequence) / denominator;
            if(tmpProb > probability){
                probability = tmpProb;
                g = gesture;
            }
        }

        return g;
    }

    /**
     * Calculates the denominator for the "Bayes-Classification"
     * @param gestures All gestures that should be compared
     * @param accSequence The acceleration data
     * @param gyrSequence The gyroscope data
     * @return The denominator to use
     */
    private double calculateDenominator(Vector<Gesture> gestures, Vector<Float[]> accSequence, Vector<Float[]> gyrSequence){
        double denominator = 0.0;
        for(Gesture gesture : gestures){
            denominator += gesture.getDefaultProbability() * gesture.match(accSequence, gyrSequence);
        }
        return denominator;
    }

}
