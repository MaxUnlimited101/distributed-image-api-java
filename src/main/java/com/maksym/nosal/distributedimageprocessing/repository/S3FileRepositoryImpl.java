package com.maksym.nosal.distributedimageprocessing.repository;

import com.maksym.nosal.distributedimageprocessing.repository.interfaces.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class S3FileRepositoryImpl implements ImageRepository {

    private final S3Client s3Client;

    @Value("${s3.bucket.name}")
    private String bucketName;

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    @Override
    public String upload(String filename, InputStream inputStream, long contentLength, String contentType) {
        ensureBucketExists();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filename)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));

        return filename;
    }

    @Override
    public InputStream download(String fileKey) {
        ensureBucketExists();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        try {
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void delete(String fileKey) {
        ensureBucketExists();
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        s3Client.deleteObject(deleteRequest);
    }
}
