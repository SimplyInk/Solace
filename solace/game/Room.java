package solace.game;

import java.util.*;
import solace.util.*;

/**
 * Basic room class for the engine.
 * @author Ryan Sandor Richards (Gaius)
 */
public class Room {
  String id;
  String title = "";
  String desc = "";
  Area area = null;
  LinkedList<Exit> exits = new LinkedList<Exit>();
  Hashtable<String, String> features = new Hashtable<String, String>();
  List<String> itemInstances = new LinkedList<String>();
  List<Player> characters;
  List<Item> items;
  Shop shop = null;
  List<Mobile> mobiles;

  /**
   * Creates a new room with the given id, title, and description.
   * @param i Id for the room.
   */
  public Room(String i) {
    id = i;
    characters = Collections.synchronizedList(new LinkedList<Player>());
    mobiles = Collections.synchronizedList(new LinkedList<Mobile>());
  }

  /**
   * Instantiates all templatable game objects for the room. This includes
   * items, mobiles, etc.
   */
  public void instantiate() {
    items = Collections.synchronizedList(new LinkedList<Item>());
    for (String id : itemInstances) {
      try {
        addItem(TemplateFactory.getInstance().getItem(id));
      }
      catch (TemplateNotFoundException e) {
        Log.error("Room.instantiate ("+this.id+"): " + e.getMessage());
      }
    }
  }

  /**
   * Sends a message to all of the characters in a room.
   * @param message Message to send.
   */
  public void sendMessage(String message) {
    synchronized(characters) {
      for (Player ch : characters) {
        ch.sendMessage(message);
      }
    }
  }

  /**
   * Sends a message to all of the characters in a room. Excluding the given
   * player character (useful for messages sent as a result of a character's
   * actions).
   * @param message Message to send.
   * @param exclude Player to exclude when sending the message.
   */
  public void sendMessage(String message, Player exclude) {
    synchronized(characters) {
      for (Player ch : characters) {
        if (ch == exclude)
          continue;
        ch.sendMessage(message);
      }
    }
  }

  /**
   * Sends a message to all of the characters in a room excepting those in the
   * given array of players.
   * @param message Message to send.
   * @param excludes Players to exclude when sending the message.
   */
  public void sendMessage(String message, Player[] excludes) {
    synchronized(characters) {
      for (Player ch : characters) {
        boolean exclude = false;
        for (Player x : excludes) {
          if (ch == x) {
            exclude = true;
            break;
          }
        }
        if (exclude) continue;
        ch.sendMessage(message);
      }
    }
  }

  /**
   * @return The list of characters and mobiles in the room.
   */
  public List<Player> getCharacters() {
    return characters;
  }

  /**
   * @return The list of the mobiles in the room.
   */
  public List<Mobile> getMobiles() {
    return mobiles;
  }

  /**
   * @param exclude Character or mobile to exclude.
   * @return A list of characters or mobiles excluding the one given.
   */
  public List<Player> getOtherCharacters(Player exclude) {
    List<Player> others = new LinkedList<Player>();
    synchronized (characters) {
      for (Player ch : characters) {
        if (ch == exclude)
          continue;
        others.add(ch);
      }
    }
    return Collections.unmodifiableList(others);
  }

  /**
   * Finds a character or mobile in the room with the given name prefix.
   * @param namePrefix Name prefix for the character to find.
   * @return The character or null if none was found.
   */
  public Player findPlayer(String namePrefix) {
    synchronized (characters) {
      for (Player p : characters) {
        if (p.hasName(namePrefix)) {
          return p;
        }
      }
    }
    return null;
  }

  /**
   * @return the area the room belongs to.
   */
  public Area getArea() {
    return area;
  }

  /**
   * @param a area for the room.
   */
  public void setArea(Area a) {
    area = a;
  }

  /**
   * @return The room's id.
   */
  public String getId() {
    return id;
  }

  /**
   * Adds an exit for the room.
   * @param e Exit to add to the room.
   */
  public void addExit(Exit e) {
    exits.add(e);
  }

  /**
   * Performs a prefix search that looks for an exit based off a direction
   * fragment.
   * @param fragment Fragment by which to perform the search.
   * @return The exit if an exit matching the fragment was found,
   *   <code>null</code> otherwise.
   */
  public Exit findExit(String fragment) {
    for (Exit e : exits)
      if (e.matches(fragment))
        return e;

    return null;
  }

