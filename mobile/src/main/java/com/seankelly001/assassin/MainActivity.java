/*
This class handles most of the app functionality, including the main screen, invitations, the lobby
and the game itself.

I had to do all of this functionality in this class as the room (where players are connected to each
other) would disconnect if a new activity was started.
*/
package com.seankelly001.assassin;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.Plus;

import com.google.example.games.basegameutils.BaseGameUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener,
        OnMapReadyCallback, SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback {

    //The tag of the app used for debugging purposes
    final static String TAG = "ASSASSIN";

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    //==============================================================================================
    //Map variables
    private GoogleMap mMap;
    private Location mLastLocation;
    private SensorManager sensorManager = null;
    private  boolean ROTATION_VECTOR_SUPPORTED;

    private LocationManager location_manager;
    private LocationListener location_listener;
    private String best_provider;

    //==============================================================================================
    //Game configuration variables

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    private String mRoomId = null;
    private Room mRoom = null;

    // The participants in the currently active game
    private final ArrayList<Participant> mParticipants = new ArrayList<>();

    // My participant ID in the currently active game
    private String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    private String mIncomingInvitationId = null;
    private boolean waiting_room_finished = false;

    //The maximum and minimum players in a game, not including the current player
    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 7;

    //How long the game should last
    private final int INITIAL_GAME_MINS = 5;
    private final int INITIAL_GAME_SECS = 0;
    private int current_game_mins = INITIAL_GAME_MINS;
    private int current_game_secs = INITIAL_GAME_SECS;

    //How often data should be sent to other players in game
    private final int GAME_TICK_SECONDS = 3;

    //How close a player must be to kill another player (in metres)
    private final int KILL_MAX_DISTANCE = 5;

    //==============================================================================================
    //In game variables

    private MapUtils map_tools;
    private ArrayList<Player> player_list = new ArrayList<>();
    private ScoreboardPlayerAdapter scoreboard_player_adapter;

    private static GameState GAME_STATE = GameState.NOT_IN_GAME;
    private Participant host, target, hunter;
    private Location target_location, hunter_location;
    private boolean been_killed_wait = false;
    private Player dead_player = null;

    private static ArrayList<byte[]> picture_byte_array_list;

    private ArrayList<Pair<String, String>> chat_array_list = new ArrayList<>();
    private ChatListAdapter chat_list_adapter;

    //==============================================================================================
    //Lobby variables

    private static final HashMap<String, Boolean> ready_players_map = new HashMap<>();
    private LobbyPlayerListAdapter lobby_player_list_adapter;

    private boolean lobby_countdown_begun = false;
    private final int INITIAL_LOBBY_COUNTDOWN_SECONDS = 3;
    private int lobby_countdown_seconds = INITIAL_LOBBY_COUNTDOWN_SECONDS;
    private boolean waitForIdLobby = true;

    //==============================================================================================
    //Message variables

    private final char FLAG_KILL = 'A';
    private final char FLAG_LOBBY_READY = 'B';
    private final char FLAG_HUNTER = 'C';
    private final char FLAG_TARGET = 'D';
    private final char FLAG_COORDINATES = 'E';
    private final char FLAG_TEXT = 'G';
    private final char FLAG_BEEN_KILLED = 'H';
    private final char FLAG_REQUEST_PICTURE = 'I';
    private final char FLAG_PICTURE = 'J';
    private final char FLAG_SCORE_KILL = 'K';
    private final char FLAG_SCORE_DEATH = 'L';
    private final char FLAG_CHAT_MESSAGE = 'M';

    //==============================================================================================
    //Preference variables

    private static SharedPreferences preferences;
    private static final String PREFERENCES_NAME = "com.seankelly001.assassin";
    private static final String IMAGE_PATH_KEY = "IMAGE_PATH_KEY";
    private static final String FIRST_TIME_USER_KEY = "FIRST_TIME_USER_KEY";

    //==============================================================================================
    //UI variables

    private ProgressBar target_distance_progress, hunter_distance_progress;
    private TextView target_distance_text, hunter_distance_text, killed_view, game_over_text_view,
            lobby_countdown_view, game_countdown_view, recent_message_view;

    private ImageView target_photo_view;
    private AlertDialog image_alert_dialog, first_time_user_dialog, leave_game_alert;;
    private Button show_photo_button, show_scoreboard_button,  kill_button;

    private ListView final_scoreboard_list_view, scoreboard_list_view, lobby_player_list_view,
            lobby_chat_list, game_chat_list;

    private LinearLayout scoreboard_layout, game_over_layout, game_over, game_items_layout,
            game_chat_layout, killed_gray_overlay_layout;

    private EditText chat_lobby_input, chat_game_input;
    //The current screen showing
    private int mCurScreen = -1;

    // This array lists all the individual screens our game has.
    private final static int[] SCREENS = {
            R.id.screen_main, R.id.screen_sign_in, R.id.screen_wait, R.id.screen_lobby,
            R.id.screen_map
    };

    //Array of clickable buttons
    private final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_decline_popup_invitations,
            R.id.button_invite_players, R.id.button_quick_game, R.id.button_see_invitations,
            R.id.button_sign_in, R.id.button_sign_out, R.id.kill_button, R.id.settings,
            R.id.exit_game_button, R.id.show_chat_button, R.id.about_game
    };


    //==============================================================================================
    //==============================================================================================

    //This method is first called when we start the game
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);

        Log.e("#####", "F: onCreate");

        // Create the Google Api Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }

        //Register the view from the layout
        setUpViews();

        //Get preferences, and check if user is using app for first time
        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);
        checkFirstTimeUser();

        //Register the rotation sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ROTATION_VECTOR_SUPPORTED = sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);

        registerLocationUpdates();
    }


    //This method is called after we return from a different activity
    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {

        super.onActivityResult(requestCode, responseCode, intent);

        Log.v(TAG, "REQUEST CODE: " + requestCode);
        Log.v(TAG, "RESPONSE CODE: " + responseCode);

        //The request code corresponds to the activity that was called
        switch (requestCode) {
            // we got the result from the "select players" UI -- ready to create the room
            case RC_SELECT_PLAYERS:
                handleSelectPlayersResult(responseCode, intent);
                break;
            // we got the result from the "select invitation" UI (invitation inbox). We're
            // ready to accept the selected invitation:
            case RC_INVITATION_INBOX:
                handleInvitationInboxResult(responseCode, intent);
                break;
            // we got the result from the "waiting room" UI.
            case RC_WAITING_ROOM:
                if(!waiting_room_finished) {
                    if (responseCode == Activity.RESULT_OK) {
                        // ready to start playing
                        Log.v(TAG, "Starting game (waiting room returned OK).");
                        attemptStartLobby();
                    }
                    // player indicated that they want to leave the room
                    else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM
                            || responseCode == Activity.RESULT_CANCELED)
                        leaveRoom();
                }
                break;
            //Returned from the Google sign in screen
            case RC_SIGN_IN:
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                //Connect to the Google API client
                if (responseCode == RESULT_OK)
                    mGoogleApiClient.connect();
                else
                    BaseGameUtils.showActivityResultError(this,requestCode,responseCode, R.string.signin_other_error);
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }


    //Called when player selects "Quick Game" on main screen
    //Allows players to play with random people
    private void startQuickGame() {

        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_PLAYERS,
                MAX_PLAYERS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }


    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {

        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.v(TAG, "Select players UI succeeded.");

        //Get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Log.v(TAG, "Invitee count: " + invitees.size());

        //Get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
        }

        //Create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
        Log.v(TAG, "Room created, waiting for it to be ready...");
    }


    //Handle the result of the invitation inbox UI, where the player can pick an invitation
    //to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {

        //If response is not good, return to main screen
        if (response != Activity.RESULT_OK) {
            Log.v(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }
        Log.d(TAG, "Invitation inbox UI succeeded.");
        //Accept the invitation
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);
        acceptInviteToRoom(inv.getInvitationId());
    }


    //Accept the given invitation.
    void acceptInviteToRoom(String invId) {

        //Accept the invitation
        Log.v(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        //Switch to waiting screen and join the room
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }


    //Decline invitation
    void declineInviteToRoom(String invId) {

        if(invId != null) {
            Games.RealTimeMultiplayer.declineInvitation(mGoogleApiClient, invId);
            onInvitationRemoved(invId);
        }
    }


    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {

        Log.d(TAG, "**** got onStop");

        //Leave the room (if applicable)
        leaveRoom();
        sensorManager.unregisterListener(this);

        //Stop keeping the screen on
        stopKeepingScreenOn();

        //If not signed in, go to sign in screen, else to to wait screen
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected())
            switchToScreen(R.id.screen_sign_in);
        else
            switchToScreen(R.id.screen_wait);
        super.onStop();
    }


    /* Activity just got to the foreground. We switch to the wait screen because we will now
    go through the sign-in flow. Every time the app starts, we have to go through the sign in flow.
    However, if user is authenticated, the sign in flow succeeds and the user simply sees the
    waiting screen. */
    @Override
    public void onStart() {

        //Go to waiting screen
        switchToScreen(R.id.screen_wait);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.v(TAG, "Client already connected");
            //Make sure the game state is correct, and go to main screen
            GAME_STATE = GameState.NOT_IN_GAME;
            switchToScreen(R.id.screen_main);
        }
        else {
            Log.v(TAG, "Connecting client.");
            mGoogleApiClient.connect();
        }
        super.onStart();
    }


    //Show the waiting room UI to track the progress of other players as they enter the room and connect.
    void showWaitingRoom(Room room) {

        //Minimum players required to start the game. Need +1 as not counting ourself in MIN_PLAYERS variable
        Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS + 1);
        //Show waiting room UI
        mRoom = room;
        startActivityForResult(intent, RC_WAITING_ROOM);
    }


    //Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {

        //Store incoming invitation and display invitation popup
        mIncomingInvitationId = invitation.getInvitationId();
        String invitation_text = invitation.getInviter().getDisplayName() + " invited you to a game!";
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(invitation_text);
        findViewById(R.id.invitation_popup).setVisibility(mIncomingInvitationId != null ? View.VISIBLE : View.GONE);
    }


    //Called when we either decline an invitation or it is no longer valid
    @Override
    public void onInvitationRemoved(String invitationId) {

        if (mIncomingInvitationId!=null && mIncomingInvitationId.equals(invitationId)) {
            mIncomingInvitationId = null;
            findViewById(R.id.invitation_popup).setVisibility(View.GONE);
        }
    }


    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    //Called when sign in is successful
    @Override
    public void onConnected(Bundle connectionHint) {

        Log.d(TAG, "onConnected() called. Sign in successful!");
        Log.d(TAG, "Sign-in succeeded.");

        //Register listener so we are notified if we receive an invitation to play while we are in the game
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {

            //Check if we are accepting an invitation, and if it is valid, accept it
            Invitation inv = connectionHint.getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG,"onConnected: connection hint has a room invite!");
                acceptInviteToRoom(inv.getInvitationId());
                return;
            }
        }

        if(mMap != null && map_tools != null) {
            Location current_location = getLocation();
            map_tools.updateMap(current_location);
        }
        switchToMainScreen();
    }


    //Connection is suspended, try to reconnect
    @Override
    public void onConnectionSuspended(int i) {

        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }


    //Connection has failed
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.v(TAG, "onConnectionFailed() called, result: " + connectionResult);

        //Attempt to resolve the connection (if applicable)
        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, getString(R.string.signin_other_error));
        }
        //Switch to sign in screen
        switchToScreen(R.id.screen_sign_in);
    }


    //Called when we are connected to the room
    @Override
    public void onConnectedToRoom(Room room) {

        //Get participants and my ID:
        mParticipants.clear();
        mParticipants.addAll(room.getParticipants());
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        //Save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
        if(mRoomId==null)
            mRoomId = room.getRoomId();

        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");

        //A check to make sure we don't start the lobby unless we have an id
        if(waitForIdLobby) {
            waitForIdLobby = false;
            startLobby();
        }
    }


    //Called when we've successfully left the room (this happens a result of voluntarily leaving
    //via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }


    //Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        showGameError();
    }


    //Show error message about game being cancelled and return to main screen.
    void showGameError() {

        //Don't do anything if game has ended (viewing final scoreboard)
        if(GAME_STATE != GameState.GAME_ENDED) {
            BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem));
            makeToast("GAME ERROR");
            leaveGame();
            switchToMainScreen();
        }
    }


    //Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {

        //Error creating room
        if (statusCode != GamesStatusCodes.STATUS_OK) {

            Log.e(TAG, "Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        mRoomId = room.getRoomId();
        //Show the waiting room UI
        showWaitingRoom(room);
    }


    //Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {

        //Status code is not good
        if (statusCode != GamesStatusCodes.STATUS_OK) {

            Log.e(TAG, "Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }


    //Called when joining a room
    @Override
    public void onJoinedRoom(int statusCode, Room room) {

        //Status code is not good
        if (statusCode != GamesStatusCodes.STATUS_OK) {

            Log.e(TAG, "Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        //Show the waiting room UI
        showWaitingRoom(room);
    }


    //Another player has declined an invitation
    @Override
    public void onPeerDeclined(Room room, List<String> peers) {

        updateRoom(room);
    }


    //A player has been invited to the game
    @Override
    public void onPeerInvitedToRoom(Room room, List<String> peers) {

        updateRoom(room);
    }


    //Called when a player disconnects from the room
    @Override
    public void onP2PDisconnected(String participant_id) {

        makeToast("Player has disconnected (2)");
        Participant p = getParticipantById(participant_id);
        if(p != null)
            participantLeft(p);
    }


    @Override
    public void onP2PConnected(String participant) {

        Log.d(TAG, "Participant connected: " + participant);
    }


    //Called when player(s) joins room
    @Override
    public void onPeerJoined(Room room, List<String> peers) {

        updateRoom(room);
    }


    //Called when player(s) leaves the game (voluntarily)
    @Override
    public void onPeerLeft(Room room, List<String> peers) {

        Log.v(TAG, "peers left: " + peers.toString());
        try {
            for(String peer: peers) {
                Participant p = getParticipantById(peer);
                if(p != null)
                    participantLeft(p);
            }
        }
        catch (Exception e) {Log.e("#####", e.toString());}
        updateRoom(room);
    }


    //Called when auto matching a game, when quick game is selected
    @Override
    public void onRoomAutoMatching(Room room) {

        updateRoom(room);
    }


    @Override
    public void onRoomConnecting(Room room) {

        updateRoom(room);
    }


    //Called when players have connected to room
    @Override
    public void onPeersConnected(Room room, List<String> peers) {

        //Update the room and see if we should start the lobby
        updateRoom(room);
        attemptStartLobby();
    }


    //Called when players disconnect from the room
    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {

        try {
            for(String peer: peers) {
                Participant p = getParticipantById(peer);
                if(p != null) {
                    //Notify the player of the other players that have left
                    makeToast(p.getDisplayName() + " has disconnected");
                    participantLeft(p);
                }
            }
        }
        catch (Exception e) {Log.e("#####", e.toString());}
        updateRoom(room);
    }


    //Called when players join or leave the room, either from disconnecting or voluntarily
    void updateRoom(Room room) {

        if (room != null) {

            //Update the participants list
            mParticipants.clear();
            mParticipants.addAll(room.getParticipants());

            //Iterate over the participants list
            Iterator<Participant> iterator = mParticipants.iterator();
            while(iterator.hasNext()){

                Participant p = iterator.next();
                //Participant has declined invitation or they've left, remove them from list
                if(p.getStatus() == Participant.STATUS_DECLINED
                    || p.getStatus() == Participant.STATUS_LEFT
                    || p.getStatus() == Participant.STATUS_UNRESPONSIVE) {
                    Log.v(TAG, "Removing: " + p.getDisplayName() + " " + p.getStatus());
                    iterator.remove();
                    //Also remove them from the player list
                    player_list.remove(getPlayerById(p.getParticipantId()));
                }
            }

            Log.d(TAG, "ROOM UPDATE - " + mParticipants.size());
            //Check if there are enough players to continue the game (if in game)
            //If there are not, end the game
            if(GAME_STATE == GameState.GAME_INPROGRESS && mParticipants.size() <= MIN_PLAYERS) {

                //End the game and notify the user
                makeToast("THERE ARE NOT ENOUGH PLAYERS TO CONTINUE THE GAME");
                gameOver("There are not enough players to continue the game");
            }
            //If a player leaves in the lobby, end the game/lobby
            else if(GAME_STATE == GameState.IN_LOBBY) {

                //If not enough players, end lobby
                if(mParticipants.size() <= MIN_PLAYERS) {
                    makeToast("THERE ARE NOT ENOUGH PLAYERS TO START THE GAME");
                    leaveRoom();
                }
                else
                    lobby_player_list_adapter.notifyDataSetChanged();
            }
        }
    }


    //================================LOBBY SECTION=================================================

    //Called when players accept invites/connect to room
    private void attemptStartLobby() {

        //Go through player list, if not all are joined, return (do nothing)
        for(Participant p: mParticipants) {
            Log.v(TAG, p.getDisplayName() + " : " + p.getStatus());
            if(p.getStatus() != Participant.STATUS_JOINED)
                return;
        }
        Log.v(TAG, "STARTING LOBBY!");
        //Finish the waiting room activity and start the lobby
        waiting_room_finished = true;
        finishActivity(RC_WAITING_ROOM);
        startLobby();
    }


    //Start the lobby. Here players can chat with each other, and inform each other when they are
    //ready to start the game
    void startLobby() {

        switchToScreen(R.id.screen_lobby);
        GAME_STATE = GameState.IN_LOBBY;

        //Add players to the lobby ready map, which indicated if players are ready to start the game
        for (Participant p : mParticipants) {

            String p_id = p.getParticipantId();
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if(p_id != null)
                ready_players_map.put(p_id, false);
        }

        //Add a click listener corresponding to the checkbox beside your player name
        //Players tick their box to indicate they are ready to start the game
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CheckBox player_ready_checkbox_view = (CheckBox) v;
                boolean ready = player_ready_checkbox_view.isChecked();
                if(mMyId != null)
                    ready_players_map.put(mMyId, ready);

                //Check the lobby state, i.e. if all players are ready to start the game
                checkLobbyState();

                boolean ready_status = ready_players_map.get(mMyId);
                byte[] message_bytes = {FLAG_LOBBY_READY, (byte) (ready_status ? 1 : 0)};

                //Send a message to all players indicating you are ready (or not) to start the game
                for (Participant p : mParticipants) {

                    //Don't send to self
                    if (p.getParticipantId().equals(mMyId) || p.getStatus() != Participant.STATUS_JOINED)
                        continue;
                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                            mRoomId, p.getParticipantId());
                }
            }
        };

        lobby_player_list_adapter = new LobbyPlayerListAdapter(this, mParticipants, ready_players_map, mMyId, listener);
        lobby_player_list_view.setAdapter(lobby_player_list_adapter);

        //Host is always the first player in the room list
        host = mParticipants.get(0);
        String host_name = host.getDisplayName();
        Log.v(TAG, "HOST IS: " + host_name);

        chat_list_adapter = new ChatListAdapter(this, chat_array_list);
        lobby_chat_list.setAdapter(chat_list_adapter);
    }


    //Check if all players are ready to start the game. If they are, begin lobby countdown
    private void checkLobbyState() {

        //All players are ready and countdown has not yet begun, start lobby countdown
        if(DataTypeUtils.hashMapBool(ready_players_map) && !lobby_countdown_begun) {

            Log.d(TAG, "All Players Are Ready");
            Toast.makeText(this, "All Players Are Ready, Begin Countdown", Toast.LENGTH_LONG).show();
            startLobbyCountdown();
        }
        //All players are not ready, but countdown is in progress. Stop lobby countdown
        else if(!DataTypeUtils.hashMapBool(ready_players_map) && lobby_countdown_begun)
            stopLobbyCountdown();

    }


    //Start the lobby countdown. When it finished (reaches 0), start the game
    public void startLobbyCountdown() {

        //Countdown ticks down every second
        lobby_countdown_begun = true;
        lobby_countdown_view.setVisibility(View.VISIBLE);
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (lobby_countdown_seconds <= 0 || !lobby_countdown_begun)
                    return;
                lobbyCountdownTick();
                h.postDelayed(this, 1000);
            }
        }, 1000);
    }


    //Stop the lobby countdown (a player is no longer ready to start the game)
    public void stopLobbyCountdown() {

        lobby_countdown_view.setVisibility(View.INVISIBLE);
        lobby_countdown_view.setText("0:0" + INITIAL_LOBBY_COUNTDOWN_SECONDS);
        lobby_countdown_seconds = INITIAL_LOBBY_COUNTDOWN_SECONDS;
        lobby_countdown_begun = false;
    }


    //Game tick -- update countdown, check if game ended.
    void lobbyCountdownTick() {

        if(lobby_countdown_begun) {
            if (lobby_countdown_seconds > 0)
                --lobby_countdown_seconds;

            //Update countdown
            String countdown_text = "0:" +
                    (lobby_countdown_seconds < 10 ? "0" : "")
                    + String.valueOf(lobby_countdown_seconds);
            (lobby_countdown_view).setText(countdown_text);

            //If countdown reaches 0, start the game
            if (lobby_countdown_seconds <= 0) {
                //Start the game
                Toast.makeText(this, "COUNTDOWN COMPLETE", Toast.LENGTH_SHORT).show();
                startGame();
            }
        }
    }

    //================================GAME MAP SECTION==============================================

    //Is current player the host of the game
    private boolean isHost() {

        try {return host.getParticipantId().equals(mMyId);}
        catch (Exception e) { Log.e(TAG, e.toString()); return false;}
    }


    //Start the game
    private void startGame() {

        GAME_STATE = GameState.GAME_INPROGRESS;
        kill_button.setEnabled(false);

        //Switch to map screen
        switchToScreen(R.id.screen_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Clear the map (in case any relics from previous game)
        if(mMap != null)
            mMap.clear();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);

        //Set up chat
        game_chat_list.setAdapter(chat_list_adapter);

        //Create a player instance for each player (participant) in the game
        for(Participant p: mParticipants)
            player_list.add(new Player(p));

        //If current user is the host, have to set up game
        if(isHost()) {

            //Players are initially set up in a circular pattern chasing each other
            int num_players = player_list.size();
            for(int i = 0; i < num_players; i++) {

                Player current_player = player_list.get(i);

                //Players target is the next player in the list
                Player current_players_target = player_list.get((i+1)%num_players);
                setPlayersTarget(current_player, current_players_target);

                //If first player in list, the hunter is the last player in list
                Player current_players_hunter = player_list.get((i == 0 ? num_players-1 : i-1));
                setPlayersHunter(current_player, current_players_hunter);
            }
        }

        //Set up scoreboard
        scoreboard_player_adapter = new ScoreboardPlayerAdapter(this, player_list, mMyId, null);
        scoreboard_list_view.setAdapter(scoreboard_player_adapter);

        //Send data periodically, handled in gameTick
        final int milliseconds = 1000;
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (GAME_STATE != GameState.GAME_INPROGRESS)
                    return;
                gameTick();
                h.postDelayed(this, milliseconds);
            }
        }, milliseconds);
    }


    //Called periodically every second
    private void gameTick() {

        //Send coordinates periodically, but not every second
        if(current_game_secs % GAME_TICK_SECONDS == 0)
            sendCoordinatesMessage();

        //Update the game countdown
        String countdown_text = "" + (current_game_mins < 10? "0":"") + current_game_mins + ":"
                + (current_game_secs < 10? "0":"") + current_game_secs;
        game_countdown_view.setText(countdown_text);

        //If countdown is over, end game
        if(current_game_mins <= 0 && current_game_secs <= 0) {

            makeToast("GAME HAS ENDED");
            GAME_STATE = GameState.GAME_ENDED;
            gameOver("Time limit reached");
            return;
        }
        //Update game countdown variables
        else if(current_game_secs == 0) {

            current_game_mins--;
            current_game_secs = 60;
        }
        current_game_secs--;
    }


    //Leave the game voluntarily
    private void leaveGame() {

        if (GAME_STATE == GameState.GAME_INPROGRESS || GAME_STATE == GameState.GAME_ENDED) {

            makeToast("GAME ENDED");
            leaveRoom();
            switchToMainScreen();
        }
    }


    /*End the game, either from time running out, the host leaves or there are not enough players
    to continue the game */
    private void gameOver(String message) {

        makeToast("GAME OVER!");
        GAME_STATE = GameState.GAME_ENDED;

        //Get the winner of the game
        ArrayList<Player> final_player_list = new ArrayList<>(player_list);
        Collections.sort(final_player_list);
        Collections.reverse(final_player_list);
        String winner_id = final_player_list.get(0).getId();

        //Set up final scoreboard and end game screen
        ScoreboardPlayerAdapter final_scoreboard_adapter
                = new ScoreboardPlayerAdapter(this, final_player_list, mMyId, winner_id);
        final_scoreboard_list_view.setAdapter(final_scoreboard_adapter);

        killed_view.setVisibility(View.GONE);
        game_over_layout.setVisibility(View.VISIBLE);
        game_over.setVisibility(View.VISIBLE);
        final_scoreboard_list_view.setVisibility(View.VISIBLE);
        game_over_text_view.setText("Game Over - " + message);

        //Try to move the chat view from the game screen to the end-game screen
        try {

            game_items_layout.removeView(game_chat_layout);
            game_over_layout.addView(game_chat_layout);
            game_chat_layout.setVisibility(View.VISIBLE);
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }


    //User has clicked exit game
    private void exitGame() {

        leaveRoom();
        //switchToMainScreen();
    }


    //Leave the room.
    void leaveRoom() {

        Log.d(TAG, "Leaving room");
        if (mGoogleApiClient != null && mRoomId != null) {

            Log.e(TAG, "Leave room successful");
            //This informs other users in room you have left voluntarily
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            GAME_STATE = GameState.NOT_IN_GAME;
            switchToScreen(R.id.screen_wait);
        }
        else {
            GAME_STATE = GameState.NOT_IN_GAME;
            switchToMainScreen();
        }
        resetGameVars();
    }


    //Reset the in game variables. This is done between games to stop any relics from previous games
    //interfering with the new one
    private void resetGameVars() {

        mRoom = null;
        mRoomId = null;
        mMyId = null;
        lobby_countdown_begun = false;
        waitForIdLobby = true;
        ready_players_map.clear();
        player_list.clear();
        chat_array_list.clear();
        lobby_player_list_adapter = null;
        lobby_countdown_seconds = INITIAL_LOBBY_COUNTDOWN_SECONDS;
        current_game_mins = INITIAL_GAME_MINS;
        current_game_secs = INITIAL_GAME_SECS;
        dead_player = null;
        hunter = null;
        target = null;
        game_over_text_view.setText("GAME OVER!!");
        game_over_layout.setVisibility(View.GONE);
        final_scoreboard_list_view.setVisibility(View.GONE);
        lobby_countdown_view.setVisibility(View.GONE);
        killed_gray_overlay_layout.setVisibility(View.GONE);
        kill_button.setEnabled(false);
        chat_array_list.clear();

        try {
            if(game_over_layout.findViewById(R.id.game_chat_layout)!= null){

                game_over_layout.removeView(game_chat_layout);
                game_items_layout.addView(game_chat_layout);
                game_chat_layout.setVisibility(View.GONE);
                recent_message_view.setText("");
            }
        }
        catch(IllegalStateException e) {
            Log.e(TAG, e.toString());
        }
    }


    //This is called when a player (participant) leaves the game
    private void participantLeft(Participant p) {

        try {

            ready_players_map.remove(p.getParticipantId());
            //If the player who has left is the host, end the game/lobby
            if (p.equals(host)) {

                if(GAME_STATE == GameState.IN_LOBBY) {
                    makeToast("HOST HAS LEFT");
                    leaveRoom();
                }
                else {
                    player_list.remove(getPlayerById(host.getParticipantId()));
                    if(GAME_STATE == GameState.GAME_INPROGRESS)
                        gameOver("Host (" + p.getDisplayName() + ") has left the game");
                }
            }
            //If a player has left, host must deal with it
            else if (isHost()) {
                //If player who left was the dead player, no need to do anything
                if (p == dead_player)
                    dead_player = null;
                else
                    playerLeft(p.getParticipantId());
            }
        }
        catch(Exception e) {
            Log.e("#####", e.toString());
        }
    }


    //Only host calls this method after players leaves to resolve the new game state
    private void playerLeft(String sender_id) {

        Player player_who_left = getPlayerById(sender_id);

        if(player_who_left != null && player_who_left != dead_player) {

            String player_name = player_who_left.getDisplayName();

            Player player_who_lefts_target = player_who_left.getTarget();
            Player player_who_lefts_hunter = player_who_left.getHunter();

            //If there is no dead player
            if(dead_player == null) {

                setPlayersTarget(player_who_lefts_hunter, player_who_lefts_target);
                setPlayersHunter(player_who_lefts_target, player_who_lefts_hunter);
            }
            //If there is a dead player, have to insert them into the game
            else {

                setPlayersTarget(player_who_lefts_hunter, dead_player);
                setPlayersHunter(dead_player, player_who_lefts_hunter);
                setPlayersTarget(dead_player, player_who_lefts_target);
                setPlayersHunter(player_who_lefts_target, dead_player);
                //There is now do dead player
                dead_player = null;
            }
            sendGameStateChatMessage(player_name + " has left the game");
            player_list.remove(player_who_left);
            scoreboard_player_adapter.notifyDataSetChanged();
        }
    }


    //================================MESSAGING SECTION==============================================
    //For a lot of the messages, the host will "send" the message to himself. As the API doesn't
    //Actually allow this, they just call the corresponding receive message function directly


    //Called when receiving a message from another player
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {

        byte[] message_bytes = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.v(TAG, "Message received from  " + sender + ", message length: " + message_bytes.length);
        char flag = (char) message_bytes[0];

        //Decide what to do with the message depending on the flag
        switch (flag) {

            case FLAG_LOBBY_READY: receivedLobbyReadyMessage(sender, message_bytes); break;
            case FLAG_COORDINATES: receivedCoordinateMessage(sender, message_bytes); break;
            case FLAG_KILL: receivedKillMessage(sender, message_bytes); break;
            case FLAG_TARGET: receivedTargetMessage(sender, message_bytes); break;
            case FLAG_HUNTER: receivedHunterMessage(sender, message_bytes); break;
            case FLAG_TEXT: receivedTextMessage(sender, message_bytes); break;
            case FLAG_BEEN_KILLED: receivedBeenKilledMessage(sender, message_bytes); break;
            case FLAG_REQUEST_PICTURE: receivedPictureRequestMessage(sender, message_bytes); break;
            case FLAG_PICTURE: receivedPictureMessage(sender, message_bytes); break;
            case FLAG_SCORE_KILL: receivedScoreKillMessage(sender, message_bytes); break;
            case FLAG_SCORE_DEATH: receivedScoreDeathMessage(sender, message_bytes); break;
            case FLAG_CHAT_MESSAGE: receivedChatMessage(sender, message_bytes); break;
        }
    }

    //==============================================================================================
    //Send a game state chat message, e.g. p1 killed p2
    private void sendGameStateChatMessage(String s) {

        //Create the message byte array
        byte[] message_bytes = MessageUtils.createStringMessage(FLAG_TEXT, s);
        //Send the message to all participants
        for(Participant p: mParticipants) {
            if(p.getParticipantId().equals(mMyId))
                receivedTextMessage(mMyId, message_bytes);
            else
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, p.getParticipantId());
        }
    }


    //Received a text message
    private void receivedTextMessage(String sender, byte[] buf) {

        //Received a chat message. These go in the chat history as well, but do not have a player
        //associated with them
        String message = MessageUtils.decodeStringMessage(buf);
        Pair p = new Pair("", message);
        chat_array_list.add(p);
        chat_list_adapter.notifyDataSetChanged();
        recent_message_view.setText(message);
        flashView(recent_message_view);
        makeToast(message);
    }


    //==============================================================================================

    //Message indicating a players lobby ready state
    private void receivedLobbyReadyMessage(String sender, byte[] message_bytes) {

        //Received information about the lobby state
        int ready = (int) message_bytes[1];
        boolean ready_bool = (ready == 1);

        if(sender != null)
            ready_players_map.put(sender, ready_bool);

        Participant sender_p = getParticipantById(sender);
        String sender_name = sender_p.getDisplayName();

        //Go through each player row
        for(int i = 0; i < lobby_player_list_view.getCount(); i++) {

            View view = lobby_player_list_view.getChildAt(i);
            CheckBox check_box = (CheckBox) view.findViewById(R.id.lobby_player_ready_checkbox);
            TextView text_view = (TextView) view.findViewById(R.id.lobby_player_text);
            String player_name = (String) text_view.getText();

            //Update senders checkbox
            if(player_name.equals(sender_name)) {
                check_box.setChecked(ready_bool);
                if(sender != null)
                    ready_players_map.put(sender, ready_bool);
            }
            checkLobbyState();
        }
    }


    //==============================================================================================

    //Host calls this to set a players target
    private void setPlayersTarget(Player current, Player target) {

        current.setTarget(target);
        sendTargetMessage(current, target);
    }


    //Inform a player of their target
    private void sendTargetMessage(Player player, Player target) {

        //Create the message and send it
        byte[] message_bytes = MessageUtils.createStringMessage(FLAG_TARGET, target.getId());
        if(mMyId.equals(player.getId()))
            receivedTargetMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getId());
    }


    //Received a new target
    private void receivedTargetMessage(String sender, byte[] message_bytes) {

        String target_id = MessageUtils.decodeStringMessage(message_bytes);
        target = getParticipantById(target_id);

        if(target != null) {

            //If we are waiting (are dead player), resume game
            if(been_killed_wait)
                gameResume();
            makeToast("New target: " + target.getDisplayName());
            //Request new picture of target
            sendRequestPictureMessage(target);
            kill_button.setEnabled(false);
        }
        flashView(target_distance_progress);
    }

