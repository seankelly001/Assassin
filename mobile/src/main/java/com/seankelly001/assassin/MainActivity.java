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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener,
        OnMapReadyCallback, SensorEventListener
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


    private static final int MIN_PLAYERS = 2;
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
    String mRoomId = null;
    Room mRoom = null;

    // Are we playing in multiplayer mode?
    boolean mMultiplayer = false;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    String mIncomingInvitationId = null;

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
    private boolean been_killed = false;
    //==============================================================================================

    private LocationManager location_manager;
    private LocationListener location_listener;
    private String best_provider;

    private final char FLAG_KILL = 'K';
    private final char FLAG_LOBBY_READY = 'Q';
    private final char FLAG_HUNTER = 'H';
    private final char FLAG_TARGET = 'T';
    private final char FLAG_HUNTER_COORDINATES = 'X';
    private final char FLAG_TARGET_COORDINATES = 'Y';
    private final char FLAG_COORDINATES = 'C';
    private final char FLAG_GAME_STATE = 'G';
    private final char FLAG_MESSAGE = 'M';
    private final char FLAG_BEEN_KILLED = 'B';
    private final char FLAG_PICTURE = 'P';
    private final char FLAG_PICTURE_END = 'E';

    //==============================================================================================
    private ProgressBar target_distance_progress;
    private TextView target_distance_text;

    private ProgressBar hunter_distance_progress;
    private TextView hunter_distance_text;

    private ImageView target_photo_view;

    private Context context;

    private AlertDialog image_alert_dialog;
    //==============================================================================================

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor preferences_editor;
    private static final String PREFERENCES_NAME = "com.seankelly001.assassin";
    private static final String IMAGE_PATH_KEY = "IMAGE_PATH_KEY";


