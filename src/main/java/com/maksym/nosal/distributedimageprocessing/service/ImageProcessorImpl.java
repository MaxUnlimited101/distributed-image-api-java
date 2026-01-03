package com.maksym.nosal.distributedimageprocessing.service;

import com.maksym.nosal.distributedimageprocessing.service.interfaces.ImageProcessor;
import com.maksym.nosal.distributedimageprocessing.worker.filter.GrayscaleImageFilter;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class ImageProcessorImpl implements ImageProcessor {

    public byte[] process(InputStream input, String action, Integer width, Integer height) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        if (action == null || action.isEmpty()) {
            throw new IllegalArgumentException("Action cannot be null or empty");
        }
        if (width == null || width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        if (height == null || height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        var builder = Thumbnails.of(input);

        switch (action.toUpperCase()) {
            case "RESIZE" -> builder.size(width, height);
            case "GRAYSCALE" -> builder.addFilter(new GrayscaleImageFilter()).scale(1);
            default -> builder.size(640, 480).scale(1);
        }

        builder.outputFormat("jpg");
        builder.toOutputStream(outputStream);
        return outputStream.toByteArray();
    }
}
