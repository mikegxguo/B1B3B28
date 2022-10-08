package com.mitac.at;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View.OnKeyListener;
import android.view.KeyEvent;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.util.Log;
import android.app.AlertDialog;
import android.os.SystemProperties;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileNotFoundException;


public class imei extends Activity {
    private static final String TAG = "ATService";
    private EditText CmdRespText = null;
    private String sim1_imei = null;
    private String sim2_imei = null;
    private boolean mReadIMEI = false;
    private ModemTool mTool;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.imei_layout);

        log("onCreate()");
        // Initially turn on first button.
        mTool = new ModemTool();

        CmdRespText = (EditText) findViewById(R.id.edit_sn);
        CmdRespText.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    return true;
                }
                return false;
            }
        });
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

    private String getIMEI(String cmd) {
        String str = null;
        try {
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            Log.d(TAG, "sendAT : cmd = " + cmd + "\n result = " + result);
            if (result != null && result.contains("OK")) {
                String prefix = "+EGMR: \"";
                int idx = result.indexOf(prefix);
                if(idx >= 0) {
                    idx += prefix.length();
                    str = result.substring(idx, idx+15);
                    Log.d(TAG, "imei="+str+"\n");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return str;
    }


    public void onReadIMEI(View view) {
        mReadIMEI = true;
        String cmd1 = "AT+EGMR=0,7";
        String cmd2 = "AT+EGMR=0,10";
        sim1_imei = getIMEI(cmd1);
        sim2_imei = getIMEI(cmd2);
        CmdRespText = (EditText) findViewById(R.id.edit_response);
        CmdRespText.setText("" + sim1_imei + "\n" + sim2_imei);
        return ;
    }

   private int checksum(int[] digits) {
       int sum = 0;
       int length = digits.length;
       for (int i=0; i<length; i++) {
           // get digits in order
           int digit = digits[i];
           // every 2nd number multiply with 2
           if (i%2 == 1) {
               digit *= 2;
           }
           //log("index: "+i+" digit: "+digit);
           sum += digit>9?(digit-9):digit;
       }
       return 10-(sum%10);
   }

   private boolean CustomerizeIMEI() {
       boolean bRet = false;
       CmdRespText = (EditText) findViewById(R.id.edit_sn);
       String str = CmdRespText.getText().toString();
       int num = Integer.parseInt(str);
       if(num>0 && num < 999998) {
           sim1_imei = "35007510"+String.format("%06d", num)+"0";
           num = num+1;
           sim2_imei = "35007510"+String.format("%06d", num)+"0";
           bRet = true;
           log("Customerized IMEI:\nIMEI1: "+sim1_imei+"\nIMEI2: "+sim2_imei+"\n");
       } else {
           bRet = false;
           log("The num is over the limitation!");
       }
       return bRet;
   }

   private void CaculateNewIMEI(int flag) {
       if(mReadIMEI) {
           String str = null;
           if(flag == 1) {
               str = sim1_imei;
           } else {
               str = sim2_imei;
           }
           //35007510,gemini ID
           int[]  digits = new int[]{3,5,0,0,7,5,1,0,0,0,0,0,0,0};
           //TAC + FAC + device serial number
           byte[] bytes = str.getBytes();
           for(int i=8; i<14; i++) {
               digits[i] = bytes[i]-48;
               //log("digits["+i+"]"+" = "+digits[i]);
           }
           //check sum
           int lastbyte = checksum(digits);
           if(lastbyte == 10) lastbyte = 0;
           //log("checksum = "+lastbyte);
           //New IMEI number
           StringBuilder sb = new StringBuilder();
           for (int i = 0; i < 14; i++) {
               sb.append(String.valueOf(digits[i]));
           }
           sb.append(String.valueOf(lastbyte));
           if(flag == 1) {
               sim1_imei = ""+sb;
               //log("NEW IMEI: "+sim1_imei);
           } else {
               sim2_imei = ""+sb;
               //log("NEW IMEI: "+sim2_imei);
           }
       }
   }

    public void onChangeIMEI(View view) {
        if(sim1_imei == null || sim2_imei == null) return;
        //////////////////////////////////////////////////
        CmdRespText = (EditText) findViewById(R.id.edit_response);
        CmdRespText.setText("Change IMEI now...........");
        //////////////////////////////////////////////////////
        CustomerizeIMEI();
        ////////////////////First IMEI ///////////////////////
        CaculateNewIMEI(1);

        String cmd = "AT+EGMR=1,7,\"" + sim1_imei + "\"";
        log(""+cmd);
        boolean res = sendAT(cmd);
        if(res) {
            Log.d(TAG, "Change IMEI1 successfully!!");
        }
        ////////////////////Second IMEI /////////////////////
        CaculateNewIMEI(2);

        cmd = "AT+EGMR=1,10,\"" + sim2_imei + "\"";
        log(""+cmd);
        res = sendAT(cmd);
        if(res) {
            Log.d(TAG, "Change IMEI2 successfully!!");
        }
        mReadIMEI = false;
        return ;
    }





    private void log(String msg) {
        Log.d(TAG, "IMEI: " + msg);
    }

}
