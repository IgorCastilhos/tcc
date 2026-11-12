package br.unisinos.cvmsignals.ingestion.infrastructure.parser;

import br.unisinos.cvmsignals.ingestion.application.port.DocumentParserPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * Parsing com Apache Tika (Seção 4.4): AutoDetectParser cobre PDF, HTML e
 * ZIPs do ENET (extração recursiva de anexos). Para Fatos Relevantes
 * escaneados, a estratégia OCR_AND_TEXT_EXTRACTION do PDFBox aciona o
 * Tesseract (idioma 'por') quando o PDF não contém camada de texto.
 *
 * <p>O binário vem de fora e a extração é recursiva, então o parsing roda
 * com <b>teto de caracteres</b>. Sem ele, um arquivo compactado que expande
 * muito (zip bomb) ou um PDF malformado derrubariam a aplicação por falta de
 * memória — o {@code BodyContentHandler(-1)} original aceitava saída
 * ilimitada.
 */
@Component
public class TikaDocumentParser implements DocumentParserPort {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParser.class);

    private final AutoDetectParser parser = new AutoDetectParser();
    private final int maxChars;

    public TikaDocumentParser(@Value("${ingestion.parsing.max-chars:20000000}") int maxChars) {
        this.maxChars = maxChars;
    }

    @Override
    public String parse(byte[] content) {
        BodyContentHandler handler = new BodyContentHandler(maxChars);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        TesseractOCRConfig ocr = new TesseractOCRConfig();
        ocr.setLanguage("por");
        context.set(TesseractOCRConfig.class, ocr);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.AUTO);
        context.set(PDFParserConfig.class, pdfConfig);

        try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
            parser.parse(in, handler, metadata, context);
        } catch (org.apache.tika.exception.WriteLimitReachedException e) {
            // O teto foi atingido: o que já saiu é aproveitável, e o
            // documento fica registrado como suspeito no log.
            log.warn("Documento excedeu o teto de {} caracteres; texto truncado", maxChars);
        } catch (Exception e) {
            throw new IllegalStateException("Tika falhou ao extrair texto", e);
        }
        return normalize(handler.toString());
    }

    /** Colapsa espaços repetidos preservando quebras de parágrafo, que guiam o chunking. */
    private String normalize(String raw) {
        return raw.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}
