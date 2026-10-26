package campusplan;

import campusplan.application.CampusPlanService;
import campusplan.cli.ConsoleApp;
import campusplan.persistence.JsonStorage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        // keep the exit code separate so startup errors reach the terminal properly
        int result = run(args);
        if (result != 0) {
            System.exit(result);
        }
    }

    static int run(String[] args) {
        // use these files unless a different path is passed in
        Path data = Path.of("data", "campusplan.json");
        Path seed = Path.of("sample-data", "seed.json");
        try {
            // file options take the next argument as their path
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--data" -> data = Path.of(argument(args, ++i, "--data"));
                    case "--seed" -> seed = Path.of(argument(args, ++i, "--seed"));
                    case "--help" -> {
                        // help should work without opening any saved data
                        System.out.println("Usage: java -jar target/campusplan.jar [--data FILE] [--seed FILE]");
                        System.out.println("Sample data loads only when you select Load sample data in the menu.");
                        return 0;
                    }
                    // catch misspelled options instead of starting with the wrong settings
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
        } catch (IllegalArgumentException error) {
            // report the option problem before trying to start the app
            System.err.println("Invalid command: " + error.getMessage());
            return 1;
        }

        CampusPlanService service;
        try {
            // stop here if the save is bad, instead of starting empty and risking the file
            service = new CampusPlanService(new JsonStorage(data));
        } catch (IOException error) {
            // leave the existing file alone so the user can check or recover it
            System.err.println("Cannot load data: " + error.getMessage());
            System.err.println("The existing data file was not overwritten.");
            return 1;
        }

        // use utf-8 for names and show the full paths so the chosen files are clear
        var output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        output.println("Data file: " + data.toAbsolutePath().normalize());
        output.println("Sample file: " + seed.toAbsolutePath().normalize());
        try {
            // pass the sample storage in, but let the menu decide when to load it
            new ConsoleApp(service, new JsonStorage(seed),
                    new InputStreamReader(System.in, StandardCharsets.UTF_8), output).run();
            return 0;
        } catch (IOException error) {
            // a failure that the menu cannot recover from should return an error code
            System.err.println("CampusPlan could not finish: " + error.getMessage());
            return 1;
        }
    }

    private static String argument(String[] args, int index, String option) {
        // another option is not a file path, even if there is still an argument left
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " needs a file path.");
        }
        return args[index];
    }
}
