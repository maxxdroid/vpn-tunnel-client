package com.max.maxudp;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private static final int SERVER_PORT = 9050;
    private static final String SERVER_IP = "178.128.195.163"; // Your DO IP
    private static final int VPN_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText inputText = findViewById(R.id.inputText);
        Button sendButton = findViewById(R.id.sendButton);
        Button startVpnButton = findViewById(R.id.startVpnButton);
        Button stopVpnButton = findViewById(R.id.stopVpnButton);
        TextView responseText = findViewById(R.id.responseText);

        sendButton.setOnClickListener(v -> {
            String userInput = inputText.getText().toString().trim();
            if (!userInput.startsWith("GET ")) {
                userInput = "GET " + userInput;
            }

            String finalUserInput = userInput;
            new Thread(() -> {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                    byte[] buf = finalUserInput.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, SERVER_PORT);
                    socket.send(packet);

                    byte[] responseBuf = new byte[4096];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
                    socket.setSoTimeout(5000); // 5s timeout
                    socket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    runOnUiThread(() -> responseText.setText(response));

                    socket.close();
                } catch (Exception e) {
                    runOnUiThread(() -> responseText.setText("❌ Error: " + e.getMessage()));
                }
            }).start();
        });

        startVpnButton.setOnClickListener(v -> {
            Intent vpnIntent = VpnService.prepare(this);
            if (vpnIntent != null) {
                startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
            } else {
                onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
            }
        });

        stopVpnButton.setOnClickListener(v ->
                stopService(new Intent(this, MyVpnService.class))
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startService(new Intent(this, MyVpnService.class));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
