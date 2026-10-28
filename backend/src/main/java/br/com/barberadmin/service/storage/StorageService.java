package br.com.barberadmin.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Armazena um arquivo na subpasta informada (ex: "avatars")
     * e retorna a URL relativa ou caminho para acesso público.
     */
    String store(MultipartFile file, String subfolder);

    /**
     * Remove o arquivo armazenado, liberando espaço em disco.
     */
    void delete(String fileUrl);
}
