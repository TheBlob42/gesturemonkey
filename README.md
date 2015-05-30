# GestureMonkey
GestureMonkey is a library for 3D gesture recognition, specially designed for Android and Android Wear.  
It uses the acceleroemeter sensor of your Smartphone/Smartwatch to train and recognizes certain movements of the device called gestures. You are able to 
to register listeners that react to a recognized gesture with your individual actions. In this way it offers you a new way of interacting 
with your apps and programms.

### List of contents
* [About this project](#project)
* [About this repository](#repo)
* [Add the library to your project](#add)
* [How to use GestureMonkey](#howto)
    * [Transfer sensor data to GestureMonkey](#transfer)
    * [Train new gestures](#train)
    * [Recognize gestures](#rec)
    * [Import and Export](#import)
* [Filters](#filters)
* [OnGestureListener](#listener)

### <a name="project"></a>About this project
This library and all related elements were developed during my master study at Technische Hochschule Mittelhessen ([www.thm.de](www.thm.de)) as part of 
my development project. Unfortunately i don't have the time right now to work on this project, therefore it has some rough edges. 
Nevertheless i hope it helps you or gives you some good ideas for your own projects.

*Note: This project didnt't use git as system for version control before. Thats the reason why there so few commits right now.*

### <a name="repo"></a>About this repository
This repository contains the whole project, including the library itself and the helper app called *GestureMonkeyExporter*, which lets
you train, test and export gestures for further uses directly on your Smartphone and Smartwatch. If your not interested in the code itself, just get the library .aar file and/or 
the apks:
* [Library](https://db.tt/VM4DdqGG)
* [Exporter (Smartphone-App)](https://db.tt/U86L62NF)
* [Exporter (Smartwatch-App)](https://db.tt/sdTEIndo)

## <a name="add"></a>Add the library to your project
If you're familiar with adding libraries from *.aar files* to your projects, you can skip this explanation.  
You have to manually add the library to your Project. If you're using AndroidStudio just follow these steps:

1.  Navigate to *File* → *New Module*
2.  Choose *Import .JAR or .AAR Package* and click *Next*
3.  Enter the path to the downloaded *GestureMonkey.aar* file and give the new module a name
  +  You should now see a new module in your project structure
4. In the *build.gradle* file of your main module enter the following line to the dependencies:
  ```json
        dependencies {
            //your other dependencies
            compile project(':<the name of your gesture monkey module>')
        }
  ```
5.  Sync the project

## <a name="howto"></a>How to use GestureMonkey
Almost everything you have to do is handled by the central *GestureMonkey* object which is implemented as a Singleton. With the *.getInstance()* method you are always able to access this object.

Before you use any of its functionality you should configure your instance of *GestureMonkey* like in the following example:
```java
    private GestuerMonkey monkey;

    // your other variables

    monkey = GestureMonkey.getInstance();
    monkey.addFilter(new IdleFilter(1.2));
    monkey.addFilter(new DirectionEquivalenceFilter(0.3))
    monkey.addOnGestureListener(this);
```
You should always add an *IdleFilter* and a *DirectionEquivalenceFilter* like above. These two Filters reduce the incoming data of the accelerometer sensor for a better perfomance. You also have the possibility to write your own filters. For more information about the filter classes see [here]().

You should also register a listener (or more if you want to) which should be called if a gesture was recognized. For more information about the OnGestureListener see [here](). 

### <a name="transfer"></a>Transfer sensor data to GestureMonkey
So that the *GestureMonkey* can train and recognize gestures you need to pass him the data from the accelerometer of your device. The GestureMonkey is not doing this automatically for you since you maybe also wanna use this data in your app for other purposes.  

If you never worked with the SensorAPI of Android take a look [here](http://developer.android.com/guide/topics/sensors/sensors_overview.html).  
If you are already familiar with this you just need pass the sensor data inside the *onSensorChanged* method via the *sendAccData(value)* method of the *GestureMonkey* object. Note that you have to convert the data to a *Float-Array*.
```java
    Public class MainActivity extends Activity implements SensorEventListener {
        private SensorManager mSensorManager;
        private Sensor mAccelerometer;
    
        //in onCreate add the following code
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAcceleromter = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        //in onResume add the following code
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        
        //in onPause add the following code
        mSensorManager.unregisterListener(this);
    
        //in onSensorChanged add the following code
        Float[] values = new Float[3];
        values[0] = event.values[0];
        values[1] = event.values[1];
        values[2] = event.values[2];
        monkey.sendAccData(values);
    }
```
Now the *GestureMonkey* is receiving the sensor data and can start working with it.  

*Note: I would recommend to use at least the SENSOR_DELAY_UI constant for the sensordelay. It is not tested if a lower delay will lead to a better recognition.*

### <a name="train"></a> Train new gestures
To train the *GestureMonkey* a new gesture first you have to start the train-mode via a call of *startTraining(gestureName)*. This will also set the name of your new gestures.  

__Important: You are responsible for giving all your gestures unique names. If you don't, this could lead to some errors.__

If you're finished with the training, you can stop it with a call of *stopTraining(false)* or *stopTraining(true)* if you want to abort it and have no use for the gathered data. If you're not aborting the training the GestureMonkey will create a new gesture with the given name based on the sensor data which was gathered.  
After starting the train-mode you have to repeat the gesture for the monkey several times (I recommend at least 10 times). To start and stop the several sequences use the following two methods:

+ *void startTrainingSequence()*  
Starts a new sequence. The movement of the device will be recorded.

+ *void stopTrainingSequence(boolean abort)*  
Ends the actual sequence. The gathered data will be stored until you end the training.
If you're calling this method with a value of *true* for the abort variable, the actual sequence will canceled. All gathered data will be lost. This does not stop the train-mode, so you can keep performing other sequences.

### <a name="rec"></a>Recognize gestures
There are modes to recognize gestures: Manual or Automatic.

#### Manual
Manual means, that you have to tell the *GestureMonkey* when it should listen for an incoming gesture an when to stop listening. Only the data in this period of time will be used to recognize a eventual gesture. Use the following two methods to start and stop the recognition process:

+ *void startRecognition()*  
Starts the manual recognition process. 

+ *void stopRecognition()*  
Ends the manual recognition process. The gathered data will be evaluated and the result will be send to the registered listeners.

#### Automatic
First you have to activate the automatic gesture recognition:

+ *void enableAutoRecognition(double sensitivity, double delay)*  
This activated the automatic gesture recognition. The GestureMonkey now trys to evaluate the beginning and the end of a gesture by itself.
    - *sensitivity*  
This value tells the GestureMonkey how strong a movement of the device has to be to start the automatic recognition process (f.e. 0.5)
    - *delay*  
This value tells the GestureMonkey after how many milliseconds without any further movement the  gesture recognition should end (f.e. 500 – 1000)

+ *void disableAutoRecognition()*  
This disables the automatic gesture recognition.

Of course this method is more vulnerable to errors. Because every motion of the device is a possible gesture there is a chance that the *GestureMonkey* recognizes a gesture even if the user doesn't want to perform one. 

### <a name="import"></a>Import and Export

So you don't want to have the user train all gestures over and over again. Or you just want to supply  your app with a bunch of gestures that are already part of it. For this cases there are several import and export methods you can use.

__Important: If you wish to import or export data from the SD-Card you need the permission in your manifest.xml.__

To import or export gestures you've trained use the follwing methods:

+ *void exportAllGesturesToJSON(Context context, String folderName, String fileName)*
Exports all current trained gestures to a JSON file.

+ *void exportGesturesToJSON(Context context, String folderName, String fileName, String[] gestureNames)*  
Exports only the gestures whose names are given in gestureNames (insofar the were trained).

+ *void importGesturesFromJSON(String folderName, String fileName)*  
Imports all gestures from the given JSON file and adds them to the *GestureMonkey*.

+ *void importGesturesFromJSON(InputStream inputStream)*  
Imports all gestures from the given *InputStream* object and adds them to the GestureMonkey.  
This method is especially for loading a JSON file from your apps asset folder. With the Android method *getAssets().open("filename")* you get the needed *InputStream*.

## <a name="filters"></a>Filters
Filters are there to reduce the amount of incoming sensor data for the *GestureMonkey*. Right now you can only choose between the *IdleFilter* (filters data with no movement) and the *DirectionEquivalenceFilter* (filters data that describes the same movement as previous data).  
To write your own Filter just create a new class and extends the class *Filter*. You then have to write your own filter method the way you want to and thats it. 
Take a look at the *IdleFilter* and *DirectionEquivalenceFilter* classes for some ideas.

To manage the Filters regarded by the *GestureMonkey* use the following methods:  
+ *void addFilter(Filter filter)*  
+ *void clearFilters()*


## <a name="listener"></a>OnGestureListener
The interface *OnGestureListener* has to be implemented from all classes that wants to do something in case a gesture is recognized.

+ *void onGestureRecognized(Gesture gesture)*  
This is the methode which gets called as soon as the *GestureMonkey* has finished another recognition process.The given parameter contains either the recognized gesture (which you can check via *gesture.name*) or *null* if nothing was regocnized.

To un/register a new listener just use the following methods on the *GestureMonkey* object:

+ *void addOnGestureListener(OnGestureListener listener)*  
+ *void removeOnGestureListener(OnGestureListener listener)*

