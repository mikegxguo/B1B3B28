package com.mitac.b71;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class ATService extends Service {
    private static final String TAG = "ATService";
    public static final String EXTRA_EVENT = "extra_event";
    public static final String EVENT_BOOT_COMPLETED = "event_boot_completed";
    public static final String ACTION_RESET_MODEM = "mitac.intent.action.RESET_MODEM";
    public static final String ACTION_DISABLE_GSM = "com.mitac.gsm.DISABLE_GSM";
    public static final String ACTION_ENABLE_GSM = "com.mitac.gsm.ENABLE_GSM";

    private ModemTool mTool;

    @Override
    public void onCreate() {
        super.onCreate();
        mTool = new ModemTool();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_RESET_MODEM);
        intentFilter.addAction(ACTION_DISABLE_GSM);
        intentFilter.addAction(ACTION_ENABLE_GSM);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onreceive " + intent);
            String action = intent.getAction();
            String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
            String project = SystemProperties.get("ro.product.device");
            if(ACTION_RESET_MODEM.equals(action)) {
                handleResetModem();
            } else if(ACTION_DISABLE_GSM.equals(action)) {
                if(project.contains("gemini") && sc600_sku.contains("EM")) {
                    if(IsGsmDisabled()) {
                        Log.d(TAG, "GSM is already disabled");
                    } else {
                        Log.d(TAG, "Disable GSM");
                        DisableGsm();
                        SystemProperties.set("persist.sys.gsm.manual", "1");
                    }
                }
            } else if(ACTION_ENABLE_GSM.equals(action)) {
                if(project.contains("gemini") && sc600_sku.contains("EM")) {
                    if(IsGsmDisabled()) {
                        Log.d(TAG, "Enable GSM");
                        EnableGsm();
                        SystemProperties.set("persist.sys.gsm.manual", "1");
                    } else {
                        Log.d(TAG, "GSM is already enabled");
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String event = intent == null ? "" : intent.getStringExtra(EXTRA_EVENT);
        Log.d(TAG, "onStartCommand : event = " + event);
        if (EVENT_BOOT_COMPLETED.equals(event)) {
            handleBootCompleted();
        }
        //stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void handleBootCompleted() {
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        String modem = SystemProperties.get("persist.radio.version.baseband");
        String brand = SystemProperties.get("persist.sys.rgimg.brand", null);
        String gsm_control_manual = SystemProperties.get("persist.sys.gsm.manual",null);
        boolean wifiOnly = false;
        if(sc600_sku.contains("WF") || modem.contains("WF")) { //WiFi SKU
            wifiOnly = true;
        }

        getFwVer();
        if(!wifiOnly) { //support LTE
            configureSimHotswap();
            String project = SystemProperties.get("ro.product.name");
            if(project.contains("gemini")) {
                configureSimInsertLevel();
                if(sc600_sku.contains("EM")){
                    if(IsGsmDisabled()) {
                        Log.d(TAG, "GSM is already disabled");
                        if(!gsm_control_manual.equals("1") && !brand.toLowerCase().contains("eroad")) {
                            Log.d(TAG, "If image isn't for ERoad, enable GSM");
                            EnableGsm();
                        }
                    } else {
                        if(!gsm_control_manual.equals("1") && brand.toLowerCase().contains("eroad")) {
                            Log.d(TAG, "Disable GSM");
                            DisableGsm();
                        }
                    }
                }
            }
            String [] sarKeyNeededProjects = new String [] {
                "surfing_pro", "chiron_pro", "triton", "prometheus", "gemini"
            };

            for (String p : sarKeyNeededProjects) {
                if (project.contains(p)) {
                    configureSarKey();
                    break;
                }
            }
        }
    }

    private String getAdspVer(){
        File select_image = new File("/sys/devices/soc0/select_image");
        final String filename = "/sys/devices/soc0/image_crm_version";
        BufferedWriter bw = null;
        FileReader reader = null;
        String adsp_ver = "";
        try {
            bw = new BufferedWriter(new FileWriter(select_image));
            bw.write("12");
            bw.flush();
            bw.close();

            reader = new FileReader(filename);
            char[] buf = new char[32];//N664-R00A00-000000_20210520
            int n = reader.read(buf);
            if (n > 1) {
                adsp_ver = String.valueOf(buf,0,n-1);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return adsp_ver;
    }

    private void getFwVer() {
        String adsp_ver = getAdspVer();
        Log.d(TAG, "ADSP version: " + adsp_ver);
        String baseband = SystemProperties.get("persist.radio.version.baseband");
        String[] temp = baseband.split(",");
        SystemProperties.set("persist.sys.fw.version", temp[0]+","+adsp_ver);
    }

    //Disable GSM since it may trigger UVLO
    private void DisableGsm() {
        boolean res = false;
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        if(sc600_sku.contains("EM")) {
            res = sendAT("at+qnvw=1877,0,\"0000C00600000200\"");
            res = sendAT("at+qnvr=1877,0");
            if(res) {
                Log.d(TAG, "Disable GSM since it may trigger UVLO!");
            }
            SystemProperties.set("persist.sys.gsm.status", "0");
        }
    }

    //Restore GSM since it may be disabled by the experiment(UVLO).
    private void EnableGsm() {
        boolean res = false;
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        if(sc600_sku.contains("EM")) {
            res = sendAT("at+qnvw=1877,0,\"8003E80600000200\"");
            res = sendAT("at+qnvr=1877,0");
            if(res) {
                Log.d(TAG, "Restore GSM since it may be disabled by the experiment(UVLO).");
            }
            SystemProperties.set("persist.sys.gsm.status", "1");
        }
    }

    private boolean IsGsmDisabled() {
        boolean ret = false;
        String strVal = null;
        try {
            String cmd = "at+qnvr=1877,0";
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            Log.d(TAG, "sendAT : cmd = " + cmd + "\n result = " + result);
            if (result != null && result.contains("OK")) {
                String prefix = "+QNVR: \"";
                int idx = result.indexOf(prefix);
                if(idx >= 0) {
                    idx += prefix.length();
                    strVal = result.substring(idx, idx+16);
                    Log.d(TAG, "NV item: "+strVal);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        if("0000C00600000200".equals(strVal)) {
            ret = true; //GSM is disabled
        } else {
            ret = false; //GSM keeps in original settings
        }
        SystemProperties.set("persist.sys.gsm.status", ret?"0":"1");
        return ret;
    }

    private void configureSimHotswap() {
        String curConfig = getSimHotswapConfig();

        if (!"1".equals(curConfig)) {
            boolean res = sendAT("at+qcfg=\"hotswap\",1");
            if(res) {
                curConfig = getSimHotswapConfig();
                if("1".equals(curConfig)) {
                    Log.d(TAG, "Open SIM1 hot swap successfully!!");
                    //reboot();
                }
            }
        }
    }

    private void configureSimInsertLevel() {
        String curConfig = getSimInsertLevel();

        if ("1".equals(curConfig)) {
            boolean res = sendAT("at+qcfg=\"siminsertlevel\",0"); //0:SIM insert, 1:SIM removed
            if(res) {
                curConfig = getSimInsertLevel();
                if("0".equals(curConfig)) {
                    Log.d(TAG, "Set SIM1 InsertLevel successfully!!");
                    //reboot();
                }
            }
        }
    }

    private void reboot() {
        try {
            Thread.sleep(3000);
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.reboot("Sim Recovery");
        } catch(Exception e) {
            Log.d(TAG, "reboot error", e);
        }
    }

    private String getSimHotswapConfig() {
        String config = null;
        try {
            String cmd = "at+qcfg=\"hotswap\"";
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            Log.d(TAG, "sendAT : cmd = " + cmd + "\n result = " + result);
            if (result != null && result.contains("OK")) {
                String prefix = "+QCFG: \"HOTSWAP\",";
                int idx = result.indexOf(prefix);
                if(idx >= 0) {
                    idx += prefix.length();
                    config = result.substring(idx, idx+1);
                    Log.d(TAG, "sim1 hotswap config="+config+".");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return config;
    }

    private String getSimInsertLevel() {
        String config = null;
        try {
            String cmd = "at+qcfg=\"siminsertlevel\"";
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            Log.d(TAG, "sendAT : cmd = " + cmd + "\n result = " + result);
            if (result != null && result.contains("OK")) {
                String prefix = "+QCFG: \"simInsertLevel\",";
                int idx = result.indexOf(prefix);
                if(idx >= 0) {
                    idx += prefix.length();
                    config = result.substring(idx, idx+1);
                    Log.d(TAG, "sim1 InsertLevel config="+config+".");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return config;
    }

    private void configureSarKey() {
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        String project = SystemProperties.get("ro.product.name");
        boolean res2 = false;
        String curConfig = getSarKey();
        if(!"01".equals(curConfig)) {
            //#SAR_KEY
            boolean res1 = false;
            Log.d(TAG, "sc600_sku="+sc600_sku+", project="+project);
            if(sc600_sku.contains("EM") && project.contains("surfing_pro")) {
                //#SAR_BACK_OFF_LIMIT_1(LTE: band 7)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00021312\",D200EB00EB00EB00EB00EB00EB00EB00");
            } else if(sc600_sku.contains("NA") && (project.contains("chiron_pro") || project.contains("triton"))) {
                //#SAR_BACK_OFF_LIMIT_1(WCDMA band 2)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00021322\",BE00E600E600E600E600E600E600E600");
                //#SAR_BACK_OFF_LIMIT_1(WCDMA band 4)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00021323\",C300E600E600E600E600E600E600E600");
                //#SAR_BACK_OFF_LIMIT_1(LTE: band 2)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00021308\",C300E600E600E600E600E600E600E600");
                //#SAR_BACK_OFF_LIMIT_1(LTE: band 4)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00021310\",C300E600E600E600E600E600E600E600");
                //#SAR_BACK_OFF_LIMIT_1(LTE: band 7)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00021312\",B400E600E600E600E600E600E600E600");
                //#SAR_BACK_OFF_LIMIT_1(LTE: band 25)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00022360\",BE00E600E600E600E600E600E600E600");
                //#SAR_BACK_OFF_LIMIT_1(LTE: band 41)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00021679\",B400E600E600E600E600E600E600E600");
                //#SAR_BACK_OFF_LIMIT_1(LTE: band 66)
                res2 = sendAT("at+qnvfw=\"/nv/item_files/rfnv/00029265\",C300E600E600E600E600E600E600E600");
            }
            res1 = sendAT("at+qnvfw=\"/nv/item_files/mcs/lmtsmgr/sar/sar_qmi_comp_key\",01");
            if(res1 && res2) {
                //curConfig = getSarConfig();
                //if("1".equals(curConfig)) {
                    Log.d(TAG, "Set SAR key successfully!!");
                //}
            }
        }
    }

    private String getSarKey() {
        String config = null;
        try {
            String cmd = "at+qnvfr=\"/nv/item_files/mcs/lmtsmgr/sar/sar_qmi_comp_key\"";
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            Log.d(TAG, "sendAT : cmd = " + cmd + "\n result = " + result);
            if (result != null && result.contains("OK")) {
                String prefix = "+QNVFR: ";
                int idx = result.indexOf(prefix);
                if(idx >= 0) {
                    idx += prefix.length();
                    config = result.substring(idx, idx+2);
                    Log.d(TAG, "sar config="+config+".");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return config;
    }

    private boolean sendAT(String cmd) {
        boolean res = false;
        try {
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            Log.d(TAG, "sendAT : cmd = " + cmd + ", result = " + result);
            if (result != null && result.contains("OK")) {
                res = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return res;
    }

    private void handleResetModem() {
        boolean res = false;
        res = sendAT("at+qcfg=\"reset\"");
        if(res) {
            Log.d(TAG, "Reset modem successfully!!");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unregisterReceiver(mBroadcastReceiver);
    }

}