  /**
   * @return an unmodifiable list of exits associated with this room.
   */
  public List<Exit> getExits() {
    return Collections.unmodifiableList(exits);
  }

  /**
   * @return the desc
   */
  public String getDescription() {
    return desc.trim();
  }

  /**
   * @param desc the desc to set
   */
  public void setDescription(String d) {
    desc = d;
  }

  /**
   * Appends more information to the description.
   * @param d Information to append.
   */
  public void addToDescription(String d) {
    desc += d;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Builds a string that discribes the room to the given character. This is the
   * method used by the game's look command. We define the method here because
   * it is used in various places ouside the scope of that command (after
   * moving/teleporting, upon login, etc.).
   *
   * @param ch Character who's perspective will be used.
   * @return A string describing the room.
   */
  public String describeTo(solace.game.Character ch) {
    List<Player> others = getOtherCharacters(ch);

    // Title and description of the room
    StringBuffer buffer = new StringBuffer();

    buffer.append("{y" + getTitle().trim() + "{x\n\r\n\r");
    buffer.append(
      Strings.toFixedWidth(getDescription(), 80).trim() +
      "\n\r\n\r"
    );

    // Show the items in the room
    if (items.size() > 0) {
      synchronized(items) {
        buffer.append("{cThe following items are present:{x\n\r");
        for (Item item : items) {
          buffer.append("    " + item.get("description.room") + "\n\r");
        }
      }
      buffer.append("\n\r");
    }

    // Show a list of characters in the room
    if (others.size() > 0) {
      buffer.append("{cThe following characters are present:{x\n\r");
      for (Player c : others) {
        buffer.append("    " + c.getName() + "\n\r");
      }
    }
    else {
      buffer.append("{cYou are the only one here.{x\n\r");
    }

    return buffer.toString();
  }

  /**
   * Constructs inspect data for admins to see the details of a room.
   * @return The message to send to the admin that describes the room's details.
   */
  public String inspect(solace.game.Character ch) {
    return "TODO inspect";
  }

  /**
   * Finds the description for a feature with the given name.
   * @param name Name of the feature to find.
   * @return The description of the feature, or null if no such feature was
   *   found.
   */
  public String describeFeature(String name) {
    for (String key : features.keySet()) {
      String[] names = key.split("\\s+");
      for (String n : names) {
        if (n.startsWith(name)) {
          return Strings.toFixedWidth(features.get(key));
        }
      }
    }
    return null;
  }

  /**
   * Adds a feature to the room that can be examined by the player.
   * @param names Names associated with the feature.
   * @param value Description of the feature.
   */
  public void addFeature(String names, String value) {
    features.put(names, value);
  }

  /**
   * Adds an item instance to the room. Items are actually loaded into the room
   * after the world's game data has been fully loaded.
   * @param id Id of the item to instantiate upon load.
   */
  public void addItemInstance(String id) {
    itemInstances.add(id);
  }

  /**
   * Adds an item to the game room.
   * @param item Item to be added.
   */
  public synchronized void addItem(Item item) {
    items.add(item);
  }

  /**
   * Removes an item from the game room.
   * @param item Item to be removed.
   */
  public synchronized void removeItem(Item item) {
    items.remove(item);
  }

  /**
   * @return an immutable list of all items in the room.
   */
  public List<Item> getItems() {
    return Collections.unmodifiableList(items);
  }

  /**
   * Finds an item with the given name prefix in the room.
   * @param prefix Prefix of the name of the item in question.
   * @return An item who's name has the given prefix, null if no
   *  such item was found.
   */
  public Item findItem(String prefix) {
    synchronized(items) {
      for (Item item : items) {
        if (item.hasName(prefix))
          return item;
      }
    }
    return null;
  }

  /**
   * Determine if the room has a shop.
   * @return `true` if the room has a shop, `false` otherwise.
   */
  public boolean hasShop() {
    return shop != null;
  }

  /**
   * @return The shop associated with the room.
   */
  public Shop getShop() {
    return shop;
  }

  /**
   * Sets the shop for the room.
   * @param s The shop to set.
   */
  public void setShop(Shop s) {
    shop = s;
  }
}
