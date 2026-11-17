package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.controller.PController;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import org.firstinspires.ftc.teamcode.constants.TurretConfig;

/** Relative encoder position is valid only after the operator confirms physical center. */
public class TurretSubsystem extends SubsystemBase {
  private final MotorEx motor;
  private final PController controller = new PController(0);
  private boolean calibrated;
  private boolean hasTarget;
  private int centerTicks;
  private double ticksPerTurn;
  private double minAngle;
  private double maxAngle;
  private double targetDegrees;
  private double currentDegrees;
  private double output;
  private boolean inverted;
  private String motorName;

  public TurretSubsystem(HardwareMap hardwareMap) {
    motorName = TurretConfig.motorName;
    motor = new MotorEx(hardwareMap, motorName);
    motor.motor.setPower(0);
    motor.setInverted(TurretConfig.inverted);
    motor.motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    motor.setRunMode(Motor.RunMode.RawPower);
    motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
  }

  /** Call only after physically positioning the stopped turret at its known center. */
  public boolean confirmCenter() {
    stop();
    double ticks = configuredTicksPerTurn();
    if (!validGeometry(ticks) || !motorName.equals(TurretConfig.motorName)) return false;
    motor.setInverted(TurretConfig.inverted);
    centerTicks = motor.getCurrentPosition();
    ticksPerTurn = ticks;
    minAngle = TurretConfig.minAngleDegrees;
    maxAngle = TurretConfig.maxAngleDegrees;
    inverted = TurretConfig.inverted;
    calibrated = true;
    currentDegrees = 0;
    targetDegrees = 0;
    return true;
  }

  /** Target in turret degrees from the calibrated center; unreachable targets are rejected. */
  public boolean setTargetDegrees(double angle) {
    if (!isCalibrationValid() || !Double.isFinite(angle) || angle < minAngle || angle > maxAngle) {
      stop();
      return false;
    }
    targetDegrees = angle;
    hasTarget = true;
    return true;
  }

  /** Run regularly only while a command owns this subsystem. */
  public void update() {
    if (!isCalibrationValid() || !validControl() || !hasTarget) {
      stop();
      return;
    }
    currentDegrees = (motor.getCurrentPosition() - (double) centerTicks) * 360.0 / ticksPerTurn;
    controller.setP(TurretConfig.positionKp);
    controller.setSetPoint(targetDegrees);
    output = controller.calculate(currentDegrees);
    output = Math.max(-TurretConfig.maxPower, Math.min(TurretConfig.maxPower, output));
    if (Math.abs(targetDegrees - currentDegrees) <= TurretConfig.toleranceDegrees
        || (currentDegrees <= minAngle && output < 0)
        || (currentDegrees >= maxAngle && output > 0)) output = 0;
    motor.set(output);
  }

  public void stop() {
    hasTarget = false;
    output = 0;
    controller.reset();
    motor.set(0);
  }

  public boolean isCalibrated() { return isCalibrationValid(); }
  public boolean hasTarget() { return hasTarget; }
  public double getCurrentDegrees() { return currentDegrees; }
  public double getTargetDegrees() { return targetDegrees; }
  public double getOutput() { return output; }

  private boolean isCalibrationValid() {
    double configuredTicks = configuredTicksPerTurn();
    if (calibrated && (!validGeometry(configuredTicks) || configuredTicks != ticksPerTurn
        || minAngle != TurretConfig.minAngleDegrees || maxAngle != TurretConfig.maxAngleDegrees
        || inverted != TurretConfig.inverted || !motorName.equals(TurretConfig.motorName))) {
      calibrated = false;
    }
    return calibrated;
  }

  private double configuredTicksPerTurn() {
    return TurretConfig.motorTicksPerRevolution * TurretConfig.gearboxRatio3
        * TurretConfig.gearboxRatio4 * TurretConfig.turretGearTeeth / TurretConfig.motorGearTeeth;
  }

  private boolean validGeometry(double ticks) {
    return Double.isFinite(ticks) && ticks > 0
        && Double.isFinite(TurretConfig.motorTicksPerRevolution) && TurretConfig.motorTicksPerRevolution > 0
        && Double.isFinite(TurretConfig.gearboxRatio3) && TurretConfig.gearboxRatio3 > 0
        && Double.isFinite(TurretConfig.gearboxRatio4) && TurretConfig.gearboxRatio4 > 0
        && Double.isFinite(TurretConfig.motorGearTeeth) && TurretConfig.motorGearTeeth > 0
        && Double.isFinite(TurretConfig.turretGearTeeth) && TurretConfig.turretGearTeeth > 0
        && Double.isFinite(TurretConfig.minAngleDegrees) && TurretConfig.minAngleDegrees < 0
        && Double.isFinite(TurretConfig.maxAngleDegrees) && TurretConfig.maxAngleDegrees > 0;
  }

  private boolean validControl() {
    return Double.isFinite(TurretConfig.positionKp) && TurretConfig.positionKp > 0
        && Double.isFinite(TurretConfig.toleranceDegrees) && TurretConfig.toleranceDegrees >= 0
        && Double.isFinite(TurretConfig.maxPower) && TurretConfig.maxPower > 0
        && TurretConfig.maxPower <= 1;
  }
}
