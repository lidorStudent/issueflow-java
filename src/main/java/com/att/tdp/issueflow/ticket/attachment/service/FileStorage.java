package com.att.tdp.issueflow.ticket.attachment.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorage {

  StoredFile store(InputStream in, long sizeBytes) throws IOException;

  Path resolve(String storagePath);

  void delete(String storagePath) throws IOException;

  record StoredFile(String relativePath, String sha256) {}
}
