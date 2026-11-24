package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// 작성본의 사진 참조와 첨부 파일을 검증하고 새 사진을 저장
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationPhotoServiceImpl implements InvitationPhotoService {

    // 새 대표 사진의 파일 파트 이름
    private static final String COVER_PART = "cover-upload";

    // 새 방명록 사진의 파일 파트 이름
    private static final String GUESTBOOK_PART = "guestbook-upload";

    // 새 갤러리 사진에 허용하는 파일 파트 이름
    private static final Pattern GALLERY_PART = Pattern.compile("gallery-upload-[1-6]");

    // 최종 작성본에 포함할 수 있는 최대 갤러리 사진 수
    private static final int MAX_GALLERY_PHOTOS = 6;

    // 한 번의 저장 요청에 첨부할 수 있는 최대 사진 수
    private static final int MAX_PHOTO_PARTS = 8;

    // 사진 한 장의 최대 파일 크기
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    // 디코딩 전에 검사할 이미지의 최대 픽셀 수
    private static final long MAX_PIXELS = 16_000_000L;

    // 사진 객체의 저장과 삭제를 담당
    private final InvitationPhotoStorage photoStorage;

    // 작성본 저장 트랜잭션에서 사진 키를 검증하고 새 사진을 반영
    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public ObjectNode resolvePhotos(
            Long invitationId,
            JsonNode currentBody,
            ObjectNode requestBody,
            Map<String, List<MultipartFile>> photoParts
    ) {
        // 현재 작성본의 사진 키를 수집하고 수정할 본문을 복사
        Set<String> existingKeys = collectExistingKeys(currentBody);
        ObjectNode updatedBody = requestBody.deepCopy();
        Map<String, ObjectNode> newPhotos = new LinkedHashMap<>();

        // 대표 사진에 허용된 파일 참조를 검증
        validatePhoto(
                updatedBody.path("coverPhoto"),
                true,
                COVER_PART::equals,
                existingKeys,
                newPhotos
        );

        // 최종 갤러리 사진 수와 각 사진의 파일 참조를 검증
        JsonNode galleryPhotos = updatedBody.path("gallery").path("photos");

        if (!galleryPhotos.isMissingNode() && !galleryPhotos.isNull()) {
            if (!galleryPhotos.isArray() || galleryPhotos.size() > MAX_GALLERY_PHOTOS) {
                throw invalidBody();
            }

            for (JsonNode photo : galleryPhotos) {
                validatePhoto(
                        photo,
                        false,
                        name -> GALLERY_PART.matcher(name).matches(),
                        existingKeys,
                        newPhotos
                );
            }
        }

        // 방명록 사진에 허용된 파일 참조를 검증
        validatePhoto(
                updatedBody.path("guestbook").path("photo"),
                true,
                GUESTBOOK_PART::equals,
                existingKeys,
                newPhotos
        );

        // JSON에 포함된 파일 참조와 실제 첨부 파일을 대조
        validateParts(photoParts, newPhotos.keySet());

        if (newPhotos.isEmpty()) {
            return updatedBody;
        }

        // DB 롤백에 대비해 이번 요청에서 생성하는 객체 키를 기록
        List<String> newStorageKeys = new ArrayList<>();
        registerRollbackCleanup(newStorageKeys);

        // 새 파일마다 객체 키를 생성하고 임시 참조를 교체
        for (Map.Entry<String, ObjectNode> entry : newPhotos.entrySet()) {
            MultipartFile file = photoParts.get(entry.getKey()).getFirst();
            storePhoto(
                    invitationId,
                    file,
                    entry.getValue(),
                    newStorageKeys
            );
        }

        return updatedBody;
    }

    // 현재 작성본의 대표·갤러리·방명록 사진에서 기존 키를 수집
    private Set<String> collectExistingKeys(JsonNode currentBody) {
        Set<String> keys = new HashSet<>();
        addExistingKey(keys, currentBody.path("coverPhoto"));

        JsonNode galleryPhotos = currentBody.path("gallery").path("photos");

        if (galleryPhotos.isArray()) {
            for (JsonNode photo : galleryPhotos) {
                addExistingKey(keys, photo);
            }
        }

        addExistingKey(keys, currentBody.path("guestbook").path("photo"));

        return keys;
    }

    // 기존 사진에서 비어 있지 않은 객체 키를 기록
    private void addExistingKey(Set<String> keys, JsonNode photo) {
        JsonNode key = photo.path("storageKey");

        if (key.isTextual() && !key.stringValue().isBlank()) {
            keys.add(key.stringValue());
        }
    }

    // 사진 항목의 기존 키 또는 신규 파일 참조를 검증
    private void validatePhoto(
            JsonNode photo,
            boolean optional,
            Predicate<String> allowedPart,
            Set<String> existingKeys,
            Map<String, ObjectNode> newPhotos
    ) {
        if (optional && (photo.isMissingNode() || photo.isNull())) {
            return;
        }

        if (!(photo instanceof ObjectNode photoObject)) {
            throw invalidBody();
        }

        // 기존 사진 키는 현재 작성본에 등록된 값만 허용
        if (!photoObject.has("fileRef")) {
            JsonNode key = photoObject.path("storageKey");

            if (!key.isTextual() || !existingKeys.contains(key.stringValue())) {
                throw invalidBody();
            }

            return;
        }

        // 새 사진은 해당 위치에서 허용하는 파일 파트를 한 번만 참조
        JsonNode fileRef = photoObject.path("fileRef");

        if (photoObject.has("storageKey")
                || !fileRef.isTextual()
                || !allowedPart.test(fileRef.stringValue())
                || newPhotos.putIfAbsent(fileRef.stringValue(), photoObject) != null) {
            throw invalidBody();
        }
    }

    // 참조 누락, 미사용 첨부, 중복 파일과 크기 초과를 검증
    private void validateParts(
            Map<String, List<MultipartFile>> photoParts,
            Set<String> referencedParts
    ) {
        if (photoParts.size() > MAX_PHOTO_PARTS || !photoParts.keySet().equals(referencedParts)) {
            throw invalidBody();
        }

        for (List<MultipartFile> files : photoParts.values()) {
            if (files.size() != 1 || files.getFirst().isEmpty()) {
                throw invalidPhoto();
            }

            if (files.getFirst().getSize() > MAX_FILE_SIZE) {
                throw new BusinessException(
                        InvitationErrorCode.INVITATION_PHOTO_TOO_LARGE
                );
            }
        }
    }

    // 새 사진을 저장하고 파일 참조를 저장된 객체 키로 교체
    private void storePhoto(
            Long invitationId,
            MultipartFile file,
            ObjectNode photo,
            List<String> newStorageKeys
    ) {
        try {
            byte[] imageBytes = file.getBytes();
            String extension = validateImage(imageBytes);
            String storageKey = "invitations/" + invitationId + "/photos/"
                    + UUID.randomUUID() + "." + extension;

            // 업로드 응답을 받기 전에 연결이 끊겨도 정리할 수 있도록 키를 먼저 기록
            newStorageKeys.add(storageKey);

            photoStorage.upload(
                    storageKey,
                    imageBytes,
                    "image/" + extension
            );

            photo.remove("fileRef");
            photo.put("storageKey", storageKey);
        } catch (IOException e) {
            throw invalidPhoto();
        }
    }

    // 실제 파일에서 이미지 형식과 픽셀 수를 검증하고 디코딩을 시도
    private String validateImage(byte[] imageBytes) throws IOException {
        try (MemoryCacheImageInputStream input =
                     new MemoryCacheImageInputStream(new ByteArrayInputStream(imageBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw invalidPhoto();
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);

                if (!"jpeg".equals(format) && !"png".equals(format)) {
                    throw invalidPhoto();
                }

                // 압축된 파일을 메모리에 펼치기 전에 픽셀 수를 검사
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);

                if (pixels <= 0 || pixels > MAX_PIXELS) {
                    throw invalidPhoto();
                }

                reader.read(0);

                return format;
            } finally {
                reader.dispose();
            }
        }
    }

    // DB 롤백이 확정된 경우 이번 요청에서 생성한 객체만 정리
    private void registerRollbackCleanup(List<String> newStorageKeys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            // 롤백 후 신규 객체를 삭제하고 정리 실패를 기록
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }

                for (String storageKey : newStorageKeys) {
                    try {
                        photoStorage.delete(storageKey);
                    } catch (RuntimeException e) {
                        log.warn("청첩장 사진 정리 실패. storageKey={}", storageKey, e);
                    }
                }
            }
        });
    }

    // 작성본의 사진 참조가 잘못됐을 때 사용할 예외를 생성
    private BusinessException invalidBody() {
        return new BusinessException(
                InvitationErrorCode.INVITATION_INVALID_BODY
        );
    }

    // 사진 형식이 잘못됐거나 사진을 읽을 수 없을 때 사용할 예외를 생성
    private BusinessException invalidPhoto() {
        return new BusinessException(
                InvitationErrorCode.INVITATION_INVALID_COVER_PHOTO
        );
    }
}