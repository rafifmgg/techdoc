package com.ocmsintranet.apiservice.utilities.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * PDF Generator Service using OpenHTML2PDF for HTML to PDF conversion.
 *
 * This service uses:
 * - OpenHTML2PDF (LGPL v2.1/v3.0 license) - permissive for commercial use
 * - Built on Flying Saucer + Apache PDFBox
 *
 * Features:
 * - Full HTML/CSS rendering support
 * - Tables, complex layouts
 * - CSS styling (fonts, colors, spacing)
 * - Base64 embedded images
 * - SVG content
 * - Template placeholder replacement with {{variable}} syntax
 */
@Slf4j
@Service
public class PdfGeneratorService {

    /**
     * Convert HTML string to PDF bytes using OpenHTML2PDF.
     *
     * OpenHTML2PDF renders HTML with full CSS support, including:
     * - Tables and complex layouts
     * - CSS styling (fonts, colors, spacing)
     * - Base64-encoded embedded images
     * - SVG content
     *
     * @param html HTML content
     * @return PDF as byte array
     * @throws Exception if conversion fails
     */
    public byte[] generatePdfFromHtml(String html) throws Exception {
        try {
            // Sanitize HTML: convert to XHTML format that OpenHTML2PDF can parse
            // OpenHTML2PDF requires well-formed XML, so we parse HTML with Jsoup
            // and output it as XHTML (self-closing tags, proper closing tags)
            String xhtmlContent = sanitizeHtmlToXhtml(html);

            ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();

            // Use PdfRendererBuilder for direct HTML to PDF conversion
            // This preserves all CSS styling, tables, images, and layout
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtmlContent, null);  // null baseUrl uses resource: protocol for embedded resources
            builder.toStream(pdfOutput);
            builder.run();

            byte[] pdfBytes = pdfOutput.toByteArray();
            log.info("PDF generated successfully using OpenHTML2PDF, size: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("Error generating PDF from HTML using OpenHTML2PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Convert HTML to well-formed XHTML that OpenHTML2PDF can parse.
     * OpenHTML2PDF requires strict XML parsing, so self-closing tags must be properly formatted.
     *
     * @param html HTML content
     * @return Sanitized XHTML content
     */
    private String sanitizeHtmlToXhtml(String html) {
        try {
            // Parse HTML with Jsoup (lenient parser)
            Document doc = Jsoup.parse(html);
            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

            // Get the outer HTML which will be properly formed XHTML
            String xhtml = doc.html();

            // Ensure DOCTYPE is included for proper XML parsing
            if (!xhtml.toLowerCase().contains("<!doctype") && !xhtml.toLowerCase().startsWith("<?xml")) {
                xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                        xhtml;
            }

            return xhtml;
        } catch (Exception e) {
            log.warn("Error sanitizing HTML, using original content", e);
            return html;  // Fallback to original if sanitization fails
        }
    }

    /**
     * Fill HTML template with data placeholders.
     * Uses {{placeholder}} syntax for template variables.
     *
     * @param template HTML template with {{placeholder}} syntax
     * @param data Map of placeholder keys and values
     * @return HTML with replaced placeholders
     */
    public String fillTemplate(String template, Map<String, String> data) {
        String result = template;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
