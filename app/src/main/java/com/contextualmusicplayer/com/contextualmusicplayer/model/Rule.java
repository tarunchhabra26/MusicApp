package com.contextualmusicplayer.com.contextualmusicplayer.model;

/**
 * Created by tarunchhabra on 11/13/16.
 */

public class Rule {

    private String ruleId;
    private String rulename;
    private int activityId;
    private String activityName;
    private String placeName;
    private double latitude;
    private double longitude;
    private long radius;
    private String playlistName;
    private String playlistUri;
    private String userId;

    public Rule(String ruleId, String rulename, int activityId,String activityName, String placeName,
                double latitude, double longitude, long radius, String playlistName,
                String playlistUri,String userId){
        this.ruleId = ruleId;
        this.rulename = rulename;
        this.activityId = activityId;
        this.activityName = activityName;
        this.placeName = placeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.playlistName = playlistName;
        this.playlistUri = playlistUri;
        this.userId = userId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRulename() {
        return rulename;
    }

    public void setRulename(String rulename) {
        this.rulename = rulename;
    }

    public int getActivityId() {
        return activityId;
    }

    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getRadius() {
        return radius;
    }

    public void setRadius(long radius) {
        this.radius = radius;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public String getPlaylistUri() {
        return playlistUri;
    }

    public void setPlaylistUri(String playlistUri) {
        this.playlistUri = playlistUri;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
