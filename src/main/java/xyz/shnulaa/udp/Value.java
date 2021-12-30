package xyz.shnulaa.udp;

import com.sun.org.apache.bcel.internal.Const;

import java.nio.CharBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Value {
    private long packagePosition;
    // uuid -> position
    private Map<String, Integer> position;
    private int totalLength;
    private AtomicInteger readed;
    private int expectNum;
    private CharBuffer charBuffer;
    private AtomicBoolean isDone = new AtomicBoolean(false);

    public Value(long packagePosition, Map<String, Integer> position, int totalLength, int expectNum) {
        this.packagePosition = packagePosition;
        this.position = position;
        this.totalLength = totalLength;
        this.expectNum = expectNum;
        this.readed = new AtomicInteger(1);
        this.charBuffer = CharBuffer.allocate(totalLength);
    }

    public boolean setDone() {
        return isDone.compareAndSet(false, true);
    }

    public boolean isDone() {
        return isDone.get();
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
        return readed.getAndIncrement() == expectNum;
    }

    public int getExpectNum() {
        return expectNum;
    }


    public CharBuffer getCharBuffer() {
        return charBuffer;
    }


}
