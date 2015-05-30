package thm.eu.gesturemonkey;

import android.util.Log;

import java.util.Vector;

/**
 * Created by Tobi on 12.01.2015.
 *
 * A representation for a single gesture.
 * Contains the corresponding HMM and quantizer
 */
public class Gesture {
    //the name of the gesture, use it to identify which gesture was recognized
    public String name;

    //the HMMs for acceleration and gyroscope data
    private HMM accHMM, gyrHMM;

    private Quantizer gyrQuantizer;
    private Quantizer accQuantizer;

    public Gesture(String name){
        this.name = name;

        accQuantizer = new Quantizer(true, true);
        gyrQuantizer = new Quantizer(false, true);
        accHMM = new HMM(7, 14);
        gyrHMM = new HMM(7, 14);
    }

    /**
     * Constructor for JSON
     * @param name Name of the gesture
     * @param accQuantizer Quantizer for the acceleromter data
     * @param gyrQuantizer Quantizer for the gyroscope data
     * @param accHMM HMM for the accelerometer data
     * @param gyrHMM HMM for the gyroscope data
     */
    public Gesture(String name, Quantizer accQuantizer, Quantizer gyrQuantizer, HMM accHMM, HMM gyrHMM){
        this.name = name;

        this.accQuantizer = accQuantizer;
        this.gyrQuantizer = gyrQuantizer;
        this.accHMM = accHMM;
        this.gyrHMM = gyrHMM;
    }

    /**
     * Trains the intern HMMs of this gesture with the given training data
     * @param accTrainSequences A vector of all the given training sequences (Acceleration Data)
     * @param gyrTrainSequences A vector of all the given training sequences (Gyroscope Data)
     */
    public void train(Vector<Vector<Float[]>> accTrainSequences, Vector<Vector<Float[]>> gyrTrainSequences){
        // summarize all training sequences to cluster centroids for the accQuantizer
        Vector<Float[]> sum = new Vector<Float[]>();
        for(Vector<Float[]> sequence : accTrainSequences){
            for(Float[] point : sequence){
                sum.add(point);
            }
        }
        accQuantizer.quantize(sum);

        // train the HMM for acceleration data with the discrete sequences of each train-sequence
        Vector<int[]> discreteSequences = new Vector<int[]>();
        for(Vector<Float[]> sequence : accTrainSequences){
            discreteSequences.add(accQuantizer.getQuantizedSequence(sequence));
        }
        accHMM.train(discreteSequences);

        //******************************************************************

        // summarize all training sequences to cluster centroids for the gyrQuantizer
//        Vector<Float[]> sum2 = new Vector<Float[]>();
//        for(Vector<Float[]> sequence : gyrTrainSequences){
//            for(Float[] point : sequence){
//                sum2.add(point);
//            }
//        }
//        gyrQuantizer.quantize(sum2);
//
//        // train the HMM for the gyroscope data with the discrete sequences of each train-sequence
//        Vector<int[]> discreteSequences2 = new Vector<int[]>();
//        for(Vector<Float[]> sequence : gyrTrainSequences){
//            discreteSequences2.add(gyrQuantizer.getQuantizedSequence(sequence));
//        }
//        gyrHMM.train(discreteSequences2);
    }

    /**
     * Checks how likely the given data belongs to this type of gesture
     * @param accSequence The given acceleration data
     * @param gyrSequence The given gyroscope data
     * @return The probability that this is the right gesture, for the given data
     */
    public double match(Vector<Float[]> accSequence, Vector<Float[]> gyrSequence){
        int[] discreteAccSequence = accQuantizer.getQuantizedSequence(accSequence);
//        int[] discreteGyrSequence = gyrQuantizer.getQuantizedSequence(gyrSequence);

//        if(discreteAccSequence == null || discreteGyrSequence == null){
//            return 0.0;
//        }
        Log.d("G", "*****" + name + "*****");
        double accProbability = accHMM.match(discreteAccSequence);
//        double gyrProbability = gyrHMM.match(discreteGyrSequence);

        //The gyroscope data is not used in the classification progress, but instead for determine if a gesture is definite not matching
        //(so if the gyroscope probability is zero, it is very likely that this is the wrong gesture)
//        if(gyrProbability == 0.0){
//            return 0.0;
//        }

        return accProbability;
    }

    //### GETTERS ###

    public double getDefaultProbability(){
        return accHMM.defaultProb;
    }

    public Quantizer getAccQuantizer(){
        return accQuantizer;
    }

    public Quantizer getGyrQuantizer(){
        return gyrQuantizer;
    }

    public HMM getAccHMM(){
        return accHMM;
    }

    public HMM getGyrHMM(){
        return gyrHMM;
    }
}
