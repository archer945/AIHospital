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
                document.add(new Paragraph(formatRecommendation(prescription.getAiRecommendation()), bodyFont));
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

    /**
     * 将 AI 推荐 JSON 转成便于阅读的中文描述；若无法解析则回退到格式化 JSON/原文。
     */
    private static String formatRecommendation(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(text);
            if (node.isObject()) {
                StringBuilder sb = new StringBuilder();
                if (node.has("diagnosis")) {
                    sb.append("诊断：").append(node.get("diagnosis").asText("")).append("\n");
                }
                if (node.has("symptoms") && node.get("symptoms").isArray()) {
                    sb.append("症状：");
                    sb.append(String.join("；", OBJECT_MAPPER.convertValue(node.get("symptoms"), java.util.List.class)));
                    sb.append("\n");
                }
                if (node.has("treatment_plan")) {
                    sb.append("治疗方案：").append(node.get("treatment_plan").asText("")).append("\n");
                }
                if (node.has("recommended_drugs") && node.get("recommended_drugs").isArray()) {
                    sb.append("推荐用药：\n");
                    for (JsonNode d : node.get("recommended_drugs")) {
                        sb.append(" - 药品：").append(d.path("drug_name").asText(""));
                        if (d.hasNonNull("dosage")) sb.append("；剂量：").append(d.get("dosage").asText());
                        if (d.hasNonNull("usage")) sb.append("；用法：").append(d.get("usage").asText());
                        if (d.hasNonNull("note")) sb.append("；备注：").append(d.get("note").asText());
                        sb.append("\n");
                    }
                }
                if (node.has("drug_interactions") && node.get("drug_interactions").isArray()) {
                    sb.append("药物相互作用：\n");
                    for (JsonNode d : node.get("drug_interactions")) {
                        String a = d.path("drug_a").asText("");
                        String b = d.path("drug_b").asText("");
                        String level = d.path("interaction_level").asText("");
                        sb.append(" - ").append(a).append(" + ").append(b);
                        if (StringUtils.hasText(level)) sb.append("（").append(level).append("）");
                        if (d.hasNonNull("description")) sb.append("：").append(d.get("description").asText());
                        sb.append("\n");
                    }
                }
                if (node.has("warnings")) {
                    sb.append("警告：").append(node.get("warnings").asText("")).append("\n");
                }
                if (node.has("advice")) {
                    sb.append("健康建议：").append(node.get("advice").asText("")).append("\n");
                }
                String rendered = sb.toString().trim();
                if (StringUtils.hasText(rendered)) {
                    return rendered;
                }
            } else if (node.isTextual()) {
                return node.asText();
            }
            return formatMaybeJson(text);
        } catch (Exception ignored) {
            return formatMaybeJson(text);
        }
    }
}
