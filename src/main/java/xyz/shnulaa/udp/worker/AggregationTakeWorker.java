package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AggregationTakeWorker implements Callable<CharBuffer> {
    public AggregationTakeWorker() {
        this.current = new AtomicLong(1);
    }

    private final AtomicLong current;

    @Override
    public CharBuffer call() throws Exception {
        ChannelPoolServer server = ChannelPoolServer.getInstance();
        BlockingQueue<Event> _processQueue = server.getBlockQueue();
        while (!Thread.currentThread().isInterrupted() && !server.getInteruptAWorker().get()) {
            try {
                Event event = _processQueue.take();
                String md5Sum = event.getMd5();
//                Utils.log("take-> md5:" + event.getMd5() + ", uuid:" + event.getUuid());
                Map<String, Value> positionMap = server.getPositionMap();
//                Key key = new Key(event.getMd5());
                // position map not ready yet
                if (!positionMap.containsKey(md5Sum)) {
                    server.getRetryService().submit(new Runnable() {
                        @Override
                        public void run() {
                            Utils.error("key " + md5Sum + " has no position yet.");
                            try {
                                Thread.sleep(event.addTimeout());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            event.setPriority(server.getRetryIndex().getAndIncrement());
                            _processQueue.add(event);
                        }
                    });
                    continue;
                }

                Value value = positionMap.get(md5Sum);
                if (value.isDone()) {
                    Utils.error("key " + md5Sum + " is already done.");
                    continue;
                }
                Map<String, Integer> map = value.getPosition();

//                CharBuffer charBuffer = server.initCharBuffer(value.getTotalLength());
                CharBuffer charBuffer = value.getCharBuffer();
                synchronized (Utils.class) {
                    int index = map.get(event.getUuid());
                    charBuffer.position((index * (Constant.BODY_LENGTH)));
                    charBuffer.put(event.getBody().toCharArray());
                }

                if (value.isAlready()) {
                    writeToFile(value, md5Sum);
                    charBuffer.clear();
                    charBuffer = null;
                    value.setDone();
//                    positionMap.remove(md5Sum);
                } else {
                    // Utils.error("md5:" + event.getMd5() + ",indexMap size:" + value.getExpectNum() + ", count size:" + value.getReaded().get());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utils.error(e.getMessage());
                break;
            }
        }
        return null;
    }

    /**
     * writeToFile
     *
     * @param charBuffer
     * @param length
     * @throws IOException
     */
    private void writeToFile(Value value, String md5) throws IOException {
        CharBuffer charBuffer = value.getCharBuffer();
        charBuffer.position(value.getTotalLength());
        charBuffer.flip();
        byte[] bytes = Utils.hexStringToByteArray(charBuffer.toString());
        try (RandomAccessFile file = new RandomAccessFile(new File(Constant.OUTPUT_FILE_FULL_PATH), "rw")) {
            Utils.log("write file, md5:" + md5 + ", seek:" + value.getPackagePosition() + ", position: " + value.getTotalLength());
            file.seek(value.getPackagePosition());
            file.write(bytes);
        }
    }
}