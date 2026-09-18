package com.example.mapservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface QfieldOfficeWorkPhotoMapper {
    Long lockOfficeWork(@Param("workId") long workId);
    Long getOfficeWorkId(@Param("workId") long workId);
    int countPhotos(@Param("workId") long workId, @Param("kind") String kind);
    String insertPhoto(@Param("workId") long workId,
                       @Param("kind") String kind,
                       @Param("fileName") String fileName,
                       @Param("mimeType") String mimeType,
                       @Param("fileSize") int fileSize,
                       @Param("content") byte[] content);
    String getPhotos(@Param("workId") long workId);
    Map<String, Object> getPhotoContent(@Param("workId") long workId, @Param("photoId") long photoId);
    int deletePhoto(@Param("workId") long workId, @Param("photoId") long photoId);
}
