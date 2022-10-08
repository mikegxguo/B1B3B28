package com.mitac.b71;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.SystemProperties;

import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
//import java.io.FileReader;
import java.io.FileWriter;




public class at extends Service {
	private static final String TAG = "ATService";
	public static final String EXTRA_EVENT = "imei";
  private String sim1_imei = null;
  private String sim2_imei = null;

	private ModemTool mTool;

	@Override
	public void onCreate() {
      super.onCreate();
      mTool = new ModemTool();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
      String imei = intent == null ? "" : intent.getStringExtra(EXTRA_EVENT);
      Log.d(TAG, "onStartCommand : imei = " + imei);
      handleParameter(imei);
      stopSelf(startId);
      return START_NOT_STICKY;
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


  private boolean ValidateIMEI(String str) {
      boolean bRet = false;
      String TAC = str.substring(0, 8);//Don't include the character(end_index)
      String SN = str.substring(8,14);
      int num = Integer.parseInt(SN);
      String CKSUM = str.substring(14, 15);
      Log.d(TAG, "TAC: "+TAC);
      Log.d(TAG, "SN: "+SN);
      Log.d(TAG, "length: "+str.length());
      Log.d(TAG, "CKSUM: "+CKSUM);
      if((num>=0 && num < 999998) && str.length()==15) {
          int[]  digits = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0};
          //TAC + device serial number
          byte[] bytes = str.getBytes();
          for(int i=0; i<14; i++) {
              digits[i] = bytes[i]-48;
              //Log.d(TAG, "digits["+i+"]"+" = "+digits[i]);
              if(digits[i]<0 || digits[i]>9) break;
          }
          //check sum
          int lastbyte = checksum(digits);
          if(lastbyte == 10) lastbyte = 0;
          Log.d(TAG, "calculated check sum = "+lastbyte);
          int cksum = Integer.parseInt(CKSUM);
          if(cksum == lastbyte) {
              Log.d(TAG, "CKSUM is correct");
              bRet = true;
          } else {
              Log.d(TAG, "CKSUM isn't correct");
          }
      }
      return bRet;
  }

  private void SaveTempFile(String str){
      //Save result to one log file.
      try {
          //BufferedReader br = null;
          BufferedWriter bw = null;
          FileWriter fw = null;
          String file = "/mnt/sdcard/imei.cfg";
          Log.d(TAG, "str: "+str);
          bw = new BufferedWriter(new FileWriter(file), 10*1024);
          if(bw != null){
              bw.write(str.toCharArray());
              bw.close();
          }
      } catch (Exception e) {
          Log.e(TAG, "SaveTempFile Error", e);
      }
  }

  private void handleParameter(String imei) {
      try {
          if(imei.contains("1,r")) {
              String cmd1 = "AT+EGMR=0,7";
              sim1_imei = getIMEI(cmd1);
              SaveTempFile("imei1 "+sim1_imei+"\n");
          } else if(imei.contains("2,r")) {
              String cmd2 = "AT+EGMR=0,10";
              sim2_imei = getIMEI(cmd2);
              SaveTempFile("imei2 "+sim2_imei+"\n");
          } else if(imei.contains("1,w,")) {
              sim1_imei = imei.substring(4, imei.length());
              if(ValidateIMEI(sim1_imei)) {
                  String cmd = "AT+EGMR=1,7,\"" + sim1_imei + "\"";
                  Log.d(TAG, ""+cmd);
                  boolean res = sendAT(cmd);
                  if(res) {
                      Log.d(TAG, "Change IMEI1 successfully!!");
                  }
              }
          } else if(imei.contains("2,w,")) {
              sim2_imei = imei.substring(4, imei.length());
              if(ValidateIMEI(sim2_imei)) {
                  String cmd = "AT+EGMR=1,10,\"" + sim2_imei + "\"";
                  Log.d(TAG, ""+cmd);
                  boolean res = sendAT(cmd);
                  if(res) {
                      Log.d(TAG, "Change IMEI2 successfully!!");
                  }
              }
          }
      } catch (Exception e) {
          Log.e(TAG, "SendAT Error", e);
      } finally {
          //do nothing here
      }
  }


  private boolean sendAT(String cmd) {
      boolean ret = false;
      try {
          String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
          Log.d(TAG, "sendAT : cmd = " + cmd + "\nresult : " + result);
          if (result != null && result.contains("OK")) {
              ret = true;
          }
      } catch (Exception e) {
          Log.e(TAG, "SendAT Error", e);
      }
      return ret;
  }



  private String getIMEI(String cmd) {
      String str = null;
      try {
          String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
          Log.d(TAG, "sendAT : cmd = " + cmd + "\nresult : " + result);
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

	@Override
	public IBinder onBind(Intent intent) {
      return null;
	}

  @Override
	public void onDestroy() {
      super.onDestroy();
      Log.d(TAG, "onDestroy");
	}

}
