package com.hamster.macaddresschanger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void refreshMac(View view) {
        TextView textView = (TextView) findViewById(R.id.plainText);
        try {
            textView.setText(getMacAddress());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void changeMac(View view) {
        Toast.makeText(this, "Not implement yet", Toast.LENGTH_LONG).show();
    }

    public String getMacAddress() throws SocketException {
        Enumeration enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = (NetworkInterface) enumeration.nextElement();
            //获取硬件地址，一般是MAC
            byte[] arrayOfByte = networkInterface.getHardwareAddress();
            if (arrayOfByte == null || arrayOfByte.length == 0) {
                continue;
            }
            if (!networkInterface.getName().equals("wlan0")) {
                continue;
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : arrayOfByte) {
                stringBuilder.append(String.format("%02X:", new Object[]{Byte.valueOf(b)}));
            }
            if (stringBuilder.length() > 0) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            return stringBuilder.toString();
        }
        return null;
    }
}