package com.mitac.at;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.SystemProperties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

//import java.io.File;
//import java.io.IOException;
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.FileReader;
//import java.io.FileWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


public class MainService extends Service {
    private static final String TAG = "MainService";
    public static final String EXTRA_EVENT = "event";
    public static final String EVENT_BOOT_COMPLETED = "BOOT_COMPLETED";
    private static boolean mHandled = false;
//    public static final String EXTRA_EVENT_APN = "apn";
//    public static final String EXTRA_EVENT_MODEM = "modem";
    private ModemTool mTool;


//    BroadcastReceiver mMitacAPIReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            //Bundle bundle = intent.getExtras();
//            switch (action) {
//                case Intent.ACTION_BOOT_COMPLETED:
//                    Log.d(TAG,"ACTION_BOOT_COMPLETED");
//                    String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
//                    String project = SystemProperties.get("ro.product.name");
//                    if(sc600_sku.contains("EM") && project.contains("gemini")) {
//                        mTool = new ModemTool();
//                        EnableB1B3B28();
//                    }
//                    break;
//            }
//        }
//    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

//        IntentFilter mitacAPIFilter = new IntentFilter();
//        mitacAPIFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
//        registerReceiver(mMitacAPIReceiver, mitacAPIFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        String apn = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_APN);
//        if(apn != null) {
//            Log.d(TAG, "onStartCommand : apn = " + apn);
//        }
//        String modem = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_MODEM);
//        if(modem != null) {
//            Log.d(TAG, "onStartCommand :  modem = " + modem);
//        }
//        if(apn!=null && APNUtil.ValidateAPN(apn)){
//            APNUtil.customizeAPN(MainService.this);
//        } else if(modem!=null && modem.equals("get_ver")) {
//            String adsp_ver = getAdspVer();
//            Log.d(TAG, "ADSP version: " + adsp_ver);
//            String baseband = SystemProperties.get("persist.radio.version.baseband");
//            String[] temp = baseband.split(",");
//            SystemProperties.set("persist.sys.fw.version", temp[0]+","+adsp_ver);
//        }
        String event = intent == null ? "" : intent.getStringExtra(EXTRA_EVENT);
        Log.d(TAG, "onStartCommand : event = " + event);
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        String project = SystemProperties.get("ro.product.name");
        if(sc600_sku.contains("EM") && project.contains("gemini")) {
            mTool = new ModemTool();
            EnableB1B3B28();
        }
        if (EVENT_BOOT_COMPLETED.equals(event)) {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

//    private String getAdspVer(){
//        File select_image = new File("/sys/devices/soc0/select_image");
//        final String filename = "/sys/devices/soc0/image_crm_version";
//        BufferedWriter bw = null;
//        FileReader reader = null;
//        String adsp_ver = "";
//        try {
//            bw = new BufferedWriter(new FileWriter(select_image));
//            bw.write("12");
//            bw.flush();
//            bw.close();
//
//            reader = new FileReader(filename);
//            char[] buf = new char[32];//N664-R00A00-000000_20210520
//            int n = reader.read(buf);
//            if (n > 1) {
//                adsp_ver = String.valueOf(buf,0,n-1);
//            }
//            reader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return adsp_ver;
//    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(mMitacAPIReceiver);
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private String sendGetAT(String atCommand, String prefix) {
        String content = null;
        BufferedReader br = null;
        try {
            //ATInterface atInterface = getATInterface();
            //String result = atInterface.sendAT(atCommand);
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, atCommand);
            //Log.d(TAG, "sendGetAT : atCommand=" + atCommand + ", prefix=" + prefix + ", result=" + result);
            if(result != null && result.contains("OK")) {
                br = new BufferedReader(new StringReader(result));
                String line;
                while((line = br.readLine()) != null) {
                    if(line.contains(prefix)) {
                        content = line.substring(prefix.length());
                        //content = content.replace("\"", "");
                        break;
                    }
                }
            } else if(result != null && result.contains("ERROR")) {
                content = "ERROR";
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        return content;
    }

    private boolean sendAT(String cmd) {
        boolean res = false;
        try {
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            //Log.d(TAG, "sendAT : cmd = " + cmd + ", result = " + result);
            if (result != null && result.contains("OK")) {
                res = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return res;
    }

    // Customize for Mio SKU(SC600YEM)
    // Search radio capability
    //AT+QNVR=6828,0
    //+QNVR: "DF000808E00100000000000000000000"
    //AT+QNVR=6829,0
    //+QNVR: "DF000808E00100000000000000000000"
    //AT+QNVR=1877,0
    //+QNVR: "8003E80600000200"
    //AT+QNVR=4548,0
    //+QNVR: "0000C00600000200"

    // Disable 2G+3G
    //AT+QNVW=1877,0,"0000000000000000"
    //AT+QNVW=4548,0,"0000000000000000"

    // Only keep LTE bands(1,3,28)
    //AT+QNVW=6828,0,"05000008000000000000000000000000"
    //AT+QNVW=6829,0,"05000008000000000000000000000000"

    public boolean EnableB1B3B28() {
        String prefix = "+QNVR: \"";
        String val = null;
        String NV_4548_R = "AT+QNVR=4548,0";
        String NV_4548_W = "AT+QNVW=4548,0,\"0000000000000000\"";
        String NV_1877_R = "AT+QNVR=1877,0";
        String NV_1877_W = "AT+QNVW=1877,0,\"0000000000000000\"";
        String NV_6828_R = "AT+QNVR=6828,0";
        String NV_6828_W = "AT+QNVW=6828,0,\"05000008000000000000000000000000\"";
        String NV_6829_R = "AT+QNVR=6829,0";
        String NV_6829_W = "AT+QNVW=6829,0,\"05000008000000000000000000000000\"";
        String RESET_MODEM = "AT+QCFG=\"reset\"";

        Log.d(TAG, "Checking NV items......");
        //check 4548
        val = sendGetAT(NV_4548_R, prefix);
        if(val.contains("0000000000000000")) {
            Log.d(TAG, "NV item 4548 is changed");
            mHandled = true;
        } else {
            Log.d(TAG, "Changing NV item 4548");
            mHandled = false;
            sendAT(NV_4548_W);
        }
        //check 1877
        val = sendGetAT(NV_1877_R, prefix);
        if(val.contains("0000000000000000")) {
            Log.d(TAG, "NV item 1877 is changed");
        } else {
            Log.d(TAG, "Changing NV item 1877");
            mHandled = false;
            sendAT(NV_1877_W);
        }
        //check 6828
        val = sendGetAT(NV_6828_R, prefix);
        if(val.contains("05000008000000000000000000000000")) {
            Log.d(TAG, "NV item 6828 is changed");
        } else {
            Log.d(TAG, "Changing NV item 6828");
            mHandled = false;
            sendAT(NV_6828_W);
        }
        //check 6829
        val = sendGetAT(NV_6829_R, prefix);
        if(val.contains("05000008000000000000000000000000")) {
            Log.d(TAG, "NV item 6829 is changed");
        } else {
            Log.d(TAG, "Changing NV item 6829");
            mHandled = false;
            sendAT(NV_6829_W);
        }
        //reset modem
        if(!mHandled) {
            sendAT(RESET_MODEM);
            Log.d(TAG, "Reset modem ......");
        }
        Log.d(TAG, "the process is finished");
        return true;
    }

}
