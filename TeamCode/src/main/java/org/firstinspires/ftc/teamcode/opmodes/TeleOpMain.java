package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;
import org.firstinspires.ftc.teamcode.commands.DriveCommand;
import org.firstinspires.ftc.teamcode.commands.IntakeCommand;
import org.firstinspires.ftc.teamcode.commands.ShooterCommand;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

@TeleOp(name = "SolversLib: TeleOp Mecanum", group = "TeleOp")
public class TeleOpMain extends CommandOpMode {
  private DriveSubsystem driveSubsystem;
  private IntakeSubsystem intakeSubsystem;
  private ShooterSubsystem shooterSubsystem;
  private GamepadEx driverGamepad;

  @Override
  public void initialize() {
    reset();
    driverGamepad = new GamepadEx(gamepad1);
    driveSubsystem = new DriveSubsystem(hardwareMap);
    intakeSubsystem = new IntakeSubsystem(hardwareMap);
    shooterSubsystem = new ShooterSubsystem(hardwareMap);
    configureCommands();
    telemetry.addData("Estado", "Chasis, intake y shooter listos");
    telemetry.addData("Potencia maxima", RobotConstants.DRIVE_MAX_POWER);
    telemetry.update();
  }

  private void configureCommands() {
    driveSubsystem.setDefaultCommand(new DriveCommand(
        driveSubsystem, driverGamepad::getLeftX, driverGamepad::getLeftY,
        driverGamepad::getRightX));
    intakeSubsystem.setDefaultCommand(new IntakeCommand(
        intakeSubsystem, () -> driverGamepad.getButton(GamepadKeys.Button.A),
        () -> driverGamepad.getButton(GamepadKeys.Button.B)));
    shooterSubsystem.setDefaultCommand(new ShooterCommand(
        shooterSubsystem, () -> driverGamepad.getButton(GamepadKeys.Button.X)));
  }

  @Override
  public void run() {
    super.run();
    telemetry.addData("Estado", "TeleOp activo");
    telemetry.addData("Shooter objetivo (RPM)", shooterSubsystem.getTargetRpm());
    telemetry.addData("Shooter velocidad (RPM)", shooterSubsystem.getVelocityRpm());
    telemetry.addData("Shooter potencia", shooterSubsystem.getOutputPower());
    if (RobotConstants.SHOOTER_TARGET_RPM <= 0) {
      telemetry.addData("Shooter", "Configurar RPM objetivo antes de usar");
    }
    telemetry.addData("Potencia maxima", RobotConstants.DRIVE_MAX_POWER);
    telemetry.update();
  }

  @Override
  public void end() {
    try {
      CommandScheduler.getInstance().cancelAll();
    } finally {
      try {
        if (shooterSubsystem != null) shooterSubsystem.stop();
      } finally {
        try {
          if (intakeSubsystem != null) intakeSubsystem.stop();
        } finally {
          if (driveSubsystem != null) driveSubsystem.stop();
        }
      }
    }
  }
}
