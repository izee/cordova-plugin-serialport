package me.izee.cordova.plugin;

import android_serialport_api.SerialPort;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * This class echoes a string called from JavaScript.
 */
public class NativeSerial extends CordovaPlugin {

  private SerialPort port;
  private CallbackContext watchCallback;
  private Thread watchThread;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

    switch (action) {
      case "open": {
        final String device = args.getString(0);
        final int rate = args.getInt(1);
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            NativeSerial.this.openPort(device, rate, callbackContext);
          }
        });
        return true;
      }
      case "close":
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            NativeSerial.this.closePort(callbackContext);
          }
        });
        return true;
      case "write": {
        final String data = args.getString(0);
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            NativeSerial.this.write(data, callbackContext);
          }
        });
        return true;
      }
      case "watch": {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            NativeSerial.this.registerWatcher(callbackContext);
          }
        });
        return true;
      }
    }
    return false;
  }

  private void registerWatcher(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        while (!Thread.currentThread().isInterrupted()) {
          if (NativeSerial.this.port == null) {
            try {
              Thread.sleep(5000);
              continue;
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          InputStream inputStream = NativeSerial.this.port.getInputStream();
          byte[] buffer = new byte[1024];
          if (inputStream == null) continue;
          try {
            int size = inputStream.read(buffer);
            if (size > 0) {
//              PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, new String(buffer, 0, size));
              PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, Arrays.copyOf(buffer,size) );
              pluginResult.setKeepCallback(true);
              callbackContext.sendPluginResult(pluginResult);
            }
          } catch (Exception e) {
            e.printStackTrace();
            PluginResult error = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            error.setKeepCallback(true);
            callbackContext.sendPluginResult(error);
          }
        }
      }
    });
  }

  private class watchThread extends Thread {
    @Override
    public void run() {
      while (true) {
//          InputStream inputStream = NativeSerial.this.port.getInputStream();
//          byte[] buffer = new byte[64];
//          if (inputStream == null) return;
        try {
//            int size = inputStream.read(buffer);
//            if (size > 0) {
//              callbackContext.success(new String(buffer));
//            }
          NativeSerial.this.watchCallback.success(Boolean.toString(Thread.currentThread().isInterrupted()));
        } catch (Exception e) {
          e.printStackTrace();
          NativeSerial.this.watchCallback.error(e.getMessage());
        }
      }
    }
  }

  private void openPort(String device, int rate, CallbackContext callbackContext) {
    try {
      NativeSerial.this.port = new SerialPort(new File(device), rate, 0);
    } catch (IOException e) {
      if (NativeSerial.this.port != null) {
        NativeSerial.this.port.close();
        NativeSerial.this.port = null;
      }
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
    callbackContext.success();
  }

  private void closePort(final CallbackContext callbackContext) {
    if (this.port != null) {
      this.port.close();
      this.port = null;
    }
    callbackContext.success();
  }

  private void write(final String data, final CallbackContext callbackContext) {
    try {
      NativeSerial.this.port.getOutputStream().write(data.getBytes());
      callbackContext.success();
    } catch (IOException e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
  }
}
