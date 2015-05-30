package thm.eu.gesturemonkey;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import thm.eu.gesturemonkey.filter.Filter;
import thm.eu.gesturemonkey.io.IOManager;

/**
 * Created by Tobi on 13.01.2015.
 */
public class GestureMonkey {
    private static GestureMonkey monkey;

    //all gestures known to the monkey
    private Vector<Gesture> mGestures;

    //TODO Vector für Listeners
    private Vector<OnGestureListener> mListeners;

    //classifier who decides which gesture is recognized
    private Classifier classifier;

    // --- inTraining variables ---
    private boolean inTraining = false;
    private Gesture trainingGesture;
    private Vector<Vector<Float[]>> accTrainingSequences;
    private Vector<Vector<Float[]>> gyrTrainingSequences;

    private boolean recordingTrainingSequence = false;
    private Vector<Float[]> accTrainingSequence;
    private Vector<Float[]> gyrTrainingSequence;

    // --- recognition variables ---
    private boolean inRecognition = false;
    private Vector<Float[]> accRecoSequence;
    private Vector<Float[]> gyrRecoSequence;

    //for auto recognition via motion detection
    private boolean autoRecognition = false;
    private MotionDetector mMotionDetector;

    private Vector<Filter> mFilters;

    public static GestureMonkey getInstance(){
        if(monkey == null){
            monkey = new GestureMonkey();
        }
        return monkey;
    }

    private GestureMonkey(){
        mGestures = new Vector<Gesture>();
        classifier = new Classifier();

        mFilters = new Vector<Filter>();

        mListeners = new Vector<OnGestureListener>();
    }

    //##### TRAINING - METHODS #####
    //Everything that has to do with training new gestures

    /**
     * Starts a new training session.<br/><br/>
     * If you call this while another training session takes place, the actual session will be aborted.
     * @param gestureName The name of the gesture you're going to train
     */
    public void startTraining(String gestureName){
        // abort possible training session
        if(inTraining){
            stopTraining(true);
        }

        inTraining = true;
        trainingGesture = new Gesture(gestureName);
        accTrainingSequences = new Vector<Vector<Float[]>>();
        gyrTrainingSequences = new Vector<Vector<Float[]>>();

        //TODO Recognition abbrechen
        // TEST
        // Stops AutoRecognition
//        mMotionDetector.stop();
//        autoRecognition = false;
    }

    /**
     * Ends the actual training session and starts the intern calculations of the new gesture
     * @param abort Set to "true" to abort the actual training session. All progress is lost and no
     *              gesture will be created.
     */
    public void stopTraining(boolean abort){
        inTraining = false;
        // abort actual recording session
        if(recordingTrainingSequence){
            stopTrainingSequence(true);
        }

        if(abort){
            trainingGesture = null;
            accTrainingSequences = null;
            gyrTrainingSequences = null;
        } else{
            trainingGesture.train(accTrainingSequences, gyrTrainingSequences);
            mGestures.add(trainingGesture);
        }
    }

    /**
     * Starts a new recording session for a new inTraining sequence.<br/><br/>
     * If you call this while there's no actual training session, nothing is going to happen.<br/>
     *   --> You have to call "startTraining(gestureName)" first<br/>
     * If you call this while another recording session takes place, the actual session will be aborted.<br/>
     *   --> Call "stopTrainingSequence(abort)" before starting a new recording to avoid losing data
     */
    public void startTrainingSequence(){
        if(inTraining){
            // abort possible recording session
            if(recordingTrainingSequence){
                stopTrainingSequence(true);
            }
            recordingTrainingSequence = true;
            accTrainingSequence = new Vector<Float[]>();
            gyrTrainingSequence = new Vector<Float[]>();
        }
    }

