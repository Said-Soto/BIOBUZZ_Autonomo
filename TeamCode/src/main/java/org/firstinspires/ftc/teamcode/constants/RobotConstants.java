package org.firstinspires.ftc.teamcode.constants;

public class RobotConstants {
  // Hardware map names (Mecanum Chassis)
  public static final String FRONT_LEFT_MOTOR = "front_left";
  public static final String FRONT_RIGHT_MOTOR = "front_right";
  public static final String BACK_LEFT_MOTOR = "back_left";
  public static final String BACK_RIGHT_MOTOR = "back_right";

  // Hardware map names (Intake)
  public static final String INTAKE_MOTOR = "intakeMotor";
  public static final String TRANSFER_MOTOR = "transferMotor";

  // Shooter motor configuration name.
  public static final String SHOOTER_MOTOR = "shooterMotor";
  // REV HD Hex bare motor encoder; wheel transmission is 1:1.
  public static final double SHOOTER_TICKS_PER_REVOLUTION = 28.0;
  public static final double SHOOTER_FREE_SPEED_RPM = 6000.0;
  // Initial wheel speed target; verify and tune on the robot.
  public static final double SHOOTER_TARGET_RPM = 3500.0;
  // Initial estimates only: output = kF * targetRPM + kP * errorRPM - kD * measuredAcceleration.
  public static final double SHOOTER_KF = 1.0 / SHOOTER_FREE_SPEED_RPM;
  public static final double SHOOTER_KP = 0.0001;
  public static final double SHOOTER_KD = 0.000002;
  public static final double SHOOTER_DERIVATIVE_FILTER_SECONDS = 0.10;
  public static final double SHOOTER_MAX_POWER = 1.0;

  // Limit initial chassis tests; adjust only after physical verification.
  public static final double DRIVE_MAX_POWER = 0.25;

  // Speed modifiers
  public static final double INTAKE_POWER = 0.25;
}
