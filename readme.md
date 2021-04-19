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
- [fiddler](https://www.telerik.com/download/fiddler) : 抓包分析
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
  - uiautomatorviewer: Android页面检查工具，同上包含在SDK Tools中，路径`<sdk_root>/tools/`
  - jadx : apk反编译（可选）

- 运行时：
  - mitmproxy : 抓包，可通过pip安装
  - adb : 与虚拟机通信，包含在platform-tools中，[下载](https://developer.android.com/studio/releases/platform-tools)

### APP安装

| 步骤 | 详细 | 
| --- | --- | 
| 创建模拟器 | 在genymotion中创建，Android 8.0版本；完成后启动模拟器，进行后续步骤 | 
| 配置代理 | 设置模拟器，使用fiddler或mitmproxy代理请求，[配置方法](https://support.google.com/android/answer/9654714?hl=zh-Hans#zippy=%2C%E8%AE%BE%E7%BD%AE%E4%BB%A3%E7%90%86%E4%BB%A5%E8%BF%9E%E6%8E%A5%E6%89%8B%E6%9C%BA) | 
| 安装genymotion arm translation | arm版APP无法在x86的模拟器上直接运行，需进行转换。安装包可通过adb刷入系统，或直接拖拽安装（最高支持8.0版本，[下载](https://github.com/m9rco/Genymotion_ARM_Translation)） | 
| 安装抖音APP | 官网下载安装即可，可通过adb或拖拽安装 | 
| 安装xposed | 包含[xposed framework](https://dl-xda.xposed.info/framework/)(刷机包)和[xposed installer](https://forum.xda-developers.com/attachments/xposedinstaller_3-1-5-apk.4393082/)(APP)安装方式同上。注：xposed最高支持andorid 8.0，更高版本安卓可使用magisk+edxposed方案，edxposed完全兼容xposded插件 | 
| 安装xposed插件 | xposed插件本质是android APP，这里需要使用xposed实现ssl pinning破解([Just Trust Me](https://github.com/Fuzion24/JustTrustMe))和虚拟mac地址，稍后展开 | 

### 代码实现



#### ls

## adb命令整理

```bash
# 已启动设备列表
adb devices
# 安装APP
adb
# 刷入zip包
adb
```