//==================================================================================================

    //Set a players hunter
    private void setPlayersHunter(Player current, Player hunter) {

        current.setHunter(hunter);
        sendHunterMessage(current, hunter);
    }


    //Inform a player of their new hunter
    private void sendHunterMessage(Player player, Player hunter) {

        //Create and send the message
        byte[] message_bytes = MessageUtils.createStringMessage(FLAG_HUNTER, hunter.getId());
        if(mMyId.equals(player.getId()))
            receivedHunterMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getId());
    }


    //Received a new hunter
    private void receivedHunterMessage(String sender, byte[] message_bytes) {

        //Get the hunter
        String hunter_id = MessageUtils.decodeStringMessage(message_bytes);
        hunter = getParticipantById(hunter_id);

        if(hunter != null) {

            //If we are waiting (are dead player), resume the game
            if(been_killed_wait)
                gameResume();
            makeToast("Hunter is: " + hunter.getDisplayName());
        }
        flashView(hunter_distance_progress);
    }

    //==============================================================================================

    //Called when player has a new target, and needs picture of them
    private void sendRequestPictureMessage(Participant p) {

        //If we are requesting a new picture, need to delete old one -  set to null
        picture_byte_array_list = null;
        byte[] message_bytes = {FLAG_REQUEST_PICTURE};
        Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                mRoomId, p.getParticipantId());
    }


    //Another player want your picture
    private void receivedPictureRequestMessage(String sender, byte[] buf) {

        Participant participant = getParticipantById(sender);
        Log.v(TAG, "RECEIVED PICTURE REQUEST FROM " + participant.getDisplayName());
        sendPictureMessage(participant);
    }


    //Send a player your picture
    //The Multiplayer API only allows messages of 1400 bytes in size to be sent at a time. Because of
    //this, need to split up the image and send it a chunk at a time.
    private void sendPictureMessage(Participant p) {

        final int image_bytes_size = Multiplayer.MAX_RELIABLE_MESSAGE_LEN - 9;
        String image_path = preferences.getString(IMAGE_PATH_KEY, null);
        byte[] image_bytes = createImageByteArray(image_path);
        Log.v(TAG, "Total image size: " + image_bytes.length);

        //Calculate how many message will need to be sent
        int current_sub_array_count = 0;
        int total_sub_array_count = (int) Math.ceil(((double) image_bytes.length) / ((double) image_bytes_size));

        Log.v(TAG, "Total number of messages required: " + total_sub_array_count);

        //Divide message into chunks of chunks of 1400 bytes
        //Also need 9 bytes at start of message fof identifiers
        for(int i = 0; i < image_bytes.length; i += (image_bytes_size)) {

            //Create the message byte array
            byte[] message_bytes = MessageUtils.createSubImageMessage(FLAG_PICTURE,
                    image_bytes, current_sub_array_count, total_sub_array_count, i, image_bytes_size);

            //Send the message
            Log.v(TAG, "Sending picture message num: " + i);
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, p.getParticipantId());
            current_sub_array_count++;
        }
    }


    //Method to load the entire image into a byte array
    public byte[] createImageByteArray(String file_path) {

        //Cannot load entire image as it may be too large and cause an out of memory exception
        //inJustDecodeBounds = true allows just the bitmap information to be returned and used,
        //without allocating memory for the image pixels itself
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file_path, options);

        //This method allows the image to be loaded, with width and height of 300
        options.inSampleSize = DataTypeUtils.calculateInSampleSize(options, 300, 300);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(file_path, options);

        //Convert the bitmap into a byte array and return it
        ByteArrayOutputStream bitmap_stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmap_stream);
        bitmap.recycle();
        return bitmap_stream.toByteArray();
    }


    //Received a picture message from the player we requested it from
    private void receivedPictureMessage(String sender, byte[] data) {

        Log.v(TAG, "RECEIVED PICTURE MESSAGE " + data.length);
        //Only accept message if it is from current target
        if(sender != null && target != null && sender.equals(target.getParticipantId())) {
            try {
                //If the picture is null i.e. we are receiving a new picture, create a new arraylist of the specified size;
                if (picture_byte_array_list == null) {
                    int total_sub_array_count = DataTypeUtils.byteToInt(ArrayUtils.subarray(data, 5, 9));
                    //Fill arraylist with empty byte arrays
                    picture_byte_array_list = new ArrayList<>(Collections.nCopies(total_sub_array_count, new byte[]{}));
                    Log.d(TAG, "Picture arraylist size: " + picture_byte_array_list.size());
                }
                MessageUtils.receivedSubPictureMessage(data, picture_byte_array_list);

                //If our arraylist is full, the picture has been fully received
                if(DataTypeUtils.isFull(picture_byte_array_list)) {

                    //Concat the byte arrays together
                    byte[] input_image = {};
                    for (byte[] bx : picture_byte_array_list)
                        input_image = ArrayUtils.addAll(input_image, bx);
                    //Convert the byte array into a bitmap and set the photo view
                    Bitmap bitmap = BitmapFactory.decodeByteArray(input_image, 0, input_image.length);
                    target_photo_view.setImageBitmap(bitmap);
                    Log.d(TAG, "PICTURE MESSAGE FULLY RECEIVED");
                }
            }
            catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

//==================================================================================================

    //Send current location coordinates to hunter and target
    //This message is called every few seconds while the game is in progress
    private void sendCoordinatesMessage() {

        //Get current location and convert it into a message
        Location current_location = getLocation();
        LatLng current_lat_lng = DataTypeUtils.locationToLatLng(current_location);
        byte[] message_bytes = MessageUtils.createCoordinateMessage(FLAG_COORDINATES, current_lat_lng);

        if(mParticipants != null && mRoom != null && target != null && hunter != null
                && GAME_STATE == GameState.GAME_INPROGRESS && !been_killed_wait) {
            //Send message to both hunter and target
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, target.getParticipantId());
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, hunter.getParticipantId());
        }
    }


    //Received a coordinate message from either the hunter or target
    private void receivedCoordinateMessage(String sender_id, byte[] message_bytes) {

        Log.v(TAG, "RECEIVED COORINATE MESSAGE");
        LatLng lat_lng = MessageUtils.decodeCoordinateMessage(message_bytes);

        if (map_tools != null && GAME_STATE == GameState.GAME_INPROGRESS && !been_killed_wait) {

            //Received target coordinates
            if (target != null && sender_id.equals(target.getParticipantId()))
                receivedTargetCoordinateMessage(lat_lng);
            //Received hunter coordinates
            else if (hunter != null && sender_id.equals(hunter.getParticipantId()))
                receivedHunterCoordinateMessage(lat_lng);
        }
    }


    //Received our targets coordinates
    private void receivedTargetCoordinateMessage(LatLng lat_lng) {

        //Update targets coordinates
        if (target_location == null) {
            target_location = new Location("");
        }
        double lat = lat_lng.latitude;
        double lng = lat_lng.longitude;
        target_location.setLatitude(lat);
        target_location.setLongitude(lng);
        map_tools.setDestCoordinates(lat, lng);

        //Update the map
        Location location = getLocation();
        map_tools.updateMap(location);

        //Update the target distance progress bar
        int distance = (int) mLastLocation.distanceTo(target_location);
        target_distance_text.setText("TARGET DISTANCE: " + distance + " - " + target.getDisplayName());
        int progress = 0;
        if(distance >= 70) progress = 0;
        else if(distance <= 20) progress = 100;
        else progress = (70 - distance) *2;
        target_distance_progress.setProgress(progress);

        //If target is in range, enable kill button
        if(distance < KILL_MAX_DISTANCE)
            kill_button.setEnabled(true);
        else
            kill_button.setEnabled(false);
    }


    //Received our hunters coordinates
    private void receivedHunterCoordinateMessage(LatLng lat_lng) {

        //Update hunters location
        if (hunter_location == null)
            hunter_location  = new Location("");
        double lat = lat_lng.latitude;
        double lng = lat_lng.longitude;
        hunter_location.setLatitude(lat);
        hunter_location.setLongitude(lng);

        //Update hunter progress bar
        int distance = (int) mLastLocation.distanceTo(hunter_location);
        hunter_distance_text.setText("HUNTER DISTANCE: " + distance + " - " + hunter.getDisplayName());
        int progress = 0;
        if(distance >= 70) progress = 0;
        else if(distance <= 20) progress = 100;
        else progress = (70 - distance)*2;
        hunter_distance_progress.setProgress(progress);
    }

