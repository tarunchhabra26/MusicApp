# MusicApp
Contextual Music Application

The application proposed to perform the following :

* The application should integrate with a music streaming service and automate the music playlist selection based on user's context. The context will be computed by using a variety of sensors.

* The application will collect data from sensors such as GPS, cellphone signal tower, nearby Wi-Fi in order to compute user's current location. Using this data the application can trigger a music playlist which user has customized for that particular location. For eg., the user has just entered his home and wants to listen to some Jazz music.

* The application will collect data from gyroscope and accelerometer sensors to understand the current state of the user. For e.g the user might have just entered a vehicle and has plugged in an auxiliary cable to listen to car music. The application should be able to recognize this state and automatically start playing car music playlist.

* Using set of sensor's(location,state and orientation) the application should be able to play a collection of songs based on the current activity of the user. For e.g if the application detects that the user is running and has headphones plugged in then it should start the appropriate music playlist.

## Following information is required to compute user's context ##
* Location
* Activity
* Device specific sensor (headphone plugin)
* Weather

## Current partial implementation has the following
* Integration of Android with Spotify(a streaming music service).
* Ability to detect headphone jack (plugged or unplugged).
* Ability to get users playlist and play a song from it if headphone jack is inserted.
* Ability to play a random album in case the user has to songs saved in his/her library.
* A play pause toggle button to help with playback control.

## How to run the program ##
* Pre-requisite - Have a phone which is compatible with Android API level 23 or above(as per specification), Android Studio, Spotify premium account.
* Launch the app in the phone using ADB
* At the very start the app will ask the user to login into spotify premium account to start streaming music.
* Once done with that the app can start streaming after plugging headphone or using the toggle switch.
