package com.hamster.androidappspider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import androidx.test.uiautomator.StaleObjectException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TiktokSearchTest {
    private static final String LOG_TAG = "TiktokSearchTest";

    private static final String TARGET_PACKAGE = "com.ss.android.ugc.aweme";
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int LAUNCH_TIMEOUT = 5000;

    private UiDevice mDevice;
    private int height;
    private int width;

    private OkHttpClient client = new OkHttpClient();
    private static final String KEY_SERVER = "http://10.160.33.75:4396";
    private Random random = new Random();

    @Before
    public void startMainActivityFromHomeScreen() {
        // 解决selector长时间等待视图Idle导致不返回， 0.5s时报“StaleObjectException”
        Configurator.getInstance().setWaitForIdleTimeout(1000);
        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(getInstrumentation());
        height = mDevice.getDisplayHeight();
        width = mDevice.getDisplayWidth();

        mDevice.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        launchAndWaitApp();
    }

    @After
    public void cleanUp() throws RemoteException {
        closeApp();
    }

    @Test
    public void walkAround() {
        String key = null;
        int failCount = 0;
        int hangoutCountdown = 3;
        while (true) {
            if (failCount == 0) {
                key = getSearchKey();
            }
            if (key == null) {
                break;
            }
            boolean isSucceed = doSearch(key);
//            boolean isSucceed = true;
            if (!isSucceed) {
                failCount++;
                if (failCount > 5) {
                    restoreSearchKey(key);
                    throw new RuntimeException("DeviceScrapped");
                }
                restartApp((failCount / 3) == 0);
                continue;
            }
            failCount = 0;
            hangoutCountdown--;
            if (hangoutCountdown <= 0) {
                doHangOut();
                hangoutCountdown = 2 + random.nextInt(1);
            }
        }
    }

    private void launchApp() {
        // Solution A:
//        Context context = getApplicationContext();
//        final Intent intent = context.getPackageManager()
//                .getLaunchIntentForPackage(TARGET_PACKAGE);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
//        context.startActivity(intent);

        // Solution B:
        mDevice.pressHome();
        BySelector selector = By.pkg("com.android.launcher3").clazz("android.widget.TextView").text("抖音").depth(7, 8);
        UiObject2 appIcon = mDevice.wait(Until.findObject(selector), 1000);
        if (appIcon != null) {
            appIcon.click();
        }
    }

    private void closeApp() throws RemoteException {
        // Solution A:
        // mDevice.executeShellCommand("am force-stop " + TARGET_PACKAGE);

        // Solution B:
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

    private void restartApp(boolean clearData) {
        try {
            closeApp();
            if (clearData) {
                mDevice.executeShellCommand("pm clear " + TARGET_PACKAGE);
            }
        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
        SystemClock.sleep(1000);
        launchAndWaitApp();
    }

    /**
     * 弹窗Resource表：
     * bco 个人信息保护指引
     * kls 上滑查看更多视频
     * eeh 儿童、青少年使用须知
     * ejw 检测更新，以后再说
     * com.android.packageinstaller:id/permission_allow_button 系统权限申请弹窗
     */
    private void launchAndWaitApp() {
        BySelector[] selectors = {
                By.res(TARGET_PACKAGE, "l__"),
                By.res(TARGET_PACKAGE, "bco"),
                By.res(TARGET_PACKAGE, "eeh"),
                By.res(TARGET_PACKAGE, "ejw"),
                By.res("com.android.packageinstaller", "permission_allow_button")
        };
        BySelector foundSelector = null;
        // try start app with retry
        for (int i = 0; i < 3 && foundSelector == null; i++) {
            launchApp();
            mDevice.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), LAUNCH_TIMEOUT);
            foundSelector = waitOne(selectors, 25000, 1000, 0);
            if (foundSelector == null) {
                try {
                    closeApp();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                SystemClock.sleep(2000);
            }
        }
        if (foundSelector == null) {
            throw new RuntimeException("BootMayNotFinish");
        }
        Log.i(LOG_TAG, "APP boot finish");

        BySelector[] popUpSelectors = Arrays.copyOfRange(selectors, 1, selectors.length);
        while ((foundSelector = waitOne(popUpSelectors, 3000, 500, 500)) != null) {
            Log.i(LOG_TAG, "found popup: " + foundSelector.toString());
            mDevice.findObject((foundSelector)).click();
        }
        // 上滑两次，跳过引导
        Log.i(LOG_TAG, "swipe before search");
        scrollRecommend();
        SystemClock.sleep(500);
        scrollRecommend();
    }

    /**
     * Resource Id表：
     * dy3 呼出搜索按钮
     * aia 搜索框
     * kig 取消搜索按钮/搜索按钮
     * qw 清空搜索框
     */
    private boolean doSearch(String key) {
        UiObject2 searchInput = mDevice.findObject(By.res(TARGET_PACKAGE, "aia"));
        if (searchInput == null) {
            mDevice.wait(Until.findObject(By.res(TARGET_PACKAGE, "dy3")), DEFAULT_TIMEOUT).click();
            searchInput = mDevice.wait(Until.findObject(By.res(TARGET_PACKAGE, "aia")), DEFAULT_TIMEOUT);
        }
        UiObject2 searchInputClearBtn = mDevice.findObject(By.res(TARGET_PACKAGE, "qw"));
        if (searchInputClearBtn != null) {
            searchInputClearBtn.click();
            SystemClock.sleep(100 + random.nextInt(200));
        }
        searchInput.click();
        searchInput.setText(key);
        SystemClock.sleep(100 + random.nextInt(100));
        mDevice.findObject(By.res(TARGET_PACKAGE, "kig")).click();

        // select scrollable video node that has valid content/child
        mDevice.wait(Until.hasObject(By.res(TARGET_PACKAGE, "exk")), 10000);
        UiObject2 searchResult = mDevice.wait(Until.findObject(By.res(TARGET_PACKAGE, "exk").hasChild(By.pkg(TARGET_PACKAGE))), 6000);
        if (searchResult != null) {
            for (int i = 0; i < 4; i++) {
                try {
                    searchResult.scroll(Direction.DOWN, 2 + random.nextFloat() * 0.2f, 4000 + random.nextInt(1000));
                } catch (StaleObjectException e) {
                    // ignore androidx.test.uiautomator.StaleObjectException
                }
                SystemClock.sleep(100 + random.nextInt(200));
            }
            return true;
        } else {
            return false;
        }
    }

    private void doHangOut() {
        Log.d(LOG_TAG, "hangout start");
        for (int i = 0; i < 3; i++) {
            UiObject2 goSearchBtn = mDevice.findObject(By.res(TARGET_PACKAGE, "dy3"));
            if (goSearchBtn != null) {
                break;
            }
            SystemClock.sleep(500);
            UiObject2 cancelSearchBtn = mDevice.findObject(By.res(TARGET_PACKAGE, "kig"));
            if (cancelSearchBtn != null) {
                cancelSearchBtn.click();
            }
        }
        for (int i = 0; i < 1 + random.nextInt(1); i++) {
            scrollRecommend();
            SystemClock.sleep(500 + random.nextInt(1000));
        }
        Log.d(LOG_TAG, "hangout end");
    }


    private String getSearchKey() {
        Request request = new Request.Builder().url(KEY_SERVER).build();
        try {
            Response response = client.newCall(request).execute();
            if (response.code() != 200) {
                return null;
            }
            return response.body().string();
        } catch (IOException e) {
        }
        return null;
    }

    private void restoreSearchKey(String key) {
        HttpUrl.Builder builder = HttpUrl.parse(KEY_SERVER).newBuilder();
        builder.addQueryParameter("restore", key);
        Request request = new Request.Builder().url(builder.build()).build();
        try {
            client.newCall(request).execute();
        } catch (IOException e) {
        }
    }

    private void scrollRecommend() {
        int startR = height / 16 - random.nextInt(height / 8);
        int endR = height / 16 - random.nextInt(height / 8);
        mDevice.swipe(width / 2, height / 4 * 3 + startR, width / 2, height / 4 + endR, 6 + random.nextInt(4));
    }

    /**
     * Uses package manager to find the package name of the device launcher. Usually this package
     * is "com.android.launcher" but can be different at times. This is a generic solution which
     * works on all platforms.`
     */
    private String getLauncherPackageName() {
        // Create launcher Intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        // Use PackageManager to get the launcher package name
        PackageManager pm = getApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    private BySelector waitOne(BySelector[] selectors, long timeout, long interval, long minWait) {
        long startTime = SystemClock.uptimeMillis();

        BySelector hasOne = null;
        for (long elapsedTime = 0; hasOne == null; elapsedTime = SystemClock.uptimeMillis() - startTime) {
            if (elapsedTime >= timeout) {
                break;
            }

            SystemClock.sleep(interval);
            for (int i = 0; i < selectors.length; i++) {
                if (mDevice.hasObject(selectors[i])) {
                    hasOne = selectors[i];
                    break;
                }
            }
        }

        long cost = SystemClock.uptimeMillis() - startTime;
        if (cost < minWait) {
            SystemClock.sleep(minWait - cost);
        }
        return hasOne;
    }
}
