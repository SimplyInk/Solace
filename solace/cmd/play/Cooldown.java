package solace.cmd.play;

import java.util.*;
import solace.game.*;
import solace.net.*;
import java.io.*;
import solace.util.*;

/**
 * Displays a list of non-global cooldowns and the time remaning for each.
 * @author Ryan Sandor Richards
 */
public class Cooldown extends PlayCommand {
  public Cooldown(solace.game.Character ch) {
    super("cooldown", ch);
  }

  public boolean run(Connection c, String []params) {
    StringBuilder buffer = new StringBuilder();
    for (Skill skill : character.getSkills()) {
      for (String cooldown : skill.getCooldowns()) {
        int time = character.getCooldownDuration(cooldown);
        if (time < 1) continue;
        buffer.append(String.format(
          "  [{C%3ds{x] {m%s{x\n\r", time, cooldown));
      }
    }

    if (buffer.length() > 0) {
      character.sendln("Actions on cooldown:\n\r");
      character.sendln(buffer.toString());
    } else {
      character.sendln("You have no skills on cooldown.");
    }

    return true;
  }
}
