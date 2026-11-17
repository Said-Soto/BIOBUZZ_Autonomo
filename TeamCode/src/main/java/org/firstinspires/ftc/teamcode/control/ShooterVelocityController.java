package org.firstinspires.ftc.teamcode.control;

import org.firstinspires.ftc.teamcode.constants.RobotConstants;

/** FF + PD in RPM; derivative is taken on measurement to avoid setpoint kick. */
public class ShooterVelocityController {
  private boolean hasPreviousMeasurement;
  private double previousRpm;
  private double filteredAcceleration;

  public double calculate(double targetRpm, double measuredRpm, double dtSeconds) {
    if (!Double.isFinite(targetRpm) || !Double.isFinite(measuredRpm) || targetRpm <= 0) {
      reset();
      return 0;
    }
    if (hasPreviousMeasurement && Double.isFinite(dtSeconds)
        && dtSeconds > 0 && dtSeconds <= 0.25) {
      double acceleration = (measuredRpm - previousRpm) / dtSeconds;
      double alpha = dtSeconds / (RobotConstants.SHOOTER_DERIVATIVE_FILTER_SECONDS + dtSeconds);
      filteredAcceleration += alpha * (acceleration - filteredAcceleration);
    } else {
      filteredAcceleration = 0;
    }
    previousRpm = measuredRpm;
    hasPreviousMeasurement = true;
    double power = RobotConstants.SHOOTER_KF * targetRpm
        + RobotConstants.SHOOTER_KP * (targetRpm - measuredRpm)
        - RobotConstants.SHOOTER_KD * filteredAcceleration;
    return Math.max(0, Math.min(RobotConstants.SHOOTER_MAX_POWER, power));
  }

  public void reset() {
    hasPreviousMeasurement = false;
    previousRpm = 0;
    filteredAcceleration = 0;
  }
}
