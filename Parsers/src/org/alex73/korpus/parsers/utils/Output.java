package org.alex73.korpus.parsers.utils;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.alex73.korpus.base.Ctf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Output implements Closeable {
    private ZipOutputStream os;
    private Gson gson;

    public Output(Path out) throws IOException {
        os = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(out), 1024 * 1024));
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void write(String path, Ctf text) throws IOException {
        os.putNextEntry(new ZipEntry(path));
        os.write(gson.toJson(text).getBytes(StandardCharsets.UTF_8));
        os.closeEntry();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }
}
