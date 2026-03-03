package com.securematch;

public class Banner {

    private static final String VERSION = "1.0.0";
    private static final String AUTHOR = "Ayushman Mohapatra (Solo project)";

    public static void print() {
        System.out.println();
        System.out.println("  ____                           __  __       _       _     ");
        System.out.println(" / ___|  ___  ___ _   _ _ __ ___|  \\/  | __ _| |_ ___| |__  ");
        System.out.println(" \\___ \\ / _ \\/ __| | | | '__/ _ \\ |\\/| |/ _` | __/ __| '_ \\ ");
        System.out.println("  ___) |  __/ (__| |_| | | |  __/ |  | | (_| | || (__| | | |");
        System.out.println(" |____/ \\___|\\___|\\__,_|_|  \\___|_|  |_|\\__,_|\\__\\___|_| |_|");
        System.out.println();
        System.out.println("  :: Fuzzy Searchable Encryption ::        (v" + VERSION + ")");
        System.out.println("  :: " + AUTHOR + " ::");
        System.out.println();
    }

    public static void printSection(String title) {
        System.out.println();
        System.out.println("  " + title);
        System.out.println("  " + "=".repeat(title.length()));
        System.out.println();
    }

    public static void printSuccess(String message) {
        System.out.println("  [+] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("  [i] " + message);
    }

    public static void printWarning(String message) {
        System.out.println("  [!] " + message);
    }

    public static void printError(String message) {
        System.out.println("  [X] " + message);
    }

    public static void printProperty(String key, String value) {
        System.out.printf("  %-25s : %s%n", key, value);
    }

    public static void printSeparator() {
        System.out.println("  " + "-".repeat(60));
    }
}