//==================================================================================================

    //Player has attempted to kill their target
    private void attemptKill() {

        makeToast("ATTEMPT KILL");
        if(target_location != null) {

            //Kill is only successful if player is in range of target
            double distance = mLastLocation.distanceTo(target_location);
            boolean kill_successful = distance <= KILL_MAX_DISTANCE;

            if (kill_successful) {
                makeToast("KILL SUCCESFUL");
                kill_button.setEnabled(false);

                byte[] message_bytes = {FLAG_KILL, (byte) (kill_successful ? 0 : 1)};

                //Send the kill message to the host
                if (isHost())
                    receivedKillMessage(mMyId, message_bytes);
                else
                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                            mRoomId, host.getParticipantId());
            }
        }
    }


    //Received a kill message
    private void receivedKillMessage(String sender, byte[] buf) {

        boolean kill_successful = (buf[1] == 0);
        Log.v(TAG, "RECEIVED KILL MESSAGE FROM: " + sender + " . KILL IS: " + kill_successful);

        //If the kill is successful and player is the host, must update the game state
        if(kill_successful && isHost()) {

            //Get the sender and their target
            Player current_player = getPlayerById(sender);
            Player killed_player = current_player.getTarget();

            //If no player is dead (waiting), set killed player to dead player (they have to wait)
            if(dead_player == null) {

                setDeadPlayer(killed_player);

                //Set the killers new target to their old taragets target
                Player current_players_new_target = killed_player.getTarget();
                setPlayersTarget(current_player, current_players_new_target);
                setPlayersHunter(current_players_new_target, current_player);
            }
            //There is a dead player, target now becomes dead player
            else {

                //Killers new target is the dead player who is waiting to come back into the game
                Player current_players_new_target = dead_player;
                setPlayersTarget(current_player, current_players_new_target);
                setPlayersHunter(current_players_new_target, current_player);

                //The dead player who has just come back into the game is now hunting the killed players target
                setPlayersTarget(current_players_new_target, killed_player.getTarget());
                setPlayersHunter(killed_player.getTarget(), current_players_new_target);
                setDeadPlayer(killed_player);
            }

            //Send new score messages, as well as a string message
            sendScoreKillMessage(current_player);
            sendScoreDeathMessage(killed_player);
            String message = current_player.getParticipant().getDisplayName()
                    + " has killed "
                    + killed_player.getParticipant().getDisplayName();
            sendGameStateChatMessage(message);
        }
    }


    //Set the dead player, and inform them
    private void setDeadPlayer(Player p) {

        dead_player = p;
        sendBeenKilledMessage(p);
    }


    //Send message to player telling them they're been killed
    private void sendBeenKilledMessage(Player player) {

        byte[] message_bytes = {FLAG_BEEN_KILLED,(byte)(1)};
        if(player.getId().equals(mMyId))
            receivedBeenKilledMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getId());
    }


    //Received a message that we have been killed
    private void receivedBeenKilledMessage(String sender, byte[] buf) {

        //Make the user aware they have been killed
        makeToast("You have been killed by " + hunter.getDisplayName() + "!!!");
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        boolean wait = (buf[1] == 1);
        if(wait)
            waitForGameResume();
    }


