package org.firstinspires.ftc.teamcode.opmodes;

import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;
import org.firstinspires.ftc.teamcode.commands.DriveCommand;
import org.firstinspires.ftc.teamcode.commands.IntakeCommand;
import org.firstinspires.ftc.teamcode.commands.ShooterCommand;
import org.firstinspires.ftc.teamcode.commands.TurretAimCommand;
import org.firstinspires.ftc.teamcode.constants.OdometryAimConfig;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.PinpointSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;

public abstract class TeleOpMain extends CommandOpMode {
  private DriveSubsystem driveSubsystem;
  private IntakeSubsystem intakeSubsystem;
  private ShooterSubsystem shooterSubsystem;
  private TurretSubsystem turretSubsystem;
  private PinpointSubsystem pinpointSubsystem;
  private TurretAimCommand turretCommand;
  private GamepadEx driverGamepad, operatorGamepad;
  private String turretStatus = "Configurar odometria y objetivos; reiniciar OpMode";

  protected abstract boolean isRedAlliance();

  @Override public void initialize() {
    reset();
    driverGamepad = new GamepadEx(gamepad1);
    operatorGamepad = new GamepadEx(gamepad2);
    driveSubsystem = new DriveSubsystem(hardwareMap);
    intakeSubsystem = new IntakeSubsystem(hardwareMap);
    shooterSubsystem = new ShooterSubsystem(hardwareMap);
    if (OdometryAimConfig.valid()) {
      try {
        turretSubsystem = new TurretSubsystem(hardwareMap);
        pinpointSubsystem = new PinpointSubsystem(hardwareMap);
        turretCommand = new TurretAimCommand(turretSubsystem, pinpointSubsystem,
            () -> operatorGamepad.getButton(GamepadKeys.Button.Y),
            () -> operatorGamepad.getButton(GamepadKeys.Button.RIGHT_BUMPER), isRedAlliance());
        turretSubsystem.setDefaultCommand(turretCommand);
        turretStatus = "Mantener robot quieto; confirmar centro con Y";
      } catch (RuntimeException e) {
        if (turretSubsystem != null) turretSubsystem.stop();
        turretStatus = "No se pudo inicializar torreta/Pinpoint: revisar hardware";
      }
    }
    configureCommands();
    telemetry.addData("Alianza", isRedAlliance() ? "Roja" : "Azul");
    telemetry.addData("Torreta", turretStatus);
    telemetry.update();
  }

  @Override public void initialize_loop() {
    if (pinpointSubsystem != null) {
      try {
        telemetry.addData("Pinpoint", pinpointSubsystem.readPose() == null
            ? "Calibrando: mantener robot inmovil" : "Pose inicial lista");
      } catch (RuntimeException e) {
        telemetry.addData("Pinpoint", "Error de lectura; seguimiento deshabilitado");
      }
    }
    telemetry.addData("Torreta", turretStatus);
    telemetry.update();
  }

  @Override public void preRun() {
    if (pinpointSubsystem != null) pinpointSubsystem.finishInitialization();
  }

  private void configureCommands() {
    driveSubsystem.setDefaultCommand(new DriveCommand(driveSubsystem,
        driverGamepad::getLeftX, driverGamepad::getLeftY, driverGamepad::getRightX));
    intakeSubsystem.setDefaultCommand(new IntakeCommand(intakeSubsystem,
        () -> driverGamepad.getButton(GamepadKeys.Button.A),
        () -> driverGamepad.getButton(GamepadKeys.Button.B)));
    shooterSubsystem.setDefaultCommand(new ShooterCommand(shooterSubsystem,
        () -> driverGamepad.getButton(GamepadKeys.Button.X)));
  }

  @Override public void run() {
    super.run();
    telemetry.addData("Alianza", isRedAlliance() ? "Roja" : "Azul");
    telemetry.addData("Torreta", turretCommand == null ? turretStatus : turretCommand.getStatus());
    if (turretCommand != null) {
      telemetry.addData("Pinpoint X / Y / rumbo", "%.1f / %.1f pulg / %.1f grados",
          turretCommand.getPoseX(), turretCommand.getPoseY(), turretCommand.getPoseHeading());
    }
    if (turretSubsystem != null) {
      telemetry.addData("Torreta actual / objetivo", "%.1f / %.1f grados",
          turretSubsystem.getCurrentDegrees(), turretSubsystem.getTargetDegrees());
      telemetry.addData("Torreta potencia", turretSubsystem.getOutput());
    }
    telemetry.addData("Shooter objetivo (RPM)", shooterSubsystem.getTargetRpm());
    telemetry.addData("Shooter velocidad (RPM)", shooterSubsystem.getVelocityRpm());
    telemetry.addData("Shooter potencia", shooterSubsystem.getOutputPower());
    telemetry.addData("Potencia maxima chasis", RobotConstants.DRIVE_MAX_POWER);
    telemetry.update();
  }

  @Override public void end() {
    try { CommandScheduler.getInstance().cancelAll(); }
    finally {
      try { if (turretSubsystem != null) turretSubsystem.stop(); }
      finally {
        try { if (shooterSubsystem != null) shooterSubsystem.stop(); }
        finally {
          try { if (intakeSubsystem != null) intakeSubsystem.stop(); }
          finally { if (driveSubsystem != null) driveSubsystem.stop(); }
        }
      }
    }
  }
}
