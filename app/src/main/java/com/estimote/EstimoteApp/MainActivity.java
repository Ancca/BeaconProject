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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.estimote.sdk.DeviceId;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.connection.scanner.ConfigurableDevicesScanner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "APP_LOG";
    public static final String EXTRA_SCAN_RESULT_ITEM_DEVICE = "com.estimote.configuration.SCAN_RESULT_ITEM_DEVICE";
    private Button scanButton;
    private ProgressBar spinner;
    final Context context = this;
    int durationShort = Toast.LENGTH_SHORT;
    int durationLong = Toast.LENGTH_LONG;
    String previousBeaconId = "";
    String targetBeaconId = "";
    String filename = "beacon_connection_id";
    private LinearLayout layout;
    int luku = 0;
    String buttonText = "";
    List<String> availableBeacons = new ArrayList<String>();
    ArrayList<Button> beaconButtons = new ArrayList<Button>();

    private ConfigurableDevicesScanner devicesScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        devicesScanner = new ConfigurableDevicesScanner(this);
        layout = (LinearLayout) findViewById(R.id.layout);
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        scanButton = (Button) findViewById(R.id.scanButton);

        nfcConnect();
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spinner.setVisibility(View.VISIBLE);
                scanButton.setEnabled(false);
                for(int i = 0; i < beaconButtons.size(); i++){
                    layout.removeView(beaconButtons.get(i));
                }
                devicesScanner.scanForDevices(new ConfigurableDevicesScanner.ScannerCallback() {
                    @Override
                    public void onDevicesFound(final List<ConfigurableDevicesScanner.ScanResultItem> list) {
                        for (ConfigurableDevicesScanner.ScanResultItem item : list) {
                            Log.d(TAG,item.device.deviceId.toString());
                            availableBeacons.add(item.device.deviceId.toString());
                        }
                        devicesScanner.stopScanning();
                        spinner.setVisibility(View.GONE);
                        scanButton.setEnabled(true);

                        for(int i = 0; i < list.size(); i++){
                            luku = i;
                            final Button button = new Button(context);
                            button.setText(availableBeacons.get(luku));
                            button.setId(i);
                            button.setSingleLine();
                            button.setLayoutParams(new LinearLayout.LayoutParams(850, 150));
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    buttonText = button.getText().toString();
                                    Log.d(TAG,"Pressed the button for + " + buttonText);
                                    for (ConfigurableDevicesScanner.ScanResultItem item : list) {
                                        if (item.device.deviceId.toString().equals(buttonText)) {
                                            Intent intent = new Intent(MainActivity.this, GameActivity.class);
                                            intent.putExtra(EXTRA_SCAN_RESULT_ITEM_DEVICE, list.get(button.getId()).device);
                                            spinner.setVisibility(View.GONE);
                                            startActivity(intent);
                                        }
                                    }
                                }
                            });
                            beaconButtons.add(button);
                            //optional: add your buttons to any layout if you want to see them in your screen
                            layout.addView(button);
                        }
                    }
                });
            }
        });
    }

    void nfcConnect() {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                for (int i = 0; i < rawMsgs.length; i++) {
                    NdefMessage msg = (NdefMessage) rawMsgs[i];
                    DeviceId beaconId = findBeaconId(msg);
                    if (beaconId != null) {
                        targetBeaconId = beaconId.toString();
                        connectToBeacon();
                    }
                }
            }
        }
    }

    void connectToBeacon() {
        if (SystemRequirementsChecker.checkWithDefaultDialogs(this)) {
            spinner.setVisibility(View.VISIBLE);
            scanButton.setEnabled(false);
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
                            scanButton.setEnabled(true);
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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        devicesScanner.stopScanning();
    }
}