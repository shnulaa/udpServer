package xyz.shnulaa.udp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Bootstrap {
    public static void main(String[] args) throws InterruptedException {
        // server
        if (args.length == 2 && "-s".equals(args[0])) {
            Constant.OUTPUT_FILE_FULL_PATH = args[1] + UUID.randomUUID().toString();
            ChannelPoolServer server = null;
            try {
                server = ChannelPoolServer.getInstance();
                server.getTransferService().awaitTermination(1, TimeUnit.DAYS);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (server != null && server.getTransferService() != null) {
                    server.getTransferService().shutdown();
                }

                if (server != null && server.getAggregationService() != null) {
                    server.getAggregationService().shutdown();
                }

                if (server != null && server.getCommunicationService() != null) {
                    server.getCommunicationService().shutdown();
                }

                if (server != null && server.getRetryService() != null) {
                    server.getRetryService().shutdown();
                }
            }
        } else if (args.length == 3 && "-c".equals(args[0])) {
            ChannelPoolClient client = null;
            try {
                client = ChannelPoolClient.getInstance().init(args[1]);
                try (InputStream inputStream = new FileInputStream(new File(args[2]));) {
                    System.out.println("send...........");
                    client.send(inputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("usage");
            System.err.println("-s");
            System.err.println("-c ip file");
        }
    }
}
