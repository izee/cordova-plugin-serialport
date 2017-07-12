package me.izee.cordova.plugin;

import android.util.Base64;
import android.util.Log;
import android_serialport_api.SerialPort;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * This class echoes a string called from JavaScript.
 */
public class NativeSerial extends CordovaPlugin {
  private static final String LOG_TAG = "NativeSerial";
  private SerialPort port;
  private List<CallbackContext> watchers = new LinkedList<CallbackContext>();
  private Future futureWatch;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

    if (action.equals("open")) {
      Log.d(LOG_TAG, "execute open");
      final String device = args.getString(0);
      final int rate = args.getInt(1);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          NativeSerial.this.openPort(device, rate, callbackContext);
          NativeSerial.this.startWatch();
        }
      });
      return true;
    } else if (action.equals("close")) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          NativeSerial.this.closePort(callbackContext);
        }
      });
      return true;
    } else if (action.equals("write")) {
      final String data = args.getString(0);
      Log.d(LOG_TAG, "execute write:" + data);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          byte[] decode = Base64.decode(data, Base64.NO_WRAP);
          Log.d(LOG_TAG, "write bytes:" + decode);
          NativeSerial.this.writeBytes(decode, callbackContext);
        }
      });
      return true;
    } else if (action.equals("writeText")) {
      final String data = args.getString(0);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          NativeSerial.this.writeText(data, callbackContext);
        }
      });
      return true;
    } else if (action.equals("register")) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          watchers.add(callbackContext);
          NativeSerial.this.startWatch();
        }
      });
      return true;
    }
    Log.d(LOG_TAG, "unknown action:" + action);
    return false;
  }

  private synchronized void startWatch() {
    if (futureWatch != null && !(futureWatch.isDone() || futureWatch.isCancelled())) {
      return;
    }
    futureWatch = cordova.getThreadPool().submit(new Runnable() {
      public void run() {
        Log.d(LOG_TAG, "watch start run");
        while (!Thread.currentThread().isInterrupted()) {
          if (NativeSerial.this.port == null) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          InputStream inputStream = NativeSerial.this.port.getInputStream();

          if (inputStream == null) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          try {
            byte[] buffer = new byte[64];
            int size = inputStream.read(buffer);
            if (size > 0) {
              Log.d(LOG_TAG, String.format("%s,got input:%s", System.currentTimeMillis(), size));
//              PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, new String(buffer, 0, size));
              byte[] data = Arrays.copyOf(buffer, size);
              PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
              pluginResult.setKeepCallback(true);
              for (CallbackContext watcher : watchers) {
                watcher.sendPluginResult(pluginResult);
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
            PluginResult error = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            error.setKeepCallback(true);
            for (CallbackContext watcher : watchers) {
              watcher.sendPluginResult(error);
            }
          }
        }
      }
    });
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

  private void writeBytes(final byte[] bytes, final CallbackContext callbackContext) {
    try {
      OutputStream outputStream = NativeSerial.this.port.getOutputStream();
      outputStream.write(10);
      outputStream.write(13);
      outputStream.write(bytes);
      callbackContext.success();
    } catch (IOException e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
  }

  private void writeText(final String data, final CallbackContext callbackContext) {
    try {
      NativeSerial.this.port.getOutputStream().write(data.getBytes());
      callbackContext.success();
    } catch (IOException e) {
      e.printStackTrace();
      callbackContext.error(e.getMessage());
    }
  }
}
