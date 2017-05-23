package org.cloudsky.cordovaPlugins;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;

import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import org.cloudsky.cordovaPlugins.ZBarScannerActivity;

public class ZBar extends CordovaPlugin{

    //permissions
    private String [] permissions = { Manifest.permission.CAMERA };


    // Configuration ---------------------------------------------------

    private static int SCAN_CODE = 1;


    // State -----------------------------------------------------------

    private boolean isInProgress = false;
    private CallbackContext scanCallbackContext;

    private JSONObject params;
    private static final String LOG_TAG = "BarcodeScanner";

    // Plugin API ------------------------------------------------------

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext)
    throws JSONException
    {
        scanCallbackContext = callbackContext;
        params = args.optJSONObject(0);

        if(hasPermission()){
            if(action.equals("scan")) {
                if(isInProgress) {
                    callbackContext.error("A scan is already in progress!");
                } else {

                    isInProgress = true;
                    createScanActivity();
                }
            } else {
                return false;
            }
        }else{
            requestPermissions(0);
        }

        return true;
    }

    private void createScanActivity(){

         Context appCtx = cordova.getActivity().getApplicationContext();
         Intent scanIntent = new Intent(appCtx, ZBarScannerActivity.class);
         scanIntent.putExtra(ZBarScannerActivity.EXTRA_PARAMS, params.toString());
         cordova.startActivityForResult(this, scanIntent, SCAN_CODE);

    }

    /**
    * check application's permissions
    */
    public boolean hasPermission() {
       for(String p : permissions)
       {
           if(!PermissionHelper.hasPermission(this, p))
           {
               return false;
           }
       }
       return true;
    }

    /**
    * We override this so that we can access the permissions variable, which no longer exists in
    * the parent class, since we can't initialize it reliably in the constructor!
    *
    * @param requestCode The code to get request action
    */
    public void requestPermissions(int requestCode)
    {
        PermissionHelper.requestPermissions(this, requestCode, permissions);
    }

    /**
    * processes the result of permission request
    *
    * @param requestCode The code to get request action
    * @param permissions The collection of permissions
    * @param grantResults The result of grant
    */
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                     int[] grantResults) throws JSONException
    {

        PluginResult result;
        Boolean hasAllPermissions = true;

        for (int r : grantResults) {

            if (r == PackageManager.PERMISSION_DENIED) {
                hasAllPermissions = false;
                scanCallbackContext.error("Unknown error");
            }
        }

        if(hasAllPermissions){
            createScanActivity();
        }
   }


    // External results handler ----------------------------------------

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent result)
    {
        if(requestCode == SCAN_CODE) {
            switch(resultCode) {
                case Activity.RESULT_OK:
                    String barcodeValue = result.getStringExtra(ZBarScannerActivity.EXTRA_QRVALUE);
                    scanCallbackContext.success(barcodeValue);
                    break;
                case Activity.RESULT_CANCELED:

                    int cancelledValue = result.getIntExtra(ZBarScannerActivity.EXTRA_CANCELLED, 0);

                    if(cancelledValue == 0){
                        scanCallbackContext.error("cancelled");
                    }else{
                        scanCallbackContext.error("add_manually");
                    }
                    break;
                case ZBarScannerActivity.RESULT_ERROR:
                    scanCallbackContext.error("Scan failed due to an error");
                    break;
                default:
                    scanCallbackContext.error("Unknown error");
            }
            isInProgress = false;
            scanCallbackContext = null;
        }
    }
}
