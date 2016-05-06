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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener,
        OnMapReadyCallback, SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback
{

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    final static String TAG = "ASSASSIN";

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;


    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 7;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

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

    // Message buffer for sending messages
    byte[] mMsgBuf = new byte[2];

    private boolean game_started = false;
    private boolean waiting_room_finished = false;

    //==============================================================================================
    //MAP STUFF
    private GoogleMap mMap;
    private Location mLastLocation;
    private SensorManager sensorManager = null;
    private  boolean ROTATION_VECTOR_SUPPORTED;
    //==============================================================================================

    //GAME STUFF
    private Participant host, target, hunter;
    private Location target_location, hunter_location;
    private boolean been_killed_wait = false;

    private Player dead_player = null;

    public static ArrayList<byte[]> picture_byte_array_list;
    //==============================================================================================

    private LocationManager location_manager;
    private LocationListener location_listener;
    private String best_provider;

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
    private ProgressBar target_distance_progress, hunter_distance_progress;
    private TextView target_distance_text, hunter_distance_text, killed_view, game_over_text_view,
                    lobby_countdown_view, game_countdown_view, target_view, hunter_view,
                    recent_message_view;

    private ImageView target_photo_view;
    private AlertDialog image_alert_dialog;
    private AlertDialog first_time_user_dialog;

    private ListView final_scoreboard_list_view, scoreboard_list_view, lobby_player_list_view,
                    lobby_chat_list, game_chat_list;

    private Button show_photo_button, show_scoreboard_button,  kill_button;

    private LinearLayout scoreboard_layout, game_over_layout, game_over, game_items_layout,
                        game_chat_layout, killed_gray_overlay_layout;

    private EditText chat_lobby_input, chat_game_input;
    //==============================================================================================

    private ArrayList<Pair<String, String>> chat_array_list = new ArrayList<>();
    private ChatListAdapter chat_list_adapter;

    private final int INITIAL_GAME_MINS = 5;
    private final int INITIAL_GAME_SECS = 00;
    private int current_game_mins = INITIAL_GAME_MINS;
    private int current_game_secs = INITIAL_GAME_SECS;

    private final int KILL_MAX_DISTANCE = 5;


    //Lobby stuff
    private static final HashMap<String, Boolean> ready_players_map = new HashMap<>();
    private LobbyPlayerListAdapter lobby_player_list_adapter;

    private boolean lobby_countdown_begun = false;
    private final int INITIAL_LOBBY_COUNTDOWN_SECONDS = 3;
    private int lobby_countdown_seconds = INITIAL_LOBBY_COUNTDOWN_SECONDS;
    private boolean waitForIdLobby = true;


    //==============================================================================================

    private static SharedPreferences preferences;
    private static final String PREFERENCES_NAME = "com.seankelly001.assassin";
    private static final String IMAGE_PATH_KEY = "IMAGE_PATH_KEY";
    private static final String FIRST_TIME_USER_KEY = "FIRST_TIME_USER_KEY";

    private static GameState GAME_STATE = GameState.NOT_IN_GAME;

    //==============================================================================================

    private final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_decline_popup_invitations,
            R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out,
            R.id.kill_button,
            R.id.settings,
            R.id.exit_game_button,
            R.id.show_chat_button,
            R.id.about_game
            // R.id.button_single_player_2
    };


    //==============================================================================================

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


        /*
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)) {

            Log.e("#####", "REQUESTING PERMISSIONS");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}
                    , MY_PERMISSIONS_REQUEST);
        }*/




        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);

        checkFirstTimeUser();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ROTATION_VECTOR_SUPPORTED = sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);

        registerLocationUpdates();
    }


    public void hideSoftKeyboard() {

        if(this.getCurrentFocus() != null) {
            InputMethodManager inputManager =
                    (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(
                    this.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /*
    private boolean checkPermissions() {

        Log.e("#####", "CHECKING PERMISSIONS");
        return ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED));
    }

    private void requestPermissions() {

        if(requesting_permissions_in_progress) {
            Log.e("######", "CHECKING PERMISSIONS IN PROGRESS");
            return;
        }
        requesting_permissions_in_progress = true;
        Log.e("#####", "REQUESTING PERMISSIONS");
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}
                , MY_PERMISSIONS_REQUEST);
    }*/

           /*sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);


        target_photo_view = (ImageView) findViewById(R.id.target_image_view);
        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);

        checkImageFileExists(); */



    /*
    @Override
    public void Acc onRequestPermissionsResult(int request_code, String[] permissions, int[] grant_results) {

        Log.e("#####", "PERMISSION CALLBACK");

        switch (request_code) {
            case(MY_PERMISSIONS_REQUEST): {

                boolean fine_location_permission_granted = grant_results[0] == PackageManager.PERMISSION_GRANTED;
                boolean read_storage_permission_granted = grant_results[1] == PackageManager.PERMISSION_GRANTED;

                Log.e("#####", "PERMISSIONS: (" + PackageManager.PERMISSION_GRANTED + ") : " + fine_location_permission_granted + "," + read_storage_permission_granted );

                if (fine_location_permission_granted && read_storage_permission_granted) {
                    makeToast("PERMISSIONS GRANTED");
                    continueCreate();
                }
                else {
                    //makeToast("PERMISSIONS NEED TO BE GRANTED TO PLAY GAME");

                    Log.e("#####", "MAKING DIALOG");

                }
                requesting_permissions_in_progress = false;
            }
        }
    }*/


    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        Log.e("######", "REQUEST CODE: " + requestCode);
        Log.e("######", "RESPONSE CODE: " + responseCode);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                handleSelectPlayersResult(responseCode, intent);
                break;
            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                handleInvitationInboxResult(responseCode, intent);
                break;
            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if(!waiting_room_finished) {
                    if (responseCode == Activity.RESULT_OK) {
                        // ready to start playing
                        Log.e("######", "Starting game (waiting room returned OK).");
                        checkStartLobby();
                    } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                        // player indicated that they want to leave the room
                        leaveRoom();
                    } else if (responseCode == Activity.RESULT_CANCELED) {
                        // Dialog was cancelled (user pressed back key, for instance). In our game,
                        // this means leaving the room too. In more elaborate games, this could mean
                        // something else (like minimizing the waiting room UI).
                        leaveRoom();
                    }
                }
                break;
            case RC_SIGN_IN:
                Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                        + responseCode + ", intent=" + intent);
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (responseCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                else {
                    BaseGameUtils.showActivityResultError(this,requestCode,responseCode, R.string.signin_other_error);
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }


    private void startQuickGame() {

        // quick-start a game with 1 randomly selected opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
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

        Log.d(TAG, "Select players UI succeeded.");
        Log.e("#####", "F: handleSelectPlayersResult");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Log.d(TAG, "Invitee count: " + invitees.size());


        Log.e("#####", "invitee size: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
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
        Log.d(TAG, "Room created, waiting for it to be ready...");
        Log.e("#####", "FINISHED SELECT PLAYERS");
    }


    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {

        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }
        Log.d(TAG, "Invitation inbox UI succeeded.");
        Log.e("#####", "handleInvitationInboxResult");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);
        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }


    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {

        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }


    void declineInviteToRoom(String invId) {

        if(invId != null) {
            Log.e("#####", "Declining Invitation");
            Games.RealTimeMultiplayer.declineInvitation(mGoogleApiClient, invId);
            //TODO
            //REMOVE INVITATION FROM SCREEN
            onInvitationRemoved(invId);
        }
    }


    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {

        Log.d(TAG, "**** got onStop");
        Log.e("#####", "F: onStop");

        // if we're in a room, leave it.
        leaveRoom();
        sensorManager.unregisterListener(this);

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()){
            switchToScreen(R.id.screen_sign_in);
        }
        else {
            switchToScreen(R.id.screen_wait);
        }
        super.onStop();
    }


    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        switchToScreen(R.id.screen_wait);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.w(TAG,
                    "GameHelper: client was already connected on onStart()");
            GAME_STATE = GameState.NOT_IN_GAME;
            switchToScreen(R.id.screen_main);
        }
        else {
            Log.d(TAG, "Connecting client.");
            mGoogleApiClient.connect();
        }
        super.onStart();
    }


    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {

        Log.e("#####", "F: SHOW WAITING ROOM");
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        //final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS + 1);

        // show waiting room UI
        mRoom = room;
        startActivityForResult(intent, RC_WAITING_ROOM);
    }


    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitationId = invitation.getInvitationId();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
                invitation.getInviter().getDisplayName() + " " +
                        getString(R.string.is_inviting_you));
        //switchToScreen(mCurScreen); // This will show the invitation popup
        findViewById(R.id.invitation_popup).setVisibility(mIncomingInvitationId != null ? View.VISIBLE : View.GONE);
    }


    @Override
    public void onInvitationRemoved(String invitationId) {

        if (mIncomingInvitationId!=null && mIncomingInvitationId.equals(invitationId)) {
            mIncomingInvitationId = null;
            //switchToScreen(mCurScreen); // This will hide the invitation popup
            findViewById(R.id.invitation_popup).setVisibility(View.GONE);
        }
    }


    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    @Override
    public void onConnected(Bundle connectionHint) {

        Log.e("#####", "F: onConnected");
        Log.d(TAG, "onConnected() called. Sign in successful!");
        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {
            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
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


    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

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

        switchToScreen(R.id.screen_sign_in);
    }


    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {

        Log.e("#####", "F: onConnectedToRoom.");

        //get participants and my ID:
        //mParticipants = room.getParticipants();
        mParticipants.clear();
        mParticipants.addAll(room.getParticipants());

        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
        if(mRoomId==null)
            mRoomId = room.getRoomId();

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");

        Log.e("#####", "My ID " + mMyId);

        if(waitForIdLobby) {
            waitForIdLobby = false;
            startLobby();
        }
    }


    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }


    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        showGameError();
    }


    // Show error message about game being cancelled and return to main screen.
    void showGameError() {

        //Don't do anything if game has ended (viewing scoreboard)
        if(GAME_STATE != GameState.GAME_ENDED) {
            BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem));
            makeToast("GAME ERROR");
            endGame();
            switchToMainScreen();
        }
    }


    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {

        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // save room ID so we can leave cleanly before the game starts.
        mRoomId = room.getRoomId();
        // show the waiting room UI
        showWaitingRoom(room);
    }


    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }


    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {

        Log.e("#####", "F: onPeerDeclined - " + arg1.toString());
        updateRoom(room);
    }


    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        Log.e("#####", "F: onPeerInvitedToRoom");
        updateRoom(room);
    }


    @Override
    public void onP2PDisconnected(String participant_id) {

        Log.e("#####", "F: onP2PDisconnected");
        makeToast("Player has disconnected (2)");
        Participant p = getParticipantById(participant_id);
        if(p != null)
            participantLeft(p);
    }


    @Override
    public void onP2PConnected(String participant) {
        Log.e("#####", "F: onP2PConnected");
    }


    @Override
    public void onPeerJoined(Room room, List<String> peers) {

        Log.e("#####", "F: onPeerJoined");
        for(String s: peers) {
            Log.e("#####", "Peer: " + s);
        }
        updateRoom(room);
    }


    @Override
    public void onPeerLeft(Room room, List<String> peers) {
        Log.e("#####", "F: onPeerLeft");
        try {
            for(String peer: peers) {
                Participant p = getParticipantById(peer);
                if(p != null) {
                    makeToast(p.getDisplayName() + " has left");
                    participantLeft(p);
                }
            }
        }
        catch (Exception e) {Log.e("#####", e.toString());}
        updateRoom(room);
    }


    @Override
    public void onRoomAutoMatching(Room room) {
        Log.e("#####", "F: onRoomAutoMatching");
        updateRoom(room);
    }


    @Override
    public void onRoomConnecting(Room room) {
        Log.e("#####", "F: onRoomConnecting");
        updateRoom(room);
    }


    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        Log.e("#####", "F: onPeersConnected");
        for(String s: peers) {
            Log.e("#####", "Peer: " + s);
        }
        updateRoom(room);
        checkStartLobby();
    }


    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {

        Log.e("#####", "F: onPeersDisconnected (1)");
        try {
            for(String peer: peers) {

                Participant p = getParticipantById(peer);
                if(p != null) {
                    makeToast(p.getDisplayName() + " has disconnected (1)");
                    participantLeft(p);
                }
            }
        }
        catch (Exception e) {Log.e("#####", e.toString());}
        updateRoom(room);
        //endGame();
    }


    void updateRoom(Room room) {

        if (room != null) {
           //mParticipants = room.getParticipants();
            mParticipants.clear();
            mParticipants.addAll(room.getParticipants());

            Iterator<Participant> iter = mParticipants.iterator();
            while(iter.hasNext()){
                Participant p = iter.next();
                //Participant has declined invitation, remove them from list
                if(p.getStatus() == Participant.STATUS_DECLINED
                    || p.getStatus() == Participant.STATUS_LEFT
                    || p.getStatus() == Participant.STATUS_UNRESPONSIVE) {
                    Log.e("#####", "Removing: " + p.getDisplayName() + " " + p.getStatus());
                    iter.remove();
                    player_list.remove(getPlayerById(p.getParticipantId()));
                }
            }

            Log.e("#####", "ROOM UPDATE - " + mParticipants.size());
            if(GAME_STATE == GameState.GAME_INPROGRESS && mParticipants.size() <= MIN_PLAYERS) {

                makeToast("THERE ARE NOT ENOUGH PLAYERS TO CONTINUE THE GAME");
                //game_over_text_view.setText("GAME OVER\n There are not enough players to continue the game");
                gameOver("There are not enough players to continue the game");
            }
            else if(GAME_STATE == GameState.IN_LOBBY) {

                if(mParticipants.size() <= MIN_PLAYERS) {
                    makeToast("THERE ARE NOT ENOUGH PLAYERS TO START THE GAME");
                    leaveRoom();
                }
                else {
                    lobby_player_list_adapter.notifyDataSetChanged();
                }
            }
        }
    }


    //==========================LOBBY STUFF ========================================================


    private void checkStartLobby() {

        Log.e("#####", "F: checkStartLobby");
        for(Participant p: mParticipants) {
            Log.e("#####", p.getDisplayName() + " : " + p.getStatus());
            if(p.getStatus() != Participant.STATUS_JOINED) return;
        }
        Log.e("#####", "STARTING LOBBY!");
        waiting_room_finished = true;
        finishActivity(RC_WAITING_ROOM);
        startLobby();
    }


    //Start the lobby
    void startLobby() {

        Log.e("#####", "F: startLobby");
        switchToScreen(R.id.screen_lobby);
        GAME_STATE = GameState.IN_LOBBY;

        //if(mMyId != null)
       //     ready_players_map.put(mMyId, false);

        for (Participant p : mParticipants) {

            String p_id = p.getParticipantId();
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if(p_id != null)
                ready_players_map.put(p_id, false);
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("#######", "TEST");

                CheckBox player_ready_checkbox_view = (CheckBox) v;
                boolean ready = player_ready_checkbox_view.isChecked();
                if(mMyId != null)
                    ready_players_map.put(mMyId, ready);

                Log.e("#######", "I am ready: " + ready);
                checkLobbyState();

                boolean ready_status = ready_players_map.get(mMyId);
                mMsgBuf[0] = (byte) FLAG_LOBBY_READY;
                mMsgBuf[1] = (byte) (ready_status ? 1 : 0);

                for (Participant p : mParticipants) {

                    //Don't send to self
                    if (p.getParticipantId().equals(mMyId) || p.getStatus() != Participant.STATUS_JOINED)
                        continue;

                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, mMsgBuf,
                            mRoomId, p.getParticipantId());
                }
            }
        };

        lobby_player_list_adapter = new LobbyPlayerListAdapter(this, mParticipants, ready_players_map, mMyId, listener);
        lobby_player_list_view.setAdapter(lobby_player_list_adapter);

        host = mParticipants.get(0);
        String host_name = host.getDisplayName();
        Toast.makeText(this, "HOST IS: " + host_name, Toast.LENGTH_LONG).show();

        chat_list_adapter = new ChatListAdapter(this, chat_array_list);
        lobby_chat_list.setAdapter(chat_list_adapter);
    }


    private void checkLobbyState() {

        if(hashMapBool(ready_players_map) && !lobby_countdown_begun) {

            Log.e("######", "All Players Are Ready");
            Toast.makeText(this, "All Players Are Ready, Begin Countdown", Toast.LENGTH_LONG).show();
            startLobbyCountdown();
        }
        else if(!hashMapBool(ready_players_map) && lobby_countdown_begun) {

            stopLobbyCountdown();
        }
    }


    public void startLobbyCountdown() {

        Log.e("#####", "Lobby countdown begun");
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


    public void stopLobbyCountdown() {

        Log.e("#####", "Lobby countdown cancelled");
        lobby_countdown_view.setVisibility(View.INVISIBLE);
        lobby_countdown_view.setText("0:0" + INITIAL_LOBBY_COUNTDOWN_SECONDS);
        lobby_countdown_seconds = INITIAL_LOBBY_COUNTDOWN_SECONDS;
        lobby_countdown_begun = false;
    }


    // Game tick -- update countdown, check if game ended.
    void lobbyCountdownTick() {

        if(lobby_countdown_begun) {
            if (lobby_countdown_seconds > 0)
                --lobby_countdown_seconds;

            // update countdown
            (lobby_countdown_view).setText("0:" +
                    (lobby_countdown_seconds < 10 ? "0" : "") + String.valueOf(lobby_countdown_seconds));

            if (lobby_countdown_seconds <= 0) {
                //Start the game
                Toast.makeText(this, "COUNTDOWN COMPLETE", Toast.LENGTH_SHORT).show();
                startGame();
            }
        }
    }


    private boolean hashMapBool(HashMap<String, Boolean> map) {

        Iterator<HashMap.Entry<String,Boolean>> iter = map.entrySet().iterator();
        while(iter.hasNext()) {
            HashMap.Entry<String, Boolean> entry = iter.next();
            if(entry == null)
                iter.remove();
        }

        for(String s: map.keySet()) {

            if(!map.get(s)) {
                return false;
            }
        }
        return true;
    }


    //==============================================================================================
    //GAME MAP STUFF
    private MapTools map_tools;
    private ArrayList<Player> player_list = new ArrayList<>();

    private ScoreboardPlayerAdapter scoreboard_player_adapter;

    private boolean isHost() {

        try {return host.getParticipantId().equals(mMyId);}
        catch (Exception e) { Log.e("####", e.toString()); return false;}
    }


    private void startGame() {

        game_started = true;
        GAME_STATE = GameState.GAME_INPROGRESS;
        kill_button.setEnabled(false);

        Log.e("######", "Switching to map screen");
        switchToScreen(R.id.screen_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(mMap != null)
            mMap.clear();


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);

        game_chat_list.setAdapter(chat_list_adapter);

        for(Participant p: mParticipants) {
            player_list.add(new Player(p));
        }

        //If current user is the host, have to set up game
        if(isHost()) {

            Log.e("#####", "AM HOST");

            int num_players = player_list.size();

            for(int i = 0; i < num_players; i++) {

                Player current_player = player_list.get(i);

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

        //send data periodically
        final int milliseconds = 1000;
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (GAME_STATE != GameState.GAME_INPROGRESS)
                    return;
                //sendCoordinatesMessage();
                gameTick();
                h.postDelayed(this, milliseconds);
            }
        }, milliseconds);
    }




    private void gameTick() {

        //Send coordinates every 5 seconds
        if(current_game_secs % 5 == 0)
            sendCoordinatesMessage();
        game_countdown_view.setText("" + (current_game_mins < 10? "0":"") + current_game_mins + ":"
                + (current_game_secs < 10? "0":"") + current_game_secs);

        if(current_game_mins == 0 && current_game_secs == 0) {
            //Game has ended
            //TODO
            makeToast("GAME HAS ENDED");
            GAME_STATE = GameState.GAME_ENDED;
            gameOver("Time limit reached");
            return;
        }
        else if(current_game_secs == 0) {
            current_game_mins--;
            current_game_secs = 60;
        }
        current_game_secs--;
    }


    private void endGame() {

        if (GAME_STATE.equals(GameState.GAME_INPROGRESS)) {
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

        ArrayList<Player> final_player_list = new ArrayList<>(player_list);
        Collections.sort(final_player_list);
        Collections.reverse(final_player_list);

        String winner_id = final_player_list.get(0).getId();
        Log.e("#####", "WINNDER IS: " + final_player_list.get(0).getParticipant().getDisplayName());

        ScoreboardPlayerAdapter final_scoreboard_adapter
                = new ScoreboardPlayerAdapter(this, final_player_list, mMyId, winner_id);
        final_scoreboard_list_view.setAdapter(final_scoreboard_adapter);

        killed_view.setVisibility(View.GONE);
        game_over_layout.setVisibility(View.VISIBLE);
        game_over.setVisibility(View.VISIBLE);
        final_scoreboard_list_view.setVisibility(View.VISIBLE);

        game_over_text_view.setText("Game Over - " + message);

        try {
            game_items_layout.removeView(game_chat_layout);
            game_over_layout.addView(game_chat_layout);
            game_chat_layout.setVisibility(View.VISIBLE);
        }
        catch (Exception e) {
            Log.e("#####", e.toString());
        }
    }


    private void exitGame() {

        Log.e("#####", "F: exitGame");
        leaveRoom();
       // switchToMainScreen();
    }


    // Leave the room.
    void leaveRoom() {

        Log.d(TAG, "Leaving room.");
        Log.e("#####", "F: leaveRoom");
        if (mGoogleApiClient != null && mRoomId != null) {

            Log.e("#####", "Leave room successful");
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            game_started = false;
            GAME_STATE = GameState.NOT_IN_GAME;
            switchToScreen(R.id.screen_wait);
        }
        else {
            GAME_STATE = GameState.NOT_IN_GAME;
            switchToMainScreen();
        }
        resetGameVars();
    }


    private void resetGameVars() {

        Log.e("#####", "F: exitGame");
        mRoom = null;
        mRoomId = null;
        mMyId = null;
        game_started = false;
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

        try {

            if(game_over_layout.findViewById(R.id.game_chat_layout)!= null){

                game_over_layout.removeView(game_chat_layout);
                game_items_layout.addView(game_chat_layout);
                game_chat_layout.setVisibility(View.GONE);
                recent_message_view.setText("");
            }
        }
        catch(IllegalStateException e) {
            Log.e("#####", e.toString());
        }
        chat_array_list.clear();
    }


    private void participantLeft(Participant p) {

        try {

            ready_players_map.remove(p.getParticipantId());
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
            else if (isHost()) {
                Log.e("#####", "Player has left");
                if (p == dead_player) {
                    Log.e("#####", "Player was dead");
                    makeToast("PLAYER WAS DEAD");
                    dead_player = null;
                }
                else {
                    playerLeft(p.getParticipantId());
                }
            }
        }
        catch(Exception e) {
            Log.e("#####", e.toString());
        }
    }


    //Only host calls this method after players leaves to resolve the new game state
    private void playerLeft(String sender_id) {

        Log.e("#####", "F: playerLeft");
        Player player_who_left = getPlayerById(sender_id);

        if(player_who_left != null && player_who_left != dead_player) {

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
            player_list.remove(player_who_left);
            scoreboard_player_adapter.notifyDataSetChanged();
        }
    }


//==================================================================================================
/* Messaging Section - Code which handles the creation and sending of messages, and of receiving
                       and decoding them
 */

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {

        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.v(TAG, "Message received: " + ArrayUtils.toString(buf));

        switch (buf[0]) {

            case FLAG_LOBBY_READY: receivedLobbyReadyMessage(sender, buf); break;
            case FLAG_COORDINATES: receivedCoordinateMessage(sender, buf); break;
            case FLAG_KILL: receivedKillMessage(sender, buf); break;
            case FLAG_TARGET: receivedTargetMessage(sender, buf); break;
            case FLAG_HUNTER: receivedHunterMessage(sender, buf); break;
            case FLAG_TEXT: receivedTextMessage(sender, buf); break;
            case FLAG_BEEN_KILLED: receivedBeenKilledMessage(sender, buf); break;
            case FLAG_REQUEST_PICTURE: receivedPictureRequestMessage(sender, buf); break;
            case FLAG_PICTURE: receivedPictureMessage(sender, buf); break;
            case FLAG_SCORE_KILL: receivedScoreKillMessage(sender, buf); break;
            case FLAG_SCORE_DEATH: receivedScoreDeathMessage(sender, buf); break;
            case FLAG_CHAT_MESSAGE: receivedChatMessage(sender, buf); break;
        }
    }


    //2 methods to craete generic method that just send Strings
    public byte[] createStringMessage(final char flag, String s) {

        byte[] iden_bytes = {(byte) flag};
        byte[] s_bytes = s.getBytes(Charset.forName("UTF-8"));
        byte[] concat_bytes = ArrayUtils.addAll(iden_bytes, s_bytes);
        return concat_bytes;
    }


    public String decodeStringMessage(byte[] buf) {

        byte[] message_bytes = ArrayUtils.subarray(buf, 1, buf.length);
        return new String(message_bytes, Charset.forName("UTF-8"));
    }


    //Send a text message
    private void sendTextMessage(String s) {

        byte[] message_bytes = createStringMessage(FLAG_TEXT, s);
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

        String message = decodeStringMessage(buf);
        Pair p = new Pair("", message);
        chat_array_list.add(p);
        chat_list_adapter.notifyDataSetChanged();
        recent_message_view.setText(message);
        flashView(recent_message_view);
        makeToast(message);
    }




//==================================================================================================

    private void receivedLobbyReadyMessage(String sender, byte[] buf) {

        //Received information about the lobby state
        int ready = (int) buf[1];
        boolean ready_bool = (ready == 1);

        if(sender != null)
            ready_players_map.put(sender, ready_bool);


       /* for(Participant p: mParticipants) {

            Log.e("=========", p.getParticipantId());
            if(p.getParticipantId().equals(sender)) {
                sender_p = p;
            }
        } */

        //Participant sender_p = mRoom.getParticipant(sender);

        Participant sender_p = getParticipantById(sender);
        String sender_name = sender_p.getDisplayName();

        for(int i = 0; i < lobby_player_list_view.getCount(); i++) {

            View view = lobby_player_list_view.getChildAt(i);
            CheckBox check_box = (CheckBox) view.findViewById(R.id.lobby_player_ready_checkbox);
            TextView text_view = (TextView) view.findViewById(R.id.lobby_player_text);
            String player_name = (String) text_view.getText();

            if(player_name.equals(sender_name)) {

                check_box.setChecked(ready_bool);
                if(sender != null)
                    ready_players_map.put(sender, ready_bool);
            }

            Log.e("#########", "Message Received, Player Ready States: " + ready_players_map.toString());
            checkLobbyState();
        }
    }


//==================================================================================================

    private void setPlayersTarget(Player current, Player target) {

        current.setTarget(target);
        sendTargetMessage(current, target);
    }


    private void sendTargetMessage(Player player, Player target) {

        byte[] message_bytes = createStringMessage(FLAG_TARGET, target.getId());

        if(mMyId.equals(player.getId()))
            receivedTargetMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getId());
    }


    private void receivedTargetMessage(String sender, byte[] message_bytes) {

        Log.e("######", "RECEIVED TARGET MESSAGE");
        Log.e("######", "SENDER: " + sender);

        String target_id = decodeStringMessage(message_bytes);
        target = getParticipantById(target_id);

        if(target == null) {
            makeToast("TARGET IS NULL!!!");
        }
        else {

            if(been_killed_wait)
                gameResume();

            target_view.setText("TARGET: " + target.getDisplayName());
            makeToast("Target is: " + target.getDisplayName());
            sendRequestPictureMessage(target);

            kill_button.setEnabled(true);
        }
        flashView(target_distance_progress);
    }