    /**
     * Stops the recording session for the actual training sequence.<br/><br/>
     * If you call this while there's no actual recording session, nothing is going to happen.<br/>
     *   --> You have to call "startTrainingSequence()" first.
     * @param abort Set to "true" to abort the actual recording session. All progress is lost and no
     *              training sequence will be created.
     * @return The amount of training sequences saved for this actual session
     */
    public int stopTrainingSequence(boolean abort){
        if(recordingTrainingSequence){
            recordingTrainingSequence = false;

            if(abort){
                accTrainingSequence = null;
                gyrTrainingSequence = null;
            } else{
                if(accTrainingSequence.size() > 0 && gyrTrainingSequence.size() > 0){
                    accTrainingSequences.add(accTrainingSequence);
                    gyrTrainingSequences.add(gyrTrainingSequence);
                } else{
                    Log.d("GestureMonkey", "No training sequence was recorded (Acceleration-Size: "
                        + accTrainingSequence.size() + " | Gyroscope-Size: " + gyrTrainingSequence.size());
                }
            }

            return accTrainingSequences.size();
        }

        return 0;
    }

    //##### DATA - METHODS #####
    //Methods which are called to send sensor data to the monkey

    /**
     * This methode is called from place whenever new acceleration data should be processed by the monkey
     * @param values The new acceleration data (3 Dimensional)
     */
    public void sendAccData(Float[] values){
        //filter the incoming data
        for(Filter filter : mFilters){
            if(values != null){
                values = filter.filter(values);
            }
        }

        if(values == null)
            return;

        // if in a recording session, just add the values to the actual trainings-sequence
        if(recordingTrainingSequence){
            accTrainingSequence.add(values);
        }
        // if in manual recognition, just add the values to the actual recognition-sequence
        else if(inRecognition && !autoRecognition){
            accRecoSequence.add(values);
        }
        // if in recognition with enabled auto recognition, add values to the recognition-sequence or stop the recognition-process
        else if(inRecognition && autoRecognition){
            Float[] vector = mMotionDetector.checkForMotion(values);

            if(vector != null){
                accRecoSequence.add(vector);
            } else{
                stopRecognition();
                Log.d("Recognition_Progress", "Stop Recognition (senAccData)");
            }
        }
        // if auto recognition is active start a new recognition sequence (depending on the data)
        else if(autoRecognition){
            Float[] vector = mMotionDetector.checkForMotion(values);

            if(vector != null){
                startRecognition();
                accRecoSequence.add(vector);
                Log.d("Recognition_Progress", "Start Recognition (sendAccData)");
            }
        }
    }

    /**
     * This methode is called from place whenever new gyroscope data should be processed by the monkey
     * @param values The new gyroscope data (3 Dimensional)
     */
    public void sendGyrData(Float[] values){
        //TODO Extra Filter für Gyroscope-Daten einrichten

        if(values == null)
            return;

        // if in inTraining, just add the values to the actual trainings-sequence
        if(recordingTrainingSequence){
            gyrTrainingSequence.add(values);
        }
        // if in manual recognition, just add the values to the actual recognition-sequence
        else if(inRecognition && !autoRecognition){
            gyrRecoSequence.add(values);
        }
        //if in recognition with enabled auto recognition, just add the values to the actual recognition-sequence
        //if the recognition process should be enabled is based on the acceleration data (see "sendAccData")
        else if(inRecognition && autoRecognition){
            gyrRecoSequence.add(values);
        }
    }

    //##### FILTER - METHODS #####
    //Everything that has to do with filtering incoming sensor data

    /**
     * Adds a new acceleration filter to the monkey
     * @param filter The filter you want to add
     */
    public void addFilter(Filter filter){
        mFilters.add(filter);
    }

    /**
     * Removes all acceleration filters from the monkey
     */
    public void clearFilters(){
        mFilters.clear();
    }

    //##### RECOGNITION _ METHODS ######
    //Everything that has to do with recognizing trained gestures

    /**
     * Enables the auto recognition via a motion detector
     * @param sensitivity The sensitivity of the detector
     * @param delay The delay of the detector
     */
    public void enableAutoRecognition(double sensitivity, long delay){
        autoRecognition = true;
        mMotionDetector = new MotionDetector(stopsRecognitionHandler, sensitivity, delay);
    }

    /**
     * Disables the auto recognition via a motion detector
     */
    public void disableAutoRecognition(){
        autoRecognition = false;
        mMotionDetector = null;
    }

