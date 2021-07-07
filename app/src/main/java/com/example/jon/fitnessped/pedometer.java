package com.example.jon.fitnessped;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;


public class pedometer extends Fragment implements ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener,
        ResultCallback<LocationSettingsResult> {
    Chronometer SWatch;
    View rootview;
    boolean toggle = true;
    private long timeStopped = 0;
    Button tbutton, rbutton;
    TextView Stepcount;
    Handler handler;
    TextView locationtext, locationtext2;


     private String dataToSend;
    byte delimiter = 10;
    boolean stopWorker = false;
    int readBufferPosition = 0;
    byte[] readBuffer = new byte[1024];

    final int RECEIVE_MESSAGE = 1;
    private OutputStream outStream = null;
    private static final String TAG = "";
    private BluetoothAdapter BTAdapter = null;
    private BluetoothSocket BTSocket = null;
    private StringBuilder sb = new StringBuilder();
    private static String address = "30:15:01:07:09:74";
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream inStream = null;
    private ConnectedThread mConnectedThread;


    private final static int Play_Service = 1000;
    protected static final int RequestSettings = 0x1;
    private Location lastlocation;
    protected GoogleApiClient GoogleAPI;

    protected boolean locationupdate;
    protected LocationRequest locationrequest;
    protected LocationSettingsRequest locationrequestseetings;
    private static int UpdateInt = 10000; //10 seconds
    private static int fastestInt = UpdateInt / 2; //5 seconds
    private static int displacement = 10; //10 meters
    protected String LastupdateTime;
    protected final static String KeyRequest = "Requesting location updates";
    protected final static String KeyLocation = "location";
    protected final static String KeyLastUpdate = "last updated";



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_pedometer, container, false);
    handler = new Handler();
        //Stopwatch
        SWatch = (Chronometer) rootview.findViewById(R.id.chronometer);

        // Buttons
        tbutton = (Button) rootview.findViewById(R.id.SButton);
        tbutton.setOnClickListener(mStartListener);
        rbutton = (Button) rootview.findViewById(R.id.ResetB);
        rbutton.setOnClickListener(mResetListener);
        locationtext = (TextView) rootview.findViewById(R.id.LocationView);
        locationtext2 = (TextView) rootview.findViewById(R.id.LocationView2);
        locationupdate = false;
        Stepcount = (TextView) rootview.findViewById(R.id.Stepct);

        CheckBT();
        BluetoothDevice device = BTAdapter.getRemoteDevice(address);
        Log.e("Jon", device.toString());

        LastupdateTime = "";

        //   if (checkPlayServices()) {

        // Building the GoogleApi client
        //   buildGoogleApiClient();

        //    createLocationRequest();
        //  }
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
        return rootview;
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KeyRequest)) {
                locationupdate = savedInstanceState.getBoolean(
                        KeyRequest);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KeyLocation)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                lastlocation = savedInstanceState.getParcelable(KeyLocation);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KeyLastUpdate)) {
                LastupdateTime = savedInstanceState.getString(KeyLastUpdate);
            }
            updateUI();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        GoogleAPI = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        GoogleAPI.connect();
    }

    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        locationrequest = new LocationRequest();
        locationrequest.setInterval(UpdateInt);
        locationrequest.setFastestInterval(fastestInt);
        locationrequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationrequest);
        locationrequestseetings = builder.build();
    }

    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        GoogleAPI,
                        locationrequestseetings
                );
        result.setResultCallback(this);
    }

    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i(TAG, "All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(getActivity(), RequestSettings);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case RequestSettings:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    protected void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(
                GoogleAPI,
                locationrequest,
                this
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                locationupdate = true;

            }
        });

    }

    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
    }

    private void setButtonsEnabledState() {
        if (locationupdate) {
            tbutton.setEnabled(false);
            rbutton.setEnabled(true);
        } else {
            tbutton.setEnabled(true);
            rbutton.setEnabled(false);
        }
    }

    private void updateLocationUI() {
        if (lastlocation != null) {
            locationtext.setText(String.valueOf(lastlocation.getLatitude()));
            locationtext2.setText(String.valueOf(lastlocation.getLongitude()));

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (GoogleAPI != null) {
            GoogleAPI.connect();
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (GoogleAPI.isConnected() && locationupdate) {
            startLocationUpdates();
        }
        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = BTAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            BTSocket = createBluetoothSocket(device);
        } catch (IOException e) {

        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        BTAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            BTSocket.connect();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                BTSocket.close();
            } catch (IOException e2) {

            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(BTSocket);
        mConnectedThread.start();

    }

    @Override
    public void onPause() {
        super.onPause();
        //stopLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (GoogleAPI.isConnected()) {
            GoogleAPI.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");


        if (lastlocation == null) {
            lastlocation = LocationServices.FusedLocationApi.getLastLocation(GoogleAPI);

            updateLocationUI();
        }
    }

    public void onLocationChanged(Location location) {
        // Assign the new location
        //  GoogleAPI = (GoogleApiClient) location;

        lastlocation = location;
        // Displaying the new location on UI
        displayLocation();
    }

    public void onConnectionSuspended(int arg0) {
        GoogleAPI.connect();
    }

    View.OnClickListener mStartListener = new View.OnClickListener() {
        public void onClick(View v) {
            checkLocationSettings();
            //displayLocation();
            // toggleupdates();
            Connect();

            Button tbutton = (Button) rootview.findViewById(R.id.SButton);
            if (toggle) {

                int stoppedMilliseconds = 0;
                String chronoText = SWatch.getText().toString();
                SWatch.start();
                tbutton.setText("Start");
                dataToSend = "1";
                 //writeData(dataToSend);
                mConnectedThread.write("1");
                String array[] = chronoText.split(":");
                if (array.length == 2) {
                    stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 1000
                            + Integer.parseInt(array[1]) * 1000;
                } else if (array.length == 3) {
                    stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 60 * 1000
                            + Integer.parseInt(array[1]) * 60 * 1000
                            + Integer.parseInt(array[2]) * 1000;
                }
                SWatch.setBase(SystemClock.elapsedRealtime() - stoppedMilliseconds);
                SWatch.setBase(SystemClock.elapsedRealtime() + timeStopped);


            } else {

                tbutton.setText("Stop");
                timeStopped = SWatch.getBase() - SystemClock.elapsedRealtime();

                SWatch.stop();
            }

            toggle = !toggle;
        }

    };
    View.OnClickListener mResetListener = new View.OnClickListener() {
        public void onClick(View v) {
            // stopLocationUpdates();
            SWatch.setBase(SystemClock.elapsedRealtime());
            SWatch.stop();
        }


    };


    private void displayLocation() {

        lastlocation = LocationServices.FusedLocationApi
                .getLastLocation(GoogleAPI);

        if (lastlocation != null) {
            double latitude = lastlocation.getLatitude();
            double longitude = lastlocation.getLongitude();

            locationtext.setText(latitude + "");
            locationtext2.setText(longitude + "");

        } else {

            locationtext.setText("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }
/*
    private void toggleupdates() {
        if (!locationupdate) {
            // Changing the button text


            locationupdate = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

        } else {
            // Changing the button text


            locationupdate = false;

            // Stopping the location updates
            //stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }
*/

    public void onDisconnected() {
        // TODO Auto-generated method stub

    }

/*
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (BTAdapter == null) {
            //
        } else {
            if (BTAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
*/

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            beginListenForData();
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void beginListenForData()   {
            try {
                inStream = BTSocket.getInputStream();
            } catch (IOException e) {
            }

            Thread workerThread = new Thread(new Runnable()
            {
                public void run()
                {
                    while(!Thread.currentThread().isInterrupted() && !stopWorker)
                    {
                        try
                        {
                            int bytesAvailable = mmInStream.available();
                            if(bytesAvailable > 0)
                            {
                                byte[] packetBytes = new byte[bytesAvailable];
                                inStream.read(packetBytes);
                                for(int i=0;i<bytesAvailable;i++)
                                {
                                    byte b = packetBytes[i];
                                    if(b == delimiter)
                                    {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;
                                        handler.post(new Runnable()
                                        {
                                            public void run()
                                            {


                                                    Stepcount.setText(data);


                                                        /* You also can use Result.setText(data); it won't display multilines
                                                        */

                                            }
                                        });
                                    }
                                    else
                                    {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        }
                        catch (IOException ex)
                        {
                            stopWorker = true;
                        }
                    }
                }
            });

            workerThread.start();
        }
/*
        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                           // Get number of bytes and message in "buffer"
                    handler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();        // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }
*/

        //Call this from the main activity to send data to the remote device
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity().getApplicationContext());
        if (resultCode == ConnectionResult.SUCCESS) {

        } else if (resultCode == ConnectionResult.SERVICE_MISSING ||
                resultCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                resultCode == ConnectionResult.SERVICE_DISABLED) {
            //  Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, , 1);
            // dialog.show();
        }
        return false;
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /*
        public void onConnected(Bundle arg0) {

            // Once connected with google api, get the location
            displayLocation();

            if (locationupdate) {
                startLocationUpdates();
            }
        }
    */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KeyLocation, locationupdate);
        savedInstanceState.putParcelable(KeyLocation, lastlocation);
        savedInstanceState.putString(KeyLastUpdate, LastupdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }


    private void CheckBT() {
        BTAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!BTAdapter.isEnabled()) {
            Toast.makeText(getActivity().getApplicationContext(), "Bluetooth Disabled !",
                    Toast.LENGTH_SHORT).show();
        }

        if (BTAdapter == null) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Bluetooth null !", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void Connect() {
        Log.d(TAG, address);
        BluetoothDevice device = BTAdapter.getRemoteDevice(address);
        Log.d(TAG, "Connecting to ... " + device);
        BTAdapter.cancelDiscovery();
        try {
            BTSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            BTSocket.connect();
            Log.d(TAG, "Connection made.");
        } catch (IOException e) {
            try {
                BTSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "Unable to end the connection");
            }
            Log.d(TAG, "Socket creation failed");
        }

        //beginListenForData();
    }


   /*
    private void writeData(String data) {
        try {
            outStream = BTSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "Bug BEFORE Sending stuff", e);
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            Log.d(TAG, "Bug while sending stuff", e);
        }
    }
*/
    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
           BTSocket.close();
        } catch (IOException e) {
        }
    }
/*
    public void beginListenForData()   {
        try {
            inStream = BTSocket.getInputStream();
        } catch (IOException e) {
        }

        Thread workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {

                                            if(Stepcount.getText().toString().equals(".." + data)) {
                                               Stepcount.setText(data);
                                            } else {
                                               Stepcount.append("\n"+data);
                                            }

                                                        //You also can use Result.setText(data); it won't display multilines


                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }
*/
}


