package com.github.kloping.app.myq.voiceqq.ui.login;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.kloping.app.myq.voiceqq.MyService;
import com.github.kloping.app.myq.voiceqq.OutManager;
import com.github.kloping.app.myq.voiceqq.R;
import com.github.kloping.app.myq.voiceqq.VoicePlayer;
import com.github.kloping.app.myq.voiceqq.databinding.ActivityLoginBinding;
import io.github.kloping.file.FileUtils;
import net.mamoe.mirai.event.ListenerHost;
import net.mamoe.mirai.utils.BotConfiguration;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * @author github-kloping
 */
public class LoginActivity extends AppCompatActivity implements ListenerHost {

    private static final int REQUEST_PERMISSION_OK = 101;
    private ActivityLoginBinding binding;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final EditText qqEditText = binding.qqNumber;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        binding.radiobutton1.setOnClickListener(this::onClick);
        binding.radiobutton2.setOnClickListener(this::onClick);
        binding.radiobutton3.setOnClickListener(this::onClick);
        binding.baseurl.setText(VoicePlayer.getInstance().getBase());
        loginButton.setOnClickListener(this::login);
        manager = new OutManager(binding.outLog, this).apply();
        VoicePlayer.getInstance().appendSpeak("初始化完成");
        new CheckInputManager(loginButton).add(qqEditText).add(passwordEditText, R.string.qq_password_length_most_short, 8);
        load();
        checkPermission();
        System.err.println("初始化完成");
    }

    OutManager manager;

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                startActivityForResult(intent, REQUEST_PERMISSION_OK);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_OK);
            }
        } else {
        }
    }

    private synchronized void login(View v) {
        Long q = Long.valueOf(binding.qqNumber.getText().toString());
        String p = binding.password.getText().toString();
        VoicePlayer.getInstance().setBase(binding.baseurl.getText().toString());
        Intent intent = new Intent(getBaseContext(), MyService.class);
        intent.putExtra("q", q);
        intent.putExtra("p", p);
        startService(intent);
    }

    private void load() {
        File file = new File(getApplication().getCacheDir(), "save.json");
        JSONObject jo = JSON.parseObject(FileUtils.getStringFromFile(file.getAbsolutePath()));
        if (jo == null) return;
        Long q = jo.getLong("q");
        String p = jo.getString("p").trim();
        String base = jo.getString("base").trim();
        MyService.protocol = BotConfiguration.MiraiProtocol.valueOf(jo.getString("Protocol"));
        binding.qqNumber.setText(q.toString());
        binding.password.setText(p);
        binding.baseurl.setText(base);
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.radiobutton1:
                MyService.protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD;
                break;
            case R.id.radiobutton2:
                MyService.protocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE;
                break;
            case R.id.radiobutton3:
                MyService.protocol = BotConfiguration.MiraiProtocol.ANDROID_WATCH;
                break;
            default:
                break;
        }
    }

    private void showLoginFailed(String errorString) {
        this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show());
    }

    private void showLoginMessage(String tips) {
        this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), tips, Toast.LENGTH_SHORT).show());
    }


    @Override
    protected void onResume() {
        if (manager.getCdl() != null)
            manager.getCdl().countDown();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        manager.setCdl(new CountDownLatch(1));
        moveTaskToBack(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}