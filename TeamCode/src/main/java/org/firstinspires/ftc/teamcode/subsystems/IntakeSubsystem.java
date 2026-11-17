package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;

public class IntakeSubsystem extends SubsystemBase {
  private final Motor intakeMotor;
  private final Motor transferMotor;

  public IntakeSubsystem(HardwareMap hardwareMap) {
    intakeMotor = configureMotor(hardwareMap, RobotConstants.INTAKE_MOTOR);
    transferMotor = configureMotor(hardwareMap, RobotConstants.TRANSFER_MOTOR);
  }

  private Motor configureMotor(HardwareMap hardwareMap, String name) {
    Motor motor = new Motor(hardwareMap, name);
    motor.motor.setPower(0);
    motor.setInverted(false);
    motor.motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    motor.setRunMode(Motor.RunMode.RawPower);
    motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
    return motor;
  }

  public void setPower(double power) {
    intakeMotor.set(power);
    transferMotor.set(power);
  }

  public void intake() {
    setPower(RobotConstants.INTAKE_POWER);
  }

  public void outtake() {
    setPower(-RobotConstants.INTAKE_POWER);
  }

  public void stop() {
    try {
      intakeMotor.stopMotor();
    } finally {
      transferMotor.stopMotor();
    }
  }
}
