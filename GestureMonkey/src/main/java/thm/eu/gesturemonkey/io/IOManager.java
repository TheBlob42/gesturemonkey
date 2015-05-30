package thm.eu.gesturemonkey.io;

import android.content.Context;
import android.media.MediaScannerConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import thm.eu.gesturemonkey.Gesture;
import thm.eu.gesturemonkey.Quantizer;
import thm.eu.gesturemonkey.HMM;


/**
* Created by Tobi on 13.01.2015.
*/
public class IOManager {

    private static final String GESTURE_NAME = "name";
    private static final String GESTURE_ACC_QUANTIZER = "acc_quantizer";
    private static final String GESTURE_GYR_QUANTIZER = "gyr_quantizer";
    private static final String GESTURE_ACC_HMM = "acc_hmm";
    private static final String GESTURE_GYR_HMM = "gyr_hmm";

    private static final String QUANTIZER_KMEAN = "kmean";
    private static final String QUANTIZER_CLUSTER_QUANTIZED_SEQUENCE = "cluster_quantized_sequence";
    private static final String QUANTIZER_CENTROIDS = "centroids";

    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";

    private static final String HMM_NUMBER_OF_STATES = "number_of_states";
    private static final String HMM_NUMBER_OF_OBSERVATIONS = "number_of_observations";
    private static final String HMM_INITIAL_PROBABILITIES = "initial_probabilities";
    private static final String HMM_STATE_TRANSITION_PROBABILITIES = "state_transition_probabilities";
    private static final String HMM_EMISSION_PROBABILITIES = "emission_probabilities";
    private static final String HMM_DEFAULT_PROBABILITY = "default_probability";
    private static final String HMM_AVERAGE_PROBABILITY = "average_probability";
    private static final String HMM_MINIMAL_PROBABILITY = "minimal_probability";

    /**
     * Import all gestures from the given JSON file
     * @param folderName Name of the folder with the JSON file
     * @param fileName Name of the JSON file
     * @return A vector with all imported gestures
     */
    public static Vector<Gesture> importGestures(String folderName, String fileName){
        Vector<Gesture> gestures = new Vector<Gesture>();

        try{
            InputStream inputStream = new FileInputStream(folderName + fileName);

            gestures = importGestures(inputStream);
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }

        return gestures;
    }

    /**
     * Import all gestures from the given InputStream
     * @param inputStream An InputStream pointing at a JSON file which should be imported
     * @return A vector with all imported gestures
     */
    public static Vector<Gesture> importGestures(InputStream inputStream){
        Vector<Gesture> gestures = new Vector<Gesture>();

        try{
            // read all file-content as one string
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder sBuilder = new StringBuilder();
            String line;

            while((line = bufferedReader.readLine()) != null){
                sBuilder.append(line);
            }

            // convert string -> json -> gestures
            JSONArray jGestures = new JSONArray(sBuilder.toString());

            for(int i=0; i < jGestures.length(); i++){
                JSONObject jGesture = jGestures.getJSONObject(i);

                gestures.add(createGestureFromJSON(jGesture));
            }
        } catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        } catch(JSONException e){
            e.printStackTrace();
        }

