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
    // :NOTE: DEFAULT
    private final static String ZERO40_HASH = bytesToString(new byte[20]);
    private final static int BUFFER_SIZE = 1024;

    private static String bytesToString (byte[] bytes) {
        if (bytes != null) {
            StringBuilder sb = new StringBuilder();
            for (byte el : bytes) {
                sb.append(String.format("%02x", el));
            }
            return sb.toString();
        }
        return ZERO40_HASH;
    }

    private static String getHash(Path path, String fileName) throws NoSuchAlgorithmException, IOException, SecurityException {
        byte[] hash;
        try (InputStream file = Files.newInputStream(path)) {
            int value;
            byte[] buffer = new byte[BUFFER_SIZE];
            MessageDigest sha;
            try {
                sha = MessageDigest.getInstance("SHA-1");
                while ((value = file.read(buffer)) >= 0) {
                    sha.update(buffer, 0, value);
                }
                hash = sha.digest();
            } catch (IOException e) {
                throw new IOException("Error while reading the file " + fileName + " : " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                throw new NoSuchAlgorithmException("No SHA-1 algorithm" + e.getMessage());
            }
        } catch (IOException e) {
            throw new IOException("Cannot read the file " + fileName);
            // :NOTE: security exception
        } catch (SecurityException e) {
            throw new SecurityException("Forbidden to read the file " + fileName + " : " + e.getMessage());
        }

        return bytesToString(hash);
    }

    public static void main (String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Invalid arguments. Expected 2 not-null arguments: <input file> <output file>.");
            return;
        }

        Path input, output;
        try {
            input = Paths.get(args[0]);
            output = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Invalid path in argument files: " + e.getMessage());
            return;
        }

        try {
            // :NOTE: getParent
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
        } catch (IOException e) {
            System.err.println("Cannot create output file: " + e.getMessage());
            return;
        } catch (SecurityException e) {
            System.err.println("Forbidden to create output file: " + e.getMessage());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Path path = Paths.get(line);
                    writer.write(String.format("%s %s", getHash(path, line), line));
                } catch (IOException e) {
                    System.err.println("Error while writing to file: " + e.getMessage());
                    writer.write(String.format("%s %s", ZERO40_HASH, line));
                } catch (InvalidPathException ignored) {
                    writer.write(String.format("%s %s", ZERO40_HASH, line));
                } catch (SecurityException | NoSuchAlgorithmException e) {
                    System.err.println(e.getMessage());
                    writer.write(String.format("%s %s", ZERO40_HASH, line));
                }
                writer.newLine();
            }
            // :NOTE: do not merge exception
        } catch (IOException e) {
            System.err.println("Cannot open Input/Output files error: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Forbidden to read/write files: " + e.getMessage());
        }
    }
}

