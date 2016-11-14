package com.contextualmusicplayer;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.contextualmusicplayer.com.contextualmusicplayer.model.Rule;
import com.contextualmusicplayer.com.contextualmusicplayer.utils.CommonMethods;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.bind.CollectionTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.spotify.sdk.android.player.Player;

import org.json.JSONArray;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CreateRuleActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    private SpotifyApi mSpotifyApi = null;
    List<PlaylistSimple> playlists = null;

    public static final String PREFS_NAME = "MyRules";

    private SpotifyService mSpotifyService = null;
    private static final String CLIENT_ID = "7fa7394578ac43a491bf9a3ec6e07207";
    private static final String REDIRECT_URI = "contextualmusicplayer://callback";

    private SharedPreferences mPrefs = null;
    private static SharedPreferences.Editor editor = null;

    private ArrayList<String> spinnerArray;
    private final String TAG = getClass().getSimpleName();
    private SecureRandom random = new SecureRandom();
    private String placeName;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_rule);
        Intent intent = getIntent();

        mSpotifyApi = new SpotifyApi();
        String accessToken = intent.getStringExtra("ACCESS_TOKEN");

        mSpotifyApi.setAccessToken(accessToken);
        // Setting SpotifyService instance
        mSpotifyService = mSpotifyApi.getService();
        spinnerArray = new ArrayList<String>();

        mSpotifyService.getMyPlaylists(new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                playlists = playlistSimplePager.items;
                for (PlaylistSimple playlist : playlists){
                    spinnerArray.add(playlist.name);
                }
                Log.d(TAG, "playlist success");
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)getFragmentManager()
                .findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(placeSelectionListener);


        Spinner playlistSpinner = (Spinner)findViewById(R.id.SpinnerPlaylist);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        spinnerArrayAdapter.add("Choose a playlist");
        playlistSpinner.setAdapter(spinnerArrayAdapter);
        mPrefs = getSharedPreferences(PREFS_NAME,MODE_PRIVATE);

    }

    PlaceSelectionListener placeSelectionListener = new PlaceSelectionListener() {
        @Override
        public void onPlaceSelected(Place place) {
            Log.i(TAG, "Place: " + place.getName());
            LatLng latLng = place.getLatLng();
            TextView latitudeTitle = (TextView)findViewById(R.id.LatitudeTitle);
            latitudeTitle.setText("Latitude : " + latLng.latitude);
            latitude = latLng.latitude;

            TextView longitudeTitle = (TextView)findViewById(R.id.LongitudeTitle);
            longitudeTitle.setText("Longitude : " + latLng.longitude);
            longitude = latLng.longitude;

            placeName = place.getName().toString();

        }

        @Override
        public void onError(Status status) {
            // Handle the error.
            Log.i(TAG, "An error occurred: " + status);
        }
    };

    public void saveRule(View view){
        String rulesJson = null;
        boolean isValid = true;
        EditText ruleName = (EditText)findViewById(R.id.RuleTextName);
        String strRuleName = ruleName.getText().toString();
        if (strRuleName == null || strRuleName.isEmpty()) {
            ruleName.setError("Please enter a rule name");
            isValid = false;
        }
        Spinner activityName = (Spinner)findViewById(R.id.SpinnerActivityType);
        String strActivityName = activityName.getSelectedItem().toString();
        CommonMethods methods = new CommonMethods();
        EditText radius = (EditText)findViewById(R.id.RadiusText);
        String strRadius = radius.getText().toString();
        if (strRadius == null || strRadius.isEmpty()){
            strRadius = "0";
        }
        long lRadius = Long.parseLong(strRadius);
        Spinner playlistName = (Spinner)findViewById(R.id.SpinnerPlaylist);
        String strPlaylistName = playlistName.getSelectedItem().toString();
        if (strPlaylistName.equalsIgnoreCase("Choose a playlist")){
            isValid = false;
            methods.createToast(this, "Please choose atlease one playlist",Toast.LENGTH_SHORT);
        }
        String playlistUri = null;
        if (null != strPlaylistName){
            for (PlaylistSimple playlist : playlists){
                if (playlist.name.equalsIgnoreCase(strPlaylistName)){
                    playlistUri = playlist.uri;
                    break;
                }
            }
        }


        if (mPrefs != null){
           rulesJson = mPrefs.getString("rules",null);
            if (rulesJson == null){
                Log.d(TAG, "Json : " + rulesJson);
                List<Rule> rulesList = new ArrayList<Rule>();
                Rule rule = new Rule(new BigInteger(130, random).toString(32),strRuleName,
                        methods.getActivityID(strActivityName),
                        strActivityName, placeName,latitude,longitude,lRadius, strPlaylistName,
                        playlistUri,"default");
                rulesList.add(rule);
                Gson gson = new Gson();
                String json = gson.toJson(rulesList);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString("rules",json);
                if (isValid) {
                    editor.commit();
                    methods.createToast(this, "Rule id : " + rule.getRuleId() + " rule name :"
                            + rule.getRulename() + " created", Toast.LENGTH_SHORT);
                }
            } else {
                // entry exists
                //methods.createToast(this, "Rules json : " + rulesJson.toString(), Toast.LENGTH_SHORT);
                Log.d(TAG, "Json : " + rulesJson);
                Gson gson = new Gson();
                Type collectionType = new TypeToken<Collection<Rule>>(){}.getType();
                Collection<Rule> rules = gson.fromJson(rulesJson,collectionType);

                Rule rule = new Rule(new BigInteger(130, random).toString(32),strRuleName,
                        methods.getActivityID(strActivityName),
                        strActivityName, placeName,latitude,longitude,lRadius, strPlaylistName,
                        playlistUri,"default");
                rules.add(rule);
                String json = gson.toJson(rules);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString("rules",json);
                if (isValid) {
                    editor.commit();
                    methods.createToast(this, "Rule id : " + rule.getRuleId() + " rule name :"
                            + rule.getRulename() + " created", Toast.LENGTH_SHORT);
                }

            }
            if (isValid) {
                finish();
            }
        }


    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
