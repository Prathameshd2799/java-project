const resultBox = document.getElementById("resultBox");
const historyList = document.getElementById("historyList");
const favList = document.getElementById("favList");
const templateSelect = document.getElementById("templateSelect");
const searchInput = document.getElementById("historySearch");
const languageSelect = document.getElementById("languageSelect");
const fileInput = document.getElementById("fileInput");
const speechText = document.getElementById("speechText");
const micBtn = document.getElementById("micBtn");

let recognizing = false;
let recognition = null;
let extractedFileText = "";


/* ----------------- LOAD APP ------------------- */
window.addEventListener("load", async () => {
    restoreTheme();
    await loadTemplates();
    await loadHistory();
    initSpeech();
});

/* ------------------- THEME --------------------- */
function toggleTheme(){
    document.body.classList.toggle("dark");
    localStorage.setItem(
        "theme",
        document.body.classList.contains("dark") ? "dark" : "light"
    );
}

function restoreTheme(){
    const t = localStorage.getItem("theme") || "light";
    if (t === "dark") document.body.classList.add("dark");
}

/* ------------------ TEMPLATES ------------------ */
async function loadTemplates(){
    try{
        const res = await fetch("/templates");
        const obj = await res.json();

        templateSelect.innerHTML = "";
        for (const key of Object.keys(obj)){
            const opt = document.createElement("option");
            opt.value = key;
            opt.textContent = key === "" ? "No template" : key;
            templateSelect.appendChild(opt);
        }
    }catch(e){
        console.error(e);
    }
}

/* ---------------- FILE UPLOAD ------------------ */
async function uploadFile(){
    const f = fileInput.files[0];
    if (!f){
        alert("Please choose a file");
        return;
    }

    const fd = new FormData();
    fd.append("file", f);

    resultBox.textContent = "Extracting text...";

    try{
        const res = await fetch("/upload-file", {
            method:"POST",
            body:fd
        });

        const json = await res.json();

        if (json.text){
            extractedFileText = json.text;
            speechText.value = json.text.slice(0, 5000);
            resultBox.textContent = "Text extracted successfully.";
        }else{
            resultBox.textContent = "No text found.";
        }
    }catch(e){
        resultBox.textContent = "Upload failed.";
        console.error(e);
    }
}

/* ------------------- SPEECH -------------------- */
function initSpeech(){
    if (!("webkitSpeechRecognition" in window || "SpeechRecognition" in window)){
        micBtn.disabled = true;
        micBtn.textContent = "Mic N/A";
        return;
    }

    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    recognition = new SR();
    recognition.lang = "en-US";
    recognition.interimResults = true;

    recognition.onstart = () => {
        recognizing = true;
        micBtn.textContent = "🔴 Stop";
    };

    recognition.onend = () => {
        recognizing = false;
        micBtn.textContent = "🎤 Start";
    };

    recognition.onresult = (ev) => {
        let transcript = "";
        for (let i = 0; i < ev.results.length; i++){
            transcript += ev.results[i][0].transcript;
        }
        speechText.value = transcript;
    };
}

function startStopSpeech(){
    if (!recognition) initSpeech();

    if (languageSelect.value === "Hindi")
        recognition.lang = "hi-IN";
    else if (languageSelect.value === "Marathi")
        recognition.lang = "mr-IN";
    else
        recognition.lang = "en-US";

    if (recognizing) recognition.stop();
    else recognition.start();
}

function pasteSpeechToPoints(){
    document.getElementById("points").value += "\n" + speechText.value;
}

/* --------------- GENERATE EMAIL ---------------- */
async function generateEmail(){
    const data = {
        purpose: document.getElementById("purpose").value.trim(),
        tone: document.getElementById("tone").value.trim(),
        receiver: document.getElementById("receiver").value.trim(),
        points: document.getElementById("points").value.trim(),
        templateType: templateSelect.value,
        language: languageSelect.value,
        uploadedText: extractedFileText || speechText.value || ""
    };

    if (!data.purpose){
        alert("Please enter purpose");
        return;
    }

    resultBox.textContent =
