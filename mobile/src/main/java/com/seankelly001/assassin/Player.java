package com.seankelly001.assassin;

import android.graphics.Bitmap;

import com.google.android.gms.games.multiplayer.Participant;

/**
 * Created by Sean on 31/03/2016.
 */
public class Player {



    private final Participant participant;
    private Participant target;
    private Participant hunter;
    private Bitmap picture;

    public Player(Participant participant, Participant target, Participant hunter) {

        this.participant = participant;
        this.target = target;
        this.hunter = hunter;
    }

    public Participant getParticipant() {
        return participant;
    }

    public String getId() {
        return participant.getParticipantId();
    }

    public Participant getTarget() {
        return target;
    }

    public Participant getHunter() {
        return hunter;
    }

    public Bitmap getPicture() {return picture;}

    public void setHunter(Participant hunter) {
        this.hunter = hunter;
    }

    public void setTarget(Participant target) {
        this.target = target;
    }

    public void setPicture(Bitmap picture) { this.picture = picture;}


}
