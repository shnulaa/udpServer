package xyz.shnulaa.udp;

import java.util.concurrent.atomic.AtomicLong;

public class Event implements Comparable<Event> {

    private String md5;
    private String uuid;
    //    private long packagePosition;
//    private int position;
    private String body;
    private long priority;
    private AtomicLong timeoutMillSec = new AtomicLong(1);

    public long addTimeout() {
        return timeoutMillSec.getAndAdd(100);
    }

    public Event(String md5, String uuid,/* long packagePosition, int position,*/ String body, long priority) {
        this.md5 = md5;
        this.uuid = uuid;
//        this.packagePosition = packagePosition;
//        this.position = position;
        this.body = body;
        this.priority = priority;
    }

    public String getMd5() {
        return md5;
    }

    public String getUuid() {
        return uuid;
    }

    /*public long getPackagePosition() {
        return packagePosition;
    }

    public int getPosition() {
        return position;
    }*/

    public String getBody() {
        return body;
    }

    public long getPriority() {
        return priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(Event o) {
        return this.priority > o.priority ? -1 : 1;
    }
}
