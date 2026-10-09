package com.example.capstone.ingestion;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

@Component
public class TikaDocumentReader {

    private final Tika tika = new Tika();

    public String read(Path sourcePath) {
        try {
            return tika.parseToString(sourcePath.toFile());
        } catch (IOException | TikaException e) {
            throw new IllegalStateException("Unable to read document: " + sourcePath, e);
        }
    }
}
