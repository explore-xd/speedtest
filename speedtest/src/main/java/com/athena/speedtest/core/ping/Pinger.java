package com.athena.speedtest.core.ping;

import android.util.Log;

import com.athena.speedtest.core.base.Connection;

import java.io.InputStream;

public abstract class Pinger extends Thread {
    private static final String TAG = Pinger.class.getSimpleName();
    private Connection c;
    private String path;
    private boolean stopASAP = false;

    private int total = 0;
    private int lost = 0;

    public Pinger(Connection c, String path) {
        this.c = c;
        this.path = path;
        start();
    }

    public int getTotal(){
        return total;
    }

    public int getLost(){
        return lost;
    }

    public void run() {
        try {
            String s = path;
            InputStream in = c.getInputStream();
            total = 0;
            lost = 0;
            for (; ; ) {
                if (stopASAP) break;
                total++;
                c.GET(s, true);
                if (stopASAP) break;
                long t = System.nanoTime();
                boolean chunked = false;
                boolean ok = false;
                while (true) {
                    String l = c.readLineUnbuffered();
                    if (l == null) break;
                    l = l.trim().toLowerCase();
                    if (l.equals("transfer-encoding: chunked")) chunked = true;
                    if (l.contains("200 ok")) ok = true;
                    if (l.trim().isEmpty()) {
                        if (chunked) {
                            c.readLineUnbuffered();
                            c.readLineUnbuffered();
                        }
                        break;
                    }
                }
                if (!ok) {
                    lost++;
//                    throw new Exception("Did not get a 200");
                    Log.e(TAG, "Did not get a 200");
                }
                t = System.nanoTime() - t;
                if (stopASAP) break;
                if (!onPong(t / 2)) {
                    break;
                }
            }
            c.close();
        } catch (Throwable t) {
            try {
                c.close();
            } catch (Throwable t1) {
            }
            onError(t.toString());
        }
    }

    public abstract boolean onPong(long ns);

    public abstract void onError(String err);

    public void stopASAP() {
        this.stopASAP = true;
    }
}