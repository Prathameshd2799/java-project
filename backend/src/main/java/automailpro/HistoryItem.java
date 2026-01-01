package automailpro;

public class HistoryItem {
    public int id;
    public String purpose;
    public String tone;
    public String receiver;
    public String points;
    public String email;
    public String created_at;
    public int is_favorite;
    public String template_type;

    public HistoryItem(int id, String purpose, String tone, String receiver, String points, String email, String created_at, int is_favorite, String template_type) {
        this.id = id;
        this.purpose = purpose;
        this.tone = tone;
        this.receiver = receiver;
        this.points = points;
        this.email = email;
        this.created_at = created_at;
        this.is_favorite = is_favorite;
        this.template_type = template_type;
    }
}
