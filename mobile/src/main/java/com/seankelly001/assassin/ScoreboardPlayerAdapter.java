/* Class used to create in game scoreboard

 */

package com.seankelly001.assassin;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;


public class ScoreboardPlayerAdapter extends ArrayAdapter<Player> {

    private String mMyId, winnder_id;

    public ScoreboardPlayerAdapter(Context context, ArrayList<Player> players, String mMyId, String winnder_id) {

        super(context, R.layout.scoreboard_row_layout, players);
        this.mMyId = mMyId;
        this.winnder_id = winnder_id;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.scoreboard_row_layout, parent, false);

        Player p = getItem(position);

        //Get the player information and populate the scoreboard row
        String player_id = p.getId();
        String player_name = p.getParticipant().getDisplayName();
        int player_kills = p.getKills();
        int player_deaths = p.getDeaths();
        double player_kdr = p.getKdr();

        TextView player_name_view = (TextView) view.findViewById(R.id.scoreboard_name);
        TextView player_kills_view = (TextView) view.findViewById(R.id.scoreboard_kills);
        TextView player_deaths_view = (TextView) view.findViewById(R.id.scoreboard_deaths);
        TextView player_kdr_view = (TextView) view.findViewById(R.id.scoreboard_kdr);

        player_name_view.setText(player_name);
        player_kills_view.setText("" + player_kills);
        player_deaths_view.setText("" + player_deaths);
        player_kdr_view.setText("" + player_kdr);

        //Current player row is me
        if(player_id.equals(mMyId))
            player_name_view.setTextColor(Color.RED);
        //If there is a winnder (game is over), set different background to indicate this
        if(winnder_id != null && winnder_id.equals(player_id))
            view.setBackgroundResource(R.drawable.scoreboard_row_winnder_background);
        return view;
    }
}
