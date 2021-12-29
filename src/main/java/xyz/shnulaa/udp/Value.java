package xyz.shnulaa.udp;

import com.sun.org.apache.bcel.internal.Const;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Value {
    private long packagePosition;
    // uuid -> position
    private Map<String, Integer> position;
    private int totalLength;
    private AtomicInteger readed;
    private int expectNum;

    public Value(long packagePosition, Map<String, Integer> position, int totalLength, int expectNum) {
        this.packagePosition = packagePosition;
        this.position = position;
        this.totalLength = totalLength;
        this.expectNum = expectNum;
        this.readed = new AtomicInteger(1);
    }

    public long getPackagePosition() {
        return packagePosition;
    }

    public Map<String, Integer> getPosition() {
        return position;
    }

    public int getTotalLength() {
        return totalLength;
    }

    public AtomicInteger getReaded() {
        return readed;
    }

    public boolean isAlready() {
        return readed.incrementAndGet() == expectNum -1;
    }

    public int getExpectNum() {
        return expectNum;
    }


}
