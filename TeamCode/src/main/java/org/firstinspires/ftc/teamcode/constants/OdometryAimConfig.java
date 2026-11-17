package org.firstinspires.ftc.teamcode.constants;

import com.bylazar.configurables.annotations.Configurable;

/** Field coordinates are inches; heading and turret mounting angles are CCW degrees. */
@Configurable
public final class OdometryAimConfig {
  private OdometryAimConfig() { }
  // Set true only after measuring every value and testing the coordinate convention.
  public static boolean measuredConfigurationConfirmed = false;
  public static String pinpointName = "pinpoint";
  public static double xPodOffsetMm = 0;
  public static double yPodOffsetMm = 0;
  public static boolean reverseX = false;
  public static boolean reverseY = false;
  public static double initialXInches = 0;
  public static double initialYInches = 0;
  public static double initialHeadingDegrees = 0;
  public static double turretForwardInches = 0;
  public static double turretLeftInches = 0;
  public static double turretZeroDegrees = 0;
  // Robot position on the selected field axis chooses the CELL target.
  public static boolean splitOnX = true;
  public static double splitCoordinateInches = 0;
  public static boolean rightOnGreaterSide = true;
  public static double redLeftX = 0, redLeftY = 0, redRightX = 0, redRightY = 0;
  public static double blueLeftX = 0, blueLeftY = 0, blueRightX = 0, blueRightY = 0;

  public static boolean valid() {
    double[] values = {xPodOffsetMm, yPodOffsetMm, initialXInches, initialYInches,
        initialHeadingDegrees, turretForwardInches, turretLeftInches, turretZeroDegrees,
        splitCoordinateInches, redLeftX, redLeftY, redRightX, redRightY,
        blueLeftX, blueLeftY, blueRightX, blueRightY};
    if (!measuredConfigurationConfirmed || pinpointName == null || pinpointName.trim().isEmpty()) return false;
    for (double value : values) if (!Double.isFinite(value)) return false;
    return (redLeftX != redRightX || redLeftY != redRightY)
        && (blueLeftX != blueRightX || blueLeftY != blueRightY);
  }

  /** Snapshot used to stop tracking if field calibration changes during an OpMode. */
  public static String signature() {
    return pinpointName + ":" + reverseX + ":" + reverseY + ":" + splitOnX + ":" + rightOnGreaterSide
        + java.util.Arrays.toString(new double[] {xPodOffsetMm, yPodOffsetMm, initialXInches,
        initialYInches, initialHeadingDegrees, turretForwardInches, turretLeftInches,
        turretZeroDegrees, splitCoordinateInches, redLeftX, redLeftY, redRightX, redRightY,
        blueLeftX, blueLeftY, blueRightX, blueRightY});
  }
}
