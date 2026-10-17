package com.itservice.desk.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/**
 * 本机内网 IPv4 解析。
 *
 * 用途：二维码/入口地址必须给出「手机能访问的地址」。管理员常在本机用
 * http://localhost:8080 打开后台，若直接把请求 Host 拿去生成二维码，
 * 手机扫出来是 localhost，必然打不开。
 *
 * 只认站点本地地址（10/172.16-31/192.168），并排除 WSL、Hyper-V、VMware 等虚拟网卡
 * （否则会拿到 172.x 的虚拟网段）。
 */
public final class LanAddress {

    private static final String EXCLUDE =
            "(?i).*(virtual|vmware|hyper-v|vbox|virtualbox|docker|wsl|loopback|tailscale|zerotier|bluetooth|vethernet|wan miniport).*";

    private LanAddress() {
    }

    /** 返回第一个可用的内网 IPv4；解析不到返回 null（调用方自行降级）。 */
    public static String resolve() {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nics)) {
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                String label = String.valueOf(ni.getDisplayName()) + " " + ni.getName();
                if (label.matches(EXCLUDE)) {
                    continue;
                }
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // 拿不到就走降级分支
        }
        return null;
    }

    /** 判断请求里的 host 是否只对"本机"有意义（需要替换成内网 IP）。 */
    public static boolean isLoopback(String host) {
        if (host == null || host.isBlank()) {
            return true;
        }
        return "localhost".equalsIgnoreCase(host)
                || host.startsWith("127.")
                || "0.0.0.0".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }
}
