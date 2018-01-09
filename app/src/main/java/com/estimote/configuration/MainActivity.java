package com.estimote.configuration;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.connection.scanner.ConfigurableDevicesScanner;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_RESULT_ITEM_DEVICE = "com.estimote.configuration.SCAN_RESULT_ITEM_DEVICE";
    private Button connectButton;
    private ProgressBar spinner;

    private ConfigurableDevicesScanner devicesScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        devicesScanner = new ConfigurableDevicesScanner(this);

        spinner = (ProgressBar)findViewById(R.id.progressBar);
        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectButton.setEnabled(false);
                spinner.setVisibility(View.VISIBLE);
                connectToBeacon();
            }
        });
    }

    void connectToBeacon() {
        if (SystemRequirementsChecker.checkWithDefaultDialogs(this)) {
            devicesScanner.scanForDevices(new ConfigurableDevicesScanner.ScannerCallback() {
                @Override
                public void onDevicesFound(List<ConfigurableDevicesScanner.ScanResultItem> list) {
                    String deviceIdentifier = "[8222e7d7ede31af84798e4b6566d592c]";
                    for (ConfigurableDevicesScanner.ScanResultItem item : list) {
                        if (item.device.deviceId.toString().equals(deviceIdentifier)) {
                            devicesScanner.stopScanning();
                            Intent intent = new Intent(MainActivity.this, GameActivity.class);
                            intent.putExtra(EXTRA_SCAN_RESULT_ITEM_DEVICE, item.device);
                            spinner.setVisibility(View.GONE);
                            startActivity(intent);
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectButton.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        devicesScanner.stopScanning();
        spinner.setVisibility(View.GONE);
    }
}