package com.junfengxiong.moto2;

/**
 * Created by junfengxiong on 26/01/2018.
 */


import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.OptionsPickerView;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.OnDataReceivedListener;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import butterknife.BindView;
import butterknife.ButterKnife;

import static java.lang.Thread.sleep;


public class PrimaryActivity extends AppCompatActivity {

//    //辅助类
//    class ViewHolder{
//        TextView tv1;
//        TextView tv2;
//    }

    ArrayAdapter<String> historyAdapter;
    List<String> historyErrors = new ArrayList();

    String mInf = "";


    @BindView(R.id.tvInf)
    TextView tvInf;

    @BindView(R.id.listViewInf)
    ListView mListViewInf;

    private List<Map<String, Object>> dataList;
    private SimpleAdapter adapter;

    /*
                            {
                                "lastClearTime":0,
                                    "errors": [
                                {
                                    "softwareNumber": "abc",
                                    "saveTime": 1234,
                                    "error": "5234:0|5235:1"   //errCode:lamp
                                }
        ]
                            }
*/
    private static String mDefaultMotorObject = "{\"lastClearTime\":0,\"errors\":[]}";

    BluetoothSPP bt;
    Intent distDevice;

    int m_DynamicTestCnt = 0;   //动态测试返回结果计数

    Properties m_errCodes  = new Properties();
    Properties m_dynamicCodes  = new Properties();

    Boolean blueToothIsOk = false;
    String currCmd = "";            // 当前指令
    String prefixCmd = "AT+";

    @BindView(R.id.tabLayout)
    TabLayout mTabLayout;

    @BindView(R.id.viewPager)
    ViewPager mViewPager;

    @BindView(R.id.tvsStatus)
    TextView tvSysStatus;

    ProgressDialog m_ProgressDlg;   //进度对话框

    List<String> mInfList = new ArrayList<String>();//实际的数据源
    ArrayAdapter<String> mInfAdapter;

    List<String> mFaultList = new ArrayList<String>(); //

    String mDeviceName; //诊断仪的蓝牙名称
    String mMotoID = "zrb";     //摩托车设备id
    String mSoftwareNumber = "time"; //软件号

    private ArrayList<String> mCmdList = new ArrayList<>();

    private LayoutInflater mInflater;
    private List<View> mViewList = new ArrayList<>();//页卡视图集合

//    Map<String, Object> mInfMap;


    private OptionsPickerView pvCmdOptions;


    private void showProgressDlg(String msg, Integer time) {
        //清空返回结果计数
        m_DynamicTestCnt = 0;
        mInf = "操作完成";
        if (null == m_ProgressDlg) {
            m_ProgressDlg = ProgressDialog.show(PrimaryActivity.this, "", msg);
            m_ProgressDlg.setCancelable(false);
        } else {
            m_ProgressDlg.show();
        }
        mHandler.sendEmptyMessageDelayed(MSG_DISMISS_DIALOG, time);
    }

    private void closesProgressDlg(String inf) {
        //为了调试,暂时不清空当前指令
//        currCmd = "";
        mInf = inf;
        mHandler.sendEmptyMessage(MSG_DISMISS_DIALOG);
    }

    private void closesProgressDlg() {
        closesProgressDlg("");
    }


