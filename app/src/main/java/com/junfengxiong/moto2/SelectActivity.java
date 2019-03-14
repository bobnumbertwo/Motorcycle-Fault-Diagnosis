package com.junfengxiong.moto2;

/**
 * Created by junfengxiong on 26/01/2018.
 */


import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;

import android.widget.GridView;
import android.widget.SimpleAdapter;
import java.util.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.OnDataReceivedListener;
import app.akexorcist.bluetotohspp.library.BluetoothState;



public class SelectActivity extends AppCompatActivity {
    private List<Map<String, Object>> dataList;
    private SimpleAdapter adapter;

    BluetoothSPP bt;
    Intent distDevice;


    Boolean blueToothIsOk = false;
    String currCmd = "";            // 当前指令
    String prefixCmd = "AT+";

    @BindView(R.id.tvsStatus)
    TextView tvSysStatus;

    TextView m_tvInf = null;


    String deviceName;


    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive() && this.getCurrentFocus() != null) {
            if (this.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    public static byte[] shortToByteArray(int a) {
        return new byte[] {
            (byte) ((a >> 8) & 0xFF),
            (byte) (a & 0xFF)
        };
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


    void initBt() {
        bt = new BluetoothSPP(this);

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
//            finish();
        } else {
            bt.setOnDataReceivedListener(new OnDataReceivedListener() {
                public void onDataReceived(byte[] data, String message) {
                }
            });

            bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
                public void onDeviceDisconnected() {
                    blueToothIsOk = false;
                    tvSysStatus.setText("状态: 连接丢失,将自动重连...");
                    bt.connect(distDevice);

                }

                public void onDeviceConnectionFailed() {
                    blueToothIsOk = false;
                    tvSysStatus.setText("状态: 连接失败,将自动重连...");
                    bt.connect(distDevice);
                }

                public void onDeviceConnected(String name, String address) {
                    deviceName = name;
                    blueToothIsOk = true;
                    tvSysStatus.setText("状态: 连接到 " + deviceName);


                    //发送 AT+BOSCH 看是否我们的设备
                    // String at = "AT+BOSCH";
                    // bt.send(at, true);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);
        ButterKnife.bind(this);

        initBt();
        //初始化数据
        initMenuItem();
    }

    public void initMenuItem() {
        GridView gridView = (GridView) findViewById(R.id.gridview);

        //图标
        int icno[] = { R.drawable.ic_menu_camera, R.drawable.ic_menu_send, R.drawable.ic_menu_share, R.drawable.ic_menu_manage };
        //图标下的文字
        String name[]={"诊断功能","人工排气","真空加注","故障分析"};
        dataList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i <icno.length; i++) {
            Map<String, Object> map=new HashMap<String, Object>();
            map.put("img", icno[i]);
            map.put("text",name[i]);
            dataList.add(map);
        }

        String[] from={"img","text"};

        int[] to={R.id.img,R.id.text};

        adapter=new SimpleAdapter(this, dataList, R.layout.gridview_item, from, to);

        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                switch (arg2){
                    case 0:
                        showDiagnoseDialog();
                        break;
                    case 1:
                        showGasDialog();
                        break;
                    case 2:
                        showVacuoDialog();
                        break;
                    case 3:
                        showFaultDialog();
                        break;
                }
                //                AlertDialog.Builder builder= new AlertDialog.Builder(SelectActivity.this);
//                builder.setTitle("提示").setMessage(dataList.get(arg2).get("text").toString()).create().show();
            }
        });

    }

    // 处理接收到的信息
    public void parseReceive(String inf) {
        // 如果没有当前命令则返回
        if (currCmd == "") return;

        //人工排气结果, abs告警, 轮速测试, 动态测试, 清除故障码
        if (currCmd == "RBPFWC" || currCmd == "RBSFWC" || currCmd == "RBPRWC" || currCmd == "RBSRWC"
                || currCmd == "WarningLamp" || currCmd == "WSSTest" || currCmd == "DynamicTest" || currCmd == "ClearDTC") {
            if (inf == "OK")
                tvSysStatus.append("OK!");
            else
                tvSysStatus.append("失败!");
        } else if (currCmd == "+EWSS") {    //评估轮转速测试
            tvSysStatus.append(inf);
        } else if (currCmd == "Variant") {  // 当前故障信息
            if (m_tvInf == null) return;
            m_tvInf.setText("当前故障:" + inf);
        } else if (currCmd == "History") {  // 历史信息
            if (m_tvInf == null) return;
            m_tvInf.setText("历史信息:" + inf);
        }

    }

    public void sendCmdByte(String cmd, String content) {
        if (blueToothIsOk == false) {
            tvSysStatus.setText("状态: 设备未连接,无法发送指令");
            return;
        }
        currCmd = cmd;
        bt.send(prefixCmd + currCmd + content, true);
        //        bt.send(new byte[] {0x30, 0x38}, false);
    }

    public void sendCmd(String cmd) {
        if (blueToothIsOk == false) {
            tvSysStatus.setText("状态: 设备未连接,无法发送指令");
            return;
        }
        currCmd = cmd;
        bt.send(prefixCmd + currCmd, true);

    }

    //人工排气界面
    public void showGasDialog() {
        /* @setView 装入自定义View ==> R.layout.dialog_customize
     * 由于dialog_customize.xml只放置了一个EditView，因此和图8一样
     * dialog_customize.xml可自定义更复杂的View
     */

        SimpleAdapter gAdapter;
        final AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(SelectActivity.this);
        final View dialogView = LayoutInflater.from(SelectActivity.this)
                .inflate(R.layout.window_gas,null);
        customizeDialog.setTitle("人工排气");
        customizeDialog.setView(dialogView);


        GridView gasGridView = (GridView) dialogView.findViewById(R.id.gasGridView);
        //初始化数据
        //图标
        int icno[] = { R.drawable.ic_menu_manage, R.drawable.ic_menu_manage, R.drawable.ic_menu_manage, R.drawable.ic_menu_manage };
        //图标下的文字
        String name[]={"前轴一回","前轴二回","后轴一回","后轴二回"};
        dataList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i <icno.length; i++) {
            Map<String, Object> map=new HashMap<String, Object>();
            map.put("img", icno[i]);
            map.put("text",name[i]);
            dataList.add(map);
        }

        String[] from={"img","text"};

        int[] to={R.id.img,R.id.text};

        gAdapter = new SimpleAdapter(this, dataList, R.layout.gridview_item, from, to);
        gasGridView.setAdapter(gAdapter);
        gasGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                switch (arg2) {
                    case 0:
                        tvSysStatus.setText("状态: 前轴一回,指令已发送...");
                        sendCmd("RBPFWC");
                        break;
                    case 1:
                        tvSysStatus.setText("状态: 前轴二回,指令已发送...");
                        sendCmd("RBSFWC");
                        break;
                    case 2:
                        tvSysStatus.setText("状态: 后轴一回,指令已发送...");
                        sendCmd("RBPRWC");
                        break;
                    case 3:
                        tvSysStatus.setText("状态: 后轴二回,指令已发送...");
                        sendCmd("RBSRWC");
                        break;
                }

            }
        });




        customizeDialog.show();
    }


    // 真空注入界面
    public void showVacuoDialog() {
        final AlertDialog.Builder dialog =
                new AlertDialog.Builder(SelectActivity.this);
        View dialogView = LayoutInflater.from(SelectActivity.this)
                .inflate(R.layout.window_vacuo,null);
        dialog.setTitle("真空注入");
        dialog.setView(dialogView);
        dialog.show();

        //对控件进行赋值
        Button btVacuo = (Button) dialogView.findViewById(R.id.btVacuo);
        final TextView txt1 = (TextView) dialogView.findViewById(R.id.txtVacuo1);
        final TextView txt2 = (TextView) dialogView.findViewById(R.id.txtVacuo2);
        //修改button的名字
        //绑定点击事件监听（这里用的是匿名内部类创建监听）
        btVacuo.setOnClickListener(new View.OnClickListener(){
            int i = 0;

            public void onClick(View v) {
                // 点击弹出你消息框
                Short s1 = Short.parseShort(txt1.getText().toString());
                Short s2 = Short.parseShort(txt2.getText().toString());
                tvSysStatus.setText("指令: 发送真空注入指令");
                //关闭键盘
                hideKeyboard();
                sendCmdByte("EvacFilling", String.format("%02x", s1) + String.format("%02x", s1));
            }
        });
    }


    // 故障诊断界面
    public void showFaultDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.window_fault);

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.BOTTOM);

        WindowManager m = getWindowManager();
        DisplayMetrics dm = new DisplayMetrics();
        m.getDefaultDisplay().getMetrics(dm);
        lp.height = (int)(dm.heightPixels * 0.9);
        lp.width = (int)(dm.widthPixels * 0.95);

        dialogWindow.setAttributes(lp);

        dialog.setTitle("故障处理");
        dialog.show();

        final TextView tvInf = (TextView) dialog.findViewById(R.id.tvInf);
        m_tvInf = tvInf;
        tvInf.setMovementMethod(new ScrollingMovementMethod());


        Button btClearScreen = (Button) dialog.findViewById(R.id.btClearScreen);
        btClearScreen.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvInf.setText("");
            }
        });


        Button btClearCode = (Button) dialog.findViewById(R.id.btClearCode);
        btClearCode.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 清除故障码");
                sendCmd("ClearDTC");
            }
        });


        Button btCurrInf = (Button) dialog.findViewById(R.id.btCurrInf);
        btCurrInf.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 获取当前故障码");
                sendCmd("Variant");
            }
        });


        Button btHistoryInf = (Button) dialog.findViewById(R.id.btCurrInf);
        btHistoryInf.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 获取历史信息");
                sendCmd("History");
            }
        });


        final TextView txtCeshi = (TextView) dialog.findViewById(R.id.txtCeshi);
        Button btCeshi = (Button) dialog.findViewById(R.id.btCurrInf);
        btCeshi.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 发送测试指令");
                sendCmd(txtCeshi.getText().toString());
            }
        });


    }


    // 诊断界面
    public void showDiagnoseDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setTitle("系统诊断");
        dialog.setContentView(R.layout.window_diagnose);

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.BOTTOM);
        dialog.show();


        //处理ABS
        Button btABS = (Button) dialog.findViewById(R.id.btABS);
        btABS.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 发送ABS告警灯指令");
                sendCmd("WarningLamp");
            }
        });

        //处理评估轮速测试
        Button btD1 = (Button) dialog.findViewById(R.id.btD1);
        btD1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 评估轮速传感器测试");
                sendCmd("EWSS");
            }
        });


        //处理轮速测试
        Button btlun = (Button) dialog.findViewById(R.id.btlun);
        final TextView txtLun = (TextView) dialog.findViewById(R.id.txtLun);
        btlun.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 轮速传感器测试");
                // 点击弹出你消息框
                int s1 = Integer.parseInt(txtLun.getText().toString());
                //关闭键盘
                hideKeyboard();
                sendCmdByte("WSSTest", String.format("%04x", s1));
            }
        });

        //处理动态测试
        Button btD = (Button) dialog.findViewById(R.id.btD);
        final TextView txtD1 = (TextView) dialog.findViewById(R.id.txtD1);
        final TextView txtD2 = (TextView) dialog.findViewById(R.id.txtD2);
        final TextView txtD3 = (TextView) dialog.findViewById(R.id.txtD3);
        final TextView txtD4 = (TextView) dialog.findViewById(R.id.txtD4);
        btD.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                tvSysStatus.setText("指令: 动态测试");
                // 点击弹出你消息框
                int s1 = Integer.parseInt(txtD1.getText().toString());
                int s2 = Integer.parseInt(txtD2.getText().toString());
                int s3 = Integer.parseInt(txtD3.getText().toString());
                int s4 = Integer.parseInt(txtD4.getText().toString());

                //关闭键盘
                hideKeyboard();
                sendCmdByte("DynamicTest", String.format("%04x", s1) +
                        String.format("%04x", s2) +
                        String.format("%04x", s3) +
                        String.format("%04x", s4)
                );
            }
        });
    }

}