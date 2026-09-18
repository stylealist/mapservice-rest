package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.QfieldReportMapper;
import com.example.mapservice.service.QfieldReportService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class QfieldReportServiceImpl implements QfieldReportService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QfieldReportServiceImpl.class);

    private final QfieldReportMapper reportMapper;

    // 테마 컬러
    private static final Color COLOR_PRIMARY = new Color(31, 95, 168);       // #1F5FA8
    private static final Color COLOR_PRIMARY_BG = new Color(238, 243, 248);  // #EEF3F8
    private static final Color COLOR_BORDER = new Color(207, 216, 226);      // #CFD8E2
    private static final Color COLOR_TEXT_DARK = new Color(17, 32, 46);      // #11202E
    private static final Color COLOR_TEXT_MUTED = new Color(86, 101, 117);   // #566575
    private static final Color COLOR_DONE = new Color(27, 107, 79);          // #1B6B4F
    private static final Color COLOR_HOLD = new Color(168, 87, 28);          // #A8571C

    // 외업(QField 앱) 사진은 DB 에 경로만 있고 원본은 QFieldCloud 에 있어 이 서비스가 중계해 받아온다
    private final com.example.mapservice.service.QfieldMediaService mediaService;

    public QfieldReportServiceImpl(QfieldReportMapper reportMapper,
                                   com.example.mapservice.service.QfieldMediaService mediaService) {
        this.reportMapper = reportMapper;
        this.mediaService = mediaService;
    }

    /**
     * 외업 사진(photo_1~photo_5)을 QFieldCloud 에서 받아온다.
     *
     * 계정 미설정·파일 없음 등으로 실패해도 보고서 자체는 나와야 하므로, 실패한 사진은 건너뛰고 경고만 남긴다.
     * (계정이 없으면 QfieldMediaService 가 NOT_CONFIGURED 로 예외를 던진다)
     */
    private List<Map<String, Object>> loadFieldPhotos(String totalId, Map<String, Object> data) {
        List<Map<String, Object>> loaded = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Object raw = data.get("photo_" + i);
            String path = raw == null ? "" : String.valueOf(raw).trim();
            if (path.isEmpty() || "null".equalsIgnoreCase(path)) {
                continue;
            }
            try {
                com.example.mapservice.service.QfieldMediaService.MediaContent media =
                        mediaService.getFacilityMedia(totalId, path);
                if (media != null && media.body() != null && media.body().length > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("content", media.body());
                    item.put("file_name", media.fileName() != null ? media.fileName() : path);
                    loaded.add(item);
                }
            } catch (Exception e) {
                log.warn("보고서 외업 사진을 불러오지 못했습니다. totalId={}, path={}, 사유={}", totalId, path, e.getMessage());
            }
        }
        return loaded;
    }

    /**
     * PDF 에 임베드할 한글 폰트 후보 경로.
     * 운영 이미지는 alpine 이라 Dockerfile 에서 font-nanum 을 설치하며, 그 경로가 데비안 계열과 다르다.
     * REPORT_FONT_PATH 환경변수로 덮어쓸 수 있다.
     */
    private static final String[] KOREAN_FONT_PATHS = {
            "/usr/share/fonts/nanum/NanumGothic.ttf",            // alpine: apk add font-nanum
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",   // debian/ubuntu: fonts-nanum
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",   // 한글은 안 되지만 최후 수단
            "C:/Windows/Fonts/malgun.ttf",                       // 로컬 개발(윈도우)
            "C:/Windows/Fonts/NanumGothic.ttf"
    };

    // 폰트는 요청마다 새로 읽을 필요가 없다
    private volatile BaseFont cachedBaseFont;

    private BaseFont getBaseFont() {
        BaseFont cached = cachedBaseFont;
        if (cached != null) {
            return cached;
        }
        try {
            String override = System.getenv("REPORT_FONT_PATH");
            String found = null;
            if (override != null && !override.isBlank() && new File(override).exists()) {
                found = override;
            } else {
                for (String path : KOREAN_FONT_PATHS) {
                    if (new File(path).exists()) {
                        found = path;
                        break;
                    }
                }
            }

            BaseFont bf;
            if (found != null) {
                bf = BaseFont.createFont(found, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                log.info("보고서 PDF 한글 폰트: {} (임베드)", found);
            } else {
                // 폰트를 못 찾으면 임베드 없이 CJK 폰트로 넘어간다 — 뷰어에 따라 글자가 깨질 수 있으므로 경고를 남긴다
                bf = BaseFont.createFont("HYGoThic-Medium", "UniKS-UCS2-H", BaseFont.NOT_EMBEDDED);
                log.warn("보고서 PDF 한글 폰트를 찾지 못해 임베드 없이 생성합니다. 이미지에 나눔폰트를 설치하거나 REPORT_FONT_PATH 를 지정하세요. 확인한 경로: {}",
                        String.join(", ", KOREAN_FONT_PATHS));
            }
            cachedBaseFont = bf;
            return bf;
        } catch (Exception e) {
            throw new RuntimeException("한글 폰트 초기화 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateFacilityReportPdf(String totalId, Long workId) {
        Map<String, Object> data = reportMapper.getFacilityReportData(totalId, workId);
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("시설물 정보 또는 내업 기록을 찾을 수 없습니다. (totalId=" + totalId + ")");
        }

        String status = String.valueOf(data.get("work_status"));
        if (!"DONE".equalsIgnoreCase(status) && !"HOLD".equalsIgnoreCase(status)) {
            throw new IllegalStateException("내업 처리 상태가 완료(DONE) 또는 보류(HOLD)인 시설물만 보고서를 출력할 수 있습니다. (현재 상태: " + status + ")");
        }

        long actualWorkId = ((Number) data.get("work_id")).longValue();
        List<Map<String, Object>> photos = reportMapper.getPhotosWithContent(actualWorkId);

        BaseFont bf = getBaseFont();
        Font fTitle = new Font(bf, 17, Font.BOLD, COLOR_PRIMARY);
        Font fSub = new Font(bf, 8.5f, Font.NORMAL, COLOR_TEXT_MUTED);
        Font fSecTitle = new Font(bf, 10.5f, Font.BOLD, COLOR_PRIMARY);
        Font fTh = new Font(bf, 8.5f, Font.BOLD, COLOR_TEXT_DARK);
        Font fTd = new Font(bf, 8.5f, Font.NORMAL, COLOR_TEXT_DARK);
        Font fCaption = new Font(bf, 7.5f, Font.NORMAL, COLOR_TEXT_MUTED);

        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PageNumberEvent(bf));
            document.open();

            // 1. 헤더 (문서 제목 및 상태 배지)
            String statusKo = "DONE".equalsIgnoreCase(status) ? "완료" : "보류";
            Color statusColor = "DONE".equalsIgnoreCase(status) ? COLOR_DONE : COLOR_HOLD;

            PdfPTable titleTable = new PdfPTable(new float[]{70, 30});
            titleTable.setWidthPercentage(100);
            titleTable.setSpacingAfter(10);

            // 좌측: 문서 타이틀 및 메타정보
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            Paragraph pTitle = new Paragraph("시설물 내업 처리 결과보고서 (" + statusKo + ")", fTitle);
            pTitle.setSpacingAfter(4);
            titleCell.addElement(pTitle);

            String issueDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
            titleCell.addElement(new Paragraph("관리번호: " + totalId + "-" + actualWorkId + "   |   발행일시: " + issueDate, fSub));
            titleTable.addCell(titleCell);

            // 우측: 상태 인장 배지
            PdfPCell badgeCell = new PdfPCell();
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPTable badgeBox = new PdfPTable(1);
            badgeBox.setWidthPercentage(60);
            badgeBox.setHorizontalAlignment(Element.ALIGN_RIGHT);
            PdfPCell bCell = new PdfPCell(new Phrase(statusKo, new Font(bf, 14, Font.BOLD, statusColor)));
            bCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bCell.setBorderColor(statusColor);
            bCell.setBorderWidth(1.5f);
            bCell.setPaddingTop(6);
            bCell.setPaddingBottom(6);
            bCell.setBackgroundColor(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 25));
            badgeBox.addCell(bCell);
            badgeCell.addElement(badgeBox);

            titleTable.addCell(badgeCell);
            document.add(titleTable);

            // 2. 시설물 기본 정보 섹션
            addSectionTitle(document, "1. 시설물 기본 정보", fSecTitle);

            PdfPTable facTable = new PdfPTable(new float[]{18, 32, 18, 32});
            facTable.setWidthPercentage(100);
            facTable.setSpacingAfter(12);

            String sidoNm = String.valueOf(data.get("sido_nm"));
            String sggNm = String.valueOf(data.get("sgg_nm"));
            String emdNm = String.valueOf(data.get("emd_nm"));
            String fullAdm = String.join(" ", sidoNm, sggNm, emdNm).trim();

            addTableCell(facTable, "시설물 번호", fTh, true);
            addTableCell(facTable, String.valueOf(data.get("total_id")), fTd, false);
            addTableCell(facTable, "시설물 명", fTh, true);
            addTableCell(facTable, String.valueOf(data.get("fclt_nm")), fTd, false);

            addTableCell(facTable, "관리 기관", fTh, true);
            addTableCell(facTable, String.valueOf(data.get("inst_nm")), fTd, false);
            addTableCell(facTable, "행정구역", fTh, true);
            addTableCell(facTable, fullAdm.isEmpty() ? "-" : fullAdm, fTd, false);

            addTableCell(facTable, "소재지 주소", fTh, true);
            PdfPCell addrCell = createCell(String.valueOf(data.get("daddr")), fTd, false);
            addrCell.setColspan(3);
            facTable.addCell(addrCell);

            addTableCell(facTable, "점검 상태", fTh, true);
            addTableCell(facTable, String.valueOf(data.get("facility_condition")), fTd, false);
            addTableCell(facTable, "현장 점검일", fTh, true);
            addTableCell(facTable, String.valueOf(data.get("inv_dt")), fTd, false);

            document.add(facTable);

            // 외업(현장조사 앱) 사진 — 시설물 기본 정보에 이어서 보여준다
            List<Map<String, Object>> fieldPhotos = loadFieldPhotos(totalId, data);
            PdfPTable fieldPhotoTable = new PdfPTable(2);
            fieldPhotoTable.setWidthPercentage(100);
            fieldPhotoTable.setSpacingBefore(6);
            fieldPhotoTable.setSpacingAfter(12);

            PdfPCell fieldHeader = new PdfPCell(new Phrase("외업 현장 사진 (조사 앱 촬영)", fTh));
            fieldHeader.setColspan(2);
            fieldHeader.setBackgroundColor(COLOR_PRIMARY_BG);
            fieldHeader.setBorderColor(COLOR_BORDER);
            fieldHeader.setPadding(5);
            fieldHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            fieldPhotoTable.addCell(fieldHeader);

            if (fieldPhotos.isEmpty()) {
                PdfPCell none = new PdfPCell(new Phrase("등록된 외업 사진 없음", fCaption));
                none.setColspan(2);
                none.setBorderColor(COLOR_BORDER);
                none.setPadding(10);
                none.setHorizontalAlignment(Element.ALIGN_CENTER);
                fieldPhotoTable.addCell(none);
            } else {
                for (Map<String, Object> photo : fieldPhotos) {
                    // 기존 헬퍼는 목록을 받아 첫 장을 그린다 — 한 칸에 한 장씩 넣는다
                    fieldPhotoTable.addCell(createPhotoCell(java.util.Collections.singletonList(photo), bf, fCaption));
                }
                // 한 줄에 2장씩이라 홀수면 빈 칸을 채운다
                if (fieldPhotos.size() % 2 == 1) {
                    PdfPCell filler = new PdfPCell(new Phrase("", fCaption));
                    filler.setBorderColor(COLOR_BORDER);
                    filler.setPadding(6);
                    fieldPhotoTable.addCell(filler);
                }
            }
            document.add(fieldPhotoTable);

            // 3. 내업 처리 상세 내역 섹션
            addSectionTitle(document, "2. 내업 처리 상세 내역", fSecTitle);

            PdfPTable workTable = new PdfPTable(new float[]{18, 32, 18, 32});
            workTable.setWidthPercentage(100);
            workTable.setSpacingAfter(12);

            addTableCell(workTable, "처리 상태", fTh, true);
            Font fStatusVal = new Font(bf, 8.5f, Font.BOLD, statusColor);
            addTableCell(workTable, statusKo + " (" + status + ")", fStatusVal, false);

            addTableCell(workTable, "완료 일자", fTh, true);
            addTableCell(workTable, String.valueOf(data.get("complete_date")), fTd, false);

            addTableCell(workTable, "담당 부서", fTh, true);
            addTableCell(workTable, String.valueOf(data.get("dept_nm")), fTd, false);
            addTableCell(workTable, "담당자 / 연락처", fTh, true);
            String mgr = String.valueOf(data.get("manager_nm"));
            String tel = String.valueOf(data.get("manager_tel"));
            addTableCell(workTable, mgr + (tel.equals("-") ? "" : " (" + tel + ")"), fTd, false);

            addTableCell(workTable, "소요 비용", fTh, true);
            Object costObj = data.get("cost");
            String costFormatted = "-";
            if (costObj != null && !String.valueOf(costObj).trim().isEmpty()) {
                try {
                    long costVal = Long.parseLong(String.valueOf(costObj));
                    costFormatted = new DecimalFormat("#,###").format(costVal) + " 원";
                } catch (Exception ignored) {
                    costFormatted = String.valueOf(costObj) + " 원";
                }
            }
            addTableCell(workTable, costFormatted, fTd, false);

            addTableCell(workTable, "시공/수리업체", fTh, true);
            addTableCell(workTable, String.valueOf(data.get("vendor_nm")), fTd, false);

            addTableCell(workTable, "계약 번호", fTh, true);
            addTableCell(workTable, String.valueOf(data.get("contract_no")), fTd, false);
            addTableCell(workTable, "접수/예정일", fTh, true);
            addTableCell(workTable, String.valueOf(data.get("plan_date")), fTd, false);

            addTableCell(workTable, "처리 내용", fTh, true);
            PdfPCell contentCell = createCell(String.valueOf(data.get("work_content")), fTd, false);
            contentCell.setColspan(3);
            contentCell.setMinimumHeight(35);
            workTable.addCell(contentCell);

            addTableCell(workTable, "비고 사항", fTh, true);
            PdfPCell remarkCell = createCell(String.valueOf(data.get("remark")), fTd, false);
            remarkCell.setColspan(3);
            workTable.addCell(remarkCell);

            document.add(workTable);

            // 4. 현장 처리 전·후 사진 섹션
            addSectionTitle(document, "3. 현장 처리 전·후 사진 비교", fSecTitle);

            List<Map<String, Object>> beforePhotos = new ArrayList<>();
            List<Map<String, Object>> afterPhotos = new ArrayList<>();
            for (Map<String, Object> p : photos) {
                String kind = String.valueOf(p.get("kind"));
                if ("BEFORE".equalsIgnoreCase(kind)) beforePhotos.add(p);
                else afterPhotos.add(p);
            }

            PdfPTable photoTable = new PdfPTable(2);
            photoTable.setWidthPercentage(100);
            photoTable.setSpacingAfter(14);

            // 사진 헤더
            PdfPCell hBefore = new PdfPCell(new Phrase("처리 전 사진 (BEFORE)", fTh));
            hBefore.setBackgroundColor(COLOR_PRIMARY_BG);
            hBefore.setBorderColor(COLOR_BORDER);
            hBefore.setHorizontalAlignment(Element.ALIGN_CENTER);
            hBefore.setPadding(5);
            photoTable.addCell(hBefore);

            PdfPCell hAfter = new PdfPCell(new Phrase("처리 후 사진 (AFTER)", fTh));
            hAfter.setBackgroundColor(COLOR_PRIMARY_BG);
            hAfter.setBorderColor(COLOR_BORDER);
            hAfter.setHorizontalAlignment(Element.ALIGN_CENTER);
            hAfter.setPadding(5);
            photoTable.addCell(hAfter);

            // 사진 내용
            PdfPCell cBefore = createPhotoCell(beforePhotos, bf, fCaption);
            PdfPCell cAfter = createPhotoCell(afterPhotos, bf, fCaption);
            photoTable.addCell(cBefore);
            photoTable.addCell(cAfter);

            document.add(photoTable);

            // 5. 확인 및 결재 서명란
            PdfPTable signTable = new PdfPTable(new float[]{50, 25, 25});
            signTable.setWidthPercentage(100);
            signTable.setKeepTogether(true);

            PdfPCell noticeCell = new PdfPCell();
            noticeCell.setBorder(Rectangle.NO_BORDER);
            noticeCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            noticeCell.addElement(new Paragraph("본 보고서는 시설물 관리 조사 및 내업 처리 내역에 따라 작성되었음을 확인합니다.", fCaption));
            signTable.addCell(noticeCell);

            PdfPCell sign1 = new PdfPCell();
            sign1.setBorderColor(COLOR_BORDER);
            sign1.setPadding(6);
            sign1.addElement(new Paragraph("담당자 확인", fTh));
            Paragraph pSign1 = new Paragraph("\n\n(인 또는 서명)", fCaption);
            pSign1.setAlignment(Element.ALIGN_RIGHT);
            sign1.addElement(pSign1);
            signTable.addCell(sign1);

            PdfPCell sign2 = new PdfPCell();
            sign2.setBorderColor(COLOR_BORDER);
            sign2.setPadding(6);
            sign2.addElement(new Paragraph("부서장 확인", fTh));
            Paragraph pSign2 = new Paragraph("\n\n(인 또는 서명)", fCaption);
            pSign2.setAlignment(Element.ALIGN_RIGHT);
            sign2.addElement(pSign2);
            signTable.addCell(sign2);

            document.add(signTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private PdfPCell createPhotoCell(List<Map<String, Object>> photoList, BaseFont bf, Font fCaption) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(8);
        cell.setMinimumHeight(130);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (photoList == null || photoList.isEmpty()) {
            Paragraph pEmpty = new Paragraph("등록된 사진이 없습니다.", new Font(bf, 8.5f, Font.ITALIC, COLOR_TEXT_MUTED));
            pEmpty.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(pEmpty);
            return cell;
        }

        for (int i = 0; i < Math.min(photoList.size(), 2); i++) {
            Map<String, Object> photo = photoList.get(i);
            byte[] bytes = (byte[]) photo.get("content");
            if (bytes != null && bytes.length > 0) {
                try {
                    Image img = Image.getInstance(bytes);
                    img.scaleToFit(230, 140);
                    img.setAlignment(Element.ALIGN_CENTER);
                    cell.addElement(img);

                    String caption = String.valueOf(photo.get("file_name"));
                    Paragraph pCap = new Paragraph(caption, fCaption);
                    pCap.setAlignment(Element.ALIGN_CENTER);
                    pCap.setSpacingAfter(4);
                    cell.addElement(pCap);
                } catch (Exception e) {
                    cell.addElement(new Paragraph("[이미지 로드 오류: " + photo.get("file_name") + "]", fCaption));
                }
            }
        }
        return cell;
    }

    @Override
    public String getFacilityReportFileName(String totalId, Long workId) {
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return "시설물내업보고서_" + totalId + "_" + today + ".pdf";
    }

    @Override
    public byte[] generateFacilityListReportPdf(List<String> statuses, String sidoCd, String sggCd, String emdCd, String keyword) {
        String code = emdCd != null && !emdCd.isEmpty() ? emdCd :
                (sggCd != null && !sggCd.isEmpty() ? sggCd :
                        (sidoCd != null && !sidoCd.isEmpty() ? sidoCd : null));

        List<String> queryStatuses = (statuses == null || statuses.isEmpty())
                ? List.of("DONE", "HOLD")
                : statuses;

        List<Map<String, Object>> list = reportMapper.getFacilityReportList(queryStatuses, code, keyword);

        BaseFont bf = getBaseFont();
        Font fTitle = new Font(bf, 16, Font.BOLD, COLOR_PRIMARY);
        Font fSub = new Font(bf, 8.5f, Font.NORMAL, COLOR_TEXT_MUTED);
        Font fTh = new Font(bf, 8.0f, Font.BOLD, COLOR_TEXT_DARK);
        Font fTd = new Font(bf, 7.5f, Font.NORMAL, COLOR_TEXT_DARK);
        Font fStats = new Font(bf, 9.0f, Font.BOLD, COLOR_PRIMARY);

        // 일괄 현황 보고서는 가로(A4 Landscape) 모드
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 35, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PageNumberEvent(bf));
            document.open();

            // 1. 헤더 (타이틀 및 생성일시)
            String statusLabels = queryStatuses.contains("DONE") && queryStatuses.contains("HOLD") ? "완료 · 보류" :
                    (queryStatuses.contains("DONE") ? "완료" : "보류");

            Paragraph title = new Paragraph("시설물 내업 처리 현황 보고서 (" + statusLabels + ")", fTitle);
            title.setSpacingAfter(4);
            document.add(title);

            String issueDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
            Paragraph sub = new Paragraph("생성일시: " + issueDate + "   |   대상 상태: " + String.join(", ", queryStatuses), fSub);
            sub.setSpacingAfter(10);
            document.add(sub);

            // 2. 요약 통계 박스
            int countDone = 0;
            int countHold = 0;
            long totalCost = 0;
            for (Map<String, Object> item : list) {
                String st = String.valueOf(item.get("work_status"));
                if ("DONE".equalsIgnoreCase(st)) countDone++;
                else if ("HOLD".equalsIgnoreCase(st)) countHold++;

                Object c = item.get("cost");
                if (c != null) {
                    try { totalCost += Long.parseLong(String.valueOf(c)); } catch (Exception ignored) {}
                }
            }

            PdfPTable statTable = new PdfPTable(4);
            statTable.setWidthPercentage(100);
            statTable.setSpacingAfter(12);

            addTableCell(statTable, "총 대상 건수", fTh, true);
            addTableCell(statTable, list.size() + " 건", fStats, false);
            addTableCell(statTable, "완료 / 보류 건수", fTh, true);
            addTableCell(statTable, "완료 " + countDone + " 건   |   보류 " + countHold + " 건", fStats, false);

            addTableCell(statTable, "총 소요 비용", fTh, true);
            addTableCell(statTable, new DecimalFormat("#,###").format(totalCost) + " 원", fStats, false);
            addTableCell(statTable, "검색 필터 조건", fTh, true);
            String filterDesc = (code != null ? "지역코드: " + code : "전체지역") + (keyword != null ? ", 검색어: " + keyword : "");
            addTableCell(statTable, filterDesc, fTd, false);

            document.add(statTable);

            // 3. 목록 테이블 (순번, 시설물 번호, 시설물명, 행정구역, 주소, 상태, 완료일, 담당부서/담당자, 시공업체, 비용)
            PdfPTable listTable = new PdfPTable(new float[]{4, 11, 16, 11, 18, 6, 8, 12, 10, 8});
            listTable.setWidthPercentage(100);
            listTable.setHeaderRows(1);

            String[] headers = {"No", "시설물번호", "시설물명", "행정구역", "소재지 주소", "상태", "완료일", "담당부서/담당자", "시공업체", "비용(원)"};
            for (String h : headers) {
                PdfPCell thCell = new PdfPCell(new Phrase(h, fTh));
                thCell.setBackgroundColor(COLOR_PRIMARY_BG);
                thCell.setBorderColor(COLOR_BORDER);
                thCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                thCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                thCell.setPadding(4.5f);
                listTable.addCell(thCell);
            }

            if (list.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("조회된 시설물 내업 기록이 없습니다.", fTd));
                emptyCell.setColspan(headers.length);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(15);
                emptyCell.setBorderColor(COLOR_BORDER);
                listTable.addCell(emptyCell);
            } else {
                int no = 1;
                for (Map<String, Object> row : list) {
                    addTableCell(listTable, String.valueOf(no++), fTd, false, Element.ALIGN_CENTER);
                    addTableCell(listTable, String.valueOf(row.get("total_id")), fTd, false, Element.ALIGN_CENTER);
                    addTableCell(listTable, String.valueOf(row.get("fclt_nm")), fTd, false, Element.ALIGN_LEFT);

                    String admStr = String.join(" ",
                            String.valueOf(row.get("sido_nm")),
                            String.valueOf(row.get("sgg_nm")),
                            String.valueOf(row.get("emd_nm"))).trim();
                    addTableCell(listTable, admStr.isEmpty() ? "-" : admStr, fTd, false, Element.ALIGN_LEFT);
                    addTableCell(listTable, String.valueOf(row.get("daddr")), fTd, false, Element.ALIGN_LEFT);

                    String st = String.valueOf(row.get("work_status"));
                    String stKo = "DONE".equalsIgnoreCase(st) ? "완료" : ("HOLD".equalsIgnoreCase(st) ? "보류" : st);
                    Font fStRow = new Font(bf, 7.5f, Font.BOLD, "DONE".equalsIgnoreCase(st) ? COLOR_DONE : COLOR_HOLD);
                    addTableCell(listTable, stKo, fStRow, false, Element.ALIGN_CENTER);

                    addTableCell(listTable, String.valueOf(row.get("complete_date")), fTd, false, Element.ALIGN_CENTER);

                    String dept = String.valueOf(row.get("dept_nm"));
                    String mgr = String.valueOf(row.get("manager_nm"));
                    addTableCell(listTable, dept + (dept.equals("-") || mgr.equals("-") ? "" : " / ") + mgr, fTd, false, Element.ALIGN_LEFT);

                    addTableCell(listTable, String.valueOf(row.get("vendor_nm")), fTd, false, Element.ALIGN_LEFT);

                    Object cost = row.get("cost");
                    String costStr = "-";
                    if (cost != null && !String.valueOf(cost).trim().isEmpty()) {
                        try {
                            costStr = new DecimalFormat("#,###").format(Long.parseLong(String.valueOf(cost)));
                        } catch (Exception ignored) {
                            costStr = String.valueOf(cost);
                        }
                    }
                    addTableCell(listTable, costStr, fTd, false, Element.ALIGN_RIGHT);
                }
            }

            document.add(listTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("일괄 현황 보고서 PDF 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFacilityListReportFileName(List<String> statuses) {
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String stDesc = (statuses != null && statuses.size() == 1) ? statuses.get(0) : "ALL";
        return "시설물내업현황보고서_" + stDesc + "_" + today + ".pdf";
    }

    private void addSectionTitle(Document doc, String title, Font font) throws DocumentException {
        Paragraph p = new Paragraph(title, font);
        p.setSpacingBefore(4);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private void addTableCell(PdfPTable table, String text, Font font, boolean isHeader) {
        addTableCell(table, text, font, isHeader, isHeader ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
    }

    private void addTableCell(PdfPTable table, String text, Font font, boolean isHeader, int align) {
        PdfPCell cell = createCell(text, font, isHeader);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(4.5f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (isHeader) {
            cell.setBackgroundColor(COLOR_PRIMARY_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        } else {
            cell.setBackgroundColor(Color.WHITE);
        }
        return cell;
    }

    /**
     * 하단 페이지 번호 출력 이벤트
     */
    static class PageNumberEvent extends PdfPageEventHelper {
        private final BaseFont bf;
        public PageNumberEvent(BaseFont bf) { this.bf = bf; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();
            cb.beginText();
            cb.setFontAndSize(bf, 7.5f);
            cb.setColorFill(new Color(110, 120, 130));
            String text = "SJ 시설물 관리 시스템  |  페이지 " + writer.getPageNumber();
            cb.showTextAligned(Element.ALIGN_CENTER, text, (document.left() + document.right()) / 2, document.bottom() - 14, 0);
            cb.endText();
            cb.restoreState();
        }
    }
}
