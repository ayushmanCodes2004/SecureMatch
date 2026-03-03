package com.securematch;

import com.securematch.cli.SecureMatchCLI;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {

        if (args.length > 0) {
            Banner.print();
        }

        Config.load();

        CommandLine cmd = new CommandLine(new SecureMatchCLI());

        int exitCode = cmd.execute(args);

        System.exit(exitCode);
    }
}
