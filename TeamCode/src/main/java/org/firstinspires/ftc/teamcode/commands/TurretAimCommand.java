package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import java.util.function.BooleanSupplier;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.constants.OdometryAimConfig;
import org.firstinspires.ftc.teamcode.control.TurretAimGeometry;
import org.firstinspires.ftc.teamcode.subsystems.PinpointSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;

/** Tracks a selected field point; it does not decide whether a CELL is currently scorable. */
public class TurretAimCommand extends CommandBase {
  private final TurretSubsystem turret;
  private final PinpointSubsystem odometry;
  private final BooleanSupplier centerButton, aimButton;
  private final boolean red;
  private boolean centerWasPressed, aimWasPressed, tracking;
  private String status = "Confirmar centro con Y";
  private double poseX = Double.NaN, poseY = Double.NaN, poseHeading = Double.NaN;

  public TurretAimCommand(TurretSubsystem turret, PinpointSubsystem odometry,
      BooleanSupplier centerButton, BooleanSupplier aimButton, boolean red) {
    this.turret = turret; this.odometry = odometry; this.centerButton = centerButton;
    this.aimButton = aimButton; this.red = red;
    addRequirements(turret, odometry);
  }

  @Override public void initialize() {
    tracking = false; turret.stop();
    centerWasPressed = centerButton.getAsBoolean(); aimWasPressed = aimButton.getAsBoolean();
  }

  @Override public void execute() {
    boolean center = centerButton.getAsBoolean(), aim = aimButton.getAsBoolean();
    try {
      Pose2D pose = odometry.readPose();
      poseX = pose == null ? Double.NaN : pose.getX(DistanceUnit.INCH);
      poseY = pose == null ? Double.NaN : pose.getY(DistanceUnit.INCH);
      poseHeading = pose == null ? Double.NaN : pose.getHeading(AngleUnit.DEGREES);
      if (center && !centerWasPressed && !tracking) {
        status = turret.confirmCenter() ? "Centro confirmado" : "Configuracion de torreta invalida";
      }
      if (aim && !aimWasPressed) {
        if (tracking) { tracking = false; turret.stop(); status = "Seguimiento apagado"; }
        else if (pose != null && turret.isCalibrated()) { tracking = true; status = "Seguimiento activo"; }
        else status = "Requiere Pinpoint listo y centro confirmado";
      }
      if (tracking) {
        if (pose == null || !turret.isCalibrated()) { fail("Pose o calibracion invalida"); }
        else {
          double x = pose.getX(DistanceUnit.INCH), y = pose.getY(DistanceUnit.INCH);
          boolean right = TurretAimGeometry.rightSide(x, y, OdometryAimConfig.splitOnX,
              OdometryAimConfig.splitCoordinateInches, OdometryAimConfig.rightOnGreaterSide);
          double tx = red ? (right ? OdometryAimConfig.redRightX : OdometryAimConfig.redLeftX)
              : (right ? OdometryAimConfig.blueRightX : OdometryAimConfig.blueLeftX);
          double ty = red ? (right ? OdometryAimConfig.redRightY : OdometryAimConfig.redLeftY)
              : (right ? OdometryAimConfig.blueRightY : OdometryAimConfig.blueLeftY);
          double target = TurretAimGeometry.targetDegrees(x, y, pose.getHeading(AngleUnit.DEGREES),
              OdometryAimConfig.turretForwardInches, OdometryAimConfig.turretLeftInches,
              OdometryAimConfig.turretZeroDegrees, tx, ty);
          if (!turret.setTargetDegrees(target)) fail("Objetivo fuera de alcance o invalido");
          else { turret.update(); status = right ? "Objetivo derecho" : "Objetivo izquierdo"; }
        }
      }
    } catch (RuntimeException e) { fail("Error de lectura de odometria o torreta"); }
    centerWasPressed = center; aimWasPressed = aim;
  }

  private void fail(String reason) { tracking = false; status = reason; turret.stop(); }
  public String getStatus() { return status; }
  public double getPoseX() { return poseX; }
  public double getPoseY() { return poseY; }
  public double getPoseHeading() { return poseHeading; }
  @Override public void end(boolean interrupted) { tracking = false; turret.stop(); }
}