//==================================================================================================

    private void setPlayersHunter(Player current, Player hunter) {

        current.setHunter(hunter);
        sendHunterMessage(current, hunter);
    }


    private void sendHunterMessage(Player player, Player hunter) {

        byte[] message_bytes = createStringMessage(FLAG_HUNTER, hunter.getId());
        if(mMyId.equals(player.getId()))
            receivedHunterMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getId());
    }


    private void receivedHunterMessage(String sender, byte[] message_bytes) {

        Log.e("######", "RECEIVED HUNTER MESSAGE");
        Log.e("######", "SENDER: " + sender);
        String hunter_id = decodeStringMessage(message_bytes);
        hunter = getParticipantById(hunter_id);

        if(hunter == null) {
            makeToast("HUNTER IS NULL!!!");
        }
        else {

            if(been_killed_wait)
                gameResume();

            hunter_view.setText("HUNTER: " + hunter.getDisplayName());
            makeToast("Hunter is: " + hunter.getDisplayName());
        }
        flashView(hunter_distance_progress);
    }

//==================================================================================================

    private void sendRequestPictureMessage(Participant p) {

        //If we are requesting a new picture, need to delete old one -  set to null
        picture_byte_array_list = null;
        byte[] message_bytes = {FLAG_REQUEST_PICTURE};
        Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                mRoomId, p.getParticipantId());
    }


    private void receivedPictureRequestMessage(String sender, byte[] buf) {

        Participant p = getParticipantById(sender);
        Log.v("#####", "RECEIVED PICTURE REQUEST FROM " + p.getDisplayName());
        sendPictureMessage(p);
    }


    private void sendPictureMessage(Participant p) {

        final int image_bytes_size = Multiplayer.MAX_RELIABLE_MESSAGE_LEN - 9;
        String image_path = preferences.getString(IMAGE_PATH_KEY, null);
        byte[] message_bytes = createImageByteArray(image_path);
        Log.e("#####", "Total image size: " + message_bytes.length);

        int current_sub_array_count = 0;
        int total_sub_array_count = (int) Math.ceil(((double) message_bytes.length) / ((double) image_bytes_size));

        Log.e("#####", "Total number of messages required: " + total_sub_array_count);

        //Divide message into chunks of chunks of 1400 bytes
        //Also need 9 bytes at start of message fof identifiers
        for(int i = 0; i < message_bytes.length; i += (image_bytes_size)) {

            byte[] image_byte_message = createSubImageMessage(message_bytes, current_sub_array_count, total_sub_array_count, i, image_bytes_size);

            Log.v("#####", "SENDING PICTURRsE: " + image_byte_message.length);
            Log.v("#####", "i: " + i);
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, image_byte_message,
                    mRoomId, p.getParticipantId());

            current_sub_array_count++;
        }
    }


    public byte[] createSubImageMessage(byte[] message_bytes, int current_sub_array_count, int total_count, int start_pos, int sub_array_size) {

        //Set up the identifier bytes
        byte[] iden_bytes = {FLAG_PICTURE};
        byte[] current_sub_array_count_array = ByteBuffer.allocate(4).putInt(current_sub_array_count).array();
        byte[] total_sub_array_count_array = ByteBuffer.allocate(4).putInt(total_count).array();
        byte[] iden_bytes_array = ArrayUtils.addAll(iden_bytes, ArrayUtils.addAll(current_sub_array_count_array, total_sub_array_count_array));
        byte[] sub_message_bytes = ArrayUtils.subarray(message_bytes, start_pos, start_pos + sub_array_size);
        /*
        if((start_pos + sub_array_size) <= message_bytes.length)
            sub_message_bytes = ArrayUtils.subarray(message_bytes, start_pos, sub_array_size);
        else
            sub_message_bytes = ArrayUtils.subarray(message_bytes, start_pos, message_bytes.length);
        */
        byte[] message = ArrayUtils.addAll(iden_bytes_array, sub_message_bytes);
        Log.e("####", "sending message size: " + message.length);
        return message;
    }



    public byte[] createImageByteArray(String file_path) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file_path, options);

        options.inSampleSize = calculateInSampleSize(options, 300, 300);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(file_path, options);
        //bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, false);

        ByteArrayOutputStream bitmap_stream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmap_stream);
        bitmap.recycle();

        byte[] message_bytes = bitmap_stream.toByteArray();
        return message_bytes;
    }


    //http://stackoverflow.com/questions/477572/strange-out-of-memory-issue-while-loading-an-image-to-a-bitmap-object
    public int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    private void receivedPictureMessage(String sender, byte[] data) {

        Log.v("#####", "RECEIVED PICTURE MESSAGE " + data.length);

        if(sender != null && target != null && sender.equals(target.getParticipantId())) {

            try {

                //If the picture is null i.e. we are receiving a new picture, create a new arraylist of the specified size;
                if (picture_byte_array_list == null) {

                    int total_sub_array_count = byteToInt(ArrayUtils.subarray(data, 5, 9));
                    picture_byte_array_list = new ArrayList<>(Collections.nCopies(total_sub_array_count, new byte[]{}));
                   // Log.e("#####", "local size: " + picture_array_list.size());
                    Log.e("#####", "global size: " + picture_byte_array_list.size());
                }

                receivedSubPictureMessage(data, picture_byte_array_list);
                Log.e("####", "T1");

                if(isFull(picture_byte_array_list)) {

                    Log.e("####", "T2");
                    byte[] input_image = {};
                    for (byte[] bx : picture_byte_array_list) {
                        input_image = ArrayUtils.addAll(input_image, bx);
                    }
                    Log.e("######", "RECEIVED MESSAGE SIZE: " + input_image.length);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(input_image, 0, input_image.length);
                    target_photo_view.setImageBitmap(bitmap);
                    Log.e("#####", "PICTURE MESSAGE FULLY RECEIVED");
                }
            }
            catch (Exception e) {
                Log.e("#####", e.toString());
            }
        }
    }


    public void receivedSubPictureMessage(byte[] message, ArrayList<byte[]> picture_array_list) throws Exception{

        int current_byte_pos = byteToInt(ArrayUtils.subarray(message, 1, 5));

        Log.e("#####", "received picture message size: " + message.length);

        byte[] bitmap_data = ArrayUtils.subarray(message, 9, message.length);
        picture_array_list.set(current_byte_pos, bitmap_data);
    }


    public boolean isFull(ArrayList<byte[]> list) {

        if (list == null) {
            Log.e("#####", "List is null");
            return false;
        }
        for (byte[] bx : list) {
            if (bx == null || bx.length == 0)
                return false;
        }
        return true;
    }

