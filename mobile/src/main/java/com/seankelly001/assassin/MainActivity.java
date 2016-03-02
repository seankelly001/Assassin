package com.seankelly001.assassin;

        import android.Manifest;
        import android.app.Activity;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.hardware.GeomagneticField;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.location.Location;
        import android.location.LocationListener;
        import android.os.Bundle;
        import android.os.Handler;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.app.FragmentActivity;
        import android.util.Log;
        import android.view.KeyEvent;
        import android.view.View;
        import android.view.WindowManager;
        import android.widget.ArrayAdapter;
        import android.widget.CheckBox;
        import android.widget.ListView;
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
        import com.google.android.gms.maps.model.Marker;
        import com.google.android.gms.plus.Plus;

        import com.google.example.games.basegameutils.BaseGameUtils;

        import org.apache.commons.lang3.ArrayUtils;

        import java.nio.ByteBuffer;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.HashSet;
        import java.util.List;
        import java.util.Map;
        import java.util.Set;


public class MainActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener,
        OnMapReadyCallback, LocationListener, SensorEventListener
{

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    final static String TAG = "Assassin";

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


    //==============================================================================================
    //MAP STUFF
    private GoogleMap mMap;
   // private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation;

    private int zoom_level = 15;
    private GeomagneticField geoField;
    private LocationListener listener;

    private TextView magneticX;
    private TextView magneticY;
    private TextView magneticZ;
    private SensorManager sensorManager = null;

    private float mDeclination;
    private float old_heading = 0;

    private Marker arrow_marker;
    private Marker direction_marker;
    //==============================================================================================



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);

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

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);

        map_tools = new MapTools(mMap, mGoogleApiClient);

        Log.e("HEELLO", "test!");
    }


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
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 3);
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
            case R.id.button_quick_game:
                // user wants to play against a random opponent right now
                startQuickGame();
                break;
            case R.id.button_click_me:
                // (gameplay) user clicked the "click me" button
                scoreOnePoint();
                break;
            case R.id.map_test_button:
                startGame(true);
                break;
        }
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
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "Starting game (waiting room returned OK).");
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


    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

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
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
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
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
        if (mParticipants != null) {
            updatePeerScoresDisplay();
        }
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
    private int lobby_countdown_seconds = 10;


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
                Toast.makeText(context1, "OUTER CLICK", Toast.LENGTH_LONG).show();

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
        lobby_countdown_view.setText("0:10");
        lobby_countdown_seconds = 10;
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






    // Game tick -- update countdown, check if game ended.
    void gameTick() {
        if (mSecondsLeft > 0)
            --mSecondsLeft;

        // update countdown
        ((TextView) findViewById(R.id.countdown)).setText("0:" +
                (mSecondsLeft < 10 ? "0" : "") + String.valueOf(mSecondsLeft));

        if (mSecondsLeft <= 0) {
            // finish game
            findViewById(R.id.button_click_me).setVisibility(View.GONE);
            broadcastScore(true);
        }
    }

    // indicates the player scored one point
    void scoreOnePoint() {
        if (mSecondsLeft <= 0)
            return; // too late!
        ++mScore;
        updateScoreDisplay();
        updatePeerScoresDisplay();

        // broadcast our new score to our peers
        broadcastScore(false);
    }

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
        Log.e(TAG, "Message received: " + ArrayUtils.toString(buf));

        if (buf[0] == 'F' || buf[0] == 'U') {
            // score update.
            int existingScore = mParticipantScore.containsKey(sender) ?
                    mParticipantScore.get(sender) : 0;
            int thisScore = (int) buf[1];
            if (thisScore > existingScore) {
                // this check is necessary because packets may arrive out of
                // order, so we
                // should only ever consider the highest score we received, as
                // we know in our
                // game there is no way to lose points. If there was a way to
                // lose points,
                // we'd have to add a "serial number" to the packet.
                mParticipantScore.put(sender, thisScore);
            }

            // update the scores on the screen
            updatePeerScoresDisplay();

            // if it's a final score, mark this participant as having finished
            // the game
            if ((char) buf[0] == 'F') {
                mFinishedParticipants.add(rtm.getSenderParticipantId());
            }
        }
        else if(buf[0] == 'Q') {

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

          //  Toast.makeText(this, "Player " + sender + " is " + ready_bool, Toast.LENGTH_LONG).show();
        }
        else if(buf[0] == 'C') {

            receiveCoordinateMessage(sender, buf);
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

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists everything that's clickable, so we can install click
    // event handlers.
    final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out, R.id.button_click_me, R.id.button_single_player,
            R.id.map_test_button
           // R.id.button_single_player_2
    };

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait, R.id.screen_lobby, R.id.screen_map
    };
    int mCurScreen = -1;

    void switchToScreen(int screenId) {


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

    void switchToMainScreen() {
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

    // updates the screen with the scores from our peers
    void updatePeerScoresDisplay() {
        ((TextView) findViewById(R.id.score0)).setText(formatScore(mScore) + " - Me");
        int[] arr = {
                R.id.score1, R.id.score2, R.id.score3
        };
        int i = 0;

        if (mRoomId != null) {
            for (Participant p : mParticipants) {
                String pid = p.getParticipantId();
                if (pid.equals(mMyId))
                    continue;
                if (p.getStatus() != Participant.STATUS_JOINED)
                    continue;
                int score = mParticipantScore.containsKey(pid) ? mParticipantScore.get(pid) : 0;
                ((TextView) findViewById(arr[i])).setText(formatScore(score) + " - " +
                        p.getDisplayName());
                ++i;
            }
        }

        for (; i < arr.length; ++i) {
            ((TextView) findViewById(arr[i])).setText("");
        }
    }

    //==============================================================================================
    //GAME MAP STUFF
    private MapTools map_tools;

    void startGame(boolean multiplayer) {

        game_started = true;
        //map_tools = new MapTools(mMap, mGoogleApiClient);

        Log.e("######", "Switching to map screen");
        switchToScreen(R.id.screen_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);


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


    private void sendCoordinatesMessage() {

        Location current_location = getLocation();
        byte[] message_bytes = createCoordinateMessage(current_location);



        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;

            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message_bytes,
                    mRoomId, p.getParticipantId());

        }
    }


    private byte[] createCoordinateMessage(Location location) {

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        byte[] iden_bytes = {'C'};
        byte[] lat_bytes = doubleToByteArray(lat);
        byte[] lng_bytes = doubleToByteArray(lng);

        byte[] concat_bytes = ArrayUtils.addAll(iden_bytes, ArrayUtils.addAll(lat_bytes, lng_bytes));

        return concat_bytes;
    }


    private void receiveCoordinateMessage(String sender, byte[] message_bytes) {

        Log.e("------", "RECEIVED COORINATE MESSAGE");
        Log.e("------", "MESSAGE LENGTH:" + message_bytes.length);
        byte[] lat_bytes = ArrayUtils.subarray(message_bytes, 1, 9);
        byte[] lng_bytes = ArrayUtils.subarray(message_bytes, 9, 17);

        Log.e("------", "lat bytes len:" + lat_bytes.length);
        double lat = byteArraytoDouble(lat_bytes);
        double lng = byteArraytoDouble(lng_bytes);

        if(map_tools != null && game_started) {
            map_tools.setDestCoordinates(lat, lng);
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



    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        Log.e("######", "MAP READY");
        Location current_location = getLocation();
        map_tools = new MapTools(mMap, mGoogleApiClient);
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

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
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

    //LocationListener Method
//--------------------------------------------------------------------------------------------------
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

//--------------------------------------------------------------------------------------------------


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


    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
