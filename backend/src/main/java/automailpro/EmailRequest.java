package automailpro;

public class EmailRequest {
    public String purpose;
    public String tone;
    public String receiver;
    public String points;
    public String templateType;
    public String language;      // "English", "Hindi", "Marathi"
    public String uploadedText;  // text extracted from file or voice (optional)
}
