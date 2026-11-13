package com.todaysdrive.car.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.todaysdrive.car.CarApp;
import com.todaysdrive.car.R;
import com.todaysdrive.core.reward.Prize;
import com.todaysdrive.core.reward.Roulette;
import com.todaysdrive.ui.RouletteView;

import java.security.SecureRandom;

/**
 * 룰렛. 주차 중에만 돌릴 수 있다. 티켓을 먼저 쓰고(서버 또는 차량 장부에 기록) 그 결과 칸으로 돌린다.
 */
public class RouletteActivity extends Activity {

    private CarApp app;
    private RouletteView roulette;
    private TextView textBalance;
    private TextView textResult;
    private Button buttonSpin;
    private Roulette picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roulette);
        app = (CarApp) getApplication();
        picker = new Roulette(app.prizes(), new SecureRandom());

        roulette = findViewById(R.id.roulette);
        textBalance = findViewById(R.id.textBalance);
        textResult = findViewById(R.id.textResult);
        buttonSpin = findViewById(R.id.buttonSpin);
        TextView textOdds = findViewById(R.id.textOdds);

        roulette.setPrizes(app.prizes());
        textOdds.setText(String.join("\n", app.prizes().probabilityLines()));
        buttonSpin.setOnClickListener(v -> spin());
        findViewById(R.id.buttonRouletteClose).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance();
    }

    private void loadBalance() {
        app.link().ticketBalance((balance, error) -> {
            int n = balance == null ? 0 : balance;
            textBalance.setText(getString(R.string.tickets_left, n));
            buttonSpin.setEnabled(n > 0 && !roulette.isSpinning());
            if (n == 0 && textResult.getText().length() == 0) {
                textResult.setText(R.string.roulette_no_ticket);
            }
        });
    }

    private void spin() {
        if (!app.session().isParked()) {
            textResult.setText(R.string.roulette_driving);
            return;
        }
        buttonSpin.setEnabled(false);
        textResult.setText("");
        int index = picker.spinIndex();
        Prize prize = app.prizes().prizes().get(index);
        app.link().spendTicket(prize, (spin, error) -> {
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