//==================================================================================================

    //Send current location coordinates to hunter and target
    private void sendCoordinatesMessage() {

        Location current_location = getLocation();
        LatLng current_lat_lng = locationToLatLng(current_location);
        byte[] message_bytes = createCoordinateMessage(current_lat_lng);

        if(mParticipants != null
                && mRoom != null
                && target != null
                && hunter != null
                && GAME_STATE == GameState.GAME_INPROGRESS
                && !been_killed_wait) {

            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, target.getParticipantId());
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, hunter.getParticipantId());
        }
    }


    public byte[] createCoordinateMessage(LatLng lat_lng) {

        if(lat_lng != null) {

            double lat = lat_lng.latitude;
            double lng = lat_lng.longitude;
            byte[] iden_bytes = {FLAG_COORDINATES};
            byte[] lat_bytes = doubleToByteArray(lat);
            byte[] lng_bytes = doubleToByteArray(lng);
            byte[] concat_bytes = ArrayUtils.addAll(iden_bytes, ArrayUtils.addAll(lat_bytes, lng_bytes));
            return concat_bytes;
        }
        else {
            return null;
        }
    }


    private void receivedCoordinateMessage(String sender_id, byte[] message_bytes) {

        Log.v("------", "RECEIVED COORINATE MESSAGE");
        Log.v("------", "MESSAGE LENGTH:" + message_bytes.length);
        LatLng lat_lng = decodeCoordinateMessage(message_bytes);

        if (map_tools != null && GAME_STATE == GameState.GAME_INPROGRESS && !been_killed_wait) {

            //Received target coordinates
            if (target != null && sender_id.equals(target.getParticipantId()))
                receivedTargetCoordinateMessage(lat_lng);
            //Received hunter coordinates
            else if (hunter != null && sender_id.equals(hunter.getParticipantId()))
                receivedHunterCoordinateMessage(lat_lng);
        }
    }


    public LatLng decodeCoordinateMessage(byte[] message_bytes) {

        byte[] lat_bytes = ArrayUtils.subarray(message_bytes, 1, 9);
        byte[] lng_bytes = ArrayUtils.subarray(message_bytes, 9, 17);
        double lat = byteArraytoDouble(lat_bytes);
        double lng = byteArraytoDouble(lng_bytes);
        return new LatLng(lat, lng);
    }


    private void receivedTargetCoordinateMessage(LatLng lat_lng) {

        if (target_location == null) {
            target_location = new Location("");
        }
        double lat = lat_lng.latitude;
        double lng = lat_lng.longitude;

        target_location.setLatitude(lat);
        target_location.setLongitude(lng);
        map_tools.setDestCoordinates(lat, lng);
        Location l = getLocation();
        map_tools.updateMap(l);

        int distance = (int) mLastLocation.distanceTo(target_location);
        target_distance_text.setText("TARGET DISTANCE: " + distance + " - " + target.getDisplayName());

        int progress = 0;
        if(distance >= 70) progress = 0;
        else if(distance <= 20) progress = 100;
        else progress = (70 - distance) *2;
        target_distance_progress.setProgress(progress);

        if(distance < KILL_MAX_DISTANCE)
            kill_button.setEnabled(true);
        else
            kill_button.setEnabled(false);
    }


    private void receivedHunterCoordinateMessage(LatLng lat_lng) {

        if (hunter_location == null)
            hunter_location  = new Location("");
        double lat = lat_lng.latitude;
        double lng = lat_lng.longitude;
        hunter_location.setLatitude(lat);
        hunter_location.setLongitude(lng);
        int distance = (int) mLastLocation.distanceTo(hunter_location);
        hunter_distance_text.setText("HUNTER DISTANCE: " + distance + " - " + hunter.getDisplayName());
        int progress = 0;
        if(distance >= 70) progress = 0;
        else if(distance <= 20) progress = 100;
        else progress = (70 - distance)*2;
        hunter_distance_progress.setProgress(progress);
    }

