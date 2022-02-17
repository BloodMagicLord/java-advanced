package info.kgeorgiy.ja.Maksonov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Walk {
    private final static String ZERO40_HASH = "0000000000000000000000000000000000000000";
    private final static int BUFFER_SIZE = 1024;

    private static String getHash(String fileName) {
        String result = ZERO40_HASH;
        byte[] hash = null;

        try (InputStream file = Files.newInputStream(Paths.get(fileName))) {
            int value;
            byte[] buffer = new byte[BUFFER_SIZE];
            MessageDigest sha;
            try {
                sha = MessageDigest.getInstance("SHA-1");
                while ((value = file.read(buffer)) >= 0) {
                    sha.update(buffer, 0, value);
                }
                hash = sha.digest();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("No such algorithm Exception: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Input/Output files error: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Files security error: " + e.getMessage());
        }

        if (hash != null) {
            StringBuilder sb = new StringBuilder();
            for (byte el : hash) {
                sb.append(String.format("%02x", el));
            }
            result = sb.toString();
        }

        return result;
    }

    public static void main (String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Invalid arguments. Expected 2 not-null arguments.");
            return;
        }

        try {
            Path input = Paths.get(args[0]);
            Path output = Paths.get(args[1]);

            if (output.getParent() != null) {
                try {
                    Files.createDirectories(output.getParent());
                } catch (IOException | SecurityException e) {
                    System.err.println("Cannot create output file: " + e.getMessage());
                    return;
                }
            }

            try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
                 BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        Path path = Paths.get(line);
                        writer.write(String.format("%s %s", getHash(path.toString()), line));
                    } catch (InvalidPathException e) {
                        writer.write(String.format("%s %s", ZERO40_HASH, line));
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("Input/Output files error: " + e.getMessage());
            } catch (SecurityException e) {
                System.err.println("Files security error: " + e.getMessage());
            }
        } catch (InvalidPathException e) {
            System.err.println("Invalid argument files: " + e.getMessage());
        }
    }
}
