# JARVIS Phase 1 — Tamil, English & Tanglish Voice Engine Analysis & Benchmark Guide

## 1. Speech Recognition (ASR) Comparison for Realme 9i 5G (Android 14)

| Engine | Tamil (`ta-IN`) | Tanglish (Code-Switching) | Latency | Network | Cost | Verdict |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Android `SpeechRecognizer` (Google)** | Good for clear `ta-IN` | Moderate (tends to force either Tamil or English script) | Low (~350–700 ms) | Online primary (offline pack device-dependent) | Free | **Implemented in Phase 1** as native zero-key baseline. |
| **Azure Speech Streaming SDK** | Strong `ta-IN` + custom phrase lists | Good with multi-locale hints | Low (~300–550 ms) | Online required | Paid (free tier: 5 hrs/mo) | **Recommended Phase 1/3 Upgrade** via SecretStore key. |
| **OpenAI Whisper / `gpt-4o-transcribe`** | Good multilingual | Strongest on mixed Tanglish | Medium (~900–1800 ms, non-streaming REST) | Online required | Paid per minute | Best for batch/eval & fallback. |

## 2. Text-to-Speech (TTS) Comparison (Deep, Composed, Natural Voice Target)

| Engine | Tamil Naturalness | English Naturalness | Latency | Network | Cost | Verdict |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Android System TTS (Google `ta-IN` / `en-IN`)** | Acceptable; clearer at `0.85` pitch, `0.95` rate | Good (`en-IN` male/female network/local voices) | Very Low (~80–150 ms) | Offline if voice pack downloaded | Free | **Implemented in Phase 1** as zero-key baseline & offline fallback. |
| **Azure Neural TTS (`ta-IN-ValluvarNeural` / `en-IN-PrabhatNeural`)** | **Natural, deep, composed** with SSML `<lang>` switching | **Very natural** | Low (~250–450 ms) | Online required | Paid (free tier: 0.5M chars/mo) | **Recommended Primary Cloud Voice** for Phase 1–3 via user SecretStore key. |
| **OpenAI TTS (`onyx` / `ash`)** | Uneven Tamil pronunciation | **Cinematic, deep, composed** | Medium (~500–900 ms) | Online required | Paid | Strong English voice; test Tamil carefully before committing. |

## 3. Practical Recommendation for Realme 9i 5G
1. **Default Out-of-the-Box (Zero API Key)**: Use **Android `SpeechRecognizer`** + **Android System TTS** tuned to `pitch = 0.85`, `speechRate = 0.95` (deeper, composed cadence) with `ta-IN` / `en-IN` switching.
2. **Recommended Studio Upgrade (When User Enters Azure Speech Key in SecretStore)**: Switch TTS to **Azure Neural TTS** (`ta-IN-ValluvarNeural` for Tamil/Tanglish, `en-IN-PrabhatNeural` for English) using SSML `<lang>` tags.
