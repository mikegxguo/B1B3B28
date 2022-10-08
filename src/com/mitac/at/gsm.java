package com.mitac.at;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.os.SystemProperties;
import android.content.Context;
import android.os.PowerManager;
import android.content.Intent;


public class gsm extends Activity {
    private static final String TAG = "ATService";
    private boolean mGsmDisabled = false;
    private Button  mEnableGsmBtn;
    private Button  mDisableGsmBtn;
    private ModemTool mTool;
    private Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.gsm_layout);
        mContext = this;
        mEnableGsmBtn = (Button)findViewById(R.id.enable_gsm);
        mDisableGsmBtn = (Button)findViewById(R.id.disable_gsm);

        log("onCreate()");

        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        String project = SystemProperties.get("ro.product.name");
        if(sc600_sku.contains("EM") && project.contains("gemini")) {
            mTool = new ModemTool();

            mGsmDisabled = IsGsmDisabled();
            if(mGsmDisabled == true) {
                mEnableGsmBtn.setEnabled(true);
                mDisableGsmBtn.setEnabled(false);
            } else {
                mEnableGsmBtn.setEnabled(false);
                mDisableGsmBtn.setEnabled(true);
            }
        } else {
            mEnableGsmBtn.setEnabled(false);
            mDisableGsmBtn.setEnabled(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        log("onPause()");
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume()");
    }

    private void reboot() {
        try {
            Thread.sleep(3000);
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.reboot("Control GSM function");
        } catch(Exception e) {
            Log.d(TAG, "reboot error", e);
        }
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

    public void onEnableGsm(View view) {
        if(mGsmDisabled == true) {
            mGsmDisabled = false;
            EnableGsm();
            mEnableGsmBtn.setEnabled(false);
            mDisableGsmBtn.setEnabled(true);
            SystemProperties.set("persist.sys.gsm.manual", "1");
            reboot();
        }
        //FIXME: ONLY FOR TEST
        /*
        Intent intent = new Intent();
        intent.setAction(ATService.ACTION_DISABLE_GSM);
        mContext.sendBroadcast(intent);
        */
        return ;
    }

    public void onDisableGsm(View view) {
        if(mGsmDisabled == false) {
            mGsmDisabled = true;
            DisableGsm();
            mEnableGsmBtn.setEnabled(true);
            mDisableGsmBtn.setEnabled(false);
            SystemProperties.set("persist.sys.gsm.manual", "1");
            reboot();
        }
        return ;
    }

    private void log(String msg) {
        Log.d(TAG, "GSM: " + msg);
    }

}
