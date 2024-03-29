package com.example.kotlinbluetooth

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "Orientation"

private const val SENSOR_DELAY_MICROS = 16 * 1000 // 16ms

private const val ALPHA = 0.25f
var rotSensorVals: FloatArray? = null

class Orientation(activity: Activity) : SensorEventListener {

    interface Listener {
        fun onOrientationChanged(roll: Float)
    }

    private val mWindowManager: WindowManager = activity.window.windowManager
    private val mSensorManager: SensorManager = activity.getSystemService(Activity.SENSOR_SERVICE) as SensorManager

    private val mRotationSensor: Sensor?
    private var mLastAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE

    private var mListener: Listener? = null

    init {
        // Can be null if the sensor hardware is not available
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }


    fun startListening(listener: Listener) {
        if (mListener === listener) {
            return
        }

        mListener = listener

        if (mRotationSensor == null) {
            Log.w(TAG, "Rotation vector sensor not available; will not provide orientation data.")
            return
        }

        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }


    fun stopListening() {
        mSensorManager.unregisterListener(this)
        mListener = null
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        mLastAccuracy = accuracy
    }


    override fun onSensorChanged(event: SensorEvent) {
        if (mListener == null) {
            return
        }

        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }

        if (event.sensor == mRotationSensor) {
           /* rotSensorVals = lowPass(event.values.clone(), rotSensorVals)
            updateOrientation(rotSensorVals!!)*/
            updateOrientation(event.values)
        }
    }


    private fun updateOrientation(rotationVector: FloatArray) {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val (worldAxisForDeviceAxisX, worldAxisForDeviceAxisY) = when (mWindowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Z)
            Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X)
            Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Z)
            Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X)
            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Z)
        }


        val adjustedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix)

        // Transform rotation matrix into azimuth/pitch/roll
        val orientation = FloatArray(3)
        SensorManager.getOrientation(adjustedRotationMatrix, orientation)

        // Convert radians to degrees
        val pitch = orientation[1] * -57
        var roll = orientation[2] * -57


        mListener?.onOrientationChanged(roll)

    }

    fun lowPass(input: FloatArray, output: FloatArray?) : FloatArray {
        if(output == null) {
            return input
        }

        for (i in 0 until input.size) {
            output[i] = output[i] + ALPHA * (input[i] - output[i])
        }
        return output
    }
}