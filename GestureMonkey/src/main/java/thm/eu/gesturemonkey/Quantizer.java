package thm.eu.gesturemonkey;

import java.util.Vector;

/**
 * Created by Tobi on 12.01.2015.
 */
public class Quantizer {
    private final int numClusterCentroids = 14;
    private float[][] clusterCentroids;

    //if true the KMean-Algorithm is used for clustering the centroids
    //if false no clustering will take place (centroids will just be calculated one time)
    private boolean useKMEanForClustering = false;

    //if true each quantized sequence will be clustered before the return
    private boolean clusterQuantizedSequence = false;

    /**
     * Class for quantizing incoming sensor data into discrete sequences <br/>
     * (for acceleration data i recommend (true, true) for gyroscope (false, true))
     * @param useKMeanForClustering Should the quantizer uses the KMeans-Algorithm to refine the cluster centroids?
     * @param clusterQuantizedSequence Should the quantizer clusters the discrete sequence after their calculation?
     */
    public Quantizer(boolean useKMeanForClustering, boolean clusterQuantizedSequence){
        this.useKMEanForClustering = useKMeanForClustering;
        this.clusterQuantizedSequence = clusterQuantizedSequence;

        clusterCentroids = new float[numClusterCentroids][3];
    }

    public Quantizer(boolean useKMeanForClustering, boolean clusterQuantizedSequence, float[][] clusterCentroids){
        this.useKMEanForClustering = useKMeanForClustering;
        this.clusterQuantizedSequence = clusterQuantizedSequence;
        this.clusterCentroids = clusterCentroids;
    }

    /**
     * Calculates the optimal cluster centroids for this quantizer (uses KMean for refining)
     * @param points All the points of all training-sequences in one Vector<Float[]>
     */
    public void quantize(Vector<Float[]> points){
        float max = Float.NEGATIVE_INFINITY;
        float min = Float.POSITIVE_INFINITY;

        //determine the min/max values (x, y or z) of all the points
        for(Float[] values : points){
            for(Float value : values){
                if(value > max){
                    max = value;
                }
                if(value < min){
                    min = value;
                }
            }
        }

        float radius = (Math.abs(max) + Math.abs(min)) / 2;

        double pi = Math.PI;
        //X und Y Achsen
        //           ^ y                o           ^ z
        //           |                  o           |
        //        o  2  o               o        o  8  o
        //      3    |    1             o      10   |    9
        //    o      |      o           o    o      |      o
        // ---4-------------0---> x     o ---o-------------o---> x
        //    o      |      o           o    o      |      o
        //      5    |    7             o      12   |    13
        //        o  6  o               o        o  11 o

        clusterCentroids[0] = new float[] {radius, 0.0f, 0.0f};
        clusterCentroids[1] = new float[] {(float) Math.cos(pi / 4) * radius, (float) Math.sin(pi / 4) * radius, 0.0f};
        clusterCentroids[2] = new float[] {0.0f, radius, 0.0f};
        clusterCentroids[3] = new float[] {(float) Math.cos(3 * pi / 4) * radius, (float) Math.sin(3 * pi / 4) * radius, 0.0f};
        clusterCentroids[4] = new float[] {-radius, 0.0f, 0.0f};
        clusterCentroids[5] = new float[] {(float) Math.cos(5 * pi / 4) * radius, (float) Math.sin(5 * pi / 4) * radius, 0.0f};
        clusterCentroids[6] = new float[] {0.0f, -radius, 0.0f};
        clusterCentroids[7] = new float[] {(float) Math.cos(7 * pi / 4) * radius, (float) Math.sin(7 * pi / 4) * radius, 0.0f};
        clusterCentroids[8] = new float[] {0.0f, 0.0f, radius};
        clusterCentroids[9] = new float[] {(float) Math.sin(pi / 4) *radius, 0.0f, (float) Math.cos(pi / 4) * radius};
        clusterCentroids[10] = new float[] {(float) Math.sin(3 * pi / 4) *radius, 0.0f, (float) Math.cos(3 * pi / 4) * radius};
        clusterCentroids[11] = new float[] {0.0f, 0.0f, -radius};
        clusterCentroids[12] = new float[] {(float) Math.sin(5 * pi / 4) *radius, 0.0f, (float) Math.cos(5 * pi / 4) * radius};
        clusterCentroids[13] = new float[] {(float) Math.sin(7 * pi / 4) *radius, 0.0f, (float) Math.cos(7 * pi / 4) * radius};

        //use KMean-Algorithm to refine the centroids (if requested)
        if(useKMEanForClustering){
            float[][] map = mapPointsToCentroids(points);
            float[][] map_old;

            do {
                map_old = copyArray(map);

                clusterCentroids = recalculateClusterCentroids(points, map);
                map = mapPointsToCentroids(points);
            } while (!compareArrays(map, map_old));
        }
    }

