package com.maksym.nosal.distributedimageprocessing.service.interfaces;

import java.io.InputStream;

public interface ImageProcessor {
    public byte[] process(InputStream input, String action, Integer width, Integer height) throws Exception;
}
