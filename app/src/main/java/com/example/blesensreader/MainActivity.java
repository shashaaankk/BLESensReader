package com.example.blesensreader;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;


import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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

        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            //Update Permissions
            public void onActivityResult(Map<String, Boolean> result) {
                if(result.get(android.Manifest.permission.BLUETOOTH_SCAN)!=null){
                    isBLUETOOTH_SCAN = result.get(android.Manifest.permission.BLUETOOTH_SCAN);
                }
                if(result.get(android.Manifest.permission.BLUETOOTH_SCAN)!=null){
                    isBLUETOOTH_ADVERTISE = result.get(android.Manifest.permission.BLUETOOTH_ADVERTISE);
                }
                if(result.get(android.Manifest.permission.BLUETOOTH_SCAN)!=null){
                    isBLUETOOTH_CONNECT = result.get(android.Manifest.permission.BLUETOOTH_CONNECT);
                }

            }
        });
        requestPermission();
    }

    /*Check if Permission has been Granted and update the boolean flags for permission*/
    private void requestPermission(){
        isBLUETOOTH_SCAN = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;

        isBLUETOOTH_ADVERTISE = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;

        isBLUETOOTH_CONNECT = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        List<String> permissions = new ArrayList<String>();

        if (!isBLUETOOTH_SCAN){
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!isBLUETOOTH_ADVERTISE) {
            permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (!isBLUETOOTH_CONNECT) {
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Request any permissions that have not been granted
        if (!permissions.isEmpty()) {
            mPermissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }



}