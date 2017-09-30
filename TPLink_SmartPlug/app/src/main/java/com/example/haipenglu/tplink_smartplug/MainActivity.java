package com.example.haipenglu.tplink_smartplug;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.HashMap;

import static com.example.haipenglu.tplink_smartplug.R.layout.*;

class Device {
    int device_id;
    String ipAddress;
    int port_num;
    String label;
    public Device(int id, String ip, int port){
        device_id = id;
        ipAddress = ip;
        port_num = port;
        label = "Label";
    }
}

public class MainActivity extends AppCompatActivity {

//    static String ipAddress = "192.168.43.216";
//    static  String server_ip = "192.168.43.29";
    String server_ip = "10.0.0.94";
    static int server_port = 8000;
    static String ipAddress = "10.0.0.106";
    static int port_num = 9999;
    static final int ACT_DEV_REQ = 1;
    static final int ACT_SEV_REQ = 2;
    static final int ACT_MAIN_RES = 0;

    static int button_num;
    static HashMap<Integer,Device> device_map = new HashMap<Integer, Device>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                addDevice(view);
            }
        });
        button_num = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this,addDeviceActivity.class);
            intent.putExtra("ip",server_ip);
            intent.putExtra("port",server_port);
            startActivityForResult(intent,ACT_SEV_REQ);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addButton(View view) {
//        android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//        LinearLayout layout = new LinearLayout(this);
//        layout.setOrientation(LinearLayout.VERTICAL);

        // button
        Button button1 = new Button(this);
        button1.setId(button_num); // id
        button1.setText("Device"+Integer.toString(button_num));

//        button1.setLayoutParams(params);
        LinearLayout layout = (LinearLayout)findViewById(R.id.include2);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(button1);

        View.OnClickListener clicks = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
//                if(v.getId() == button_num){
//                }
                Intent intent_device = new Intent(getApplicationContext(), DeviceActivity.class);
                intent_device.putExtra("device_id", v.getId());
                intent_device.putExtra("server_ip",server_ip);
                intent_device.putExtra("server_port",server_port);
                startActivity(intent_device);
//                intent_device.putExtra("plug_ip",device_map.get(v.getId()).ipAddress);
//                intent_device.putExtra("plug_port",device_map.get(v.getId()).port_num);
//                startActivityForResult(intent_device,ACT_DEV_REQ);

            }
        };

        button1.setOnClickListener(clicks);
    }

    public void addDevice(View view) {
        Intent intent = new Intent(this,addDeviceActivity.class);
        intent.putExtra("ip",ipAddress);
        intent.putExtra("port",port_num);
        startActivityForResult(intent,ACT_DEV_REQ);
        addButton(view);
    }


    protected void onActivityResult(int request_code, int result_code, Intent data) {
        if(result_code == ACT_MAIN_RES) {
            if(request_code == ACT_DEV_REQ) {
                ipAddress = data.getStringExtra("ip");
                port_num = data.getIntExtra("port",9999);
                device_map.put(button_num,new Device(button_num,ipAddress, port_num));
                button_num++;
            } else if (request_code == ACT_SEV_REQ) {
                server_ip = data.getStringExtra("ip");
                server_port = data.getIntExtra("port",9999);
            }
        }
    }
}
