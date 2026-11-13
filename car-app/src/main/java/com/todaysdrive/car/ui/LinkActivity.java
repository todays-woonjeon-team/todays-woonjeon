package com.todaysdrive.car.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.todaysdrive.car.CarApp;
import com.todaysdrive.car.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 휴대폰 연결. 주차 중 화면에 6자리 연결 코드를 띄우고, 휴대폰 앱에서 입력하면 연결된다.
 * 이름이나 전화번호 없이 연결되고, 동의한 경우에만 하루 결과 요약을 보낸다.
 */
public class LinkActivity extends Activity {

    private CarApp app;
    private CheckBox checkConsent;
    private TextView textStatus;
    private TextView textCode;
    private TextView textCodeInfo;
    private Button buttonMakeCode;
    private Button buttonUnlink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);
        app = (CarApp) getApplication();

        checkConsent = findViewById(R.id.checkConsent);
        textStatus = findViewById(R.id.textLinkStatus);
        textCode = findViewById(R.id.textCode);
        textCodeInfo = findViewById(R.id.textCodeInfo);
        buttonMakeCode = findViewById(R.id.buttonMakeCode);
        buttonUnlink = findViewById(R.id.buttonUnlink);

        buttonMakeCode.setOnClickListener(v -> makeCode());
        buttonUnlink.setOnClickListener(v -> {
            buttonUnlink.setEnabled(false);
            app.link().unlink((r, e) -> {
                textCode.setText("");
                textCodeInfo.setText("");
                refresh();
            });
        });
        findViewById(R.id.buttonLinkClose).setOnClickListener(v -> finish());
        refresh();
    }

    private void refresh() {
        boolean configured = app.link().isConfigured();
        boolean linked = app.link().isLinked();
        checkConsent.setChecked(linked);
        buttonMakeCode.setEnabled(configured);
        buttonUnlink.setEnabled(linked);
        if (!configured) {
            textStatus.setText(R.string.link_not_configured);
        } else {
            textStatus.setText(linked ? R.string.link_linked : R.string.link_not_linked);
        }
    }

    private void makeCode() {
        if (!app.session().isParked()) {
            textStatus.setText(R.string.link_driving);
            return;
        }
        if (!checkConsent.isChecked()) {
            textStatus.setText(R.string.link_need_consent);
            return;
        }
        buttonMakeCode.setEnabled(false);
        app.link().createCode(app.session().support(), (pairing, error) -> {
            buttonMakeCode.setEnabled(true);
            if (error != null) {
                textStatus.setText(getString(R.string.error_prefix, error.getMessage()));
                return;
            }
            textCode.setText(pairing.code);
            String until = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(pairing.expiresAt));
            textCodeInfo.setText(getString(R.string.link_code_info) + " (" + until + "까지)");
            refresh();
        });
    }
}
