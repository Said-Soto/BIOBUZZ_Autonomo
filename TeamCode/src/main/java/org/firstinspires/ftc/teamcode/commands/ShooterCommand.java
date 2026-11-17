package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import java.util.function.BooleanSupplier;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

public class ShooterCommand extends CommandBase {
  private final ShooterSubsystem shooter;
  private final BooleanSupplier toggleButton;
  private boolean wasPressed;
  private boolean enabled;

  public ShooterCommand(ShooterSubsystem shooter, BooleanSupplier toggleButton) {
    this.shooter = shooter;
    this.toggleButton = toggleButton;
    addRequirements(shooter);
  }

  @Override
  public void initialize() {
    enabled = false;
    wasPressed = toggleButton.getAsBoolean();
    shooter.stop();
  }

  @Override
  public void execute() {
    boolean pressed = toggleButton.getAsBoolean();
    if (pressed && !wasPressed) {
      if (enabled) {
        shooter.stop();
        enabled = false;
      } else {
        double rpm = RobotConstants.SHOOTER_TARGET_RPM;
        if (Double.isFinite(rpm) && rpm > 0 && rpm <= RobotConstants.SHOOTER_FREE_SPEED_RPM) {
          shooter.setTargetRpm(rpm);
          enabled = true;
        }
      }
    }
    wasPressed = pressed;
    if (enabled) shooter.update();
  }

  @Override
  public void end(boolean interrupted) {
    enabled = false;
    shooter.stop();
  }
}
