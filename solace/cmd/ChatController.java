package solace.cmd;

import solace.game.*;
import solace.net.Connection;
import java.util.*;

/**
 * Out of game chat controller. The out of game chat allows people to chat on the server without
 * having to actually be logged into the game and playing (useful for discussing strategy, talking
 * with friends, asking questions, and general banter when not actually playing).
 * 
 * @author Ryan Sandor Richards (Gaius)
 */
public class ChatController
	extends AbstractStateController
{
	public ChatController(Connection c)
	{
		super(c);
		String intro = Game.getMessageManager().get("ChatIntro");
		
		// Add the commands
		addCommand(new Quit());
		addCommand(new Help());
		
		// Add the user to the chat list
		Game.getWorld().getOogChat().add(c);
		
		// Set the prompt and send the intro
		c.setPrompt("{cchat>{x ");
		connection.sendln(intro);
	}
	
	/**
	 * Specialized parser for handling basic chatting.
	 */
	public void parse(String s)
	{
		if (s == null || s.length() == 0)
			return;
		
		if (s.charAt(0) == '/')
			super.parse(s);
		else
			broadcast(s);
	}
	
	/**
	 * Sends a message from the connected user to all the people in the oog chat.
	 * @param msg Message to send.
	 */
	void broadcast(String msg)
	{
		String name = connection.getAccount().getName().toLowerCase();
		String format = "{y" + name + ": {x" + msg;
		
		Collection chatters = Collections.synchronizedCollection(Game.getWorld().getOogChat());
		synchronized (chatters)
		{
			Iterator i = chatters.iterator();
			while (i.hasNext())
			{
				Connection c = (Connection)i.next();
				c.sendln(format);
			}
		}
	}
	
	/**
	 * OOG Chat help command.
	 * @author Ryan Sandor Richards (Gaius)
	 */
	class Help extends AbstractCommand
	{
		public Help() { super("/help"); }
		public void run(Connection c, String []args)
		{
			String help = Game.getMessageManager().get("ChatHelp");
			c.sendln(help);
		}
	}
	
	/**
	 * Exits the out of game chat.
	 * @author Ryan Sandor Richards (Gaius)
	 */
	class Quit extends AbstractCommand
	{
		public Quit() { super("/quit"); }
		public void run(Connection c, String []args)
		{
			c.sendln("Later!");
			
			// Remove the user from chat
			Game.getWorld().getOogChat().remove(c);
			
			// Send them back to the main menu
			connection.setStateController( new MainMenu(connection) );
		}
	}
}
