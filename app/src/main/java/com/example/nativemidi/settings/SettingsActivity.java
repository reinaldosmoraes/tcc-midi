package com.example.nativemidi.settings;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nativemidi.R;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar accentTresholdSeekBar;
    private TextView tresholdSelectedValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        accentTresholdSeekBar = findViewById(R.id.accentTresholdSeekBar);
        tresholdSelectedValue = findViewById(R.id.tresholdSelectedValue);

        tresholdSelectedValue.setText("Treshold: " + 20 + " / " + accentTresholdSeekBar.getMax());

        accentTresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tresholdSelectedValue.setText("Treshold: " + progress + " / " + seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

}
