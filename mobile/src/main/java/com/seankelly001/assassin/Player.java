package com.seankelly001.assassin;

import android.graphics.Bitmap;

import com.google.android.gms.games.multiplayer.Participant;

/**
 * Created by Sean on 31/03/2016.
 */
public class Player implements Comparable<Player>{

    private final Participant participant;
    private Player target, hunter;
    private Bitmap picture;
    private int kills = 0, deaths = 0;
    private double kdr = 0;

    public Player(Participant participant) {

        this.participant = participant;
    }

    public void incrementKills() {kills++; calculateKdr();}

    public void incrementDeaths() {deaths++; calculateKdr();}

    public void calculateKdr() {

        if(deaths == 0)
            kdr = kills;
        else
            kdr = kills/deaths;
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

    public int getKills() {return kills;}

    public int getDeaths() {return deaths;};

    public double getKdr() {return kdr;}

    public void setHunter(Player hunter) {
        this.hunter = hunter;
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    public void setPicture(Bitmap picture) { this.picture = picture;}

    //Used to determine who wins the game
    @Override
    public int compareTo(Player px) {
        if(this.kdr > px.kdr)
            return 1;
        else if(this.kdr == px.kdr) {
            if(this.kills > px.kills)
                return 1;
            else if(this.kills == px.kills) {
                if (this.deaths < px.deaths)
                    return 1;
                else if (this.deaths == px.deaths)
                    return 0;
            }
        }
        return -1;
    }
}
