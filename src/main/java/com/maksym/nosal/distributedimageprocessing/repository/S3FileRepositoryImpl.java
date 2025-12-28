package com.maksym.nosal.distributedimageprocessing.repository;

import com.maksym.nosal.distributedimageprocessing.repository.interfaces.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class S3FileRepositoryImpl implements ImageRepository {

    private final S3Client s3Client;
    private final String bucketName = "images";

    @Override
    public String upload(String filename, InputStream inputStream, long contentLength, String contentType) {
        String fileKey = UUID.randomUUID() + "-" + filename;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));

        return fileKey;
    }

    @Override
    public InputStream download(String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    @Override
    public void delete(String fileKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        s3Client.deleteObject(deleteRequest);
    }
}
