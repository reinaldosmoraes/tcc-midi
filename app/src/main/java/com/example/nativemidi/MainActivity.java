/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.example.nativemidi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;

import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.nativemidi.models.Hand;
import com.example.nativemidi.models.Tap;
import com.example.nativemidi.settings.SettingsActivity;
import com.example.nativemidi.utils.SharedPreferecesManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Application MainActivity handles UI and Midi device hotplug event from
 * native side.
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = MainActivity.class.getName();;

    private AppMidiManager mAppMidiManager;

    // Connected devices
    private ArrayList<MidiDeviceInfo> mReceiveDevices = new ArrayList<MidiDeviceInfo>();
    private ArrayList<MidiDeviceInfo> mSendDevices = new ArrayList<MidiDeviceInfo>();

    // Send Widgets
    Spinner mOutputDevicesSpinner;

    // Receive Widgets
    Spinner mInputDevicesSpinner;
    TextView mReceiveMessageTx;

    // Metronome
    ImageView mMetronomeImageView;
    TextView mMetronomeText;
    int bpm = 120;

    // Feedback
    TextView mOutputMessage;
    Button mClearTapsButton;

    // Expected Pattern
    ImageView mExpectedPatternImageView;
    TextView mExpectedPatternText;

    // Model base
    ArrayList<Tap> taps = new ArrayList();
    ArrayList<Tap> pattern = Tap.getParadiddlePattern();

    // Set pattern dialog
    TextView mHandPatternDialogText;

    // Treshold
    int accentTreshold = 0;

    // Metronome
    long startTime = 0;
    int currentTempo = 0;
    TextView metronomeCurrentTempoTextView;
    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    MediaPlayer player;
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            Log.d("METRONOME", String.format("%d:%02d", minutes, seconds));

            if(currentTempo < 4) {
                currentTempo++;
            } else {
                currentTempo = 1;
            }
            metronomeCurrentTempoTextView.setText(String.valueOf(currentTempo));

            playMetronomeSound(currentTempo == 1);

            timerHandler.postDelayed(this, bpmToMs(bpm));
        }
    };

    private void insertTapOnListWithInterval(Tap tap) {
        Tap currentTap;

        if (taps.isEmpty()) {
            currentTap = tap;
            currentTap.setInterval((long) 0);
        } else {
            Tap previousTap = taps.get(taps.size() - 1);
            currentTap = tap;
            long interval = currentTap.getTapTime().getTime() - previousTap.getTapTime().getTime();
            currentTap.setInterval(interval);
        }
        taps.add(currentTap);
    }

    private boolean verifyTap(Tap correctTap, Tap currentTap) {
        if(isCorretHand(correctTap, currentTap) && isInTime(correctTap, currentTap) && isCorretAccent(correctTap, currentTap)) {
//            icCorrectImageView.setVisibility(View.VISIBLE);
//            icIncorrectImageView.setVisibility(View.INVISIBLE);
            return true;
        } else {
//            icCorrectImageView.setVisibility(View.INVISIBLE);
//            icIncorrectImageView.setVisibility(View.VISIBLE);
            return false;
        }
    }

    private boolean isCorretHand(Tap correctTap, Tap currentTap) {
        return correctTap.getHand() == currentTap.getHand();
    }

    private boolean isCorretAccent(Tap correctTap, Tap currentTap) {
        if(currentTap.getIntensity() >= accentTreshold) {
            currentTap.setAccented(true);
        }
        return correctTap.isAccented() == currentTap.isAccented();
    }

    private boolean isInTime(Tap correctTap, Tap currentTap) {
        long interval = getDateDiff(correctTap.getTapTime(), currentTap.getTapTime(), TimeUnit.MILLISECONDS);
        return interval < 800;

        // Outro método
//        long tolerance = 1000;
//        Date lowTolerance = new Date(correctTap.getTapTime().getTime() - tolerance);
//        Date highTolerance = new Date(correctTap.getTapTime().getTime() + tolerance);
//
//        return currentTap.getTapTime().getTime() >= lowTolerance.getTime() && currentTap.getTapTime().getTime() <= highTolerance.getTime();
    }

    private long bpmToMs (int bpm) {
        long beatsPerMilliseconds = 60000 / bpm;
        return beatsPerMilliseconds;
    }

    private int msToBpm (long ms) {
        if (ms == 0) { return 0; }
        int beatsPerMinutes = 60000 / (int)ms;
        return beatsPerMinutes;
    }

    // Force to load the native library
    static {
        AppMidiManager.loadNativeAPI();
    }

    public void showBpmPickerDialog() {
        final Dialog bpmPickerDialog = new Dialog(MainActivity.this);
        bpmPickerDialog.setTitle("NumberPicker");
        bpmPickerDialog.setContentView(R.layout.bpm_picker_dialog);

        Button okButton = (Button) bpmPickerDialog.findViewById(R.id.ok_button_bpm_picker_id);
        Button cancelButton = (Button) bpmPickerDialog.findViewById(R.id.cancel_button_bpm_picker_id);

        final NumberPicker bpmNumberPicker = (NumberPicker) bpmPickerDialog.findViewById(R.id.bpm_number_picker_id);
        bpmNumberPicker.setMaxValue(200);
        bpmNumberPicker.setMinValue(60);
        bpmNumberPicker.setWrapSelectorWheel(false);
        bpmNumberPicker.setValue(bpm);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bpm = bpmNumberPicker.getValue();
                mMetronomeText.setText(String.valueOf(bpmNumberPicker.getValue()));
                bpmPickerDialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bpmPickerDialog.dismiss();
            }
        });

        bpmPickerDialog.show();
    }

    public void showSetHandPatternDialog() {
        final Dialog setHandPatternDialog = new Dialog(MainActivity.this);
        setHandPatternDialog.setTitle("SetHandPatternDialog");
        setHandPatternDialog.setContentView(R.layout.set_hand_pattern_dialog);

        final ArrayList<Tap> newPattern = new ArrayList<>();
        Button rightHandButton = setHandPatternDialog.findViewById(R.id.right_hand_button_id);
        Button leftHandButton = setHandPatternDialog.findViewById(R.id.left_hand_button_id);
        Button accentedRightHandButton = setHandPatternDialog.findViewById(R.id.accented_right_hand_button_id);
        Button accentedLeftHandButton = setHandPatternDialog.findViewById(R.id.accented_left_hand_button_id);

        mHandPatternDialogText = setHandPatternDialog.findViewById(R.id.new_expected_pattern_id);

        rightHandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandPatternDialogText.setText(mHandPatternDialogText.getText() + "R ");
                newPattern.add(new Tap(Hand.RIGHT, null, null, null, false));

                if(checkIfInputIsFinished(newPattern)) {
                    setHandPatternDialog.dismiss();
                }
            }
        });

        leftHandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandPatternDialogText.setText(mHandPatternDialogText.getText() + "L ");
                newPattern.add(new Tap(Hand.LEFT, null, null, null, false));

                if(checkIfInputIsFinished(newPattern)) {
                    setHandPatternDialog.dismiss();
                }
            }
        });

        accentedRightHandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandPatternDialogText.setText(mHandPatternDialogText.getText() + "R' ");
                newPattern.add(new Tap(Hand.RIGHT, null, null, null, true));

                if(checkIfInputIsFinished(newPattern)) {
                    setHandPatternDialog.dismiss();
                }
            }
        });

        accentedLeftHandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandPatternDialogText.setText(mHandPatternDialogText.getText() + "L ");
                newPattern.add(new Tap(Hand.LEFT, null, null, null, true));

                if(checkIfInputIsFinished(newPattern)) {
                    setHandPatternDialog.dismiss();
                }
            }
        });

        setHandPatternDialog.show();
    }

    private boolean checkIfInputIsFinished(ArrayList<Tap> newPattern) {
        if (newPattern.size() >= 8) {
            pattern = newPattern;
            mExpectedPatternText.setText(getHandPatternText(pattern));
            taps.clear();
            return true;
        }
        return false;
    }

    private String getHandPatternText(ArrayList<Tap> taps) {
        String pattern = "";
        for (Tap tap : taps) {

            if (tap.getHand() == Hand.RIGHT && tap.isAccented()) {
                pattern += "R'      ";
            } else if (tap.getHand() == Hand.RIGHT && !tap.isAccented()) {
                pattern += "R      ";
            } else if (tap.getHand() == Hand.LEFT && tap.isAccented()) {
                pattern += "L'      ";
            } else {
                pattern += "L      ";
            }
        }
        return pattern;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configureToolbar();
        initNative();

        mOutputDevicesSpinner = (Spinner)findViewById(R.id.outputDevicesSpinner);
        mOutputDevicesSpinner.setOnItemSelectedListener(this);

        mInputDevicesSpinner = (Spinner)findViewById(R.id.inputDevicesSpinner);
        mInputDevicesSpinner.setOnItemSelectedListener(this);

        mReceiveMessageTx = (TextView)findViewById(R.id.receiveMessageTx);
        mOutputMessage = findViewById(R.id.outputMessage);
        mClearTapsButton = findViewById(R.id.clearTapsButton);

        metronomeCurrentTempoTextView = findViewById(R.id.metrnomeCurrentTempoTextView);
        mClearTapsButton.setText("Iniciar");
        mClearTapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClearTapsButton.getText().equals("Pausar")) {
                    taps.clear();
                    mOutputMessage.setText("");
                    timerHandler.removeCallbacks(timerRunnable);
                    mClearTapsButton.setText("Iniciar");
                    metronomeCurrentTempoTextView.setVisibility(View.INVISIBLE);
                    currentTempo = 0;
                } else {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    mClearTapsButton.setText("Pausar");
                    metronomeCurrentTempoTextView.setVisibility(View.VISIBLE);
                    calculateCorrectExecution();
                }
            }
        });

        mExpectedPatternText = findViewById(R.id.expectedPatternText);
        mExpectedPatternImageView = findViewById(R.id.expectedPatternImageView);
        mExpectedPatternImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetHandPatternDialog();
            }
        });

        mMetronomeImageView = findViewById(R.id.metronomeImageView);
        mMetronomeText = findViewById(R.id.metronomeTextView);
        mExpectedPatternText.setText(getHandPatternText(pattern));
        mMetronomeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBpmPickerDialog();
            }
        });

        MidiManager midiManager = (MidiManager) getSystemService(Context.MIDI_SERVICE);
        midiManager.registerDeviceCallback(new MidiDeviceCallback(), new Handler());

        //
        // Setup the MIDI interface
        //
        mAppMidiManager = new AppMidiManager(midiManager);

        // Initial Scan
        ScanMidiDevices();
        
    }

    private void calculateCorrectExecution() {
        for (int i = 0; i < pattern.size(); i++) {
            Tap tap = pattern.get(i);

            if (i == 0) {
                tap.setInterval((long) 0);
                tap.setTapTime(new Date(startTime + (bpmToMs(bpm) * 4)));
            } else {
                tap.setInterval(bpmToMs(bpm));
                tap.setTapTime(new Date(pattern.get(i - 1).getTapTime().getTime() + bpmToMs(bpm)));
            }
        }
    }

    private void playMetronomeSound(boolean isHighSound) {
        if (isHighSound) {
            player = MediaPlayer.create(this, R.raw.tap_high);
        } else {
            player = MediaPlayer.create(this, R.raw.tap_low);
        }
        player.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        mClearTapsButton.setText("Iniciar");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccentTresholdFromSharedPrefences();
    }

    /**
     * Device Scanning
     * Methods are called by the system whenever the set of attached devices changes.
     */
    private class MidiDeviceCallback extends MidiManager.DeviceCallback {
        @Override
        public void onDeviceAdded(MidiDeviceInfo device) {
            ScanMidiDevices();
        }

        @Override
        public void onDeviceRemoved(MidiDeviceInfo device) {
            ScanMidiDevices();
        }
    }

    /**
     * Scans and gathers the list of connected physical devices,
     * then calls onDeviceListChange() to update the UI. This has the
     * side-effect of causing a list item to be selected, which then
     * invokes the listener logic which connects the device(s).
     */
    private void ScanMidiDevices() {
        mAppMidiManager.ScanMidiDevices(mSendDevices, mReceiveDevices);
        onDeviceListChange();
    }

    //
    // UI Helpers
    //
    /**
     * Formats a set of MIDI message bytes into a user-readable form.
     * @param message   The bytes comprising a Midi message.
     */
    private void showReceivedMessage(byte[] message) {
        switch ((message[0] & 0xF0) >> 4) {
            case MidiSpec.MIDICODE_NOTEON:
                mReceiveMessageTx.setText(
                        "NOTE_ON [ch:" + (message[0] & 0x0F) +
                                " key:" + message[1] +
                                " vel:" + message[2] + "]");
                break;

            case MidiSpec.MIDICODE_NOTEOFF:
                Hand hand = message[1] == 48 ? Hand.LEFT : Hand.RIGHT;
                Tap tap = new Tap(hand, (int) message[2], new Date(), null, null);
                insertTapOnListWithInterval(tap);

                int currentTapPosition = (taps.size() - 1) % 8;

                String feedbackMessage = "Lado: " + hand.toString() +
                        "\nIntensidade: " + message[2] +
                        "\nIntervalo: " + msToBpm(taps.get(taps.size() - 1).getInterval()) + " bpm" +
                        "\nTempo correto: " + isInTime(pattern.get(currentTapPosition), taps.get(currentTapPosition)) +
                        "\nToque correto: " + verifyTap(pattern.get(currentTapPosition), taps.get(currentTapPosition));

                mOutputMessage.setText(feedbackMessage);

                break;
        }

    }

    //
    // AdapterView.OnItemSelectedListener overriden methods
    //
    @Override
    public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
        switch (spinner.getId()) {
        case R.id.outputDevicesSpinner: {
                MidiDeviceListItem listItem = (MidiDeviceListItem) spinner.getItemAtPosition(position);
                mAppMidiManager.openReceiveDevice(listItem.getDeviceInfo());
            }
            break;

        case R.id.inputDevicesSpinner: {
                MidiDeviceListItem listItem = (MidiDeviceListItem)spinner.getItemAtPosition(position);
                mAppMidiManager.openSendDevice(listItem.getDeviceInfo());
            }
            break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    /**
     * A class to hold MidiDevices in the list controls.
     */
    private class MidiDeviceListItem {
        private MidiDeviceInfo mDeviceInfo;

        public MidiDeviceListItem(MidiDeviceInfo deviceInfo) {
            mDeviceInfo = deviceInfo;
        }

        public MidiDeviceInfo getDeviceInfo() { return mDeviceInfo; }

        @Override
        public String toString() {
            return mDeviceInfo.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
        }
    }

    /**
     * Fills the specified list control with a set of MidiDevices
     * @param spinner   The list control.
     * @param devices   The set of MidiDevices.
     */
    private void fillDeviceList(Spinner spinner, ArrayList<MidiDeviceInfo> devices) {
        ArrayList<MidiDeviceListItem> listItems = new ArrayList<MidiDeviceListItem>();
        for(MidiDeviceInfo devInfo : devices) {
            listItems.add(new MidiDeviceListItem(devInfo));
        }

        // Creating adapter for spinner
        ArrayAdapter<MidiDeviceListItem> dataAdapter =
                new  ArrayAdapter<MidiDeviceListItem>(this,
                        android.R.layout.simple_spinner_item,
                        listItems);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
    }

    /**
     * Fills the Input & Output UI device list with the current set of MidiDevices for each type.
     */
    private void onDeviceListChange() {
        fillDeviceList(mOutputDevicesSpinner, mReceiveDevices);
        fillDeviceList(mInputDevicesSpinner, mSendDevices);
    }

    //
    // Native Interface methods
    //
    private native void initNative();

    /**
     * Called from the native code when MIDI messages are received.
     * @param message
     */
    private void onNativeMessageReceive(final byte[] message) {
        // Messages are received on some other thread, so switch to the UI thread
        // before attempting to access the UI
        runOnUiThread(new Runnable() {
            public void run() {
                showReceivedMessage(message);
            }
        });
    }

    /* Toolbar */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.practice_section_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:return super.onOptionsItemSelected(item);
        }
    }

    private void configureToolbar() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
    }

    private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    private void updateAccentTresholdFromSharedPrefences() {
        accentTreshold = SharedPreferecesManager.Companion.getAccentTreshold(getApplicationContext());
    }
}
