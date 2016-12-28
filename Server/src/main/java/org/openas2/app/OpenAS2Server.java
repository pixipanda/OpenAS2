package org.openas2.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.OpenAS2Exception;
import org.openas2.XMLSession;
import org.openas2.cmd.CommandManager;
import org.openas2.cmd.CommandRegistry;
import org.openas2.cmd.processor.BaseCommandProcessor;


/** 
 * original author unknown
 * 
 * in this release added ability to have multiple command processors
 * @author joseph mcverry
 *
 */
public class OpenAS2Server {
	protected BufferedWriter sysOut;
	BaseCommandProcessor cmd = null;
	XMLSession session = null;

	
	public static void main(String[] args) {
		OpenAS2Server server = new OpenAS2Server();
		server.start(args);
	}

	public void start(String[] args) {
		int exitStatus = 0;

		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	shutdown();
                System.out.println("Shutdown due to process interruption!");
            }
        });
		try {
			Log logger = LogFactory.getLog(OpenAS2Server.class.getSimpleName());
			
			write("Retrieving config file..." + System.getProperty("line.separator"));
			// Check for passed in as parameter first then look for system property
			String configFile = (args.length > 0)?args[0]:System.getProperty("openas2.config.file");
			if (configFile == null || configFile.length() < 1) {
				// Try the default location assuming the app was started in the bin folder
				configFile = System.getProperty("user.dir") + "/../config/config.xml";
			}
			File cfg = new File(configFile);
			if (!cfg.exists()) {
				write("No config file found: " + configFile + System.getProperty("line.separator"));
				write("Pass as the first paramter on the command line or set the system property \"openas2.config.file\" to identify the configuration file to start OpenAS2" + System.getProperty("line.separator"));
				throw new Exception("Missing configuration file");
			}
			session = new XMLSession(configFile);

			write("Starting Server..." + System.getProperty("line.separator"));

			// create the OpenAS2 Session object
			// this is used by all other objects to access global configs and functionality
			write("Loading configuration..." + System.getProperty("line.separator"));

			write(session.getAppTitle() + ": Session instantiated." + System.getProperty("line.separator"));
			// create a command processor

			// get a registry of Command objects, and add Commands for the Session
			write("Registering Session to Command Processor..." + System.getProperty("line.separator"));

			CommandRegistry reg = session.getCommandRegistry();

			// start the active processor modules
			write("Starting Active Modules..." + System.getProperty("line.separator"));
			session.getProcessor().startActiveModules();

			// enter the command processing loop
			write(session.getAppTitle() + " Started" + System.getProperty("line.separator"));

			
			logger.info("- OpenAS2 Started - V" + session.getAppVersion());
			
			CommandManager cmdMgr = session.getCommandManager();
			List<BaseCommandProcessor> processors = cmdMgr.getProcessors();
			for (int i = 0; i < processors.size(); i++) {
				write("Loading Command Processor..." + processors.toString()
						+ System.getProperty("line.separator"));
				cmd = (BaseCommandProcessor) processors.get(i);
				cmd.init();
				cmd.addCommands(reg);
				cmd.start();
			}
			breakOut : while (true) {
				for (int i = 0; i < processors.size(); i++) {
					cmd = (BaseCommandProcessor) processors.get(i);
					if (cmd.isTerminated())
						break breakOut;
					Thread.sleep(100); 
				}
			}
			logger.info("- OpenAS2 Stopped -");
		} catch (Exception e) {
			exitStatus = -1;
			e.printStackTrace();
		} catch (Error err) {
			exitStatus = -1;
			err.printStackTrace();
		} finally {
			shutdown();
			System.exit(exitStatus);
		}
	}

	public void shutdown()
	{
		if (session != null) {
			try {
				session.getProcessor().stopActiveModules();
			} catch (OpenAS2Exception same) {
				same.terminate();
			}
		}

		if (cmd != null) {
			try {
				cmd.deInit();
			} catch (OpenAS2Exception cdie) {
				cdie.terminate();
			}
		}

		write("OpenAS2 has shut down\r\n");
		

	}
	
	public void write(String msg) {
		if (sysOut == null) {
			sysOut = new BufferedWriter(new OutputStreamWriter(System.out));
		}

		try {
			sysOut.write(msg);
			sysOut.flush();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}
}