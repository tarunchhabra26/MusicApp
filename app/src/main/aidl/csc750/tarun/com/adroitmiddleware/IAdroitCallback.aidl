// IAdroitCallback.aidl
package csc750.tarun.com.adroitmiddleware;

// Declare any non-default types here with import statements

interface IAdroitCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */

    /*
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    */

    void weatherUpdate(int weatherCondition);
    void locationUpdateCallback(String latitude, String longitude);

}
