package com.voting.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    public String save(MultipartFile file, String subDir) {
        try {
            String dir = "uploads/" + subDir + "/";
            Files.createDirectories(Paths.get(dir));
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path dest = Paths.get(dir + filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            // return web-accessible relative path
            return dir + filename;
        } catch (IOException e) {
            throw new RuntimeException("Could not save file: " + e.getMessage(), e);
        }
    }

    public void delete(String path) {
        if (path == null) return;
        try { Files.deleteIfExists(Paths.get(path)); } catch (IOException ignored) {}
    }

    /** Convert stored path to a URL the browser can fetch */
    public String toUrl(String path) {
        if (path == null) return null;
        // path like "uploads/faces/xxx.jpg"  → "/uploads/faces/xxx.jpg"
        return "/" + path.replace("\\", "/");
    }
}