    //handler which stops the recognition progress if he gets called
    public Handler stopsRecognitionHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            stopRecognition();
            Log.d("Recognition_Progress", "Stop Recognition (Handler)");
        }
    };

    /**
     * Starts the manual recognition progress
     */
    public void startRecognition(){
        inRecognition = true;
        accRecoSequence = new Vector<Float[]>();
        gyrRecoSequence = new Vector<Float[]>();
    }

    /**
     * Stops the recognition progress and informs the listeners if a gesture was recognized or not
     */
    public void stopRecognition(){
        inRecognition = false;

        Gesture g = null;

        if(accRecoSequence.size() > 0){// && gyrRecoSequence.size() > 0){
            // if there's a restriction, test only the gestures you're allowed to
            if(testOnly != null){
                g = classifier.getRecognizedGesture(testOnly, accRecoSequence, gyrRecoSequence);
            } else{
                g = classifier.getRecognizedGesture(mGestures, accRecoSequence, gyrRecoSequence);
            }
        }

        for(OnGestureListener listener : mListeners){
            listener.onGestureRecognized(g);
        }
    }

    public void addOnGestureListener(OnGestureListener listener){
        mListeners.add(listener);
    }

    public void removeOnGestureListener(OnGestureListener listener){
        mListeners.remove(listener);
    }

    //##### EXPORT/IMPORT - METHODS #####
    //Everything that has to do with importing and exporting trained gestures

    /**
     * Export all gestures to a JSON-File
     * @param context
     * @param folderName The name of the folder to save the file
     * @param fileName The name of the file to save the gestures
     */
    public void exportAllGesturesToJSON(Context context, String folderName, String fileName){
        IOManager.exportGestures(context, folderName, fileName, mGestures);
    }

    /**
     * Export only the given gestures to a JSON-File
     * @param context
     * @param folderName The name of the folder to save the file
     * @param fileName The name of the file to save the gestures
     * @param gestureNames All names of gestures that should get exported
     */
    public void exportGesturesToJSON(Context context, String folderName, String fileName, String[] gestureNames){
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(gestureNames));
        Vector<Gesture> selectedGestures = new Vector<Gesture>();

        for(Gesture gesture : mGestures){
            if(names.contains(gesture.name)){
                selectedGestures.add(gesture);
            }
        }

        IOManager.exportGestures(context, folderName, fileName, selectedGestures);
    }

    /**
     * Import all gestures from the given file
     * @param folderName The destination folder
     * @param fileName The file to import
     */
    public void importGesturesFromJSON(String folderName, String fileName){
        Vector<Gesture> importedGestures = IOManager.importGestures(folderName, fileName);

        for(Gesture gesture : importedGestures){
            mGestures.add(gesture);
        }
    }

    /**
     * Import all gestures from the given file
     * @param inputStream An InputStream pointing at the file to import
     */
    public void importGesturesFromJSON(InputStream inputStream){
        Vector<Gesture> importedGestures = IOManager.importGestures(inputStream);

        for(Gesture gesture : importedGestures){
            mGestures.add(gesture);
        }
    }

    //##### GETTERS & SETTERS #####

    /**
     * Returns all gestures known to the monkey
     * @return A Vector of all gestures
     */
    public Vector<Gesture> getAllGestures(){
        return mGestures;
    }

    //##### JUST FOR TESTING #####
    public void fillWithGestures(){
        mGestures.add(new Gesture("1"));
        mGestures.add(new Gesture("2"));
        mGestures.add(new Gesture("3"));
        mGestures.add(new Gesture("4"));
        mGestures.add(new Gesture("5"));
        mGestures.add(new Gesture("6"));
        mGestures.add(new Gesture("7"));
        mGestures.add(new Gesture("8"));
        mGestures.add(new Gesture("9"));
        mGestures.add(new Gesture("10"));
        mGestures.add(new Gesture("11"));
        mGestures.add(new Gesture("12"));
        mGestures.add(new Gesture("13"));
        mGestures.add(new Gesture("14"));
    }

    // set a list with gestures that the monkey is allowed to recognize
    // until you call "removeRestrictionList" the monkey will only test the gestures in testOnly
    private Vector<Gesture> testOnly = null;
    public void addRestrictionList(ArrayList<String> only){
        testOnly = new Vector<Gesture>();
        for(Gesture g : mGestures){
            if(only.contains(g.name)){
                testOnly.add(g);
            }
        }
    }

    public void removeRestrictionList(){
        testOnly = null;
    }
}
