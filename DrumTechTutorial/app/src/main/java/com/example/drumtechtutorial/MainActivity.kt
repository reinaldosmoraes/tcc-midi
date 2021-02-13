package com.example.drumtechtutorial

import android.app.Activity
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiManager.DeviceCallback
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.SeekBar.OnSeekBarChangeListener
import java.util.*
import kotlin.experimental.and


/**
 * Application MainActivity handles UI and Midi device hotplug event from
 * native side.
 */
class MainActivity : Activity(), View.OnClickListener,
    OnSeekBarChangeListener, OnItemSelectedListener {
    private var mAppMidiManager: AppMidiManager? = null

    // Connected devices
    private val mReceiveDevices = ArrayList<MidiDeviceInfo>()
    private val mSendDevices = ArrayList<MidiDeviceInfo>()

    // Send Widgets
    var mOutputDevicesSpinner: Spinner? = null
    var mControllerSB: SeekBar? = null
    var mPitchBendSB: SeekBar? = null
    var mProgNumberEdit: EditText? = null

    // Receive Widgets
    var mInputDevicesSpinner: Spinner? = null
    var mReceiveMessageTx: TextView? = null

    //Teste do meu tcc
//    var outputMessage: TextView? = null
//    var pattern: ArrayList<Tap?> = createPattern(
//        Hand.RIGHT,
//        Hand.LEFT,
//        Hand.RIGHT,
//        Hand.RIGHT,
//        Hand.LEFT,
//        Hand.RIGHT,
//        Hand.LEFT,
//        Hand.LEFT
//    )
//    var defaultIntensity = 10
//    var bpm = 120
//    var taps: ArrayList<Tap?> = ArrayList<Any?>()
//    private fun createPattern(vararg hands: Hand): ArrayList<Tap?> {
//        val pattern: ArrayList<Tap?> = ArrayList<Any?>()
//        for (hand in hands) {
//            val tap = Tap(hand, defaultIntensity, 500)
//            pattern.add(tap)
//        }
//        return pattern
//    }
//
//    private fun insertTapOnListWithInterval(tap: Tap) {
//        Log.d("MIDI_DEBUG", "insertTapOnListWithInterval_method")
//        val currentTap: Tap
//        if (taps.isEmpty()) {
//            currentTap = tap
//        } else {
//            val previousTap: Tap? = taps[taps.size - 1]
//            currentTap = tap
//            val interval: Long =
//                currentTap.getTapTime().getTime() - previousTap.getTapTime().getTime()
//            currentTap.setInterval(interval)
//        }
//        taps.add(currentTap)
//        Log.d("MIDI_DEBUG", "TAPS")
//        Log.d("MIDI_DEBUG", taps.toString())
//    }
//
//    private fun verifyTap(correctTap: Tap?, currentTap: Tap?): Boolean {
//        Log.d("MIDI_DEBUG", "VERIFY_TAP_METHOD")
//        return if (correctTap.getHand() === currentTap.getHand() && isInTime(currentTap.getInterval())) {
//            //            icCorrectImageView.setVisibility(View.VISIBLE);
//            //            icIncorrectImageView.setVisibility(View.INVISIBLE);
//            Log.d("MIDI_DEBUG", "VERIFY_TAP_METHOD_IS_VALID")
//            true
//        } else {
//            //            icCorrectImageView.setVisibility(View.INVISIBLE);
//            //            icIncorrectImageView.setVisibility(View.VISIBLE);
//            Log.d("MIDI_DEBUG", "VERIFY_TAP_IS_INVALID")
//            false
//        }
//    }
//
//    private fun isInTime(currentInterval: Long): Boolean {
//        return if (currentInterval == 0L) { //first tap
//            true
//        } else if (currentInterval > bpmToMs(bpm) - 200 && currentInterval < bpmToMs(bpm) + 200) { //in time
//            true
//        } else { //out of time
//            false
//        }
//    }
//
//    private fun bpmToMs(bpm: Int): Long {
//        return (60000 / bpm).toLong()
//    }
//
//    companion object {
//        private val TAG = MainActivity::class.java.name
//
//        // Force to load the native library
//        init {
//            AppMidiManager.loadNativeAPI()
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //
        // Init JNI for data receive callback
        //
        initNative()

        //
        // Setup UI
        //
        mOutputDevicesSpinner = findViewById<View>(R.id.outputDevicesSpinner) as Spinner
        mOutputDevicesSpinner!!.onItemSelectedListener = this
        (findViewById<View>(R.id.keyDownBtn) as Button).setOnClickListener(this)
        (findViewById<View>(R.id.keyUpBtn) as Button).setOnClickListener(this)
        (findViewById<View>(R.id.progChangeBtn) as Button).setOnClickListener(this)
        mControllerSB = findViewById<View>(R.id.controllerSeekBar) as SeekBar
        mControllerSB!!.max = MidiSpec.MAX_CC_VALUE.toInt()
        mControllerSB!!.setOnSeekBarChangeListener(this)
        mPitchBendSB = findViewById<View>(R.id.pitchBendSeekBar) as SeekBar
        mPitchBendSB!!.max = MidiSpec.MAX_PITCHBEND_VALUE
        mPitchBendSB!!.progress = MidiSpec.MID_PITCHBEND_VALUE
        mPitchBendSB!!.setOnSeekBarChangeListener(this)
        mInputDevicesSpinner = findViewById<View>(R.id.inputDevicesSpinner) as Spinner
        mInputDevicesSpinner!!.onItemSelectedListener = this
        mProgNumberEdit = findViewById<View>(R.id.progNumEdit) as EditText
        mReceiveMessageTx = findViewById<View>(R.id.receiveMessageTx) as TextView
//        outputMessage = findViewById(R.id.outputMessage)
        val midiManager = getSystemService(MIDI_SERVICE) as MidiManager
        midiManager.registerDeviceCallback(MidiDeviceCallback(), Handler())

        //
        // Setup the MIDI interface
        //
        mAppMidiManager = AppMidiManager(midiManager)

        // Initial Scan
        ScanMidiDevices()
    }

    /**
     * Device Scanning
     * Methods are called by the system whenever the set of attached devices changes.
     */
    private inner class MidiDeviceCallback : DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            ScanMidiDevices()
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            ScanMidiDevices()
        }
    }

    /**
     * Scans and gathers the list of connected physical devices,
     * then calls onDeviceListChange() to update the UI. This has the
     * side-effect of causing a list item to be selected, which then
     * invokes the listener logic which connects the device(s).
     */
    private fun ScanMidiDevices() {
        mAppMidiManager!!.ScanMidiDevices(mSendDevices, mReceiveDevices)
        onDeviceListChange()
    }
    //
    // UI Helpers
    //
    /**
     * Formats a set of MIDI message bytes into a user-readable form.
     * @param message   The bytes comprising a Midi message.
     */
    private fun showReceivedMessage(message: ByteArray) {
        when (Helper.teste(message[0])) {
            MidiSpec.MIDICODE_NOTEON -> {
                Log.d("MIDI_DEBUG", "NOTE_OFF")
                mReceiveMessageTx!!.text = "NOTE_ON [ch:" + (message[0] and 0x0F) +
                        " key:" + message[1] +
                        " vel:" + message[2] + "]"
            }
            MidiSpec.MIDICODE_NOTEOFF -> {
                mReceiveMessageTx!!.text = "NOTE_OFF [ch:" + (message[0] and 0x0F) +
                        " key:" + message[1] +
                        " vel:" + message[2] + "]"
//                Log.d("MIDI_DEBUG", "NOTE_OFF")
//                val hand: Hand = if (message[1] == 48) Hand.LEFT else Hand.RIGHT
//                val tap = Tap(hand, message[2], Date())
//                Log.d("MIDI_DEBUG", tap.toString())
//                insertTapOnListWithInterval(tap)
//                if (verifyTap(
//                        pattern[(taps.size - 1) % pattern.size],
//                        taps[taps.size - 1]
//                    )
//                ) { //TODO: mudar nome para isTapValid
//                    outputMessage!!.text = "Left hand tap"
//                } else {
//                    outputMessage!!.text = "Right hand tap"
//                }
            }
        }
    }

    //
    // View.OnClickListener overriden methods
    //
    override fun onClick(view: View) {
        val keys = byteArrayOf(60, 64, 67) // C Major chord
        val velocities = byteArrayOf(60, 60, 60) // Middling velocity
        val channel: Byte = 0 // send on channel 0
        when (view.id) {
            R.id.keyDownBtn ->                 // Simulate a key-down
                mAppMidiManager!!.sendNoteOn(channel, keys, velocities)
            R.id.keyUpBtn ->                 // Simulate a key-up (converse of key-down above).
                mAppMidiManager!!.sendNoteOff(channel, keys, velocities)
            R.id.progChangeBtn -> {

                // Send a MIDI program change message
                try {
                    val progNumStr = mProgNumberEdit!!.text.toString()
                    val progNum = progNumStr.toInt()
                    mAppMidiManager!!.sendProgramChange(channel, progNum.toByte())
                } catch (ex: NumberFormatException) {
                    // Maybe let the user know
                }
            }
        }
    }

    //
    // SeekBar.OnSeekBarChangeListener overriden messages
    //
    override fun onProgressChanged(seekBar: SeekBar, pos: Int, fromUser: Boolean) {
        when (seekBar.id) {
            R.id.controllerSeekBar -> mAppMidiManager!!.sendController(
                0.toByte(), MidiSpec.MIDICC_MODWHEEL,
                pos.toByte()
            )
            R.id.pitchBendSeekBar -> mAppMidiManager!!.sendPitchBend(0.toByte(), pos)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    //
    // AdapterView.OnItemSelectedListener overriden methods
    //
    override fun onItemSelected(spinner: AdapterView<*>, view: View, position: Int, id: Long) {
        when (spinner.id) {
            R.id.outputDevicesSpinner -> {
                val listItem = spinner.getItemAtPosition(position) as MidiDeviceListItem
                mAppMidiManager!!.openReceiveDevice(listItem.deviceInfo)
            }
            R.id.inputDevicesSpinner -> {
                val listItem = spinner.getItemAtPosition(position) as MidiDeviceListItem
                mAppMidiManager!!.openSendDevice(listItem.deviceInfo)
            }
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}

    /**
     * A class to hold MidiDevices in the list controls.
     */
    private inner class MidiDeviceListItem(val deviceInfo: MidiDeviceInfo) {

        override fun toString(): String {
            return deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)!!
        }
    }

    /**
     * Fills the specified list control with a set of MidiDevices
     * @param spinner   The list control.
     * @param devices   The set of MidiDevices.
     */
    private fun fillDeviceList(spinner: Spinner?, devices: ArrayList<MidiDeviceInfo>) {
        val listItems = ArrayList<MidiDeviceListItem>()
        for (devInfo in devices) {
            listItems.add(MidiDeviceListItem(devInfo))
        }

        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listItems
        )
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // attaching data adapter to spinner
        spinner!!.adapter = dataAdapter
    }

    /**
     * Fills the Input & Output UI device list with the current set of MidiDevices for each type.
     */
    private fun onDeviceListChange() {
        fillDeviceList(mOutputDevicesSpinner, mReceiveDevices)
        fillDeviceList(mInputDevicesSpinner, mSendDevices)
    }

    //
    // Native Interface methods
    //
    private external fun initNative()

    /**
     * Called from the native code when MIDI messages are received.
     * @param message
     */
    private fun onNativeMessageReceive(message: ByteArray) {
        // Messages are received on some other thread, so switch to the UI thread
        // before attempting to access the UI
        runOnUiThread { showReceivedMessage(message) }
    }
}