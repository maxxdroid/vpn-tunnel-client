package com.max.maxudp;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class MyVpnService extends VpnService {

    private static final String TAG = "MyVpnService";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        if (vpnThread != null) return;

        vpnThread = new Thread(() -> {
            try {
                Builder builder = new Builder();
                builder.setSession("Max UDP VPN")
                        .addAddress("10.0.0.2", 32)      // Fake local address
                        .addDnsServer("8.8.8.8")         // DNS server
                        .addRoute("0.0.0.0", 0);         // Route all traffic

                vpnInterface = builder.establish();
                if (vpnInterface == null) {
                    Log.e(TAG, "Failed to establish VPN interface");
                    return;
                }

                Log.i(TAG, "VPN started");

                // We'll handle traffic here later

            } catch (Exception e) {
                Log.e(TAG, "Error starting VPN: " + e.getMessage(), e);
            }
        });
        vpnThread.start();
    }

    @Override
    public void onDestroy() {
        try {
            if (vpnInterface != null) vpnInterface.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing VPN interface", e);
        }
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }
        Log.i(TAG, "VPN stopped");
    }
}
