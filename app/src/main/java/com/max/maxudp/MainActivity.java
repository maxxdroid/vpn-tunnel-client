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
    private static final String SERVER_IP = "178.128.195.163"; // Your server IP
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

                    socket.setSoTimeout(3000); // Max wait time for each chunk

                    StringBuilder[] chunks = null;
                    int expectedChunks = -1;
                    int receivedChunks = 0;

                    while (true) {
                        byte[] responseBuf = new byte[4096];
                        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);

                        try {
                            socket.receive(responsePacket);
                        } catch (Exception timeout) {
                            break; // No more packets
                        }

                        String chunkData = new String(responsePacket.getData(), 0, responsePacket.getLength());

                        if (!chunkData.startsWith("[CHUNK")) continue;

                        int headerEnd = chunkData.indexOf("]\n");
                        if (headerEnd == -1) continue;

                        String header = chunkData.substring(0, headerEnd + 1); // e.g. [CHUNK 1/5]
                        String content = chunkData.substring(headerEnd + 2);

                        String[] parts = header.replace("[CHUNK ", "").replace("]", "").split("/");
                        int chunkIndex = Integer.parseInt(parts[0].trim()) - 1;
                        int totalChunks = Integer.parseInt(parts[1].trim());

                        if (chunks == null) {
                            expectedChunks = totalChunks;
                            chunks = new StringBuilder[expectedChunks];
                        }

                        if (chunkIndex < 0 || chunkIndex >= expectedChunks) continue;

                        if (chunks[chunkIndex] == null) {
                            chunks[chunkIndex] = new StringBuilder(content);
                            receivedChunks++;
                        }

                        if (receivedChunks >= expectedChunks) break;
                    }

                    StringBuilder fullResponse = new StringBuilder();
                    if (chunks != null) {
                        for (int i = 0; i < expectedChunks; i++) {
                            if (chunks[i] != null) {
                                fullResponse.append(chunks[i]);
                            }
                        }
                    }

                    String response = fullResponse.toString();
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
