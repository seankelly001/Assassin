/* This class is used to create the rows in the chat box */

package com.seankelly001.assassin;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class ChatListAdapter extends ArrayAdapter<Pair<String, String>> {

    public ChatListAdapter(Context context, ArrayList<Pair<String, String>> chat_array_list) {
        super(context, R.layout.chat_row_layout, chat_array_list);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.chat_row_layout, parent, false);

        //Set the chat row to the corresponding information contained in the arraylist (sender and
        //text message
        Pair p = getItem(position);
        String player_name = (String) p.first;
        String chat_string = (String) p.second;
        TextView chat_player = (TextView) view.findViewById(R.id.chat_player);
        TextView chat_text = (TextView) view.findViewById(R.id.chat_text);
        chat_player.setText(player_name);
        chat_text.setText(chat_string);

        //If there is no name, message is about game e.g. someone killed someone message
        if(player_name.equals("")) {
            chat_text.setTextColor(getContext().getResources().getColor(R.color.AccentColor));
        }
        return view;
    }
}
