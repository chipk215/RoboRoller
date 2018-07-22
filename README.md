# RoboRoller Introduction 
RoboRoller is a toy Android and Raspberry Pi application. The roll angle of the Android device is obtained using the Android hardware SensorManager library. The roll angle of the Android is displayed using a custom view on the Android device. The angle is then sent to a Raspberry Pi which positions a servo to the same roll angle. If the Android device and servo are aligned so that the axes of roll rotation for both devices are parallel then the servo will rotate in concert with the Android device as the Android is rotated about its longitudinal roll axis.  

## Major System Components
![System Components](docs/readmeImages/system.png)

### Android Device
The Android app was developed on a phone but could be modified to run on a tablet. The app is locked to the portrait orientation for convenience. There is nothing really noteworthy about the device.

### Android Software
The Android application is contained in the app folder of the project Repo. The app uses two external libraries, Jake Wharton's Timber library for logging and the RabbitMQ amqp-client for sending roll angle data to the RabbitMQ server.

The Android app subscribes for orientation updates provided by the android.hardware.SensorManager. [Keith Platfoot's](https://tinyurl.com/y9ayq2cn) excellent Android rotation sensor example provided the basis for obtaining the roll angle of the device. The yaw and pitch angle data is not used but could similarly be mapped to two additional servos for a more complete remote attitude positioning of the device.

A custom guage view was created for displaying the roll angle on the Android. The RollView gauge is packaged as an Android library and is derived from the beautiful [Gauge](https://tinyurl.com/yc9qf7ht) developed by Serge Helfrich. I found the Gauge project to be an outstanding example of creating an artful design!

![Roll Angle](docs/readmeImages/robo-gauge.png)  
  

The Android app applies some high pass filtering of the roll angle and only transmits angle changes of 1 degree or more to the RabbitMQ server. As noted in the Servo section, the servo frequency is 50 Hz which could easily be overrun by more granular angle updates.

Finally, as noted, the last functional aspect of the Android app is to send the filtered roll angle samples to the RabbitMQ server.


### Raspberry Pi
I used a Model B+ Raspebrry Pi, model year 2014, leftover from a previous project which I used to drive the servo. I used the [pi4j](http://pi4j.com/) Java library for writing the pi code. Any of the Raspberry models should be adequate for the solution that have a PWM output pin. 

### Raspberry Pi Software
The Pi software is contained in the Raspberry directory of this repository. The Pi software provides functionality for configuring a PWM GPIO pin connected to the servo, receiving the roll angle updates from the RabbitMQ broker, and then calculating the PWM value for positioning the servo to correspond to the roll angle provided by the Android device. The PWM calculations are provided below in the Servo section.

As noted above, my Pi is the B+ model and I ran into some issues with the configuration of pi4j and the pi4j dependency on WiringPi.  See https://www.raspberrypi.org/forums/viewtopic.php?t=182191.  I updated pi4j and WiringPi as recommended but to no avail. The solution for me came by telling pi4j to not use it's embedded WiringPi but to instead use the the shared WiringPi library installed on the Pi.  This was inconvenient because it required building the executable JAR for the Pi on the Pi rather than on the iMAc I was using to write the Pi code. My Pi workflow distilled down to creating the source file on my iMAc, copying the file to the Pi, and then building the JAR file on the Pi.

My compile command on the Pi:  javac -cp .:classes:/opt/pi4j/lib/*:/opt/amqp/*:/opt/slf4j/* -d . RoboClient.java

To execute: sudo java -Dpi4j.linking=dynamic -classpath .:classes:/opt/pi4j/lib/'*':/opt/amqp/*:/opt/slf4j/* com.keyeswest.roboclient.RoboClient


### Servo
An inexpensive servo was used to track the roll angle. Specifically, a [TowerPro SG-5010](https://tinyurl.com/ya6trczd) which is spec'd to have a 180&deg; range of motion but which I found required some experimentation to achieve.  I powered the servo using a regulated power supply, independent of the Pi operationg at 5.5 volts.

The servo is expected to operate at 50 Hz (20 mSec period) with the center range position achieved with a 1.5 mSec pulse at the beginning of the 50 Hz cycle. The lower end (-90&deg;) of the servo range should be achieved with a 1.0 mSec pulse while the upper end (+90&deg;) is achieved with a 2.0mSec pulse.  In terms of duty cycle the -90&deg; position corresponds to 5% of the 50HZ duty cycle, the center position at 7.5% and the +90&deg; position at 10%. I found these calculations to just be starting points for controlling the servo.

The pi4j library provides an API for configuring a PWM pin on the Raspberry Pi. I used GPIO pin 01. Configuring the GPIO pin required specifying parameters for the equation:

  PWM Frequency = PWM base clock frequency / pwmClock / pwmRange
     50 Hz = 19.2MHz = pwmClock / pwmRange
     
I arbitrarily chose a range of 1000 and clock of 384 to set the PWM frequency to 50 Hz.

With the chosen parameters I found that the servo was only moving +/70&deg; so I began experimenting with the duty cycle to extend the range to +/-90&deg;. 
I kept the center point at 7.5% of the pwmRange corresponding to a pwm value of 75. A linear relationship could then be established for the endpoints using:
    pwm value = m * angle + b   
    
    where m= slope = (75 - lowerLimit) / 180
    b = y intercept = lowerlimit
    
Choosing a lower limit of 35 for the pwm value yields:  

pwm value = roll angle * (4/9) + 35  for 0 <= angle <= 180  

which resulted in nearly 180&deg; range of motion for the servo.  This calculation is contained in the updateServo function of the RoboClient.java class.


### RabbitMQ Server
The roll angle of the Android device is transmitted to the Raspberry Pi using a RabbitMQ message broker. I hosted the RabbitMQ server on an iMac so that the Android code and Raspeberry Pi code could be developed without requiring both platforms to be up and running all of the time. Initially, I also had some reservation about hosting the RabbitMQ server on the Pi but it appears from other project reviews on the web that the Pi is more than capable of hosting the RabbitMQ server. Something to investigate if interested.

### System Response
The system response characterizes the ability of the servo to track the roll angle of the rotating android device. A planar surface could be attached to the servo motor which would correspond to the surface of the android. The system response error would be the difference in the instantaneous roll ange of the servo mounted plane to the horizontal orientation of the andoid device.

A couple of contributing errors come into play. There is propagation error corresponding to the delay in detecting and transmitting the roll angle on the android to the servo through the message broker and there is a limit to how fast the servo can be updated with new servo positions.

The 50 Hz PWM frequency establishes the maximum update rate of 50 samples per second. The servo is also limited by how fast it can rotate. The TowerPro SG-5010 quotes an average speed of 0.16sec/60&deg;, which seems to be an inverted velocity spec (radial velocity should be in degrees/sec) but approximately corresponds to 1 second for the servo to rotate across its full 180&deg; range.  The update rate and radial velocity of the servo obviously limits how fast we can rotate the android device across 180&deg; of rotation.

More to come...








