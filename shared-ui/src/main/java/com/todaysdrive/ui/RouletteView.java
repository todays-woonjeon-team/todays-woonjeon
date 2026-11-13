package com.todaysdrive.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.todaysdrive.core.reward.Prize;
import com.todaysdrive.core.reward.PrizeTable;

import java.util.ArrayList;
import java.util.List;

/**
 * 룰렛 화면. 칸 크기를 당첨 확률에 비례하게 그려서, 보이는 것과 실제 확률이 같다.
 * 당첨 결과는 {@link com.todaysdrive.core.reward.Roulette}가 먼저 정하고, 이 화면은 그 칸에 멈추는 연출만 한다.
 */
public class RouletteView extends View {

    private static final int[] SLICE_COLORS = {
            0xFFF2B84B, 0xFF4FB3A9, 0xFFE07A5F, 0xFF81B29A, 0xFF9C89B8, 0xFFF4A261, 0xFF5C6773
    };
    private static final float POINTER_ANGLE = 270f; // 12시 방향
    private static final long SPIN_MILLIS = 4000;

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path pointer = new Path();

    private final List<String> labels = new ArrayList<>();
    private float[] starts = new float[0];
    private float[] sweeps = new float[0];
    private float rotationDeg;
    private ValueAnimator animator;

    public RouletteView(Context context) {
        this(context, null);
    }

    public RouletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        slicePaint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(0xFF12161C);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setColor(Color.WHITE);
        pointerPaint.setColor(Color.WHITE);
        setContentDescription("룰렛");
    }

    public void setPrizes(PrizeTable table) {
        List<Prize> prizes = table.prizes();
        labels.clear();
        starts = new float[prizes.size()];
        sweeps = new float[prizes.size()];
        float angle = 0;
        for (int i = 0; i < prizes.size(); i++) {
            Prize p = prizes.get(i);
            starts[i] = angle;
            sweeps[i] = 360f * p.weight() / table.totalWeight();
            angle += sweeps[i];
            labels.add(shortLabel(p.name()));
        }
        invalidate();
    }

    /** "차량 정비권(엔진오일 교환)" → "차량 정비권" */
    private static String shortLabel(String name) {
        int paren = name.indexOf('(');
        return paren > 0 ? name.substring(0, paren).trim() : name;
    }

    public boolean isSpinning() {
        return animator != null && animator.isRunning();
    }

    /** index 칸이 12시 방향에 오도록 몇 바퀴 돌다가 멈춘다. */
    public void spinTo(int index, final Runnable onEnd) {
        if (index < 0 || index >= starts.length || isSpinning()) {
            return;
        }
        float center = starts[index] + sweeps[index] / 2f;
        float current = ((rotationDeg % 360f) + 360f) % 360f;
        float target = ((POINTER_ANGLE - center) % 360f + 360f) % 360f;
        float delta = ((target - current) % 360f + 360f) % 360f;
        float end = rotationDeg + 360f * 5 + delta;

        animator = ValueAnimator.ofFloat(rotationDeg, end);
        animator.setDuration(SPIN_MILLIS);
        animator.setInterpolator(new DecelerateInterpolator(2.2f));
        animator.addUpdateListener(a -> {
            rotationDeg = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rotationDeg = rotationDeg % 360f;
                if (onEnd != null) {
                    onEnd.run();
                }
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(w, h);
        if (size == 0) {
            size = Math.max(w, h);
        }
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.88f;
        if (radius <= 0 || starts.length == 0) {
            return;
        }
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);
        labelPaint.setTextSize(radius * 0.085f);
        rimPaint.setStrokeWidth(radius * 0.025f);

        for (int i = 0; i < starts.length; i++) {
            slicePaint.setColor(SLICE_COLORS[i % SLICE_COLORS.length]);
            canvas.drawArc(oval, starts[i] + rotationDeg, sweeps[i], true, slicePaint);
            if (sweeps[i] >= 14f) { // 너무 좁은 칸은 글자를 생략(확률표에 표시)
                canvas.save();
                canvas.rotate(starts[i] + rotationDeg + sweeps[i] / 2f, cx, cy);
                canvas.drawText(labels.get(i), cx + radius * 0.6f, cy + labelPaint.getTextSize() / 3f, labelPaint);
                canvas.restore();
            }
        }
        canvas.drawCircle(cx, cy, radius, rimPaint);
        canvas.drawCircle(cx, cy, radius * 0.12f, pointerPaint);

        float tip = cy - radius * 0.9f;
        float half = radius * 0.07f;
        pointer.reset();
        pointer.moveTo(cx, tip);
        pointer.lineTo(cx - half, cy - radius - half * 1.6f);
        pointer.lineTo(cx + half, cy - radius - half * 1.6f);
        pointer.close();
        canvas.drawPath(pointer, pointerPaint);
    }
}
