package com.quectel.modemtool;

public class ModemTool{
    public static final String TAG = "ModemTool-local";
    //String ttydevice =  "/dev/smd11"; //For device using android8 and before
    String ttydevice = "/dev/smd8";//For device using android9 and after

    // Used to load the 'qlmodem' library on application startup.
    static {
        System.loadLibrary("qlmodem");
        //System.loadLibrary("native-lib");
    }

    public String sendAtCommand(int commandId, String atCommand) {
        return sendAtCommandtty(commandId,atCommand,ttydevice);
    }

    /**
     * A native method that is implemented by the 'qlmodem' native library,
     * which is a common api that can be used to send AT command to write nv.
     * @param commandId  which listed in {@link NvConstants} and start with REQUEST_COMMON_COMMAND_* .
     * @param atCommand treat the whole at command as a parameter.
     * @return  NULL: fail to send at command
     *          contain "ERROR" : at comand send successfully, but not get the result correctly;
     *          contain "OK" : get the result correctly.
     */
    //public native String sendAtCommand(int commandId, String atCommand);
    public native String sendAtCommandtty(int commandId, String atCommand,String ttydevice);
}
