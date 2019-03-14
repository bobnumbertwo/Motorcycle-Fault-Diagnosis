package com.junfengxiong.moto2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.OnDataReceivedListener;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class BlueSPPActivity extends AppCompatActivity {
    BluetoothSPP bt;

    Intent distDevice;

    Menu menu;
    @BindView(R.id.btLastPage)
    Button btLastPage;
    @BindView(R.id.btClearScreen)
    Button btClearScreen;
    @BindView(R.id.btNextPage)
    Button btNextPage;
    @BindView(R.id.btCodeStatus)
    Button btCodeStatus;
    @BindView(R.id.btClearInf)
    Button btClearInf;
    @BindView(R.id.btCurrInf)
    Button btCurrInf;
    @BindView(R.id.btHistoryInf)
    Button btHistoryInf;
    @BindView(R.id.tvSysStatus)
    TextView tvSysStatus;
    @BindView(R.id.tvInf)
    TextView tvInf;

    @BindView(R.id.btCeshi)
    TextView btCeshi;

    @BindView(R.id.txtCeshi)
    TextView txtCeshi;


    protected String deviceName = "";
    private int retryCnt = 3;   //蓝牙重连次数;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_spp);
        ButterKnife.bind(this);
        Log.i("Check", "onCreate");


        tvInf.setMovementMethod(new ScrollingMovementMethod());
        bt = new BluetoothSPP(this);

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
//            finish();
        } else {
            bt.setOnDataReceivedListener(new OnDataReceivedListener() {
                public void onDataReceived(byte[] data, String message) {
                tvInf.append("接收: " + message + "\n");
                }
            });


            bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
                public void onDeviceDisconnected() {
                    tvSysStatus.setText("状态: 连接丢失,将自动重连...");
                    bt.connect(distDevice);

                }

                public void onDeviceConnectionFailed() {
                    tvSysStatus.setText("状态: 连接失败,将自动重连...");
                    bt.connect(distDevice);
                }

                public void onDeviceConnected(String name, String address) {
                    deviceName = name;
                    tvSysStatus.setText("状态: 连接到 " + deviceName);


                    //发送 AT+BOSCH 看是否我们的设备
                    // String at = "AT+BOSCH";
                    // bt.send(at, true);
                }
            });
        }
    }

//    public boolean onCreateOptionsMenu(Menu menu) {
//        this.menu = menu;
//        getMenuInflater().inflate(R.menu.menu_connection, menu);
//        return true;
//    }

