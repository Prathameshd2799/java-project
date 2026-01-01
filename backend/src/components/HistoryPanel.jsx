import { useEffect, useState } from "react";

export default function HistoryPanel() {

    const [history, setHistory] = useState([]);

    useEffect(() => {
        fetch("http://localhost:8080/history")
            .then(res => res.json())
            .then(data => setHistory(data));
    }, []);

    return (
        <div style={{
            width: "300px",
            height: "100vh",
            overflowY: "scroll",
            position: "fixed",
            right: 0,
            top: 0,
            background: "#f5f5f5",
            padding: "15px",
            borderLeft: "1px solid #ddd"
        }}>
            <h3>History</h3>
            {history.map((item, i) => (
                <div key={i} style={{marginBottom: "20px"}}>
                    <p><b>{item.purpose}</b></p>
                    <small>{item.created_at}</small>
                    <hr/>
                </div>
            ))}
        </div>
    );
}
