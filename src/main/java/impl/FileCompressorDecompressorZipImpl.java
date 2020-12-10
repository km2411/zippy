package impl;

import com.google.common.collect.Lists;
import com.google.common.collect.SortedSetMultimap;
import enums.ZipFormatType;
import interfaces.FileCompressorDecompressor;
import models.ChunkedUnzip;
import models.ChunkedZip;
import utils.ZippyUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileCompressorDecompressorZipImpl implements FileCompressorDecompressor {

    private final ExecutorService executor;

    public FileCompressorDecompressorZipImpl(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);

    }

    @Override
    public void zip(String sourceDir, String destinationDir, Integer maxFileSize) {
        long start = System.currentTimeMillis();
        Map<String, List<String>> pathToFileList = ZippyUtils.getAllFilesInDir(sourceDir);
        for (Map.Entry<String, List<String>> entry : pathToFileList.entrySet()) {
            String relativePath = entry.getKey();
            for (String file : entry.getValue()) {
                ZippyUtils.createOutDirsIfMissing(destinationDir + relativePath);
                executor.execute(new ChunkedZip(file, sourceDir + relativePath,
                                    destinationDir + relativePath, maxFileSize));
            }
        }
        terminate(start);
    }

    @Override
    public void unzip(String sourceDir, String destinationDir) {
        long start = System.currentTimeMillis();
        Map<String, SortedSetMultimap<String, String>> pathToFileAndPartFiles = ZippyUtils.getAllZippedFilesWithParts(sourceDir,
                                                                            ZipFormatType.ZIP.getExtension());
        for (Map.Entry<String, SortedSetMultimap<String, String>> entry : pathToFileAndPartFiles.entrySet()) {
            String relativePath = entry.getKey();
            for (String file : entry.getValue().keySet()) {
                ZippyUtils.createOutDirsIfMissing(destinationDir + relativePath);
                executor.execute(new ChunkedUnzip(file, sourceDir + relativePath,
                                    destinationDir + relativePath, Lists.newArrayList(entry.getValue().get(file))));
            }
        }
        terminate(start);
    }

    private void terminate(long start) {
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        } finally {
            long finish = System.currentTimeMillis();
            System.out.println("Elapsed Time: " + ((float)(finish - start))/1000 + " seconds");
        }
    }

}
