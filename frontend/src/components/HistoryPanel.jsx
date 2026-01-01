import { useEffect, useState } from "react";

export default function HistoryPanel() {
    const [history, setHistory] = useState([]);

    useEffect(() => {
        fetch("/history")
            .then(res => res.json())
            .then(data => setHistory(data));
    }, []);

    return (
        <div style={{
            width: "300px",
            height: "100vh",
            position: "fixed",
            right: 0,
            top: 0,
            padding: "15px",
            overflow: "scroll",
            background: "#f5f5f5",
            borderLeft: "1px solid #ddd"
        }}>
            <h3>History</h3>

            {history.map((item, i) => (
                <div key={i} style={{ marginBottom: "20px" }}>
                    <b>{item.purpose}</b>
                    <br />
                    <small>{item.created_at}</small>
                    <hr />
                </div>
            ))}
        </div>
    );
}
