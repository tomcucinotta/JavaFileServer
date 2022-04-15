package it.sssupserver.app.executors.samples;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

import it.sssupserver.app.base.Path;
import it.sssupserver.app.commands.schedulables.SchedulableCommand;
import it.sssupserver.app.commands.schedulables.SchedulableCreateOrReplaceCommand;
import it.sssupserver.app.commands.schedulables.SchedulableExistsCommand;
import it.sssupserver.app.commands.schedulables.SchedulableReadCommand;
import it.sssupserver.app.commands.schedulables.SchedulableTruncateCommand;
import it.sssupserver.app.executors.Executor;

public class FlatTmpExecutor implements Executor {
    private String prefix = "JAVA-FILE-SERVER-";
    private java.nio.file.Path baseDir;

    static int MAX_CHUNK_SIZE = 2 << 16;

    public FlatTmpExecutor() throws Exception
    {
        this.baseDir = Files.createTempDirectory(prefix);
        System.out.println("Dase directory: " + this.baseDir);
        this.baseDir.toFile().deleteOnExit();
    }

    private boolean started;
    @Override
    public void start() throws Exception {
        if (this.started) {
            throw new Exception("Executor already started");
        }
        this.started = true;
    }

    @Override
    public void stop() throws Exception {
        if (!this.started) {
            throw new Exception("Executor was not previously started started");
        }
        this.started = false;
    }

    private void ensureFlatPath(Path path) throws Exception {
        if (!path.isFlat()) {
            throw new Exception("Path is not flat!");
        }
    }

    private void handleRead(SchedulableReadCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fin = FileChannel.open(filePath, StandardOpenOption.READ)) {
            var fileSz = fin.size();
            var toRead = Math.min((int)fileSz, command.getLen() != 0 ? command.getLen() : MAX_CHUNK_SIZE);
            var bytes = new byte[toRead];
            var buffer = ByteBuffer.wrap(bytes);
            fin.read(buffer, command.getBegin());
            command.reply(bytes);
        } catch (NoSuchFileException e) {
            command.notFound();
        } catch (Exception e) {
            throw e;
        }
    }

    private void handleCreateOrReplace(SchedulableCreateOrReplaceCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fout = FileChannel.open(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            var bytes = command.getData();
            var buffer = ByteBuffer.wrap(bytes);
            fout.write(buffer);
            command.reply(true);
        } catch (Exception e) {
            command.reply(false);
            throw e;
        }
    }

    private void handleTruncate(SchedulableTruncateCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        try (var fin = FileChannel.open(filePath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            command.reply(true);
        } catch (NoSuchFileException e) {
            command.reply(false);
        } catch (Exception e) {
            throw e;
        }
    }

    private void handleExists(SchedulableExistsCommand command) throws Exception {
        ensureFlatPath(command.getPath());
        var filename = command.getPath().toString();
        var filePath = this.baseDir.resolve(filename);
        // For security reasons do no follow symlinks
        var exists = Files.exists(filePath, LinkOption.NOFOLLOW_LINKS);
        command.reply(exists);
    }

    @Override
    public void execute(SchedulableCommand command) throws Exception {
        if (command instanceof SchedulableReadCommand) {
            handleRead((SchedulableReadCommand)command);
        } else if (command instanceof SchedulableCreateOrReplaceCommand) {
            handleCreateOrReplace((SchedulableCreateOrReplaceCommand)command);
        } else if (command instanceof SchedulableTruncateCommand) {
            handleTruncate((SchedulableTruncateCommand)command);
        } else if (command instanceof SchedulableExistsCommand) {
            handleExists((SchedulableExistsCommand)command);
        } else {
            throw new Exception("Unknown command");
        }
    }

    @Override
    public void scheduleExecution(SchedulableCommand command) throws Exception {
        execute(command);
    }
}
