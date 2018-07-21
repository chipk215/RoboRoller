package com.keyeswest.roboclient;

import com.pi4j.util.Console;
import com.rabbitmq.client.*;
import com.pi4j.io.gpio.*;


import java.time.Instant;

import static java.lang.System.out;

/*
 This code runs on the Raspberry PI
 */
public class RoboClient{

    private static final String EXCHANGE_NAME = "roller.fan";
    private static final String QUEUE_NAME = "ROLL_QUEUE";

    // Update rate is 20 mSec
    private static final Long UPDATE_INTERVAL = 20l;
    private static Long lastUpdate = null;


    private static void updateServo(GpioPinPwmOutput pwm,String message, long receivedTime){
        String[] splitMessage = message.trim().split("\\s+");
        long delay = receivedTime - Long.parseLong(splitMessage[0]);
        out.println("Updating... Message delay (mSec)= " + Long.toString(delay));

        float angle = Float.parseFloat(splitMessage[1]);
        angle += 90.0;
        int pwmValue = (int)((angle * 4.0f / 9.0f ) + 35.0);
        pwm.setPwm(pwmValue);
    }

    public static void main(String[] args) throws Exception {

        out.println("RoboClient");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.0.9");
        factory.setUsername("roller");
        factory.setPassword("roller");
        factory.setPort(5672);

        // set up servo
        int range = 1000;
        int clock = 384;

        // create GPIO controller instance
        GpioController gpio = GpioFactory.getInstance();
        Pin pwmPin = RaspiPin.GPIO_01;

        final GpioPinPwmOutput pwm = gpio.provisionPwmOutputPin(pwmPin);
        // you can optionally use these wiringPi methods to further customize the PWM generator
        // see: http://wiringpi.com/reference/raspberry-pi-specifics/
        com.pi4j.wiringpi.Gpio.pwmSetMode(com.pi4j.wiringpi.Gpio.PWM_MODE_MS);
        com.pi4j.wiringpi.Gpio.pwmSetRange(range);
        com.pi4j.wiringpi.Gpio.pwmSetClock(clock);

        // create Pi4J console wrapper/helper
        // (This is a utility class to abstract some of the boilerplate code)
        final Console console = new Console();

        // allow for user to exit program using CTRL-C
        console.promptForExit();

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclarePassive(EXCHANGE_NAME);
        channel.queueDeclarePassive(QUEUE_NAME);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) {

                try {


                    Instant instant = Instant.now();
                    long receivedTime = instant.toEpochMilli();

                    String message = new String(body, "UTF-8");
                    out.println("Received Time + Message= " +  Long.toString(receivedTime) + " " + message);

                    if ((lastUpdate == null) ||  ((receivedTime - lastUpdate) > UPDATE_INTERVAL)) {
                        // update
                        updateServo(pwm, message, receivedTime);
                        lastUpdate = receivedTime;
                    }
                    else{
                        out.println("Not updating servo.");
                    }


                }catch(Exception ex){
                    out.println(ex);
                }
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);

        console.println("Press ENTER to stop");
        //noinspection Since15
        System.console().readLine();

        // set the PWM rate to 0
        pwm.setPwm(0);
        console.println("PWM rate is: " + pwm.getPwm());

        // stop all GPIO activity/threads by shutting down the GPIO controller
        // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
        gpio.shutdown();

    }


}
