package com.nearme.moin.nearme;

import android.util.Log;

import java.util.TimerTask;

/**
 * Created by moin on 3/4/15.
 */
public class NotificationTimer extends TimerTask {

    private Runnable r;

    public NotificationTimer(Runnable r)
    {
        this.setR(r);
    }

    public Runnable getR() {
        return r;
    }

    public void setR(Runnable r) {
        this.r = r;
    }

    @Override
    public void run() {
        Log.e("Notification","This is the notification");
        Thread t = new Thread(r);
        t.start();
    }
}
