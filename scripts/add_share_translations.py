from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
TRANSLATIONS = {
    "values-ar": "مشاركة",
    "values-b+ms+Arab": "کوڠسي",
    "values-bn": "শেয়ার",
    "values-cs": "Sdílet",
    "values-da": "Del",
    "values-da-rDK": "Del",
    "values-de": "Teilen",
    "values-el": "Κοινοποίηση",
    "values-es": "Compartir",
    "values-fa": "اشتراک‌گذاری",
    "values-fi": "Jaa",
    "values-fi-rFI": "Jaa",
    "values-fil": "Ibahagi",
    "values-fr": "Partager",
    "values-gu": "શેર કરો",
    "values-he": "שיתוף",
    "values-hi": "शेयर करें",
    "values-hu": "Megosztás",
    "values-id": "Bagikan",
    "values-it": "Condividi",
    "values-ja": "共有",
    "values-kn": "ಹಂಚಿಕೆ",
    "values-ko": "공유",
    "values-mr": "शेअर करा",
    "values-ms": "Kongsi",
    "values-my": "မျှဝေပါ",
    "values-nb-rNO": "Del",
    "values-nl": "Delen",
    "values-no": "Del",
    "values-pa": "ਸਾਂਝਾ ਕਰੋ",
    "values-pl": "Udostępnij",
    "values-pt": "Partilhar",
    "values-pt-rBR": "Compartilhar",
    "values-ro": "Distribuie",
    "values-ru": "Поделиться",
    "values-ru-rRU": "Поделиться",
    "values-sk": "Zdieľať",
    "values-sw": "Shiriki",
    "values-sv": "Dela",
    "values-sv-rSE": "Dela",
    "values-ta": "பகிர்",
    "values-te": "పంచుకోండి",
    "values-th": "แชร์",
    "values-tr": "Paylaş",
    "values-tr-rTR": "Paylaş",
    "values-uk": "Поділитися",
    "values-uk-rUA": "Поділитися",
    "values-ur": "شیئر کریں",
    "values-vi": "Chia sẻ",
    "values-zh-rCN": "分享",
    "values-zh-rTW": "分享",
}

ANCHORS = [
    '<string name="btn_cancel"',
    '<string name="btn_save"',
    '<string name="btn_no"',
    '<string name="btn_close"',
]

def insert_translation(path: Path, text: str) -> bool:
    data = path.read_text(encoding="utf-8")
    if 'name="btn_share"' in data:
        return False
    for anchor in ANCHORS:
        idx = data.find(anchor)
        if idx == -1:
            continue
        newline_idx = data.find("\n", idx)
        if newline_idx == -1:
            newline_idx = len(data)
        insertion = f"    <string name=\"btn_share\">{text}</string>\n"
        new_data = data[: newline_idx + 1] + insertion + data[newline_idx + 1 :]
        path.write_text(new_data, encoding="utf-8")
        return True
    raise RuntimeError(f"Could not find insertion point in {path}")

if __name__ == "__main__":
    updated = []
    skipped = []
    missing = []
    for folder, translation in TRANSLATIONS.items():
        target = ROOT / folder / "strings.xml"
        if not target.exists():
            missing.append(str(target))
            continue
        if insert_translation(target, translation):
            updated.append(folder)
        else:
            skipped.append(folder)
    print("Updated:", ", ".join(updated))
    if skipped:
        print("Already localized:", ", ".join(skipped))
    if missing:
        print("Missing string files:", ", ".join(missing))
