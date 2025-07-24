package br.inf.solus.processaArquivos.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class FileUtils {

    private static final String BASE_FOLDER = System.getenv("ARQUIVOS_DIR");

    public static void AllPermission(Path file) {
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(file, perms);
        } catch (Exception e) {
            System.out.printf("Não foi possível dar permissão para o arquivo! %s%n", e.getMessage());
        }
    }

    public static String GetName(String filepath) {
        return GetName(filepath, true);
    }

    public static String GetName(String filePath, Boolean includeExtension) {
        String LReturn = "";
        try {
            LReturn = Paths.get(filePath).getFileName().toString();
        } catch (Exception e) {
            LReturn = filePath;
        }

        if ((!includeExtension) && (LReturn != null) && (!LReturn.equals(""))) {
            LReturn = LReturn.contains(".") ? LReturn.substring(0, LReturn.lastIndexOf('.')) : LReturn;
        }

        return LReturn;
    }

    public static String GetLastFolder(String filePath, Integer position) {
        Path path = Paths.get(filePath);

        // Se o caminho for um arquivo, começar pelo pai
        if (!path.toFile().isDirectory()) {
            path = path.getParent();
        }

        // Subir a quantidade de pastas conforme posição
        for (int i = 0; i < position; i++) {
            if (path != null && path.getParent() != null) {
                path = path.getParent();
            } else {
                // Chegou na raiz ou não tem mais pais
                break;
            }
        }

        if (path == null) {
            return "";
        }

        return path.getFileName() != null ? path.getFileName().toString() : path.toString();
    }

    public static Path AvailabePath(String serviceFolder) {
        Path availabePath = Paths.get(BASE_FOLDER, "disponiveis/" + serviceFolder);

        if (!Files.exists(availabePath) || !Files.isDirectory(availabePath)) {
            System.err.println("✖ Erro: Subpasta '" + availabePath + "' não existe ou não é um diretório.");
            return null;
        }

        return availabePath;
    }

    public static Path MoveToBase(String subFolderOfBase, Path originFile) {
        if (BASE_FOLDER == null || BASE_FOLDER.isBlank()) {
            throw new IllegalStateException("Variável de ambiente 'ARQUIVOS_DIR' não está definida.");
        }

        String fileName = originFile.getFileName().toString();
        String rawFileName = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        if (rawFileName.contains("--")) {
            rawFileName = rawFileName.substring(0, rawFileName.indexOf("--"));
        }

        Path targetFolder = Paths.get(BASE_FOLDER, subFolderOfBase, rawFileName);

        try {
            Files.createDirectories(targetFolder);
            AllPermission(targetFolder);

            Path finalPath = targetFolder.resolve(fileName);
            Files.move(originFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
            AllPermission(finalPath);

            System.out.printf("✔ Arquivo '%s' movido para '%s'%n", fileName, finalPath);
            return finalPath;
        } catch (Exception e) {
            System.err.printf("✖ Erro ao mover o arquivo %s para o destino %s! - %s%n", originFile, targetFolder,
                    e.getMessage());
            return null;
        }
    }
}
