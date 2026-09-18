package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.QfieldOfficeWorkPhotoMapper;
import com.example.mapservice.service.QfieldOfficeWorkPhotoService;
import com.example.mapservice.service.QfieldOfficeWorkService.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QfieldOfficeWorkPhotoServiceImpl implements QfieldOfficeWorkPhotoService {

    private static final Set<String> photoKinds = Set.of("BEFORE", "AFTER");
    private static final Set<String> allowedMimeTypes = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "webp");

    private final QfieldOfficeWorkPhotoMapper mapper;

    @Override
    @Transactional
    public String uploadPhoto(long workId, String kind, MultipartFile file) {
        String photoKind = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        if (!photoKinds.contains(photoKind)) {
            throw new ValidationException("kind must be one of BEFORE, AFTER");
        }
        if (file == null || file.isEmpty()) {
            throw new ValidationException("file is required");
        }
        if (file.getSize() > maxFileSize) {
            throw new FileTooLargeException("file must be at most 10MB");
        }

        String fileName = toFileName(file.getOriginalFilename());
        String extension = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!allowedExtensions.contains(extension)) {
            throw new ValidationException("file extension must be jpg, jpeg, png or webp");
        }
        String declaredType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!allowedMimeTypes.contains(declaredType)) {
            throw new ValidationException("content type must be image/jpeg, image/png or image/webp");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // 헤더·확장자는 클라이언트가 정하는 값이므로 실제 내용(파일 시그니처)으로 이미지 형식을 판별해 저장한다
        String mimeType = detectMimeType(content);
        if (mimeType == null) {
            throw new ValidationException("file is not a valid JPEG, PNG or WebP image");
        }

        // 내업 기록을 잠근 뒤 장수를 세야 동시에 올린 사진이 한도를 넘지 않는다
        if (mapper.lockOfficeWork(workId) == null) {
            return null;
        }
        if (mapper.countPhotos(workId, photoKind) >= maxPhotosPerKind) {
            throw new LimitExceededException(photoKind + " photos are limited to " + maxPhotosPerKind
                    + " per office work; delete one before uploading another");
        }
        return mapper.insertPhoto(workId, photoKind, fileName, mimeType, content.length, content);
    }

    @Override
    public String getPhotos(long workId) {
        if (mapper.getOfficeWorkId(workId) == null) {
            return null;
        }
        return mapper.getPhotos(workId);
    }

    @Override
    public PhotoContent getPhotoContent(long workId, long photoId) {
        Map<String, Object> row = mapper.getPhotoContent(workId, photoId);
        if (row == null) {
            return null;
        }
        return new PhotoContent((String) row.get("file_name"), (String) row.get("mime_type"), (byte[]) row.get("content"));
    }

    @Override
    @Transactional
    public boolean deletePhoto(long workId, long photoId) {
        return mapper.deletePhoto(workId, photoId) > 0;
    }

    /** 브라우저가 붙이는 경로(C:\fakepath\ 등)와 제어 문자를 떼고 DB 길이(255)에 맞춘다 */
    private String toFileName(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (name.isEmpty()) {
            throw new ValidationException("file name is required");
        }
        if (name.length() > 255) {
            // 확장자 검사가 가능하도록 뒤쪽(확장자)을 남긴다
            name = name.substring(name.length() - 255);
        }
        return name;
    }

    /** 파일 시그니처(magic number)로 JPEG / PNG / WebP 를 판별한다. 그 외는 null */
    private String detectMimeType(byte[] content) {
        if (startsWith(content, 0, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(content, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        // RIFF....WEBP
        if (startsWith(content, 0, 'R', 'I', 'F', 'F') && startsWith(content, 8, 'W', 'E', 'B', 'P')) {
            return "image/webp";
        }
        return null;
    }

    private boolean startsWith(byte[] content, int offset, int... signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
