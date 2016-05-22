package com.seankelly001.assassin;

public enum MessageType {

    FLAG_KILL('A'),
    FLAG_LOBBY_READY('B'),
    FLAG_HUNTER ('C'),
    FLAG_TARGET('D'),
    FLAG_COORDINATES('E'),
    FLAG_TEXT('G'),
    FLAG_BEEN_KILLED('H'),
    FLAG_PICTURE('J');

    public char value;

    MessageType(char value) {
        this.value = value;
    }
}

    /*
private final char FLAG_KILL = 'A';
private final char FLAG_LOBBY_READY = 'B';
private final char FLAG_HUNTER = 'C';
private final char FLAG_TARGET = 'D';
private final char FLAG_COORDINATES = 'E';
private final char FLAG_TEXT = 'G';
private final char FLAG_BEEN_KILLED = 'H';
private final char FLAG_REQUEST_PICTURE = 'I';
private final char FLAG_PICTURE = 'J';
private final char FLAG_SCORE_KILL = 'K';
private final char FLAG_SCORE_DEATH = 'L';
private final char FLAG_CHAT_MESSAGE = 'M'; */