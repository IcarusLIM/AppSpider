package com.hamster.macaddresschanger;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Random;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainXposed implements IXposedHookLoadPackage {

    private static byte[] currMac = getRandomMac();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("Loaded app:" + lpparam.packageName);
        // hook 本插件，使在插件内可看到hook后的mac地址
        if (lpparam.packageName.equals("com.hamster.macaddresschanger")) {
            findAndHookMethod("com.hamster.macaddresschanger.MainActivity", lpparam.classLoader, "getMacAddress", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult("Hooked: " + param.getResult());
                }
            });
            findAndHookMethod("java.net.NetworkInterface", lpparam.classLoader, "getHardwareAddress", XC_MethodReplacement.returnConstant(currMac));
        }
        // hook 抖音
        if (lpparam.packageName.equals("com.ss.android.ugc.aweme")) {
            findAndHookMethod("java.net.NetworkInterface", lpparam.classLoader, "getHardwareAddress", XC_MethodReplacement.returnConstant(currMac));
        }
    }

    public static byte[] getRandomMac() {
        Random random = new Random();
        // genymotion: 08:00:27:6a:c7:cf
        byte[] mac = new byte[6];
        mac[0] = 8;
        mac[1] = 0;
        mac[2] = 39;
        for (int i = 3; i < mac.length; i++) {
            mac[i] = (byte) random.nextInt();
        }
        XposedBridge.log("New random mac: " + Arrays.toString(mac));
        return mac;
    }

}