//==================================================================================================

        //Methods to do with killing other players
    private void attemptKill() {

        makeToast("ATTEMPT KILL");

        if(target_location == null) {
            makeToast("TARGET LOCATION IS NULL");
        }
        else {
            double distance = mLastLocation.distanceTo(target_location);
            makeToast("DISTANCE TO TARGET: " + distance);

            boolean kill_succesful = distance < KILL_MAX_DISTANCE;

            if(kill_succesful) {
                makeToast("KILL SUCCESFUL");
                kill_button.setEnabled(false);
            }
            else makeToast("KILL FAILED");

            byte[] message_bytes = {FLAG_KILL, (byte)(kill_succesful? 0 : 1)};

            //If you are the host, no need to send a message, just call the messagesa
            if(isHost()) {
                receivedKillMessage(mMyId, message_bytes);
            }
            else {
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, host.getParticipantId());
            }
        }
    }


    private void receivedKillMessage(String sender, byte[] buf) {

        boolean kill_successful = (buf[1] == 0);

        makeToast("RECEIVED KILL MESSAGE FROM: " + sender + " . KILL IS: " + kill_successful);

        //If the kill is successful and player is the host, must update the game state
        if(kill_successful && isHost()) {

            Player current_player = getPlayerById(sender);
            Player current_players_old_target = current_player.getTarget();

            //If no player is dead (waiting), set killed player to dead player
            if(dead_player == null) {

                setDeadPlayer(current_players_old_target, true);

                Player current_players_new_target = current_players_old_target.getTarget();
                setPlayersTarget(current_player, current_players_new_target);
                setPlayersHunter(current_players_new_target, current_player);
            }
            //Player is dead, target now becomes dead player
            else {

                Player current_players_new_target = dead_player;
                setPlayersTarget(current_player, current_players_new_target);
                setPlayersHunter(current_players_new_target, current_player);

                setPlayersTarget(current_players_new_target, current_players_old_target.getTarget());
                setPlayersHunter(current_players_old_target.getTarget(), current_players_new_target);
                setDeadPlayer(current_players_old_target, true);
            }

            sendScoreKillMessage(current_player);
            sendScoreDeathMessage(current_players_old_target);
            sendTextMessage(current_player.getParticipant().getDisplayName() + " has killed " + current_players_old_target.getParticipant().getDisplayName());
        }
    }


    private void setDeadPlayer(Player p, boolean wait) {

        if(wait)
            dead_player = p;
        sendBeenKilledMessage(p.getParticipant(), wait);
    }


    private void sendBeenKilledMessage(Participant player, boolean wait) {

        byte[] message_bytes = {FLAG_BEEN_KILLED,(byte)(wait? 1:0)};
        if(player.getParticipantId().equals(mMyId))
            receivedBeenKilledMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getParticipantId());
    }


    private void receivedBeenKilledMessage(String sender, byte[] buf) {

        makeToast("You have been killed by " + hunter.getDisplayName() + "!!!");

        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
        boolean wait = (buf[1] == 1);

        if(wait)
            waitForGameResume();
    }


