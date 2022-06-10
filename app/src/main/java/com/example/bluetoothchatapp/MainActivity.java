package com.example.bluetoothchatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private ListView chat_list;
    private EditText edit_message;
    private Button send_button;
    private ArrayAdapter<String> adapter_chat;

    RelativeLayout text_box;

    private final int REQUEST_LOCATION_PERMISSION = 101;
    private final int REQUEST_CODE_SELECT_DEVICE = 202;
    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;
    public static final String DEVICE_NAME_KEY = "device_name";
    public static final String TOAST_KEY = "toast";
    private String connected_device;
    private BluetoothAdapter bluetoothAdapter;

    private ChatUtils chatUtils;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1){
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Looking for device");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: "+connected_device);
                            text_box.setVisibility(View.VISIBLE);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer_output=(byte[]) msg.obj;
                    String output_buffer=new String(buffer_output);
                    adapter_chat.add("Me: \n"+output_buffer);
                    break;
                case MESSAGE_READ:
                    byte[] buffer=(byte[]) msg.obj;
                    String input_buffer = new String(buffer,0,msg.arg1);
                    adapter_chat.add(connected_device+": \n"+input_buffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connected_device=msg.getData().getString(DEVICE_NAME_KEY);
                    Toast.makeText(MainActivity.this,connected_device,Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this,msg.getData().getString(TOAST_KEY),Toast.LENGTH_SHORT).show();
                    break;

            }
            return false;
        }
    }) ;

    private void setState(CharSequence subTitle){

        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        checkBluetooth();
        chatUtils = new ChatUtils(this,handler);
    }

    private void init(){
        chat_list=(ListView)findViewById(R.id.chat_list);
        edit_message=(EditText)findViewById(R.id.edit_message);
        send_button=(Button)findViewById(R.id.sendBtn);
        text_box=(RelativeLayout)findViewById(R.id.text_box);
        text_box.setVisibility(View.INVISIBLE);
        adapter_chat = new ArrayAdapter<String>(this, R.layout.chat_item);
        chat_list.setAdapter(adapter_chat);

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = edit_message.getText().toString();
                if(!message.isEmpty()){
                    edit_message.setText("");
                    chatUtils.write(message.getBytes());
                }
            }
        });
    }
    public void checkBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){

            //Toast.makeText(this,"No Bluetooth Found",Toast.LENGTH_LONG).show();

            AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);

            builder.setTitle("No Bluetooth Found");

            builder.setMessage("Close App");
            builder.setCancelable(false);
            builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.finish();
                }
            });

            AlertDialog alertDialog=builder.create();
            alertDialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_screen_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_search_devices:
                //Toast.makeText(this,"Search_Clicked",Toast.LENGTH_LONG).show();
                get_permissions();
                return true;
            case R.id.menu_bluetooth_on:
                turnBluetoothOn();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void get_permissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_PERMISSION);
        }
        else {
            Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SELECT_DEVICE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        if(requestCode == REQUEST_CODE_SELECT_DEVICE && resultCode == RESULT_OK) {
            String adress = data.getStringExtra("device_adress");
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(adress));
            //Toast.makeText(MainActivity.this,"Adress:"+adress,Toast.LENGTH_SHORT).show();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent=new Intent(MainActivity.this,DeviceListActivity.class);
                startActivityForResult(intent,REQUEST_CODE_SELECT_DEVICE);

            }
            else {
                AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Permission Not Granted");

                builder.setMessage("Close App");
                builder.setCancelable(false);
                builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        get_permissions();
                    }
                });

                builder.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                });

                AlertDialog alertDialog=builder.create();
                alertDialog.show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void turnBluetoothOn(){
        if(bluetoothAdapter.isEnabled()){
            Toast.makeText(this,"Bluetooth Already On",Toast.LENGTH_SHORT).show();
        }
        else {
            bluetoothAdapter.enable();
        }

        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){

            Intent make_visible=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            make_visible.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,180);
            startActivity(make_visible);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatUtils != null){
            chatUtils.stop();
        }
    }
}