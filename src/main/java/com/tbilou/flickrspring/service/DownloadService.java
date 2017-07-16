package com.tbilou.flickrspring.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Service;

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

    public void downloadAndSave(String imageUrl, String filename, String foldername) {
        InputStream is = null;
        try {
            URL url = new URL(imageUrl);
            is = url.openStream();
            File targetFile = new File(MessageFormat.format("/tmp/{0}/{1}.jpg", foldername, filename));
            targetFile.getParentFile().mkdirs();

            // Stream the bytes from the web to disk
            java.nio.file.Files.copy(
                    is,
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
        }


    }
}
