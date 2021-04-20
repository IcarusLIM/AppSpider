package com.hamster.androidappspider;

import android.os.RemoteException;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class AppTest {
    private static final String LOG_TAG = "TiktokSearchTest";
    private static final String TARGET_PACKAGE = "com.ss.android.ugc.aweme";
    private UiDevice mDevice;

    @Before
    public void startMainActivityFromHomeScreen() {
        // 解决selector长时间等待视图Idle导致不返回， 0.5s时报“StaleObjectException”
        Configurator.getInstance().setWaitForIdleTimeout(1000);
        mDevice = UiDevice.getInstance(getInstrumentation());
    }


    @Test
    public void closeApp() throws RemoteException {
        mDevice.pressHome();
        mDevice.pressRecentApps();
        BySelector selector = By.res("com.android.systemui", "task_view_bar")
                .hasChild(By.res("com.android.systemui", "title").text("抖音"))
                .hasChild(By.res("com.android.systemui", "dismiss_task"));
        UiObject2 o = mDevice.wait(Until.findObject(selector), 5000);
        if (o != null) {
            o.findObject(By.res("com.android.systemui", "dismiss_task")).click();
        }
        mDevice.pressHome();
    }

    @Test
    public void openApp() {
        mDevice.pressHome();
        BySelector selector = By.pkg("com.android.launcher3").clazz("android.widget.TextView").text("抖音").depth(7, 8);
        UiObject2 appIcon = mDevice.wait(Until.findObject(selector), 1000);
        appIcon.click();
    }
}
