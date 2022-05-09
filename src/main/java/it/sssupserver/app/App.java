package it.sssupserver.app;

import it.sssupserver.app.handlers.*;
import it.sssupserver.app.controllers.ControllerFactory;
import it.sssupserver.app.executors.*;

/**
 * Hello world!
 */
public class App
{
    static void Help() {
        System.err.println("Usage:");
        ExecutorFactory.Help("\t");
        System.out.println();
        RequestHandlerFactory.Help("\t");
        System.out.println();
        System.exit(1);
    }

    public static void main( String[] args ) throws Exception
    {
        if (args.length > 0 && args[0].equals("--help")) {
            Help();
        }
        var executor = ExecutorFactory.getExecutor(args);
        var handler = RequestHandlerFactory.getRequestHandler(executor, args);
        var controller = ControllerFactory.getController();
        executor.start();
        handler.start();
        //var cmd = handler.receiveCommand();
        //System.out.println("Cmd: " + cmd);
        controller.run(executor, handler);
        handler.stop();
        //handler.receiveAndExecuteCommand();
        executor.stop();
    }
}
