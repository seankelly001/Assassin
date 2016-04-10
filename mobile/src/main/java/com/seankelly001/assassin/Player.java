package com.seankelly001.assassin;

import android.graphics.Bitmap;

import com.google.android.gms.games.multiplayer.Participant;

/**
 * Created by Sean on 31/03/2016.
 */
public class Player {



    private final Participant participant;
    private Player target;
    private Player hunter;
    private Bitmap picture;

    public Player(Participant participant) {

        this.participant = participant;
    }

    public Participant getParticipant() {
        return participant;
    }

    public String getId() {
        return participant.getParticipantId();
    }

    public Player getTarget() {
        return target;
    }

    public Player getHunter() {
        return hunter;
    }

    public Bitmap getPicture() {return picture;}

    public void setHunter(Player hunter) {
        this.hunter = hunter;
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    public void setPicture(Bitmap picture) { this.picture = picture;}


}
