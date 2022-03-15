package com.github.kloping.app.myq.voiceqq;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.github.kloping.file.FileUtils;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
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

    public static String getStringFromMessageChain(MessageEvent event) {
        StringBuilder sb = new StringBuilder();
        for (Object o : event.getMessage()) {
            if (o instanceof OnlineMessageSource)
                continue;
            if (o instanceof PlainText) {
                sb.append(((PlainText) o).getContent());
            } else if (o instanceof At) {
                if (event instanceof GroupMessageEvent) {
                    GroupMessageEvent gme = (GroupMessageEvent) event;
                    At at = (At) o;
                    if (at.getTarget() == gme.getBot().getId()) {
                        sb.append("at我");
                    } else {
                        sb.append("[at").append(at.getDisplay(gme.getGroup())).append("]");
                    }
                }
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
            } else {
                continue;
            }
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
                if (!f1.exists()) {
                    createDeviceFile(f1);
                }
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
                    String s0 = getStringFromMessageChain(event);
                    if (qid == null || qid != event.getSender().getId()) {
                        s0 = event.getSender().getNick() + "给你发了: " + s0;
                    }
                    VoicePlayer.getInstance().appendSpeak(s0);
                    qid = event.getSender().getId();
                }

                @EventHandler
                public void onMessage(@NotNull StrangerMessageEvent event) throws Exception {
                    String s0 = getStringFromMessageChain(event);
                    if (qid == null || qid != event.getSender().getId()) {
                        s0 = "一个陌生人给你发了: " + s0;
                    }
                    VoicePlayer.getInstance().appendSpeak(s0);
                    qid = event.getSender().getId();
                }

                @EventHandler
                public void onMessage(@NotNull GroupTempMessageEvent event) throws Exception {
                    String s0 = getStringFromMessageChain(event);
                    if (qid == null || qid != event.getSender().getId()) {
                        s0 = "一个陌生人给你发了: " + s0;
                    }
                    VoicePlayer.getInstance().appendSpeak(s0);
                    qid = event.getSender().getId();
                }

                @EventHandler
                public void onMessage(@NotNull GroupMessageEvent event) throws Exception {
                    if (containsAtMe(event)) {
                        String s0 = getStringFromMessageChain(event);
                        s0 = event.getSender().getNick() + "在" + event.getSubject().getName() + "群里提到了你:" + s0;
                        VoicePlayer.getInstance().appendSpeak(s0);
                        qid = event.getSender().getId();
                    }
                }
            });
        });
        return START_STICKY;
    }

    private void createDeviceFile(File f1) throws IOException {
        f1.getParentFile().mkdirs();
        f1.createNewFile();
        JSONObject jsonObject = JSON.parseObject("{\n" +
                "    \"deviceInfoVersion\": 2,\n" +
                "    \"data\": {\n" +
                "        \"display\": \"MIRAI.758805.001\",\n" +
                "        \"product\": \"mirai\",\n" +
                "        \"device\": \"mirai\",\n" +
                "        \"board\": \"mirai\",\n" +
                "        \"brand\": \"mamoe\",\n" +
                "        \"model\": \"mirai\",\n" +
                "        \"bootloader\": \"unknown\",\n" +
                "        \"fingerprint\": \"mamoe/mirai/mirai:10/MIRAI.200122.001/3638038:user/release-keys\",\n" +
                "        \"bootId\": \"0D1E035A-2D96-D8A6-07BF-F9E812D8DEFD\",\n" +
                "        \"procVersion\": \"Linux version 3.0.31-xb2W51sH (android-build@xxx.xxx.xxx.xxx.com)\",\n" +
                "        \"baseBand\": \"\",\n" +
                "        \"version\": {\n" +
                "            \"incremental\": \"5891938\",\n" +
                "            \"release\": \"10\",\n" +
                "            \"codename\": \"REL\"\n" +
                "        },\n" +
                "        \"simInfo\": \"T-Mobile\",\n" +
                "        \"osType\": \"android\",\n" +
                "        \"macAddress\": \"02:00:00:00:00:00\",\n" +
                "        \"wifiBSSID\": \"02:00:00:00:00:00\",\n" +
                "        \"wifiSSID\": \"<unknown ssid>\",\n" +
                "        \"imsiMd5\": \"9f601d1b0b77581d96178e596b28df75\",\n" +
                "        \"imei\": \"959896676458509\",\n" +
                "        \"apn\": \"wifi\"\n" +
                "    }\n" +
                "}");
        jsonObject.getJSONObject("data").put("imsiMd5", SystemUtil.md5(SystemUtil.getSimOperator()));
        jsonObject.getJSONObject("data").put("imei", SystemUtil.getDeviceId(this));
        FileUtils.putStringInFile(JSON.toJSONString(jsonObject, true), f1);
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