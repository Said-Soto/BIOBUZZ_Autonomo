package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.constants.OdometryAimConfig;

public class PinpointSubsystem extends SubsystemBase {
  private final GoBildaPinpointDriver pinpoint;
  private final String configurationSignature;
  private boolean poseInitialized;
  private boolean initializationClosed;

  public PinpointSubsystem(HardwareMap hardwareMap) {
    if (!OdometryAimConfig.valid()) throw new IllegalStateException("Odometry not configured");
    configurationSignature = OdometryAimConfig.signature();
    pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, OdometryAimConfig.pinpointName);
    pinpoint.setOffsets(OdometryAimConfig.xPodOffsetMm, OdometryAimConfig.yPodOffsetMm, DistanceUnit.MM);
    pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
    pinpoint.setEncoderDirections(
        OdometryAimConfig.reverseX ? GoBildaPinpointDriver.EncoderDirection.REVERSED : GoBildaPinpointDriver.EncoderDirection.FORWARD,
        OdometryAimConfig.reverseY ? GoBildaPinpointDriver.EncoderDirection.REVERSED : GoBildaPinpointDriver.EncoderDirection.FORWARD);
    // Robot must stay still during IMU calibration; initial field pose is applied once READY.
    pinpoint.resetPosAndIMU();
  }

  public void finishInitialization() { initializationClosed = true; }

  /** Returns null if the pose is not trustworthy or configuration was changed. */
  public Pose2D readPose() {
    if (!OdometryAimConfig.valid() || !configurationSignature.equals(OdometryAimConfig.signature())) return null;
    pinpoint.update();
    if (pinpoint.getDeviceStatus() != GoBildaPinpointDriver.DeviceStatus.READY) return null;
    if (!poseInitialized) {
      if (initializationClosed) return null;
      pinpoint.setPosition(new Pose2D(DistanceUnit.INCH, OdometryAimConfig.initialXInches,
          OdometryAimConfig.initialYInches, AngleUnit.DEGREES, OdometryAimConfig.initialHeadingDegrees));
      poseInitialized = true;
      // Require a subsequent hardware read after setting pose.
      return null;
    }
    Pose2D pose = pinpoint.getPosition();
    if (pose == null || !Double.isFinite(pose.getX(DistanceUnit.INCH))
        || !Double.isFinite(pose.getY(DistanceUnit.INCH))
        || !Double.isFinite(pose.getHeading(AngleUnit.DEGREES))) return null;
    return pose;
  }
}
