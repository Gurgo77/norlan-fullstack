package it.norlan.clientportal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void storeFile_SalvataggioCorretto_RitornaPercorsoRelativo() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "Contenuto fittizio del pdf".getBytes()
        );

        String resultPath = fileStorageService.storeFile(mockFile, "sicurezza");

        assertNotNull(resultPath);
        assertTrue(resultPath.startsWith("sicurezza/"));
        assertTrue(resultPath.endsWith("_documento.pdf"));
        assertTrue(Files.exists(tempDir.resolve(resultPath)));
    }

    @Test
    void storeFile_ErroreLetturaStream_LanciaRuntimeException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("documento_corrotto.pdf");
        when(mockFile.getInputStream()).thenThrow(new IOException("Simulazione errore I/O"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileStorageService.storeFile(mockFile, "sicurezza")
        );

        assertTrue(exception.getMessage().contains("Errore nel salvataggio del file"));
    }

    @Test
    void loadFileAsResource_FileEsistente_RitornaResourceValida() throws IOException {
        Path subFolder = tempDir.resolve("attestati");
        Files.createDirectories(subFolder);
        Path targetFile = subFolder.resolve("attestato_123.pdf");
        Files.writeString(targetFile, "Contenuto attestato");

        String relativePath = "attestati/attestato_123.pdf";

        Resource resource = fileStorageService.loadFileAsResource(relativePath);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadFileAsResource_FileNonEsistente_LanciaRuntimeException() {
        String relativePath = "attestati/fantasma.pdf";

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fileStorageService.loadFileAsResource(relativePath)
        );

        assertEquals("File non trovato: " + relativePath, exception.getMessage());
    }
}
