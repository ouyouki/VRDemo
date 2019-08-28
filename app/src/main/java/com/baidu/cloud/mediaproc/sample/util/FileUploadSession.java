package com.baidu.cloud.mediaproc.sample.util;

import android.util.Log;

import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.AbortMultipartUploadRequest;
import com.baidubce.services.bos.model.CompleteMultipartUploadRequest;
import com.baidubce.services.bos.model.CompleteMultipartUploadResponse;
import com.baidubce.services.bos.model.InitiateMultipartUploadRequest;
import com.baidubce.services.bos.model.InitiateMultipartUploadResponse;
import com.baidubce.services.bos.model.PartETag;
import com.baidubce.services.bos.model.UploadPartRequest;
import com.baidubce.services.bos.model.UploadPartResponse;

import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class FileUploadSession {

    private static final String TAG = "FileUploadSession";

    static final long CHUNK_SIZE = 1024 * 1024 * 5L;
    private BosClient bosClient;

    private String bucket;
    private String bosKey;
    private String uploadId;
    private File file;
    private List<PartETag> partETags;

    public FileUploadSession(BosClient bosClient) {
        this.bosClient = bosClient;
    }

    public boolean upload(File file, String bucket, String bosKey) {
        this.file = file;
        this.bucket = bucket;
        this.bosKey = bosKey;
        Log.d(TAG, "upload file bucket=" + bucket + ";bosKey=" + bosKey + ";file=" + file.getName());

        long fileLength = file.length();
        int parts = (int) (fileLength / CHUNK_SIZE);
        if (fileLength % CHUNK_SIZE > 0) {
            parts++;
        }
        partETags = new ArrayList<PartETag>(parts);

        initMultipartUpload();
        int processors = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "availableProcessors =" + processors);
        ExecutorService pool = Executors.newFixedThreadPool(processors);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>(parts);

        for (int i = 0; i < parts; i++) {
            futures.add(pool.submit(new UploadPartTask(this, i)));
        }

        boolean success = true;
        for (int i = 0; i < futures.size(); i++) {
            Future<Boolean> future = futures.get(i);
            try {
                if (future.get()) {
                    Log.d(TAG, "The upload task [ " + i + "] completed.");
                } else {
                    Log.d(TAG, "The upload task [ " + i + "] failed.");
                    success = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
        }
        pool.shutdownNow();

        if (success) {
            Collections.sort(partETags, new Comparator<PartETag>() {
                public int compare(PartETag a, PartETag b) {
                    return a.getPartNumber() - b.getPartNumber();
                }
            });
            // send multi-part upload completion request
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucket, bosKey, uploadId, partETags);

            try {
                CompleteMultipartUploadResponse response =
                        bosClient.completeMultipartUpload(completeMultipartUploadRequest);
                Log.i(TAG, "Success to upload file: " + file.getAbsolutePath() + " to BOS with ETag: "
                        + response.getETag());
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(bucket, bosKey, uploadId);
            bosClient.abortMultipartUpload(abortMultipartUploadRequest);
            Log.i(TAG, "Failed to upload file: " + file.getAbsolutePath());
        }

        return success;
    }

    private boolean uploadPart(int partNum) {
        Log.i(TAG, "Upload part: " + partNum);

        int tryCount = 5;
        while (tryCount > 0) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                long skipBytes = CHUNK_SIZE * partNum;
                fis.skip(skipBytes);

                // compute chunk size
                long partSize = (CHUNK_SIZE < file.length() - skipBytes) ? CHUNK_SIZE : file.length() - skipBytes;

                Log.d(TAG, "[skipBytes]= " + skipBytes + ", [partSize] = " + partSize
                        + ", [file.length() - skipBytes] = " + (file.length() - skipBytes));

                byte[] buf = new byte[(int) partSize];
                int offset = 0;
                while (true) {
                    int byteRead = fis.read(buf, offset, (int) partSize);
                    offset += byteRead;
                    if (byteRead < 0 || offset >= partSize) {
                        break;
                    }
                }
                ByteArrayInputStream bufStream = new ByteArrayInputStream(buf);

                // upload chunk
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucket);
                uploadPartRequest.setKey(bosKey);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(bufStream);
                uploadPartRequest.setPartSize(partSize);
                // part number is 1-based
                uploadPartRequest.setPartNumber(partNum + 1);
                UploadPartResponse uploadPartResponse = bosClient.uploadPart(uploadPartRequest);

                // add ETag to result list
                partETags.add(uploadPartResponse.getPartETag());
                Log.i(TAG, "Complete upload with ETag: " + uploadPartResponse.getPartETag());

            } catch (IOException e) {
                Log.e(TAG, "Failed to upload the part " + partNum + " [tryCount] = " + tryCount);
                tryCount--;
                continue;
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            break;
        }
        if (tryCount == 0) {
            Log.e(TAG, "Failed to upload the part " + partNum);
        } else {
            Log.i(TAG, "Success to upload the part " + partNum);
        }
        return tryCount > 0;
    }

    private void initMultipartUpload() {
        InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                new InitiateMultipartUploadRequest(bucket, bosKey);

        InitiateMultipartUploadResponse initiateMultipartUploadResponse =
                bosClient.initiateMultipartUpload(initiateMultipartUploadRequest);

        uploadId = initiateMultipartUploadResponse.getUploadId();
    }

    class UploadPartTask implements Callable<Boolean> {
        int partNum;
        FileUploadSession session;

        UploadPartTask(FileUploadSession session, int partNum) {
            this.session = session;
            this.partNum = partNum;
        }

        public Boolean call() {
            return session.uploadPart(partNum);
        }
    }
}
