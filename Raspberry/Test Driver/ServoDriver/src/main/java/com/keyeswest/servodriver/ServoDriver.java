package com.keyeswest.servodriver;

public class ServoDriver {

    public static void main(String[] args) throws Exception{

        System.out.println("Starting ServoDriver.");

        // send roll angles -90 to 90
        RabbitClient rabbitClient =RabbitClient.getInstance();
/*
        for (int i=-90; i<= 90; i++){
            rabbitClient.sendRollAngle(i);
        }

        for (int i=90; i>= -90; i--){
            rabbitClient.sendRollAngle(i);
        }


*/
        rabbitClient.sendRollAngle(0);
        Thread.sleep(1000);

        // +90 -> CCW
        rabbitClient.sendRollAngle(+90);
        Thread.sleep(1000);

        // -90 -> CW
        rabbitClient.sendRollAngle(-90);
        Thread.sleep(1000);

        rabbitClient.sendRollAngle(0);
        Thread.sleep(1000);


        rabbitClient.close();
    }
}
