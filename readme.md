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

### 开发环境配置

#### 图形界面

VNC安装：参考[http://websac.sogou/rookie_guide/work_env/#vnc](http://websac.sogou/rookie_guide/work_env/#vnc)

XFCE安装：
```bash
yum groupinstall "Xfce" -y
```

#### Android模拟器

> virtualbox依赖虚拟化技术，无法在虚机运行，请使用实机

选用genymotion+virtualbox，相比其他模拟器，提供了较丰富的命令行工具，更方便编程操作

virtualbox安装：
```bash
dnf config-manager --add-repo=https://download.virtualbox.org/virtualbox/rpm/el/virtualbox.repo
yum search virtualbox
yum install virtualbox
```
> 若最新版virtualbox卡顿崩溃，可尝试安装6.0及以下版本

genymotion安装：[下载](https://www.genymotion.com/download/)linux安装文件，直接运行即可完成安装

> 若virtualbox启动报错：  
> WARNING: The vboxdrv kernel module is not loaded. Either there is no module available for the current kernel (4.18.0-193.el8.x86_64) or it failed to load. If your system is using EFI Secure Boot you may need to sign the kernel modules (vboxdrv, vboxnetflt, vboxnetadp, vboxpci) before you can load them  
> 可尝试安装dkms解决

#### 开发工具


### APP安装

### 抓包



