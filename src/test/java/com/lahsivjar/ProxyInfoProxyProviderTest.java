package com.lahsivjar;

import org.apache.maven.wagon.proxy.ProxyInfo;
import org.junit.Assert;
import org.junit.Test;

public class ProxyInfoProxyProviderTest {

    @Test
    public void testWithNullProxyInfo() {
        final ProxyInfoProxyProvider withNullProxyInfo = new ProxyInfoProxyProvider(null);
        Assert.assertNull(withNullProxyInfo.getProxyInfo("gs"));
    }

    @Test
    public void testWithNullProxyInfoAndNullProtocol() {
        final ProxyInfoProxyProvider withNullProxyInfo = new ProxyInfoProxyProvider(null);
        Assert.assertNull(withNullProxyInfo.getProxyInfo(null));
    }

    @Test
    public void testWithNonNullProxyInfo() {
        final ProxyInfo expectedProxyInfo = new ProxyInfo();
        expectedProxyInfo.setHost("actual.proxy.info");
        expectedProxyInfo.setType("gs");

        final ProxyInfoProxyProvider proxyProvider = new ProxyInfoProxyProvider(expectedProxyInfo);
        final ProxyInfo actualProxyInfo = proxyProvider.getProxyInfo("gs");
        Assert.assertNotNull(actualProxyInfo);
        Assert.assertEquals(expectedProxyInfo.getHost(), actualProxyInfo.getHost());
    }

    @Test
    public void testWithNonNullProxyInfoNullType() {
        final ProxyInfo expectedProxyInfo = new ProxyInfo();
        expectedProxyInfo.setHost("actual.proxy.info");
        expectedProxyInfo.setType("gs");

        final ProxyInfoProxyProvider proxyProvider = new ProxyInfoProxyProvider(expectedProxyInfo);
        final ProxyInfo actualProxyInfo = proxyProvider.getProxyInfo(null);
        Assert.assertNotNull(actualProxyInfo);
        Assert.assertEquals(expectedProxyInfo.getHost(), actualProxyInfo.getHost());
    }

    @Test
    public void testWithNonNullProxyInfoDifferentType() {
        final ProxyInfo expectedProxyInfo = new ProxyInfo();
        expectedProxyInfo.setHost("actual.proxy.info");
        expectedProxyInfo.setType("gs");

        final ProxyInfoProxyProvider proxyProvider = new ProxyInfoProxyProvider(expectedProxyInfo);
        final ProxyInfo actualProxyInfo = proxyProvider.getProxyInfo("diff");
        Assert.assertNull(actualProxyInfo);
    }
}
