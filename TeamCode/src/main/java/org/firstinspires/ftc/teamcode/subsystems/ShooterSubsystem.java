package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;
import org.firstinspires.ftc.teamcode.control.ShooterVelocityController;

public class ShooterSubsystem extends SubsystemBase {
  private final MotorEx motor;
  private final ShooterVelocityController controller = new ShooterVelocityController();
  private double targetRpm;
  private double measuredRpm;
  private double outputPower;
  private long lastUpdateNanos;

  public ShooterSubsystem(HardwareMap hardwareMap) {
    motor = new MotorEx(hardwareMap, RobotConstants.SHOOTER_MOTOR);
    motor.motor.setPower(0);
    motor.setInverted(false);
    motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.FLOAT);
    // The software controller owns the output; encoder readings remain available.
    motor.motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    motor.setRunMode(Motor.RunMode.RawPower);
  }

  public void setTargetRpm(double rpm) {
    if (!Double.isFinite(rpm) || rpm < 0 || rpm > RobotConstants.SHOOTER_FREE_SPEED_RPM) {
      throw new IllegalArgumentException("Invalid shooter target RPM");
    }
    if (rpm == 0) {
      stop();
      return;
    }
    targetRpm = rpm;
    controller.reset();
    lastUpdateNanos = 0;
  }

  /** Called while the shooter command owns the subsystem. */
  public void update() {
    measuredRpm = motor.getVelocity() * 60.0 / RobotConstants.SHOOTER_TICKS_PER_REVOLUTION;
    if (targetRpm <= 0) return;
    long now = System.nanoTime();
    double dt = lastUpdateNanos == 0 ? 0 : (now - lastUpdateNanos) * 1e-9;
    lastUpdateNanos = now;
    outputPower = controller.calculate(targetRpm, measuredRpm, dt);
    motor.set(outputPower);
  }

  public double getTargetRpm() { return targetRpm; }
  public double getVelocityRpm() { return measuredRpm; }
  public double getOutputPower() { return outputPower; }

  public void stop() {
    targetRpm = 0;
    outputPower = 0;
    lastUpdateNanos = 0;
    controller.reset();
    // Use set(0) to keep MotorEx's cached power consistent for the next start.
    motor.set(0);
  }
}
