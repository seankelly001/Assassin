/* This method is used to create the list of players in the lobby */

package com.seankelly001.assassin;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.gms.games.multiplayer.Participant;

import java.util.HashMap;
import java.util.List;


public class LobbyPlayerListAdapter extends ArrayAdapter<Participant> {

    private static HashMap<String, Boolean> ready_players_map;
    private final String mMyId;
    private final View.OnClickListener listener;

    public LobbyPlayerListAdapter(Context context, List<Participant> players, HashMap<String, Boolean> ready_players_map, String mMyId, View.OnClickListener listener) {

        super(context, R.layout.lobby_player_row_layout, players);
        this.ready_players_map = ready_players_map;
        this.mMyId = mMyId;
        this.listener = listener;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.lobby_player_row_layout, parent, false);

        Participant player = getItem(position);
        String player_name = player.getDisplayName();

        TextView player_name_text_view = (TextView) view.findViewById(R.id.lobby_player_text);
        player_name_text_view.setText(player_name);

        final String player_id = player.getParticipantId();
        final CheckBox player_ready_checkbox_view = (CheckBox) view.findViewById(R.id.lobby_player_ready_checkbox);
        player_ready_checkbox_view.setEnabled(false);

        //If row corresponds to your player, change its colour and enable the checkbox
        if(player_id.equals(mMyId)) {

            player_ready_checkbox_view.setEnabled(true);
            player_ready_checkbox_view.setOnClickListener(listener);
            player_name_text_view.setTextColor(Color.RED);
        }

        return view;
    }
}
