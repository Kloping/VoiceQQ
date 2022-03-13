package com.github.kloping.app.myq.voiceqq;

import android.media.MediaPlayer;

import java.net.URLEncoder;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author github.kloping
 */
public class VoicePlayer implements MediaPlayer.OnCompletionListener {
    private boolean in = false;
    public Queue<String> queue = new ConcurrentLinkedDeque<>();
    private static VoicePlayer voicePlayer;

    public static final ExecutorService SERVICE = Executors.newFixedThreadPool(3);

    public static VoicePlayer getInstance() {
        return voicePlayer == null ? voicePlayer = new VoicePlayer() : voicePlayer;
    }

    private CountDownLatch cdl;

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (cdl != null)
            cdl.countDown();
        mp.reset();
    }

    private MediaPlayer mediaPlayer;

    public synchronized void appendSpeak(String str) {
        String s0 = String.format(getBase(), URLEncoder.encode(str));
        queue.offer(s0);
        if (in) {
            return;
        } else {
            in = true;
            SERVICE.submit(() -> {
                String l0 = null;
                while ((l0 = queue.poll()) != null) {
                    try {
                        cdl = new CountDownLatch(1);
                        mediaPlayer = null;
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(l0);
                        mediaPlayer.prepare();
                        mediaPlayer.setOnCompletionListener(this);
                        mediaPlayer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(l0);
                        cdl.countDown();
                    }
                    try {
                        cdl.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                in = false;
            });
        }
    }

    private String base = "https://tts.youdao.com/fanyivoice?word=%s&le=zh&keyfrom=speaker-target";

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }
}