//==================================================================================================

    //Send a kill score message to all players
    private void sendScoreKillMessage(Player player) {

        String player_id = player.getId();
        byte[] message_bytes = MessageUtils.createStringMessage(FLAG_SCORE_KILL, player_id);

        for(Participant p: mParticipants) {
            if(p.getParticipantId().equals(mMyId))
                receivedScoreKillMessage(mMyId, message_bytes);
            else
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, p.getParticipantId());
        }
    }


    //Received a score kill message, update scoreboard
    private void receivedScoreKillMessage(String sender, byte[] message_bytes) {

        String player_id = MessageUtils.decodeStringMessage(message_bytes);
        Player player = getPlayerById(player_id);
        if(player != null)
            player.incrementKills();
        scoreboard_player_adapter.notifyDataSetChanged();
    }


    //Send a score death message to all players
    private void sendScoreDeathMessage(Player player) {

        String player_id = player.getId();
        byte[] message_bytes = MessageUtils.createStringMessage(FLAG_SCORE_DEATH, player_id);

        for(Participant p: mParticipants) {
            if(p.getParticipantId().equals(mMyId))
                receivedScoreDeathMessage(mMyId, message_bytes);
            else
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, p.getParticipantId());
        }
    }


    //Received a score death message, update scoreboard
    private void receivedScoreDeathMessage(String sender, byte[] message_bytes) {

        String player_id = MessageUtils.decodeStringMessage(message_bytes);
        Player player = getPlayerById(player_id);
        if(player != null)
            player.incrementDeaths();
        scoreboard_player_adapter.notifyDataSetChanged();
    }