//==================================================================================================


    private void sendScoreKillMessage(Player player) {

        Log.e("#####", "Sending score kill message: " + player.getParticipant().getDisplayName());

        String player_id = player.getId();
        byte[] message_bytes = createStringMessage(FLAG_SCORE_KILL, player_id);

        for(Participant p: mParticipants) {

            //Dont send message to self
            if(p.getParticipantId().equals(mMyId))
                receivedScoreKillMessage(mMyId, message_bytes);
            else
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, p.getParticipantId());
        }
    }


    private void receivedScoreKillMessage(String sender, byte[] message_bytes) {

        String player_id = decodeStringMessage(message_bytes);
        Player player = getPlayerById(player_id);
        player.incrementKills();
        scoreboard_player_adapter.notifyDataSetChanged();

        Log.e("#####", "Received score kill message: " + player.getParticipant().getDisplayName());
    }


    private void sendScoreDeathMessage(Player player) {

        Log.e("#####", "Sending score death message: " + player.getParticipant().getDisplayName());

        String player_id = player.getId();
        byte[] message_bytes = createStringMessage(FLAG_SCORE_DEATH, player_id);

        for(Participant p: mParticipants) {

            //Dont send message to self
            if(p.getParticipantId().equals(mMyId))
                receivedScoreDeathMessage(mMyId, message_bytes);
            else
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, p.getParticipantId());
        }
    }


    private void receivedScoreDeathMessage(String sender, byte[] message_bytes) {

        String player_id = decodeStringMessage(message_bytes);
        Player player = getPlayerById(player_id);

        if(player != null) {
            player.incrementDeaths();
            scoreboard_player_adapter.notifyDataSetChanged();
        }
        else {
            Log.e("#####", "Player is null!!");
        }
        Log.e("#####", "Received score death message: " + player.getParticipant().getDisplayName());

    }

