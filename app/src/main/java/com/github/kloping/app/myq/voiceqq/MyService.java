package com.github.kloping.app.myq.voiceqq;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.alibaba.fastjson.JSONObject;
import io.github.kloping.file.FileUtils;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.BotConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author github-kloping
 */
public class MyService extends Service {


    /**
     * 绑定的客户端接口
     */
    IBinder mBinder;

    private Intent intent;

    public static String getStringFromMessageChain(MessageChain event) {
        StringBuilder sb = new StringBuilder();
        for (Object o : event) {
            if (o instanceof OnlineMessageSource)
                continue;
            if (o instanceof PlainText) {
                sb.append(((PlainText) o).getContent());
            } else if (o instanceof At) {
                At at = (At) o;
                sb.append("[@").append(at.getTarget()).append("]");
            } else if (o instanceof FlashImage) {
                FlashImage flashImage = (FlashImage) o;
                sb.append("[一张闪照").append(Image.queryUrl(flashImage.getImage())).append("]");
            } else if (o instanceof Audio) {
                sb.append("[一条语音").append(((Audio) o).getFilename()).append("]");
            } else if (o instanceof Face) {
                Face face = (Face) o;
                sb.append("[Face:" + face.getName() + "]");
            } else if (o instanceof Image) {
                Image image = (Image) o;
                sb.append("[一张图片]");
            } else continue;
        }
        String s0 = sb.toString();
        if (s0.length() >= 100) {
            return "省略一百多个文字";
        }
        return s0;
    }

    private void save(Long q, String p, String base) {
        File file = new File(getApplication().getCacheDir(), "save.json");
        JSONObject jo = new JSONObject();
        jo.put("q", q);
        jo.put("p", p);
        jo.put("base", base);
        jo.put("Protocol", protocol.name());
        FileUtils.putStringInFile(jo.toString(), file);
    }

    public static BotConfiguration.MiraiProtocol protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD;
    private static Bot bot;

    public static final ExecutorService SERVICE = Executors.newFixedThreadPool(5);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (bot != null) {
            bot.close();
            VoicePlayer.getInstance().appendSpeak("登出成功");
        }
        SERVICE.submit(() -> {
            Long q = intent.getLongExtra("q", -1L);
            String p = intent.getStringExtra("p").trim();
            BotConfiguration configuration = new BotConfiguration();
            configuration.setProtocol(protocol);
            File file = getApplicationContext().getCacheDir();
            File f0 = new File(file, "workDirs");
            f0.mkdirs();
            configuration.setWorkingDir(f0);
            if (bot != null) {
                bot.close();
            }
            File f1 = null;
            try {
                f1 = new File("/sdcard/bot/" + q + "-device.json");
                f1.getParentFile().mkdirs();
                f1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            configuration.setWorkingDir(f1.getParentFile());
            configuration.fileBasedDeviceInfo(f1.getAbsolutePath());
            bot = BotFactory.INSTANCE.newBot(q, p, configuration);
            try {
                VoicePlayer.getInstance().appendSpeak("开始登录");
                bot.login();
                save(q, p, VoicePlayer.getInstance().getBase());
                VoicePlayer.getInstance().appendSpeak("登录成功");
            } catch (Exception e) {
                e.printStackTrace();
                VoicePlayer.getInstance().appendSpeak("登陆失败");
            }
            bot.getEventChannel().registerListenerHost(new SimpleListenerHost() {
                @Override
                public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
                    super.handleException(context, exception);
                }

                Long qid;

                @EventHandler
                public void onMessage(@NotNull FriendMessageEvent event) throws Exception {
                    String s0 = getStringFromMessageChain(event.getMessage());
                    if (qid == null || qid != event.getSender().getId()) {
                        s0 = event.getSender().getNick() + "给你发了: " + s0;
                    }
                    VoicePlayer.getInstance().appendSpeak(s0);
                    qid = event.getSender().getId();
                }

                @EventHandler
                public void onMessage(@NotNull GroupMessageEvent event) throws Exception {
                    if (containsAtMe(event)) {
                        String s0 = getStringFromMessageChain(event.getMessage());
                        s0 = event.getSender().getNick() + "在" + event.getSubject().getName() + "群里提到了你:" + s0;
                        VoicePlayer.getInstance().appendSpeak(s0);
                        qid = event.getSender().getId();
                    }
                }
            });
        });
        return START_STICKY;
    }

    private boolean containsAtMe(MessageEvent event) {
        long iid = event.getBot().getId();
        for (SingleMessage singleMessage : event.getMessage()) {
            if (singleMessage instanceof At) {
                At at = (At) singleMessage;
                if (at.getTarget() == iid) return true;
            }
        }
        return false;
    }

    /**
     * 通过bindService()绑定到服务的客户端
     */
    @Override
    public IBinder onBind(Intent intent) {
        this.intent = intent;
        System.out.println("service started");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}