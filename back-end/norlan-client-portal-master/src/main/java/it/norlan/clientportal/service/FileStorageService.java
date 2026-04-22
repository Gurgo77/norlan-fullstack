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

    /**
     * Salva un file su disco con un nome univoco per evitare sovrascritture.
     */
    public String storeFile(MultipartFile file, String subFolder) {
        // Pulizia del nome file
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Generiamo un nome univoco (UUID) per evitare conflitti tra utenti diversi
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            Path targetLocation = this.fileStorageLocation.resolve(subFolder).resolve(fileName);
            Files.createDirectories(targetLocation.getParent()); // Crea la sottocartella se non esiste

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return subFolder + "/" + fileName; // Restituiamo il percorso relativo da salvare nel DB
        } catch (IOException ex) {
            throw new RuntimeException("Errore nel salvataggio del file " + originalFileName, ex);
        }
    }

    /**
     * Carica un file come risorsa per il download.
     */
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