//==================================================================================================

    //Have been killed, must wait for game to resume
    private void waitForGameResume() {

        //No longer have target or hunter
        Log.v("#####", "F; waitForGameResume");
        been_killed_wait = true;
        hunter = null;
        target = null;
        kill_button.setClickable(false);

        //Display killed screen
        killed_gray_overlay_layout.setVisibility(View.VISIBLE);

        target_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_disabled));
        hunter_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_disabled));
        target_distance_progress.setProgress(0);
        hunter_distance_progress.setProgress(0);
        hunter_distance_text.setText("");
        target_distance_text.setText("");
        show_photo_button.setEnabled(false);
    }


    //Another player has been killed/left, resume the game
    private void gameResume() {

        been_killed_wait = false;
        kill_button.setClickable(true);
        killed_gray_overlay_layout.setVisibility(View.GONE);
        target_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_target));
        hunter_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_hunter));
        show_photo_button.setEnabled(true);
    }

    //==============================================================================================

    //Send a chat message to all players
    private void sendChatMessage(String message) {

        byte[] message_bytes = MessageUtils.createStringMessage(FLAG_CHAT_MESSAGE, message);
        for(Participant p: mParticipants) {
            //We also send the message to ourselves in a manner of speaking
            if(p.getParticipantId().equals(mMyId))
                receivedChatMessage(mMyId, message_bytes);
            else
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, p.getParticipantId());

        }
    }


    //Received a chat message
    private void receivedChatMessage(String sender, byte[] message_bytes) {

        String message = MessageUtils.decodeStringMessage(message_bytes);
        Pair p = new Pair(getParticipantById(sender).getDisplayName(), message);
        chat_array_list.add(p);
        chat_list_adapter.notifyDataSetChanged();
        recent_message_view.setText(getParticipantById(sender).getDisplayName() + ": " + message);
        //Flash the recent message view
        flashView(recent_message_view);
    }

