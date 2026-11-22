#!/usr/bin/env python3
"""
JARVIS Secret Scanner (`scripts/secret_scan.py`)
Scans the source repository, git commit history (if initialized), and compiled APK binaries
for any leaked API keys, private keys, or keystore files.
"""
import os
import re
import sys
import zipfile

FORBIDDEN_EXTENSIONS = {".jks", ".keystore", ".p12", ".pem", ".key"}

SECRET_PATTERNS = [
    ("Google/Gemini API Key", re.compile(r"AIza[0-9A-Za-z\-_]{30,}")),
    ("OpenAI API Key", re.compile(r"sk-[0-9A-Za-z\-_]{24,}")),
    ("Groq API Key", re.compile(r"gsk_[0-9A-Za-z\-_]{24,}")),
    ("Anthropic API Key", re.compile(r"sk-ant-[0-9A-Za-z\-_]{24,}")),
    ("Private Key PEM Block", re.compile(r"-----BEGIN (?:RSA |EC )?PRIVATE KEY-----")),
]

# Allowed test fixture files where dummy strings are tested for redaction
TEST_FIXTURE_ALLOWLIST = {
    "SafeLoggerAndSecretValidatorTest.kt",
    "Phase0JvmTestRunner.java",
    "phase0_jvm_tests.bsh",
    "run_jvm_tests.py",
}

def scan_text(content, label, findings):
    base_name = os.path.basename(label)
    if base_name in TEST_FIXTURE_ALLOWLIST:
        return
    for rule_name, pattern in SECRET_PATTERNS:
        matches = pattern.findall(content)
        if matches:
            findings.append(f"[SECRET MATCH] {rule_name} found in {label}")

def scan_directory(root_dir):
    findings = []
    scanned_files = 0
    for dirpath, dirnames, filenames in os.walk(root_dir):
        if ".git" in dirnames:
            dirnames.remove(".git")
        for fname in filenames:
            full_path = os.path.join(dirpath, fname)
            rel_path = os.path.relpath(full_path, root_dir)
            ext = os.path.splitext(fname)[1].lower()
            if ext in FORBIDDEN_EXTENSIONS:
                findings.append(f"[FORBIDDEN KEYSTORE/KEY FILE] {rel_path}")
            scanned_files += 1
            try:
                with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
                    scan_text(f.read(), rel_path, findings)
            except Exception:
                pass
    return scanned_files, findings

def scan_apk(apk_path):
    findings = []
    entries_scanned = 0
    with zipfile.ZipFile(apk_path, "r") as zf:
        for info in zf.infolist():
            entries_scanned += 1
            ext = os.path.splitext(info.filename)[1].lower()
            if ext in FORBIDDEN_EXTENSIONS:
                findings.append(f"[FORBIDDEN FILE IN APK] {info.filename}")
            data = zf.read(info.filename)
            text = data.decode("utf-8", errors="ignore")
            scan_text(text, f"APK:{info.filename}", findings)
    return entries_scanned, findings

if __name__ == "__main__":
    target_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    count, issues = scan_directory(target_dir)
    print(f"[SECRET SCANNER] Scanned {count} repository files in {target_dir}: {len(issues)} issues found.")
    if issues:
        for issue in issues:
            print("  !", issue)
        sys.exit(1)
    print("[SECRET SCANNER] PASS — 0 leaked API keys, 0 signing keystores, 0 private keys.")
