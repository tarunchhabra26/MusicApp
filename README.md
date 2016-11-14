# MusicApp - Adroit
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

## Full implementation has the following
* Integration of Android with Spotify(a streaming music service).
* Ability to detect headphone jack (plugged or unplugged).
* Ability to get users playlist and play a song from it if headphone jack is inserted.
* Ability to play a random album in case the user has to songs saved in his/her library.
* A complete music player with rich UI, play/pause, next/previous and forward/rewind buttons.
* Material UI with App drawer for menu and options and album art.
* Ability to play music based on current local weather condition.
* User has ability to create custom rules for run-time adaptation. A user can choose a combination of geo fence and activity to determine the right choice of his/her own spotify playlist.
* Create and delete operations for such rules.


## How to run the program ##
* Pre-requisite - Have a phone which is compatible with Android API level 23 or above(as per specification), Android Studio, Spotify premium account.
* Launch the app in the phone using ADB.
* Application name will appear as 'Adroit'. Which means "clever or skilful" given it is context aware and adaptable.
* At the very start the app will ask the user to login into spotify premium account to start streaming music.
* Once done with that the app can start streaming after plugging headphone or using the play/pause toggle switch.
* The application is fully integrated with Google Fence and Snapshot API to compute context.
* Following tests can be performed -
  * Playback based on headphone jack plugin and plug-out.
  * Open the app drawer and select 'Weather Playlist'. It will search and play a playlist which has songs which are as per the      weather condition or have similar keywords.
  * Open 'Add Rule' and create a custom rule. A simple one could be activity : still at your current location. Select a user       specific spotify playlist and save. The playlist will play if alll conditions are met. The fences take some time in           registration/de-registration and triggering as the API does it at the most optimized level of battery and CPU.
  * You can create and delete as many rules.
  * Overall the application has worked fine and was tested on a API level 23 phone (OnePlus 2) and it should work fine.           However UI may vary with other devices.
* Please feel free to contact me in cases of issues with setup. Most features are self explanatory.
  
  

## Third party libraries used so far ##
* Spotify authentication library - Helps to authenticate a user with spotify premium account access.
* Spotify play library - Used for streaming and play controls.
* Spotify Web API - Used for retrieving playlist and user's saved tracks or even searching music on spotify.
* Google awareness API - For computing context.
* Piccaso - For fetching album art images
* GSON - For serializing and deseriazing JSONs
* Google Places API - For location based information
