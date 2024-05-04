package com.example.blesensreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<String[]> mPermissionLauncher;
    private boolean isBLUETOOTH_SCAN = false;
    private boolean isBLUETOOTH_ADVERTISE = false;
    private boolean isBLUETOOTH_CONNECT = false;
    private boolean isACCESS_COARSE_LOCATION = false;
    private boolean isACCESS_FINE_LOCATION = false;
    private Button toscanButton;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private boolean mConnected = false;
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private Context context_main = this;
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context_main,"recieved broadcast",Toast.LENGTH_SHORT).show();
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mConnectionState = (TextView) findViewById(R.id.temp);
        mDataField = (TextView) findViewById(R.id.humidity);
        toscanButton = findViewById(R.id.toScanButton);
        //Transition
        toscanButton.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, ScanDispActivity.class);
            startActivity(intent);

        });
        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            //Update Permissions
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(android.Manifest.permission.BLUETOOTH_SCAN) != null) {
                    isBLUETOOTH_SCAN = result.get(android.Manifest.permission.BLUETOOTH_SCAN);
                }
                if (result.get(android.Manifest.permission.BLUETOOTH_SCAN) != null) {
                    isBLUETOOTH_ADVERTISE = result.get(android.Manifest.permission.BLUETOOTH_ADVERTISE);
                }
                if (result.get(android.Manifest.permission.BLUETOOTH_SCAN) != null) {
                    isBLUETOOTH_CONNECT = result.get(android.Manifest.permission.BLUETOOTH_CONNECT);
                }
                if (result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION) != null) {
                    isACCESS_COARSE_LOCATION = result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                if (result.get(android.Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                    isACCESS_FINE_LOCATION = result.get(android.Manifest.permission.ACCESS_FINE_LOCATION);
                }

            }
        });
        requestPermission();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    /*Check if Permission has been Granted and update the boolean flags for permission*/
    private void requestPermission() {
        isBLUETOOTH_SCAN = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;

        isBLUETOOTH_ADVERTISE = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;

        isBLUETOOTH_CONNECT = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

        isACCESS_COARSE_LOCATION = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        isACCESS_FINE_LOCATION = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        List<String> permissions = new ArrayList<String>();

        if (!isBLUETOOTH_SCAN) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!isBLUETOOTH_ADVERTISE) {
            permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (!isBLUETOOTH_CONNECT) {
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!isACCESS_COARSE_LOCATION) {
            permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!isACCESS_FINE_LOCATION) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Request any permissions that have not been granted
        if (!permissions.isEmpty()) {
            mPermissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}