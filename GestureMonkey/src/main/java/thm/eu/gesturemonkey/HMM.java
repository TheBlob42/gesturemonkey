package thm.eu.gesturemonkey;

import android.util.Log;

import java.util.Vector;

/**
 * Created by Tobi on 12.01.2015.
 */
public class HMM {
    private final String TAG = "HMM";

    private int numStates; //Number of States in the CombiHMM
    private int numObservations; //Number of possible Observations

    // the probability to start in a certain state
    private double[] initialProb;
    // the probability to switch from state A to state B
    // stateTransitionProb [state A] [state B]
    private double[][] stateTransitionProb;
    // the probability to emmit a symbol V while in state A
    // emissionProb [state A] [symbol V]
    private double[][] emissionProb;

    public double defaultProb = 0.0;

    public double minimalProb = Double.POSITIVE_INFINITY;
    public double averageProb = 0.0;

    public HMM(int numStates, int numObservations){
        this.numStates = numStates;
        this.numObservations = numObservations;

        initialProb = new double[numStates];
        stateTransitionProb = new double[numStates][numStates];
        emissionProb = new double[numStates][numObservations];

        init();
    }

    //Constructor for JSON
    public HMM(int numStates, int numObservations,
               double[] initialProb,
               double[][] stateTransitionProb,
               double[][] emissionProb,
               double defaultProb,
               double averageProb,
               double minimalProb){
        this.numStates = numStates;
        this.numObservations = numObservations;
        this.initialProb = initialProb;
        this.stateTransitionProb = stateTransitionProb;
        this.emissionProb = emissionProb;
        this.defaultProb = defaultProb;
        this.averageProb = averageProb;
        this.minimalProb = minimalProb;
    }

    /**
     * Initializes all necessary values of the Bakis-HMM
     * -> initial probabilities, state transitions probabilities and emission probabilities
     */
    private void init(){
        // Bakis-CombiHMM --> state 0 is always the start-state
        initialProb[0] = 1.0;
        for(int i = 1; i < initialProb.length; i++){
            initialProb[i] = 0.0;
        }

        // stateTransitionsProbabilities e.g. for a 4 state Bakis-CombiHMM
        //    __ 1/3         __1/3          __1/2          __ 1/1
        //   |  |           |  |           |  |           |  |
        //   \__V__         \__V__         \__V__         \__V__
        //   |__0__| -1/3-> |__1__| -1/3-> |__2__| -1/2-> |__3__|
        //      |            A  |            A
        //      |____1/3_____|  |______1/3___|
        int maxTransitionWidth = 2;
        for(int i = 0; i < numStates; i++){
            double numTransitions;
            if((i + maxTransitionWidth) < numStates){
                numTransitions = 3;
            } else if((i + maxTransitionWidth) == numStates){
                numTransitions = 2;
            } else{
                numTransitions = 1;
            }

            for(int j = 0; j < numStates; j++){
                if(j < i){
                    stateTransitionProb[i][j] = 0.0;
                } else if(j <= (i + maxTransitionWidth)){
                    stateTransitionProb[i][j] = 1.0 / numTransitions;
                } else{
                    stateTransitionProb[i][j] = 0.0;
                }
            }
        }

        // equal emission probabilities for all symbols in all states
        for(int i = 0; i < numStates; i++){
            for(int j = 0; j < numObservations; j++){
                emissionProb[i][j] = 1.0 / numObservations;
            }
        }
    }

    /**
     * Optimize the HMM-Parameters towards a certain gesture with the given training-sequences
     * @param sequences The discrete training-sequences that are used to train the HMM
     */
    public void train(Vector<int[]> sequences){
        double[][] newStateTransitionProb = new double[numStates][numStates];
        double[][] newEmissionProb = new double[numStates][numObservations];

        // calculate the new state-transition-probabilities
        for(int i = 0; i < numStates; i++){
            for(int j = 0; j < numStates; j++){
                double numerator = 0.0;
                double denominator = 0.0;

                for(int[] sequence : sequences){
                    double sequenceProb = getSequenceProbability(sequence);

                    double[][] forward = forwardAlgorithm(sequence);
                    double[][] backward = backwardAlgorithm(sequence);

                    double numSum = 0.0;
                    double denomSum = 0.0;
                    for(int t = 0; t < sequence.length - 1; t++){
                        numSum += forward[i][t] * stateTransitionProb[i][j] * emissionProb[i][sequence[t + 1]] * backward[j][t + 1];
                        denomSum += forward[i][t] * backward[i][t];
                    }

                    numerator += (1.0 / sequenceProb) * numSum;
                    denominator += (1.0 / sequenceProb) * denomSum;
                }

                newStateTransitionProb[i][j] = numerator / denominator;
            }
        }

        // calculates the new emission-probabilities
        boolean debug = true;
        for(int i = 0; i < numStates; i++){
            for(int k = 0; k < numObservations; k++){
                double numerator = 0.0;
                double denominator = 0.0;

                for(int[] sequence : sequences){
                    double sequenceProb = getSequenceProbability(sequence);

                    double[][] forward = forwardAlgorithm(sequence);
                    double[][] backward = backwardAlgorithm(sequence);

                    double numSum = 0.0;
                    double denomSum = 0.0;
                    for(int t = 0; t < sequence.length; t++){
                        int sigma = isSymbolPartOfSequence(k, sequence);
                        numSum += forward[i][t] * backward[i][t] * sigma;
                        denomSum += forward[i][t] * backward[i][t];
                    }

                    numerator += (1.0 / sequenceProb) * numSum;
                    denominator += (1.0 / sequenceProb) * denomSum;
                }

                newEmissionProb[i][k] = numerator / denominator;
            }
        }

        stateTransitionProb = newStateTransitionProb;
        emissionProb = newEmissionProb;

        // calculate default probability of this HMM, with the average of all training-sequences
        for(int[] sequence : sequences){
            averageProb += getSequenceProbability(sequence);
        }
        averageProb = averageProb / sequences.size();

        // calculates the minimal probability of this HMM, with the minimal value of all training-sequences
        for(int[] sequence : sequences){
            if(getSequenceProbability(sequence) < minimalProb){
                minimalProb = getSequenceProbability(sequence);
            }
        }

        //default probability of this CombiHMM is the highest probability which one of the training
        //sequences can get, after training
        double[] probs = new double[sequences.size()];
        for(int i=0; i < probs.length; i++){
            probs[i] = getSequenceProbability(sequences.get(i));
        }
        double maxDefProb = 0.0;
        for(double prob : probs){
            if(prob > maxDefProb){
                maxDefProb = prob;
            }
        }
        defaultProb = maxDefProb;
        Log.d(TAG, "DefProb: " + defaultProb);

    }

