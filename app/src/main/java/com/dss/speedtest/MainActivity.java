package com.dss.speedtest;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import com.dss.speedtest.adapter.NodeAdapter;
import com.dss.speedtest.core.SpeedTest;
import com.dss.speedtest.core.serverSelector.TestPoint;
import com.dss.speedtest.databinding.ActivityMainBinding;
import com.dss.speedtest.test.SpeedTestRunner;

import java.util.Locale;

public class MainActivity extends Activity {

    ActivityMainBinding binding;

    private boolean running = false;
    SpeedTestRunner runner;

    private NodeAdapter nodeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        reset();
        setData();
    }

    private String format(double d) {
        Locale l = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            l = getResources().getConfiguration().getLocales().get(0);
        } else {
            l = getResources().getConfiguration().locale;
        }
        if (d < 10) return String.format(l, "%.2f", d);
        if (d < 100) return String.format(l, "%.1f", d);
        return "" + Math.round(d);
    }


    private void setData() {
        runner = new SpeedTestRunner(this, new SpeedTest.SpeedtestHandler() {
            @Override
            public void onDownloadUpdate(double dl, double progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.download.setText(format(dl) + "Mbps");
                    }
                });
            }

            @Override
            public void onUploadUpdate(double ul, double progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.upload.setText(format(ul) + "Mbps");
                    }
                });
            }

            @Override
            public void onPingJitterUpdate(double ping, double jitter, double progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.ping.setText(format(ping) + "ms");
                        binding.jitter.setText(format(jitter) + "ms");
                    }
                });
            }

            @Override
            public void onPingLostUpdate(int total, int lost) {

            }

            @Override
            public void onIPInfoUpdate(String ipInfo) {

            }

            @Override
            public void onTestIDReceived(String id, String shareURL) {

            }

            @Override
            public void onEnd() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        running = false;
                        binding.start.setText("开始测试");
                    }
                });
            }

            @Override
            public void onCriticalFailure(String err) {

            }
        });
        runner.init();
        final TestPoint[] testPoints = runner.getTestPoints();
        nodeAdapter = new NodeAdapter(this, testPoints);
        binding.node.setAdapter(nodeAdapter);
        binding.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running) {
                    runner.stopTest();
                    running = false;
                    binding.start.setText("开始测试");
                } else {
                    reset();
                    final TestPoint selectedItem = (TestPoint) binding.node.getSelectedItem();
                    runner.startTest(selectedItem);
                    running = true;
                    binding.start.setText("停止测试");
                }

            }
        });
    }

    private void reset() {
        binding.ping.setText("--");
        binding.jitter.setText("--");
        binding.download.setText("--");
        binding.upload.setText("--");
    }


}
