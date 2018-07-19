package com.keyeswest.roboroller


import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import com.keyeswest.rollview.RollView
import timber.log.Timber
import java.lang.Math.abs

class MainActivity : AppCompatActivity() , Orientation.Listener{

    private var mOrientation: Orientation? = null
    private var mGauge: RollView? = null

    private var mLastRoll: Float = 90.0f

    override fun onOrientationChanged(pitch: Float, roll: Float) {
        var limitedRoll = roll


        // limit roll angle to range of -90 to + 90 degrees
        if (limitedRoll < -90.0f) {
            limitedRoll = -90.0f
        }
        if (limitedRoll > 90.0f){
            limitedRoll = 90.0f;
        }


        mGauge!!.moveToValue(-limitedRoll)


        if (abs(abs(limitedRoll) - abs(mLastRoll)) > 1.0f) {
            mLastRoll = limitedRoll
            Timber.d( "Queuing roll angle: " + java.lang.Float.toString(limitedRoll))

            val thread = object : Thread() {
                override fun run() {
                    try {
                        RabbitSender.getInstance().sendRollAngle(-limitedRoll)
                    } catch (ex: Exception) {
                        Timber.e(ex.toString())
                    }

                }
            }
            thread.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mOrientation = Orientation(this)
        mGauge = findViewById(R.id.gauge)

        mGauge!!.moveToValue(0.0f)

    }

    override fun onStart() {
        super.onStart()
        mOrientation!!.startListening(this)
    }

    override fun onStop() {
        super.onStop()
        mOrientation!!.stopListening()
        try {
            RabbitSender.getInstance().close()
        } catch (ex: Exception) {

        }

    }


}
