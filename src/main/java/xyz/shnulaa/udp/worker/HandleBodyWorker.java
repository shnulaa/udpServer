package xyz.shnulaa.udp.worker;

import xyz.shnulaa.udp.*;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import static xyz.shnulaa.udp.Constant.*;

public class HandleBodyWorker implements Callable<Void> {
    private final Selector selector;

    public HandleBodyWorker(Selector _selector) {
        this.selector = _selector;
    }

    @Override
    public Void call() throws Exception {
        ChannelPoolServer server = ChannelPoolServer.getInstance();
        ByteBuffer byteBuffer = ByteBuffer.allocate((BODY_LENGTH + UUID_HEAD_LENGTH + MD5_LENGTH) * SEND_PER_PACKAGE * 3);
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
                            if (after.isEmpty() || after.length() < Constant.UUID_HEAD_LENGTH) {
                                Utils.error("error!!");
                                continue;
                            }

                            String md5 = after.substring(0, MD5_LENGTH);
                            String uuid = after.substring(MD5_LENGTH, MD5_LENGTH + UUID_HEAD_LENGTH);
                            String body = after.substring(MD5_LENGTH + UUID_HEAD_LENGTH, after.length());
                            server.getBlockQueue().add(new Event(md5, uuid, body, server.getTotalIndex().getAndIncrement()));

//                            Map<Key, Value> positionMap = server.getPositionMap();
//                            Key key = new Key(md5, uuid);
//                            if (positionMap.containsKey(key)) {
//                                Value value = positionMap.get(key);
//                                server.getBlockQueue().put(new Event(md5, uuid , body));
//                            }


//                            synchronized (Utils.class) {
//                                Map<String, BlockingQueue<String>> map = ChannelPoolServer.getInstance().getQueue();
//                                if (!map.containsKey(md5)) {
//                                    Utils.log("HandleBodyWorker md5:" + md5 + " not exist.");
//                                    map.put(md5, new LinkedBlockingQueue<String>());
//                                }
//                                Utils.error("put -> " + md5);
//                                map.get(md5).put(body);
//                            }
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
