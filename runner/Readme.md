# list emu
VBoxManage list vms
genyshell -c "devices list"
adb devices -l

# run emu
player -n "Clone - Google Nexus 6P_5"

# stop emu
player -n "Clone - Google Nexus 6P_5" -x

# boot fully check
adb shell getprop init.svc.bootanim
> stdout: 'adb: device offline' | 'adb: no devices/emulators found' | '' | running | stopped (stoped -> boot finish)