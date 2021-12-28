package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.ChannelPool;
import xyz.shnulaa.udp.Contant;
import xyz.shnulaa.udp.Utils;

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
        CharBuffer charBuffer = ChannelPool.getInstance().initCharBuffer();

        BlockingQueue<String> _processQueue = null;

        synchronized (Utils.class) {
            Map<String, BlockingQueue<String>> map = ChannelPool.getInstance().getQueue();
            if (!map.containsKey(md5Key)) {
                Utils.log("AggregationWorker md5:" + md5Key + " not exist.");
                map.put(md5Key, new LinkedBlockingQueue<String>());
            }
            _processQueue = map.get(md5Key);
        }


//        Map<String, BlockingQueue<String>> map = ChannelPool.getInstance().getQueue();
//        synchronized (Utils.class) {
//            if (!map.containsKey(md5Key)) {
//                map.put(md5Key, new LinkedBlockingQueue<String>());
//            }
//            _processQueue = map.get(md5Key);
//        }


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

                String key = event.substring(0, Contant.headLength);
                String body = event.substring(Contant.headLength, event.length());
                Integer index = indexMap.get(key);

                if (index == null) {
                    Utils.log("key:" + key + ", body:" + body + ", index = null");
                    continue;
                }
                charBuffer.position((index * (Contant.bodyLength)));
                for (char c : body.toCharArray()) {
                    charBuffer.put(c);
                }

                Utils.log("key:" + key + ", body:" + body);
                if (count.incrementAndGet() == indexMap.size()) {
                    System.out.println("Complete........");
                    break;
                } else {
//                    Utils.error("indexMap size:" + indexMap.size() + ", count size:" + count.get());
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