package com.estimote.EstimoteApp;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    Button resetButton, rockButton, paperButton, scissorsButton, scoreButton;
    ProgressBar spinner;

    String score = "";
    String name = "";
    String bName = "Beacon";
    String bScore = "0-0-0";
    int playerSelection, beaconSelection = 0;
    String pWin,pLose,pTie,bWin,bLose,bTie = "";
    int durationShort = Toast.LENGTH_SHORT;
    int durationLong = Toast.LENGTH_LONG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate...");
        setContentView(R.layout.activity_game);
        Intent intent = getIntent();
        configurableDevice = intent.getParcelableExtra(MainActivity.EXTRA_SCAN_RESULT_ITEM_DEVICE);
        beaconName = (TextView) findViewById(R.id.beaconName);
        beaconScore = (TextView) findViewById(R.id.beaconScore);
        playerName = (TextView) findViewById(R.id.playerName);
        playerScore = (TextView) findViewById(R.id.playerScore);
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        enableSpinner();
        connectionProvider = new DeviceConnectionProvider(this);
        connectToDevice();

        // Button for resetting the beacon data
        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connection.isConnected()){
                    bScore = "0-0-0";
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put(bName,bScore);
                    connection.settings.storage.writeStorage(map, new StorageManager.WriteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Data write was a success");
                        }

                        @Override
                        public void onFailure(DeviceConnectionException e) {
                            Log.d(TAG,"Data write was a failure: " + e.getLocalizedMessage());
                        }
                    });
                }
            }
        });

        // Button to open the list for all players and their scores
        scoreButton = (Button) findViewById(R.id.scoreButton);
        scoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.scorelist, null);
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);
                alertDialogBuilder.setView(promptsView);
                final TextView scoreViewTitle = (TextView) promptsView.findViewById(R.id.scoreViewTitle);
                final TextView scoreView = (TextView) promptsView.findViewById(R.id.scoreView);

                if (connection.isConnected()) {
                    connection.settings.storage.readStorage(new StorageManager.ReadCallback() {
                        @Override
                        public void onSuccess(Map<String, String> map) {
                            String mapKey = "";
                            String mapValue = "";
                            for (int mapID = 0; mapID+1 <= map.size(); mapID++) {
                                mapKey = map.keySet().toArray()[mapID].toString();
                                mapValue = map.values().toArray()[mapID].toString();
                                Log.d(TAG, mapID + mapKey + " " + mapValue);
                                String string = mapKey + " " + mapValue + "\n";
                                addText(scoreView,string);
                            }
                        }

                        @Override
                        public void onFailure(DeviceConnectionException e) {

                        }
                    });
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
        super.onResume();
        connectToDevice();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (connection != null && connection.isConnected())
            connection.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connection != null && connection.isConnected())
            connection.close();
    }

    private void setText(final TextView text, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    private void addText(final TextView text, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append(value);
            }
        });
    }

    private void disableButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Disable all buttons
                resetButton.setEnabled(false);
                rockButton.setEnabled(false);
                paperButton.setEnabled(false);
                scissorsButton.setEnabled(false);
                scoreButton.setEnabled(false);
            }
        });
    }

    private void enableButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Enable all buttons
                //resetButton.setEnabled(true);
                rockButton.setEnabled(true);
                paperButton.setEnabled(true);
                scissorsButton.setEnabled(true);
                scoreButton.setEnabled(true);
            }
        });
    }

    private void enableSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void disableSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.GONE);
            }
        });
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

    private void randomSelection() {
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
        if (connection.isConnected()) {
            connection.settings.storage.readStorage(new StorageManager.ReadCallback() {
                @Override
                public void onSuccess(Map<String, String> map) {
                    Log.d(TAG, "First part of adding score");
                    String mapKey = "";
                    String mapValue = "";
                    int mapID;
                    for (mapID = 0; mapID+1 <= map.size(); mapID++) {
                        mapKey = map.keySet().toArray()[mapID].toString();
                        mapValue = map.values().toArray()[mapID].toString();
                        if (mapKey.equals(bName)) {
                            bScore = mapValue;
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
                            map.put(mapKey,mapValue);
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
                            map.put(mapKey,mapValue);

                            Log.d(TAG, mapKey + " " + mapValue);
                            Log.d(TAG, "Writing new data");
                            connection.settings.storage.writeStorage(map, new StorageManager.WriteCallback() {
                                @Override
                                public void onSuccess() {
                                    getData();
                                }

                                @Override
                                public void onFailure(DeviceConnectionException e) {

                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailure(DeviceConnectionException e) {
                    Log.d(TAG, "Data read was a failure : " + e.getLocalizedMessage());
                }
            });
        }
    }

    private void getData() {
        // getData() is used to fetch the data of the beacon and the user (scores)
        if (connection.isConnected()) {
            connection.settings.storage.readStorage(new StorageManager.ReadCallback() {
                @Override
                public void onSuccess(Map<String, String> map) {
                    String mapKey = "";
                    String mapValue = "";
                    int mapID;
                    for (mapID = 0; mapID+1 <= map.size(); mapID++) {
                        mapKey = map.keySet().toArray()[mapID].toString();
                        mapValue = map.values().toArray()[mapID].toString();
                        Log.d(TAG,"Map key: " + mapKey + " Map value: " + mapValue);
                        if (mapID == 0) {
                            bName = mapKey;
                            bScore = mapValue;
                            String[] tokens = mapValue.split("-");
                            bWin = tokens[0];
                            bLose = tokens[1];
                            bTie = tokens[2];
                            setText(beaconName, bName + " ");
                            setText(beaconScore,bWin + "W " + bLose + "L " + bTie + "T ");
                        }
                        else if (mapKey.equals(name)) {
                            score = mapValue;
                            String[] tokens = mapValue.split("-");
                            pWin = tokens[0];
                            pLose = tokens[1];
                            pTie = tokens[2];
                            setText(playerName,name + " ");
                            setText(playerScore,pWin + "W " + pLose + "L " + pTie + "T ");
                        }
                    }
                    enableButtons();
                    disableSpinner();
                }
                @Override
                public void onFailure(DeviceConnectionException e) {

                }
            });
        }
    }

    private void connectToDevice() {
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
                            String filename = "player_name";
                            FileInputStream inputStream;
                            int i;
                            StringBuffer sb = new StringBuffer(15);
                            // Try to open the file "player_name"
                            // If the file is found, read the file for the player's name
                            // If the file is not found, ask user a name and create a file for it
                            try {
                                inputStream = openFileInput(filename);
                                while ((i = inputStream.read())!= -1) {
                                    name = sb.append((char)i).toString();
                                }
                                inputStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(TAG,"Name file was not found");
                                // Name was not found, so a window is created for asking the user's name
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
                                            Log.d(TAG,"Name cannot be empty, contain spaces or be longer than 15 characters");
                                            displayMessage("Name cannot be empty, contain spaces or be longer than 15 characters", durationLong);
                                        }
                                        // If name was acceptable, it will be compared to the other names saved on the beacon to make sure that there are no duplicates
                                        else
                                        {
                                            // Check that there is a connection to the beacon
                                            if (connection.isConnected()){
                                                // Read the beacon data
                                                connection.settings.storage.readStorage(new StorageManager.ReadCallback() {
                                                    @Override
                                                    public void onSuccess(Map<String, String> map) {
                                                        Log.d(TAG, "Data read was a success");
                                                        // Check if the map saved on the beacon contains the key value of the name given by the user
                                                        if (map.containsKey(name)) {
                                                            // Notify the user that the name has already been chosen by someone else
                                                            Log.d(TAG,"Name has already been chosen");
                                                            displayMessage("Name has already been chosen", durationShort);
                                                        }
                                                        // If the name is OK, new file will be created for the name and the name (and score 0-0-0) will be saved on the beacon
                                                        else {
                                                            String filename = "player_name";
                                                            FileOutputStream outputStream;
                                                            try {
                                                                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                                                                outputStream.write(name.getBytes());
                                                                outputStream.close();
                                                                if (connection.isConnected()){
                                                                    map.put(name,"0-0-0");
                                                                    connection.settings.storage.writeStorage(map, new StorageManager.WriteCallback() {
                                                                        @Override
                                                                        public void onSuccess() {
                                                                            Log.d(TAG, "Data write was a success");
                                                                            getData();
                                                                        }

                                                                        @Override
                                                                        public void onFailure(DeviceConnectionException e) {
                                                                            Log.d(TAG,"Data write was a failure : " + e.getLocalizedMessage());
                                                                        }
                                                                    });
                                                                }
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                            alertDialog.cancel();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(DeviceConnectionException e) {
                                                        Log.d(TAG,"Data read was a failure :" + e.getLocalizedMessage());
                                                    }
                                                });
                                            }
                                        }
                                    }
                                });
                            }
                            // If player hasn't chosen a name yet, data will not be fetched until the player has created a name
                            Log.d(TAG,name);
                            if (name.isEmpty()) {
                                Log.d(TAG,"Waiting for player's name");
                            }
                            else {
                                getData();
                            }
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
