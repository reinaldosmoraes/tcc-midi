package com.example.nativemidi.models;

import java.util.Date;

public class Tap {

    private Hand hand;
    private int intensity;
    private Date tapTime;
    private long interval; //in milliseconds

    public Tap(Date tapTime) {
        this.tapTime = tapTime;
    }

    public Tap(Hand hand, Date tapTime) {
        this.hand = hand;
        this.tapTime = tapTime;
    }

    public Tap(Hand hand, int intensity, long interval) {
        this.hand = hand;
        this.intensity = intensity;
        this.tapTime = tapTime;
        this.interval = interval;
    }

    public Tap(Hand hand, int intensity, Date tapTime, long interval) {
        this.hand = hand;
        this.intensity = intensity;
        this.tapTime = tapTime;
        this.interval = interval;
    }

    public Tap(Hand hand, int intensity, Date tapTime) {
        this.hand = hand;
        this.intensity = intensity;
        this.tapTime = tapTime;
        this.interval = interval;
    }

    public Hand getHand() {
        return hand;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public Date getTapTime() {
        return tapTime;
    }

    public void setTapTime(Date tapTime) {
        this.tapTime = tapTime;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }
}
