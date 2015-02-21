package solace.cmd;

import java.util.*;
import java.io.*;

import solace.game.*;
import solace.net.*;
import solace.util.*;


/**
 * Controller for the game's main menu.
 * @author Ryan Sandor Richards (Gaius)
 */
public class MainMenu
    extends AbstractStateController
{
    /**
     * Creates a new main menu controller.
     */
    public MainMenu(Connection c)
    {
        // Initialize the menu
        super(c, "Sorry, that is not an option. Type '{yhelp{x' to see a list.");

        // Set the user's prompt and welcome them
        c.setPrompt("{cChoose an option:{x ");

        // Neato trick, actually use the help command to show the menu on login:
        Command help = new Help();
        help.run(c, new String("help").split(" "));

        // Add all of the commands to the main menu
        addCommand(help);
        addCommand(new Quit());
        addCommand(new Who());
        addCommand(new Chat());
        addCommand(new List());
        addCommand(new Create());
        addCommand(new Play());

        addCommand(new Shutdown());
        addCommand(new Reload());
        addCommand(new Peek());

    }

    /**
     * List command - lists all the characters for the user's account.
     * @author Ryan Sandor Richards.
     */
    class List extends AbstractCommand {
        public List() { super("list"); }
        public void run(Connection c, String []params) {
            Collection<solace.game.Character> chars = c.getAccount().getCharacters();
            if (chars.size() == 0) {
                c.sendln("You have no characters, use the '{ycreate{x' command to create a new one.");
            }
            else {
                c.sendln("{y---- {xYour Characters {y----{x");
                for (solace.game.Character ch : chars) {
                    c.sendln(ch.getName());
                }
                c.sendln("");
            }
        }
    }

    /**
     * Create command - allows players to create new characters.
     * @author Ryan Sandor Richards
     */
    class Create extends AbstractCommand {
        public Create() { super("create"); }
        public void run(Connection c, String []params) {
            c.setStateController( new CreateCharacter(c) );
        }
    }

    /**
     * Play - enter and play the main game with a character.
     */
    class Play extends AbstractCommand {
        public Play() { super("play"); }
        public void run(Connection c, String []params) {
            try {
                Account act = c.getAccount();
                solace.game.Character ch;

                if (params.length < 2) {
                    ch = act.getFirstCharacter();
                }
                else {
                    String name = params[1];
                    if (!act.hasCharacter(name)) {
                        c.sendln("Character '" + name +
                            "' not found, use the '{ylist{x' command to see a list of your characters.");
                        return;
                    }
                    ch = act.getCharacter(name);
                }


                act.setActiveCharacter(ch);
                c.setStateController(new PlayController(c, ch));
            }
            catch (GameException ge) {
                Log.error(ge.getMessage());
                c.sendln("An {rerror{x occured, please try again later.");
            }
        }
    }

    /**
     * Chat command - Logs people into the out of game (OOG) chat room.
     * @author Ryan Sandor Richards
     */
    class Chat extends AbstractCommand
    {
        public Chat() { super("chat"); }
        public void run(Connection c, String []params)
        {
            c.setStateController(new ChatController(c));
        }
    }

    /**
     * Help Command
     * @author Ryan Sandor Richards (Gaius)
     */
    class Help extends AbstractCommand
    {
        public Help() { super("help"); }
        public void run(Connection c, String []params)
        {
            c.sendln(Message.get("MainMenu"));
            if (c.getAccount().isAdmin())
                c.sendln(Message.get("AdminMenu"));
        }
    }

    /**
     * Quit Command
     * @author Ryan Sandor Richards (Gaius)
     */
    class Quit extends AbstractCommand
    {
        public Quit() { super("quit"); }
        public synchronized void run(Connection c, String []params)
        {
            c.sendln("Goodbye!");

            // If the connection has an account, remove it from the accounts list
            if (connection.hasAccount())
                World.removeAccount(connection.getAccount());

            // Remove it from the connections list
          World.removeConnection(connection);

            c.close();
        }
    }

    /**
     * Who command - lists who is online.
     * @author Ryan Sandor Richards
     */
    class Who extends AbstractCommand
    {
        public Who() { super("who"); }

        /*
         * Note: This whole thing will become SIGNIFICANTLY easier
         * when I switch the code base to java 6.
         */
        public void run(Connection c, String []params) {
            Collection connections = World.getConnections();

            c.sendln("{y---- {xPlayers Online{y ----{x");
            synchronized (connections) {
                Iterator iter = connections.iterator();
                while (iter.hasNext()) {
                    Connection oc = (Connection)iter.next();
                    if (oc.hasAccount()) {
                        Account acct = (Account)oc.getAccount();
                        c.sendln(acct.getName());
                    }
                }
            }
            c.sendln("");
        }
    }


    /**
     * Shutdown (Admin Command) - Safely shuts the game server down and exits the program.
     * TODO Make this more robust, for now it will work fine. What I want to do is have
     * it take a couple of arguments (message, possibly a time in minutes for the shutdown
     * to occur, for instance).
     *
     * @author Ryan Sandor Richards
     */
    class Shutdown extends AdminCommand
    {
        public Shutdown() { super("shutdown"); }
        public void run(Connection c, String []params)
        {
            Game.getServer().shutdown();
        }
    }

    /**
     * Reload (Admin Command) - Reloads all "static" game messages (such as help files, etc.).
     * This is useful for when you have to make changes/corrections to game message files and
     * you just need to quickly update them without doing a full reboot/reload of the game.
     *
     * TODO Eventually I would like the 'reload' command to allow admins to reload more than
     * just game messages. They should be able to specify what they want to reload via an
     * argument to the command and it will reload the specific thing they wish (areas, messages,
     * etc.)
     *
     * @author Ryan Sandor Richards
     */
    class Reload extends AdminCommand
    {
        public Reload() { super("reload"); }

        /**
         * Reloads the game's areas.
         * @param c Connection that initiated the reload.
         */
        protected void reloadAreas(Connection c) throws IOException {
            Log.info("Area reload commenced by '" + c.getAccount().getName().toLowerCase() + "'.");
            Collection<Connection> players = Collections.synchronizedCollection(World.getActivePlayers());
            synchronized (players) {
                try {
                    // Freeze all the players (ignore their input)
                    // TODO: Once battle is in place we will need to freeze battle as well.
                    for (Connection con : players) {
                        con.sendln("\n{yGame areas being reloaded, please stand by...{x");
                        con.setIgnoreInput(true);
                    }


                    // Reload all the areas
                    World.loadAreas();
                    Room defaultRoom = World.getDefaultRoom();
                    if (defaultRoom == null) {
                        Log.error("Default room null on area reload.");
                    }

                    // Place players into their original rooms if available, or the default room if not
                    for (Connection con : players) {
                        if (!con.hasAccount())
                            continue;

                        Account act = con.getAccount();
                        if (act == null) {
                            Log.error("Null account encountered on area reload.");
                            continue;
                        }

                        if (!act.hasActiveCharacter()) {
                            Log.error(
                                "Account without active character (" +
                                act.getName().toLowerCase() +
                                ") encountered on area reload."
                            );
                            continue;
                        }

                        solace.game.Character ch = act.getActiveCharacter();
                        if (ch == null) {
                            Log.error("Null character encounted on area reload.");
                            continue;
                        }

                        Room room = ch.getRoom();

                        if (room == null) {
                            Log.error("Null room encountered on area reload.");
                            ch.setRoom(defaultRoom);
                        }
                        else {
                            Area area = room.getArea();
                            if (area == null) {
                                ch.setRoom(defaultRoom);
                            }
                            else {
                                String room_id = room.getId();
                                String area_id = area.getId();
                                Area new_area = World.getArea(area_id);

                                if (new_area == null) {
                                    ch.setRoom(defaultRoom);
                                }
                                else {
                                    Room new_room = new_area.getRoom(room_id);
                                    if (new_room == null)
                                        ch.setRoom(defaultRoom);
                                    else
                                        ch.setRoom(new_room);
                                }
                            }
                        }
                    }

                    c.sendln("Areas reloaded.");
                }
                catch (GameException ge) {
                    c.sendln("Unable to reload areas: default room could not be determined for the game.");
                    Log.error(
                        "Area reload by user '" +
                        c.getAccount().getName().toLowerCase() +
                        "' aborted: no default room could be determined."
                    );
                }
                finally {
                    // Un-freeze the players and force them to take a look around :)
                    for (Connection con : players) {
                        con.sendln("{yAreas reloaded, thanks for your patience!{x\n");
                        con.getStateController().force("look");
                        con.setIgnoreInput(false);
                        con.send(con.getPrompt());
                    }
                }
            }
        }

        /**
         * Reloads the game's static messages.
         * @param c Connection that initiated the reload.
         */
        protected void reloadMessages(Connection c) throws IOException {
            Message.reload();
            c.sendln("Game messages reloaded.");
        }

        public void run(Connection c, String []params) {
            String errorStr = "";

            try {
                if (params.length > 1 && new String("areas").startsWith(params[1])) {
                    errorStr = "areas";
                    reloadAreas(c);
                }
                else if ((params.length > 1 && new String("messages").startsWith(params[1])) || params.length == 1) {
                    errorStr = "game messages";
                    reloadMessages(c);
                }
                else
                    c.sendln("Unable to reload '" + params[1] + "', you can either reload 'areas' or 'messages'.");
            }
            catch (IOException ioe)
            {
                c.sendln("An error occured while trying to reload " + errorStr + ".");
                Log.error("An IO exception occured when reloading " + errorStr + " by user '" +
                        c.getAccount().getName().toLowerCase() + "'");
            }
        }
    }

    /**
     * The peek command allows an administrator to view details about a particular player's
     * connection to the game.
     *
     * Note: This will get more and more detailed as the engine gets fleshed out.
     *
     * @author Ryan Sandor Richards
     */
    class Peek extends AdminCommand
    {
        public Peek() { super("peek"); }

        public void run(Connection c, String []params)
        {
            if (params.length < 2)
            {
                c.sendln("Syntax: peek <player1> <player2> ... | all");
                return;
            }


            if (params[1].toLowerCase().equals("all"))
            {
                Collection connections = Collections.synchronizedCollection(World.getConnections());
                synchronized (connections)
                {
                    Iterator i = connections.iterator();
                    while (i.hasNext())
                        c.sendln( formatInfo((Connection)i.next()) );
                }
            }
            else
            {
                // Generate peek reports for each of the given names
                for (int i = 1; i < params.length; i++)
                {
                    if (!World.isLoggedIn(params[i]))
                        c.sendln("Player '"+params[i]+"' is not currently logged in.");
                    else
                    {
                        Connection target = World.connectionFromName(params[i]);
                        c.sendln(formatInfo(target));
                    }
                }
            }
        }

        /**
         * Helper function to format information for the peek command.
         * @param c Connection to peek into.
         * @return Formatted peek information about the user.
         */
        protected String formatInfo(Connection c)
        {
            String format = "Player '" + c.getAccount().getName() + "', logged on at: " + c.getConnectionTime() +
            " from address: " + c.getInetAddress();
            return format;
        }
    }



} // End of MainMenu
