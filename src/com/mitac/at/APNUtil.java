package com.mitac.b71;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.SecurityException;


public class APNUtil {
    public static final String TAG = "APNUtil";
    public static Uri APN_URI = Uri.parse("content://telephony/carriers");
    public static Uri CURRENT_APN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static String mTitle = "";
    private static String mAPN = "";
    private static String mNumeric = ""; //MCC+MNC
    private static String mAPNType = "default"; //default,ia,ims,fota
    private static String mUser = "";
    private static String mPassword = "";
    private static String mAuthType = "-1";//Null,PAP,CHAP,PAP or CHAP(0-3)
    private static String mMvnoType = "";//Mobile Virtual Network Operator
    private static String mMvnoData = "";//Matched data

    //String Format: name#apn#numberic#apn_type#user#password#auth_type#mvno_type#mvno_match_data
    public static boolean ValidateAPN(String str) {
        boolean bRet = true;
        mTitle = "";
        mAPN = "";
        mNumeric = ""; //MCC+MNC
        mAPNType = "default"; //default,ia,ims,fota
        mUser = "";
        mPassword = "";
        mAuthType = "-1";//Null,PAP,CHAP,PAP or CHAP(0-3)
        mMvnoType = "";//Mobile Virtual Network Operator
        mMvnoData = "";//Matched data

        String[] temp = str.split("#");
        for(int i=0; i<temp.length; i++) {
            Log.d(TAG, temp[i]);
        }
        if(temp.length>=2) {
            mTitle = temp[0];
            mAPN = temp[1];
        } else {
            bRet = false;
            Log.d(TAG, "parameter is incomplete !!!");
        }
        if(temp.length>=3) {
            mNumeric = temp[2];
            int mcc = Integer.parseInt(mNumeric.substring(0, 3));
            int mnc = Integer.parseInt(mNumeric.substring(3, mNumeric.length()));
            if(mcc==0 || mnc>999) {
                bRet = false;
                Log.d(TAG, "mcc or mnc is incorrect !!!");
            }
        }
        if(temp.length>=4) {
            if(temp[3].equals("null")){
                mAPNType = "default";
            } else {
                mAPNType = temp[3];
            }
        }
        if(temp.length>=5) {
            if(!temp[4].equals("null")){
                mUser = temp[4];
            }
        }
        if(temp.length>=6) {
            if(!temp[5].equals("null")){
                mPassword = temp[5];
            }
        }
        if(temp.length>=7) {
            mAuthType = temp[6];
            int auth_type = Integer.parseInt(mAuthType);
            if(auth_type<-1 || auth_type>3) {
                bRet = false;
                Log.d(TAG, "authentication type is incorrect !!!");
            }
        }
        if(temp.length>=8) {
            if(!temp[7].equals("null")){
                mMvnoType = temp[7];
            }
        }
        if(temp.length>=9) {
            if(!temp[8].equals("null")){
                mMvnoData = temp[8];
            }
        }
        if(temp.length>=10) {
            bRet = false;
            Log.d(TAG, "total length of parameter is incorrect !!!");
        }

        return bRet;
    }

    public static void customizeAPN(Context context) {
        int apn_id = getAPN(context, mAPN);
        if(apn_id == -1){
            apn_id = addAPN(context);
            if(apn_id == -1){
                Log.e(TAG, "faile to set APN");
            }
        }

        if(apn_id != -1){
            setAPN(context, apn_id);
        }
    }

    public static int addAPN(Context context) throws SecurityException {
        int id = -1;
        String NUMERIC = mNumeric;//getSIMInfo(context);
        if (NUMERIC == null) {
            return -1;
        }

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("name", mTitle);                                     //APN title/name
        values.put("apn", mAPN);                                        //APN
        values.put("type", mAPNType);                                   //APN type
        values.put("numeric", NUMERIC);                                 //NUMERIC
        values.put("mcc", NUMERIC.substring(0, 3));                     //MCC: Mobile Country Code
        values.put("mnc", NUMERIC.substring(3, NUMERIC.length()));      //MNC: Mobile Network Code
        values.put("proxy", "");                                        //proxy
        values.put("port", "");                                         //port
        values.put("mmsproxy", "");                                     //MMS proxy
        values.put("mmsport", "");                                      //MMS port
        values.put("user", mUser);                                      //user name
        values.put("server", "");                                       //server
        values.put("password", mPassword);                              //password
        values.put("mmsc", "");                                         //MMSC
        values.put("authtype", mAuthType);                              //Authentication type
        values.put("mvno_type", mMvnoType);                             //MVNO type
        values.put("mvno_match_data", mMvnoData);                       //MVNO match data
        values.put("protocol", "IPV4V6");                               //protocol
        values.put("roaming_protocol", "IPV4V6");                       //roaming protocol
        Cursor c = null;
        Uri newRow = resolver.insert(APN_URI, values);
        if (newRow != null) {
            c = resolver.query(newRow, null, null, null, null);
            int idIndex = c.getColumnIndex("_id");
            c.moveToFirst();
            id = c.getShort(idIndex);
        }
        if (c != null)
            c.close();

        return id;
    }



    public static String getSIMInfo(Context context) {
        TelephonyManager iPhoneManager = (TelephonyManager)context
            .getSystemService(Context.TELEPHONY_SERVICE);
        return iPhoneManager.getSimOperator();
    }


    //Set APN
    public static void setAPN(Context context, int id) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("apn_id", id);
        resolver.update(CURRENT_APN_URI, values, null, null);
    }



    public static int getAPN(Context context, String apn){
        ContentResolver resolver = context.getContentResolver();
        String apn_condition = String.format("apn like '%%%s%%'", apn);
        Cursor c = resolver.query(APN_URI, null, apn_condition, null, null);

        //If APN exists
        if (c != null && c.moveToNext()) {
            int id = c.getShort(c.getColumnIndex("_id"));
            String name1 = c.getString(c.getColumnIndex("name"));
            String apn1 = c.getString(c.getColumnIndex("apn"));

            Log.e(TAG, "APN has exist\nAPN: "+ id +" "+ name1 +" "+ apn1);
            return id;
        }

        return -1;
    }
}
