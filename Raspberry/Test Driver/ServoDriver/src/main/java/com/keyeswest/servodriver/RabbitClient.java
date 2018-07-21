package com.keyeswest.servodriver;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.time.Instant;


public class RabbitClient {

    private final static String QUEUE_NAME = "ROLL_QUEUE";
    private final static String HOST_IP = "192.168.0.9";
    private final static String RABBIT_USER_NAME = "roller";
    private final static String RABBIT_PASSWORD = "roller";
    private final static int RABBIT_PORT= 5672;

    private final static ConnectionFactory sFactory = new ConnectionFactory();
    private static Connection sConnection;
    private static Channel sChannel;

    private RabbitClient(){}

    private static RabbitClient sInstance;

    public static RabbitClient getInstance(){
        if (sInstance == null){
            try {
                sInstance = new RabbitClient();
                sFactory.setHost(HOST_IP);
                sFactory.setUsername(RABBIT_USER_NAME);
                sFactory.setPassword(RABBIT_PASSWORD);
                sFactory.setPort(RABBIT_PORT);
                sConnection = sFactory.newConnection();
                sChannel = sConnection.createChannel();

            }catch(Exception ex){
                System.out.println(ex.toString());
            }

        }

        return sInstance;
    }

    public  void sendRollAngle(float rollAngle) throws Exception{

        System.out.println("Sending roll angle to rabbit queue");

        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();

        String message = Long.toString(timeStampMillis) + " " + Float.toString(rollAngle);

        if (sChannel != null) {
            sChannel.basicPublish("", QUEUE_NAME, null,
                    message.getBytes("UTF-8"));
        }else{
            System.out.println("Rabbit channel not configured");
        }

    }

    public  void close() throws Exception{
        sChannel.close();
        sConnection.close();
    }
}
