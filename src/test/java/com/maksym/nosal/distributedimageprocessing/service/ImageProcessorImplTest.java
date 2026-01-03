package com.maksym.nosal.distributedimageprocessing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImageProcessorImplTest {

    @InjectMocks
    private ImageProcessorImpl imageProcessor;

    private InputStream createTestImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Add some color to make it a valid image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, (x * y) % 0xFFFFFF);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Test
    void process_Resize_Success() throws Exception {
        // Arrange
        InputStream input = createTestImage(1000, 1000);
        
        // Act
        byte[] result = imageProcessor.process(input, "RESIZE", 500, 500);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        // Verify image dimensions
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(500, resultImage.getWidth());
        assertEquals(500, resultImage.getHeight());
    }

    @Test
    void process_ResizeLarger_Success() throws Exception {
        // Arrange
        InputStream input = createTestImage(100, 100);
        
        // Act
        byte[] result = imageProcessor.process(input, "RESIZE", 200, 200);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(200, resultImage.getWidth());
        assertEquals(200, resultImage.getHeight());
    }

    @Test
    void process_ResizeWithNullDimensions_ThrowsException() throws Exception {
        // Arrange
        InputStream input = createTestImage(1000, 1000);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            imageProcessor.process(input, "RESIZE", null, null));
    }

    @Test
    void process_Grayscale_Success() throws Exception {
        // Arrange
        InputStream input = createTestImage(100, 100);
        
        // Act
        byte[] result = imageProcessor.process(input, "GRAYSCALE", 100, 100);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        // Verify it's a valid image (actual grayscale conversion depends on implementation)
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result));
        assertNotNull(resultImage);
        assertEquals(100, resultImage.getWidth());
        assertEquals(100, resultImage.getHeight());
    }

    @Test
    void process_UnknownAction_ThrowsException() throws Exception {
        // Arrange
        InputStream input = createTestImage(200, 200);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            imageProcessor.process(input, "UNKNOWN_ACTION", null, null));
    }

    @Test
    void process_NullInputStream_ThrowsException() {
        // Act & Assert
        assertThrows(Exception.class, 
            () -> imageProcessor.process(null, "RESIZE", 100, 100));
    }

    @Test
    void process_InvalidImageData_ThrowsException() {
        // Arrange
        InputStream invalidInput = new ByteArrayInputStream("not an image".getBytes());
        
        // Act & Assert
        assertThrows(Exception.class, 
            () -> imageProcessor.process(invalidInput, "RESIZE", 100, 100));
    }

    @Test
    void process_VerySmallDimensions_Success() throws Exception {
        // Arrange
        InputStream input = createTestImage(500, 500);
        
        // Act
        byte[] result = imageProcessor.process(input, "RESIZE", 10, 10);

        // Assert
        assertNotNull(result);
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(10, resultImage.getWidth());
        assertEquals(10, resultImage.getHeight());
    }

    @Test
    void process_LargeDimensions_Success() throws Exception {
        // Arrange
        InputStream input = createTestImage(100, 100);
        
        // Act
        byte[] result = imageProcessor.process(input, "RESIZE", 2000, 2000);

        // Assert
        assertNotNull(result);
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(2000, resultImage.getWidth());
        assertEquals(2000, resultImage.getHeight());
    }
}
