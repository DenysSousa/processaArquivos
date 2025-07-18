package br.inf.solus.processaArquivos.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static String GetName(String filepath){
        return GetName(filepath, true);
    }

    public static String GetName(String filePath, Boolean includeExtension){
        String LReturn = "";
        try{
            LReturn  = Paths.get(filePath).getFileName().toString();
        }catch(Exception e) {
            LReturn = filePath;
        }

        if ((!includeExtension) && (LReturn != null) && (!LReturn.equals(""))) {
            LReturn = LReturn.contains(".") ? LReturn.substring(0, LReturn.lastIndexOf('.')) : LReturn;
        }

        return LReturn;
    }

    public static String GetLastFolder(String filePath, Integer position){
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
}
