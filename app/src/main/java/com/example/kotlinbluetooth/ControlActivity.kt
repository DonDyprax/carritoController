package com.example.kotlinbluetooth

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.control_layout.*
import java.io.IOException
import java.util.*
import kotlin.math.roundToInt
import android.system.Os.socket
import androidx.annotation.WorkerThread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.schedule


class ControlActivity : AppCompatActivity(), Orientation.Listener {
    lateinit var mOrientation: Orientation
    val ALPHA = 0.25f

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
        var charset = Charsets.UTF_8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)
        m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)

        mOrientation = Orientation(this)


        ConnectToDevice(this).execute()



        control_disconnect.setOnClickListener { disconnect() }

        //control_send.setOnClickListener { sendCommand("3") }
    }


    fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray(charset))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    override fun onStart() {
        super.onStart()
        mOrientation.startListening(this)
    }

    override fun onStop() {
        super.onStop()
        mOrientation.stopListening()
    }

    override fun onOrientationChanged(roll: Float) {

        var rollVal = roll.toInt()

        if(rollVal > 45) { rollVal = 45 }
        if(rollVal < -45) { rollVal = -45 }

        when(rollVal) {
            in 45 downTo 31 -> sendCommand("A")
            in 30 downTo 16 -> sendCommand("B")
            in 15 downTo 1 -> sendCommand("C")
            in 0 downTo  -15 -> sendCommand("D")
            in -16 downTo -30 -> sendCommand("E")
            in -31 downTo -45 -> sendCommand("F")
        }

        /*var rollLimit: Double

        if (roll < -45) {
            rollLimit = -45.00
            sendCommand(rollLimit.toString())
            return
        }

        if (roll > 45) {
            rollLimit = 45.00
            sendCommand(rollLimit.toString())
            return
        }
*/
       // control_pitch.text = pitch.roundToInt().toString()
        //control_roll.text = roll.roundToInt().toString()
    }

    class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        var connectSuccess: Boolean = true
        val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("data", "could not connect")
            } else {
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }

/*
    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>()  // UI thread
    {
        val context: Context
        //var ConnectSuccess = false //if it's here, it's almost connected

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg params: Void?): String? //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()//get the mobile bluetooth device
                    var device = m_bluetoothAdapter.getRemoteDevice(m_address)//connects to the device's address and checks if it's available
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()//start connection
                }
            } catch (e: IOException) {
                m_isConnected = false//if the try failed, you can check the exception here
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!m_isConnected) {
                Log.i("data", "could not connect")
            } else {
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }
*/
}

