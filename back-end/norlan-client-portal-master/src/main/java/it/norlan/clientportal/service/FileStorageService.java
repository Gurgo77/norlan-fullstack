package it.norlan.clientportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Livello di servizio architetturale per la gestione dell'I/O (Input/Output) su disco.
 * Astrae le operazioni di salvataggio e lettura fisica dei file, garantendo la sicurezza
 * contro le collisioni (tramite UUID) e prevenendo vulnerabilità di Path Traversal.
 */

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Impossibile creare la cartella di upload.", ex);
        }
    }

    // Sanifica il nome originale, genera un UUID univoco per l'idempotenza e persiste lo stream binario sfruttando le API moderne java.nio
    public String storeFile(MultipartFile file, String subFolder) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            Path targetLocation = this.fileStorageLocation.resolve(subFolder).resolve(fileName);
            Files.createDirectories(targetLocation.getParent());

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return subFolder + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Errore nel salvataggio del file " + originalFileName, ex);
        }
    }

    // Risolve il percorso fisico sul filesystem del server e lo incapsula in una UrlResource pronta per essere servita via HTTP
    public Resource loadFileAsResource(String relativePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File non trovato: " + relativePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Percorso file malformato.", ex);
        }
    }
}
