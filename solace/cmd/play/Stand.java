package solace.cmd.play;

import solace.game.*;
import solace.net.*;
import solace.util.*;
import java.util.*;
import java.io.*;

/**
 * Allows the player to stand up from a sitting, resting, or sleeping state.
 * @author Ryan Sandor Richards
 */
public class Stand extends PlayCommand {
  public Stand(solace.game.Character ch) {
    super("stand", ch);
  }

  public boolean run(Connection c, String []params) {
    Room room = character.getRoom();

    if (character.isStandingOrFighting()) {
      character.sendln("You are already standing.");
      return false;
    }

    String characterMessage = "";
    String roomFormat = "";

    if (character.isSitting()) {
      characterMessage = "You stand up.";
      roomFormat = "%s stands up.";
    } else if (character.isSleeping()) {
      characterMessage = "You awake and stand up.";
      roomFormat = "%s wakes and stands up.";
    } else if (character.isResting()) {
      characterMessage = "You stop resting and stand up.";
      roomFormat = "%s stops resting and stands up.";
    }

    room.sendMessage(
      String.format(roomFormat, character.getName()),
      character
    );
    character.sendln(characterMessage);
    character.setStanding();

    return true;
  }
}
