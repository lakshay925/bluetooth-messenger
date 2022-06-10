package com.example.bluetoothchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView paired_list,available_list;

    private ArrayAdapter<String> adapter_paired_devices,adapter_available_devices;

    private BluetoothAdapter bluetoothAdapter;

    private ProgressBar progress_scan_devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        progress_scan_devices=(ProgressBar)findViewById(R.id.progress_circular);
        progress_scan_devices.setVisibility(View.GONE);

        paired_list=(ListView)findViewById(R.id.list_paired_devices);
        available_list=(ListView)findViewById(R.id.list_available_devices);

        adapter_paired_devices=new ArrayAdapter<String>(DeviceListActivity.this,R.layout.device_list_item);
        adapter_available_devices=new ArrayAdapter<String>(DeviceListActivity.this,R.layout.device_list_item);

        paired_list.setAdapter(adapter_paired_devices);
        available_list.setAdapter(adapter_available_devices);

        available_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info=((TextView)view).getText().toString();
                String adress=info.substring(info.length() - 17);

                Intent intent_main=new Intent();
                intent_main.putExtra("device_adress",adress);
                setResult(RESULT_OK,intent_main);
                DeviceListActivity.this.finish();
            }
        });
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> paired_devices=bluetoothAdapter.getBondedDevices();

        if(paired_devices.size() == 0 || paired_devices == null){
            adapter_paired_devices.add("No paired Devices Available");
        }

        if(paired_devices != null && paired_devices.size() > 0){
            for(BluetoothDevice device:paired_devices){
                adapter_paired_devices.add(device.getName() + "\n" + device.getAddress());
            }
        }

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetooth_device_listener,intentFilter);

        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetooth_device_listener,intentFilter1);

        paired_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();

                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Log.d("Address", address);

                Intent intent = new Intent();
                intent.putExtra("device_adress", address);

                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private BroadcastReceiver bluetooth_device_listener=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    adapter_available_devices.add(device.getName() +"\n"+ device.getAddress());
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                progress_scan_devices.setVisibility(View.GONE);
                if(adapter_available_devices.getCount() == 0){
                    Toast.makeText(DeviceListActivity.this,"No More Devices Available",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(DeviceListActivity.this,"Click On Devices To Start Chat",Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_list_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.device_list_menu_button:
                //Toast.makeText(this,"Search devices clicked",Toast.LENGTH_LONG).show();
                scan_devices();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scan_devices(){
        adapter_available_devices.clear();

        Toast.makeText(this,"Scan Started",Toast.LENGTH_SHORT).show();
        progress_scan_devices.setVisibility(View.VISIBLE);

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }
}