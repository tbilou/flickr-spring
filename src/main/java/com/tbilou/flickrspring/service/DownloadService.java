package com.tbilou.flickrspring.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {

    @Value("${download.path}")
    private String path;

    public void downloadAndSave(String imageUrl, String filename, String foldername, String id) {
        InputStream is = null;
        File targetFile = null;
        try {

            final URL url = new URL(imageUrl);
            is = url.openStream();
            targetFile = new File(MessageFormat.format("{4}/{0}/{1}-{2}.{3}", foldername.replaceAll("[\\/:\"*?<>|]", "_"), filename, id, StringUtils.getFilenameExtension(imageUrl), path));
            targetFile.getParentFile().mkdirs();

            log.debug("Saving to disk: {}", targetFile.getAbsolutePath());
            // If the file already exists don't download again
            if (targetFile.exists()) {
                log.debug("File already exists on disk. NOT Downloading it again");
                return;
            }

            // Stream the bytes from the web to disk
            java.nio.file.Files.copy(
                    is,
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            targetFile.delete();
            throw new RuntimeException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }


    }
}
