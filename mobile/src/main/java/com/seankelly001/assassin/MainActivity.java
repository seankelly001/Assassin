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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
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
    private ArrayList<Participant> mParticipants = null;

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
    //==============================================================================================

    private LocationManager location_manager;
    private LocationListener location_listener;
    private String best_provider;

    private final char FLAG_KILL = 'A';
    private final char FLAG_LOBBY_READY = 'B';
    private final char FLAG_HUNTER = 'C';
    private final char FLAG_TARGET = 'D';
    private final char FLAG_COORDINATES = 'E';
    private final char FLAG_GAME_STATE = 'F';
    private final char FLAG_MESSAGE = 'G';
    private final char FLAG_BEEN_KILLED = 'H';
    private final char FLAG_REQUEST_PICTURE = 'I';
    private final char FLAG_PICTURE = 'J';
    private final char FLAG_SCORE_KILL = 'K';
    private final char FLAG_SCORE_DEATH = 'L';
    private final char FLAG_CHAT_MESSAGE = 'M';

    //==============================================================================================
    private ProgressBar target_distance_progress;
    private TextView target_distance_text;

    private ProgressBar hunter_distance_progress;
    private TextView hunter_distance_text;

    private ImageView target_photo_view;
    private AlertDialog image_alert_dialog;

    private ListView final_scoreboard_list_view;

    private ListView scoreboard_list_view;

    private Button show_photo_button;
    private Button show_scoreboard_button;

    private LinearLayout scoreboard_layout;

    private LinearLayout game_over_layout;
    private LinearLayout game_over;
    private TextView killed_view;

    private TextView game_over_text_view;

    private  LinearLayout game_items_layout;



    private Button kill_button;

    private ListView lobby_player_list_view;
    private TextView lobby_countdown_view;

    private ListView lobby_chat_list;
    private EditText chat_lobby_input;

    private LinearLayout game_chat_layout;
    private ListView game_chat_list;
    private EditText chat_game_input;

    private TextView game_countdown_view;

    private TextView target_view;
    private TextView hunter_view;

    private ArrayList<Pair<String, String>> chat_array_list = new ArrayList<>();
    private ChatListAdapter chat_list_adapter;

    //==============================================================================================

    private static SharedPreferences preferences;
    private static final String PREFERENCES_NAME = "com.seankelly001.assassin";
    private static final String IMAGE_PATH_KEY = "IMAGE_PATH_KEY";

    private static GameState GAME_STATE = GameState.NOT_IN_GAME;

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


        lobby_chat_list = (ListView) findViewById(R.id.lobby_chat_list);

        chat_lobby_input = (EditText) findViewById(R.id.chat_lobby_input);
        chat_lobby_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if(actionId == EditorInfo.IME_ACTION_SEND) {

                    String chat_input_string = chat_lobby_input.getText().toString();
                    chat_lobby_input.setText("");
                    sendChatMessage(chat_input_string);
                    hideSoftKeyboard();
                }
                return false;
            }
        });

        //private LinearLayout game_chat_layout;
        //private ListView game_chat_list;
        //private EditText chat_game_input;

        game_chat_layout = (LinearLayout) findViewById(R.id.game_chat_layout);
        game_chat_list = (ListView) findViewById(R.id.game_chat_list);
        chat_game_input = (EditText) findViewById(R.id.chat_game_input);

        chat_game_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if(actionId == EditorInfo.IME_ACTION_SEND) {

                    String chat_input_string = chat_game_input.getText().toString();
                    chat_game_input.setText("");
                    sendChatMessage(chat_input_string);
                    hideSoftKeyboard();
                }
                return false;
            }
        });


        target_photo_view = (ImageView) findViewById(R.id.target_image_view);
        target_distance_progress = (ProgressBar) findViewById(R.id.target_distance_progress);
        target_distance_text = (TextView) findViewById(R.id.target_distance_text);
        hunter_distance_progress = (ProgressBar) findViewById(R.id.hunter_distance_progress);
        hunter_distance_text = (TextView) findViewById(R.id.hunter_distance_text);

        game_over_layout = (LinearLayout) findViewById(R.id.game_gray_overlay_layout);
        game_over = (LinearLayout) findViewById(R.id.game_over);
        killed_view = (TextView) findViewById(R.id.killed_view);
        game_over_text_view = (TextView) findViewById(R.id.game_over_text_view);

        game_items_layout = (LinearLayout) findViewById(R.id.game_items_layout);

        lobby_countdown_view = (TextView) findViewById(R.id.lobby_countdown);
        lobby_player_list_view = (ListView) findViewById(R.id.lobby_player_list);

        target_view = (TextView) findViewById(R.id.target_name);
        hunter_view = (TextView) findViewById(R.id.hunter_name);

        final_scoreboard_list_view = (ListView) findViewById(R.id.final_scoreboard_view);

        scoreboard_list_view = (ListView) findViewById(R.id.scoreboard_view);
        scoreboard_layout = (LinearLayout) findViewById(R.id.scoreboard_layout);

        kill_button = (Button) findViewById(R.id.kill_button);

        show_photo_button = (Button) findViewById(R.id.show_photo_button);
        //show_photo_button.getBackground();

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
        checkImageFileExists();

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
        //resetGameVars();
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
        //resetGameVars();
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
        //resetGameVars();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }


    void declineInviteToRoom(String invId) {

        if(invId != null) {
            Log.e("#####", "Declining Invitation");
            Games.RealTimeMultiplayer.declineInvitation(mGoogleApiClient, invId);
            //TODO
            //REMOVE INVITATION FROM SCREEN
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


    /*
    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            leaveRoom();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    } */



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
        switchToScreen(mCurScreen); // This will show the invitation popup
    }


    @Override
    public void onInvitationRemoved(String invitationId) {

        if (mIncomingInvitationId!=null && mIncomingInvitationId.equals(invitationId)) {
            mIncomingInvitationId = null;
            switchToScreen(mCurScreen); // This will hide the invitation popup
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
        mParticipants = room.getParticipants();
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
    public void onP2PDisconnected(String participant) {

        Log.e("#####", "F: onP2PDisconnected");
        makeToast("Player has disconnected (2)");
        endGame();
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
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        Log.e("#####", "F: onPeerLeft");
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
                String player_name = getParticipantById(peer).getDisplayName();
                makeToast(player_name + " has disconnected (1)");
                if(isHost())
                    playerLeft(peer);
            }
        }
        catch (Exception e) {Log.e("#####", e.toString());}
        updateRoom(room);
        //endGame();
    }


    void updateRoom(Room room) {

        if (room != null) {

            mParticipants = room.getParticipants();
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
                game_over_text_view.setText("GAME OVER\n There are not enough players to continue the game");
                gameOver();
            }
        }
    }


    private void checkStartLobby() {

        Log.e("#####", "F: checkStartLobby");
        int player_count = 0;
        for(Participant p: mParticipants) {
            Log.e("#####", p.getDisplayName() + " : " + p.getStatus());
            if(p.getStatus() != Participant.STATUS_JOINED) return;
        }
        Log.e("#####", "STARTING LOBBY!");
        waiting_room_finished = true;
        finishActivity(RC_WAITING_ROOM);
        startLobby();
    }


    //==========================LOBBY STUFF ============================

    private HashMap<String, Boolean> ready_players_map;
    private ArrayAdapter lobby_player_list_adapter;

    private boolean lobby_countdown_begun = false;
    private final int INITIAL_LOBBY_COUNTDOWN_SECONDS = 3;
    private int lobby_countdown_seconds = INITIAL_LOBBY_COUNTDOWN_SECONDS;
    private boolean waitForIdLobby = true;



    //Start the lobby
    void startLobby() {

        Log.e("#####", "F: startLobby");

        switchToScreen(R.id.screen_lobby);
        GAME_STATE = GameState.IN_LOBBY;

        ready_players_map = new HashMap<>();
        ready_players_map.put(mMyId, false);
        for (Participant p : mParticipants) {

            String p_id = p.getParticipantId();
            if (p_id.equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            ready_players_map.put(p_id, false);
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("#######", "TEST");

                CheckBox player_ready_checkbox_view = (CheckBox) v;
                boolean ready = player_ready_checkbox_view.isChecked();
                ready_players_map.put(mMyId, ready);

                Log.e("#######", "I am ready: " + ready);
                checkLobbyState();

                for (Participant p : mParticipants) {
                    if (p.getParticipantId().equals(mMyId))
                        continue;
                    if (p.getStatus() != Participant.STATUS_JOINED)
                        continue;

                    boolean ready_status = ready_players_map.get(mMyId);
                    mMsgBuf[0] = (byte) FLAG_LOBBY_READY;
                    mMsgBuf[1] = (byte) (ready_status ? 0 : 1);

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


        //==========================================================
        //TODO
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


    public void stopLobbyCountdown() {

        lobby_countdown_view.setVisibility(View.INVISIBLE);
        lobby_countdown_view.setText("0:0" + INITIAL_LOBBY_COUNTDOWN_SECONDS);
        lobby_countdown_seconds = INITIAL_LOBBY_COUNTDOWN_SECONDS;
        lobby_countdown_begun = false;
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

        Log.e("######", "Switching to map screen");
        switchToScreen(R.id.screen_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(mMap != null)
            mMap.clear();

        game_countdown_view = (TextView) findViewById(R.id.game_countdown_view);

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


            /*
            player_list.add(new Player(getParticipantById(mMyId), target, hunter));

            TextView target_view = (TextView) findViewById(R.id.target_name);
            target_view.setText("TARGET: " + target.getDisplayName());
            makeToast("Target is: " + target.getDisplayName());

            TextView hunter_view = (TextView) findViewById(R.id.hunter_name);
            hunter_view.setText("HUNTER: " + hunter.getDisplayName());
            makeToast("Hunter is: " + hunter.getDisplayName());
            sendPictureMessage(hunter);

            //Start at i = 1, as you don't include yourself
            for(int i = 1; i < num_participants; i++) {

                //Make a new player in the list
                Participant current = mParticipants.get(i);
                Participant current_target = mParticipants.get((i + 1) % num_participants);
                Participant current_hunter = mParticipants.get(i - 1);

                player_list.add(new Player(current, current_target, current_hunter));

                //Send target message
                sendTargetMessage(current, current_target);
                //Send hunter message
                sendHunterMessage(current, current_hunter);
            }*/
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


    private final int INITIAL_GAME_MINS = 1;
    private final int INITIAL_GAME_SECS = 00;
    private int current_game_mins = INITIAL_GAME_MINS;
    private int current_game_secs = INITIAL_GAME_SECS;


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
            gameOver();
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


    private void gameOver() {

        makeToast("GAME OVER!");
        GAME_STATE = GameState.GAME_ENDED;

        ArrayList<Player> final_player_list = new ArrayList<>(player_list);
        Collections.sort(final_player_list);
        Collections.reverse(final_player_list);

        String winner_id = final_player_list.get(0).getId();
        Log.e("#####", "WINNDER IS: " + final_player_list.get(0).getParticipant().getDisplayName());

        ScoreboardPlayerAdapter final_scoreboard_adapter = new ScoreboardPlayerAdapter(this, final_player_list, mMyId, winner_id);
        final_scoreboard_list_view.setAdapter(final_scoreboard_adapter);

        killed_view.setVisibility(View.GONE);
        game_over_layout.setVisibility(View.VISIBLE);
        game_over.setVisibility(View.VISIBLE);
        final_scoreboard_list_view.setVisibility(View.VISIBLE);

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
        game_started = false;
        lobby_countdown_begun = false;
        waitForIdLobby = true;
        ready_players_map = null;
        lobby_player_list_adapter = null;
        lobby_countdown_seconds = 3;
        current_game_mins = INITIAL_GAME_MINS;
        current_game_secs = INITIAL_GAME_SECS;
        player_list.clear();

        game_over_text_view.setText("GAME OVER!!");

        game_over_layout.setVisibility(View.GONE);
        final_scoreboard_list_view.setVisibility(View.GONE);
        lobby_countdown_view.setVisibility(View.GONE);
        killed_view.setVisibility(View.GONE);



        game_over_layout.removeView(game_chat_layout);
        game_items_layout.addView(game_chat_layout);
        game_chat_layout.setVisibility(View.GONE);

        chat_array_list.clear();
    }




    //==================================================================================================
/*
 * COMMUNICATIONS SECTION. Methods that implement the game's network
 * protocol.
 */

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {

        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.v(TAG, "Message received: " + ArrayUtils.toString(buf));

        switch (buf[0]) {

            case FLAG_LOBBY_READY: receivedLobbyReadyMessage(sender, buf); break;
            case FLAG_COORDINATES: receiveCoordinateMessage(sender, buf); break;
            case FLAG_GAME_STATE: receivedGameStateMessage(sender, buf); break;
            case FLAG_KILL: receivedKillMessage(sender, buf); break;
            case FLAG_TARGET: receivedTargetMessage(sender, buf); break;
            case FLAG_HUNTER: receivedHunterMessage(sender, buf); break;
            case FLAG_MESSAGE: receivedMessage(sender, buf); break;
            case FLAG_BEEN_KILLED: receivedBeenKilledMessage(sender, buf); break;
            case FLAG_REQUEST_PICTURE: receivedPictureRequestMessage(sender, buf); break;
            case FLAG_PICTURE: receivedPictureMessage(sender, buf); break;
            case FLAG_SCORE_KILL: receivedScoreKillMessage(sender, buf); break;
            case FLAG_SCORE_DEATH: receivedScoreDeathMessage(sender, buf); break;
            case FLAG_CHAT_MESSAGE: receivedChatMessage(sender, buf); break;
        }
    }


    //Send a text message
    private void sendMessage(String s) {

        byte[] iden_bytes = {FLAG_MESSAGE};
        byte[] s_bytes = s.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, s_bytes);
        for(Participant p: mParticipants) {
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, p.getParticipantId());
        }
    }


    //Received a text message
    private void receivedMessage(String sender, byte[] buf) {

        byte[] message_bytes = ArrayUtils.subarray(buf, 1, buf.length);
        String message = new String(message_bytes, Charset.forName("UTF-8"));
        makeToast(message);
    }


    private void receivedLobbyReadyMessage(String sender, byte[] buf) {

        //Received information about the lobby state
        int ready = (int) buf[1];
        boolean ready_bool = (ready == 0 ? true : false);

        ready_players_map.put(sender, ready_bool);

        Log.e("#########", "My Id: " + mMyId);
        Log.e("#########", "Message received from" + sender);

        Participant sender_p = null;
        for(Participant p: mParticipants) {

            Log.e("=========", p.getParticipantId());
            if(p.getParticipantId().equals(sender)) {
                sender_p = p;
            }
        }
        //Participant sender_p = mRoom.getParticipant(sender);
        String sender_name = sender_p.getDisplayName();

        for(int i = 0; i < lobby_player_list_view.getCount(); i++) {

            View view = lobby_player_list_view.getChildAt(i);
            CheckBox check_box = (CheckBox) view.findViewById(R.id.lobby_player_ready_checkbox);
            TextView text_view = (TextView) view.findViewById(R.id.lobby_player_text);
            String player_name = (String) text_view.getText();

            if(player_name.equals(sender_name)) {

                check_box.setChecked(ready_bool);
                ready_players_map.put(sender, ready_bool);
            }

            Log.e("#########", "Message Received, Player Ready States: " + ready_players_map.toString());
            checkLobbyState();
        }
    }


    //Not needed??
    private void receivedGameStateMessage(String sender_id, byte[] message_bytes) {

        Log.e("######", "RECEIVED GAME STATE MESSAGE");
        Log.e("######", "SENDER: " + sender_id);
        Log.e("######", "HOST: " + host.getParticipantId());

        byte[] game_state_bytes = ArrayUtils.subarray(message_bytes, 1, message_bytes.length);
        String game_state_string = new String(game_state_bytes, Charset.forName("UTF-8"));

        Log.e("######", "GAME STATE MESSAGE: " + game_state_string);

        String[] game_state_array = game_state_string.split("~");
        String target_id = game_state_array[0];
        String hunter_id = game_state_array[1];

        //TODO
        Log.e("#####", "TARGET: " + target_id);
        Log.e("#####", "HUNTER: " + hunter_id);
    }


    private void setPlayersTarget(Player current, Player target) {

        current.setTarget(target);
        sendTargetMessage(current.getParticipant(), target.getParticipant());
    }


    private void sendTargetMessage(Participant player, Participant target) {

        String message_string = target.getParticipantId();
        byte[] iden_bytes = {FLAG_TARGET};
        byte[] info_bytes = message_string.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, info_bytes);

        if(mMyId.equals(player.getParticipantId()))
            receivedTargetMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getParticipantId());
    }


    private void receivedTargetMessage(String sender, byte[] message_bytes) {

        Log.e("######", "RECEIVED TARGER MESSAGE");
        Log.e("######", "SENDER: " + sender);

        byte[] target_bytes =  ArrayUtils.subarray(message_bytes, 1, message_bytes.length);
        String target_id = new String(target_bytes, Charset.forName("UTF-8"));
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
        }
    }


    private void setPlayersHunter(Player current, Player hunter) {

        current.setHunter(hunter);
        sendHunterMessage(current.getParticipant(), hunter.getParticipant());
    }


    private void sendHunterMessage(Participant player, Participant hunter) {

        String message_string = hunter.getParticipantId();
        byte[] iden_bytes = {FLAG_HUNTER};
        byte[] info_bytes = message_string.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, info_bytes);

        if(mMyId.equals(player.getParticipantId()))
            receivedHunterMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getParticipantId());
    }


    private void receivedHunterMessage(String sender, byte[] message_bytes) {

        Log.e("######", "RECEIVED HUNTER MESSAGE");
        Log.e("######", "SENDER: " + sender);

        byte[] hunter_bytes =  ArrayUtils.subarray(message_bytes, 1, message_bytes.length);
        String hunter_id = new String(hunter_bytes, Charset.forName("UTF-8"));
        hunter = getParticipantById(hunter_id);

        if(hunter == null) {
            makeToast("HUNTER IS NULL!!!");
        }
        else {

            if(been_killed_wait)
                gameResume();

            hunter_view.setText("HUNTER: " + hunter.getDisplayName());
            makeToast("Hunter is: " + hunter.getDisplayName());

           // sendPictureMessage(hunter);
        }
    }


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

        String image_path = preferences.getString(IMAGE_PATH_KEY, null);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image_path, options);

        options.inSampleSize = calculateInSampleSize(options, 300, 300);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(image_path, options);
        //bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, false);

        ByteArrayOutputStream bitmap_stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmap_stream);
        bitmap.recycle();

        byte[] message_bytes = bitmap_stream.toByteArray();
        Log.e("#####", "Total image size: " + message_bytes.length);

        byte[] iden_bytes = {FLAG_PICTURE};
        int current_sub_array_count = 0;

        int total_sub_array_count = (int) Math.ceil(((double) message_bytes.length) / ((double) Multiplayer.MAX_RELIABLE_MESSAGE_LEN - 9));
        byte[] total_sub_array_count_array = ByteBuffer.allocate(4).putInt(total_sub_array_count).array();

        Log.e("#####", "Total number of messages required: " + total_sub_array_count);
        //Divide message into chunks of chunks of 1400 bytes
        //Also need 3 bytes at start of message fof identifiers
        for(int i = 0; i < message_bytes.length; i += (Multiplayer.MAX_RELIABLE_MESSAGE_LEN - 9)) {

            byte[] sub_message;

            byte[] current_sub_array_count_array = ByteBuffer.allocate(4).putInt(current_sub_array_count).array();
            byte[] iden_bytes_array = ArrayUtils.addAll(iden_bytes, ArrayUtils.addAll(current_sub_array_count_array, total_sub_array_count_array));

            if(i <= message_bytes.length) {

                byte[] sub_message_bytes = ArrayUtils.subarray(message_bytes, i, i + Multiplayer.MAX_RELIABLE_MESSAGE_LEN-9);
                sub_message = ArrayUtils.addAll(iden_bytes_array, sub_message_bytes);
            }
            else {

                byte[] sub_message_bytes = ArrayUtils.subarray(message_bytes, i, message_bytes.length);
                sub_message = ArrayUtils.addAll(iden_bytes, sub_message_bytes);
            }

            Log.v("#####", "SENDING PICTURRsE: " + sub_message.length);
            Log.v("#####", "i: " + i);
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, sub_message,
                    mRoomId, p.getParticipantId());

            current_sub_array_count++;
        }
    }


    //http://stackoverflow.com/questions/477572/strange-out-of-memory-issue-while-loading-an-image-to-a-bitmap-object
    public static int calculateInSampleSize(
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



    private ArrayList<byte[]> picture_byte_array_list;

    private void receivedPictureMessage(String sender, byte[] data) {

        Log.v("#####", "RECEIVED PICTURE MESSAGE " + data.length);

        if(sender != null && target != null && sender.equals(target.getParticipantId())) {

            try {
                //If the picture is null i.e. we are receiving a new picture, create a new arraylist of the specified size;
                byte[] current_byte_array_array = ArrayUtils.subarray(data, 1, 5);
                ByteBuffer wrapped = ByteBuffer.wrap(current_byte_array_array);
                int current_byte_array = wrapped.getInt(); //data[1] & 0xFF;

                byte[] total_byte_array_array = ArrayUtils.subarray(data, 5, 9);
                ByteBuffer wrapped2 = ByteBuffer.wrap(total_byte_array_array);
                int total_byte_array = wrapped2.getInt(); //data[2] & 0xFF;

                if (picture_byte_array_list == null) {
                    picture_byte_array_list = new ArrayList<>(total_byte_array);
                    for (int i = 0; i < total_byte_array; i++) {
                        picture_byte_array_list.add(null);
                    }
                }

                byte[] bitmap_data = ArrayUtils.subarray(data, 9, data.length);
                picture_byte_array_list.set(current_byte_array, bitmap_data);

                if (isFull()) {

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


    private boolean isFull() {

        int i = 0;
        for(byte[] bx: picture_byte_array_list) {
            if(bx == null) {

                Log.e("#####", "" + i + " is null!!!");
                return false;
            }
            i++;
        }
        return true;
    }


    private void sendCoordinatesMessage() {

        Location current_location = getLocation();
        byte[] message_bytes = createCoordinateMessage(current_location);

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


    private byte[] createCoordinateMessage(Location location) {

        if(location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

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


    private void receiveCoordinateMessage(String sender_id, byte[] message_bytes) {

        Log.v("------", "RECEIVED COORINATE MESSAGE");
        Log.v("------", "MESSAGE LENGTH:" + message_bytes.length);
        byte[] lat_bytes = ArrayUtils.subarray(message_bytes, 1, 9);
        byte[] lng_bytes = ArrayUtils.subarray(message_bytes, 9, 17);

        Log.v("------", "lat bytes len:" + lat_bytes.length);
        double lat = byteArraytoDouble(lat_bytes);
        double lng = byteArraytoDouble(lng_bytes);

        if (map_tools != null && game_started && !been_killed_wait) {

            //Received target coordinates
            if (target != null && sender_id.equals(target.getParticipantId())) {

                if (target_location == null) {
                    target_location = new Location("");
                }
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

            }
            //Received hunter coordinates
            if (hunter != null && sender_id.equals(hunter.getParticipantId())) {

                if (hunter_location == null) {
                    hunter_location  = new Location("");
                }
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
        }
    }


    //Methods to do with killing other players
    private void attemptKill() {

        makeToast("ATTEMPT KILL");

        if(target_location == null) {
            makeToast("TARGET LOCATION IS NULL");
        }
        else {
            double distance = mLastLocation.distanceTo(target_location);
            makeToast("DISTANCE TO TARGET: " + distance);

            boolean kill_succesful = distance < 5;

            if(kill_succesful) makeToast("KILL SUCCESFUL");
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

            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, target.getParticipantId());
        }
    }


    private Player dead_player = null;

    private void receivedKillMessage(String sender, byte[] buf) {

        boolean kill_successful = (buf[1] == 0? true : false);

        makeToast("RECEIVED KILL MESSAGE FROM: " + sender + " . KILL IS: " + kill_successful);

        //If the kill is successful and player is the host, must update the game state
        if(kill_successful && isHost()) {

            Player current_player = getPlayerById(sender);
            Player current_players_target = current_player.getTarget();

            if(dead_player == null) {

                setDeadPlayer(current_players_target, true);
                //dead_player = current_players_target;

                Player current_players_new_target = current_players_target.getTarget();
                setPlayersTarget(current_player, current_players_new_target);
                setPlayersHunter(current_players_new_target, current_player);
            }
            else {

                setDeadPlayer(current_players_target, false);

                Player current_players_new_target = dead_player;

                setPlayersTarget(current_player, current_players_new_target);
                setPlayersHunter(current_players_new_target, current_player);

                setPlayersTarget(current_players_new_target, current_players_target);
                setPlayersHunter(current_players_target, current_players_new_target);

                dead_player = null;
            }

            sendScoreKillMessage(current_player);
            sendScoreDeathMessage(current_players_target);
            sendMessage(current_player.getParticipant().getDisplayName() + " has killed " + current_players_target.getParticipant().getDisplayName());
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
        boolean wait = (buf[1] == 1? true:false);
        if(wait)
            waitForGameResume();
    }


    private void sendScoreKillMessage(Player player) {

        Log.e("#####", "Sending score kill message: " + player.getParticipant().getDisplayName());

        String player_id = player.getId();
        byte[] iden_bytes = {FLAG_SCORE_KILL};
        byte[] info_bytes = player_id.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, info_bytes);

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

        byte[] player_id_bytes = ArrayUtils.subarray(message_bytes, 1, message_bytes.length);
        String player_id = new String(player_id_bytes, Charset.forName("UTF-8"));
        Player player = getPlayerById(player_id);
        player.incrementKills();
        scoreboard_player_adapter.notifyDataSetChanged();

        Log.e("#####", "Received score kill message: " + player.getParticipant().getDisplayName());
    }


    private void sendScoreDeathMessage(Player player) {

        Log.e("#####", "Sending score death message: " + player.getParticipant().getDisplayName());

        String player_id = player.getId();
        byte[] iden_bytes = {FLAG_SCORE_DEATH};
        byte[] info_bytes = player_id.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, info_bytes);

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

        byte[] player_id_bytes = ArrayUtils.subarray(message_bytes, 1, message_bytes.length);
        String player_id = new String(player_id_bytes, Charset.forName("UTF-8"));
        Player player = getPlayerById(player_id);
        player.incrementDeaths();
        scoreboard_player_adapter.notifyDataSetChanged();

        Log.e("#####", "Received score death message: " + player.getParticipant().getDisplayName());

    }


    private void waitForGameResume() {

        been_killed_wait = true;
        hunter = null;
        target = null;
        kill_button.setClickable(false);
        game_over_layout.setVisibility(View.VISIBLE);
        killed_view.setVisibility(View.VISIBLE);
    }

    private void gameResume() {

        been_killed_wait = false;
        kill_button.setClickable(true);
        game_over_layout.setVisibility(View.GONE);
        killed_view.setVisibility(View.GONE);
    }


    //Only host calls this method after players leaves to resolve the new game state
    private void playerLeft(String sender_id) {

        Player sender = getPlayerById(sender_id);
        if(sender != null) {

            Player senders_target = sender.getTarget();
            Player senders_hunter = sender.getHunter();

            setPlayersTarget(senders_hunter, senders_target);
            setPlayersHunter(senders_target, senders_hunter);

            player_list.remove(sender);
        }
    }


    private void sendChatMessage(String message) {

        byte[] message_bytes = ArrayUtils.addAll(new byte[]{FLAG_CHAT_MESSAGE}, message.getBytes(Charset.forName("UTF-8")));
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


    private void receivedChatMessage(String sender, byte[] buf) {

        byte[] message_bytes = ArrayUtils.subarray(buf, 1, buf.length);
        String message = new String(message_bytes, Charset.forName("UTF-8"));

        Pair p = new Pair(getParticipantById(sender).getDisplayName(), message);
        chat_array_list.add(p);
        chat_list_adapter.notifyDataSetChanged();

        //int current_pos = lobby_chat_list.get
       // if(current_pos >= lobby_chat_list.getCount()-2)
         //   lobby_chat_list.smoothScrollToPosition(lobby_chat_list.getCount()-1);
    }


    private void toggleShowChat() {

        Log.e("#####", "F: toggleChat");
        if(game_chat_layout.getVisibility() == View.GONE)
            game_chat_layout.setVisibility(View.VISIBLE);
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

        checkImageFileExists();
    }



    private Location getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "NULLLLL", Toast.LENGTH_LONG).show();
            return null;
        }
        else {

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            return mLastLocation;
        }

    }

    //==============================================================================================

    private void launchSettings() {

        Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    //LocationListener Method
/*--------------------------------------------------------------------------------------------------
    @Override
    public void onLocationChanged(Location current_location) {

        Toast.makeText(this, "Location changed", Toast.LENGTH_LONG);

        if(map_tools != null) {
            map_tools.updateMap(current_location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        Toast.makeText(this, "status changed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {

        Toast.makeText(this, "provider enabled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {

    }
*/
//--------------------------------------------------------------------------------------------------


    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists everything that's clickable, so we can install click
    // event handlers.
    private final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_decline_popup_invitations,
            R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out, R.id.button_click_me, R.id.button_single_player,
            R.id.kill_button,
            R.id.settings,
            R.id.exit_game_button,
            R.id.show_chat_button
            // R.id.button_single_player_2
    };


    @Override
    public void onClick(View v) {
        Intent intent;

        v.performHapticFeedback(View.HAPTIC_FEEDBACK_ENABLED);


        switch (v.getId()) {

            case R.id.button_sign_in:
                // user wants to sign in
                // Check to see the developer who's running this sample code read the instructions :-)
                // NOTE: this check is here only because this is a sample! Don't include this
                // check in your actual production app.
                if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
                    Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
                }

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
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait, R.id.screen_lobby, R.id.screen_map
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
            showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
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


    public static byte[] doubleToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }


    public static double byteArraytoDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
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
                if(GAME_STATE.equals(GameState.GAME_INPROGRESS))
                    endGame();
                else
                    switchToMainScreen();
                return true;
            default:
                return super.dispatchKeyEvent(event);
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
