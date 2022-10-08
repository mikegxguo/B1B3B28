package com.mitac.b71;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;


public class api extends Service {
    private static final String TAG = "LTE_API";
    public static final String EXTRA_EVENT_APN = "apn";
    public static final String EXTRA_EVENT_MODEM = "modem";


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String apn = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_APN);
        if(apn != null) {
            Log.d(TAG, "onStartCommand : apn = " + apn);
        }
        String modem = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_MODEM);
        if(modem != null) {
            Log.d(TAG, "onStartCommand :  modem = " + modem);
        }
        if(apn!=null && APNUtil.ValidateAPN(apn)){
            APNUtil.customizeAPN(api.this);
        } else if(modem!=null && modem.equals("get_ver")) {
            String adsp_ver = getAdspVer();
            Log.d(TAG, "ADSP version: " + adsp_ver);
            String baseband = SystemProperties.get("persist.radio.version.baseband");
            String[] temp = baseband.split(",");
            SystemProperties.set("persist.sys.fw.version", temp[0]+","+adsp_ver);
        }
        stopSelf(startId);
        return START_NOT_STICKY;
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