        return gestures;
    }

    /**
     * Creates a gesture from the given JSONObject
     * @param jGesture The JSONObject representing the gesture
     * @return The created gesture
     */
    private static Gesture createGestureFromJSON(JSONObject jGesture){
        Gesture gesture = null;

        try{
            String name = jGesture.getString(GESTURE_NAME);

            JSONObject jAccQuantizer = jGesture.getJSONObject(GESTURE_ACC_QUANTIZER);
            JSONObject jGyrQuantizer = jGesture.getJSONObject(GESTURE_GYR_QUANTIZER);
            Quantizer accQuantizer = createQuantizerFromJSON(jAccQuantizer);
            Quantizer gyrQuantizer = createQuantizerFromJSON(jGyrQuantizer);

            JSONObject jAccHMM = jGesture.getJSONObject(GESTURE_ACC_HMM);
            JSONObject jGyrHMM = jGesture.getJSONObject(GESTURE_GYR_HMM);
            HMM accHMM = createHMMFromJSON(jAccHMM);
            HMM gyrHMM = createHMMFromJSON(jGyrHMM);

            gesture = new Gesture(name, accQuantizer, gyrQuantizer, accHMM, gyrHMM);
        } catch(JSONException e){
            e.printStackTrace();
        }

        return gesture;
    }

    /**
     * Creates a quantizer from the given JSONObject
     * @param jQuantizer The JSONObject representing the quantizer
     * @return The created quantizer
     */
    private static Quantizer createQuantizerFromJSON(JSONObject jQuantizer){
        Quantizer quantizer = null;

        try{
            boolean useKMean = jQuantizer.getBoolean(QUANTIZER_KMEAN);
            boolean clusterQuantizedSequences = jQuantizer.getBoolean(QUANTIZER_CLUSTER_QUANTIZED_SEQUENCE);

            JSONArray jClusterCentroids = jQuantizer.getJSONArray(QUANTIZER_CENTROIDS);
            float[][] clusterCentroids = new float[jClusterCentroids.length()][3];
            for(int i=0; i < jClusterCentroids.length(); i++){
                JSONObject jCentroid = jClusterCentroids.getJSONObject(i);
                float[] centroid = new float[3];
                centroid[0] = (float)jCentroid.getDouble(X);
                centroid[1] = (float)jCentroid.getDouble(Y);
                centroid[2] = (float)jCentroid.getDouble(Z);
                clusterCentroids[i] = centroid;
            }

            quantizer = new Quantizer(useKMean, clusterQuantizedSequences, clusterCentroids);
        } catch(JSONException e){
            e.printStackTrace();
        }

        return quantizer;
    }

    /**
     * Creates a HMM from the given JSONObject
     * @param jHMM The JSONObject representing the HMM
     * @return The created HMM
     */
    private static HMM createHMMFromJSON(JSONObject jHMM){
        HMM hmm = null;

        try{
            int numStates = jHMM.getInt(HMM_NUMBER_OF_STATES);
            int numObservations = jHMM.getInt(HMM_NUMBER_OF_OBSERVATIONS);
            double defaultProb = jHMM.getDouble(HMM_DEFAULT_PROBABILITY);
            double averageProb = jHMM.getDouble(HMM_AVERAGE_PROBABILITY);
            double minimalProb = jHMM.getDouble(HMM_MINIMAL_PROBABILITY);

            JSONArray jInitialProb = jHMM.getJSONArray(HMM_INITIAL_PROBABILITIES);
            double[] initialProb = new double[jInitialProb.length()];
            for(int i=0; i < jInitialProb.length(); i++){
                double prob = jInitialProb.getDouble(i);
                initialProb[i] = prob;
            }

            JSONArray jStateTransitionProb = jHMM.getJSONArray(HMM_STATE_TRANSITION_PROBABILITIES);
            double[][] stateTransitionProb = new double[jStateTransitionProb.length()][];
            for(int i=0; i < jStateTransitionProb.length(); i++){
                JSONArray jArray = jStateTransitionProb.getJSONArray(i);
                double[] array = new double[jArray.length()];

                for(int j=0; j < jArray.length(); j++){
                    array[j] = jArray.getDouble(j);
                }

                stateTransitionProb[i] = array;
            }

            JSONArray jEmissionProb = jHMM.getJSONArray(HMM_EMISSION_PROBABILITIES);
            double[][] emissionProb = new double[jEmissionProb.length()][];
            for(int i=0; i < jEmissionProb.length(); i++){
                JSONArray jArray = jEmissionProb.getJSONArray(i);
                double[] array = new double[jArray.length()];

                for(int j=0; j < jArray.length(); j++){
                    array[j] = jArray.getDouble(j);
                }

                emissionProb[i] = array;
            }

            hmm = new HMM(numStates, numObservations, initialProb, stateTransitionProb, emissionProb, defaultProb, averageProb, minimalProb);

        } catch(JSONException e){
            e.printStackTrace();
        }

        return hmm;
    }

    /**
     * Export all given gestures to the given address
     * @param context
     * @param folderName The name of the folder to save the file
     * @param fileName The name of the file
     * @param gestures All gestures that should be exported
     */
    public static void exportGestures(Context context, String folderName, String fileName, Vector<Gesture> gestures){
        try{
            // create folder if not existing
            File folder = new File(folderName);
            if(!folder.exists() || !folder.isDirectory()){
                folder.mkdirs();
            }

            // write data to file
            File file = new File(folderName + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] data = convertGesturesToJSONArray(gestures).getBytes();

            fos.write(data);
            fos.flush();
            fos.close();

            // makes the new created file instantly visible for connected desktop pcs or laptops
            MediaScannerConnection.scanFile(context, new String[]{folderName + fileName}, null, null);
        } catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Converts all given gestures to a JSONArray
     * @param gestures The gestures to convert
     * @return A String representing the JSONArray
     */
    private static String convertGesturesToJSONArray(Vector<Gesture> gestures){
        JSONArray jGestures = new JSONArray();

        for(Gesture gesture : gestures){
            jGestures.put(convertGestureToJSON(gesture));
        }

        return jGestures.toString();
    }

    /**
     * Converts a single gesture to a JSONObject
     * @param gesture The gesture to convert
     * @return The created JSONObject for the given gesture
     */
    private static JSONObject convertGestureToJSON(Gesture gesture){
        JSONObject jGesture = new JSONObject();
        JSONObject jAccHMM = new JSONObject();
        JSONObject jGyrHMM = new JSONObject();
        JSONObject jAccQuantizer = new JSONObject();
        JSONObject jGyrQuantizer = new JSONObject();

        try{
            jGesture.put(GESTURE_NAME, gesture.name);

            jGesture.put(GESTURE_ACC_QUANTIZER, convertQuantizerToJSONObject(gesture.getAccQuantizer()));
            jGesture.put(GESTURE_GYR_QUANTIZER, convertQuantizerToJSONObject(gesture.getGyrQuantizer()));

            jGesture.put(GESTURE_ACC_HMM, convertHMMTOJSONObject(gesture.getAccHMM()));
            jGesture.put(GESTURE_GYR_HMM, convertHMMTOJSONObject(gesture.getGyrHMM()));
        } catch(JSONException e){
            e.printStackTrace();
        }

        return jGesture;
    }

    /**
     * Converts a HMM to a JSONObject
     * @param hmm The HMM to convert
     * @return The converted HMM
     */
    private static JSONObject convertHMMTOJSONObject(HMM hmm){
        JSONObject jHMM = new JSONObject();

        try{
            jHMM.put(HMM_NUMBER_OF_STATES, hmm.getNumberOfStates());
            jHMM.put(HMM_NUMBER_OF_OBSERVATIONS, hmm.getNumberOfObservations());

            JSONArray jInitialProb = new JSONArray();
            for(double prob : hmm.getInitialProbabilities()){
                jInitialProb.put(prob);
            }
            jHMM.put(HMM_INITIAL_PROBABILITIES, jInitialProb);

            JSONArray jStateTransitionProb = new JSONArray();
            for(double[] probsForState : hmm.getStateTransitionProbabilities()){
                JSONArray jProbsForState = new JSONArray();
                for(double prob : probsForState){
                    jProbsForState.put(prob);
                }
                jStateTransitionProb.put(jProbsForState);
            }
            jHMM.put(HMM_STATE_TRANSITION_PROBABILITIES, jStateTransitionProb);

            JSONArray jEmissionProb = new JSONArray();
            for(double[] probsForState : hmm.getEmissionProbabilities()){
                JSONArray jProbsForState = new JSONArray();
                for(double prob : probsForState){
                    jProbsForState.put(prob);
                }
                jEmissionProb.put(jProbsForState);
            }
            jHMM.put(HMM_EMISSION_PROBABILITIES, jEmissionProb);

            jHMM.put(HMM_DEFAULT_PROBABILITY, hmm.getDefaultProbability());
            jHMM.put(HMM_AVERAGE_PROBABILITY, hmm.getAverageProbability());
            jHMM.put(HMM_MINIMAL_PROBABILITY, hmm.getMinimalProbability());
        } catch(JSONException e){
            e.printStackTrace();
        }

        return jHMM;
    }

    /**
     * Converts a Quantizer to a JSONObject
     *
     * @param quantizer The Quantizer to convert
     * @return The converted Quantizer
     */
    private static JSONObject convertQuantizerToJSONObject(Quantizer quantizer){
        JSONObject jQuantizer = new JSONObject();

        try{
            jQuantizer.put(QUANTIZER_KMEAN, quantizer.usesKMeanForClustering());
            jQuantizer.put(QUANTIZER_CLUSTER_QUANTIZED_SEQUENCE, quantizer.clustersQuantizedSequences());

            JSONArray jClusterCentroids = new JSONArray();
            float[][] clusterCentroids = quantizer.getClusterCentroids();

            for(float[] centroid : clusterCentroids){
                JSONObject jCentroid = new JSONObject();
                jCentroid.put(X, centroid[0]);
                jCentroid.put(Y, centroid[1]);
                jCentroid.put(Z, centroid[2]);

                jClusterCentroids.put(jCentroid);
            }

            jQuantizer.put(QUANTIZER_CENTROIDS, jClusterCentroids);
        } catch(JSONException e){
            e.printStackTrace();
        }

        return  jQuantizer;
    }
}
