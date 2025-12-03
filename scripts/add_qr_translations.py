from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
ANCHOR = '<string name="help_footer_format">'

BASE_TRANSLATIONS = {
    "values-ar": {
        "share_qr_description": "امسح هذا الرمز لفتح صفحة التطبيق على Google Play.",
        "desc_share_qr": "رمز QR يرتبط بتطبيق Yet Another Timer على Google Play",
    },
    "values-b+ms+Arab": {
        "share_qr_description": "ايمبس کود ايني اونتوق ممبوکا سناٴي Google Play.",
        "desc_share_qr": "کود QR يڠ مماوءت ک Yet Another Timer د Google Play",
    },
    "values-bn": {
        "share_qr_description": "এই কোডটি স্ক্যান করে Google Play তালিকাটি খুলুন।",
        "desc_share_qr": "Google Play-এ Yet Another Timer-এ নিয়ে যায় এমন QR কোড",
    },
    "values-cs": {
        "share_qr_description": "Naskenujte tento kód a otevřete záznam v Google Play.",
        "desc_share_qr": "QR kód odkazující na Yet Another Timer v Google Play",
    },
    "values-da": {
        "share_qr_description": "Scan denne kode for at åbne butikssiden på Google Play.",
        "desc_share_qr": "QR-kode der linker til Yet Another Timer på Google Play",
    },
    "values-de": {
        "share_qr_description": "Scannen Sie diesen Code, um die Seite im Google Play Store zu öffnen.",
        "desc_share_qr": "QR-Code, der zu Yet Another Timer im Google Play Store führt",
    },
    "values-el": {
        "share_qr_description": "Σαρώστε αυτόν τον κωδικό για να ανοίξετε την καταχώριση στο Google Play.",
        "desc_share_qr": "QR κωδικός που οδηγεί στο Yet Another Timer στο Google Play",
    },
    "values-es": {
        "share_qr_description": "Escanea este código para abrir la ficha en Google Play.",
        "desc_share_qr": "Código QR que enlaza a Yet Another Timer en Google Play",
    },
    "values-fa": {
        "share_qr_description": "برای باز کردن صفحه برنامه در Google Play این کد را اسکن کنید.",
        "desc_share_qr": "کد QR که به Yet Another Timer در Google Play لینک می‌شود",
    },
    "values-fi": {
        "share_qr_description": "Skannaa tämä koodi avataksesi sovelluksen Google Playssa.",
        "desc_share_qr": "QR-koodi, joka vie Yet Another Timeriin Google Playssa",
    },
    "values-fil": {
        "share_qr_description": "I-scan ang code na ito para buksan ang listing sa Google Play.",
        "desc_share_qr": "QR code na kumokonekta sa Yet Another Timer sa Google Play",
    },
    "values-fr": {
        "share_qr_description": "Scannez ce code pour ouvrir la fiche sur Google Play.",
        "desc_share_qr": "Code QR menant à Yet Another Timer sur Google Play",
    },
    "values-gu": {
        "share_qr_description": "Google Play પરનું લિસ્ટિંગ ખોલવા માટે આ કોડ સ્કેન કરો.",
        "desc_share_qr": "Google Play પર Yet Another Timer જોડતો QR કોડ",
    },
    "values-he": {
        "share_qr_description": "סרקו את הקוד הזה כדי לפתוח את דף החנות ב-Google Play.",
        "desc_share_qr": "קוד QR שמפנה ל-Yet Another Timer ב-Google Play",
    },
    "values-hi": {
        "share_qr_description": "Google Play की सूची खोलने के लिए इस कोड को स्कैन करें।",
        "desc_share_qr": "Google Play पर Yet Another Timer से जोड़ने वाला QR कोड",
    },
    "values-hu": {
        "share_qr_description": "Olvassa be ezt a kódot a Google Play-oldal megnyitásához.",
        "desc_share_qr": "QR-kód, amely a Yet Another Timerhez vezet a Google Playen",
    },
    "values-id": {
        "share_qr_description": "Pindai kode ini untuk membuka listing di Google Play.",
        "desc_share_qr": "Kode QR yang menaut ke Yet Another Timer di Google Play",
    },
    "values-it": {
        "share_qr_description": "Scansiona questo codice per aprire la scheda su Google Play.",
        "desc_share_qr": "Codice QR che rimanda a Yet Another Timer su Google Play",
    },
    "values-ja": {
        "share_qr_description": "このコードをスキャンして Google Play の掲載ページを開きます。",
        "desc_share_qr": "Google Play の Yet Another Timer につながる QR コード",
    },
    "values-kn": {
        "share_qr_description": "Google Play ಪಟ್ಟಿಯನ್ನು ತೆರೆಯಲು ಈ ಕೋಡ್ ಅನ್ನು ಸ್ಕ್ಯಾನ್ ಮಾಡಿ.",
        "desc_share_qr": "Google Play ನಲ್ಲಿ Yet Another Timer ಗೆ ಸಂಪರ್ಕಿಸುವ QR ಕೋಡ್",
    },
    "values-ko": {
        "share_qr_description": "이 코드를 스캔하여 Google Play 상품 페이지를 여세요.",
        "desc_share_qr": "Google Play에서 Yet Another Timer로 연결되는 QR 코드",
    },
    "values-mr": {
        "share_qr_description": "Google Play वरील सूची उघडण्यासाठी हा कोड स्कॅन करा.",
        "desc_share_qr": "Google Play वरील Yet Another Timer ला जोडणारा QR कोड",
    },
    "values-ms": {
        "share_qr_description": "Imbas kod ini untuk membuka penyenaraian di Google Play.",
        "desc_share_qr": "Kod QR yang memaut ke Yet Another Timer di Google Play",
    },
    "values-my": {
        "share_qr_description": "ဤကုဒ်ကို စကန်ဖတ်၍ Google Play စာမျက်နှာကို ဖွင့်ပါ။",
        "desc_share_qr": "Google Play ပေါ်ရှိ Yet Another Timer သို့ ချိတ်ဆက်ပေးသော QR ကုဒ်",
    },
    "values-nb-rNO": {
        "share_qr_description": "Skann denne koden for å åpne butikksiden på Google Play.",
        "desc_share_qr": "QR-kode som lenker til Yet Another Timer på Google Play",
    },
    "values-nl": {
        "share_qr_description": "Scan deze code om de vermelding op Google Play te openen.",
        "desc_share_qr": "QR-code die verwijst naar Yet Another Timer op Google Play",
    },
    "values-pa": {
        "share_qr_description": "Google Play ਦੀ ਲਿਸਟਿੰਗ ਖੋਲ੍ਹਣ ਲਈ ਇਸ ਕੋਡ ਨੂੰ ਸਕੈਨ ਕਰੋ।",
        "desc_share_qr": "Google Play ਉੱਤੇ Yet Another Timer ਨਾਲ ਜੁੜਨ ਵਾਲਾ QR ਕੋਡ",
    },
    "values-pl": {
        "share_qr_description": "Zeskanuj ten kod, aby otworzyć kartę w Google Play.",
        "desc_share_qr": "Kod QR prowadzący do Yet Another Timer w Google Play",
    },
    "values-pt": {
        "share_qr_description": "Digitalize este código para abrir a listagem no Google Play.",
        "desc_share_qr": "Código QR que liga ao Yet Another Timer no Google Play",
    },
    "values-pt-rBR": {
        "share_qr_description": "Escaneie este código para abrir a página no Google Play.",
        "desc_share_qr": "Código QR que leva ao Yet Another Timer no Google Play",
    },
    "values-ro": {
        "share_qr_description": "Scanează acest cod pentru a deschide pagina din Google Play.",
        "desc_share_qr": "Cod QR care duce la Yet Another Timer în Google Play",
    },
    "values-ru": {
        "share_qr_description": "Сканируйте этот код, чтобы открыть страницу в Google Play.",
        "desc_share_qr": "QR-код, ведущий на Yet Another Timer в Google Play",
    },
    "values-sk": {
        "share_qr_description": "Naskenujte tento kód a otvorte záznam v službe Google Play.",
        "desc_share_qr": "QR kód odkazujúci na Yet Another Timer v službe Google Play",
    },
    "values-sw": {
        "share_qr_description": "Changanua msimbo huu ili kufungua ukurasa wa Google Play.",
        "desc_share_qr": "Msimbo wa QR unaounganisha na Yet Another Timer kwenye Google Play",
    },
    "values-sv": {
        "share_qr_description": "Skanna den här koden för att öppna listningen på Google Play.",
        "desc_share_qr": "QR-kod som länkar till Yet Another Timer på Google Play",
    },
    "values-ta": {
        "share_qr_description": "Google Play பட்டியலைத் திறக்க இந்தக் குறியீட்டை ஸ்கேன் செய்யவும்.",
        "desc_share_qr": "Google Play இல் Yet Another Timer-க்கு இணைக்கும் QR குறியீடு",
    },
    "values-te": {
        "share_qr_description": "Google Play జాబితాను తెరవడానికి ఈ కోడ్‌ను స్కాన్ చేయండి.",
        "desc_share_qr": "Google Playలోని Yet Another Timer‌కు కలుపుతున్న QR కోడ్",
    },
    "values-th": {
        "share_qr_description": "สแกนโค้ดนี้เพื่อเปิดหน้ารายการใน Google Play",
        "desc_share_qr": "คิวอาร์โค้ดที่เชื่อมไปยัง Yet Another Timer บน Google Play",
    },
    "values-tr": {
        "share_qr_description": "Bu kodu tarayarak Google Play listesini açın.",
        "desc_share_qr": "Google Play’de Yet Another Timer’a bağlanan QR kodu",
    },
    "values-uk": {
        "share_qr_description": "Відскануйте цей код, щоб відкрити сторінку в Google Play.",
        "desc_share_qr": "QR-код, що веде до Yet Another Timer у Google Play",
    },
    "values-ur": {
        "share_qr_description": "Google Play کی فہرست کھولنے کے لیے اس کوڈ کو اسکین کریں۔",
        "desc_share_qr": "Google Play پر Yet Another Timer سے جڑنے والا QR کوڈ",
    },
    "values-vi": {
        "share_qr_description": "Quét mã này để mở trang trên Google Play.",
        "desc_share_qr": "Mã QR dẫn tới Yet Another Timer trên Google Play",
    },
    "values-zh-rCN": {
        "share_qr_description": "扫描此代码以打开 Google Play 列表。",
        "desc_share_qr": "指向 Google Play 上 Yet Another Timer 的二维码",
    },
    "values-zh-rTW": {
        "share_qr_description": "掃描此代碼以開啟 Google Play 資訊頁。",
        "desc_share_qr": "連結至 Google Play 上 Yet Another Timer 的 QR 圖碼",
    },
}

