package br.com.barberadmin.service.storage;

import br.com.barberadmin.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public LocalStorageService(@Value("${storage.location:uploads}") String location) {
        this.rootLocation = Paths.get(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            log.info("Diretório de armazenamento de uploads inicializado em: {}", this.rootLocation);
        } catch (IOException e) {
            log.error("Não foi possível inicializar o diretório de armazenamento {}", this.rootLocation, e);
            throw new BusinessException("Falha ao inicializar diretório de armazenamento.");
        }
    }

    @Override
    public String store(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("O arquivo de imagem não pode estar vazio.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Formato de imagem inválido. Formatos suportados: JPG, JPEG, PNG, WEBP.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        } else {
            // Se não veio extensão no nome, extrai do content type
            if ("image/png".equalsIgnoreCase(contentType)) extension = "png";
            else if ("image/webp".equalsIgnoreCase(contentType)) extension = "webp";
            else extension = "jpg";
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Extensão de imagem inválida (permitidas: .jpg, .jpeg, .png, .webp).");
        }

        String cleanSubfolder = subfolder != null ? subfolder.replaceAll("[^a-zA-Z0-9_-]", "") : "misc";
        Path targetFolder = this.rootLocation.resolve(cleanSubfolder).normalize();

        try {
            Files.createDirectories(targetFolder);

            // Gera nome aleatório único para evitar conflito de cache e colisões
            String uniqueFileName = UUID.randomUUID() + "." + extension;
            Path destinationFile = targetFolder.resolve(uniqueFileName).normalize();

            // Proteção contra Path Traversal
            if (!destinationFile.getParent().equals(targetFolder)) {
                throw new BusinessException("Tentativa de gravação de arquivo fora do diretório autorizado.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Foto armazenada com sucesso: {}", destinationFile);

            // Retorna URL relativa padronizada servida pela API
            return "/api/uploads/" + cleanSubfolder + "/" + uniqueFileName;
        } catch (IOException e) {
            log.error("Erro ao salvar arquivo em disco: {}", originalFilename, e);
            throw new BusinessException("Erro ao processar e salvar a imagem no servidor.");
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            // Extrai o caminho relativo após /uploads/ ou /api/uploads/
            String pathPart = fileUrl;
            if (pathPart.contains("/uploads/")) {
                pathPart = pathPart.substring(pathPart.indexOf("/uploads/") + "/uploads/".length());
            }

            Path targetFile = this.rootLocation.resolve(pathPart).normalize();

            // Proteção contra Path Traversal
            if (!targetFile.startsWith(this.rootLocation)) {
                log.warn("Tentativa de exclusão de arquivo fora do diretório de uploads: {}", fileUrl);
                return;
            }

            if (Files.exists(targetFile)) {
                Files.delete(targetFile);
                log.info("Arquivo antigo removido com sucesso: {}", targetFile);
            }
        } catch (Exception e) {
            log.warn("Não foi possível excluir o arquivo antigo {}: {}", fileUrl, e.getMessage());
        }
    }
}