    public double match(int[] sequence){
        double probability = getSequenceProbability(sequence);

        Log.d(TAG, "*****************");
        Log.d(TAG, "MinProb: " + minimalProb);
        Log.d(TAG, "AveProb: " + averageProb);
        Log.d(TAG, "DefProb: " + defaultProb);
        Log.d(TAG, "SeqProb: " + probability);

        if(probability < minimalProb){
            return 0.0;
        }

        return probability;
    }

    /**
     * Checks if the given sequence (int[]) contains the given symbol (int)
     * @param symbol The symbol you're searching
     * @param sequence The sequence to check
     * @return 0 = sequence contains the symbol | 1 = sequence does not contain the symbol <br/>
     * This result is used for multiplication, therefor the method returns 1 or 0 not true or false
     */
    private int isSymbolPartOfSequence(int symbol, int[] sequence){
        for(int i : sequence){
            if(i == symbol){
                return 1;
            }
        }
        return 0;
    }

    /**
     * Calculates the probability for a given sequence with the forward algorithm
     * @param sequence The discrete sequence for which you want the default probability
     * @return The default probability for the given sequence
     */
    public double getSequenceProbability(int[] sequence){
        double sequenceProb = 0.0;
        double[][] forward = forwardAlgorithm(sequence);

        for(int i = 0; i < numStates; i++){
            sequenceProb += forward[i][sequence.length - 1];
        }

        return sequenceProb;
    }

    // calculates an array with all the forward-variables
    // -> the probability to be in state i and already emitted the first t symbols of the sequence
    // -> forward [state i] [point in time t]
    private double[][] forwardAlgorithm(int[] sequence){
        double[][] forward = new double[numStates][sequence.length];

        for(int i = 0; i < numStates; i++){
            forward[i][0] = initialProb[i] * emissionProb[i][sequence[0]];
        }

        for(int i = 0; i < numStates; i++){
            for(int t = 1; t < sequence.length; t++){
                double sum = 0.0;

                for(int j = 0; j < numStates; j++){
                    sum += forward[j][t - 1] * stateTransitionProb[j][i];
                }

                forward[i][t] = sum * emissionProb[i][sequence[t]];
            }
        }

        return forward;
    }

    // calculates an array with all the backward-variables
    // -> the probability to be in state i and the last T - t symbols of the sequence are going to be emitted
    // -> backward [state i] [point in time t]
    private double[][] backwardAlgorithm(int[] sequence){
        double[][] backward = new double[numStates][sequence.length];

        for(int i = 0; i < numStates; i++){
            backward[i][sequence.length - 1] = 1.0;
        }

        for(int i = 0; i < numStates; i++){
            for(int t = sequence.length - 2; t >= 0; t--){
                double sum = 0.0;

                for(int j = 0; j < numStates; j++){
                    sum += backward[j][t + 1] * stateTransitionProb[i][j] * emissionProb[j][sequence[t + 1]];
                }

                backward[i][t] = sum;
            }
        }

        return backward;
    }

    //### GETTERS ###

    public int getNumberOfStates(){
        return numStates;
    }

    public int getNumberOfObservations(){
        return numObservations;
    }

    public double[] getInitialProbabilities(){
        return initialProb;
    }

    public double[][] getStateTransitionProbabilities(){
        return stateTransitionProb;
    }

    public double[][] getEmissionProbabilities(){
        return emissionProb;
    }

    public double getDefaultProbability(){
        return defaultProb;
    }

    public double getAverageProbability(){
        return averageProb;
    }

    public double getMinimalProbability(){
        return minimalProb;
    }


}
