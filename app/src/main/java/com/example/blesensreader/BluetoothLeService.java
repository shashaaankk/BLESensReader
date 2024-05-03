package com.example.blesensreader;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.UUID;

/*
* Reference: “Bluetooth Low energy,” Android Developers. https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview
*/
public class BluetoothLeService extends Service {

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

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

    //TODO: UUID
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