TITLE_TRANSLATIONS = {
    "values-ar": "شارك تطبيق Yet Another Timer",
    "values-b+ms+Arab": "کوڠسي Yet Another Timer",
    "values-bn": "Yet Another Timer শেয়ার করুন",
    "values-cs": "Sdílet Yet Another Timer",
    "values-da": "Del Yet Another Timer",
    "values-de": "Yet Another Timer teilen",
    "values-el": "Κοινοποιήστε το Yet Another Timer",
    "values-es": "Compartir Yet Another Timer",
    "values-fa": "اشتراک‌گذاری Yet Another Timer",
    "values-fi": "Jaa Yet Another Timer",
    "values-fil": "Ibahagi ang Yet Another Timer",
    "values-fr": "Partager Yet Another Timer",
    "values-gu": "Yet Another Timer શેર કરો",
    "values-he": "שתף את Yet Another Timer",
    "values-hi": "Yet Another Timer साझा करें",
    "values-hu": "Yet Another Timer megosztása",
    "values-id": "Bagikan Yet Another Timer",
    "values-it": "Condividi Yet Another Timer",
    "values-ja": "Yet Another Timer を共有",
    "values-kn": "Yet Another Timer ಅನ್ನು ಹಂಚಿಕೊಳ್ಳಿ",
    "values-ko": "Yet Another Timer 공유",
    "values-mr": "Yet Another Timer शेअर करा",
    "values-ms": "Kongsi Yet Another Timer",
    "values-my": "Yet Another Timer ကိုမျှဝေပါ",
    "values-nb-rNO": "Del Yet Another Timer",
    "values-nl": "Deel Yet Another Timer",
    "values-pa": "Yet Another Timer ਸਾਂਝਾ ਕਰੋ",
    "values-pl": "Udostępnij Yet Another Timer",
    "values-pt": "Partilhar o Yet Another Timer",
    "values-pt-rBR": "Compartilhar Yet Another Timer",
    "values-ro": "Distribuie Yet Another Timer",
    "values-ru": "Поделиться Yet Another Timer",
    "values-sk": "Zdieľať Yet Another Timer",
    "values-sw": "Shiriki Yet Another Timer",
    "values-sv": "Dela Yet Another Timer",
    "values-ta": "Yet Another Timer பகிர்",
    "values-te": "Yet Another Timer ను పంచుకోండి",
    "values-th": "แชร์ Yet Another Timer",
    "values-tr": "Yet Another Timer’ı paylaş",
    "values-uk": "Поділитися Yet Another Timer",
    "values-ur": "Yet Another Timer شیئر کریں",
    "values-vi": "Chia sẻ Yet Another Timer",
    "values-zh-rCN": "分享 Yet Another Timer",
    "values-zh-rTW": "分享 Yet Another Timer",
}

