package com.keyeswest.roboroller;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import timber.log.Timber;


public class RabbitSender {

    private final static String QUEUE_NAME = "ROLL_QUEUE";
    private final static String HOST_IP = "192.168.0.9";
    private final static String RABBIT_USER_NAME = "roller";
    private final static String RABBIT_PASSWORD = "roller";
    private final static int RABBIT_PORT = 5672;

    private final static ConnectionFactory sFactory = new ConnectionFactory();
    private static Connection sConnection;
    private static Channel sChannel;
    private static RabbitSender sInstance;

    private RabbitSender() {}

    public static RabbitSender getInstance() {
        if (sInstance == null) {
            try {
                sInstance = new RabbitSender();
                sFactory.setHost(HOST_IP);
                sFactory.setUsername(RABBIT_USER_NAME);
                sFactory.setPassword(RABBIT_PASSWORD);
                sFactory.setPort(RABBIT_PORT);
                sConnection = sFactory.newConnection();
                sChannel = sConnection.createChannel();

            } catch (Exception ex) {
                Timber.e(ex.toString());
            }
        }

        return sInstance;
    }


    /**
     * @param rollAngle - negative angles rotate servo CCW from 0 degree center
     *                  - positive angles rotate servo CW from 0 degree center
     * @throws Exception
     */
    public void sendRollAngle(float rollAngle) throws Exception {

        Timber.d("Sending roll angle to rabbit queue");


        long timeStampMillis = System.currentTimeMillis();

        String message = Long.toString(timeStampMillis) + " " + Float.toString(-rollAngle);

        if (sChannel != null) {
            sChannel.basicPublish("", QUEUE_NAME, null,
                    message.getBytes("UTF-8"));
        } else {
            Timber.d("Rabbit channel not configured");
        }

    }

    public void close() throws Exception {
        sChannel.close();
        sConnection.close();
    }
}