//==================================================================================================

    private void waitForGameResume() {

        Log.e("#####", "F; waitForGameResume");
        been_killed_wait = true;
        hunter = null;
        target = null;
        kill_button.setClickable(false);
        killed_gray_overlay_layout.setVisibility(View.VISIBLE);

        target_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_disabled));
        hunter_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_disabled));
        target_distance_progress.setProgress(0);
        hunter_distance_progress.setProgress(0);
        hunter_distance_text.setText("");
        target_distance_text.setText("");
        show_photo_button.setEnabled(false);
    }


    private void gameResume() {

        been_killed_wait = false;
        kill_button.setClickable(true);
        killed_gray_overlay_layout.setVisibility(View.GONE);
        target_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_target));
        hunter_distance_progress.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_hunter));
        show_photo_button.setEnabled(true);
    }

//==================================================================================================

    private void sendChatMessage(String message) {

        byte[] message_bytes = createStringMessage(FLAG_CHAT_MESSAGE, message);//ArrayUtils.addAll(new byte[]{FLAG_CHAT_MESSAGE}, message.getBytes(Charset.forName("UTF-8")));
        for(Participant p: mParticipants) {
            if(p.getParticipantId().equals(mMyId)) {
                receivedChatMessage(mMyId, message_bytes);
            }
            else {
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                        mRoomId, p.getParticipantId());
            }
        }
    }


    private void receivedChatMessage(String sender, byte[] message_bytes) {

        String message = decodeStringMessage(message_bytes);
        Pair p = new Pair(getParticipantById(sender).getDisplayName(), message);
        chat_array_list.add(p);
        chat_list_adapter.notifyDataSetChanged();
        recent_message_view.setText(getParticipantById(sender).getDisplayName() + ": " + message);
        flashView(recent_message_view);
    }


    private void flashView(View v) {

        if(v != null) {
            Animation anim = new AlphaAnimation(0.5f, 1.0f);
            anim.setDuration(200); //You can manage the time of the blink with this parameter
            anim.setStartOffset(20);
            v.startAnimation(anim);
        }
    }


    private void toggleShowChat() {

        Log.e("#####", "F: toggleChat");
        if(game_chat_layout.getVisibility() == View.GONE) {

            game_chat_layout.setVisibility(View.VISIBLE);
            chat_game_input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(chat_game_input, InputMethodManager.SHOW_IMPLICIT);
        }
        else
            game_chat_layout.setVisibility(View.GONE);
    }


