package net.sourceforge.kolmafia.textui.command;

import java.util.HashSet;
import java.util.Set;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HermitCommand extends AbstractCommand {
  public HermitCommand() {
    this.usage = "[?] [<item>] - get clover status, or trade for item.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!KoLmafia.permitsContinue()) {
      return;
    }

    int cloverCount = HermitRequest.cloverCount();

    if (parameters.equals("")) {
      KoLmafia.updateDisplay(
          "The Hermit has "
              + cloverCount
              + " clover"
              + (cloverCount == 1 ? "" : "s")
              + " available today.");
      return;
    }

    int count = 1;

    if (Character.isDigit(parameters.charAt(0))) {
      int spaceIndex = parameters.indexOf(" ");
      count = StringUtilities.parseInt(parameters.substring(0, spaceIndex));
      parameters = parameters.substring(spaceIndex);
    } else if (parameters.charAt(0) == '*') {
      int spaceIndex = parameters.indexOf(" ");
      count = Integer.MAX_VALUE;
      parameters = parameters.substring(spaceIndex);
    }

    parameters = parameters.toLowerCase().trim();
    int itemId = -1;

    if (KoLCharacter.inZombiecore() && parameters.contains("clover")) {
      if (!Preferences.getBoolean("_zombieClover")) {
        itemId = ItemPool.ELEVEN_LEAF_CLOVER;
      }
    } else {
      Set<String> names = new HashSet(ItemDatabase.getMatchingNames(parameters));
      for (var item : KoLConstants.hermitItems) {
        if (names.contains(item.getName())) {
          if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
            RequestLogger.printLine(item.getName());
            return;
          }

          itemId = item.getItemId();
          break;
        }
      }
    }

    if (itemId == -1) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You can't get a " + parameters + " from the hermit today.");
      return;
    }

    // "*" for clovers means all the hermit has available today.
    // For any other item, it means as many as you can get with
    // the worthless items you currently have

    count =
        count == Integer.MAX_VALUE
            ? itemId == ItemPool.ELEVEN_LEAF_CLOVER
                ? Math.min(count, cloverCount)
                : Math.min(count, HermitRequest.getWorthlessItemCount())
            : count;

    if (count > 0) {
      if (KoLCharacter.inZombiecore()) {
        RequestThread.postRequest(new HermitRequest());
      } else {
        RequestThread.postRequest(new HermitRequest(itemId, count));
      }
    }
  }
}
