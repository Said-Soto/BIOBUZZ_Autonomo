package org.firstinspires.ftc.teamcode.constants;

import com.bylazar.configurables.annotations.Configurable;

/** Initial values must be verified on the actual robot before calibration. */
@Configurable
public final class TurretConfig {
  private TurretConfig() { }

  public static String motorName = "turretMotor";
  public static boolean inverted = false;
  // Reserved for vision integration; negative values mean not configured.
  public static String limelightName = "limelight";
  public static int limelightPipeline = -1;
  public static int limelightTagId = -1;
  public static double motorTicksPerRevolution = 28.0;
  public static double gearboxRatio3 = 2.89;
  public static double gearboxRatio4 = 3.61;
  public static double motorGearTeeth = 47.0;
  public static double turretGearTeeth = 100.0;

  // Provisional 180-degree total travel; verify cable clearance before motion.
  public static double minAngleDegrees = -90.0;
  public static double maxAngleDegrees = 90.0;
  public static double positionKp = 0.01; // power per degree; initial estimate
  public static double toleranceDegrees = 2.0;
  public static double maxPower = 0.15;
}