//==================================================================================================

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        Log.e("######", "MAP READY");
        Location current_location = getLocation();
        map_tools = new MapTools(this, mMap, mGoogleApiClient, ROTATION_VECTOR_SUPPORTED);
        map_tools.updateMap(current_location);
    }


    @Override
    protected void onPause() {

        // Unregister the listener
        sensorManager.unregisterListener(this);
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.e("#####", "F: onREsume");

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);

        target_photo_view = (ImageView) findViewById(R.id.target_image_view);
        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);

        if(first_time_user_dialog == null || !first_time_user_dialog.isShowing())
            checkImageFileExists();
    }


    private Location getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "NULLLLL", Toast.LENGTH_LONG).show();
            return null;
        }
        else {
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

//--------------------------------------------------------------------------------------------------

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists everything that's clickable, so we can install click
    // event handlers.
    @Override
    public void onClick(View v) {
        Intent intent;

        Log.e("#####", "Haptic enabled: " + v.isHapticFeedbackEnabled());
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        switch (v.getId()) {

            case R.id.button_sign_in:
                // start the sign-in flow
                Log.d(TAG, "Sign-in button clicked");
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            case R.id.button_sign_out:
                // user wants to sign out
                // sign out.
                Log.d(TAG, "Sign-out button clicked");
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                // show list of invitable players
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, MIN_PLAYERS, MAX_PLAYERS);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_SELECT_PLAYERS);
                break;
            case R.id.button_see_invitations:
                // show list of pending invitations
                intent = Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                break;
            case R.id.button_accept_popup_invitation:
                // user wants to accept the invitation shown on the invitation popup
                // (the one we got through the OnInvitationReceivedListener).
                acceptInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_decline_popup_invitations:
                declineInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_quick_game:
                // user wants to play against a random opponent right now
                startQuickGame();
                break;
            case R.id.kill_button:
                attemptKill();
                break;
            case R.id.settings:
                launchSettings();
                break;
            case R.id.about_game:
                launchAboutGame();
                break;
            case R.id.exit_game_button:
                exitGame();
                break;
            case R.id.show_chat_button:
                toggleShowChat();
                break;
        }
    }


    // This array lists all the individual screens our game has.
    private final static int[] SCREENS = {
            R.id.screen_main, R.id.screen_sign_in, R.id.screen_wait, R.id.screen_lobby,
            R.id.screen_map
    };
    int mCurScreen = -1;


    private void switchToScreen(int screenId) {

        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;
        if (mIncomingInvitationId == null) {
            // no invitation, so no popup
            showInvPopup = false;
        }
        else {
            // single-player: show on main screen and gameplay screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        }
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }


    private void switchToMainScreen() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            switchToScreen(R.id.screen_main);
        }
        else {
            switchToScreen(R.id.screen_sign_in);
        }
    }

    //====================================================================================
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(map_tools != null) {
            synchronized (this) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

                    map_tools.onSensorChanged(sensorEvent);
                }
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    //==============================================================================================
    /*
     * MISC SECTION. Miscellaneous methods.
     */


    AlertDialog leave_game_alert;
    private void confirmLeaveGame() {

        Log.e("#####", "F: confirmLeaveGame");
        leave_game_alert = new AlertDialog.Builder(this)
                .setTitle("Leave Game?")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        endGame();
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

    AlertDialog leave_lobby_alert;
    private void confirmLeaveLobby() {

        Log.e("#####", "F: confirmLeaveLobby");
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


    private boolean checkFirstTimeUser() {

        boolean first_time_user = preferences.getBoolean(FIRST_TIME_USER_KEY, true);
        if(first_time_user) {
            Log.e("#####", "FIRST TIME USER");
            preferences.edit().putBoolean(FIRST_TIME_USER_KEY, false).commit();
            makeFirstTimeUserDialog();
        }
        return first_time_user;
    }


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

    private void checkImageFileExists() {

        String image_path = preferences.getString(IMAGE_PATH_KEY, null);

        boolean file_exisits = false;
        if(image_path != null) {
            File image_file = new File(image_path);
            if (image_file.exists()) {
                //makeToast("FILE EXISTS");
                Log.e("#####", "FILE EXISTS");
                file_exisits = true;
                closeAlertDialog();
            }
        }
        if(!file_exisits) {
            makeToast("FILE DOES NOT EXIST");
            makeImageAlertDialog();
        }
    }


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

    private void closeAlertDialog() {

        if(image_alert_dialog != null && image_alert_dialog.isShowing()) {
            image_alert_dialog.dismiss();
        }
    }



    public static byte[] doubleToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }


    public static double byteArraytoDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }



    private int byteToInt(byte[] array) {
        ByteBuffer wrapped = ByteBuffer.wrap(array);
        int num = wrapped.getInt(); //data[1] & 0xFF;
        return num;
    }


    private LatLng locationToLatLng(Location l) {
        LatLng lat_lng = new LatLng(l.getLatitude(), l.getLongitude());
        return lat_lng;
    }


    private Participant getParticipantById(String id) {

        for(Participant p: mParticipants) {
            if(id.equals(p.getParticipantId()))
                return p;
        }
        return null;
    }


    private Player getPlayerById(String id) {
        for(Player p: player_list) {
            if(p.getId().equals(id))
                return p;
        }
        return null;
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    attemptKill();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    attemptKill();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if(action == KeyEvent.ACTION_DOWN) {
                    Log.e("#####", "BACK BUTTON PRESS");
                    if (GAME_STATE.equals(GameState.GAME_INPROGRESS)) {

                        if (leave_game_alert != null && leave_game_alert.isShowing()) {
                            leave_game_alert.dismiss();
                            return true;
                        } else if (game_chat_layout.getVisibility() == View.VISIBLE) {
                            Log.e("#####", " CHAT IS VISIBLE");
                            game_chat_layout.setVisibility(View.GONE);
                            return true;
                        } else {
                            confirmLeaveGame();
                            return true;
                            //endGame();
                        }
                    }
                    else if(GAME_STATE == GameState.IN_LOBBY) {
                        confirmLeaveLobby();
                    }
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





    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.

    private void makeToast(String s) {

        Log.e("#####", s);
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }


    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    //TODO REFACTOR
    private void setUpViews() {

        target_distance_text = (TextView) findViewById(R.id.target_distance_text);
        hunter_distance_text = (TextView) findViewById(R.id.hunter_distance_text);
        killed_view = (TextView) findViewById(R.id.killed_view);
        game_over_text_view = (TextView) findViewById(R.id.game_over_text_view);
        lobby_countdown_view = (TextView) findViewById(R.id.lobby_countdown);
        target_view = (TextView) findViewById(R.id.target_name);
        hunter_view = (TextView) findViewById(R.id.hunter_name);
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
        show_photo_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //makeToast("DOWN");
                    //Log.e("#####", "DOWN");
                    target_photo_view.setVisibility(View.VISIBLE);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // makeToast("UP");
                    //Log.e("#####", "UP");
                    target_photo_view.setVisibility(View.GONE);
                }
                return false;
            }
        });
        show_scoreboard_button = (Button) findViewById(R.id.show_scoreboard_button);
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
        chat_lobby_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    String chat_input_string = chat_lobby_input.getText().toString();
                    chat_lobby_input.setText("");
                    sendChatMessage(chat_input_string);
                    hideSoftKeyboard();
                }
                return false;
            }
        });
        chat_game_input = (EditText) findViewById(R.id.chat_game_input);
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
        } else {
            Log.v(TAG, "Provider: " + best_provider);
        }

        location_listener = new MyLocationListener();

        try {
            location_manager.requestLocationUpdates(best_provider, 1000, 0, location_listener);

            // connect to the GPS location service
            Location oldLocation = location_manager.getLastKnownLocation(best_provider);

            if (oldLocation != null)  {
               /* Log.v(TAG, "Got Old location");
                latitude = Double.toString(oldLocation.getLatitude());
                longitude = Double.toString(oldLocation.getLongitude());
                waitingForLocationUpdate = false;
                getNearbyStores();*/
            } else {
                Log.v(TAG, "NO Last Location found");
            }
        }
        catch(SecurityException se) {Log.e("!!!!!!", se.toString()); }
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
