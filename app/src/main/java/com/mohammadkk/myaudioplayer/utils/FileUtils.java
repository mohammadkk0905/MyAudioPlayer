package com.mohammadkk.myaudioplayer.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class FileUtils {
    public static String safeGetCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
    @NonNull
    public static List<File> listFilesDeep(@NonNull File directory, @Nullable FileFilter fileFilter) {
        List<File> files = new LinkedList<>();
        handleListFilesDeep(files, directory, fileFilter);
        return files;
    }
    private static void handleListFilesDeep(@NonNull Collection<File> files, @NonNull File dir, @Nullable FileFilter filter) {
        File[] found = dir.listFiles(filter);
        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    handleListFilesDeep(files, file, filter);
                } else {
                    files.add(file);
                }
            }
        }
    }
    public static boolean isAudioFile(@Nullable File file) {
        if (file == null) return false;
        String filePath = file.getPath();
        int dotPos = filePath.lastIndexOf('.');
        if (dotPos == -1) return false;
        String[] extensions = new String[] {"mp3", "wav", "wma", "ogg", "m4a", "opus", "flac", "aac", "m4b"};
        String fileExtension = filePath.substring(dotPos + 1).toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (fileExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
