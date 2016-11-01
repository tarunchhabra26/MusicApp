package com.contextualmusicplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;


import java.util.List;

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

public class MainActivity extends Activity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private MusicIntentReceiver myReceiver;

    // Replace with your client ID
    private static final String CLIENT_ID = "7fa7394578ac43a491bf9a3ec6e07207";
    // Replace with your redirect URI
    private static final String REDIRECT_URI = "contextualmusicplayer://callback";

    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;

    private Player mPlayer;

    //private String mAccessToken;

    private SpotifyApi mSpotifyApi = null;

    private SpotifyService mSpotifyService = null;

    private List<SavedTrack> mDefaultTracks = null;

    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.d("Status :","OK!");
        }

        @Override
        public void onError(Error error) {
            Log.d("Status :","Error");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSpotifyApi = new SpotifyApi();

        myReceiver = new MusicIntentReceiver();

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-private", "streaming", "user-library-read"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);


    }

    @Override public void onResume() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        super.onResume();
    }

    @Override public void onPause() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                //mAccessToken = response.getAccessToken();

                // Setting access token to spotify service
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
               Toast.makeText(MainActivity.this,"Delivered",Toast.LENGTH_LONG).show();
               SavedTrack track = mDefaultTracks.get(0);
               mDefaultTracks.remove(0);
               Log.d("Next track :" , track.track.name);
               mPlayer.queue(mOperationCallback, track.track.uri);

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
        //mPlayer.playUri(null, "spotify:track:2TpxZ7JUBn3uw46aR7qd6V", 0, 0);

       /* mSpotifyService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.d("Album failure", spotifyError.toString());
                mSpotifyService.getAlbum("2dIGnmEIy1WZIcZCFSj6i8", new Callback<Album>() {
                    @Override
                    public void success(Album album, Response response) {
                        Log.d("Album success", album.name);
                        Log.d("Album release date", album.release_date);
                        Log.d("Album type", album.type);

                        mPlayer.playUri(null, "spotify:album:2dIGnmEIy1WZIcZCFSj6i8", 0, 0);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d("Album failure", error.toString());
                    }
                });
            }

            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                List<SavedTrack> tracks = savedTrackPager.items;
                int range = (tracks.size() - 0);
                int random = (int)(Math.random() * range) + 0;
                Track currentTrack = tracks.get(random).track;
                String uri = currentTrack.uri;
                Log.d("Current track uri: ", uri);
                tracks.remove(random);
                mDefaultTracks = tracks;
                mPlayer.setShuffle(mOperationCallback,true);
                mPlayer.playUri(mOperationCallback,"spotify:album:2dIGnmEIy1WZIcZCFSj6i8",0,0);
            }
        });*/




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

    private class MusicIntentReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d("Context", "Headset is unplugged");
                        if (mPlayer != null)
                            mPlayer.pause(mOperationCallback);
                        break;
                    case 1:
                        Log.d("Context", "Headset is plugged");
                        if (mPlayer != null) {
                            PlaybackState pState = mPlayer.getPlaybackState();
                            if (pState.isActiveDevice)
                                mPlayer.resume(mOperationCallback);
                            else {
                                mSpotifyService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
                                    @Override
                                    public void failure(SpotifyError spotifyError) {
                                        Log.d("Album failure", spotifyError.toString());
                                        mSpotifyService.getAlbum("0K4pIOOsfJ9lK8OjrZfXzd", new Callback<Album>() {
                                            @Override
                                            public void success(Album album, Response response) {
                                                Log.d("Album success", album.name);
                                                Log.d("Album release date", album.release_date);
                                                Log.d("Album type", album.type);

                                                mPlayer.playUri(null, "spotify:album:0K4pIOOsfJ9lK8OjrZfXzd", 0, 0);
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                Log.d("Album failure", error.toString());
                                            }
                                        });
                                    }

                                    @Override
                                    public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                                        List<SavedTrack> tracks = savedTrackPager.items;
                                        int range = (tracks.size() - 0);
                                        int random = (int) (Math.random() * range) + 0;
                                        Track currentTrack = tracks.get(random).track;
                                        String uri = currentTrack.uri;
                                        Log.d("Current track uri: ", uri);
                                        tracks.remove(random);
                                        mDefaultTracks = tracks;
                                        mPlayer.setShuffle(mOperationCallback, true);
                                        mPlayer.playUri(mOperationCallback, "spotify:album:0K4pIOOsfJ9lK8OjrZfXzd", 0, 0);
                                    }
                                });
                            }
                        }
                        break;
                    default:
                        Log.d("Context", "I have no idea what the headset state is");
                }
            }

        }
    }
}