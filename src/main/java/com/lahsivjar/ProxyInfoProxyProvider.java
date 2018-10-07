package com.lahsivjar;

import com.google.common.base.Strings;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;

final class ProxyInfoProxyProvider implements ProxyInfoProvider {

    private final ProxyInfo proxyInfo;

    ProxyInfoProxyProvider(ProxyInfo proxyInfo) {
        this.proxyInfo = proxyInfo;
    }

    @Override
    public ProxyInfo getProxyInfo(String protocol) {
        if (proxyInfo == null || Strings.isNullOrEmpty(protocol) || protocol.equalsIgnoreCase(proxyInfo.getType())) {
            return proxyInfo;
        }
        return null;
    }
}