//===============================MAP AND LOCATION SECTION===========================================

    //Called when the map is ready to be used
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Update the map with our current location
        mMap = googleMap;
        Location current_location = getLocation();
        map_tools = new MapUtils(this, mMap, ROTATION_VECTOR_SUPPORTED);
        map_tools.updateMap(current_location);
    }


    //Called when activity is suspended
    @Override
    protected void onPause() {

        // Unregister the listener
        sensorManager.unregisterListener(this);
        super.onPause();
    }


    //Called when activity is resumed
    @Override
    protected void onResume() {
        super.onResume();

        //Register rotation listener
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);

        if(first_time_user_dialog == null || !first_time_user_dialog.isShowing())
            checkImageFileExists();
    }


    //Get our location
    private Location getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Permissions not set!!");
            return null;
        }
        else {
            //Get last location and return it
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            return mLastLocation;
        }
    }

    //==============================================================================================

    //Start the Settings Activity
    private void launchSettings() {

        Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }


    //Start the About Game Activity
    private void launchAboutGame() {

        Log.e("#####", "Launching about game");
        Intent intent = new Intent(this, AboutGameActivity.class);
        this.startActivity(intent);
    }

//=================================UI SECTION=======================================================

    //This method handles any button that is clicked
    @Override
    public void onClick(View v) {

        Intent intent;
        //Perform haptic feedback
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        switch (v.getId()) {

            // start the sign-in flow
            case R.id.button_sign_in:
                Log.d(TAG, "Sign-in button clicked");
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            //Sign user out
            case R.id.button_sign_out:
                Log.d(TAG, "Sign-out button clicked");
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                switchToScreen(R.id.screen_sign_in);
                break;
            //Show invite screen
            case R.id.button_invite_players:
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, MIN_PLAYERS, MAX_PLAYERS);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_SELECT_PLAYERS);
                break;
            //Show list of pending invitations
            case R.id.button_see_invitations:
                intent = Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                break;
            //Accept popup invitation
            case R.id.button_accept_popup_invitation:
                acceptInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            //Decline popup invitation
            case R.id.button_decline_popup_invitations:
                declineInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_quick_game:
                //Play against random opponents
                startQuickGame();
                break;
            //In game: kill button pressed
            case R.id.kill_button:
                attemptKill();
                break;
            //Launch the settings activity
            case R.id.settings:
                launchSettings();
                break;
            //Launch about game activity
            case R.id.about_game:
                launchAboutGame();
                break;
            //Exit game clicked
            case R.id.exit_game_button:
                exitGame();
                break;
            //Show/hide chat in game
            case R.id.show_chat_button:
                toggleShowChat();
                break;
        }
    }


    //Switch to a certain screen
    private void switchToScreen(int screenId) {

        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        boolean showInvPopup;
        if (mIncomingInvitationId == null)
            //No invitation, so no popup
            showInvPopup = false;
        else
            // single-player: show on main screen and gameplay screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }


    //Switch to the main screen. If not connected to Google API client, go to sign in screen
    private void switchToMainScreen() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            switchToScreen(R.id.screen_main);
        }
        else {
            switchToScreen(R.id.screen_sign_in);
        }
    }


    //Show or hide in-game chat
    private void toggleShowChat() {

        //If chat not there, show chat and automatically open keyboard
        if(game_chat_layout.getVisibility() == View.GONE) {

            game_chat_layout.setVisibility(View.VISIBLE);
            chat_game_input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(chat_game_input, InputMethodManager.SHOW_IMPLICIT);
        }
        else
            game_chat_layout.setVisibility(View.GONE);
    }


    //Flash a view to draw users attention to it
    private void flashView(View v) {

        if(v != null) {
            Animation anim = new AlphaAnimation(0.5f, 1.0f);
            anim.setDuration(200);
            anim.setStartOffset(20);
            v.startAnimation(anim);
        }
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(map_tools != null) {
            synchronized (this) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
                    map_tools.onSensorChanged(sensorEvent);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //================================MISC SECTION===================================================

    //Alert dialog to confirm that a user wants to leave the game
    private void confirmLeaveGame() {

        leave_game_alert = new AlertDialog.Builder(this)
                .setTitle("Leave Game?")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leaveGame();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("######", "Alert cancelled");
                        dialog.dismiss();
                    }
                })
                .show();
    }


    //Alert dialog to confirm user wants to leave the lobby
    private void confirmLeaveLobby() {

        leave_game_alert = new AlertDialog.Builder(this)
                .setTitle("Leave Lobby?")
                .setMessage("Are you sure you want to leave the lobby?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leaveRoom();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e("######", "Alert cancelled");
                        dialog.dismiss();
                    }
                })
                .show();
    }


    //Check if user is using game for the first time
    private boolean checkFirstTimeUser() {

        //If they are a first time user, edit preference so they no longer are
        boolean first_time_user = preferences.getBoolean(FIRST_TIME_USER_KEY, true);
        if(first_time_user) {
            preferences.edit().putBoolean(FIRST_TIME_USER_KEY, false).apply();
            makeFirstTimeUserDialog();
        }
        return first_time_user;
    }


    //Called when user is using app for first time. Indicates the user should view how to play the game
    private void makeFirstTimeUserDialog() {

        //Don't make a new alert if one is already showing
        //If it is null, need to make new one
        if(first_time_user_dialog == null || !first_time_user_dialog.isShowing()) {

            first_time_user_dialog = new AlertDialog.Builder(this)
                    .setTitle("First Time User")
                    .setMessage("You are playing this game for the first time, would you like to" +
                            "see how this app works?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            makeToast("YES SELECTED");
                            launchAboutGame();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            makeToast("NO SELECTED");
                            checkImageFileExists();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }


    //Check if the image file the user selected exists
    private void checkImageFileExists() {

        String image_path = preferences.getString(IMAGE_PATH_KEY, null);

        boolean file_exists = false;
        if(image_path != null) {
            File image_file = new File(image_path);
            if (image_file.exists()) {
                file_exists = true;
                closeImageAlertDialog();
            }
        }
        //If file does not exists, user prompted that they must choose an image to use
        if(!file_exists)
            makeImageAlertDialog();
    }


    //Alert user they need to pick a photo of themselves to be used to play the game
    private void makeImageAlertDialog() {

        //Don't make a new alert if one is already showing
        //If it is null, need to make new one
        if(image_alert_dialog == null || !image_alert_dialog.isShowing()) {

            image_alert_dialog = new AlertDialog.Builder(this)
                    .setTitle("Select Photo")
                    .setMessage("Please Select A Photo Of Your Face")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            makeToast("OK SELECTED");
                            launchSettings();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            makeToast("CANCEL SELECTED");
                            finish();
                            System.exit(0);
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }


    //Close the choose image dialog
    private void closeImageAlertDialog() {

        if(image_alert_dialog != null && image_alert_dialog.isShowing()) {
            image_alert_dialog.dismiss();
        }
    }


    //Get a participant by their id
    private Participant getParticipantById(String id) {

        for(Participant p: mParticipants) {
            if(id.equals(p.getParticipantId()))
                return p;
        }
        return null;
    }


    //Get a player by their id
    private Player getPlayerById(String id) {
        for(Player p: player_list) {
            if(p.getId().equals(id))
                return p;
        }
        return null;
    }


    //This method is used to capture keys pressed on users phone e.g. the back button
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {

            //If volume buttons are pressed, attempt to kill if in game
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN && GAME_STATE == GameState.GAME_INPROGRESS)
                    attemptKill();
                return true;
            //If back button pressed, alert user if they want to leave the game
            case KeyEvent.KEYCODE_BACK:
                if(action == KeyEvent.ACTION_DOWN) {
                    if (GAME_STATE.equals(GameState.GAME_INPROGRESS)) {

                        //Close the leave game alert dialog if it is showing
                        if (leave_game_alert != null && leave_game_alert.isShowing()) {
                            leave_game_alert.dismiss();
                            return true;
                        }
                        //Close the chat if it is visible
                        else if (game_chat_layout.getVisibility() == View.VISIBLE) {
                            game_chat_layout.setVisibility(View.GONE);
                            return true;
                        }
                        //Show the leave game dialgo
                        else {
                            confirmLeaveGame();
                            return true;
                            //leaveGame();
                        }
                    }
                    else if(GAME_STATE == GameState.IN_LOBBY)
                        confirmLeaveLobby();
                    //If in main screen, close the app
                    else if(mCurScreen ==  R.id.screen_main) {
                        finish();
                        System.exit(0);
                    }
                    else
                        switchToMainScreen();
                    return true;
                }
            default:
                return super.dispatchKeyEvent(event);
        }
    }


    //Hide the on screen keyboard
    public void hideSoftKeyboard() {

        if(this.getCurrentFocus() != null) {
            InputMethodManager inputManager =
                    (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    //Alert the user of something
    private void makeToast(String s) {

        Log.v(TAG, s);
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }


    //Keep the screen on during the game/lobby or room will disconnect
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    //Stop keeping the screen on
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    //Register the UI views and layout to be used
    private void setUpViews() {

        target_distance_text = (TextView) findViewById(R.id.target_distance_text);
        hunter_distance_text = (TextView) findViewById(R.id.hunter_distance_text);
        killed_view = (TextView) findViewById(R.id.killed_view);
        game_over_text_view = (TextView) findViewById(R.id.game_over_text_view);
        lobby_countdown_view = (TextView) findViewById(R.id.lobby_countdown);
        recent_message_view = (TextView) findViewById(R.id.recent_message_text_view);
        game_countdown_view = (TextView) findViewById(R.id.game_countdown_view);

        lobby_player_list_view = (ListView) findViewById(R.id.lobby_player_list);
        lobby_chat_list = (ListView) findViewById(R.id.lobby_chat_list);
        game_chat_list = (ListView) findViewById(R.id.game_chat_list);
        final_scoreboard_list_view = (ListView) findViewById(R.id.final_scoreboard_view);

        scoreboard_list_view = (ListView) findViewById(R.id.scoreboard_view);

        game_chat_layout = (LinearLayout) findViewById(R.id.game_chat_layout);
        game_over_layout = (LinearLayout) findViewById(R.id.game_gray_overlay_layout);
        game_over = (LinearLayout) findViewById(R.id.game_over);
        game_items_layout = (LinearLayout) findViewById(R.id.game_items_layout);
        scoreboard_layout = (LinearLayout) findViewById(R.id.scoreboard_layout);
        killed_gray_overlay_layout = (LinearLayout) findViewById(R.id.killed_gray_overlay_layout);

        target_photo_view = (ImageView) findViewById(R.id.target_image_view);

        target_distance_progress = (ProgressBar) findViewById(R.id.target_distance_progress);
        hunter_distance_progress = (ProgressBar) findViewById(R.id.hunter_distance_progress);

        kill_button = (Button) findViewById(R.id.kill_button);
        show_photo_button = (Button) findViewById(R.id.show_photo_button);
        //Register a touch listener for the show targets photo button
        show_photo_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //Only show photo if button is pressed down
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    target_photo_view.setVisibility(View.VISIBLE);
                else if (event.getAction() == MotionEvent.ACTION_UP)
                    target_photo_view.setVisibility(View.GONE);
                return false;
            }
        });
        show_scoreboard_button = (Button) findViewById(R.id.show_scoreboard_button);
        //Register a touch listener for the show scoreboard button
        show_scoreboard_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //makeToast("DOWN");
                    scoreboard_layout.setVisibility(View.VISIBLE);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // makeToast("UP");
                    scoreboard_layout.setVisibility(View.GONE);
                }
                return false;
            }
        });


        chat_lobby_input = (EditText) findViewById(R.id.chat_lobby_input);
        //Set a listener for the keyboard "send" button in the lobby chat
        chat_lobby_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    //Send the text and hide keyboard
                    String chat_input_string = chat_lobby_input.getText().toString();
                    chat_lobby_input.setText("");
                    sendChatMessage(chat_input_string);
                    hideSoftKeyboard();
                }
                return false;
            }
        });
        chat_game_input = (EditText) findViewById(R.id.chat_game_input);
        //Do the same for the in-game chat
        chat_game_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    String chat_input_string = chat_game_input.getText().toString();
                    chat_game_input.setText("");
                    sendChatMessage(chat_input_string);
                    hideSoftKeyboard();
                }
                return false;
            }
        });
    }


//==================================================================================================

    //Register the location listener
    private void registerLocationUpdates() {

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);

        location_manager = (LocationManager)this.getSystemService(LOCATION_SERVICE);
        best_provider = location_manager.getBestProvider(criteria, true);

        // Cant get a hold of provider
        if (best_provider == null) {
            Log.v(TAG, "Provider is null");
            return;
        }
        else
            Log.v(TAG, "Provider: " + best_provider);

        location_listener = new MyLocationListener();

        try {

            location_manager.requestLocationUpdates(best_provider, 1000, 0, location_listener);

            // connect to the GPS location service
            location_manager.getLastKnownLocation(best_provider);
        }
        catch(SecurityException se) {Log.e(TAG, se.toString()); }
    }

    private class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location current_location) {

            //Toast.makeText(this, "Location changed", Toast.LENGTH_LONG);
            Log.v("######", "Location changed");

            if(map_tools != null) {
                map_tools.updateMap(current_location);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {

            //Toast.makeText(this, "status changed", Toast.LENGTH_LONG).show();
        }

        public void onProviderEnabled(String provider) {

//            Toast.makeText(this, "provider enabled", Toast.LENGTH_LONG).show();
        }

        public void onProviderDisabled(String provider) {

        }
    }
}