    private static int MSG_DISMISS_DIALOG = 0;
    private Handler mHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            if (MSG_DISMISS_DIALOG == msg.what) {
                if(null != m_ProgressDlg){
                    if(m_ProgressDlg.isShowing()){
                        m_ProgressDlg.dismiss();
                    }
                }
                if (mInf.equals("") == false)
                    appendInf(mInf);
            }
        }
    };



    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive() && this.getCurrentFocus() != null) {
            if (this.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }


    private void setup() {
        Intent intent = new Intent(getApplicationContext(), DeviceListActivity.class);


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

        } else if (!bt.isBluetoothEnabled()) {
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
                    , "蓝牙无效"
                    , Toast.LENGTH_SHORT).show();
//            finish();
        } else {
            bt.setOnDataReceivedListener(new OnDataReceivedListener() {
                public void onDataReceived(byte[] data, String message) {
                    parseReceive(message);
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
                    mDeviceName = name;
                    blueToothIsOk = true;
                    tvSysStatus.setText("状态: 连接到 " + mDeviceName);

                    //发送 AT+ID 获取设备id
                  //     sendCmd("ECU2");
//                    sendCmd("ID");
                }
            });
        }
    }


    void initListView () {
        mInfList.clear();
        //适配器，其中 R.layout.xmlforitem是列表中每一项的布局，可以用默认的也可自建，strName则是将数据源绑定到适配器
        mInfAdapter = new ArrayAdapter<String>(PrimaryActivity.this, R.layout.listview_item, R.id.textViewCmd, mInfList);
        //将适配器绑定到列表显示控件ListView；
        mListViewInf.setAdapter(mInfAdapter);
        //增加一项，引起list变化，Adapter也随之变化；
        mInfList.add("ODB板载检测");
//        strInfList.set(strInfList.size()-1, "aaaa");
        //调用notifyDataSetChanged();更新适配器，ListView会自动刷新，notifyDataSetChanged()方法可能需要在UI线程中调用，建议自行测试
        mInfAdapter.notifyDataSetChanged();

////        listViewInf
//        for (int i = 0; i < 16; i++) {
//            mInfMap = new HashMap<String, Object>();
//            mInfMap.put("Id", "100"+i);
//            mInfMap.put("Name","Name_"+i);
//            mInfList.add(mInfMap);
//
//        }
//        listViewInf.setAdapter(new BaseAdapter() {
//            @Override
//            public int getCount() {
//                // TODO Auto-generated method stub
//                return mInfList.size();//数目
//            }
//
//            @Override
//            public Object getItem(int position) {
//                // TODO Auto-generated method stub
//                return mInfList.get(position);
//            }
//
//            @Override
//            public long getItemId(int position) {
//                // TODO Auto-generated method stub
//                return position;
//            }
//
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                // TODO Auto-generated method stub
//                ViewHolder viewHolder;
//                if(convertView == null){
//                    LayoutInflater inflater = PrimaryActivity.this.getLayoutInflater();
//                    convertView = inflater.inflate(R.layout.listview_item, null);
//                    viewHolder = new ViewHolder();
//                    viewHolder.tv1 = (TextView) convertView.findViewById(R.id.textViewCmd);
//                    viewHolder.tv2 =(TextView) convertView.findViewById(R.id.textViewRtn);
//                    convertView.setTag(viewHolder);
//
//                }else{
//                    viewHolder = (ViewHolder) convertView.getTag();
//                }
//                viewHolder.tv1.setText(mInfList.get(position).get("Id").toString());
//                viewHolder.tv2.setText(mInfList.get(position).get("Name").toString());
//                return convertView;
//            }
//
//
//        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.window_primary);
        ButterKnife.bind(this);

        tvInf.setMovementMethod(new ScrollingMovementMethod());
//
//        Integer status = Integer.valueOf("ac", 16);
//        String z =  Integer.toString(status, 2);



        initListView();

        initBt();
        //初始化数据
//        initMenuItem();

        initErrCode();
        initDynamicCode();

        initViews();
    }

    // 处理接收到的信息
    public void parseReceive(String inf) {
        // 如果没有当前命令则返回
        if (currCmd.equals("")) return;

        tvSysStatus.setText("指令完成");

        if (currCmd.equals("TEST")) {
            appendInf("返回:" + inf);
        }
//        // 获取设备id
//        else if (currCmd.equals("ID")) {
//            mMotoID = inf;
//        }
        //人工排气结果
        else if (currCmd.equals("RBPFWC") || currCmd.equals("RBSFWC")  || currCmd.equals("RBPRWC") || currCmd.equals("RBSRWC")) {
//            closesProgressDlg();
            if (inf.equals("OK")){
                if (currCmd.equals("RBPFWC"))
                    mInf = "前轮第一回路排气完成";
//                    appendInf("前轮第一回路排气完成");
                else if (currCmd.equals("RBSFWC"))
                    mInf = "前轮第二回路排气完成";
//                    appendInf("前轮第二回路排气完成");
                else if (currCmd.equals("RBPRWC"))
                    mInf = "后轮第一回路排气完成";
//                    appendInf("后轮第一回路排气完成");
                else if (currCmd.equals("RBSRWC"))
                    mInf = "后轮第二回路排气完成";
//                    appendInf("后轮第二回路排气完成");
            }
            else {
                if (currCmd.equals("RBPFWC"))
                    mInf = "前轮第一回路排气失败";
//                    appendInf("前轮第一回路排气失败");
                else if (currCmd.equals("RBSFWC"))
                    mInf = "前轮第二回路排气失败";
//                    appendInf("前轮第二回路排气失败");
                else if (currCmd.equals("RBPRWC"))
                    mInf = "后轮第一回路排气失败";
//                    appendInf("后轮第一回路排气失败");
                else if (currCmd.equals("RBSRWC"))
                    mInf = "后轮第二回路排气失败";
//                    appendInf("后轮第二回路排气失败");
            }
        }
        //清除故障码
        else if (currCmd.equals("ClearDTC")) {
            if (inf.equals("OK")) {
                appendInf("成功!");
                // 设置最后清除时间
                if (mMotoID.equals("") == false) {
                    JsonParser parser = new JsonParser();
                    JsonObject motoObj = parser.parse(getConfigData("Device:" + mMotoID, mDefaultMotorObject)).getAsJsonObject();
                    motoObj.addProperty("lastClearTime", System.currentTimeMillis());
                    setConfigData("Device:" + mMotoID,  motoObj.toString());
                }
            }
            else
                appendInf("失败!");
        }
        //abs告警, 轮速测试
        else if (currCmd.equals("WarningLamp") || currCmd.equals("WSSTest")||currCmd.equals("EvacFilling")) {
            if (inf.equals("OK"))
                appendInf("成功!");
            else
                appendInf("失败!");
        }
        // ECU2
        else if (currCmd.equals("ECU2")) {
            // 设置 SoftwareNumber
            if (inf.startsWith("+SoftwareNumber=")) {
                mSoftwareNumber = inf.substring(16);
            }

        }
        // ECU
        else if (currCmd.equals("ECU")) {
            if (inf.indexOf("Manufacturer") >= 0 ) {
                inf = "+制造商=博世系统";
            }else {
                inf = inf.replace("SoftwareNumber", "软件号");
//                inf = inf.replace("Manufacturer", "制造商");
                inf = inf.replace("PartsNumber", "硬件号");
            }
            appendInf(inf);
        }
        //动态测试A,等待12条指令或超时
//        else if (currCmd.equals("DynamicTestA")) {
//            m_DynamicTestCnt ++;
//            if (m_DynamicTestCnt <= 6 && inf.startsWith("+DynamicTest")) {
//                int pos = inf.indexOf(":", 12);
//                if (pos > 12) {
//                    appendInf(getDynamicInf(inf.substring(12, pos)) + inf.substring(pos));
//                }
//            }
//            if (m_DynamicTestCnt >= 6) closesProgressDlg();
//        }
//        //动态测试B,等待12条指令或超时
//        else if (currCmd.equals("DynamicTestB")) {
//            m_DynamicTestCnt ++;
//            if (m_DynamicTestCnt <= 6 && inf.startsWith("+DynamicTest")) {
//                int pos = inf.indexOf(":", 12);
//                if (pos > 12) {
//                    appendInf(getDynamicInf(inf.substring(12, pos)) + inf.substring(pos));
//                }
//            }
//            if (m_DynamicTestCnt >= 6) closesProgressDlg();
//        }

        //动态测试A/B,等待16条指令或超时
        else if (currCmd.equals("DynamicTestA") || currCmd.equals("DynamicTestB")) {
            m_DynamicTestCnt ++;
            if (m_DynamicTestCnt <= 16) {
                // 正常
                if (inf.startsWith("+DynamicTest") || inf.equals(":OK"))
                    appendInf(inf);
                // 有错误
                else if (inf.startsWith("--")) {
                    appendInf(getDynamicInf(inf.substring(2)));
                    closesProgressDlg();
                    return;
                }
            }
            if (m_DynamicTestCnt >= 16) closesProgressDlg();
        }
        else if (currCmd.equals("EWSS")) {    //评估轮转速测试
            if (inf.compareToIgnoreCase("+EWSS=7F3322") == 0 || inf.compareToIgnoreCase("7F3322") == 0) {
                appendInf("转速:测试条件不具备");
                return;
            }
            // +EWSS=16进制字符串
            try {
                if (inf.length() >= 22) {
                    double ratio = 0.05625;
                    String msg = String.format("前轮最大/小速度(km/h):%.2f/%.2f;后轮最大/小速度:%.2f/%.2f",
                            Integer.valueOf(inf.substring(6, 10),16) * ratio, Integer.valueOf(inf.substring(10, 14),16) * ratio,
                            Integer.valueOf(inf.substring(14, 18),16) * ratio, Integer.valueOf(inf.substring(18, 22),16) * ratio
                            );
                    appendInf(msg);
                }
                else
                    appendInf("解析错误");
            }catch (Exception e) {
                appendInf("解析错误");
            }
        } else if (currCmd.equals("Variant")) {  // 当前故障信息
            if (inf.equals("+Variant="))
                appendInf("当前信息:无数据");
            else{
                //按照实际结果来进行解析
                try {
                    String content = inf.substring(9);
//                    int start = 2;
//                    int dtcNumber  = Integer.valueOf(content.substring(0, 2),16);
                    int start = 0;
                    int dtcNumber  = content.length() / 6;
                    //清空faultList
                    mFaultList.clear();
                    String sS;
                    Long lastClearTime = 0L;
                    String historyError = "";
                    JsonObject motoObj = null;
                    JsonArray errors = new JsonArray();
                    JsonObject errorObj = new JsonObject();
                    JsonParser parser = new JsonParser();

                    appendInf("-DTC当前故障数量:" + dtcNumber);

                    //                    Long sCurrSaveTime = System.currentTimeMillis();
                    for (int i = 0; i < dtcNumber; ++i) {
                        Integer status = Integer.valueOf(content.substring(start + 4, start + 5), 16);
                        String sStatus = Integer.toString(status, 2);
                        String sStorageState = sStatus.substring(1, 3);
                        String sLamp = sStatus.substring(3);
                        //当前信息
                        //dtc解析
                        sS = "-故障" + i + ":" + getErrInf(content.substring(start, start + 4)) + ":";
                        //dtc状态解析,字符1：T1高4位16进制字符,字符2：T1低4位16进制字符
                        //由于低4位没有使用,只解析T1
                        if (sLamp.equals("0"))
                            sS += "灯关";
                        else
                            sS += "灯开";

                        if (mMotoID.equals("") == false) {
                            if (motoObj == null) {
                                motoObj = parser.parse(getConfigData("Device:" + mMotoID, mDefaultMotorObject)).getAsJsonObject();
                                // 获取最后清除历史记录时间,最后保存历史记录时间
                                lastClearTime = motoObj.get("lastClearTime").getAsLong();
                                errors = motoObj.get("errors").getAsJsonArray();
                            }
                            boolean isExist = false;
                            for (int it = 0; it < errors.size(); it++) {
                                long saveTime = errors.get(it).getAsJsonObject().get("saveTime").getAsLong();
                                //保存时间小于最后清除时间的不处理
                                if (saveTime <= lastClearTime) continue;
                                String error = errors.get(it).getAsJsonObject().get("error").getAsString();
                                //已经保存了不处理
                                if (error.indexOf(content.substring(start, start + 4)) >= 0) {
                                    isExist = true;
                                    break;
                                }
                            }
                            if (isExist == false) {
                                historyError += content.substring(start, start + 4) + ":" + sLamp + "|";
                            }
                        }

                        appendFault(sS);
                        start += 6;
                    }
                    if (historyError.equals("") == false) {
                        errorObj.addProperty("saveTime", System.currentTimeMillis());
                        errorObj.addProperty("softwareNumber", mSoftwareNumber);
                        errorObj.addProperty("error", historyError);
                        errors.add(errorObj);
                        motoObj.add("errors", errors);
                        setConfigData("Device:" + mMotoID,  motoObj.toString());
                    }
                } catch(Exception e) {
                    appendInf("解析错误");
                }
            }
        }
        else if (currCmd.equals("History")) {  // 历史信息
            if (inf.equals("+Variant="))
                appendInf("历史信息:无数据");
            else {
                //按照实际结果来进行解析
                try {
                    String content = inf.substring(9);
//                    int start = 2;
//                    int dtcNumber  = Integer.valueOf(content.substring(0, 2),16);
                    int start = 0;
                    int dtcNumber  = content.length() / 6;
                    int realCnt = 0;
                    //清空faultList
                    mFaultList.clear();
                    String sS;
                    for (int i = 0; i < dtcNumber; ++i) {
                        Integer status = Integer.valueOf(content.substring(start + 4,  start + 5), 16);
                        String sStatus =  Integer.toString(status, 2);
                        String sStorageState = sStatus.substring(1,3);
                        String sLamp = sStatus.substring(3);
                        //当前信息
                        if (sStorageState.equals("11") == false){
                            realCnt++;
                            //dtc解析
                            sS = "-历史故障" + realCnt + ":" + getErrInf(content.substring(start,  start + 4)) + ":";
                            //dtc状态解析,字符1：T1高4位16进制字符,字符2：T1低4位16进制字符
                            //由于低4位没有使用,只解析T1
                            if (sLamp.equals("0"))
                                sS += "灯关";
                            else
                                sS += "灯开";
                            appendInf(sS);
                        }
                        start += 6;
                    }
                    appendInf("-DTC历史故障数量:" + realCnt);
                } catch(Exception e) {
                    appendInf("解析错误");
                }



//                appendInf("历史信息");
//                //进行历史信息解析
//                try {
//                    String content = inf.substring(9);
//                    //日期
//                    String s = content.substring(0, 6);
//                    appendInf("-日期:20" + s);
//                    if (content.substring(6,8).equals("00"))
//                        appendInf(":故障:无");
//                    else
//                        appendInf(":故障:有");
//                } catch(Exception e) {
//                    appendInf("解析错误");
//                }
            }
        }

    }


    private  void sendCmdByte(String cmd, String content, int gasTime) {
        if (blueToothIsOk == false) {
            appendInf(":未连接,无法发送");
            tvSysStatus.setText(":未连接,无法发送");
            return;
        }

        showProgressDlg("命令处理中", gasTime * 1000);

        currCmd = cmd;
        bt.send(prefixCmd + cmd + content, true);

    }

    public void sendCmdByte(String cmd, String content) {
        if (blueToothIsOk == false) {
            appendInf(":未连接,无法发送");
            tvSysStatus.setText(":未连接,无法发送");
            return;
        }

        // 动态测试
        if (cmd.equals("DynamicTestA") || cmd.equals("DynamicTestB")) showProgressDlg("命令处理中", 14000);

        currCmd = cmd;
        bt.send(prefixCmd + cmd + content, true);
        //        bt.send(new byte[] {0x30, 0x38}, false);
    }

    public void sendTestCmd(String cmd) {
        appendInf("发送指令(测试):" + prefixCmd + cmd);
        tvSysStatus.setText("指令(测试):" + prefixCmd + cmd + "运行中...");
        if (blueToothIsOk == false) {
            appendInf(":未连接,无法发送");
            tvSysStatus.setText(":未连接,无法发送");
            return;
        }
        currCmd = "TEST";
        bt.send(prefixCmd + cmd, true);
    }

    public void sendCmd(String cmd) {
        if (blueToothIsOk == false) {
            appendInf(":未连接,无法发送");
            tvSysStatus.setText(":未连接,无法发送");
            return;
        }
        currCmd = cmd;
        //ecu2指令也是发送ecu,只是解析不同而已.
        if (cmd.equals("ECU2")) cmd = "ECU";
        bt.send(prefixCmd + cmd, true);
    }


    // 真空注入界面3
    private void setVacuoView(View dialog) {
        //对控件进行赋值
        final TextView txt1 = (TextView) dialog.findViewById(R.id.txtVacuo1);
        final TextView txt2 = (TextView) dialog.findViewById(R.id.txtVacuo2);

        txt1.setText(getConfigData("VacuoT1", "1"));
        txt2.setText(getConfigData("VacuoT2", "1"));

        //绑定点击事件监听（这里用的是匿名内部类创建监听）
        Button btVacuo = (Button) dialog.findViewById(R.id.btVacuo);
        btVacuo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            try {
                Short s1 = Short.parseShort(txt1.getText().toString());
                Short s2 = Short.parseShort(txt2.getText().toString());
                appendInf("指令:真空注入");
                //关闭键盘
                hideKeyboard();
                sendCmdByte("EvacFilling", String.format("%02x", s1) + String.format("%02x", s2));
            } catch (Exception e) {
                Toast.makeText(getApplicationContext()
                        , "输入有误"
                        , Toast.LENGTH_SHORT).show();
            }

            }
        });

        Button btVacuo1 = (Button) dialog.findViewById(R.id.btVacuo1);
        btVacuo1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("VacuoT1", txt1.getText().toString());
            }
        });

        Button btVacuo2 = (Button) dialog.findViewById(R.id.btVacuo2);
        btVacuo2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("VacuoT2", txt2.getText().toString());
            }
        });
    }

    //故障分析4
    private void setFaultView(View dialog) {
        final TextView txtFaultNO = (TextView) dialog.findViewById(R.id.txtFaultNO);

        Button btClearScreen = (Button) dialog.findViewById(R.id.btClearScreen);
        btClearScreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mInfList.clear();
                mInfList.add("ODB板载检测");
                mInfAdapter.notifyDataSetChanged();
//                tvInf.setText("");
//                tvInf.scrollTo(0, 0);
            }
        });


        Button btClearDTC = (Button) dialog.findViewById(R.id.btClearDTC);
        btClearDTC.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mMotoID.equals(""))
                    appendInf("未获取到设备号,无法清除设备故障信息");
                else {
                    appendInf("指令:清除设备故障信息");
                    sendCmd("ClearDTC");
                }
            }
        });

        Button btClearDataset = (Button) dialog.findViewById(R.id.btClearDataset);
        btClearDataset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mMotoID.equals(""))
                    appendInf("未获取到设备号,无法清除数据库故障信息");
                else {
                    appendInf("指令:清除数据库故障信息");
                    setConfigData("Device:" + mMotoID, mDefaultMotorObject);
                    appendInf("返回:成功");
                }
            }
        });

        Button btClearCode = (Button) dialog.findViewById(R.id.btClearCode);
        btClearCode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int s1 = Integer.parseInt(txtFaultNO.getText().toString());
                    if (s1 > mFaultList.size()) throw new Exception("ss");
                    Boolean rtn =  delFault(s1);
                    if (rtn == true)
                        appendInf("清除故障码:" + s1 + ":" + "成功");
                    else
                        appendInf("清除故障码:" + s1 + ":" + "失败");
                    //关闭键盘
                    hideKeyboard();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext()
                            , "输入有误"
                            , Toast.LENGTH_SHORT).show();
                }
            }
        });


        Button btCurrInf = (Button) dialog.findViewById(R.id.btCurrInf);
        btCurrInf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                appendInf("指令:获取当前故障");
                sendCmd("Variant");
            }
        });

        Button btECUInf = (Button) dialog.findViewById(R.id.btECUInf);
        btECUInf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                appendInf("指令:获取设备信息");
                sendCmd("ECU");
            }
        });



        Button btHistoryInf = (Button) dialog.findViewById(R.id.btHistoryInf);
        btHistoryInf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                BottomSheetLayout bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);
                bottomSheet.showWithSheetView(LayoutInflater.from(PrimaryActivity.this).inflate(R.layout.window_history_list, bottomSheet, false));
                ListView lvHistories = (ListView) findViewById(R.id.lvHistories);

                if (mMotoID.equals("")) {
                    appendInf("指令:无有效设备号,无法获取历史信息");
                    return;
                }
                historyErrors.clear();
                // 读取历史信息
                JsonParser parser = new JsonParser();
                JsonArray errors = new JsonArray();
                JsonObject errorObj = new JsonObject();

                JsonObject motoObj = parser.parse(getConfigData("Device:" + mMotoID, mDefaultMotorObject)).getAsJsonObject();
                errors = motoObj.get("errors").getAsJsonArray();
                for (int it = 0; it < errors.size(); it++) {
                    long saveTime = errors.get(it).getAsJsonObject().get("saveTime").getAsLong();
                    String cSoftwareNumber = errors.get(it).getAsJsonObject().get("softwareNumber").getAsString();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    historyErrors.add(cSoftwareNumber + ":" + simpleDateFormat.format(new Date(saveTime)));
                    String error = errors.get(it).getAsJsonObject().get("error").getAsString();
                    String[] es  = error.split("\\|");
                    for (int j = 0; j < es.length; j++) {
                        String sInf = "--";
                        sInf += getErrInf(es[j].substring(0, 4)) + ":";
                        if (es[j].substring(4).equals("0"))
                            sInf += "灯关";
                        else
                            sInf += "灯开";
                        historyErrors.add(sInf);
                    }
                }

                historyAdapter = new ArrayAdapter<String>(PrimaryActivity.this, android.R.layout.simple_list_item_1, historyErrors);
                lvHistories.setAdapter(historyAdapter);

            }
        });


        final TextView txtCeshi = (TextView) dialog.findViewById(R.id.txtCeshi);
        mCmdList.clear();
        mCmdList.add("ECU(设备信息)");
        mCmdList.add("Variant(当前信息)");
        mCmdList.add("WarningLamp(ABS告警灯)");
        mCmdList.add("WSSTest(轮速传感器测试)");
        mCmdList.add("EWSS(评估轮速传感器测试)");
        mCmdList.add("DynamicTestA(动态测试A)");
        mCmdList.add("DynamicTestB(动态测试B)");
        mCmdList.add("RBPFWC(前轮第一回路排气)");
        mCmdList.add("RBSFWC(前轮第二回路排气)");
        mCmdList.add("RBPRWC(后轮第一回路排气)");
        mCmdList.add("RBSRWC(后轮第二回路排气)");
        mCmdList.add("EvacFilling(真空加注)");

        pvCmdOptions = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) {
                String st = mCmdList.get(options1);
                txtCeshi.setText(st.substring(0, st.indexOf('(')));
            }
        }).build();
        pvCmdOptions.setPicker(mCmdList);

        Button btSelectCmd = (Button) dialog.findViewById(R.id.btSelectCmd);
        btSelectCmd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pvCmdOptions != null) pvCmdOptions.show();
            }
        });


        Button btCeshi = (Button) dialog.findViewById(R.id.btCeshi);
        btCeshi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendTestCmd(txtCeshi.getText().toString());
            }
        });
    }

    // 诊断功能界面1
    private void setDiagnoseView(View dialog) {
        final TextView txtLun = (TextView) dialog.findViewById(R.id.txtLun);
        final TextView txtD1 = (TextView) dialog.findViewById(R.id.txtD1);
        final TextView txtD2 = (TextView) dialog.findViewById(R.id.txtD2);
        final TextView txtD3 = (TextView) dialog.findViewById(R.id.txtD3);
        final TextView txtD4 = (TextView) dialog.findViewById(R.id.txtD4);


        txtLun.setText(getConfigData("V_Lun", "1"));
        txtD1.setText(getConfigData("V_D1", "1"));
        txtD2.setText(getConfigData("V_D2", "1"));
        txtD3.setText(getConfigData("V_D3", "1"));
        txtD4.setText(getConfigData("V_D4", "1"));


        //处理ABS
        Button btABS = (Button) dialog.findViewById(R.id.btABS);
        btABS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                appendInf("指令:ABS告警灯闪烁");
                sendCmd("WarningLamp");
            }
        });

        //处理评估轮速测试
        Button btDD = (Button) dialog.findViewById(R.id.btDD);
        btDD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                appendInf("指令:评估轮速传感器测试");
                sendCmd("EWSS");
            }
        });


        //处理轮速测试
        Button btlun = (Button) dialog.findViewById(R.id.btlun);
        btlun.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            try {
                appendInf("指令:轮速传感器测试");
                int s1 = Integer.parseInt(txtLun.getText().toString()) / 10;
                //关闭键盘
                hideKeyboard();
                sendCmdByte("WSSTest", String.format("%04x", s1));
            } catch (Exception e) {
                Toast.makeText(getApplicationContext()
                        , "输入有误"
                        , Toast.LENGTH_SHORT).show();
            }
            }
        });

        //处理动态测试
        Button btD = (Button) dialog.findViewById(R.id.btD);
        Button btDB = (Button) dialog.findViewById(R.id.btDB);
        Button btlun1 = (Button) dialog.findViewById(R.id.btlun1);
        Button btD1 = (Button) dialog.findViewById(R.id.btD1);
        Button btD2 = (Button) dialog.findViewById(R.id.btD2);
        Button btD3 = (Button) dialog.findViewById(R.id.btD3);
        Button btD4 = (Button) dialog.findViewById(R.id.btD4);

        btD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            try {
                appendInf("指令:动态测试A");
                int s1 = Integer.parseInt(txtD1.getText().toString()) / 10 ;
                int s2 = Integer.parseInt(txtD2.getText().toString()) / 10 ;

                //关闭键盘
                hideKeyboard();
                sendCmdByte("DynamicTestA", String.format("%04x", s1) +
                        String.format("%04x", s2)
                );
            } catch (Exception e) {
                Toast.makeText(getApplicationContext()
                        , "输入有误"
                        , Toast.LENGTH_SHORT).show();
            }
            }
        });


        btDB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    appendInf("指令:动态测试B");
                    int s3 = Integer.parseInt(txtD3.getText().toString()) / 10;
                    int s4 = Integer.parseInt(txtD4.getText().toString()) / 10;

                    //关闭键盘
                    hideKeyboard();
                    sendCmdByte("DynamicTestB", String.format("%04x", s3) +
                            String.format("%04x", s4)
                    );
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext()
                            , "输入有误"
                            , Toast.LENGTH_SHORT).show();
                }
            }
        });


        btlun1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("V_Lun", txtLun.getText().toString());
            }
        });


        btD1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("V_D1", txtD1.getText().toString());
            }
        });

        btD2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("V_D2", txtD2.getText().toString());
            }
        });

        btD3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("V_D3", txtD3.getText().toString());
            }
        });
        btD4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("V_D4", txtD4.getText().toString());
            }
        });
    }

    //人工排气界面2
    private void setGasView(View dialog) {
        //对控件进行赋值
        final TextView txt1 = (TextView) dialog.findViewById(R.id.txtLun1);
        final TextView txt2 = (TextView) dialog.findViewById(R.id.txtLun2);
        final TextView txt3 = (TextView) dialog.findViewById(R.id.txtLun3);
        final TextView txt4 = (TextView) dialog.findViewById(R.id.txtLun4);

        txt1.setText(getConfigData("Gas1", "25"));
        txt2.setText(getConfigData("Gas2", "100"));
        txt3.setText(getConfigData("Gas3", "25"));
        txt4.setText(getConfigData("Gas4", "100"));

        //绑定点击事件监听(这里用的是匿名内部类创建监听)
        Button btLun1 = (Button) dialog.findViewById(R.id.btLun1);
        btLun1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int val = Integer.parseInt(txt1.getText().toString());
                    int s1 =  val / 25;
                    //关闭键盘
                    hideKeyboard();
                    sendCmdByte("RBPFWC", String.format("%02x", s1), val);
                    appendInf("指令:前轮第一回路排气" + txt1.getText().toString() + "秒");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext()
                            , "输入有误"
                            , Toast.LENGTH_SHORT).show();
                }

            }
        });

        Button btLun2 = (Button) dialog.findViewById(R.id.btLun2);
        btLun2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int val = Integer.parseInt(txt2.getText().toString());
                    int s2 = val / 25;
                    //关闭键盘
                    hideKeyboard();
                    sendCmdByte("RBSFWC", String.format("%02x", s2), val);
                    appendInf("指令:前轮第二回路排气" + txt2.getText().toString() + "秒");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext()
                            , "输入有误"
                            , Toast.LENGTH_SHORT).show();
                }

            }
        });

        Button btLun3 = (Button) dialog.findViewById(R.id.btLun3);
        btLun3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int val = Integer.parseInt(txt3.getText().toString());
                    int s3 = val / 25;
                    //关闭键盘
                    hideKeyboard();
                    sendCmdByte("RBPRWC", String.format("%02x", s3), val);
                    appendInf("指令:后轮第一回路排气" + txt3.getText().toString() + "秒");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext()
                            , "输入有误"
                            , Toast.LENGTH_SHORT).show();
                }
            }
        });


        Button btLun4 = (Button) dialog.findViewById(R.id.btLun4);
        btLun4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int val = Integer.parseInt(txt4.getText().toString());
                    int s4 = val / 25;
                    //关闭键盘
                    hideKeyboard();
                    sendCmdByte("RBSRWC", String.format("%02x", s4), val);
                    appendInf("指令:后轮第二回路排气" + txt4.getText().toString() + "秒");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext()
                            , "输入有误"
                            , Toast.LENGTH_SHORT).show();
                }
            }
        });


        Button btSetLun1 = (Button) dialog.findViewById(R.id.btSetLun1);
        btSetLun1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("Gas1", txt1.getText().toString());
            }
        });

        Button btSetLun2 = (Button) dialog.findViewById(R.id.btSetLun2);
        btSetLun2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("Gas2", txt2.getText().toString());
            }
        });

        Button btSetLun3 = (Button) dialog.findViewById(R.id.btSetLun3);
        btSetLun3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("Gas3", txt2.getText().toString());
            }
        });

        Button btSetLun4 = (Button) dialog.findViewById(R.id.btSetLun4);
        btSetLun4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setConfigData("Gas4", txt2.getText().toString());
            }
        });
    }

    //废弃
    private void setGasView2(View gasView) {
        GridView gasGridView = (GridView) gasView.findViewById(R.id.gasGridView);
        //初始化数据
        //图标
        int icno[] = {R.drawable.ic_menu_manage, R.drawable.ic_menu_manage, R.drawable.ic_menu_manage, R.drawable.ic_menu_manage};
        //图标下的文字
        String name[] = {"前轴一回", "前轴二回", "后轴一回", "后轴二回"};
        dataList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < icno.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("img", icno[i]);
            map.put("text", name[i]);
            dataList.add(map);
        }

        String[] from = {"img", "text"};

        int[] to = {R.id.img, R.id.text};

        SimpleAdapter gAdapter = new SimpleAdapter(this, dataList, R.layout.gridview_item, from, to);
        gasGridView.setAdapter(gAdapter);
        gasGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                switch (arg2) {
                    case 0:
                        appendInf("指令:前轴一回打开");
                        sendCmd("RBPFWC");
                        break;
                    case 1:
                        appendInf("指令:前轴二回打开");
                        sendCmd("RBSFWC");
                        break;
                    case 2:
                        appendInf("指令:后轴一回打开");
                        sendCmd("RBPRWC");
                        break;
                    case 3:
                        appendInf("指令:后轴二回打开");
                        sendCmd("RBSRWC");
                        break;
                }

            }
        });
    }

    private void initViews() {
        mInflater = LayoutInflater.from(this);

        View view;

        view = mInflater.inflate(R.layout.window_diagnose, null);
        setDiagnoseView(view);
        mViewList.add(view);

        view = mInflater.inflate(R.layout.window_gas2, null);
        setGasView(view);
        mViewList.add(view);

        view = mInflater.inflate(R.layout.window_vacuo, null);
        setVacuoView(view);
        mViewList.add(view);

        view = mInflater.inflate(R.layout.window_fault2, null);
        setFaultView(view);
        mViewList.add(view);

        mViewPager.setAdapter(new PagerAdapter() {
            private String[] mTitles = {"诊断功能", "人工排气", "真空加注", "故障分析"};
            private List<Fragment> mFragmentTab;

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;//官方推荐写法
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(mViewList.get(position));//添加页卡
                return mViewList.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(mViewList.get(position));//删除页卡
            }

            @Override
            public int getCount() {
                return mTitles.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return mTitles[position];
            }
        });
        mTabLayout.setupWithViewPager(mViewPager);
    }


    public String getConfigData(String name, String defaultVal) {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        String val = preferences.getString(name, defaultVal);
        return val;
    }

    public void setConfigData(String name, String val) {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString(name, val);
        editor.commit();
    }


    private void appendInf(String msg) {
        mInfList.add(msg);
        //调用notifyDataSetChanged();更新适配器，ListView会自动刷新，notifyDataSetChanged()方法可能需要在UI线程中调用，建议自行测试
        mInfAdapter.notifyDataSetChanged();
        mListViewInf.setSelection(mInfAdapter.getCount() - 1);
        if (msg.startsWith("指令:")) {
            tvSysStatus.setText(msg + " 运行中...");
        }
    }

    private void appendFault(String msg) {
        mFaultList.add(msg);
        appendInf(msg);
    }

    private Boolean delFault(int idx) {
        return mInfList.remove(mFaultList.get(idx - 1));
    }

    private String getErrInf(String code) {
        String Inf = m_errCodes.getProperty(code);
        return (Inf == null) ? code : Inf;
    }

    private String getDynamicInf(String code) {
        String Inf = m_dynamicCodes.getProperty(code);
        return (Inf == null) ? code : Inf;
    }


    private void initErrCode() {
//        m_errCodes = new Properties();
        try {
            //方法一：通过activity中的context攻取setting.properties的FileInputStream
            //方法二：通过class获取setting.properties的FileInputStream
            //InputStream in = PropertiesUtill.class.getResourceAsStream("/assets/  setting.properties "));
//            m_errCodes.load(getAssets().open("errCode.properties"));

            InputStream inputStream = getAssets().open("errCode.properties");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            m_errCodes.load(bufferedReader);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void initDynamicCode() {
        try {
            InputStream inputStream = getAssets().open("dynamicCode.properties");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            m_dynamicCodes.load(bufferedReader);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}