# APP内数据抓取--抖音

> 需求：提供查询词，抓抖音的搜索结果，并下载视频文件

## 抓取方案

### 方案

抖音不提供网页端搜索，同时app内搜索时，网络请求包含大量加密参数，破解较困难  
因此采用**安卓模拟器+抓包**的方式进行抓取

### 难点

1. 抖音采用了ssl pinning技术，不接受用户安装的cert证书，无法通过中间人攻击方式抓包
2. 抖音app启动后生成唯一device_id，不随清空app数据、卸载重装、重启等操作改变。单个设备频繁搜索会触发封禁（测试中，即使搜索之间穿插浏览信息流操作，在20词左右就会触发封禁）

## 工具

用于前期APP分析、后期数据抓取，主要包括：
- [fiddler](https://www.telerik.com/download/fiddler) : 抓包分析，（或手机端抓包工具[HttpCanary](https://m.apkpure.com/httpcanary-%E2%80%94-http-sniffer-capture-analysis/com.guoshi.httpcanary)和[PacketCapture](https://m.apkpure.com/packet-capture/app.greyshirts.sslcapture)）
- [jadx](https://github.com/skylot/jadx) : 安卓APP反编译工具
- [adb](https://developer.android.com/studio/command-line/adb) : Android Debug Bridge，与安卓设备通信，可执行拷贝、安装、启动APP操作
- [android studio](https://developer.android.com/studio) : 安卓开发IDE
- [xposed](https://repo.xposed.info/module/de.robv.android.xposed.installer) : 运行时hook，可用来绕过APP的一些限制
- [genymotion](https://www.genymotion.com/download/) : 安卓模拟器
- [virtulbox](https://www.virtualbox.org/wiki/Downloads) : genymotion依赖
- vnc+xfce : VNC可用于远程连接Linux图形界面；genymotion不支持无头模式，必须安装图形界面

## 详细步骤

> 操作系统：CentOS 8  
> 抖音版本：15.1.0

### 开发环境

#### 图形界面

VNC安装：参考[http://websac.sogou/rookie_guide/work_env/#vnc](http://websac.sogou/rookie_guide/work_env/#vnc)

XFCE安装：
```bash
yum groupinstall "Xfce" -y
```

#### Android模拟器

> virtualbox依赖虚拟化技术，无法在虚机运行，请使用实机  
> genymotion依赖的libstdc++.so在centos7上不满足，需自行编译；建议直接使用centos8  

选用genymotion+virtualbox，相比其他模拟器，提供了较丰富的命令行工具，更方便编程操作

virtualbox安装：
```bash
dnf config-manager --add-repo=https://download.virtualbox.org/virtualbox/rpm/el/virtualbox.repo
yum search virtualbox
yum install virtualbox
```

> 若最新版virtualbox卡顿崩溃，可尝试安装6.0及以下版本

> 若virtualbox启动报错：  
> WARNING: The vboxdrv kernel module is not loaded. Either there is no module available for the current kernel (4.18.0-193.el8.x86_64) or it failed to load. If your system is using EFI Secure Boot you may need to sign the kernel modules (vboxdrv, vboxnetflt, vboxnetadp, vboxpci) before you can load them  
> 可尝试安装dkms解决

genymotion安装：[下载](https://www.genymotion.com/download/)linux安装文件，直接运行即可完成安装

#### 开发工具

部分工具仅在开发阶段用到，或开发与运行时所需不同，列举如下：

- 开发环境：
  - fiddler : 抓包，可视化分析
  - Android Studio : 安卓开发IDE
  - adb : 与虚拟机通信，包含在SDK Tools中，可通过Android Studio附带的SDK Manager下载，路径`<sdk_root>/platform-tools/`
  - uiautomatorviewer : Android页面检查工具，同上包含在SDK Tools中，路径`<sdk_root>/tools/`
  - jadx : apk反编译（可选）

- 运行时：
  - mitmproxy : 抓包，可通过pip安装
  - adb : 与虚拟机通信，包含在platform-tools中，[下载](https://developer.android.com/studio/releases/platform-tools)

### APP安装

| 步骤 | 详细 | 
| --- | --- | 
| 创建模拟器 | 在genymotion中创建，Android 8.0版本；完成后启动模拟器，进行后续步骤 | 
| 配置代理 | 设置模拟器，使用fiddler或mitmproxy代理请求，[配置方法](https://www.telerik.com/blogs/how-to-capture-android-traffic-with-fiddler) | 
| 安装genymotion arm translation | arm版APP无法在x86的模拟器上直接运行，需进行转换。安装包可通过adb刷入系统，或直接拖拽安装（最高支持8.0版本，[下载](https://github.com/m9rco/Genymotion_ARM_Translation)） | 
| 安装抖音APP | 官网下载安装即可，可通过adb或拖拽安装 | 
| 安装xposed | 包含[xposed framework](https://dl-xda.xposed.info/framework/)(刷机包)和[xposed installer](https://forum.xda-developers.com/attachments/xposedinstaller_3-1-5-apk.4393082/)(APP)安装方式同上。注：xposed最高支持andorid 8.0，更高版本安卓可使用magisk+edxposed方案，edxposed完全兼容xposded插件 | 
| 安装xposed插件 | xposed插件本质是android APP，这里需要使用xposed实现ssl pinning破解([JustTrustMe](https://github.com/Fuzion24/JustTrustMe))和虚拟mac地址，稍后展开 | 

### 代码实现

代码主要包括以下部分：
- instrument-test : 标准android APP，按照[UI Automator](https://developer.android.com/training/testing/ui-automator)测试框架编写，实际执行启动抖音APP、输入查询词、点击搜索和滚屏动作
- runner : python脚本，用于管理模拟器，在多个模拟器上启动由instrument-test构建的测试APP，从而让测试APP接管对抖音的操作
- macaddr-changer : xposed插件，hook mac地址，重启时随机生成
- keyserver : 查询词放在redis中，由于不清楚app内如何连redis，所以包装成http服务提供查询词（不展开）
- proxy : 基于mitmproxy，记录搜索结果，保存到redis。*注：搜索结果可能是几段json字符串拼接，需解析（不展开）
- spider : 解析proxy的结果，下载视频并存储（不展开）

下面按照实现顺序简要分析

#### Anti-Anti

Xposed是一个运行在Android系统的hook框架，通过对`Zygote`线程的定制，实现了运行时hook方法调用的能力，可实现方法定制甚至替换，详细信息参见[文档](https://github.com/rovo89/XposedBridge/wiki/Development-tutorial)和[API](https://api.xposed.info/reference/packages.html)。这里借助xposde绕过抖音的反爬机制

部分应用通过应用列表、调用栈等手段检测xposed，在安装了xposed的设备上直接闪退，可采用Magisk+EdXposed，用Magisk Hide绕过

##### SSL Pinning

中间人攻击需要在客户端(Android)安装代理颁发的CA证书，安卓7.0以后证书必须安装到系统证书目录下（需ROOT）[教程](https://blog.zhangkunzhi.com/2020/02/10/%E5%AE%89%E5%8D%93%E5%AF%BC%E5%85%A5%E8%AF%81%E4%B9%A6%E5%88%B0%E7%B3%BB%E7%BB%9F%E7%9B%AE%E5%BD%95%E4%B8%AD/index.html)，而部分应用采用了[ssl pinning](http://fiddler.wikidot.com/certpinning)技术，只信任特定证书，表现形式为：即使已将fiddler证书安装到系统证书目录，抓包依然报网络错误或无法解码`Fiddler's HTTPS Decryption feature is enabled, but this specific tunnel was configured not to be decrypted`

通过反编译抖音APP可以看到抖音使用了`okhttp3`包，推测ssl pinning由该包实现，相关类`okhttp3.CertificatePinner`，方法签名：
```java
public void check(String, List)
```
从xposed的模块库里能直接找到破解ssl pinning的模块，这里使用JustTrustMe模块，其hook的核心代码
```java
XposedHelpers.findAndHookMethod("okhttp3.CertificatePinner", classLoader, "check", String.class, List.class, new XC_MethodReplacement() {
  public Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
      return null;
  }
})
```

##### 设备封禁

抖音APP未登录状态下，搜索几次后，后续搜索结果为空。抓包发现请求参数中带有device_id、openudid，推测抖音根据二者标识设备

通常openudid的生成策略为：  
1. 若存储中存在openudid相关文件，直接读取
2. 否则获取android_id，若有效则作为openudid
3. 否则随机生成，并存储供下次读取

清除抖音APP数据后重新启动，新生成的openudid和andorid_id一致，可以判断符合如上策略，因此可通过更换android_id间接更换openudid

device_id值和Android的`Device ID/IMEI`不同，推测device_id根据mac地址和openudid生成（[参考](https://github.com/coder-fly/douyin_device_register)），想要生成新的device_id需要实现更换mac。直接替换mac地址文件存在问题（如导致通过wlan连接的adb断连）且高版本安卓难以替换，因此使用xposed向抖音返回假的可更换的mac地址来解决。

android 6.0及以上获取mac地址方式如下：
```java
public String getMacAddress() throws SocketException {
    Enumeration enumeration = NetworkInterface.getNetworkInterfaces();
    while (enumeration.hasMoreElements()) {
        NetworkInterface networkInterface = (NetworkInterface) enumeration.nextElement();
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
```
需要hook的方法为`java.net.NetworkInterface`类的`getHardwareAddress`，方法签名：
```java
public byte[] getHardwareAddress()
```
现有模块没找到有效的，自行实现，macaddr-changer的xposed入口位于`com.hamster.macaddresschanger.MainXposed`，核心代码：
```java
// 只对抖音生效
if (lpparam.packageName.equals("com.ss.android.ugc.aweme")) {
    findAndHookMethod("java.net.NetworkInterface", lpparam.classLoader, "getHardwareAddress", XC_MethodReplacement.returnConstant(currMac));
}
```

基于以上，可以实现设备被封禁后自动更换标识继续抓取

> 另外抖音似乎针对**多设备同IP**的情况有限制，在同一台linux上起多个模拟器时，只有一到两台模拟器有搜索结果  
> 这里通过在多台开发机运行mitmproxy，将模拟器分别连接到不同mitmproxy实例解决

#### APP自动化控制

常用的连接Android模拟器控制APP的框架有UI Automator和APPium，后者是基于前者的封装支持python但性能较差，因此这里直接选用UI Automator

UI Automator 是一个界面测试框架，适用于整个系统上以及多个已安装应用间的跨应用功能界面测试，通过该框架可以实现页面元素检查、输入、点击、滑动和拖拽等

instrument-test用于控制抖音APP，基于UI Automator，实现了如下操作：
1. 启动抖音
2. 识别启动弹框（位置、存储权限申请）并跳过
3. 点击主页搜索图表进入搜索
4. 通过http请求获取查询词
5. 将查询词设置到输入框
6. 点击查询按钮
7. 向下滚动触发动态加载

其中部分步骤需要等待上个操作完成，通过页面元素检查和等待实现，不详细展开

下面简单介绍一些需要特殊注意的点

1. 页面元素定位  
   通常可使用resource_id定位页面元素(`By.res("pkgName", "resouceId")`)，必要时可加入depth和children的信息进一步限制范围  
   

## adb命令整理

```bash
# 已启动设备列表
adb devices
# 安装APP
adb
# 刷入zip包
adb
```


