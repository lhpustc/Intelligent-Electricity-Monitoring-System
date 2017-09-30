package com.example.haipenglu.tplink_smartplug;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

public class addDeviceActivity extends AppCompatActivity {
    EditText et_ip;
    EditText et_port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device2);
        Intent intent = getIntent();
        et_ip= (EditText)findViewById(R.id.IP);
        et_ip.setText(intent.getStringExtra("ip"));
        et_port = (EditText)findViewById(R.id.Port);
        et_port.setText(Integer.toString(intent.getIntExtra("port",9999)));
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
//        EditText ip_text = (EditText)findViewById(R.id.IP);
//        EditText port_text = (EditText)findViewById(R.id.Plug_Port);
        String ipAddress = et_ip.getText().toString();
        String port = et_port.getText().toString();
        data.putExtra("ip",ipAddress);
        data.putExtra("port", Integer.parseInt(port));
        setResult(0,data);
        finish();
    }
}
