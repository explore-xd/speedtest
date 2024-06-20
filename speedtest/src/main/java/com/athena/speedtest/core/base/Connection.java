package com.athena.speedtest.core.base;

import android.os.Build;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Connection {
    private Socket socket;
    private String host;
    private int port;

    //授权信息
    private String authorization;

    private int mode = MODE_NOT_SET;
    private static final int MODE_NOT_SET = 0, MODE_HTTP = 1, MODE_HTTPS = 2;

    private static final String USER_AGENT = "Speedtest-Android/1.2.3 (SDK " + Build.VERSION.SDK_INT + "; " + Build.PRODUCT + "; Android " + Build.VERSION.RELEASE + ")",
            LOCALE = Build.VERSION.SDK_INT >= 21 ? Locale.getDefault().toLanguageTag() : null;

    public Connection(String url, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer) {
        boolean tryHTTP = false, tryHTTPS = false;
        Locale.getDefault().toString();
        if (url.startsWith("http://")) {
            tryHTTP = true;
            try {
                URL u = new URL(url);
                host = u.getHost();
                port = u.getPort();
            } catch (Throwable t) {
                throw new IllegalArgumentException("Malformed URL (HTTP)");
            }
        } else if (url.startsWith("https://")) {
            tryHTTPS = true;
            try {
                URL u = new URL(url);
                host = u.getHost();
                port = u.getPort();
            } catch (Throwable t) {
                throw new IllegalArgumentException("Malformed URL (HTTPS)");
            }
        } else if (url.startsWith("//")) {
            tryHTTP = true;
            tryHTTPS = true;
            try {
                URL u = new URL("http:" + url);
                host = u.getHost();
                port = u.getPort();
            } catch (Throwable t) {
                throw new IllegalArgumentException("Malformed URL (HTTP/HTTPS)");
            }
        } else {
            throw new IllegalArgumentException("Malformed URL (Unknown or unspecified protocol)");
        }
        try {
            if (mode == MODE_NOT_SET && tryHTTPS) {
//                SocketFactory factory = SSLSocketFactory.getDefault();
//                socket = factory.createSocket();
//                if (connectTimeout > 0) {
//                    socket.connect(new InetSocketAddress(host, port == -1 ? 443 : port), connectTimeout);
//                } else {
//                    socket.connect(new InetSocketAddress(host, port == -1 ? 443 : port));
//                }
                //忽略https证书验证
                //获取SSLContext对象
                SSLContext context = SSLContext.getInstance("TLS");
                //设置管理器
                context.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        Log.d("Connection", "checkClientTrusted authType:" + authType);
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        Log.d("Connection", "checkServerTrusted authType:" + authType);
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, null);
                final SSLSocketFactory factory = context.getSocketFactory();
                //设置管理器
                HttpsURLConnection.setDefaultSSLSocketFactory(factory);
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                    @Override
                    public boolean verify(String hostName, SSLSession arg1) {
                        //返回true
//                        final boolean success = host.equalsIgnoreCase(hostName);
//                        return success;
                        return true;
                    }
                });

                socket = factory.createSocket();
                if (connectTimeout > 0) {
                    socket.connect(new InetSocketAddress(host, port == -1 ? 443 : port), connectTimeout);
                } else {
                    socket.connect(new InetSocketAddress(host, port == -1 ? 443 : port));
                }

                mode = MODE_HTTPS;
            }
        } catch (Throwable t) {
        }
        try {
            if (mode == MODE_NOT_SET && tryHTTP) {
                SocketFactory factory = SocketFactory.getDefault();
                socket = factory.createSocket();
                if (connectTimeout > 0) {
                    socket.connect(new InetSocketAddress(host, port == -1 ? 80 : port), connectTimeout);
                } else {
                    socket.connect(new InetSocketAddress(host, port == -1 ? 80 : port));
                }
                mode = MODE_HTTP;
            }
        } catch (Throwable t) {
        }
        if (mode == MODE_NOT_SET) throw new IllegalStateException("Failed to connect");
        if (soTimeout > 0) {
            try {
                socket.setSoTimeout(soTimeout);
            } catch (Throwable t) {
            }
        }
        if (recvBuffer > 0) {
            try {
                socket.setReceiveBufferSize(recvBuffer);
            } catch (Throwable t) {
            }
        }
        if (sendBuffer > 0) {
            try {
                socket.setSendBufferSize(sendBuffer);
            } catch (Throwable t) {
            }
        }
    }

    public Connection(String url, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, String authorization) {
        this(url, connectTimeout, soTimeout, recvBuffer, sendBuffer);
        this.authorization = authorization;
    }

    private static final int DEFAULT_CONNECT_TIMEOUT = 2000, DEFAULT_SO_TIMEOUT = 5000;

    public Connection(String url) {
        this(url, DEFAULT_CONNECT_TIMEOUT, DEFAULT_SO_TIMEOUT, -1, -1);
    }

    public InputStream getInputStream() {
        try {
            return socket.getInputStream();
        } catch (Throwable t) {
            return null;
        }
    }

    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream();
        } catch (Throwable t) {
            return null;
        }
    }

    private PrintStream ps = null;

    public PrintStream getPrintStream() {
        if (ps == null) {
            try {
                ps = new PrintStream(getOutputStream(), false, "utf-8");
            } catch (Throwable t) {
                ps = null;
            }
        }
        return ps;
    }

    private InputStreamReader isr = null;

    public InputStreamReader getInputStreamReader() {
        if (isr == null) {
            try {
                isr = new InputStreamReader(getInputStream(), "utf-8");
            } catch (Throwable t) {
                isr = null;
            }
        }
        return isr;
    }

    public void GET(String path, boolean keepAlive) throws Exception {
        try {
            if (!path.startsWith("/")) path = "/" + path;
            PrintStream ps = getPrintStream();
            ps.print("GET " + path + " HTTP/1.1\r\n");
            ps.print("Host: " + host + "\r\n");
            ps.print("User-Agent: " + USER_AGENT);
            ps.print("Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n");
            ps.print("Accept-Encoding: identity\r\n");
            if (null != authorization && !authorization.isEmpty()) {
                ps.print("Authorization: " + authorization + "\r\n");
            }
            if (LOCALE != null) ps.print("Accept-Language: " + LOCALE + "\r\n");
            ps.print("\r\n");
            ps.flush();
        } catch (Throwable t) {
            throw new Exception("Failed to send GET request");
        }
    }

    public void POST(String path, boolean keepAlive, String contentType, long contentLength) throws Exception {
        try {
            if (!path.startsWith("/")) path = "/" + path;
            PrintStream ps = getPrintStream();
            ps.print("POST " + path + " HTTP/1.1\r\n");
            ps.print("Host: " + host + "\r\n");
            ps.print("User-Agent: " + USER_AGENT + "\r\n");
            ps.print("Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n");
            ps.print("Accept-Encoding: identity\r\n");
            if (LOCALE != null) ps.print("Accept-Language: " + LOCALE + "\r\n");
            if (contentType != null) ps.print("Content-Type: " + contentType + "\r\n");
            ps.print("Content-Encoding: identity\r\n");
            if (null != authorization && !authorization.isEmpty()) {
                ps.print("Authorization: " + authorization + "\r\n");
            }
            if (contentLength >= 0) ps.print("Content-Length: " + contentLength + "\r\n");
            ps.print("\r\n");
            ps.flush();
        } catch (Throwable t) {
            throw new Exception("Failed to send POST request");
        }
    }

    public String readLineUnbuffered() {
        try {
            InputStreamReader in = getInputStreamReader();
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = in.read();
                if (c == -1) break;
                sb.append((char) c);
                if (c == '\n') break;
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public HashMap<String, String> parseResponseHeaders() throws Exception {
        try {
            HashMap<String, String> ret = new HashMap<>();
            String s = readLineUnbuffered();
            if (!s.contains("200 OK"))
                throw new Exception("Did not receive an HTTP 200 (" + s.trim() + ")");
            while (true) {
                s = readLineUnbuffered();
                if (s.trim().isEmpty()) break;
                if (s.contains(":")) {
                    ret.put(s.substring(0, s.indexOf(":")).trim().toLowerCase(), s.substring(s.indexOf(":") + 1).trim());
                }
            }
            return ret;
        } catch (Throwable t) {
            throw new Exception("Failed to get response headers (" + t + ")");
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (Throwable t) {
        }
        socket = null;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getMode() {
        return mode;
    }

}
