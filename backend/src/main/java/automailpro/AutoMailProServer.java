package automailpro;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;
import spark.Spark;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.generate.OllamaGenerateRequest;
import io.github.ollama4j.models.response.OllamaResult;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AutoMailProServer {

    private static final Gson gson = new Gson();
    private static final String MODEL = "gemma3:1b";

    public static void main(String[] args) {

        Spark.port(9000);
        Spark.staticFiles.location("/static");

        HistoryDB.init();

        Spark.get("/", (req, res) -> {
            res.type("text/html");
            return AutoMailProServer.class.getResourceAsStream("/static/index.html");
        });

        Spark.get("/health", (req, res) -> "OK");

        Spark.get("/templates", (req, res) -> {
            res.type("application/json");
            return gson.toJson(getTemplates());
        });

        // Upload file endpoint (multipart) - returns extracted text
        Spark.post("/upload-file", (req, res) -> {
            res.type("application/json");
            try {
                if (!ServletFileUpload.isMultipartContent(req.raw())) {
                    res.status(400);
                    return gson.toJson(Map.of("error", "Not multipart request"));
                }
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> items = upload.parseRequest(req.raw());
                StringBuilder extracted = new StringBuilder();
                for (FileItem item : items) {
                    if (!item.isFormField()) {
                        String name = item.getName().toLowerCase();
                        File tmp = File.createTempFile("upload-", "-" + new File(name).getName());
                        try (InputStream in = item.getInputStream();
                             FileOutputStream out = new FileOutputStream(tmp)) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                        }
                        // detect file type
                        if (name.endsWith(".pdf")) {
                            try (PDDocument doc = PDDocument.load(tmp)) {
                                PDFTextStripper stripper = new PDFTextStripper();
                                extracted.append(stripper.getText(doc)).append("\n");
                            }
                        } else if (name.endsWith(".docx") || name.endsWith(".doc")) {
                            try (FileInputStream fis = new FileInputStream(tmp);
                                 XWPFDocument docx = new XWPFDocument(fis)) {
                                XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
                                extracted.append(extractor.getText()).append("\n");
                            }
                        } else {
                            // fallback: read as text
                            try (BufferedReader br = new BufferedReader(new FileReader(tmp))) {
                                String line;
                                while ((line = br.readLine()) != null) extracted.append(line).append("\n");
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                        tmp.delete();
                    }
                }
                return gson.toJson(Map.of("text", extracted.toString()));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        });

        // Generate Email API (includes language + uploadedText if provided)
        Spark.post("/generate-email", (req, res) -> {
            res.type("application/json");
            EmailRequest data = gson.fromJson(req.body(), EmailRequest.class);

            String templateHint = data.templateType == null ? "" : data.templateType;
            String uploadedText = data.uploadedText == null ? "" : data.uploadedText;

            String prompt = """
                    Write a professional email.
                    Template: %s
                    Language: %s
                    Purpose: %s
                    Tone: %s
                    Receiver: %s
                    Points: %s
                    Extra (from uploaded file or speech): %s
                    Include greeting, body and closing.
                    """
                    .formatted(templateHint,
                            data.language == null ? "English" : data.language,
                            data.purpose, data.tone, data.receiver, data.points, uploadedText);

            try {
                Ollama ollama = new Ollama("http://localhost:11434");
                ollama.setRequestTimeoutSeconds(90);

                OllamaGenerateRequest gen = OllamaGenerateRequest.builder()
                        .withModel(MODEL)
                        .withPrompt(prompt)
                        .build();

                OllamaResult result = ollama.generate(gen, null);
                String email = result.getResponse();

                HistoryDB.save(data.purpose, data.tone, data.receiver, data.points, email, data.templateType);

                return gson.toJson(new EmailResponse(email));
            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(new EmailResponse("Error: " + e.getMessage()));
            }
        });

        Spark.get("/history", (req, res) -> {
            res.type("application/json");
            List<HistoryItem> rows = HistoryDB.list();
            return gson.toJson(rows);
        });

        Spark.post("/toggle-favorite/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            HistoryDB.toggleFavorite(id);
            return "{\"ok\":true}";
        });

        Spark.delete("/delete/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            HistoryDB.deleteById(id);
            return "{\"ok\":true}";
        });

        Spark.post("/clear-history", (req, res) -> {
            HistoryDB.clearAll();
            return "{\"ok\":true}";
        });

        System.out.println("Backend running at: http://localhost:9000");
    }

    private static Map<String,String> getTemplates() {
        Map<String,String> t = new HashMap<>();
        t.put("","No template");
        t.put("Job Application","Job application: Apply for a job with introduction and qualifications.");
        t.put("Leave Request","Leave request: Requesting leave with duration and reason.");
        t.put("College Request","College request: Request for document/certificate from college.");
        t.put("Professional Complaint","Professional complaint: Formal complaint about service/issue.");
        t.put("Apology Email","Apology: Sincere apology with reason and corrective steps.");
        t.put("Project Update","Project update: Summary of progress, blockers, next steps.");
        return t;
    }
}
