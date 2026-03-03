package com.securematch.cli;

import com.securematch.commands.AddCommand;
import com.securematch.commands.InitCommand;
import com.securematch.commands.MultiAddCommand;
import com.securematch.commands.MultiSearchCommand;
import com.securematch.commands.SearchCommand;
import com.securematch.commands.StatsCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "securematch",
    description = "SecureMatch - Fuzzy Searchable Encryption System",
    subcommands = {
        InitCommand.class,
        AddCommand.class,
        SearchCommand.class,
        StatsCommand.class,
        MultiAddCommand.class,
        MultiSearchCommand.class,
        CommandLine.HelpCommand.class
    },
    mixinStandardHelpOptions = true,
    version = "SecureMatch 1.0.0"
)
public class SecureMatchCLI implements Runnable {

    @Override
    public void run() {
        System.out.println();
        System.out.println("SecureMatch - Fuzzy Searchable Encryption");
        System.out.println("   Team: Ayushman Mohapatra (Solo project)");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  init         Initialize secure database");
        System.out.println("  add          Encrypt and store a record");
        System.out.println("  search       Search encrypted database");
        System.out.println("  stats        Show database statistics");
        System.out.println("  multi-add    Add multi-field patient record");
        System.out.println("  multi-search Search multi-field records");
        System.out.println();
        System.out.println("Run: securematch <command> --help for details");
        System.out.println();
    }
}