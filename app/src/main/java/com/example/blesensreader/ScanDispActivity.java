package com.example.blesensreader;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;

public class ScanDispActivity extends ListActivity {
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;     //Scan Period of 10s

    private LeDeviceListAdapter mleDeviceListAdapter;
    BluetoothAdapter bluetoothAdapter;
    ListView listView;
    ArrayList<String> listItems;
    ArrayAdapter<String> adapter;

    private Button toWeather;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        listItems = new ArrayList<>();

        setContentView(R.layout.activity_scandisp);

        listView = getListView();
        mleDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mleDeviceListAdapter);

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
                if (bluetoothLeScanner == null) {
                    Toast.makeText(this, "Unable to obtain a BluetoothLeScanner", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "BluetoothManager not available", Toast.LENGTH_SHORT).show();
        }

        toWeather = findViewById(R.id.transition);

        //Transition
        toWeather.setOnClickListener(v -> {

            Intent intent = new Intent(ScanDispActivity.this, MainActivity.class);
            startActivity(intent);

        });

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
        } else {
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

            mleDeviceListAdapter.addDevice(result.getDevice());
            mleDeviceListAdapter.notifyDataSetChanged();
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mleDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, MainActivity.class);
        // TODO: Information Passing w Intents as Required, PutExtra
        if (scanning) {
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) BleScanCallback);
            scanning = false;
        }
        startActivity(intent);
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress; //MAC
    }

}
