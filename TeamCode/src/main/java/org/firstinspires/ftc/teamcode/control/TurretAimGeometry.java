package org.firstinspires.ftc.teamcode.control;

/** Field-frame aiming geometry; angles are degrees CCW and distances share one unit. */
public final class TurretAimGeometry {
  private TurretAimGeometry() { }

  /** Select the right-side target using an explicitly configured field axis and boundary. */
  public static boolean rightSide(double x, double y, boolean splitOnX,
      double boundary, boolean rightOnGreaterSide) {
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(boundary)) {
      throw new IllegalArgumentException("Invalid field position or boundary");
    }
    return ((splitOnX ? x : y) >= boundary) == rightOnGreaterSide;
  }

  /** Returns NaN for invalid inputs or an undefined bearing at the target. */
  public static double targetDegrees(double robotX, double robotY, double headingDegrees,
      double turretForward, double turretLeft, double turretZeroDegrees,
      double targetX, double targetY) {
    double[] values = {robotX, robotY, headingDegrees, turretForward, turretLeft,
        turretZeroDegrees, targetX, targetY};
    for (double value : values) if (!Double.isFinite(value)) return Double.NaN;
    double heading = Math.toRadians(headingDegrees);
    double pivotX = robotX + turretForward * Math.cos(heading) - turretLeft * Math.sin(heading);
    double pivotY = robotY + turretForward * Math.sin(heading) + turretLeft * Math.cos(heading);
    double dx = targetX - pivotX;
    double dy = targetY - pivotY;
    if (Math.hypot(dx, dy) < 1e-9) return Double.NaN;
    double angle = Math.toDegrees(Math.atan2(dy, dx)) - headingDegrees - turretZeroDegrees;
    return ((angle + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
  }
}
