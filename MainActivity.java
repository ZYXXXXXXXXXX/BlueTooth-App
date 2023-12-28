package com.example.blcommunicate;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    TextView tv_blueToothStatus;
    Button btn_linkBlueTooth;
    Button btn_send;
    EditText etxt_receiveMessage;
    EditText etxt_sendMessage;
    private BluetoothAdapter bluetoothAdapter;
    BluetoothChatUtil mBlthChatUtil;
    boolean readOver = true;
    String strGet = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_blueToothStatus = (TextView) findViewById(R.id.tv_blueToothStatus);
        btn_linkBlueTooth = (Button) findViewById(R.id.btn_linkBlueTooth);
        btn_send = (Button) findViewById(R.id.btn_send);
        etxt_receiveMessage = (EditText) findViewById(R.id.etxt_receiveMessage);
        etxt_sendMessage = (EditText) findViewById(R.id.etxt_sendMessage);

        if (isSupported()) {
            Toast.makeText(this, "设备支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        }

        if (bluetoothAdapter.isEnabled()) {
            //已经打开蓝牙，判断Android版本是否需要添加权限，解决：无法发现蓝牙设备的问题
            Toast.makeText(this, "蓝牙已打开", Toast.LENGTH_SHORT).show();
            getPermission();
        } else {
            //开启蓝牙
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.enable();
            //关闭蓝牙：bluetoothAdapter.disable();
        }
    }

    /**
     * 连接蓝牙
     * @param view
     */
    public void linkBlueTooth(View view) {
        etxt_receiveMessage.setText("");
        mBlthChatUtil = BluetoothChatUtil.getInstance(this);
        mBlthChatUtil.registerHandler(mHandler);
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            //如果有配对的设备
            for(BluetoothDevice device : pairedDevices){
                //通过array adapter在列表中添加设备名称和地址
                if(device.getName().equals("ESBoard-v1")){
                    if (bluetoothAdapter.isDiscovering()) {
                        //取消搜索
                        bluetoothAdapter.cancelDiscovery();
                    }
                    if (mBlthChatUtil.getState() == BluetoothChatUtil.STATE_CONNECTED) {
                        showToast("蓝牙已连接");
                    }else {
                        mBlthChatUtil.connect(device);
                        if (mBlthChatUtil.getState() == BluetoothChatUtil.STATE_CONNECTED) {
                            showToast("蓝牙连接成功");
                        }

                    }
                    break;
                }
            }
        }else{
            showToast("暂无已配对设备");
        }
    }

    /**
     * 发送
     * @param view
     */
    public void send(View view) {
        byte[] buffer2 =etxt_sendMessage.getText().toString().getBytes();
        mBlthChatUtil.write(buffer2);
    }

    /**
     * 判断是否设备是否支持蓝牙
     * @return 是否支持
     */
    private boolean isSupported() {
        //初始化
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 获取权限
     */
    @SuppressLint("WrongConstant")
    private void getPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            int permissionCheck = 0;
            permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                int ACCESS_LOCATION = 1;// 自定义常量,任意整型
                //未获得权限
                this.requestPermissions( // 请求授权
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        ACCESS_LOCATION);
            }
        }
    }

    /**
     * 显示消息
     * @param str
     */
    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    /**
     * 消息句柄
     */
    private Handler mHandler = new Handler(){
        @RequiresApi(api = Build.VERSION_CODES.R)
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
            String deviceName = msg.getData().getString(BluetoothChatUtil.DEVICE_NAME);
            switch(msg.what){
                case BluetoothChatUtil.STATE_CONNECTED:
                    showToast("连接成功");
                    tv_blueToothStatus.setText("蓝牙状态：已连接HC-05");
                    break;
                case BluetoothChatUtil.STATAE_CONNECT_FAILURE:
                    showToast("连接失败");
                    break;
                case BluetoothChatUtil.STATE_CHANGE:
                   showToast("正在连接设备..");
                    break;
                case BluetoothChatUtil.MESSAGE_DISCONNECTED:
                    showToast("与设备断开连接");
                    break;
                //读到另一方传送的消息
                case BluetoothChatUtil.MESSAGE_READ:{
//                    showToast("接收消息成功");
                    byte[] buf;
                    String str;
                    buf = msg.getData().getByteArray(BluetoothChatUtil.READ_MSG);
                    str = new String(buf,0,buf.length);

                    //根据HC-05传来的消息进行相应的处理后显示
                    if(readOver){
                        strGet=str.trim();
                        if(strGet.length()<9){
                            readOver=false;
                            return;
                        }
                    }
                    else{
                        strGet+=str.trim();
                        readOver=true;
                    }
                    etxt_receiveMessage.setText(etxt_receiveMessage.getText()+"\n"+strGet);
                    break;
                }

                case BluetoothChatUtil.MESSAGE_WRITE:{
//                    showToast("发送消息成功");
                    break;
                }
                default:
                    break;
            }
        };
    };
}