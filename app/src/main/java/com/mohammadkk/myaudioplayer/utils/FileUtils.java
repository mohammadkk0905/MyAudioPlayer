package com.mohammadkk.myaudioplayer.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mohammadkk.myaudioplayer.models.FileItem;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class FileUtils {
    private static final String[] FALLBACKS = {
            "cover.jpg", "album.jpg", "folder.jpg", "cover.png", "album.png", "folder.png"
    };

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
    @NonNull
    public static List<FileItem> listFilesDeep(@NonNull Context context, @NonNull Uri treeUri) {
        List<FileItem> files = new LinkedList<>();
        handleListFilesDeep(context, files, treeUri);
        return files;
    }
    private static void handleListFilesDeep(@NonNull Context context, @NonNull Collection<FileItem> files, @NonNull Uri treeUri) {
        List<FileItem> found = listFiles(context, treeUri);
        for (FileItem file : found) {
            if (file.isDirectory()) {
                handleListFilesDeep(context, files, file.getContentUri());
            } else if (isAudioFile(file.getFilename())) {
                files.add(file);
            }
        }
    }
    private static List<FileItem> listFiles(Context context, Uri treeUri) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,
                DocumentsContract.getDocumentId(treeUri));
        final String[] projection = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };
        final List<FileItem> results = new ArrayList<>();
        Cursor c = null;
        try {
            c = resolver.query(childrenUri, projection, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    final String documentId = c.getString(0);
                    final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                    final String name = c.getString(1);
                    final String mimetype = c.getString(2);
                    final boolean isDirectory = mimetype.equals(DocumentsContract.Document.MIME_TYPE_DIR);
                    final long modified =  c.getLong(3);
                    results.add(new FileItem(name, isDirectory, modified, docUri));
                }
            }
        } catch (Exception e) {
            Log.w("FileUtils", "Failed query: " + e);
        } finally {
            closeQuietly(c);
        }
        return results;
    }
    private static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
    public static boolean isAudioFile(@Nullable String path) {
        if (path == null) return false;
        int dotPos = path.lastIndexOf('.');
        if (dotPos == -1) return false;
        String[] extensions = new String[] {"mp3", "wav", "wma", "ogg", "m4a", "opus", "flac", "aac", "m4b"};
        String fileExtension = path.substring(dotPos + 1).toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (fileExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }
    public static InputStream fallback(String path) {
        final File parent = new File(path).getParentFile();
        if (parent == null) return null;
        for (String fallback : FALLBACKS) {
            File cover = new File(parent, fallback);
            if (cover.exists()) {
                try {
                    return new FileInputStream(cover);
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
        }
        return null;
    }
    public static boolean deleteSingle(@NonNull Context context, Uri treeUri) {
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), treeUri);
        } catch (Exception e) {
            return false;
        }
    }
}