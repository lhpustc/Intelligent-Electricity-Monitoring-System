package com.example.haipenglu.tplink_smartplug;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class DeviceActivity extends AppCompatActivity {
//    String server_ip = "192.168.43.122";
    String server_ip ;//= "10.0.0.94";
    int server_port ;//= 8000;

    int device_id;
    String plug_ip;
    int plug_port;

    TextView dsp_info;
    EditText et_label;
    String dsp_text;
    String device_label;
    Socket sock;
    Device device;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        Intent intent = getIntent();
        device_id = intent.getIntExtra("device_id",0);
        server_ip = intent.getStringExtra("server_ip");
        server_port = intent.getIntExtra("server_port",8000);
        device = MainActivity.device_map.get(device_id);
        plug_ip = device.ipAddress;
        plug_port = device.port_num;
        device_label = device.label;
        dsp_info = (TextView)findViewById(R.id.tv_info);
        et_label = (EditText)findViewById(R.id.ET_Label);
        et_label.setText(device_label);
        et_label.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            et_label.setText("");
                                        }
                                    }
        );
    }

    Thread t_register = new Thread(){
        @Override
        public void run() {
            try {
                sock = new Socket(server_ip, server_port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
                out.print(plug_ip+":r:"+plug_ip);
                out.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String msg = in.readLine();
                if (msg!=null && msg.length()>0 && msg.equals("OK")) {
                    dsp_text = "register succeed";
                }
                else dsp_text = "register failed";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dsp_info.setText(dsp_text);
                    }
                });
                sock.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };

    public void Register(View view) {
        t_register.start();
    }

    Thread t_on=new Thread() {
        public void run() {
            try {
                sock = new Socket(server_ip, server_port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
                out.print(plug_ip+":c:on");
                out.flush();
                sock.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    };

    public void turnOn(View view) {
        t_on.start();
    }
    Thread t_off=new Thread() {
        @Override
        public void run() {
            try {
                sock = new Socket(server_ip, server_port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
                out.print(plug_ip+":c:off");
                out.flush();
                sock.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };

    public void turnOff(View view) {
        t_off.start();
    }


    Thread t_rtInfo = new Thread() {
        public void run() {
            try {
                sock = new Socket(server_ip, server_port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
                out.print(plug_ip+":c:emeter");
                out.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String msg = in.readLine();
                if (msg!=null && msg.length()>0) {
                    dsp_text = msg;
                }
                else dsp_text = "read error";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dsp_info.setText(dsp_text);
                    }
                });
                sock.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };
    public void getRTInfo(View view) {
        t_rtInfo.start();
    }

    Thread t_status = new Thread(){
        public void run() {
            try {
                sock = new Socket(server_ip, server_port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
                out.print(plug_ip+":s:"+device_label);
                out.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String msg = in.readLine();
                if (msg!=null && msg.length()>0) {
                    dsp_text = msg;
                }
                else dsp_text = "read error";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dsp_info.setText(dsp_text);
                    }
                });
                sock.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };
    public void checkStatus(View view) {
        device_label = et_label.getText().toString();
        t_status.start();
    }

    Thread t_power_usage = new Thread(){
        public void run() {
            try {
                sock = new Socket(server_ip, server_port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
                out.print(plug_ip+":u:"+"power_rate");
                out.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String msg = in.readLine();
                if (msg!=null && msg.length()>0) {
                    dsp_text = msg;
                }
                else dsp_text = "read error";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dsp_info.setText(dsp_text);
                    }
                });
                sock.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };
    public void powerUsage(View view) {
        t_power_usage.start();
    }

    Thread t_sync=new Thread() {
        @Override
        public void run() {
            try {
                sock = new Socket(server_ip, server_port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
                out.print(plug_ip+":m:"+device_label);
                out.flush();
                sock.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };

    public void DataSync(View view) {
        device_label = et_label.getText().toString();
        t_sync.start();
    }

    @Override
    public void onBackPressed() {
        device.label = et_label.getText().toString();
        finish();
    }
}
