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

    public static String GetLastFolder(String filePath){
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        return parent != null ? parent.getFileName().toString() : "";
    }
}
