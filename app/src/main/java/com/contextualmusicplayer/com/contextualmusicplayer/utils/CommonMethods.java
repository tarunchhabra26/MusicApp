package com.contextualmusicplayer.com.contextualmusicplayer.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.state.Weather;

/**
 * Created by tarunchhabra on 11/7/16.
 */

public class CommonMethods {
    /**
     * Public method to show a toast notification
     * @param context - Context in which the notification has to be shown
     * @param text - The text to be displayed
     * @param duration - The time duration for the text to be visible
     */
    public void createToast(Context context, CharSequence text, int duration) {
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    /**
     * Show dialog box for generating response for permission request
     * @param message
     * @param okListener
     * @param currentActivity
     */
    public void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener,
                                    Activity currentActivity){
        new AlertDialog.Builder(currentActivity)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Gets the weather condition for a given instance of weather
     * @param weather - Weather instance of awareness API
     * @return Weather is a string format, which can be used for searching
     */
    public String getWeatherCondition(Weather weather){
        String condition = "Unknown";
        switch (weather.getConditions()[0]){
            case Weather.CONDITION_CLEAR :
                condition = "Clear+Sky";
                break;
            case Weather.CONDITION_CLOUDY:
                condition = "Cloudy";
                break;
            case Weather.CONDITION_FOGGY:
                condition = "Foggy";
                break;
            case Weather.CONDITION_HAZY:
                condition = "Hazy";
                break;
            case Weather.CONDITION_ICY:
                condition = "Icy";
                break;
            case Weather.CONDITION_RAINY:
                condition = "Rainy";
                break;
            case Weather.CONDITION_SNOWY:
                condition = "Snowy";
                break;
            case Weather.CONDITION_STORMY:
                condition = "Stormy";
                break;
            case Weather.CONDITION_WINDY:
                condition = "Windy";
                break;
            default: condition = "Unknown";
                break;
        }
        return condition;
    }

    public int getActivityID(String activityName){
        int result = 4;
        switch (activityName.toLowerCase()){
            case "in vehicle": result = DetectedActivityFence.IN_VEHICLE;
                break;
            case "on bicycle": result = DetectedActivityFence.ON_BICYCLE;
                break;
            case "on foot": result = DetectedActivityFence.ON_BICYCLE;
                break;
            case "running": result = DetectedActivityFence.RUNNING;
                break;
            case "still": result = DetectedActivityFence.STILL;
                break;
            case "tilting": result = DetectedActivityFence.TILTING;
                break;
            case "walking": result = DetectedActivityFence.WALKING;
                break;
            default: result = DetectedActivityFence.UNKNOWN;
                break;
        }
        return result;
    }
}
