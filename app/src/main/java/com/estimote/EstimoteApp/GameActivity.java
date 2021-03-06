package com.estimote.EstimoteApp;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;
import com.estimote.sdk.connection.DeviceConnection;
import com.estimote.sdk.connection.DeviceConnectionCallback;
import com.estimote.sdk.connection.DeviceConnectionProvider;
import com.estimote.sdk.connection.exceptions.DeviceConnectionException;
import com.estimote.sdk.connection.scanner.ConfigurableDevice;
import com.estimote.sdk.connection.settings.storage.StorageManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "APP_LOG";
    final Context context = this;

    private ConfigurableDevice configurableDevice;
    private DeviceConnection connection;
    private DeviceConnectionProvider connectionProvider;

    TextView playerName, playerScore, beaconName, beaconScore;
    Button rockButton, paperButton, scissorsButton, scoreButton;
    ProgressBar spinner;

    String file_data = "beacon_";

    String score = "";
    String name = "";
    String version = "";
    String connectedBeaconGame = "";
    String connectedBeaconID = "";
    String connectedBeaconVersion = "";
    String connectedBeaconGameK = "";
    String connectedBeaconIDK = "";
    String connectedBeaconVersionK = "";
    String bName = "Beacon";
    String bScore = "0-0-0";
    String data = "";
    int playerID;
    int playerSelection, beaconSelection = 0;
    String pWin,pLose,pTie,bWin,bLose,bTie = "";
    int durationShort = Toast.LENGTH_SHORT;
    int durationLong = Toast.LENGTH_LONG;
    Map<String, String> beaconDataMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "running onCreate");
        setContentView(R.layout.activity_game);
        Intent intent = getIntent();
        configurableDevice = intent.getParcelableExtra(MainActivity.EXTRA_SCAN_RESULT_ITEM_DEVICE);
        beaconName = (TextView) findViewById(R.id.beaconName);
        beaconScore = (TextView) findViewById(R.id.beaconScore);
        playerName = (TextView) findViewById(R.id.playerName);
        playerScore = (TextView) findViewById(R.id.playerScore);
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        connectionProvider = new DeviceConnectionProvider(this);
        connectToDevice();

        // Button to open the list for all players and their scores
        scoreButton = (Button) findViewById(R.id.scoreButton);
        scoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "running scoreButton");
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.scorelist, null);
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);
                alertDialogBuilder.setView(promptsView);
                final TextView scoreViewSubtitle = (TextView) promptsView.findViewById(R.id.scoreViewSubtitle);
                final TextView scoreView = (TextView) promptsView.findViewById(R.id.scoreView);

                String subtitle = beaconDataMap.values().toArray()[1].toString() + " (v." + beaconDataMap.values().toArray()[2].toString() + ")";
                addText(scoreViewSubtitle,subtitle);

                String mapKey;
                String mapValue;
                scoreView.setMovementMethod(new ScrollingMovementMethod());
                for (int mapID = 3; mapID+1 <= beaconDataMap.size(); mapID++) {
                    mapKey = beaconDataMap.keySet().toArray()[mapID].toString();
                    mapValue = beaconDataMap.values().toArray()[mapID].toString();
                    String[] tokens = mapValue.split("-");
                    String win = tokens[0];
                    String lose = tokens[1];
                    String tie = tokens[2];
                    String scoreString = win + "W " + lose + "L " + tie + "T";
                    Log.d(TAG, mapID + " " + mapKey + " " + scoreString);
                    String string = mapKey + " " + scoreString + "\n";
                    addText(scoreView,string);
                }

                alertDialogBuilder
                        .setCancelable(true)
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });
                final AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        // Button for rock
        rockButton = (Button) findViewById(R.id.rockButton);
        rockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "running rockButton");
                playerSelection = 1;
                Log.d(TAG,"Player selected rock");
                randomSelection();
                disableButtons();
                enableSpinner();
            }
        });

        // Button for paper
        paperButton = (Button) findViewById(R.id.paperButton);
        paperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "running paperButton");
                playerSelection = 2;
                Log.d(TAG, "Player selected paper");
                randomSelection();
                disableButtons();
                enableSpinner();
            }
        });

        // Button for scissors
        scissorsButton = (Button) findViewById(R.id.scissorsButton);
        scissorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "running scissorsButton");
                playerSelection = 3;
                Log.d(TAG, "Player selected scissors");
                randomSelection();
                disableButtons();
                enableSpinner();
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "running onResume");
        super.onResume();
        connectToDevice();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "running onStop");
        super.onStop();
        disableButtons();
        if (connection != null && connection.isConnected())
            connection.close();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "running onPause");
        super.onPause();
        disableButtons();
        if (connection != null && connection.isConnected())
            connection.close();
    }

    private void setText(final TextView text, final String value) {
        Log.d(TAG, "running setText");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    private void addText(final TextView text, final String value) {
        Log.d(TAG, "running addText");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append(value);
            }
        });
    }

    private void disableButtons() {
        Log.d(TAG, "running disableButtons");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Disable all buttons
                rockButton.setEnabled(false);
                paperButton.setEnabled(false);
                scissorsButton.setEnabled(false);
                scoreButton.setEnabled(false);
            }
        });
    }

    private void enableButtons() {
        Log.d(TAG, "running enableButtons");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Enable all buttons
                rockButton.setEnabled(true);
                paperButton.setEnabled(true);
                scissorsButton.setEnabled(true);
                scoreButton.setEnabled(true);
            }
        });
    }

    private void enableSpinner() {
        Log.d(TAG, "running enableSpinner");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void disableSpinner() {
        Log.d(TAG, "running disableSpinner");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private void displayMessage(final String message, final Integer duration) {
        Log.d(TAG, "running displayMessage");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context,message,duration);
                toast.show();
            }
        });
    }

    private void randomSelection() {
        Log.d(TAG, "running randomSelection");
        // randomSelection() is used to generate a random selection (1/2/3 | rock/paper/scissors) for the beacon
        String selectionString = "";
        beaconSelection = ((int) (Math.random()*(4 - 1))) + 1;
        switch (beaconSelection) {
            case 1: selectionString = "Beacon selected rock";
                break;
            case 2: selectionString = "Beacon selected paper";
                break;
            case 3: selectionString = "Beacon selected scissors";
                break;
        }
        Log.d(TAG,selectionString);
        testForWinner();
    }

    private void testForWinner() {
        Log.d(TAG, "running testForWinner");
        // testForWinner() is used to test whether the player or the beacon wins
        if (playerSelection == beaconSelection) {
            Log.d(TAG, "It's a tie");
            Toast toast = Toast.makeText(context,"It's a tie",durationShort);
            toast.show();
            addScore(name,"tie");
        }
        else if (playerSelection == 1) {
            if (beaconSelection == 2) {
                Log.d(TAG, "Beacon wins");
                Toast toast = Toast.makeText(context,"Beacon wins",durationShort);
                toast.show();
                addScore(name,"lose");
            }
            else if (beaconSelection == 3) {
                Log.d(TAG, "Player wins");
                Toast toast = Toast.makeText(context,"Player wins",durationShort);
                toast.show();
                addScore(name,"win");

            }
        }
        else if (playerSelection == 2) {
            if (beaconSelection == 1) {
                Log.d(TAG, "Player wins");
                Toast toast = Toast.makeText(context,"Player wins",durationShort);
                toast.show();
                addScore(name,"win");

            }
            else if (beaconSelection == 3) {
                Log.d(TAG, "Beacon wins");
                Toast toast = Toast.makeText(context,"Beacon wins",durationShort);
                toast.show();
                addScore(name,"lose");

            }
        }
        else if (playerSelection == 3) {
            if (beaconSelection == 1) {
                Log.d(TAG, "Beacon wins");
                Toast toast = Toast.makeText(context,"Beacon wins",durationShort);
                toast.show();
                addScore(name,"lose");

            }
            else if (beaconSelection == 2) {
                Log.d(TAG,"Player wins");
                Toast toast = Toast.makeText(context,"Player wins",durationShort);
                toast.show();
                addScore(name,"win");
            }
        }
    }

    private void addScore(final String target, final String scoreName) {
        Log.d(TAG, "running addScore");
        if (connection.isConnected()) {
            Log.d(TAG, "First part of adding score");
            String mapKey = "";
            String mapValue = "";
            int mapID;
            for (mapID = 0; mapID+1 <= beaconDataMap.size(); mapID++) {
                mapKey = beaconDataMap.keySet().toArray()[mapID].toString();
                mapValue = beaconDataMap.values().toArray()[mapID].toString();
                if (mapKey.equals(bName)) {
                    String[] tokens = mapValue.split("-");
                    String win = tokens[0];
                    String lose = tokens[1];
                    String tie = tokens[2];
                    if (scoreName.equals("win")) {
                        Log.d(TAG, "Adding a loss (b)");
                        int i = Integer.parseInt(lose);
                        i++;
                        lose = Integer.toString(i);
                    }
                    else if (scoreName.equals("lose")) {
                        Log.d(TAG, "Adding a win (b)");
                        int i = Integer.parseInt(win);
                        i++;
                        win = Integer.toString(i);
                    }
                    else if (scoreName.equals("tie")) {
                        Log.d(TAG, "Adding a tie (b)");
                        int i = Integer.parseInt(tie);
                        i++;
                        tie = Integer.toString(i);
                    }
                    mapValue = win + "-" + lose + "-" + tie;
                    bScore = mapValue;
                    beaconDataMap.put(mapKey,mapValue);
                }
                else if (mapKey.equals(target)) {
                    Log.d(TAG, "Second part of adding score");
                    String[] tokens = mapValue.split("-");
                    String win = tokens[0];
                    String lose = tokens[1];
                    String tie = tokens[2];
                    if (scoreName.equals("win")) {
                        Log.d(TAG, "Adding a win");
                        int i = Integer.parseInt(win);
                        i++;
                        win = Integer.toString(i);
                    }
                    else if (scoreName.equals("lose")) {
                        Log.d(TAG, "Adding a loss");
                        int i = Integer.parseInt(lose);
                        i++;
                        lose = Integer.toString(i);
                    }
                    else if (scoreName.equals("tie")) {
                        Log.d(TAG, "Adding a tie");
                        int i = Integer.parseInt(tie);
                        i++;
                        tie = Integer.toString(i);
                    }
                    mapValue = win + "-" + lose + "-" + tie;
                    score = mapValue;
                    beaconDataMap.put(mapKey,mapValue);

                    Log.d(TAG, mapKey + " " + mapValue);
                    Log.d(TAG, "Writing new data");
                    connection.settings.storage.writeStorage(beaconDataMap, new StorageManager.WriteCallback() {
                        @Override
                        public void onSuccess() {
                            setData();
                        }

                        @Override
                        public void onFailure(DeviceConnectionException e) {
                            e.printStackTrace();

                        }
                    });
                }
            }
        }
    }

    private void getData() {
        // getData() is used to fetch the data of the beacon and the user (scores)
        Log.d(TAG, "running getData");
        connection.settings.storage.readStorage(new StorageManager.ReadCallback() {
            @Override
            public void onSuccess(Map<String, String> map) {
                beaconDataMap = map;
                Log.d(TAG,"Data been read");
                String mapKey = "";
                String mapValue = "";
                int mapID;
                for (mapID = 0; mapID+1 <= beaconDataMap.size(); mapID++) {
                    mapKey = beaconDataMap.keySet().toArray()[mapID].toString();
                    mapValue = beaconDataMap.values().toArray()[mapID].toString();
                    Log.d(TAG,"Map key: " + mapKey + " Map value: " + mapValue);
                    if (mapID == 3) {
                        // MapID == 3 is the line for beacon's own game data
                        bName = mapKey;
                        bScore = mapValue;
                    }
                    else if (mapKey.equals(name)) {
                        playerID = mapID;
                        score = mapValue;
                    }
                }
                setData();
                enableButtons();
                disableSpinner();
                Log.d(TAG,name);
            }

            @Override
            public void onFailure(DeviceConnectionException e) {
                e.printStackTrace();
            }
        });
    }


    private void setData() {
        Log.d(TAG, "running setData");
        // Set beacon data (display it)
        String[] tokens = bScore.split("-");
        bWin = tokens[0];
        bLose = tokens[1];
        bTie = tokens[2];
        setText(beaconName, bName + " ");
        setText(beaconScore,bWin + "W " + bLose + "L " + bTie + "T ");

        // Set player data (display it)
        String[] tokens2 = score.split("-");
        pWin = tokens2[0];
        pLose = tokens2[1];
        pTie = tokens2[2];
        setText(playerName,name + " ");
        setText(playerScore,pWin + "W " + pLose + "L " + pTie + "T ");

        enableButtons();
        disableSpinner();
    }

    private void createPlayer() {
        Log.d(TAG,"Player name was not found");
        Log.d(TAG,"running createPlayer");
        // Name was not found, so a window is created for asking the user's name
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.nameregisterprompt, null);
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);
                alertDialogBuilder.setView(promptsView);
                final EditText userInput = (EditText) promptsView.findViewById(R.id.editText1);
                alertDialogBuilder
                        .setCancelable(false);
                final AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                final Button okButton = (Button) promptsView.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Check the name user has given
                        name = userInput.getText().toString();
                        if (name.isEmpty() || name.contains(" ") || name.length() > 15) {
                            Log.d(TAG, "Name cannot be empty, contain spaces or be longer than 15 characters");
                            displayMessage("Name cannot be empty, contain spaces or be longer than 15 characters", durationLong);
                        }
                        // If name was acceptable, it will be compared to the other names saved on the beacon to make sure that there are no duplicates
                        else {
                            // Check that there is a connection to the beacon
                            if (beaconDataMap.containsKey(name)) {
                                // Notify the user that the name has already been chosen by someone else
                                Log.d(TAG, "Name has already been chosen");
                                displayMessage("Name has already been chosen", durationShort);
                            }
                            // If the name is OK, new file will be created for the name and the name (and score 0-0-0) will be saved on the beacon
                            else {
                                FileOutputStream outputStream;
                                try {
                                    outputStream = openFileOutput(file_data, Context.MODE_PRIVATE);
                                    String data2 = connectedBeaconVersion + " " + name;
                                    Log.d(TAG,data2);
                                    outputStream.write(data2.getBytes());
                                    outputStream.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (connection.isConnected()) {
                                    beaconDataMap.put(name, "0-0-0");
                                    connection.settings.storage.writeStorage(beaconDataMap, new StorageManager.WriteCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Data write was a success");
                                            getData();
                                        }

                                        @Override
                                        public void onFailure(DeviceConnectionException e) {
                                            Log.d(TAG, "Data write was a failure : " + e.getLocalizedMessage());
                                        }
                                    });
                                }
                                alertDialog.cancel();
                            }
                        }
                    }
                });
            }
        });

    }

    private void connectToDevice() {
        Log.d(TAG, "running connectToDevice");
        enableSpinner();
        // Check if app is already connected to the beacon
        if (connection == null || !connection.isConnected()) {
            connectionProvider.connectToService(new DeviceConnectionProvider.ConnectionProviderCallback() {
                @Override
                public void onConnectedToService() {
                    // Connect to the selected beacon (configurableDevice)
                    connection = connectionProvider.getConnection(configurableDevice);
                    connection.connect(new DeviceConnectionCallback() {
                        @Override
                        public void onConnected() {
                            Log.d(TAG, "Connected");
                            displayMessage("Connected",durationShort);
                            Log.d(TAG,configurableDevice.deviceId.toString());
                            connection.settings.storage.readStorage(new StorageManager.ReadCallback() {
                                @Override
                                public void onSuccess(Map<String, String> map) {
                                    beaconDataMap = map;
                                    if (beaconDataMap.isEmpty()){
                                        Log.d(TAG,"Beacon has no data");
                                        displayMessage("Beacon is not used for the game",durationLong);
                                    }
                                    else {
                                        try {
                                            connectedBeaconGame = beaconDataMap.values().toArray()[0].toString();
                                            connectedBeaconID = beaconDataMap.values().toArray()[1].toString();
                                            connectedBeaconVersion = beaconDataMap.values().toArray()[2].toString();
                                            connectedBeaconGameK = beaconDataMap.keySet().toArray()[0].toString();
                                            connectedBeaconIDK = beaconDataMap.keySet().toArray()[1].toString();
                                            connectedBeaconVersionK = beaconDataMap.keySet().toArray()[2].toString();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        Log.d(TAG,"Data has been read");
                                        if (connectedBeaconGameK.equals("BeaconGame") && connectedBeaconIDK.equals("BeaconId") && connectedBeaconVersionK.equals("BeaconVersion")){
                                            if (connectedBeaconGame.equals("true")){
                                                FileInputStream inputStream;
                                                int i;
                                                // Read the file for the specific beacon
                                                file_data = file_data+connectedBeaconID;
                                                try {
                                                    inputStream = openFileInput(file_data);
                                                    StringBuffer sb = new StringBuffer(100);
                                                    while ((i = inputStream.read())!= -1) {
                                                        data = sb.append((char)i).toString();
                                                    }
                                                    inputStream.close();
                                                    String[] data2 = data.split("\\s+");
                                                    version = data2[0];
                                                    name = data2[1];
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                // If player hasn't chosen a name yet, data will not be fetched until the player has created a name
                                                if (name.isEmpty() || !version.equals(connectedBeaconVersion)) {
                                                    createPlayer();
                                                }
                                                else {
                                                    getData();
                                                }
                                            }
                                            else {
                                                displayMessage("Beacon game is disabled for this beacon",durationLong);
                                                spinner.setVisibility(View.GONE);
                                            }
                                        }
                                        else {
                                            displayMessage("This beacon does not support the beacon game",durationLong);
                                            spinner.setVisibility(View.GONE);
                                        }
                                    }
                                }
                                @Override
                                public void onFailure(DeviceConnectionException e) {
                                    e.printStackTrace();
                                }
                            });
                        }

                        @Override
                        public void onDisconnected() {
                            Log.d(TAG, "Disconnected");
                            displayMessage("Disconnected",durationShort);
                        }

                        @Override
                        public void onConnectionFailed(DeviceConnectionException e) {
                            Log.d(TAG, "Connection failed : " + e.getMessage());
                            displayMessage("Connection failed, try again", durationShort);
                        }
                    });
                }
            });
        }
    }
}