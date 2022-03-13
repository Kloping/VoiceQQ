package com.github.kloping.app.myq.voiceqq;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import io.github.kloping.io.ReadIOUtils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author github.kloping
 */
public class OutManager {
    private ListView listView;
    private Activity activity;

    public OutManager(ListView listView, Activity ac) {
        this.listView = listView;
        this.activity = ac;
    }

    private int max = 200;
    public CountDownLatch cdl = null;
    private int num = 0;

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public CountDownLatch getCdl() {
        return cdl;
    }

    public void setCdl(CountDownLatch cdl) {
        this.cdl = cdl;
    }

    public static final ExecutorService SERVICE = Executors.newFixedThreadPool(5);
    private List<View> views = new LinkedList<>();

    private synchronized void test() {
        num++;
        listView.smoothScrollToPosition(num);
        if (num >= getMax()) {
            for (View view : views) {
                listView.removeFooterView(view);
            }
            num = 0;
        }
    }

    private ReadIOUtils.ReadOutputStreamImpl out0;
    private ReadIOUtils.ReadOutputStreamImpl err0;

    public OutManager apply() {
        listView.setAdapter(new ArrayAdapter<TextView>(listView.getContext(), android.R.layout.simple_list_item_1, new ArrayList<>()));
        SERVICE.submit(() -> {
            out0 = ReadIOUtils.connectOs(System.out);
            out0.setMode(1);
            System.setOut(new PrintStream(out0.getOs()));
            while (true) {
                if (cdl != null) {
                    try {
                        cdl.await();
                        cdl = null;
                        out0.clearCache();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String m1 = out0.readLine();
                Object[] os = ColorA.getColorString(m1);
                activity.runOnUiThread(() -> {
                    listView.addFooterView(create(os[0].toString(), (Integer) os[1]));
                    test();
                });
            }
        });
        SERVICE.submit(() -> {
            err0 = io.github.kloping.io.ReadIOUtils.connectOs(System.err);
            err0.setMode(1);
            System.setErr(new PrintStream(err0.getOs()));
            while (true) {
                if (cdl != null) {
                    try {
                        cdl.await();
                        cdl = null;
                        err0.clearCache();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String line = err0.readLine();
                activity.runOnUiThread(() -> {
                    listView.addFooterView(create(line.trim(), Color.RED));
                    test();
                });
            }
        });
        SERVICE.submit(() -> {
            while (true) {
                Thread.sleep(100000);
                if (cdl != null) {
                    err0.clearCache();
                    out0.clearCache();
                }
            }
        });
        return this;
    }

    private TextView create(String text, int color) {
        TextView textView = new TextView(listView.getContext());
        if (color != -1)
            textView.setTextColor(color);
        else textView.setTextColor(Color.BLACK);
        textView.setText(text);
        views.add(textView);
        return textView;
    }
}
