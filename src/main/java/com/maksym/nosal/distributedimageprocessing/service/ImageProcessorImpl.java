package com.maksym.nosal.distributedimageprocessing.service;

import com.maksym.nosal.distributedimageprocessing.service.interfaces.ImageProcessor;
import com.maksym.nosal.distributedimageprocessing.worker.filter.GrayscaleImageFilter;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class ImageProcessorImpl implements ImageProcessor {

    public byte[] process(InputStream input, String action, Integer width, Integer height) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        var builder = Thumbnails.of(input);

        switch (action.toUpperCase()) {
            case "RESIZE" -> builder.size(width, height);
            case "GRAYSCALE" -> builder.addFilter(new GrayscaleImageFilter());
            default -> builder.size(640, 480);
        }

        builder.toOutputStream(outputStream);
        return outputStream.toByteArray();
    }
}
