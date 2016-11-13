package com.contextualmusicplayer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.contextualmusicplayer.com.contextualmusicplayer.model.Rule;
import com.google.gson.Gson;

public class ViewRule extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_rule);
        Intent intent = getIntent();
        String ruleJson = intent.getStringExtra("RULE_JSON");
        Gson gson = new Gson();
        Rule obj = gson.fromJson(ruleJson, Rule.class);

        TextView heading = (TextView) findViewById(R.id.heading);
        heading.setTextSize(40);
        heading.setText(obj.getRulename());

        TextView activity = (TextView) findViewById(R.id.activity);
        activity.setTextSize(15);
        activity.setText("Activity : " + obj.getActivityName());

        TextView place = (TextView) findViewById(R.id.place);
        place.setTextSize(15);
        place.setText("Place name : " + obj.getPlaceName());

        TextView latitude = (TextView) findViewById(R.id.latitude);
        latitude.setTextSize(15);
        latitude.setText("Latitude : " + obj.getLatitude());

        TextView longitude = (TextView) findViewById(R.id.longitude);
        longitude.setTextSize(15);
        longitude.setText("Longitude : " + obj.getLongitude());

        TextView radius = (TextView) findViewById(R.id.radius);
        radius.setTextSize(15);
        radius.setText("Radius(in meters) : " + obj.getRadius());

        TextView playlist = (TextView) findViewById(R.id.playlist);
        playlist.setTextSize(15);
        playlist.setText("Playlist : " + obj.getPlaylistName());
    }
}
