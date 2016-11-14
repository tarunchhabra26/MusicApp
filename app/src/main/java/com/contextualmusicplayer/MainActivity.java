package com.contextualmusicplayer;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.contextualmusicplayer.com.contextualmusicplayer.model.Rule;
import com.contextualmusicplayer.com.contextualmusicplayer.utils.CommonMethods;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.squareup.picasso.Picasso;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback, NavigationView.OnNavigationItemSelectedListener {

    private MusicIntentReceiver myReceiver;

    private final String defaultAlbum = "spotify:user:starbucks:playlist:0LPsYH4hIRjLUKXuZd2vAt";

    // Awareness API Fence

    private GoogleApiClient mApiClient;

    private final String FENCE_KEY = "fence_key";

    private PendingIntent mPendingIntent;

    private FenceReceiver mFenceReceiver;

    // The intent action which will be fired when your fence is triggered.
    private final String FENCE_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";

    private SeekBar seekBar;

    private TextView playListName;

    private String previousPlaylist;

    private long timeElapsed = 0L, finalTime = 0L;


    private final String TAG = getClass().getSimpleName();


    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    // Replace with your client ID
    private static final String CLIENT_ID = "7fa7394578ac43a491bf9a3ec6e07207";
    // Replace with your redirect URI
    private static final String REDIRECT_URI = "contextualmusicplayer://callback";

    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;

    private Player mPlayer;

    private SpotifyApi mSpotifyApi = null;

    private SpotifyService mSpotifyService = null;

    private boolean isPlaying = false;

    private FloatingActionButton playPause = null;

    private Context mCurrent;

    private String mAccessToken;

    private CommonMethods mCommon;

    private Handler durationHandler = new Handler();

    private TextView duration;

    private int forwardTime = 3000, rewindTime = 3000;

    private String currentUri = null;

    private boolean isHeadPhoneToggle = false;

    private Weather weather;

    private SharedPreferences mPrefs = null;
    public static final String PREFS_NAME = "MyRules";
    private List<Rule> ruleList = null;
    private Map<String,AwarenessFence> allRuleFences = null;


    private Runnable updateSeekBarTime = new Runnable() {
        @Override
        public void run() {
            // Get current position
            if (mPlayer != null) {
                timeElapsed = mPlayer.getPlaybackState().positionMs;
                seekBar.setProgress((int) timeElapsed);
                long timeRemaining = finalTime - timeElapsed;
                duration = (TextView) findViewById(R.id.songDuration);
                duration.setText(String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining),
                        TimeUnit.MILLISECONDS.toSeconds((long) timeRemaining) -
                                TimeUnit.MINUTES.toSeconds(
                                        TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining))));
                durationHandler.postDelayed(this, 100);
            }

        }
    };

    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.d("Status :", "OK!");
        }

        @Override
        public void onError(Error error) {
            Log.d("Status :", "Error");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mCommon = new CommonMethods();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize Spotify
        initSpotify();

        // Initialize Awareness API
        initAwarenessAPI();

    }

    private void initPermissions() {
        if (mCommon == null)
            mCommon = new CommonMethods();

        // Check for accessing fine location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
//                mCommon.showMessageOKCancel("You need to allow access to fine location",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                                        REQUEST_CODE_ASK_PERMISSIONS);
//                            }
//                        }, this);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
//                mCommon.showMessageOKCancel("You need to allow access to coarse location",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                                        REQUEST_CODE_ASK_PERMISSIONS);
//                            }
//                        }, this);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }

    }

    private void initAwarenessAPI() {
        if (mCurrent == null)
            mCurrent = getApplicationContext();
        mApiClient = new GoogleApiClient.Builder(mCurrent)
                .addApi(Awareness.API)
                .enableAutoManage(this, 1, null)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        // Set up the PendingIntent that will be fired when the fence is triggered.
                        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
                        mPendingIntent =
                                PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

                        // The broadcast receiver that will receive intents when a fence is triggered.
                        mFenceReceiver = new FenceReceiver();
                        registerReceiver(mFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
                        setupFences();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
    }

    public void next(View view) {
        if (mPlayer != null) {
            if (mPlayer.getPlaybackState().isActiveDevice) {
                mPlayer.skipToNext(mOperationCallback);
            }
        }


    }

    public void previous(View view) {
        if (mPlayer != null) {
            if (mPlayer.getPlaybackState().isActiveDevice) {
                mPlayer.skipToPrevious(mOperationCallback);
            }
        }
    }

    private void setupFences() {
        if (mCommon == null)
            mCommon = new CommonMethods();
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String rulesJson = mPrefs.getString("rules", null);
        if (rulesJson != null) {
            Gson gson = new Gson();
            Type collectionType = new TypeToken<Collection<Rule>>() {
            }.getType();
            Collection<Rule> rules = gson.fromJson(rulesJson, collectionType);
            ruleList = new ArrayList<Rule>(rules);
        }

        allRuleFences = createFences(ruleList);

        if (allRuleFences != null){
            for (final String key : allRuleFences.keySet()){
                Log.d(TAG,"About register fence for rule id : " + key);
                Awareness.FenceApi.updateFences(
                        mApiClient,
                        new FenceUpdateRequest.Builder()
                                .addFence(key, allRuleFences.get(key), mPendingIntent)
                                .build())
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.d(TAG, "Fence was successfully registered for key " + key);
                                } else {
                                    Log.d(TAG, "Fence could not be registered: " + status);
                                }
                            }
                        });
            }
        }
    }

    private Map<String,AwarenessFence> createFences(List<Rule> ruleList) {
        Map<String,AwarenessFence> fences = null;

        if (ruleList == null) {
            return fences;
        }

        fences = new LinkedHashMap<String,AwarenessFence>();

        for (Rule rule : ruleList) {
            AwarenessFence activityFence = null;
            AwarenessFence geoFence = null;
            AwarenessFence combinationFence = null;
            if (rule.getActivityId() != DetectedActivityFence.UNKNOWN) {
                activityFence = DetectedActivityFence.during(rule.getActivityId());
            }
            if (rule.getLatitude() != 0.0 || rule.getLongitude() != 0.0) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    initPermissions();
                }
                geoFence = LocationFence.in(rule.getLatitude(), rule.getLongitude(), rule.getRadius(), 0L);
            }
            if (activityFence != null && geoFence != null){
                combinationFence = AwarenessFence.and(activityFence,geoFence);
                fences.put(rule.getRuleId(),combinationFence);
            } else if (activityFence != null && geoFence == null){
                fences.put(rule.getRuleId(),activityFence);
            } else if (activityFence == null && geoFence != null){
                fences.put(rule.getRuleId(),geoFence);
            }
        }

        if (fences.size() > 0)
            return fences;
        else
            return null;
    }


    private void initSpotify() {
        mSpotifyApi = new SpotifyApi();
        myReceiver = new MusicIntentReceiver();
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming", "user-library-read"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }


    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        super.onResume();
    }

    @Override
    public void onPause() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        // Unregister the fences:
        if(allRuleFences != null){
            for (String key : allRuleFences.keySet()){
                Awareness.FenceApi.updateFences(
                        mApiClient,
                        new FenceUpdateRequest.Builder()
                                .removeFence(key)
                                .build())
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Fence was successfully unregistered.");
                                } else {
                                    Log.e(TAG, "Fence could not be unregistered: " + status);
                                }
                            }
                        });
            }
        }
        super.onPause();

    }

    @Override
    public void onStart() {
        super.onStart();
        playPause = (FloatingActionButton) findViewById(R.id.media_play);
        playListName = (TextView) findViewById(R.id.playList);
        getWeatherData();
    }

    private void getWeatherData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            initPermissions();
        }
        Awareness.SnapshotApi.getWeather(mApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        if (!weatherResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get weather.");
                            if (mCommon == null)
                                mCommon = new CommonMethods();
                            mCommon.createToast(mCurrent,"Weather information not available "
                                    ,Toast.LENGTH_SHORT);
                            return;
                        }
                        weather = weatherResult.getWeather();
                        Log.d(TAG, "Weather: " + weather);
                        if (mCommon == null)
                            mCommon = new CommonMethods();
                    }
                });

        }

    private void playPauseToggle(String uri, boolean isToggle, boolean isReset){
        if (isPlaying && mPlayer != null && isToggle == true) {
            mPlayer.pause(mOperationCallback);
            playPause.setImageDrawable(getResources().
                    getDrawable(android.R.drawable.ic_media_play,
                            MainActivity.this.getTheme()));
            isPlaying = false;
        } else {
            PlaybackState playState = mPlayer.getPlaybackState();
            if (playState.isActiveDevice && !playState.isPlaying) {
                mPlayer.resume(mOperationCallback);
            } else if (playState.isActiveDevice && isReset && currentUri != previousPlaylist){
                mPlayer.playUri(mOperationCallback,uri,0,0);
            } else if (currentUri != previousPlaylist){
                mPlayer.playUri(mOperationCallback,uri,0,0);
            }

            playPause.setImageDrawable(getResources().
                    getDrawable(android.R.drawable.ic_media_pause,
                            MainActivity.this.getTheme()));
            isPlaying = true;
        }
    }
    private void playPauseToggle(String uri, boolean isToggle){
                if (isPlaying && mPlayer != null && isToggle == true) {
                    mPlayer.pause(mOperationCallback);
                    playPause.setImageDrawable(getResources().
                            getDrawable(android.R.drawable.ic_media_play,
                                    MainActivity.this.getTheme()));
                    isPlaying = false;
                } else {
                    PlaybackState playState = mPlayer.getPlaybackState();
                    if (playState.isActiveDevice) {
                        mPlayer.resume(mOperationCallback);
                    } else {
                        mPlayer.playUri(mOperationCallback,uri,0,0);
                    }

                    playPause.setImageDrawable(getResources().
                            getDrawable(android.R.drawable.ic_media_pause,
                                    MainActivity.this.getTheme()));
                    isPlaying = true;
                }
    }

    public void playPauseToggleHandler(View view){
        if (currentUri == null){
            currentUri = defaultAlbum;
        }
        playPauseToggle(currentUri, true);
    }

    public void forward(View view){
        if (mPlayer != null){
            if (timeElapsed + forwardTime <= mPlayer.getMetadata().currentTrack.durationMs){
                timeElapsed += forwardTime;
                mPlayer.seekToPosition(mOperationCallback, (int)timeElapsed);
            }
        }
    }

    public void rewind(View view){
        if (mPlayer != null){
            if (timeElapsed - rewindTime >= 0L){
                timeElapsed -= rewindTime;
                mPlayer.seekToPosition(mOperationCallback, (int)timeElapsed);
            }
        }
    }


        @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                // Setting access token to spotify service
                mAccessToken = response.getAccessToken();
                mSpotifyApi.setAccessToken(response.getAccessToken());

                // Setting SpotifyService instance
                mSpotifyService = mSpotifyApi.getService();

                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " +
                                throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onStop(){
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        // Unregister the fences:
        if(allRuleFences != null){
            for (String key : allRuleFences.keySet()){
                Awareness.FenceApi.updateFences(
                        mApiClient,
                        new FenceUpdateRequest.Builder()
                                .removeFence(key)
                                .build())
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Fence was successfully unregistered.");
                                } else {
                                    Log.e(TAG, "Fence could not be unregistered: " + status);
                                }
                            }
                        });
            }
        }
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        unregisterReceiver(myReceiver);
        // Unregister the fences:
        if(allRuleFences != null){
            for (String key : allRuleFences.keySet()){
                Awareness.FenceApi.updateFences(
                        mApiClient,
                        new FenceUpdateRequest.Builder()
                                .removeFence(key)
                                .build())
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Fence was successfully unregistered.");
                                } else {
                                    Log.e(TAG, "Fence could not be unregistered: " + status);
                                }
                            }
                        });
            }
        }
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        if (mPlayer != null) {
            switch (playerEvent) {
                // Handle event type as necessary
                case kSpPlaybackNotifyPlay:
                    if (mCommon == null)
                        mCommon = new CommonMethods();
                    //mCommon.createToast(mCurrent, "Music playing", Toast.LENGTH_SHORT);
                    Metadata data = mPlayer.getMetadata();
                    Metadata.Track currentTrack = data.currentTrack;
                    TextView songName = (TextView) findViewById(R.id.songName);
                    if (currentTrack != null) {
                    songName.setText(currentTrack.name);
                        TextView artist = (TextView) findViewById(R.id.songArtist);
                        artist.setText(currentTrack.artistName + "(" + currentTrack.albumName + ")");
                        String albumCover = currentTrack.albumCoverWebUrl;
                        ImageView image = (ImageView) findViewById(R.id.mp3Image);
                        Picasso.with(mCurrent).load(albumCover).resize(image.getWidth(), image.getHeight()).into(image);
                        finalTime = currentTrack.durationMs;
                        seekBar = (SeekBar) findViewById(R.id.seekBar);
                        seekBar.setMax((int) finalTime);
                        seekBar.setClickable(false);
                    }
                    if (currentUri == defaultAlbum)
                        playListName.setText("Default");
                    durationHandler.postDelayed(updateSeekBarTime, 100);
                    break;

                case kSpPlaybackNotifyTrackChanged:
                    data = mPlayer.getMetadata();
                    currentTrack = data.currentTrack;
                    songName = (TextView) findViewById(R.id.songName);
                    songName.setText(currentTrack.name);
                    TextView artist = (TextView) findViewById(R.id.songArtist);
                    artist.setText(currentTrack.artistName + "(" + currentTrack.albumName + ")");
                    String albumCover = currentTrack.albumCoverWebUrl;
                    ImageView image = (ImageView) findViewById(R.id.mp3Image);
                    Picasso.with(mCurrent).load(albumCover).resize(image.getWidth(), image.getHeight()).into(image);
                    finalTime = currentTrack.durationMs;
                    seekBar = (SeekBar) findViewById(R.id.seekBar);
                    seekBar.setMax((int) finalTime);
                    seekBar.setClickable(false);
                    durationHandler.postDelayed(updateSeekBarTime, 100);
                    break;

                case kSpPlaybackNotifyNext:
                    playPause.setImageDrawable(getResources().
                            getDrawable(android.R.drawable.ic_media_pause,
                                    MainActivity.this.getTheme()));
                    isPlaying = true;
                    break;

                case kSpPlaybackNotifyPrev:
                    playPause.setImageDrawable(getResources().
                            getDrawable(android.R.drawable.ic_media_pause,
                                    MainActivity.this.getTheme()));
                    isPlaying = true;
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        // Request permissions
        initPermissions();

    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(int i) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_rules) {
            // Handle the navigation to rules display
            manageRuleIntent();
        } else if (id == R.id.nav_add_rule) {
            // Handle to create a new rule
            createRuleIntent();
        } else if (id == R.id.weather_playlist){
            // Get weather and play the playlist
            getWeatherData();
            mCommon.createToast(mCurrent,"Weather based playlist ",Toast.LENGTH_SHORT);
            if (null != weather) {
                String strWeather = mCommon.getWeatherCondition(weather);
                mSpotifyService.searchPlaylists(strWeather, new Callback<PlaylistsPager>() {
                    @Override
                    public void success(PlaylistsPager playlistsPager, Response response) {
                        List<PlaylistSimple> playlists =
                                new ArrayList<PlaylistSimple>(playlistsPager
                                        .playlists.items);
                        if (playlists.size() > 0) {
                            PlaylistSimple playlist = playlists
                                    .get(new Random().nextInt(playlists.size()));
                            currentUri = playlist.uri;
                            if (mPlayer != null) {
                                playPauseToggle(currentUri, false, true);
                                playListName.setText(playlist.name);
                            }
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        if (mCommon == null)
                            mCommon = new CommonMethods();
                        mCommon.createToast(mCurrent, "No weather data, playing default ", Toast.LENGTH_SHORT);
                        if (mPlayer != null) {
                            playPauseToggle(defaultAlbum, false, true);
                        }
                    }
                });
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void manageRuleIntent(){
        Intent intent = new Intent(this, ManageRuleActivity.class);
        startActivity(intent);
    }
    public void createRuleIntent(){
        Intent intent = new Intent(this, CreateRuleActivity.class);
        intent.putExtra("ACCESS_TOKEN",mAccessToken);
        startActivity(intent);
    }


    private class MusicIntentReceiver extends BroadcastReceiver {
        TextView valHPStatus = (TextView) findViewById(R.id.headPhoneStatus);

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d("Context", "Headset is unplugged");
                        valHPStatus.setText("Headphones : Unplugged");
                        if (mPlayer != null){
                            PlaybackState pbState = mPlayer.getPlaybackState();
                            if (pbState != null && pbState.isPlaying && isHeadPhoneToggle) {
                                if (currentUri == null)
                                    currentUri = defaultAlbum;
                                playPauseToggle(currentUri, true);
                                isHeadPhoneToggle = false;
                            }
                        }
                        break;
                    case 1:
                        Log.d("Context", "Headphones :Plugged in");
                        valHPStatus.setText("Headphones : Plugged");
                        if (mPlayer != null) {
                            if (currentUri == null)
                                currentUri = defaultAlbum;
                            playPauseToggle(currentUri, false);
                            isHeadPhoneToggle = true;
                        }
                        break;
                    default:
                        Log.d("Context", "I have no idea what the headset state is");
                        valHPStatus.setText("Unknown");
                }
            }

        }
    }

    public class FenceReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCommon == null)
                mCommon = new CommonMethods();
            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
                mCommon.createToast(mCurrent,
                        "Received an unsupported action in FenceReceiver: action=" + intent.getAction(),
                        Toast.LENGTH_SHORT);
                return;
            }

            // The state information for the given fence is
            FenceState fenceState = FenceState.extract(intent);
            String fenceStateStr = null;
            String fenceKey = null;
            if (allRuleFences != null && allRuleFences.containsKey(fenceState.getFenceKey())) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "true";
                        fenceKey = fenceState.getFenceKey();
                        mCommon.createToast(mCurrent,
                                "Fence state:" + fenceStateStr + " for fenceKey : " + fenceKey,
                                Toast.LENGTH_SHORT);
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "false";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                }
            }
            if (fenceKey != null && ruleList != null){
                Rule detectedRule = null;
                    for (Rule rule : ruleList){
                        if (rule.getRuleId().equals(fenceKey)){
                            detectedRule = rule;
                            break;
                        }
                    }
                if (detectedRule != null && mPlayer != null){
                    previousPlaylist = currentUri;
                    currentUri = detectedRule.getPlaylistUri();
                    playListName.setText(detectedRule.getPlaylistName());
                    mCommon.createToast(mCurrent, "Attempting to play playlist : ",Toast.LENGTH_SHORT);
                    playPauseToggle(currentUri,false,true);
                }
            }
        }


    }
}