ALIASES = {
    "values-da-rDK": "values-da",
    "values-fi-rFI": "values-fi",
    "values-no": "values-nb-rNO",
    "values-ru-rRU": "values-ru",
    "values-sv-rSE": "values-sv",
    "values-tr-rTR": "values-tr",
    "values-uk-rUA": "values-uk",
}

TRANSLATIONS: dict[str, dict[str, str]] = {key: value for key, value in BASE_TRANSLATIONS.items()}
for alias, target in ALIASES.items():
    if target not in BASE_TRANSLATIONS:
        raise SystemExit(f"Missing base translation for alias source {target}")
    TRANSLATIONS[alias] = BASE_TRANSLATIONS[target].copy()
    TITLE_TRANSLATIONS.setdefault(alias, TITLE_TRANSLATIONS.get(target))

for folder in TRANSLATIONS:
    if folder not in TITLE_TRANSLATIONS:
        raise SystemExit(f"Missing title translation for {folder}")


def read_text(path: Path) -> str:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return handle.read()


def write_text(path: Path, data: str) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        handle.write(data)


def detect_newline(content: str) -> str:
    return "\r\n" if "\r\n" in content else "\n"


def upsert_string(*, content: str, name: str, value: str, newline: str, path: Path) -> str:
    pattern = re.compile(rf'(<string name="{name}">)(.*?)(</string>)', re.DOTALL)
    if pattern.search(content):
        return pattern.sub(lambda match: f"{match.group(1)}{value}{match.group(3)}", content, count=1)

    insertion = f"    <string name=\"{name}\">{value}</string>{newline}"
    anchor_index = content.find(ANCHOR)
    if anchor_index != -1:
        return content[:anchor_index] + insertion + content[anchor_index:]

    closing_tag = "</resources>"
    closing_index = content.find(closing_tag)
    if closing_index == -1:
        raise RuntimeError(f"Could not find insertion point in {path}")
    return content[:closing_index] + insertion + content[closing_index:]


def update_file(path: Path, mapping: dict[str, str]) -> bool:
    content = read_text(path)
    newline = detect_newline(content)
    updated = content
    for name, value in mapping.items():
        updated = upsert_string(content=updated, name=name, value=value, newline=newline, path=path)
    if updated != content:
        write_text(path, updated)
        return True
    return False


def main() -> None:
    updated: list[str] = []
    skipped: list[str] = []
    missing: list[str] = []

    for folder, mapping in TRANSLATIONS.items():
        target = ROOT / folder / "strings.xml"
        if not target.exists():
            missing.append(str(target))
            continue
        mapping_with_title = mapping.copy()
        mapping_with_title["title_share_qr"] = TITLE_TRANSLATIONS[folder]
        if update_file(target, mapping_with_title):
            updated.append(folder)
        else:
            skipped.append(folder)

    if updated:
        print("Updated:", ", ".join(sorted(updated)))
    if skipped:
        print("Already up to date:", ", ".join(sorted(skipped)))
    if missing:
        print("Missing string files:", ", ".join(sorted(missing)))


if __name__ == "__main__":
    main()
