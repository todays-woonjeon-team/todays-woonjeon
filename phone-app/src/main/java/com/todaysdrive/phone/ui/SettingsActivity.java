package com.todaysdrive.phone.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

import com.todaysdrive.phone.PhoneApp;
import com.todaysdrive.phone.R;
import com.todaysdrive.phone.data.PhoneSettings;
import com.todaysdrive.phone.work.WorkScheduler;

import java.time.LocalDate;
import java.util.Locale;

/** 띠 운세용 생년월일, 출근 전 운세 알림 시각, 연결 끊기 */
public class SettingsActivity extends Activity {

    private PhoneSettings settings;
    private Button buttonBirth;
    private Button buttonTime;
    private Switch switchReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        settings = ((PhoneApp) getApplication()).settings();

        buttonBirth = findViewById(R.id.buttonBirth);
        buttonTime = findViewById(R.id.buttonReminderTime);
        switchReminder = findViewById(R.id.switchReminder);

        buttonBirth.setOnClickListener(v -> {
            LocalDate b = settings.birthDate() != null ? settings.birthDate() : LocalDate.of(1995, 1, 1);
            new DatePickerDialog(this, (view, y, m, d) -> {
                settings.setBirthDate(LocalDate.of(y, m + 1, d));
                refresh();
            }, b.getYear(), b.getMonthValue() - 1, b.getDayOfMonth()).show();
        });
        findViewById(R.id.buttonBirthClear).setOnClickListener(v -> {
            settings.setBirthDate(null);
            refresh();
        });

        switchReminder.setChecked(settings.reminderOn());
        switchReminder.setOnCheckedChangeListener((b, on) -> {
            settings.setReminder(on, settings.reminderMinutes());
            WorkScheduler.scheduleReminder(this);
            refresh();
        });
        buttonTime.setOnClickListener(v -> {
            int m = settings.reminderMinutes();
            new TimePickerDialog(this, (view, hour, minute) -> {
                settings.setReminder(settings.reminderOn(), hour * 60 + minute);
                WorkScheduler.scheduleReminder(this);
                refresh();
            }, m / 60, m % 60, false).show();
        });

        findViewById(R.id.buttonUnpair).setOnClickListener(v -> {
            settings.unpair();
            WorkScheduler.scheduleReportCheck(this);
            finish();
        });
        refresh();
    }

    private void refresh() {
        LocalDate b = settings.birthDate();
        buttonBirth.setText(b == null ? getString(R.string.settings_birth_none)
                : b + " · " + settings.zodiac().label() + "띠");
        int m = settings.reminderMinutes();
        buttonTime.setText(String.format(Locale.ROOT, "%s %02d:%02d",
                getString(R.string.settings_reminder_time), m / 60, m % 60));
        buttonTime.setEnabled(settings.reminderOn());
        findViewById(R.id.buttonUnpair).setEnabled(settings.isPaired());
    }
}