//==============================================================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);

        context = this;

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

        Button show_photo_button = (Button) findViewById(R.id.show_photo_button);
        show_photo_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    //makeToast("DOWN");
                    target_photo_view.setVisibility(View.VISIBLE);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    // makeToast("UP");
                    target_photo_view.setVisibility(View.GONE);
                }
                return false;
            }
        });


        target_photo_view = (ImageView) findViewById(R.id.target_image_view);
        preferences = this.getSharedPreferences(PREFERENCES_NAME, 0);


        checkImageFileExists();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ROTATION_VECTOR_SUPPORTED = sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);


        //location_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        /*
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String best_provider = mgr.getBestProvider(criteria, false);
        mgr.requestLocationUpdates(be);
        */

        registerLocationUpdates();
        Log.e("HEELLO", "test!");

        target_distance_progress = (ProgressBar) findViewById(R.id.target_distance_progress);
        target_distance_text = (TextView) findViewById(R.id.target_distance_text);

        hunter_distance_progress = (ProgressBar) findViewById(R.id.hunter_distance_progress);
        hunter_distance_text = (TextView) findViewById(R.id.hunter_distance_text);
    }


    void startQuickGame() {
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
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }


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
                        startLobby(true);
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
                } else {
                    BaseGameUtils.showActivityResultError(this,requestCode,responseCode, R.string.signin_other_error);
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
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
        resetGameVars();
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
        resetGameVars();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());


    }

    void declineInviteToRoom(String invId) {

        if(invId != null) {
            Log.e("#####", "Declining Invitation");
            Games.RealTimeMultiplayer.declineInvitation(mGoogleApiClient, invId);
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
            switchToScreen(R.id.screen_main);
        } else {
            Log.d(TAG,"Connecting client.");
            mGoogleApiClient.connect();
        }
        super.onStart();
    }


    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            leaveRoom();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }


    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        Log.e("#####", "F: leaveRoom");
        mSecondsLeft = 0;
        stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
            switchToMainScreen();
        }
    }


    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {

        Log.e("#####", "F: SHOW WAITING ROOM");
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        //final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        mRoom = room;
        startActivityForResult(i, RC_WAITING_ROOM);
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

        if (mIncomingInvitationId.equals(invitationId)&&mIncomingInvitationId!=null) {
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
        Log.d(TAG, "onConnectedToRoom.");

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
        mRoomId = null;
        showGameError();
    }


    // Show error message about game being cancelled and return to main screen.
    void showGameError() {
        BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem));
        makeToast("GAME ERROR");
        switchToMainScreen();
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
        checkStartLobby();
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        Log.e("#####", "F: onPeerInvitedToRoom");
        updateRoom(room);
        checkStartLobby();
    }

    @Override
    public void onP2PDisconnected(String participant) {

        makeToast("Player has disconnected (2)");
        endGame();
    }

    @Override
    public void onP2PConnected(String participant) {
        Log.e("#####", "F: onP2PConnected");
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {

        Log.e("#####", "F: onPeerJoined");
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
        updateRoom(room);
        checkStartLobby();
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        Log.e("#####", "F: onPeersDisconnected");
        try {
            String player_name = getParticipantById(peers.get(0)).getDisplayName();

            makeToast(player_name + " has disconnected (1)");
            // makeToast(peers.toString());
            // makeToast(player_name);
        }
        catch (Error e) {}
        updateRoom(room);
        endGame();
    }

    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();

            Iterator<Participant> iter = mParticipants.iterator();
            while(iter.hasNext()){
                Participant p = iter.next();
                if(p.getStatus() == Participant.STATUS_DECLINED) {
                    Log.e("#####", "Removing: " + p.getDisplayName());
                    iter.remove();
                }
            }

            Log.e("#####", "ROOM UPDATE - " + mParticipants.size());
        }
    }


    private void checkStartLobby() {

        Log.e("#####", "F: checkStartLobby");
        for(Participant p: mParticipants) {
            Log.e("#####", p.getDisplayName() + " : " + p.getStatus());
            if(p.getStatus() != Participant.STATUS_JOINED) return;
        }
        Log.e("#####", "STARTING LOBBY!");
        waiting_room_finished = true;
        finishActivity(RC_WAITING_ROOM);
        startLobby(true);
    }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */

    // Current state of the game:
    int mSecondsLeft = -1; // how long until the game ends (seconds)
    final static int GAME_DURATION = 20; // game duration, seconds.
    int mScore = 0; // user's current score

    // Reset game variables in preparation for a new game.
    void resetGameVars() {
        mSecondsLeft = GAME_DURATION;
        mScore = 0;
        mParticipantScore.clear();
        mFinishedParticipants.clear();
    }


    //==========================LOBBY STUFF ============================

    private final HashMap<String, Boolean> ready_players_map = new HashMap<>();
    private ArrayAdapter lobby_player_list_adapter;
    private ListView lobby_player_list_view;
    private TextView lobby_countdown_view;

    private boolean all_players_ready = false;
    private boolean lobby_countdown_begun = false;
    private int lobby_countdown_seconds = 3;


    void startLobby(boolean multiplayer) {

        switchToScreen(R.id.screen_lobby);
        ready_players_map.put(mMyId, false);

        lobby_countdown_view = (TextView) findViewById(R.id.lobby_countdown);

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
                //ready_players_list.set(position, ready);
                ready_players_map.put(mMyId, ready);

                Context context1 = v.getContext();
                checkLobbyState();

                for (Participant p : mParticipants) {
                    if (p.getParticipantId().equals(mMyId))
                        continue;
                    if (p.getStatus() != Participant.STATUS_JOINED)
                        continue;

                    boolean ready_status = ready_players_map.get(mMyId);
                    mMsgBuf[0] = (byte) 'Q';
                    mMsgBuf[1] = (byte) (ready_status ? 0 : 1);

                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, mMsgBuf,
                            mRoomId, p.getParticipantId());
                }

            }
        };

        lobby_player_list_view = (ListView) findViewById(R.id.lobby_player_list);
        lobby_player_list_adapter = new LobbyPlayerListAdapter(this, mParticipants, ready_players_map, mMyId, listener);
        lobby_player_list_view.setAdapter(lobby_player_list_adapter);

        host = mParticipants.get(0);
        String host_name = host.getDisplayName();
        Toast.makeText(this, "HOST IS: " + host_name, Toast.LENGTH_LONG).show();
    }


    private void checkLobbyState() {

        if(hashMapBool(ready_players_map) && !lobby_countdown_begun) {

            Log.e("######", "All Players Are Ready");
            Toast.makeText(this, "All Players Are Ready, Begin Countdown", Toast.LENGTH_LONG).show();
            all_players_ready = true;
            startLobbyCountdown();
        }
        else if(!hashMapBool(ready_players_map) && lobby_countdown_begun) {

            stopLobbyCountdown();
        }
    }


    public void stopLobbyCountdown() {

        lobby_countdown_view.setVisibility(View.INVISIBLE);
        lobby_countdown_view.setText("0:03");
        lobby_countdown_seconds = 3;
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
                // finish game
                Toast.makeText(this, "COUNTDOWN COMPLETE", Toast.LENGTH_SHORT).show();
                startGame(true);
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

//==================================================================================================

//==================================================================================================
/*
 * COMMUNICATIONS SECTION. Methods that implement the game's network
 * protocol.
 */

    // Score of other participants. We update this as we receive their scores
    // from the network.
    Map<String, Integer> mParticipantScore = new HashMap<String, Integer>();

    // Participants who sent us their final score.
    Set<String> mFinishedParticipants = new HashSet<String>();


    // Called when we receive a real-time message from the network.
    // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
    // indicating
    // whether it's a final or interim score. The second byte is the score.
    // There is also the
    // 'S' message, which indicates that the game should start.
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {

        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.v(TAG, "Message received: " + ArrayUtils.toString(buf));

        if(buf[0] == FLAG_LOBBY_READY) {

            receivedLobbyReadyMessage(sender, buf);
        }
        else if(buf[0] == FLAG_COORDINATES) {

            receiveCoordinateMessage(sender, buf);
        }
        else if(buf[0] == FLAG_GAME_STATE) {

            receivedGameStateMessage(sender, buf);
        }
        else if(buf[0] == FLAG_KILL) {

            receivedKillMessage(sender, buf);
        }
        else if(buf[0] == FLAG_TARGET) {

            receivedTargetMessage(sender, buf);
        }
        else if(buf[0] == FLAG_HUNTER) {

            receivedHunterMessage(sender, buf);
        }
        else if(buf[0] == FLAG_MESSAGE) {

            receivedMessage(sender, buf);
        }
        else if(buf[0] == FLAG_BEEN_KILLED) {

            receivedBeenKilledMessage(sender, buf);
        }
        else if(buf[0] == FLAG_PICTURE) {

            receivedPictureMessage(sender, buf);
        }
    }


    private void sendMessage(String s) {

        byte[] iden_bytes = {FLAG_MESSAGE};
        byte[] s_bytes = s.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, s_bytes);
        for(Participant p: mParticipants) {

            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, p.getParticipantId());
        }
    }


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

    // Broadcast my score to everybody else.
    void broadcastScore(boolean finalScore) {
        if (!mMultiplayer)
            return; // playing single-player mode

        // First byte in message indicates whether it's a final score or not
        mMsgBuf[0] = (byte) (finalScore ? 'F' : 'U');

        // Second byte is the score.
        mMsgBuf[1] = (byte) mScore;

        // Send to every other participant.
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (finalScore) {
                // final score notification must be sent via reliable message
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, mMsgBuf,
                        mRoomId, p.getParticipantId());
            } else {
                // it's an interim score notification, so we can use unreliable
                Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, mMsgBuf, mRoomId,
                        p.getParticipantId());
            }
        }
    }



    //==============================================================================================
    //GAME MAP STUFF
    private MapTools map_tools;

    private ArrayList<Player> player_list = new ArrayList<>();

    private boolean isHost() {

        return host.getParticipantId().equals(mMyId);
    }


    private void startGame(boolean multiplayer) {

        game_started = true;
        //map_tools = new MapTools(mMap, mGoogleApiClient);

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

        //If current user is the host, have to set up game
        if(isHost()) {

            Log.e("#####", "AM HOST");
            int num_participants = mParticipants.size();
            target = mParticipants.get(1);
            hunter = mParticipants.get(mParticipants.size() - 1);

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
            }
        }

        //send data periodically
        final boolean game_in_progress = true;
        final int milliseconds = 1000;

        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!game_in_progress)
                    return;
                sendCoordinatesMessage();
                h.postDelayed(this, milliseconds);
            }
        }, milliseconds);
    }


    private void endGame() {

        if (game_started) {
            makeToast("GAME ENDED");
            try {
                Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);
            } catch (Exception e) {

            }
            game_started = false;
            switchToMainScreen();
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


    private void sendTargetMessage(Participant player, Participant target) {

        String message_string = target.getParticipantId();
        byte[] iden_bytes = {FLAG_TARGET};
        byte[] info_bytes = message_string.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, info_bytes);

        if(player.getParticipantId().equals(mMyId))
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
            TextView target_view = (TextView) findViewById(R.id.target_name);
            target_view.setText("TARGET: " + target.getDisplayName());
            makeToast("Target is: " + target.getDisplayName());
        }
    }


    private void sendHunterMessage(Participant player, Participant hunter) {

        String message_string = hunter.getParticipantId();
        byte[] iden_bytes = {FLAG_HUNTER};
        byte[] info_bytes = message_string.getBytes(Charset.forName("UTF-8"));
        byte[] message_bytes = ArrayUtils.addAll(iden_bytes, info_bytes);

        if(player.getParticipantId().equals(mMyId))
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
            TextView hunter_view = (TextView) findViewById(R.id.hunter_name);
            hunter_view.setText("HUNTER: " + hunter.getDisplayName());
            makeToast("Hunter is: " + hunter.getDisplayName());

            sendPictureMessage(hunter);
        }
    }


    private void sendPictureMessage(Participant p) {

        String image_path = preferences.getString(IMAGE_PATH_KEY, null);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        //options.inSampleSize = 100;
        Bitmap bitmap = BitmapFactory.decodeFile(image_path, options);
        bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, false);
        //target_photo_view.setImageBitmap(bitmap);

        ByteArrayOutputStream bitmap_stream = new ByteArrayOutputStream();
        //byte[] iden_bytes = {FLAG_PICTURE};
        byte[] end_bytes = {FLAG_PICTURE_END};
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bitmap_stream);
        bitmap.recycle();

        byte[] message_bytes = bitmap_stream.toByteArray();
        makeToast("Total image size: " + message_bytes.length);
        //byte[] message_bytes = ArrayUtils.addAll(iden_bytes, bitmap_bytes);

        int count = 0;
        int total = (int) Math.ceil(((double) message_bytes.length) / ((double) Multiplayer.MAX_RELIABLE_MESSAGE_LEN - 3));

        //Divide message into chunks of chunks of 1400 bytes
        for(int i = 0; i < message_bytes.length; i += (Multiplayer.MAX_RELIABLE_MESSAGE_LEN - 3)) {

            byte[] sub_message;
            byte[] iden_bytes = {FLAG_PICTURE, (byte) count, (byte) total};

            if(i <= message_bytes.length) {

                byte[] sub_message_bytes = ArrayUtils.subarray(message_bytes, i, i + Multiplayer.MAX_RELIABLE_MESSAGE_LEN-3);
                sub_message = ArrayUtils.addAll(iden_bytes, sub_message_bytes);
            }
            else {

                byte[] sub_message_bytes = ArrayUtils.subarray(message_bytes, i, message_bytes.length);
                // sub_message = ArrayUtils.addAll(iden_bytes, ArrayUtils.addAll(sub_message_bytes, end_bytes));
                sub_message = ArrayUtils.addAll(iden_bytes, sub_message_bytes);
            }

            //makeToast("SENDING PICTURE: " + sub_message.length);
            Log.e("#####" ,"SENDING PICTURE: " + sub_message.length);
            Log.e("#####" ,"i: " + i);
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, sub_message,
                    mRoomId, p.getParticipantId());

            count++;
        }


    }


    private int picture_message_counter = 0;
    private ArrayList<byte[]> picture_byte_array_list;// = new ArrayList<>();

    private void receivedPictureMessage(String sender, byte[] data) {

        Log.e("#####", "RECEIVED PICTURE MESSAGE (" + data[1] + "," + data[2] + "): " + data.length);

        if(picture_byte_array_list == null) {
            picture_byte_array_list = new ArrayList<>((int) data[2]);
            for(int i = 0; i < (int)data[2]; i++) {
                picture_byte_array_list.add(null);
            }
        }

        byte[] bitmap_data = ArrayUtils.subarray(data, 3, data.length);
        picture_byte_array_list.set((int) data[1], bitmap_data);

        if(isFull()) {

            byte[] input_image = {};
            for(byte[] bx: picture_byte_array_list) {
                input_image = ArrayUtils.addAll(input_image, bx);
            }
            Log.e("######", "RECEIVED MESSAGE SIZE: " + input_image.length);
            Bitmap bitmap = BitmapFactory.decodeByteArray(input_image, 0, input_image.length);
            target_photo_view.setImageBitmap(bitmap);
            //target_photo_view.setVisibility(View.VISIBLE);
            makeToast("PICTURE MESSAGE FULLY RECEIVED");

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

        if(mParticipants != null && mRoom != null) {
            for (Participant p : mParticipants) {
                if (p.getParticipantId().equals(mMyId))
                    continue;
                if (p.getStatus() != Participant.STATUS_JOINED)
                    continue;

                try {
                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                            mRoomId, p.getParticipantId());
                }
                catch (Exception e) {
                    endGame();
                }
            }
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

        if (map_tools != null && game_started && !been_killed) {

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
                // map_tools.setDestCoordinates(lat, lng);
                //Location l = getLocation();
                //  map_tools.updateMap(l);
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


    private void receivedKillMessage(String sender, byte[] buf) {

        boolean kill_successful = (buf[1] == 0? true : false);

        makeToast("RECEIVED KILL MESSAGE FROM: " + sender + " . KILL IS: " + kill_successful);

        //If the kill is successful and player is the host, must update the game state
        if(kill_successful && isHost()) {

            Player current_player = getPlayerById(sender);

            Participant current_target = current_player.getTarget();
            Participant new_target = getPlayerById(current_target.getParticipantId()).getTarget();
            current_player.setTarget(new_target);

            sendTargetMessage(current_player.getParticipant(), current_player.getTarget());
            sendHunterMessage(current_player.getTarget(), current_player.getParticipant());

            sendMessage(getParticipantById(sender).getDisplayName() + " has killed " + current_target.getDisplayName());
            sendBeenKilledMessage(current_target);
        }
    }


    private void sendBeenKilledMessage(Participant player) {

        byte[] message_bytes = {FLAG_BEEN_KILLED};
        if(player.getParticipantId().equals(mMyId))
            receivedBeenKilledMessage(mMyId, message_bytes);
        else
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, player.getParticipantId());
    }

    private void receivedBeenKilledMessage(String sender, byte[] buf) {

        makeToast("You have been killed by " + hunter.getDisplayName() + "!!!");
        been_killed = true;
        hunter = null;
        target = null;
        Button kill_button = (Button) findViewById(R.id.kill_button);
        kill_button.setClickable(false);
        mMap.clear();
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
            R.id.settings
            // R.id.button_single_player_2
    };


    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.button_single_player:
                // case R.id.button_single_player_2:
                // play a single-player game
                resetGameVars();
                startLobby(false);
                break;
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
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 2, 7);
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
        } else if (mMultiplayer) {
            // if in multiplayer, only show invitation on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        } else {
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


    // updates the label that shows my score
    void updateScoreDisplay() {
        ((TextView) findViewById(R.id.my_score)).setText(formatScore(mScore));
    }


    // formats a score as a three-digit number
    String formatScore(int i) {
        if (i < 0)
            i = 0;
        String s = String.valueOf(i);
        return s.length() == 1 ? "00" + s : s.length() == 2 ? "0" + s : s;
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
                if(game_started)
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
                makeToast("FILE EXISTS");
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
