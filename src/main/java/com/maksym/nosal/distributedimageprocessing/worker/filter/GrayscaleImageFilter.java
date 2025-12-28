package com.maksym.nosal.distributedimageprocessing.worker.filter;

import net.coobird.thumbnailator.filters.ImageFilter;

import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

public class GrayscaleImageFilter implements ImageFilter {
    @Override
    public BufferedImage apply(BufferedImage bufferedImage) {
        // Create a destination image with the same dimensions but Gray type
        BufferedImage grayImage = new BufferedImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        // Define the color conversion operation
        ColorConvertOp op = new ColorConvertOp(
                bufferedImage.getColorModel().getColorSpace(),
                grayImage.getColorModel().getColorSpace(),
                null
        );

        // Perform the conversion
        op.filter(bufferedImage, grayImage);

        return grayImage;
    }
}
