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

import com.contextualmusicplayer.com.contextualmusicplayer.utils.CommonMethods;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
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


import java.util.List;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback,NavigationView.OnNavigationItemSelectedListener {

    private MusicIntentReceiver myReceiver;

    private final String defaultAlbum  = "spotify:album:1zHfDPtlXk2Biq8iVS1I3F";

    // Awareness API Fence

    private GoogleApiClient mApiClient;

    private final String FENCE_KEY = "fence_key";

    private PendingIntent mPendingIntent;

    private FenceReceiver mFenceReceiver;

    // The intent action which will be fired when your fence is triggered.
    private final String FENCE_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";

    private SeekBar seekBar;
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

    private List<SavedTrack> mDefaultTracks = null;

    private boolean isPlaying = false;

    private FloatingActionButton playPause = null;

    private Context mCurrent;

    private String mAccessToken;

    private CommonMethods mCommon;

    private Handler durationHandler = new Handler();

    private TextView duration;

    private int forwardTime = 3000, rewindTime = 3000;


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
                mCommon.showMessageOKCancel("You need to allow access to fine location",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        }, this);
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
                mCommon.showMessageOKCancel("You need to allow access to coarse location",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        }, this);
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

    public void next(View view){
        if (mPlayer != null){
            if (mPlayer.getPlaybackState().isActiveDevice){
                mPlayer.skipToNext(mOperationCallback);
            }
        }


    }

    public void previous(View view){
        if (mPlayer != null){
            if (mPlayer.getPlaybackState().isActiveDevice){
                mPlayer.skipToPrevious(mOperationCallback);
            }
        }
    }
    private void setupFences() {
        if (mCommon == null)
            mCommon = new CommonMethods();
        AwarenessFence walkingFence = DetectedActivityFence.during(DetectedActivityFence.WALKING);

        // Register the fence to receive callbacks.
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence(FENCE_KEY, walkingFence, mPendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            mCommon.createToast(mCurrent,
                                    "Fence was successfully registered", Toast.LENGTH_SHORT);
                            Log.d(TAG, "Fence was successfully registered.");
                        } else {
                            mCommon.createToast(mCurrent,
                                    "Fence could not be registered:", Toast.LENGTH_SHORT);
                            Log.d(TAG, "Fence could not be registered: " + status);
                        }
                    }
                });

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
        // Unregister the fence:
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .removeFence(FENCE_KEY)
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
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();

        getWeatherData();

        playPause = (FloatingActionButton) findViewById(R.id.media_play);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying && mPlayer != null) {
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
                        mSpotifyService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
                            @Override
                            public void failure(SpotifyError spotifyError) {
                                Log.d("Album failure", spotifyError.toString());
                                mSpotifyService.getAlbum("4DOcG4A40Wf3q2vPNGQwQg",
                                        new Callback<Album>() {
                                            @Override
                                            public void success(Album album, Response response) {
                                                Log.d("Album success", album.name);
                                                Log.d("Album release date", album.release_date);
                                                Log.d("Album type", album.type);

                                                mPlayer.playUri(null,
                                                        defaultAlbum,
                                                        0, 0);
                                               /* String uri = "spotify:album:0K4pIOOsfJ9lK8OjrZfXzd";
                                                Intent launcher = new Intent( Intent.ACTION_VIEW, Uri.parse(uri) );
                                                startActivity(launcher);*/

                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                Log.d("Album failure", error.toString());
                                            }
                                        });
                            }

                            @Override
                            public void success(Pager<SavedTrack> savedTrackPager,
                                                Response response) {
                                List<SavedTrack> tracks = savedTrackPager.items;
                                int range = (tracks.size() - 0);
                                int random = (int) (Math.random() * range) + 0;
                                Track currentTrack = tracks.get(random).track;
                                String uri = currentTrack.uri;
                                Log.d("Current track uri: ", uri);
                                tracks.remove(random);
                                mDefaultTracks = tracks;
                                mPlayer.setShuffle(mOperationCallback, true);
                                mPlayer.playUri(mOperationCallback,
                                        defaultAlbum, 0, 0);

                                /*
                                String uri2 = "spotify:user:filtrindia:playlist:4nNVfQ9eWidZXkBKZN5li4";
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setAction("android.media.action.MEDIA_PLAY_FROM_SEARCH");
                                intent.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.MainActivity"));
                                intent.setData(Uri.parse(uri2));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                if (intent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(intent);
                                }

                                Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                i.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.internal.receiver.MediaButtonReceiver"));
                                i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
                                sendOrderedBroadcast(i, null);

                                i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                i.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.internal.receiver.MediaButtonReceiver"));
                                i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
                                sendOrderedBroadcast(i, null);
                                */

                            }
                        });
                    }
                    playPause.setImageDrawable(getResources().
                            getDrawable(android.R.drawable.ic_media_pause,
                                    MainActivity.this.getTheme()));
                    isPlaying = true;
                }
            }
        });
    }

    private void getWeatherData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Awareness.SnapshotApi.getWeather(mApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        if (!weatherResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get weather.");
                            return;
                        }
                        Weather weather = weatherResult.getWeather();
                        Log.d(TAG, "Weather: " + weather);
                        if (mCommon == null)
                            mCommon = new CommonMethods();
                        mCommon.createToast(mCurrent,"Weather : " + weather,Toast.LENGTH_SHORT);
                        weather.getConditions();
                    }
                });

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
        super.onStop();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);

    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            case kSpPlaybackNotifyPlay:
                if (mCommon == null)
                    mCommon = new CommonMethods();

                mCommon.createToast(mCurrent, "Music playing", Toast.LENGTH_SHORT);
                SavedTrack track = mDefaultTracks.get(0);
                mDefaultTracks.remove(0);
                Log.d("Next track :", track.track.name);
                mPlayer.queue(mOperationCallback, track.track.uri);
                isPlaying = true;
                playPause.setImageDrawable(getResources().
                        getDrawable(android.R.drawable.ic_media_pause,
                                MainActivity.this.getTheme()));
                Metadata data = mPlayer.getMetadata();
                Metadata.Track currentTrack = data.currentTrack;
                TextView songName = (TextView) findViewById(R.id.songName);
                songName.setText(currentTrack.name);
                TextView artist = (TextView) findViewById(R.id.songArtist);
                artist.setText(currentTrack.artistName + "(" + currentTrack.albumName + ")");
                String albumCover = currentTrack.albumCoverWebUrl;
                ImageView image = (ImageView) findViewById(R.id.mp3Image);
                Picasso.with(mCurrent).load(albumCover).resize(image.getWidth(),image.getHeight()).into(image);
                finalTime = currentTrack.durationMs;
                seekBar = (SeekBar) findViewById(R.id.seekBar);
                seekBar.setMax((int) finalTime);
                seekBar.setClickable(true);
                durationHandler.postDelayed(updateSeekBarTime, 100);
                break;

            case kSpPlaybackNotifyTrackChanged:
                data = mPlayer.getMetadata();
                currentTrack = data.currentTrack;
                songName = (TextView) findViewById(R.id.songName);
                songName.setText(currentTrack.name);
                artist = (TextView) findViewById(R.id.songArtist);
                artist.setText(currentTrack.artistName + "(" + currentTrack.albumName + ")");
                albumCover = currentTrack.albumCoverWebUrl;
                image = (ImageView) findViewById(R.id.mp3Image);
                Picasso.with(mCurrent).load(albumCover).resize(image.getWidth(),image.getHeight()).into(image);
                finalTime = currentTrack.durationMs;
                seekBar = (SeekBar) findViewById(R.id.seekBar);
                seekBar.setMax((int) finalTime);
                seekBar.setClickable(false);
                durationHandler.postDelayed(updateSeekBarTime, 100);
                break;

            default:
                break;
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
        } else if (id == R.id.nav_add_rule) {
            // Handle to create a new rule
            createRuleIntent();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                        valHPStatus.setText("Headphones : Unplugged, please plugin to start");
                        if (mPlayer != null)
                            mPlayer.pause(mOperationCallback);
                            playPause.setImageDrawable(getResources().
                                getDrawable(android.R.drawable.ic_media_play,
                                        MainActivity.this.getTheme()));
                            isPlaying = false;
                        break;
                    case 1:
                        Log.d("Context", "Headphones :Plugged in");
                        valHPStatus.setText("Plugged");
                        if (mPlayer != null) {
                            PlaybackState pState = mPlayer.getPlaybackState();
                            if (pState.isActiveDevice)
                                mPlayer.resume(mOperationCallback);
                            else {
                                mSpotifyService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
                                    @Override
                                    public void failure(SpotifyError spotifyError) {
                                        Log.d("Album failure", spotifyError.toString());
                                        mSpotifyService.getAlbum("4DOcG4A40Wf3q2vPNGQwQg",
                                                new Callback<Album>() {
                                            @Override
                                            public void success(Album album, Response response) {
                                                Log.d("Album success", album.name);
                                                Log.d("Album release date", album.release_date);
                                                Log.d("Album type", album.type);

                                                mPlayer.playUri(null,
                                                        defaultAlbum,
                                                        0, 0);
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                Log.d("Album failure", error.toString());
                                            }
                                        });
                                    }

                                    @Override
                                    public void success(Pager<SavedTrack> savedTrackPager,
                                                        Response response) {
                                        List<SavedTrack> tracks = savedTrackPager.items;
                                        int range = (tracks.size() - 0);
                                        int random = (int) (Math.random() * range) + 0;
                                        Track currentTrack = tracks.get(random).track;
                                        String uri = currentTrack.uri;
                                        Log.d("Current track uri: ", uri);
                                        tracks.remove(random);
                                        mDefaultTracks = tracks;
                                        mPlayer.setShuffle(mOperationCallback, true);
                                        mPlayer.playUri(mOperationCallback,
                                                defaultAlbum, 0, 0);
                                    }
                                });
                            }
                            playPause.setImageDrawable(getResources().
                                    getDrawable(android.R.drawable.ic_media_pause,
                                            MainActivity.this.getTheme()));
                            isPlaying = true;
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
            if (TextUtils.equals(fenceState.getFenceKey(), FENCE_KEY)) {
                String fenceStateStr;
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "true";
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
                mCommon.createToast(mCurrent,
                        "Fence state:" + fenceStateStr,
                        Toast.LENGTH_SHORT);

            }
        }


        }
    }