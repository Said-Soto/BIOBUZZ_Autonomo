package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;

public class DriveSubsystem extends SubsystemBase {
  private final MecanumDrive drive;

  public DriveSubsystem(HardwareMap hardwareMap) {
    Motor frontLeft = configureMotor(hardwareMap, RobotConstants.FRONT_LEFT_MOTOR);
    Motor frontRight = configureMotor(hardwareMap, RobotConstants.FRONT_RIGHT_MOTOR);
    Motor backLeft = configureMotor(hardwareMap, RobotConstants.BACK_LEFT_MOTOR);
    Motor backRight = configureMotor(hardwareMap, RobotConstants.BACK_RIGHT_MOTOR);

    // MecanumDrive applies the right-side inversion to its output.
    drive = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);
    drive.setMaxSpeed(RobotConstants.DRIVE_MAX_POWER);
  }

  private Motor configureMotor(HardwareMap hardwareMap, String name) {
    Motor motor = new Motor(hardwareMap, name);
    motor.motor.setPower(0);
    motor.setInverted(false);
    motor.motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    motor.setRunMode(Motor.RunMode.RawPower);
    return motor;
  }

  public void driveRobotCentric(double strafe, double forward, double turn) {
    drive.driveRobotCentric(strafe, forward, turn);
  }

  public void stop() {
    drive.stop();
  }
}
