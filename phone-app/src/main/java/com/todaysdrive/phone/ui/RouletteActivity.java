package com.todaysdrive.phone.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.todaysdrive.core.reward.Prize;
import com.todaysdrive.core.reward.Roulette;
import com.todaysdrive.phone.PhoneApp;
import com.todaysdrive.phone.R;
import com.todaysdrive.ui.RouletteView;

import java.security.SecureRandom;

/** 휴대폰 룰렛. 티켓은 서버 한 곳에서 관리하므로 차와 연결한 뒤에만 돌릴 수 있다. */
public class RouletteActivity extends Activity {

    private PhoneApp app;
    private RouletteView roulette;
    private TextView textBalance;
    private TextView textResult;
    private Button buttonSpin;
    private Roulette picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roulette);
        app = (PhoneApp) getApplication();
        picker = new Roulette(app.prizes(), new SecureRandom());

        roulette = findViewById(R.id.roulette);
        textBalance = findViewById(R.id.textBalance);
        textResult = findViewById(R.id.textResult);
        buttonSpin = findViewById(R.id.buttonSpin);
        roulette.setPrizes(app.prizes());
        ((TextView) findViewById(R.id.textOdds)).setText(String.join("\n", app.prizes().probabilityLines()));
        buttonSpin.setOnClickListener(v -> spin());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance();
    }

    private boolean canUseServer() {
        return app.link() != null && app.settings().isPaired();
    }

    private void loadBalance() {
        if (!canUseServer()) {
            textBalance.setText(getString(R.string.tickets_left, 0));
            textResult.setText(R.string.roulette_need_pair);
            buttonSpin.setEnabled(false);
            return;
        }
        String pairId = app.settings().pairId();
        app.run(() -> app.link().fetchTickets(pairId).balance(), (balance, error) -> {
            if (error != null) {
                textResult.setText(getString(R.string.error_prefix, error.getMessage()));
                buttonSpin.setEnabled(false);
                return;
            }
            textBalance.setText(getString(R.string.tickets_left, balance));
            buttonSpin.setEnabled(balance > 0 && !roulette.isSpinning());
            if (balance == 0 && textResult.getText().length() == 0) {
                textResult.setText(R.string.roulette_no_ticket);
            }
        });
    }

    private void spin() {
        buttonSpin.setEnabled(false);
        textResult.setText("");
        int index = picker.spinIndex();
        Prize prize = app.prizes().prizes().get(index);
        String pairId = app.settings().pairId();
        app.run(() -> app.link().spendTicket(pairId, prize, "phone"), (spin, error) -> {
            if (error != null) {
                textResult.setText(getString(R.string.error_prefix, error.getMessage()));
                loadBalance();
                return;
            }
            if (spin == null) {
                textResult.setText(R.string.roulette_no_ticket);
                loadBalance();
                return;
            }
            roulette.spinTo(index, () -> {
                textResult.setText(prize.isWin()
                        ? getString(R.string.roulette_win, prize.name())
                        : getString(R.string.roulette_lose));
                loadBalance();
            });
        });
    }
}
