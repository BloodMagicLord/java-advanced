package info.kgeorgiy.ja.Maksonov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Walk {
    private final static String DEFAULT_HASH = bytesToString(new byte[20]);
    private final static int BUFFER_SIZE = 1024;
    private static final byte[] buffer = new byte[BUFFER_SIZE];
    private static MessageDigest sha = null;

    private static String bytesToString (byte[] bytes) {
        if (bytes != null) {
            StringBuilder sb = new StringBuilder();
            for (byte el : bytes) {
                sb.append(String.format("%02x", el));
            }
            return sb.toString();
        }
        return DEFAULT_HASH;
    }

    private static String getHash(Path path, String fileName) throws IOException, SecurityException {
        if (sha == null) {
            return DEFAULT_HASH;
        }

        byte[] hash;
        try (InputStream file = Files.newInputStream(path)) {
            int value;
            try {
                while ((value = file.read(buffer)) >= 0) {
                    sha.update(buffer, 0, value);
                }
                hash = sha.digest();
            } catch (IOException e) {
                throw new IOException("Error while reading the file " + fileName + " : " + e.getMessage());
            }
        } catch (IOException e) {
            throw new IOException("Cannot read the file " + fileName);
        }

        return bytesToString(hash);
    }

    private static List<String> recursiveHash(Path path, String pathString) throws IOException {
        final File file = new File(pathString);
        final File[] files = file.listFiles();
        List<String> hashes = new ArrayList<>();
        final BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        if (basicFileAttributes.isDirectory()) {
            if (files != null) {
                for (final File f : files) {
                    hashes.addAll(recursiveHash(f.toPath(), f.getPath()));
                }
            }
        } else if (basicFileAttributes.isRegularFile()) {
            hashes.add(buildString(getHash(path, pathString), pathString));
        }
        return hashes;
    }

    private static String buildString(String hash, String path) {
        return String.format("%s %s", hash, path);
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
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Cannot create output file: " + e.getMessage());
            return;
        }

        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No SHA-1 algorithm");
        }

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            try(BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        Path path = Paths.get(line);
                        for (String hash : recursiveHash(path, line)) {
                            writer.write(String.format("%s", hash));
                            writer.newLine();
                        }
                    } catch (IOException | InvalidPathException e) {
                        if (e instanceof IOException) {
                            System.err.println("Error while writing to file: " + line + " : " + e.getMessage());
                        }
                        writer.write(String.format("%s", buildString(DEFAULT_HASH, line)));
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                System.err.println("Cannot open output file error: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Cannot open input file error: " + e.getMessage());
        }
    }
}
