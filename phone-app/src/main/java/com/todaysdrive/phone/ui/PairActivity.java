package com.todaysdrive.phone.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.phone.PhoneApp;
import com.todaysdrive.phone.R;
import com.todaysdrive.phone.work.WorkScheduler;

import java.util.List;

/** 차량 화면의 6자리 연결 코드를 입력해 연결한다. 이름·전화번호는 묻지 않는다. */
public class PairActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        PhoneApp app = (PhoneApp) getApplication();

        EditText editCode = findViewById(R.id.editCode);
        CheckBox checkConsent = findViewById(R.id.checkConsent);
        Button buttonConnect = findViewById(R.id.buttonConnect);
        TextView textStatus = findViewById(R.id.textPairStatus);

        if (app.link() == null) {
            textStatus.setText(R.string.not_configured);
            buttonConnect.setEnabled(false);
            return;
        }

        buttonConnect.setOnClickListener(v -> {
            if (!checkConsent.isChecked()) {
                textStatus.setText(R.string.pair_need_consent);
                return;
            }
            String code = editCode.getText().toString().trim();
            buttonConnect.setEnabled(false);
            app.run(() -> {
                String pairId = app.link().claimCode(code);
                if (pairId == null) {
                    return false;
                }
                SignalSupport support = app.link().fetchSupport(pairId);
                app.settings().pair(pairId, support);
                List<DailySummary> days = app.syncDays();
                if (!days.isEmpty()) {
                    // 연결 전에 있던 리포트로 알림이 오지 않게
                    app.settings().setNotifiedAt(days.get(days.size() - 1).updatedAt());
                }
                return true;
            }, (ok, error) -> {
                buttonConnect.setEnabled(true);
                if (error != null) {
                    textStatus.setText(getString(R.string.error_prefix, error.getMessage()));
                } else if (!ok) {
                    textStatus.setText(R.string.pair_wrong);
                } else {
                    textStatus.setText(R.string.pair_done);
                    WorkScheduler.scheduleReportCheck(this);
                    finish();
                }
            });
        });
    }
}
