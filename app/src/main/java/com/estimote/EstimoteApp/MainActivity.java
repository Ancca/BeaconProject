package com.estimote.EstimoteApp;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.estimote.sdk.DeviceId;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.connection.scanner.ConfigurableDevicesScanner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "APP_LOG";
    public static final String EXTRA_SCAN_RESULT_ITEM_DEVICE = "com.estimote.configuration.SCAN_RESULT_ITEM_DEVICE";
    private Button connectButton;
    private ProgressBar spinner;
    final Context context = this;
    int durationShort = Toast.LENGTH_SHORT;
    int durationLong = Toast.LENGTH_LONG;
    String previousBeaconId = "";
    String targetBeaconId = "";
    String filename = "beacon_id";

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
                if (previousBeaconId.isEmpty()) {
                    displayMessage("You're required to connect to a beacon using NFC before you can connect to it using this button", durationLong);
                }
                else {
                    disableButtons();
                    spinner.setVisibility(View.VISIBLE);
                    targetBeaconId = previousBeaconId;
                    connectToBeacon();
                }
            }
        });

        enableButtons();
        getBeaconIdFromFile();
        nfcConnect();
    }

    void getBeaconIdFromFile() {
        FileInputStream inputStream;
        int i;
        StringBuffer sb = new StringBuffer(34);
        // Try to open the file "beacon_id"
        // If the file is found, read the file for the beacon's id
        try {
            inputStream = openFileInput(filename);
            while ((i = inputStream.read())!= -1) {
                previousBeaconId = sb.append((char)i).toString();
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void nfcConnect() {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                for (int i = 0; i < rawMsgs.length; i++) {
                    NdefMessage msg = (NdefMessage) rawMsgs[i];
                    DeviceId beaconId = findBeaconId(msg);
                    if (beaconId != null) {
                        disableButtons();
                        spinner.setVisibility(View.VISIBLE);
                        targetBeaconId = beaconId.toString();
                        connectToBeacon();
                    }
                }
            }
        }
    }

    void connectToBeacon() {
        if (SystemRequirementsChecker.checkWithDefaultDialogs(this)) {
            devicesScanner.scanForDevices(new ConfigurableDevicesScanner.ScannerCallback() {
                @Override
                public void onDevicesFound(List<ConfigurableDevicesScanner.ScanResultItem> list) {
                    for (ConfigurableDevicesScanner.ScanResultItem item : list) {
                        if (item.device.deviceId.toString().equals(targetBeaconId)) {
                            devicesScanner.stopScanning();
                            FileOutputStream outputStream;
                            try {
                                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                                outputStream.write(targetBeaconId.getBytes());
                                outputStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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

    private static DeviceId findBeaconId(NdefMessage msg) {
        NdefRecord[] records = msg.getRecords();
        for (NdefRecord record : records) {
            if (record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE) {
                String type = new String(record.getType(), Charset.forName("ascii"));
                if ("estimote.com:id".equals(type)) {
                    return DeviceId.fromBytes(record.getPayload());
                }
            }
        }
        return null;
    }

    private void displayMessage(final String message, final Integer duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context,message,duration);
                toast.show();
            }
        });
    }

    private void enableButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Enable all buttons
                connectButton.setEnabled(true);
            }
        });
    }

    private void disableButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Enable all buttons
                connectButton.setEnabled(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        devicesScanner.stopScanning();
        spinner.setVisibility(View.GONE);
        disableButtons();
    }
}