package com.example.blesensreader;
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static com.example.blesensreader.BluetoothLeService.ACTION_GATT_CONNECTED;
import static com.example.blesensreader.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import java.util.ArrayList;

public class ScanDispActivity extends ListActivity {
    // Interested UUIDs
    private final UUID uuid1 = UUID.fromString("00000002-0000-0000-FDFD-FDFDFDFDFDFD");//Weather: Service
    private final UUID uuid2 = UUID.fromString("00000001-0000-0000-FDFD-FDFDFDFDFDFD");//FAN
    private final UUID uuidlighht = UUID.fromString ("10000001-0000-0000-FDFD-FDFDFDFDFDFD");

    //THIS
    private final UUID uuidShutter = convertFromInteger(0x1801);//Shutter
    private final UUID uuidHID = convertFromInteger(0x1801);//Shutter Characteristic
    private final UUID uuid_temp = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb"); //Characteristic
    private final UUID uuid_humidity = UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb");; //Characteristic
    private final UUID uuid1_char = convertFromInteger(0x2902);

    //Reference: https://medium.com/@shahar_avigezer/bluetooth-low-energy-on-android-22bc7310387a/
    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32),LSB);
    }
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

        //Disconnection
        disconnect = findViewById(R.id.transition);

        disconnect.setOnClickListener(v -> {
            disconnect();
            Toast.makeText(this, "Disconnecting and Releasing Resources!", Toast.LENGTH_SHORT).show();
            close();
            instruct.setText("Disconnected");
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

            Toast.makeText(this, "Scanning!", Toast.LENGTH_SHORT).show();
//THIS
            //Test Device BLE BT-Shutter
            String MyBTShutterMac = "2A:07:98:10:48:A0";
            String IPVSWeather = "F6:B6:2A:79:7B:5D";
            String IPVSLight = "F8:20:74:F7:2B:82";
            ScanFilter macFilter = new ScanFilter.Builder()
                    .setDeviceAddress(MyBTShutterMac)
                    .build();
            ScanFilter filterWeather = new ScanFilter.Builder()
                    .setDeviceAddress(IPVSWeather)
                    .build();
            ScanFilter filterLight= new ScanFilter.Builder()
                    .setDeviceAddress(IPVSLight)
                    .build();

            List<ScanFilter> filters = new ArrayList<>();
//            filters.add(filter1.build());//Weather
//            filters.add(filter2);//FAN
            filters.add(macFilter);
            filters.add(filterWeather);
            filters.add(filterLight);

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

            //scanning = true;
            //bluetoothLeScanner.startScan(BleScanCallback);
            bluetoothLeScanner.startScan(filters, settings, BleScanCallback);
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
    private void setupNotifications()
    {
        BluetoothGattCharacteristic temp = bluetoothGatt.getService(uuid1).getCharacteristic(uuid_temp);
        BluetoothGattDescriptor descriptor_t = temp.getDescriptor(uuid1_char);
        bluetoothGatt.setCharacteristicNotification(temp,true);
        descriptor_t.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor_t);
    }
    @SuppressLint("MissingPermission")
    private void writefan(int value){
        byte[] bytes = new byte[2]; // uint16 takes 2 bytes
        bytes[0] = (byte) (value & 0xFF);       // Lower byte
        bytes[1] = (byte) ((value >> 8) & 0xFF); // Upper byte
        BluetoothGattCharacteristic shutter = bluetoothGatt.getService(uuid2).getCharacteristic(uuidlighht);
        shutter.setValue(bytes);
        bluetoothGatt.writeCharacteristic(shutter);
    }
    @SuppressLint("MissingPermission")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Toast.makeText(this, "Connecting!", Toast.LENGTH_SHORT).show();
        final BluetoothDevice device = mleDeviceListAdapter.getDevice(position);
        if (device == null) return;
        bluetoothGatt = device.connectGatt(this, false, gattCallback); // BTGatt instance : Conduct Client Operations. Auto connect is false

        if (mConnectionState == 2) {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        }
//        final Intent intent = new Intent(this, MainActivity.class);
//        // TODO: Information Passing w Intents as Required, PutExtra, Connection Check
//        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        if (scanning) {
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) BleScanCallback);
            scanning = false;
        }
//        startActivity(intent);
    }
    private int mConnectionState;
    /*Reference Android Connectivity Samples: GitHub*/
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        //Figure Out:
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction; // Type of action that has occurred in the Bluetooth connection

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                instruct.setText("CB: Connection to GATT server established");
                bluetoothGatt.discoverServices();
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                instruct.setText(" CB: Connection to GATT server lost");
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                instruct.setText("CB: GATT Services Discoverd");
                Log.d("service",gatt.getDevice().getName());
                Log.d("service",gatt.getServices().get(0).toString());
                setupNotifications();
                //writefan(10000);

                //shutter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);


                //if(gatt.readCharacteristic(shutter))
                //    Log.d("service","reader set");
                //shutter.getDescriptor(uuid1_char).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                if(gatt.writeDescriptor(shutter.getDescriptor(uuid1_char)))
//                    Log.d("service","descriptor set");
//                if(gatt.setCharacteristicNotification(shutter,true))
//                    Log.d("service","notifier set");
//                shutter.setValue(0,0,BluetoothGattCharacteristic.FORMAT_UINT16,0);
//                if ((shutter.getProperties() & BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) ==0)
//                    Log.d("service","no resp");
//                if ((shutter.getProperties() & BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==0)
//                    Log.d("service","default");
//                if ((shutter.getProperties() & BluetoothGattCharacteristic.WRITE_TYPE_SIGNED) ==0)
//                    Log.d("service","signed");
//                shutter.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                if (gatt.writeCharacteristic(shutter))
//                    Log.d("service","success write");
                //shutter.setValue()
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("service","desriptor writer");
            BluetoothGattCharacteristic humidity = bluetoothGatt.getService(uuid1).getCharacteristic(uuid_humidity);
            BluetoothGattDescriptor descriptor_h = humidity.getDescriptor(uuid1_char);
            if (!descriptor_h.equals(descriptor))
            {
                bluetoothGatt.setCharacteristicNotification(humidity,true);
                descriptor_h.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor_h);
            }
            //BluetoothGattCharacteristic shutter = gatt.getService(uuid1).getCharacteristic(uuid_temp);
            //shutter.setValue(new byte[] {1,1});
            //gatt.writeCharacteristic(shutter);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                instruct.setText("CB: Data Available");
                Log.d("service",characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0).toString());
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("cdgscja", String.valueOf(status));
            Log.d("service",characteristic.toString());
            BluetoothGattCharacteristic shutter = gatt.getService(uuid2).getCharacteristic(uuidlighht);
            gatt.readCharacteristic(shutter);
        }
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            if (characteristic.equals(bluetoothGatt.getService(uuid1).getCharacteristic(uuid_temp)))
                Log.d("service","sensor update temp");
            else
                Log.d("service","sensor update humidity");
        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) { }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            //Not Initialized
            return;
        }
        bluetoothGatt.disconnect();
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress; //MAC
    }

}
