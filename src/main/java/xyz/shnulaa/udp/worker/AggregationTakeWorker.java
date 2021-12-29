package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.*;

import java.io.FileOutputStream;
import java.io.IOException;
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
//                Utils.log("take-> md5:" + event.getMd5() + ", uuid:" + event.getUuid());
                Map<String, Value> positionMap = server.getPositionMap();
//                Key key = new Key(event.getMd5());
                // position map not ready yet
                if (!positionMap.containsKey(event.getMd5())) {
                    Utils.error("key has no position yet.");
//                    Thread.sleep(10);
                    event.setPriority(server.getRetryIndex().getAndIncrement());
                    _processQueue.add(event);
                    continue;
                }

                Value value = positionMap.get(event.getMd5());
                // current package position not read yet
                if (value.getPackagePosition() != current.get()) {
                    Utils.error("current package position : " + value.getPackagePosition() + " not read yet");
//                    Thread.sleep(1000);
                    event.setPriority(server.getRetryIndex().getAndIncrement());
                    _processQueue.add(event);
                    continue;
                }

                Map<String, Integer> map = value.getPosition();
                if (!map.containsKey(event.getUuid())) {
                    Utils.error("uuid position : " + event.getUuid() + " not read yet");
//                    Thread.sleep(1000);
                    event.setPriority(server.getRetryIndex().getAndIncrement());
                    _processQueue.add(event);
                    continue;
                }

                CharBuffer charBuffer = server.initCharBuffer(value.getTotalLength());
                synchronized (Utils.class) {
                    charBuffer.position((map.get(event.getUuid()) * (Constant.BODY_LENGTH)));
                    charBuffer.put(event.getBody().toCharArray());
                }

                if (value.isAlready()) {
                    writeToFile(charBuffer, value.getTotalLength());
                    charBuffer.clear();
                    current.incrementAndGet();
//                    break;
                } else {
                    Utils.error("md5:" + event.getMd5() + ",indexMap size:" + value.getExpectNum() + ", count size:" + value.getReaded().get());
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
    private void writeToFile(CharBuffer charBuffer, int length) throws IOException {
        charBuffer.position(length);
        charBuffer.flip();
        byte[] bytes = Utils.hexStringToByteArray(charBuffer.toString());
        try (FileOutputStream output = new FileOutputStream(Constant.OUTPUT_FILE_FULL_PATH, true)) {
            System.out.println("write file..");
            output.write(bytes);
        }
    }
}