    /**
     * Recalculates the cluster centroids based on the points which belong to it.
     * Add up all the points which belong to one cluster centroid. Calculate the average and set
     * this data as the new cluster centroid of this group.
     * @param points All the points of all training-sequences in one Vector<Float[]>
     * @param map An array which holds the mapping of the points to the cluster centroids
     * @return The new calculated cluster centroids
     */
    private float[][] recalculateClusterCentroids(Vector<Float[]> points, float[][] map){
        float[][] centroids = copyArray(clusterCentroids);

        // foreach centroid (row)
        for(int i = 0; i < map.length; i++){
            float numMembers = 0;
            float sumX = 0;
            float sumY = 0;
            float sumZ = 0;
            // foreach point (column)
            for(int j = 0; j < map[i].length; j++){
                // if point belongs to centroid
                if(map[i][j] == 1){
                    numMembers++;
                    sumX += points.get(j)[0];
                    sumY += points.get(j)[1];
                    sumZ += points.get(j)[2];
                }
            }

            if(numMembers > 0){
                // new centroid is the average of all points which belongs to that centroid
                centroids[i][0] = sumX / numMembers;
                centroids[i][1] = sumY / numMembers;
                centroids[i][2] = sumZ / numMembers;
            }
        }

        return centroids;
    }

    /**
     * Maps each point to the closest cluster centroid
     * @param points All the points of all training-sequences in one Vector<Float[]>
     * @return An array which contains the mapping of the points to the cluster centroids
     */
    private float[][] mapPointsToCentroids(Vector<Float[]> points){
        //            point1 point2 ...
        // centroid1:   0      1
        // centroid2:   0      0
        // centroid3:   1      0

        //initialize the map array:
        float[][] map = new float[clusterCentroids.length][points.size()];
        for(float[] row : map){
            for(int i = 0; i < row.length; i++){
                row[i] = 0;
            }
        }

        for(int i = 0; i < points.size(); i++){
            float[] point = new float[3];
            point[0] = points.get(i)[0];
            point[1] = points.get(i)[1];
            point[2] = points.get(i)[2];

            int index = 0;
            float distance = Float.POSITIVE_INFINITY;

            for(int j = 0; j < clusterCentroids.length; j++){
                float newX = point[0] - clusterCentroids[j][0];
                float newY = point[1] - clusterCentroids[j][1];
                float newZ = point[2] - clusterCentroids[j][2];

                float abs = (float) Math.sqrt(Math.pow(newX, 2) + Math.pow(newY, 2) + Math.pow(newZ, 2));

                if(abs < distance){
                    index = j;
                    distance = abs;
                }
            }

            map[index][i] = 1;
        }

        return map;
    }

    /**
     * Returns the discrete sequence for the passed point-sequence based on the cluster centroids
     * @param points The point-sequence which should be converted to a discrete sequence
     * @return The discrete sequence of the passed data
     */
    public int[] getQuantizedSequence(Vector<Float[]> points){
        int[] sequence = new int[points.size()];
        float[][] map = mapPointsToCentroids(points);

        //foreach point (points and calculated map have the same length)
        for(int i = 0; i < points.size(); i++){
            //get the related centroid
            for(int j = 0; j < map.length; j++){
                if(map[j][i] == 1){
                    sequence[i] = j;
                }
            }
        }

        //cluster the quantized sequence (1 2 2 3 3 1 -> 1 2 3 1)
        if(clusterQuantizedSequence){
            sequence = clusterSequence(sequence);
        }

        return sequence;
    }

    //******* HELPER METHODS *******

    /**
     * Make a deep copy of an two dimensional float array
     * @param source The source array
     * @return The new copied array
     */
    private float[][] copyArray(float[][] source){
        float[][] copy = new float[source.length][source[0].length];
        for(int i = 0; i < source.length; i++){
            for(int j = 0; j < source[0].length; j++){
                copy[i] = source[i];
            }
        }
        return copy;
    }

    /**
     * Compares two two dimensional float arrays to each other
     * @param array1
     * @param array2
     * @return true = both arrays contain the same values (at the same positions) |
     * false = the arrays values are unequal
     */
    private boolean compareArrays(float[][] array1, float[][] array2){
        for(int i = 0; i < array1.length; i++){
            for(int j = 0; j < array1[0].length; j++){
                if(array1[i][j] != array2[i][j]){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Clusters same sequential values inside a sequence <br/>
     * 1 2 2 3 2 2 1 1 1 --> 1 2 3 2 1
     * @param sequence The sequence you want to cluster
     * @return The clustered sequence
     */
    private int[] clusterSequence(int[] sequence){
        Vector<Integer> filteredSequence = new Vector<Integer>();
        int lastInt = -1;
        for(int value : sequence){
            if(value != lastInt){
                filteredSequence.add(value);
                lastInt = value;
            }
        }

        int[] filtered = new int[filteredSequence.size()];
        for(int i=0; i < filteredSequence.size(); i++){
            filtered[i] = filteredSequence.get(i);
        }

        return filtered;
    }

    //### GETTERS ###

    public float[][] getClusterCentroids(){
        return clusterCentroids;
    }

    public boolean usesKMeanForClustering(){
        return useKMEanForClustering;
    }

    public boolean clustersQuantizedSequences(){
        return clusterQuantizedSequence;
    }
}
