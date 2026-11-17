package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import java.util.function.BooleanSupplier;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;

public class IntakeCommand extends CommandBase {
  private final IntakeSubsystem intake;
  private final BooleanSupplier forward;
  private final BooleanSupplier reverse;

  public IntakeCommand(IntakeSubsystem intake, BooleanSupplier forward, BooleanSupplier reverse) {
    this.intake = intake;
    this.forward = forward;
    this.reverse = reverse;
    addRequirements(intake);
  }

  @Override
  public void execute() {
    boolean forwardPressed = forward.getAsBoolean();
    boolean reversePressed = reverse.getAsBoolean();
    if (forwardPressed == reversePressed) {
      intake.stop();
    } else if (forwardPressed) {
      intake.intake();
    } else {
      intake.outtake();
    }
  }

  @Override
  public void end(boolean interrupted) {
    intake.stop();
  }
}
