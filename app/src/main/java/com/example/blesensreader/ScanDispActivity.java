package com.example.blesensreader;
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
//import static com.example.blesensreader.BluetoothLeService.ACTION_GATT_CONNECTED;
//import static com.example.blesensreader.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;


import java.util.ArrayList;

public class ScanDispActivity extends ListActivity {

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;     //Scan Period of 10s

    private LeDeviceListAdapter mleDeviceListAdapter;
    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;
    ListView listView;
    ArrayList<String> listItems;
    ArrayAdapter<String> adapter;
    private Button ScanBtn;
    private Button disconnect;

    private TextView instruct;

    public Boolean isConnected;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        listItems = new ArrayList<>();

        setContentView(R.layout.activity_scandisp);

        listView = getListView();

        mleDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mleDeviceListAdapter);
        ScanBtn = findViewById(R.id.Scanbutton);

        instruct = findViewById(R.id.instruction);

        /*
         * Setting up BluetoothScanner for Discovering Nearby BLE Devices
         * Bluetooth Manager is used to obtain an instance of BluetoothAdapter
         * Bluetooth adapter represents local device bluetooth adapter, required for performing bluetooth tasks
         */
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                // Get the BluetoothLeScanner
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                Log.d(null, "Got Bluetooth Scanner");
                //Toast.makeText(this, "Got Bluetooth Scanner", Toast.LENGTH_SHORT).show();
                if (bluetoothLeScanner == null) {
                    Toast.makeText(this, "Unable to obtain a BluetoothLeScanner", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "BluetoothManager not available", Toast.LENGTH_SHORT).show();
        }

        //Scan
        ScanBtn.setOnClickListener(v -> {
            //Toast.makeText(this, "Scan Pressed!", Toast.LENGTH_SHORT).show();
            if (bluetoothLeScanner != null)
            {
                scanning = false;     //Stop Scanning!
                //Toast.makeText(this, "Starting Scan", Toast.LENGTH_SHORT).show();
                scanLeDevice();       //Start Scanning!
//                ScanSettings settings = new ScanSettings.Builder()
//                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                        .build();
//                bluetoothLeScanner.startScan(null, settings, BleScanCallback);

                Log.d(null, "Starting Scan!");

            }

        });

//        //Disconnection
//        disconnect = findViewById(R.id.transition);
//
//        disconnect.setOnClickListener(v -> {
//            disconnect();
//            Toast.makeText(this, "Disconnecting and Releasing Resources!", Toast.LENGTH_SHORT).show();
//            close();
//            instruct.setText("Disconnected");
//        });

    }

    // Adapter for holding devices found through scanning.
    // Reference: https://android.googlesource.com/example/bluetooth/le/DeviceScanActivity.java
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanDispActivity.this.getLayoutInflater();
        }
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.list_devices, viewGroup, false);  // Inflate the custom layout
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            @SuppressLint("MissingPermission") final String deviceName = device.getName() == null ? "Unknown Device" : device.getName();
            viewHolder.deviceName.setText(deviceName);
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }

    }
    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        if (!scanning) {

            Log.d(null, "Wubba Lubba dub dub!");
            Toast.makeText(this, "Scanning!", Toast.LENGTH_SHORT).show();

            // Interested UUIDs
            UUID uuid1 = UUID.fromString("00000002-0000-0000-FDFD-FDFDFDFDFDFD");//Weather
            UUID uuid2 = UUID.fromString("00000001-0000-0000-FDFD-FDFDFDFDFDFD");//FAN

            String serviceUuidMaskString = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
            ParcelUuid parcelUuidMask = ParcelUuid.fromString(serviceUuidMaskString);

            // filters for these UUIDs
            ScanFilter.Builder filter1 = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(uuid1),parcelUuidMask);
            ScanFilter filter2 = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(uuid2),parcelUuidMask)
                    .build();

            //Test Device BLE BT-Shutter
            String MyBTShutterMac = "2A:07:98:10:48:A0";
            ScanFilter macFilter = new ScanFilter.Builder()
                    .setDeviceAddress(MyBTShutterMac)
                    .build();

            List<ScanFilter> filters = new ArrayList<>();
            filters.add(filter1.build());//Weather
            filters.add(filter2);//FAN
            filters.add(macFilter);

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            mHandler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(BleScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(BleScanCallback);
            //bluetoothLeScanner.startScan(filters, settings, BleScanCallback);
        } else {
            Toast.makeText(this, "Stopping scan!", Toast.LENGTH_SHORT).show();
            scanning = false;
            bluetoothLeScanner.stopScan(BleScanCallback);
        }

    }

    /*
     * The interface is used to deliver BLE scan results.
     * When results are found, they are added to List Adapter to display to the user
     */
    private final ScanCallback BleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(null, "Scan Result Obtained");
            mleDeviceListAdapter.addDevice(result.getDevice());
            mleDeviceListAdapter.notifyDataSetChanged();
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Toast.makeText(this, "Connecting!", Toast.LENGTH_SHORT).show();
        final BluetoothDevice device = mleDeviceListAdapter.getDevice(position);
        if (device == null) return;
//        bluetoothGatt = device.connectGatt(this, false, gattCallback); // BTGatt instance : Conduct Client Operations. Auto connect is false
//
//        if (mConnectionState == 2) {
//            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
//        }
        final Intent intent = new Intent(this, MainActivity.class);
        // TODO: Information Passing w Intents as Required, PutExtra, Connection Check
        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        if (scanning) {
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) BleScanCallback);
            scanning = false;
        }
        startActivity(intent);
    }
//    private int mConnectionState;
//    /*Reference Android Connectivity Samples: GitHub*/
//    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
//        //Figure Out:
//        @SuppressLint("MissingPermission")
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            String intentAction; // Type of action that has occurred in the Bluetooth connection
//
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                intentAction = ACTION_GATT_CONNECTED; //Use
//                instruct.setText("CB: Connection to GATT server established");
//
//                mConnectionState = STATE_CONNECTED;
//                broadcastUpdate(intentAction);
//
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                intentAction = ACTION_GATT_DISCONNECTED;
//                instruct.setText(" CB: Connection to GATT server lost");
//                mConnectionState = STATE_DISCONNECTED;
//                broadcastUpdate(intentAction);
//            }
//        }
//
//        @SuppressLint("MissingPermission")
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
//                instruct.setText("CB: GATT Services Discoverd");
//            }
//        }
//
//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                                         BluetoothGattCharacteristic characteristic,
//                                         int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//                instruct.setText("CB: Characteristic Read");
//            }
//        }
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt,
//                                            BluetoothGattCharacteristic characteristic) {
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//            instruct.setText("CB: Characteristic Changed!");
//        }
//
//    };
//
//    private void broadcastUpdate(final String action) {
//        final Intent intent = new Intent(action);
//        sendBroadcast(intent);
//    }
//
//    private void broadcastUpdate(final String action,
//                                 final BluetoothGattCharacteristic characteristic) {
//
//    }
//
//    @SuppressLint("MissingPermission")
//    public void disconnect() {
//        if (bluetoothAdapter == null || bluetoothGatt == null) {
//            //Not Initialized
//            return;
//        }
//        bluetoothGatt.disconnect();
//    }
//
//    @SuppressLint("MissingPermission")
//    public void close() {
//        if (bluetoothGatt == null) {
//            return;
//        }
//        bluetoothGatt.close();
//        bluetoothGatt = null;
//    }
//

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress; //MAC
    }

}
