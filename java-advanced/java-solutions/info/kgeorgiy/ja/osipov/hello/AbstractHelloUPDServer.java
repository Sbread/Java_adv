package info.kgeorgiy.ja.osipov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public abstract class AbstractHelloUPDServer implements HelloServer {

    protected ExecutorService service;

    protected void runServer(final String[] args) {
        if (Objects.isNull(args) || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected 2 non-null arguments");
            return;
        }
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            start(port, threads);
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments. " + e.getMessage());
        }
    }
}
