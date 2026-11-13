package com.todaysdrive.core.drive;

/**
 * 차량 신호 한 건. 차량 신호 콜백이 올 때마다 하나씩 만들어 시간 순서대로 {@link DriveAnalyzer}에 넣는다.
 *
 * <p>값은 신호 종류에 따라 다음처럼 해석한다.
 * <ul>
 *   <li>SPEED: km/h</li>
 *   <li>STEERING: 핸들 각도(도), 왼쪽 음수·오른쪽 양수</li>
 *   <li>GEAR: {@link GearState#ordinal()}</li>
 *   <li>TURN_SIGNAL: {@link TurnSignalState#ordinal()}</li>
 *   <li>BRAKE, WIPER, SEATBELT: 1 = 켜짐/밟힘/착용, 0 = 아님</li>
 * </ul>
 */
public final class SignalEvent {

    private final long timeMs;
    private final VehicleSignal signal;
    private final double value;

    private SignalEvent(long timeMs, VehicleSignal signal, double value) {
        this.timeMs = timeMs;
        this.signal = signal;
        this.value = value;
    }

    public static SignalEvent speed(long timeMs, double kmh) {
        return new SignalEvent(timeMs, VehicleSignal.SPEED, Math.abs(kmh));
    }

    public static SignalEvent steering(long timeMs, double degrees) {
        return new SignalEvent(timeMs, VehicleSignal.STEERING, degrees);
    }

    public static SignalEvent gear(long timeMs, GearState gear) {
        return new SignalEvent(timeMs, VehicleSignal.GEAR, gear.ordinal());
    }

    public static SignalEvent turnSignal(long timeMs, TurnSignalState state) {
        return new SignalEvent(timeMs, VehicleSignal.TURN_SIGNAL, state.ordinal());
    }

    public static SignalEvent brake(long timeMs, boolean pressed) {
        return new SignalEvent(timeMs, VehicleSignal.BRAKE, pressed ? 1 : 0);
    }

    public static SignalEvent wiper(long timeMs, boolean on) {
        return new SignalEvent(timeMs, VehicleSignal.WIPER, on ? 1 : 0);
    }

    public static SignalEvent seatbelt(long timeMs, boolean buckled) {
        return new SignalEvent(timeMs, VehicleSignal.SEATBELT, buckled ? 1 : 0);
    }

    /** 시간만 바꾼 복사본(시나리오를 이어 붙일 때 사용). */
    public SignalEvent shiftedBy(long offsetMs) {
        return new SignalEvent(timeMs + offsetMs, signal, value);
    }

    public long timeMs() {
        return timeMs;
    }

    public VehicleSignal signal() {
        return signal;
    }

    public double value() {
        return value;
    }

    public GearState gearValue() {
        return GearState.values()[(int) value];
    }

    public TurnSignalState turnSignalValue() {
        return TurnSignalState.values()[(int) value];
    }

    public boolean isOn() {
        return value != 0;
    }

    @Override
    public String toString() {
        return timeMs + "," + signal + "," + value;
    }
}
