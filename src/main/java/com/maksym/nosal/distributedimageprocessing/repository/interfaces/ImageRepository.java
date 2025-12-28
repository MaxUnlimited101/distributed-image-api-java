package com.maksym.nosal.distributedimageprocessing.repository.interfaces;

import java.io.InputStream;

public interface ImageRepository {
    String upload(String filename, InputStream inputStream, long contentLength, String contentType);
    InputStream download(String fileKey);
    void delete(String fileKey);
}