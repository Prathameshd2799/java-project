import { useState, useEffect } from "react";
import HistoryPanel from "./components/HistoryPanel";
import "./index.css";

function App() {
    const [purpose, setPurpose] = useState("");
    const [tone, setTone] = useState("Professional");
    const [receiver, setReceiver] = useState("");
    const [points, setPoints] = useState("");
    const [email, setEmail] = useState("");
    const [status, setStatus] = useState("");

    const generateEmail = async (e) => {
        e.preventDefault();
        setStatus("Generating...");
        setEmail("");

        try {
            const response = await fetch("/generate-email", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    purpose,
                    tone,
                    receiver,
                    points,
                }),
            });

            const data = await response.json();
            setEmail(data.email);
            setStatus("Done");
        } catch (error) {
            setStatus("Error reaching backend");
        }
    };

    return (
        <>
            <div className="container">
                <h1>AutoMailPro – AI Email Generator</h1>

                <form onSubmit={generateEmail}>

                    <label>Purpose</label>
                    <input value={purpose} onChange={(e) => setPurpose(e.target.value)} required />

                    <label>Tone</label>
                    <select value={tone} onChange={(e) => setTone(e.target.value)}>
                        <option>Professional</option>
                        <option>Formal</option>
                        <option>Friendly</option>
                        <option>Casual</option>
                    </select>

                    <label>Receiver</label>
                    <input value={receiver} onChange={(e) => setReceiver(e.target.value)} />

                    <label>Key Points</label>
                    <textarea value={points} onChange={(e) => setPoints(e.target.value)} />

                    <button className="primary">Generate Email</button>
                </form>

                {email && (
                    <div className="output">
                        <h2>Generated Email</h2>
                        <pre>{email}</pre>
                    </div>
                )}
            </div>

            <HistoryPanel />
        </>
    );
}

export default App;
