package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.ChannelPoolServer;
import xyz.shnulaa.udp.Constant;
import xyz.shnulaa.udp.Utils;

import java.io.FileOutputStream;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AggregationWorker implements Callable<CharBuffer> {

    private final String keys;
    private final String md5Key;
    private final Map<String, Integer> indexMap;
    private final AtomicInteger count;


    public AggregationWorker(String _keys) {
        this.keys = _keys;
        this.md5Key = Utils.getMD5Str(this.keys);
        String[] array = _keys.split(",");
        this.indexMap = new ConcurrentHashMap<>(array.length);
        for (int index = 0; index < array.length; index++) {
            this.indexMap.put(array[index], index);
        }
        this.count = new AtomicInteger(0);
    }

    @Override
    public CharBuffer call() throws Exception {
        CharBuffer charBuffer = ChannelPoolServer.getInstance().initCharBuffer(Constant.bodyLength * indexMap.size() * 2);
        BlockingQueue<String> _processQueue = null;

        synchronized (Utils.class) {
            Map<String, BlockingQueue<String>> map = ChannelPoolServer.getInstance().getQueue();
            if (!map.containsKey(md5Key)) {
                Utils.log("AggregationWorker md5:" + md5Key + " not exist.");
                map.put(md5Key, new LinkedBlockingQueue<String>());
            }
            _processQueue = map.get(md5Key);
        }

        if (_processQueue == null) {
            System.err.println("AggregationWorker _processQueue is null..");
            return null;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String event = _processQueue.take();
                if (event.isEmpty()) {
                    continue;
                }

                Utils.log("take-> md5:" + md5Key + ", event:" + event);

                String key = event.substring(0, Constant.headLength);
                String body = event.substring(Constant.headLength, event.length());
                Integer index = indexMap.get(key);

                if (index == null) {
                    Utils.error("key:" + key + ", body:" + body + ", index = null");
                    continue;
                }

                synchronized (Utils.class) {
                    charBuffer.position((index * (Constant.bodyLength)));
                    charBuffer.put(body.toCharArray());
                }

                Utils.log("key:" + key + ", body:" + body);
                if (count.incrementAndGet() == indexMap.size()) {
                    charBuffer.position((indexMap.size() - 1) * (Constant.bodyLength) + body.length());

                    charBuffer.flip();
                    byte[] bytes = Utils.hexStringToByteArray(charBuffer.toString());
                    try (FileOutputStream output = new FileOutputStream(Constant.OUTPUT_FILE_FULL_PATH, true)) {
                        System.out.println("write file..");
                        output.write(bytes);
                    }
//                    if (charBuffer.length() != 204800) {
//                        System.out.println("Complete........, charBuffer.length():" + charBuffer.length());
//                        System.err.println("index:" + index + ",body length:" + body.length() + ",indexMap size:" + indexMap.size() + ", count size:" + count.get() + ", body:" + body);
//                    }
//                    ChannelPool.getInstance().clearCharBuffer();

                    break;
                } else {
                    Utils.error("indexMap size:" + indexMap.size() + ", count size:" + count.get());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utils.error(e.getMessage());
                break;
            }
        }
        return charBuffer;
    }
}