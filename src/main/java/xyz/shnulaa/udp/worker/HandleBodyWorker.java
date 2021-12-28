package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.ChannelPool;
import xyz.shnulaa.udp.Constant;
import xyz.shnulaa.udp.Utils;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import static xyz.shnulaa.udp.Constant.bodyLength;

public class HandleBodyWorker implements Callable<Void> {
    private final Selector selector;

    public HandleBodyWorker(Selector _selector) {
        this.selector = _selector;
    }

    @Override
    public Void call() throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bodyLength * 30);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int eventsCount = selector.select();
                if (eventsCount > 0) {
                    Set<?> selectedKeys = selector.selectedKeys();
                    Iterator<?> iterator = selectedKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = (SelectionKey) iterator.next();
                        iterator.remove();
                        if (sk.isReadable()) {
                            String after = Utils.fetchContent(byteBuffer, sk);
                            if (after.isEmpty() || after.length() < Constant.headLength) {
                                Utils.error("error!!");
                                continue;
                            }

                            String md5 = after.substring(0, Constant.md5Length);
                            String body = after.substring(Constant.md5Length, after.length());

                            synchronized (Utils.class) {
                                Map<String, BlockingQueue<String>> map = ChannelPool.getInstance().getQueue();
                                if (!map.containsKey(md5)) {
                                    Utils.log("HandleBodyWorker md5:" + md5 + " not exist.");
                                    map.put(md5, new LinkedBlockingQueue<String>());
                                }
                                map.get(md5).put(body);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
