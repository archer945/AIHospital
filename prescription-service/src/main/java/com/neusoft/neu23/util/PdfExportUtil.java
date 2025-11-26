package com.neusoft.neu23.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.neusoft.neu23.entity.DrugInfo;
import com.neusoft.neu23.entity.Prescription;
import com.neusoft.neu23.entity.PrescriptionDrug;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 简易 PDF 导出工具，用于生成诊断报告与处方单。
 */
public final class PdfExportUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PdfExportUtil() {}

    public static byte[] buildDiagnosisPdf(String title, String diagnosisText) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font bodyFont = new Font(baseFont, 12, Font.NORMAL);

            document.add(new Paragraph(title, titleFont));
            String timeLabel = "生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            document.add(new Paragraph(timeLabel, bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            document.add(new Paragraph(valueOrFallback(diagnosisText, "暂无诊断内容"), bodyFont));

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("生成 PDF 失败: " + e.getMessage(), e);
        }
    }

    public static byte[] buildPrescriptionPdf(Prescription prescription,
                                              List<PrescriptionDrug> drugs,
                                              Map<Long, DrugInfo> drugInfoMap) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font labelFont = new Font(baseFont, 12, Font.BOLD);
            Font bodyFont = new Font(baseFont, 12, Font.NORMAL);

            document.add(new Paragraph("患者处方", titleFont));
            document.add(new Paragraph("处方ID: " + prescription.getPrescriptionId(), bodyFont));
            document.add(new Paragraph("挂号ID: " + valueOrFallback(prescription.getRegisterId()), bodyFont));
            document.add(new Paragraph("患者ID: " + valueOrFallback(prescription.getPatientId()), bodyFont));
            String createdAt = prescription.getCreateTime() != null
                    ? prescription.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "未记录";
            document.add(new Paragraph("开立时间: " + createdAt, bodyFont));
            document.add(new Paragraph("\n诊断", labelFont));
            document.add(new Paragraph(valueOrFallback(prescription.getDiagnosis(), "未填写"), bodyFont));

            document.add(new Paragraph("\n用药清单", labelFont));
            if (drugs == null || drugs.isEmpty()) {
                document.add(new Paragraph("暂无药品记录", bodyFont));
            } else {
                for (int i = 0; i < drugs.size(); i++) {
                    PrescriptionDrug drug = drugs.get(i);
                    DrugInfo info = drugInfoMap.get(drug.getDrugId());
                    String drugName = info != null
                            ? info.getName()
                            : (drug.getDrugId() != null ? "药品ID: " + drug.getDrugId() : "未指定药品");
                    document.add(new Paragraph((i + 1) + ". " + drugName, labelFont));
                    document.add(new Paragraph("剂量: " + valueOrFallback(drug.getDosage(), "未填写"), bodyFont));
                    if (StringUtils.hasText(drug.getReason())) {
                        document.add(new Paragraph("说明: " + formatMaybeJson(drug.getReason()), bodyFont));
                    }
                    document.add(new Paragraph("\n", bodyFont));
                }
            }

            if (StringUtils.hasText(prescription.getAiRecommendation())) {
                document.add(new Paragraph("AI 建议/注意事项", labelFont));
                document.add(new Paragraph(formatMaybeJson(prescription.getAiRecommendation()), bodyFont));
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("生成处方 PDF 失败: " + e.getMessage(), e);
        }
    }

    private static String valueOrFallback(Object value) {
        return Objects.toString(value, "未填写");
    }

    private static String valueOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    /**
     * If the text is JSON (object/array), pretty print it for PDF readability; otherwise return original.
     */
    private static String formatMaybeJson(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(text);
            if (node.isTextual()) {
                // unwrap quoted string
                String unwrapped = node.asText();
                return StringUtils.hasText(unwrapped) ? unwrapped : text;
            }
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception ignored) {
            return text;
        }
    }
}
