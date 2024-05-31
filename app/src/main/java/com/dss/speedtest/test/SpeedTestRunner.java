package com.dss.speedtest.test;

import android.content.Context;
import android.util.Log;

import com.dss.speedtest.core.SpeedTest;
import com.dss.speedtest.core.config.SpeedTestConfig;
import com.dss.speedtest.core.config.TelemetryConfig;
import com.dss.speedtest.core.serverSelector.TestPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 * 速度测试器
 *
 * @Author :DSS
 * @Date :2024/5/30 10:46
 * @description SpeedTestRunner.java
 */
public class SpeedTestRunner {
    private static final String TAG = SpeedTestRunner.class.getSimpleName();
    private SpeedTest st;

    private Context context;

    //授权信息
    private String authorization;


    //是否已停止测试
    private boolean stopped = false;

    private SpeedTest.SpeedtestHandler callback;

    public SpeedTestRunner(Context context) {
        this.context = context;
    }

    public SpeedTestRunner(Context context, String authorization) {
        this.st = new SpeedTest();
        this.context = context;
        this.authorization = authorization;
    }

    public SpeedTestRunner(Context context, SpeedTest.SpeedtestHandler callback) {
        this.st = new SpeedTest();
        this.context = context;
        this.callback = callback;
    }

    public SpeedTestRunner(Context context, String authorization, SpeedTest.SpeedtestHandler callback) {
        this.st = new SpeedTest();
        this.context = context;
        this.authorization = authorization;
        this.callback = callback;
    }


    public void init() {
        SpeedTestConfig config = null;
        TelemetryConfig telemetryConfig = null;
        TestPoint[] servers = null;
        try {
            String c = readFileFromAssets("SpeedTestConfig.json");
            JSONObject o = new JSONObject(c);
            config = new SpeedTestConfig(o);
            c = readFileFromAssets("TelemetryConfig.json");
            o = new JSONObject(c);
            telemetryConfig = new TelemetryConfig(o);
            if (telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)) {

            }
            if (st != null) {
                try {
                    st.abort();
                } catch (Throwable e) {
                }
            }
            st = new SpeedTest();
            st.setSpeedTestConfig(config);
            st.setTelemetryConfig(telemetryConfig);
            c = readFileFromAssets("ServerList.json");
            if (c.startsWith("\"") || c.startsWith("'")) { //fetch server list from URL
                if (!st.loadServerList(c.subSequence(1, c.length() - 1).toString())) {
                    throw new Exception("Failed to load server list");
                }
            } else { //use provided server list
                JSONArray a = new JSONArray(c);
                if (a.length() == 0) throw new Exception("No test points");
                ArrayList<TestPoint> s = new ArrayList<>();
                for (int i = 0; i < a.length(); i++)
                    s.add(new TestPoint(a.getJSONObject(i)));
                servers = s.toArray(new TestPoint[0]);
                st.addTestPoints(servers);
            }
        } catch (final Throwable e) {
            System.err.println(e);
            st = null;
        }
    }

    private String readFileFromAssets(String name) throws Exception {
        BufferedReader b = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));
        String ret = "";
        try {
            for (; ; ) {
                String s = b.readLine();
                if (s == null) break;
                ret += s;
            }
        } catch (EOFException e) {
        }
        return ret;
    }

    /**
     * 获取所有测试节点
     *
     * @return
     */
    public TestPoint[] getTestPoints() {
        return st.getTestPoints();
    }

    /**
     * 开始测试
     */
    public void startTest() {
        final TestPoint[] testPoints = st.getTestPoints();
        if (null != testPoints && testPoints.length > 0) {
            startTest(testPoints[0]);
        } else {
            Log.w(TAG, "未找到测速节点信息");
        }
    }

    /**
     * 停止测试
     */
    public void stopTest() {
        stopped = true;
        st.abort();
    }

    //测试回调类
    private SpeedTest.SpeedtestHandler speedTestHandler = new SpeedTest.SpeedtestHandler() {
        @Override
        public void onDownloadUpdate(final double dl, final double progress) {
            Log.d(TAG, "onDownloadUpdate :" + dl + ",progress:" + progress);
            if (null != callback) {
                callback.onDownloadUpdate(dl, progress);
            }
        }

        @Override
        public void onUploadUpdate(final double ul, final double progress) {
            Log.d(TAG, "onUploadUpdate :" + ul + ",progress:" + progress);
            if (null != callback) {
                callback.onUploadUpdate(ul, progress);
            }
        }

        @Override
        public void onPingJitterUpdate(final double ping, final double jitter, final double progress) {
            Log.d(TAG, "onPingJitterUpdate ping:" + ping + ",jitter:" + jitter + ",progress:" + progress);
            if (null != callback) {
                callback.onPingJitterUpdate(ping, jitter, progress);
            }
        }

        @Override
        public void onPingLostUpdate(final int total, final int lost) {
            Log.d(TAG, "onPingJitterUpdate total:" + total + ",lost:" + lost);
            if (null != callback) {
                callback.onPingLostUpdate(total, lost);
            }
        }

        @Override
        public void onIPInfoUpdate(final String ipInfo) {
            if (null != callback) {
                callback.onIPInfoUpdate(ipInfo);
            }
        }

        @Override
        public void onTestIDReceived(final String id, final String shareURL) {
            if (shareURL == null || shareURL.isEmpty() || id == null || id.isEmpty()) return;
            if (null != callback) {
                callback.onTestIDReceived(id, shareURL);
            }
        }

        @Override
        public void onEnd() {
            Log.d(TAG, "onEnd.");
//            if (!stopped) {
//                //若未停止测试，则继续测试
//                st.start(speedTestHandler);
//            }
            if (null != callback) {
                callback.onEnd();
            }
        }

        @Override
        public void onCriticalFailure(String err) {

        }
    };

    /**
     * 开始测试指定节点
     *
     * @param selected
     */
    public void startTest(final TestPoint selected) {
        selected.setAuthorization(authorization);
        st.setSelectedServer(selected);
        st.start(speedTestHandler);
        stopped = false;
    }
}