//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.menu_android_connect) {
//            bt.setDeviceTarget(BluetoothState.DEVICE_ANDROID);
//            /*
//            if(bt.getServiceState() == BluetoothState.STATE_CONNECTED)
//    			bt.disconnect();*/
//            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
//            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
//        } else if (id == R.id.menu_device_connect) {
//            bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
//			/*
//			if(bt.getServiceState() == BluetoothState.STATE_CONNECTED)
//    			bt.disconnect();*/
//            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
//            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
//        } else if (id == R.id.menu_disconnect) {
//            if (bt.getServiceState() == BluetoothState.STATE_CONNECTED)
//                bt.disconnect();
//        }
//        return super.onOptionsItemSelected(item);
//    }

    public void onDestroy() {
        super.onDestroy();
        bt.stopService();
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothAvailable()) {

        }
        else if (!bt.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            }
        }
    }

    private  void setup() {
        Intent intent = new Intent(getApplicationContext(), DeviceListActivity.class);
//        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
////        intent.putExtra("layout_list", R.layout.list_devices);
//        intent.putExtra("bluetooth_devices", "蓝牙设备列表");
//        intent.putExtra("no_devices_found", "没有发现任何设备");
//        intent.putExtra("scanning", "扫描中...");
//        intent.putExtra("scan_for_devices", "扫描设备");
//        intent.putExtra("select_device", "选择");

        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                this.distDevice = data;
                bt.connect(data);
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                        , "蓝牙未打开."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @OnClick({R.id.btCeshi, R.id.btLastPage, R.id.btClearScreen, R.id.btNextPage, R.id.btCodeStatus, R.id.btClearInf, R.id.btCurrInf, R.id.btHistoryInf})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btCeshi:
                String at = txtCeshi.getText().toString();
                if(at.length() != 0) {
                    tvInf.append("发送:" + at + "\n");
                    txtCeshi.setText("");
                    bt.send(at, true);
                }
                break;
            case R.id.btLastPage:
                tvInf.setText("P0106；进气压力感知器线路不良\n" +
                        "P0107；进气压力感知器输入电压太低\n" +
                        "P0108；进气压力感知器输入电压太高\n" +
                        "P0109；进气温度感知器线路不良或进气压力感知器线路间歇不良\n" +
                        "P0110；进气温度感知器线路间歇性不良\n" +
                        "P0111；进气温度感知器线路不良\n" +
                        "P0112；进气温度感知器线路电压太低");
                break;
            case R.id.btClearScreen:
                tvInf.setText("");
                break;
            case R.id.btNextPage:
                tvInf.setText("P0113；进气温度感知器线路输入电压太高\n" +
                        "P0114；进气温度感知器线路间歇故障\n" +
                        "P0115；引擎水温感知器线路不良\n" +
                        "P0116；引擎水温感器不良\n" +
                        "P0117；引擎水温感知器电压太低\n" +
                        "P0118；引擎水温感知器电压太高\n" +
                        "P0115；引擎水温感知器线路不良\n" +
                        "P0116；引擎水温感器不良\n" +
                        "P0117；引擎水温感知器电压太低\n" +
                        "P0118；引擎水温感知器电压太高\n" +
                        "P0115；引擎水温感知器线路不良\n" +
                        "P0116；引擎水温感器不良\n" +
                        "P0117；引擎水温感知器电压太低\n" +
                        "P0118；引擎水温感知器电压太高\n" +
                        "P0115；引擎水温感知器线路不良\n" +
                        "P0116；引擎水温感器不良\n" +
                        "P0117；引擎水温感知器电压太低\n" +
                        "P0118；引擎水温感知器电压太高\n" +
                        "P0119；引擎水温感知器线路间歇故障\n" +
                        "P0120；节气门位置感知器线路不良\n" +
                        "P0121；节气门位置感知器不良\n" +
                        "P0122；节气门位置感知器信号电压太低\n" +
                        "P0123；节气门位置感知器信号电压太高\n" +
                        "P0124；节气门位置感知器线路间歇故障\n");
                break;
            case R.id.btCodeStatus:
                tvInf.setText("P0135；右侧前氧感知器加热线路不良（02 B1-S1）\n" +
                        "P0136；右侧后气感器线路（02 B1-S1）\n" +
                        "P0137；右侧后氧感知器信号低（02 B1-S1）\n" +
                        "P0137；右侧后氧感知器信号低（02 B1-S1）\n" +
                        "P0138；右侧后氧感知器信号高（02 B1-S1）\n" +
                        "P0139；右侧后氧感知器反应太慢（02 B1-S1）\n" +
                        "P0140；右侧后氧感知器反应次数太少或无作用（02 B1-S1）\n" +
                        "P0141；右侧后氧感知器加热线路故障（02 B1-S1）\n" +
                        "P0142；含氧感知器线路故障（02 B1-S1）\n" +
                        "P0143；含氧感知器电压太低（02 B1-S1）\n" +
                        "P0144；含氧感知器电压太高（02 B1-S1）\n" +
                        "P0145；含氧感知器反应太慢（02 B1-S1）\n" +
                        "P0146；含氧感知器无作用反数太少（02 B1-S1）");
//                bt.send("1", true);

                break;
            case R.id.btClearInf:
//                bt.send("2", true);
                tvInf.setText("");
                break;
            case R.id.btCurrInf:
//                bt.send("3", true);
                tvInf.setText("P0147；含氧感知器加热线路不良（02 B1-S1）\n" +
                        "P0150；左侧前氧感知器线路（02 B1-S1）\n" +
                        "P0151；左侧前氧感知器信号低（02 B1-S1）\n" +
                        "P0152；左侧前氧感知器信号高（02 B1-S1）\n" +
                        "P0153；左侧前氧感知器反应太慢（02 B1-S1）\n" +
                        "P0154；左侧前氧感知器反应次数太少（02 B1-S1）\n" +
                        "P0155；左侧前氧感知器加热线路不良（02 B1-S1）\n" +
                        "P0156；左测后氧感知器线路不良（02 B1-S1）\n" +
                        "P0157；左侧后氧感知器电压太低（02 B1-S1）\n" +
                        "P0158；左侧后氧感知器电压太高（02 B1-S1）\n" +
                        "P0164；含氧感知器电压太高（02 B1-S1）");
                break;
            case R.id.btHistoryInf:
//                bt.send("4", true);
                tvInf.setText("P0165；含氧感知器反应太慢（02 B1-S1）\n" +
                        "P0166；含氧感知器扫应次数太少或无作用于（02 B1-S1）\n" +
                        "P0167；右侧燃油加热线路不良（02 B1-S1）\n" +
                        "P0170；右侧燃油修正不良（B1）\n" +
                        "P0171；右侧混合比太稀（B1）\n" +
                        "P0172；右侧混合比太浓（B1）\n" +
                        "P0173；左侧燃油修正失效\n" +
                        "P0174；左侧混合比太稀（B2）\n" +
                        "P0175；左侧混合比太浓（B2）");
                break;
        }
    }
}