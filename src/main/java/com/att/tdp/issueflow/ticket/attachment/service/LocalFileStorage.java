package com.att.tdp.issueflow.ticket.attachment.service;

import com.att.tdp.issueflow.config.AttachmentProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

// Content addressed filesystem store for attachments.
// Files stream through a SHA-256 digest into a temp file, then move to a path derived from
// the hash. Identical uploads end up at the same path and dedupe.
// The two byte prefix layout (aa/bb/aabb...) keeps any one directory from filling up.
@Service
public class LocalFileStorage implements FileStorage {

  private final AttachmentProperties props;
  private Path root;

  public LocalFileStorage(AttachmentProperties props) {
    this.props = props;
  }

  @PostConstruct
  @SuppressWarnings("unused")
  void init() throws IOException {
    this.root = Path.of(props.dir()).toAbsolutePath();
    Files.createDirectories(root);
  }

  // Stream into a random temp file (so concurrent uploads do not collide) while hashing,
  // then atomically move it into place. If a file already exists at the hash path it is
  // the same bytes, so we drop the temp and reuse the original.
  @Override
  public StoredFile store(InputStream in, long sizeBytes) throws IOException {
    Path tmp = root.resolve("upload-" + UUID.randomUUID());
    MessageDigest digest = sha256Digest();
    try (DigestInputStream dis = new DigestInputStream(in, digest)) {
      Files.copy(dis, tmp, StandardCopyOption.REPLACE_EXISTING);
    }
    String hex = HexFormat.of().formatHex(digest.digest());
    String relative = hex.substring(0, 2) + "/" + hex.substring(2, 4) + "/" + hex + ".bin";
    Path target = root.resolve(relative);
    Files.createDirectories(target.getParent());
    if (!Files.exists(target)) {
      Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    } else {
      Files.deleteIfExists(tmp);
    }
    return new StoredFile(relative, hex);
  }

  @Override
  public Path resolve(String storagePath) {
    return root.resolve(storagePath);
  }

  @Override
  public void delete(String storagePath) throws IOException {
    Files.deleteIfExists(root.resolve(storagePath));
  }

  private MessageDigest sha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
