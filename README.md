# GestureMonkey
GestureMonkey is a library for 3D gesture recognition specially designed for Android and Android Wear. 
It uses the acceleroemeter sensor of your Smartphone/Smartwatch to train and recognizes special movements of the device. You are able to 
to register listeners that react to a recognized gesture with your individual actions. In this way it offers you a new way of interacting 
with your apps and programms.

## About this project
This library and all related elements were developed during my master study at Technische Hochschule Mittelhessen as part of 
my development project. Unfortunately i don't have the time right now to work on this project, therefore it has some rough edges. 
Nevertheless i hope it helps you or gives you some good ideas for your own projects.

## About this repository
This repository contains the whole project, including the library itself and the helper app called "GestureMonkeyExporter", which lets
you train, test and export gestures for further uses. If your not interested in the code itself, just get the library file and/or 
the apks:
* Library
* Exporter (Smartphone-App)
* Exporter (Smartwatch-App)

## Add the library to your project
If your familiar with adding libraries as .aar files to your projects, just skip this step.

You have to manually add the library to your Project. If you're using AndroidStudio just follow these steps:

1.  Navigate to “File → New Module”
2.  Choose “Import .JAR or .AAR Package” and click “Next”
3.  Enter the path to the downloaded “GestureMonkey.aar” file and give the new module a name
  3.  You should now see a new module in your project structure
4. In the “build.gradle” file of your main module enter the following line to the dependencies:

    dependencies {
  
      //your other dependencies
    
      compile project(':<the name of your gesture monkey module>')
    
    }
5.  Sync the project

## How to use GestureMonkey
Almost everything you have to do is handled by the central GestureMonkey object which is implemented as Singleton. 
With the “.getInstance()”-Method you can always access this object.

Before you use any of its functionality you should configure the instance of GestureMonkey like in the following example:

  private GestuerMonkey monkey;

  // your other variables

  monkey = GestureMonkey.getInstance();
  monkey.addFilter(new IdleFilter(1.2));
  monkey.addFilter(new DirectionEquivalenceFilter(0.3))
  monkey.addOnGestureListener(this);
  
You should always add an IdleFilter and a DirectionEquivalenceFilter like above. These two Filters reduce the incoming data of the accelerometer sensor for a better perfomance. You also have the possibility to write your own filters. For more information about the filter classes see <Link>.

You should also register a listener (ore more if you want to) which should be called if a gesture was recognized. For more information about the OnGestureListener see <link>. 
