package com.seankelly001.assassin;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.gms.games.multiplayer.Participant;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Sean on 27/02/2016.
 */
public class LobbyPlayerListAdapter extends ArrayAdapter<Participant> {

    private final HashMap<String, Boolean> ready_players_map;
    private final String mMyId;
    private final View.OnClickListener listener;

    public LobbyPlayerListAdapter(Context context, List<Participant> players, HashMap<String, Boolean> ready_players_map, String mMyId, View.OnClickListener listener) {

        super(context, R.layout.lobby_row_layout, players);
        this.ready_players_map = ready_players_map;
        this.mMyId = mMyId;
        this.listener = listener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.lobby_row_layout, parent, false);

        Participant player = getItem(position);
        String player_name = player.getDisplayName();

        TextView player_name_text_view = (TextView) view.findViewById(R.id.lobby_player_text);
        player_name_text_view.setText(player_name);

        final String player_id = player.getParticipantId();

        final CheckBox player_ready_checkbox_view = (CheckBox) view.findViewById(R.id.lobby_player_ready_checkbox);
        player_ready_checkbox_view.setEnabled(false);

        Log.e("#####", "Lobby getView, my id is: " + mMyId);
        Log.e("#####", "Lobby getView, playerid is: " + player_id);

        if(player_id.equals(mMyId)) {


            player_ready_checkbox_view.setEnabled(true);
            player_ready_checkbox_view.setOnClickListener(listener);
            player_name_text_view.setTextColor(Color.RED);


            /*
            player_ready_checkbox_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean ready = player_ready_checkbox_view.isChecked();
                    //ready_players_list.set(position, ready);
                    ready_players_map.put(player_id, ready);

                    Context context = v.getContext();
                    Toast.makeText(context, "INNER CLICK", Toast.LENGTH_LONG).show();

                }

            });*/
        }

        return view;
    }

}
