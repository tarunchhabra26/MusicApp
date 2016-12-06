// IAdroitInterface.aidl
package csc750.tarun.com.adroitmiddleware;

import csc750.tarun.com.adroitmiddleware.IAdroitCallback;
// Declare any non-default types here with import statements

interface IAdroitInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    /*
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    */

    void registerForWeatherUpdate(IAdroitCallback callback);
    void registerForLocationUpdates(IAdroitCallback callback);

    boolean isJackPluggedIn();

}
